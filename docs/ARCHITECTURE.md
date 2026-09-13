# Architecture decisions

This document records *why* the system is shaped the way it is, including the
failure modes that were found by running the application and the fixes that are now
load-bearing. It is written for whoever maintains this next.

---

## 1. Hybrid rendering: SSR for content, JSON for interaction

Two rendering strategies coexist on purpose.

**Server-rendered (Thymeleaf)** for every page a patient or a search engine reads:
home, specialities, doctors, procedures, conditions, packages, centres, blogs,
announcements, stories, about, contact. These ship complete HTML — no client-side
hydration, no dependency on JavaScript for content. That matters for a regional
hospital: crawlers index it reliably, it works on a low-end Android phone over a
weak mobile connection, and it degrades to readable text with CSS disabled.

**Client-rendered (vanilla JS + REST)** for the one surface with real interaction
state: the booking widget. A six-step flow with date grids, slot selection, live
availability and per-field validation would be painful as a chain of form posts, and
has no SEO value at all.

The boundary is drawn at *statefulness*, not at convenience. Anything that must rank
is server-rendered; anything that must respond instantly is client-rendered.

---

## 2. `open-in-view: false`, and what that obliges

Open Session In View is disabled. It hides lazy-loading mistakes by keeping a
database session open for the whole request, which turns a bug into a
production connection-pool exhaustion incident. Turning it off makes every lazy
access fail loudly at development time instead.

The obligation it creates: **all association traversal must happen inside a
`@Transactional` service method.** Three consequences shaped the code.

### 2.1 To-one associations on lookup tables are `EAGER`

`Doctor.speciality`, `Doctor.facility`, `Procedure.speciality`,
`HealthCondition.speciality`, `Appointment.patient`, `SuccessStory.doctor` and
similar are all `@ManyToOne(fetch = EAGER)`. These point at small reference tables
(18 specialities, 4 centres) that are read on essentially every page. Fetching them
eagerly costs one join and removes an entire class of runtime failure across both
templates and the API.

Collections stay `LAZY`. `Speciality.doctors`, `Speciality.procedures` and
`Doctor.schedules` are genuinely variable-size and are loaded explicitly by a named
service method when a page actually needs them.

### 2.2 Inverse collections are `@JsonIgnore`

`Speciality.doctors`, `Speciality.procedures` and `Doctor.schedules` are annotated
`@JsonIgnore`. Without it, serialising a `Speciality` walks into its `doctors`
collection — lazy, so it throws; and if it were loaded, each `Doctor` walks back to
its `speciality`, which recurses. The annotation makes the failure impossible rather
than merely unlikely.

### 2.3 The API returns DTOs, never entities

Every `/api/v1` endpoint returns a record from `dto/response`. This is not ceremony:

- DTOs are immutable and serialisation-safe by construction — no proxies, no cycles.
- They are the published contract. An entity refactor cannot silently change the API.
- They are safe to cache (see §4).

Where a projection needs a related aggregate, the DTO flattens it:
`DoctorDto.specialityName` and `DoctorDto.facilityCity` rather than nested objects.

**The bug this prevented.** An early version of `/api/v1/doctors/featured` mapped
`DoctorDto.from(entity)` *in the controller*, outside any transaction. The first
`d.getSpeciality().getName()` threw `LazyInitializationException` and surfaced as a
500. Mapping now happens in `DoctorService.featuredDtos()`, inside the read-only
transaction. The same fix applies to `bySpecialityDtos()`, `allProcedureDtos()`,
`allConditionDtos()` and `allLabTestDtos()`.

**A related trap worth naming:** a template that reads a property the DTO does not
have fails *mid-render*. Thymeleaf writes the response as it goes, so when the
expression throws part-way through `/doctors`, Spring forwards to `/error` and the
error page is appended to the already-committed output. The client sees HTTP 200 and
a document containing two `<!DOCTYPE>` declarations. That is why the verification
suite counts `<!DOCTYPE>` occurrences per page rather than trusting the status code —
a green 200 can still be a broken page. The specific instance was
`d.speciality.name` in the doctor-card fragment after the callers switched from
entities to `DoctorDto`, whose field is `specialityName`.

