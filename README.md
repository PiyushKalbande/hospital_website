# चींचे हॉस्पिटल · Chinche Hospital

A production-shaped, bilingual (English / मराठी) hospital website and patient-booking
platform for a 120-bed multispeciality hospital in **Katol, Nagpur district, Maharashtra**.

Built as a full-stack reference implementation of a modern hospital web platform:
server-rendered public site for SEO, a JSON API for the interactive booking widget,
and an authenticated staff console for reception and administration.

> **Scope note.** The information architecture (page inventory, section ordering,
> booking-flow shape) follows the conventions of large Indian hospital websites.
> All branding, copy, iconography, illustration, colour, data and code in this
> repository are original work created for this project. No third-party marketing
> text, logos or images are reproduced.

---

## 1. Quick start

### Option A — zero-infrastructure (H2, recommended for a first look)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# → http://localhost:8080
```

The `dev` profile boots an in-memory H2 database in MySQL-compatibility mode and
seeds a complete demo dataset: 4 centres, 18 specialities, 18 doctors with weekly
schedules, 15 procedures, 12 conditions, 6 health packages, 18 lab tests, blogs,
announcements, patient stories, testimonials and awards.

### Option B — MySQL 8 (production-shaped)

For Docker Compose, copy `.env.example` to `.env` and replace the example
passwords before starting the stack. `.env` is excluded from Git and the Docker
build context.

```bash
docker compose up -d mysql adminer mailpit     # MySQL :3306, Adminer :8081, Mailpit :8025
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Option C — everything containerised

```bash
docker compose --profile full up -d --build    # app :8080 + MySQL + Mailpit
```

### Staff console

| URL | Role | Username | Password |
|---|---|---|---|
| `/admin/login` | Administrator | `admin` | `Chinche@2026` |
| `/admin/login` | Reception | `reception` | `Reception@2026` |

Override with `ADMIN_PASSWORD` / `RECEPTION_PASSWORD` environment variables.
Demo accounts are only created when `chinche.seeding.enabled=true` (on by default
in `dev`, off in `mysql` unless explicitly enabled).

---

## 2. Architecture

```
                       ┌──────────────────────────────────────────┐
   Browser ──HTML────► │  Thymeleaf SSR  (30 public pages)         │  SEO-critical
                       │  layout/default + fragments/*             │  crawlers, OG, JSON-LD
                       └───────────────┬──────────────────────────┘
                                       │
                       ┌───────────────▼──────────────────────────┐
   Browser ──JSON────► │  REST /api/v1  (9 controllers)            │  booking widget,
                       │  ApiResponse<T> envelope                  │  search suggest, admin
                       └───────────────┬──────────────────────────┘
                                       │
                       ┌───────────────▼──────────────────────────┐
                       │  Service layer (11)  @Transactional       │  DTO projection happens
                       │  Catalog · Doctor · Booking · Slot        │  *inside* the transaction
                       │  Availability · Content · Search · …      │
                       └───────────────┬──────────────────────────┘
                                       │
                       ┌───────────────▼──────────────────────────┐
                       │  Spring Data JPA (20 repositories)        │
                       │  21 entities · 7 enums · optimistic lock  │
                       └───────────────┬──────────────────────────┘
                                       │
                          MySQL 8  (prod)  ·  H2 MySQL-mode (dev)
```

**Hybrid rendering was a deliberate choice.** Content pages that must rank — home,
specialities, doctors, procedures, conditions, packages, centres, blogs — are
server-rendered so crawlers see complete HTML with no JavaScript. The booking
widget is the one genuinely interactive surface, so it talks to the JSON API and
renders its own state client-side.

### Package layout

```
in.chinche.hospital
├── api/          9 REST controllers under /api/v1, uniform ApiResponse envelope
├── config/       AppProperties, SecurityConfig, WebConfig, I18nConfig,
│                 CacheConfig, AsyncConfig, OpenApiConfig, DataSeeder
├── domain/       21 JPA entities  +  domain/enums/ (7 enums)
├── dto/          request/ (4) and response/ (13) records — the API contract
├── exception/    ResourceNotFound (404), SlotUnavailable (409), GlobalExceptionHandler
├── repository/   20 Spring Data repositories
├── security/     AppUserDetailsService
├── service/      11 services + service/seeding/DemoDataSeeder
└── web/          6 SSR controllers + LocaleInterceptor + GlobalModelAdvice
```

---

## 3. The booking widget

`/book` (and the compact variant embedded on the home page) is a six-step flow in
`static/js/booking-widget.js` (~860 lines, no framework):

| Step | Interaction | Endpoint |
|---|---|---|
| 1 | Choose the type of care (7 service types) | — |
| 2 | Choose a speciality, then a doctor — or a health package | `/api/v1/specialities`, `/api/v1/doctors` |
| 3 | Choose in-hospital / video / home visit | — |
| 4 | Pick a date, then a free slot | `/api/v1/availability/doctors/{id}?days=16` |
| 5 | Patient details, validated client- *and* server-side | — |
| 6 | Confirm | `POST /api/v1/bookings` |

The server publishes a single serialised data island — `<script type="application/json"
id="chhBookingData">` — built by `BookingWidgetDataService` with Jackson, including
the resolved i18n strings for the active locale. The widget therefore needs exactly
one round-trip to hydrate, and switches language without refetching.

**Concurrency.** Slots are not a table; availability is derived from each doctor's
weekly schedule minus already-booked appointments. Booking is transactional and a
second request for the same doctor/date/slot is rejected with **409 Conflict** and a
message that asks the patient to pick another time. Verified behaviour:

```
POST /api/v1/bookings           → 201  CHH-2026-000001
POST /api/v1/bookings (same)    → 409  "That slot was just taken…"
POST  {past date}               → 400  field-level validation errors
POST  {bad phone}               → 400  "Enter a valid 10-digit Indian mobile number."
POST  {malformed JSON}          → 400  (not 500)
POST /bookings/{ref}/cancel     → 200  slot becomes available again
```

Reference format: `CHH-YYYY-NNNNNN`. Patients can look a booking up at
`/track` or via the SMS deep link `/track/CHH-2026-000001`.

---

## 4. Bilingual EN / मराठी

- `?lang=mr` / `?lang=en` switches locale; the choice persists in the session
  (`LocaleInterceptor` + `SessionLocaleResolver`).
- **141 message keys** in `i18n/messages.properties` with a full Marathi
  counterpart in `messages_mr.properties`.
- Every content entity carries `nameMr` / `titleMr` / `taglineMr` columns, so
  clinical vocabulary is translated by the hospital, not machine-generated.
- Devanagari is set in Noto Sans Devanagari with Latin text in Plus Jakarta Sans;
  `<html lang>` flips so screen readers pick the right voice.

---

## 5. Data model

20 tables (`docs/sql/schema-mysql.sql`, generated from the JPA metadata):

`app_user` · `patient` · `doctor` · `doctor_schedule` · `doctor_leave` ·
`speciality` · `procedure` · `health_condition` · `health_package` · `lab_test` ·
`facility` · `appointment` · `enquiry` · `blog` · `announcement` ·
`success_story` · `testimonial` · `award` · `hero_slide` · `quick_link`

Design points worth calling out:

- **Optimistic locking** (`@Version`) on `appointment`, `doctor` and `doctor_schedule`
  — two receptionists confirming the same slot cannot silently overwrite each other.
- **Auditing** via `BaseEntity` (`createdAt`, `updatedAt`) on every table.
- **Enums persisted as `ENUM` columns** in MySQL for `AppointmentStatus`,
  `ServiceType`, `VisitMode`, `UserRole`, `Gender`, `EnquiryStatus`, `ContentStatus`,
  so the data is queryable by hand.
- **Money is `DECIMAL(10,2)`**, never float.
- Slugs are unique and indexed; every public listing is filtered on `active = true`.

---

## 6. HTTP surface

**Public pages (30)** — `/`, `/book`, `/doctors`, `/doctors/{slug}`, `/specialities`,
`/specialities/{slug}`, `/procedures`, `/procedures/{slug}`, `/conditions`,
`/conditions/{slug}`, `/health-packages`, `/health-packages/{slug}`, `/lab-tests`,
`/locations`, `/locations/{slug}`, `/blogs`, `/blogs/{slug}`, `/announcements`,
`/announcements/{slug}`, `/patient-stories`, `/about`, `/contact`, `/emergency`,
`/insurance`, `/careers`, `/track`, `/track/{ref}`, `/reports`, `/blood-bank`,
`/search`, `/sitemap`.

**REST (`/api/v1`)** — full list in [`docs/API.md`](docs/API.md). Swagger UI is at
`/swagger-ui/index.html` (administrator only).

**Admin console (8 views)** — `/admin/dashboard`, `/admin/appointments`,
`/admin/doctors`, `/admin/catalogue`, `/admin/facilities`, `/admin/enquiries`,
plus the login screen and the shared console layout.

---

## 7. Security

- Public site and public booking API are anonymous; `/admin/**` and
  `/api/v1/admin/**` require `ROLE_ADMIN` or `ROLE_RECEPTIONIST`.
- Form login with BCrypt (strength 11), session-based, CSRF **on** for the admin
  console and off for the stateless `/api/v1/**` JSON surface.
- Hardened headers: HSTS (1 year, includeSubDomains), `X-Content-Type-Options`,
  `X-Frame-Options: SAMEORIGIN` (needed for the H2 console and map embeds),
  strict-origin referrer policy.
- Actuator is limited to `health` and `info` publicly; everything else is admin-only.
- Bean Validation on every request record — server-side rules are authoritative,
  the client mirrors them for UX only.

Two non-obvious traps this codebase deliberately avoids, both of which produced
real bugs during development and are documented in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md):

1. **CSRF matcher replacement.** Calling `requireCsrfProtectionMatcher(...)`
   *replaces* Spring Security's default matcher, dropping its exemption for safe
   methods. The result is a 403 on `GET /admin/login` — the login page locks itself.