---

## 3. Count instead of materialise

`SpecialityDto` needs a panel size. The obvious implementation is
`s.getDoctors().size()`, which initialises the entire lazy collection — every doctor
in the department — to produce one integer. With `open-in-view: false` it also
throws outright when called outside a transaction.

`CatalogService.specialityDtoBySlug()` instead calls
`doctors.countBySpecialityIdAndActiveTrue(id)`: one `SELECT COUNT(*)`, no collection,
no session lifetime concerns. `SpecialityDto.from()` was removed so the unsafe path
is not available to call by accident; the two remaining factories are
`of(entity, count)` and `lite(entity)`.

---

## 4. Cache DTOs, and never cache an empty result

`CacheConfig` exposes a Spring `CacheManager` over `ConcurrentMapCache`. The
abstraction is the point: moving to Redis for a multi-node deployment is a dependency
and a configuration change, with no edits to service code — *because* every cacheable
method already returns an immutable, serialisable DTO rather than a Hibernate entity.

Two rules are enforced:

**Rule 1 — cacheable methods return DTOs.** Caching an entity stores a detached
object whose lazy proxies reference a session that closed minutes ago. The next
reader gets `LazyInitializationException` from inside Jackson, with a stack trace
that points at the serialiser rather than at the cache.

**Rule 2 — every `@Cacheable` carries
`unless = "#result == null or #result.isEmpty()"`.** The web server binds its port
during context refresh, and `ApplicationRunner`-style seeding completes *after*
that. A request landing in that window — a container health probe, a browser
prefetch — would miss the cache, read an empty table, and store `[]`. Because
`ConcurrentMapCache` has no TTL, that empty list is then served forever, while an
uncached code path reading the same table shows the data is really there. This is
exactly what happened: `/admin/catalogue` listed six health packages while
`/api/v1/health-packages` returned `[]`. Refusing to cache empty results makes the
window self-healing.

---

## 5. Security configuration: two traps

### 5.1 Replacing the CSRF matcher locks the login page

```java
// Breaks the console:
.csrf(c -> c.ignoringRequestMatchers("/api/v1/**")
            .requireCsrfProtectionMatcher(new AntPathRequestMatcher("/admin/**")))
```

`requireCsrfProtectionMatcher` does not *add* to the default matcher — it **replaces**
it. The default (`CsrfFilter.createDefaultRequiresCsrfMatcher`) exempts `GET`, `HEAD`,
`TRACE` and `OPTIONS`. Replacing it with a plain path matcher means `GET /admin/login`
requires a CSRF token, which an anonymous visitor cannot have. Result: **403 on the
login page itself**, and no exception in the log — `MissingCsrfTokenException` is
handled by the `AccessDeniedHandler`, so it looks like an authorisation problem
rather than a configuration one.

The fix is to configure only the exemptions and leave the default matcher alone:

```java
.csrf(c -> c.ignoringRequestMatchers("/api/v1/**", "/h2-console/**"))
```

### 5.2 Permit rules are order-sensitive

`.requestMatchers("/admin/**").hasAnyRole(...)` also matches `/admin/login`. Declared
before the form-login configuration takes effect, it denies the login page. The
explicit permit must come first:

```java
.requestMatchers("/admin/login", "/admin/logout").permitAll()
.requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "RECEPTIONIST")
```

### 5.3 Exception handlers are scoped, and `Exception` swallows everything

`GlobalExceptionHandler` is a `@RestControllerAdvice(basePackages = "...api")` — it
governs the JSON API only. Browser traffic never reaches it; it is handled by
`ExceptionTranslationFilter` and then by `ErrorPageController`.

Two consequences:

- `AccessDeniedException` on `/api/v1/admin/**` was falling through to the catch-all
  `@ExceptionHandler(Exception.class)` and returning **500**. It now has an explicit
  handler returning 403 with the standard envelope.
- `ResourceNotFoundException` needed `@ResponseStatus(HttpStatus.NOT_FOUND)`. The
  advice does not apply to the SSR controllers, so an unknown slug such as
  `/doctors/not-a-real-doctor` was rendering the 500 page. With the annotation,
  Spring sets 404 and the error dispatch renders the branded "we couldn't find that
  page" template.

Malformed JSON is likewise a client error: `HttpMessageNotReadableException` and
`MethodArgumentTypeMismatchException` both map to 400. Before that, `POST`ing
`{bad json` to `/api/v1/bookings` produced a 500 and an error log entry — noise that
would mask real faults and lets a caller generate alert traffic for free.

---

## 6. Thymeleaf: three expression traps

### 6.1 `th:each` and `th:replace` on the same element

```html
<!-- Wrong: renders the fragment ONCE, with a null argument -->
<div th:each="d : ${doctors}" th:replace="~{fragments/page-head :: doctorCard(${d})}"></div>
```

Precedence decides this. `th:replace`/`th:insert` run at precedence 100; `th:each`
runs at 200. The replacement therefore happens *before* iteration, the fragment is
included a single time, and the loop variable is not yet bound. The same applies to
`th:unless` (precedence 300) combined with `th:replace` — the filter silently never
runs, which is how a doctor ended up listed among their own colleagues.

```html
<!-- Correct -->
<th:block th:each="d : ${doctors}">
  <div th:replace="~{fragments/page-head :: doctorCard(${d})}"></div>
</th:block>
```

### 6.2 `#lists` has no `subList`

The footer capped its speciality list with
`#lists.subList(navSpecialities, 0, ...)`. Thymeleaf's `Lists` expression object
exposes no such method, and the failure is a `SpelEvaluationException` at render
time — on *every* page, because the footer is on every page. Iteration status is the
idiomatic answer:

```html
<li th:each="s, st : ${navSpecialities}" th:if="${st.index < 10}">
```

### 6.3 Type references are forbidden, and number formatting has surprising overloads

Thymeleaf's SpEL evaluation context blocks type references, so `T(java.lang.System)`
and `T(java.lang.Math)` throw `EvaluationException: Access is forbidden for type …`.
Split a string with a literal separator; clamp with a ternary.

`#numbers.formatDecimal` is worse than it looks. The actual overloads in
Thymeleaf 3.1.2 are:

```
formatDecimal(Number, Integer minIntegerDigits, Integer decimalDigits)
formatDecimal(Number, Integer minIntegerDigits, Integer decimalDigits, String decimalPointType)
formatDecimal(Number, Integer minIntegerDigits, String thousandsPointType, Integer decimalDigits, String decimalPointType)
formatInteger(Number, Integer minIntegerDigits)
formatInteger(Number, Integer minIntegerDigits, String thousandsPointType)
```

So `formatDecimal(price, 0, 'COMMA')` — the form most people reach for — binds to the
four-argument overload with `'COMMA'` in the `decimalDigits` position and fails with
`cannot convert from java.lang.String to java.lang.Integer`. For whole rupee amounts
the unambiguous choice is `#numbers.formatInteger(price, 0, 'COMMA')`.

These signatures were confirmed by reflecting over the shipped jar rather than from
memory, which is the only reliable way to settle an overload-resolution question.

### 6.4 Fragment templates must not be decorated

Files under `templates/fragments/` declare `xmlns:th` only. If a fragment file also
carried `layout:decorate` on its root element, extracting a fragment from it would
pull the entire layout into the output. Worth stating because the symptom — a whole
second HTML document nested inside a card — does not suggest its cause.

### 6.5 No `#httpServletRequest`

Thymeleaf 3.1 removed it. Anything the template needs from the request goes into the
model explicitly (`lang`, `isMarathi`, `breadcrumb`, `activeView`).

---

## 7. Fragments take scalars, not framework objects

The pagination fragment originally accepted a page object and read `page.number`.
Callers passed two different types: Spring Data's `Page` (which has `number`) and the
API's `PageDto` (which has `page`). SpEL does not return null for a missing property
— it throws. So the fragment worked for blogs and broke for doctors.

The fragment now takes `pagination(currentPage, totalPages, baseUrl)`. Callers
supply the values:

```html
<div th:replace="~{fragments/page-head :: pagination(${results.page}, ${results.totalPages}, '/doctors')}"></div>
<div th:replace="~{fragments/page-head :: pagination(${blogPage.number}, ${blogPage.totalPages}, '/blogs')}"></div>
```