2. **Caching entities.** `@Cacheable` on a method returning JPA entities stores
   detached objects whose lazy proxies point at a closed session. Every cacheable
   method here returns an immutable DTO instead, and all of them carry
   `unless = "#result == null or #result.isEmpty()"` so a warm-up request that
   arrives before seeding completes cannot poison the cache permanently.

---

## 8. Frontend

No build step, no bundler, no CDN dependency — everything is vendored so the site
works on an intranet or in an air-gapped hospital network.

- **Bootstrap 5.3.3**, vendored under `static/vendor/bootstrap/`.
- **`static/css/app.css`** (~1,060 lines) — a `chh-`* design system layered over
  Bootstrap: primary teal `#0E7C66`, saffron accent `#E8A33D`, ink `#06211D`.
  Component classes for cards, chips, rails, tabs, the booking widget, the admin
  console, KPI tiles, status pills and the hero carousel.
- **`static/img/icons/sprite.svg`** — 82 hand-drawn medical, navigation and social
  symbols, referenced as `<svg><use href="/img/icons/sprite.svg#stethoscope"/></svg>`.
- **`static/js/app.js`** (~370 lines) — sticky header, mobile nav, omni-search
  autocomplete with keyboard navigation and `AbortController` debouncing, carousel
  rails, `IntersectionObserver` scroll reveals, animated counters, tabs, enquiry and
  newsletter forms, booking lookup, copy-to-clipboard, floating action button.
- **`static/js/booking-widget.js`** (~860 lines) — the six-step booking flow.
- PWA manifest, `robots.txt`, favicon.

Accessibility: visible focus rings, `aria-*` on tabs/carousel/disclosure widgets,
semantic landmarks, `prefers-reduced-motion` honoured, and a text-only fallback for
every icon.

---

## 9. Configuration

Everything is environment-variable driven with sane defaults — see
`src/main/resources/application.yml`.

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | MySQL location |
| `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `chinche_hospital` / `chinche` / `chinche` | credentials |
| `ADMIN_PASSWORD` | `Chinche@2026` | seeded admin password |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP (Mailpit in dev) |
| `chinche.seeding.enabled` | `true` (dev) | load the demo dataset |

Profiles: `dev` (H2, show-sql, devtools, seeding on), `mysql` (MySQL 8, ddl-auto
`update`, seeding off unless `SEED_DEMO_DATA=true` is set).

---

## 10. Scaling notes

The current deployment is a single Spring Boot instance with in-process caching —
correct for one hospital, and deliberately easy to reason about. The seams for
growth are already in place:

- **Cache** — `CacheConfig` exposes a Spring `CacheManager`. Swapping
  `ConcurrentMapCache` for Redis is a dependency plus configuration change, with no
  service-code edits, because every cacheable method already returns a serialisable DTO.
- **Database** — MySQL 8 with indexed slugs, `active` flags and date columns; the
  appointment queries used by the console are already date- and status-scoped.
- **Statelessness** — the public API holds no session. Horizontal scaling needs only
  sticky sessions for the admin console, or Spring Session backed by Redis.
- **Async** — `AsyncConfig` provides a bounded executor for confirmation mail and
  reminders, so a slow SMTP server cannot block a booking response.
- **Search** — `/api/v1/search/suggest` is a LIKE-based query today; the interface is
  stable, so an Elasticsearch or OpenSearch backend can replace the implementation.

---

## 11. Verification status

Every route was exercised against a running instance:

- **30 public pages** — HTTP 200, complete document (exactly one `<!DOCTYPE>`),
  real seeded content, no server-side template errors.
- **12 detail pages** — doctor, speciality, procedure, condition, package, centre,
  blog, announcement, plus the SMS tracking deep link.
- **23 REST endpoints** — HTTP 200 with the `ApiResponse` envelope.
- **6 admin views** — HTTP 200 authenticated; anonymous requests redirect to login;
  `/api/v1/admin/**` returns 403 without a session.
- **Error pages** — unknown slugs and unknown paths return a branded **404**
  (not 500); malformed input returns **400**.
- **Booking lifecycle** — create, duplicate rejection, validation, lookup by
  reference and by phone, cancellation, slot release, and visibility in the console.
- **Marathi locale** — `?lang=mr` renders Devanagari across navigation, widget and
  content.

---

## 12. Repository

```
chinche-hospital/
├── pom.xml                  Spring Boot 3.3.5, Java 17
├── Dockerfile               multi-stage build, non-root JRE runtime
├── docker-compose.yml       MySQL 8.4 + Adminer + Mailpit (+ app profile)
├── docs/
│   ├── API.md               REST reference
│   ├── ARCHITECTURE.md      design decisions and the bugs they prevented
│   └── sql/schema-mysql.sql generated MySQL DDL (20 tables, 22 indexes)
└── src/main/
    ├── java/in/chinche/hospital/    105 classes
    └── resources/
        ├── application*.yml         base + dev + mysql profiles
        ├── i18n/                    messages.properties, messages_mr.properties
        ├── static/                  css, js, vendored Bootstrap, SVG sprite, art
        └── templates/               layout, fragments, 30 pages, 8 admin views, error
```