Same principle as the DTO rule: a shared component should not have to know which
implementation produced its input.

---

## 8. Derived query names encode types

`findTop8ByActiveTrueAndFeaturedOrderBySortOrderAsc` fails at **startup**, not at
call time: `featured` is a `boolean`, so the property keyword must carry the value —
`...AndFeaturedTrueOrderBy...`. Spring Data reports
`expects at least 1 arguments but only found 0 … leaves an operator of type
SIMPLE_PROPERTY for property featured unbound`.

Startup failure is the good outcome here. It is worth knowing that these names are
parsed when the repository bean is created, so a typo anywhere in a derived query
prevents the application from booting at all.

---

## 9. Bean cycles across configuration classes

`WebConfig` implemented `WebMvcConfigurer` (registering `LocaleInterceptor`) *and*
declared the `LocaleResolver` bean that the interceptor injects. That is a cycle:
`webConfig → localeInterceptor → localeResolver → webConfig`. Spring Boot 3 refuses
to start, by design.

Split by responsibility: `I18nConfig` owns the locale resolver and the message
source; `WebConfig` owns interceptor registration and resource handlers. Neither
depends on the other.

---

## 10. The booking widget's single data island

`BookingWidgetDataService` serialises one JSON document — doctors, packages, service
types, visit modes, the resolved i18n strings for the active locale, and a `marathi`
flag — which the fragment injects as
`<script type="application/json" id="chhBookingData">`.

Server-side serialisation with Jackson, rather than Thymeleaf's `th:inline="javascript"`,
because JavaScript inlining does not reliably serialise Java records: the output can
be a bare `[object Object]` or a partially-expanded structure depending on nesting.
Jackson's handling of records is well-defined, and the island is a plain data
document that any client can consume.

The fragment's `data` section therefore takes **no parameters** and reads
`${bookingDataJson}` from the model. Any controller embedding the widget must
populate the same attributes; `PublicPageController.addBookingWidgetModel()` and
`BookingPageController` both do.

One round-trip hydrates the whole widget, and a language switch needs no refetch —
the strings are already in the island.

---

## 11. Availability is computed, not stored

There is no `slot` table. Availability is derived per request from:

1. the doctor's `DoctorSchedule` rows for that weekday (start, end, slot length);
2. minus `DoctorLeave` covering the date;
3. minus `Appointment` rows in a blocking status for that doctor, date and slot.

Storing slots would mean generating thousands of rows per doctor per week and keeping
them consistent against every schedule edit, every leave entry and every cancellation.
Deriving them means a cancelled booking frees its slot immediately, with no
reconciliation job — verified by cancelling a booking and re-reading availability.

Booking is transactional, and a concurrent request for the same doctor/date/slot is
rejected with `409 SlotUnavailableException` and a message that tells the patient to
choose another time. Optimistic locking (`@Version`) on `appointment`, `doctor` and
`doctor_schedule` protects against two receptionists updating the same row from the
console.

---

## 12. Verification method

Manual clicking would not have found most of the bugs above, because the dominant
failure mode is *HTTP 200 with broken markup*. The suite that was actually run:

- **`<!DOCTYPE>` count per page.** Exactly one means the document rendered to
  completion. Two means an exception occurred mid-render and the error page was
  appended to committed output. This single check caught three separate template bugs.
- **Structural assertions**, not just status codes: 12 doctor cards on `/doctors`,
  12 unique detail links, 6 package links, correct avatar initials, package inclusion
  bullets split into real list items rather than mangled character chunks.
- **Every REST endpoint** for status and envelope shape, plus item counts.
- **The admin console** authenticated (all six views), anonymous (redirect to login),
  and the admin API without a session (403).
- **Negative paths**: unknown slugs → 404, malformed JSON → 400, past appointment
  date → 400, invalid mobile → 400 with a field-level message, duplicate slot → 409.
- **Locale**: `?lang=mr` renders Devanagari across navigation, widget and content.
- **Log hygiene**: zero `ERROR` lines during a clean run of the full sweep.
