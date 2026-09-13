# REST API reference — `/api/v1`

Every response uses one envelope. Clients should branch on `success`, never on the
HTTP status alone, and read `errors` for field-level messages.

```json
{
  "success": true,
  "message": "OK",
  "data": { },
  "timestamp": "2026-09-13T13:37:33.021Z"
}
```

```json
{
  "success": false,
  "message": "Enter a valid 10-digit Indian mobile number.",
  "errors": { "patientPhone": "Enter a valid 10-digit Indian mobile number." },
  "timestamp": "2026-09-13T13:37:33.021Z"
}
```

Status codes in use: `200` read, `201` created, `400` validation or malformed body,
`403` authenticated but not permitted, `404` unknown slug or reference,
`409` slot already taken.

Swagger UI: `/swagger-ui/index.html` · OpenAPI document: `/v3/api-docs`
(both administrator-only).

---

## Catalogue

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/specialities` | `SpecialityDto[]` — 18 active departments |
| GET | `/api/v1/specialities/{slug}` | `SpecialityDto` with a live `doctorCount` |
| GET | `/api/v1/procedures` | `ProcedureDto[]` |
| GET | `/api/v1/conditions` | `ConditionDto[]` |
| GET | `/api/v1/health-packages` | `HealthPackageDto[]` |
| GET | `/api/v1/health-packages/{slug}` | `HealthPackageDto` |
| GET | `/api/v1/lab-tests` | `LabTestDto[]` |

`HealthPackageDto.includes` is already a `List<String>` — the entity stores one
delimited `TEXT` column and the DTO splits it server-side.

## Doctors

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/doctors` | `PageDto<DoctorDto>` |
| GET | `/api/v1/doctors/{slug}` | `DoctorDto` |
| GET | `/api/v1/doctors/featured` | `DoctorDto[]` — landing-page panel |

Search parameters on `/api/v1/doctors`:

| Param | Type | Default | Notes |
|---|---|---|---|
| `q` | string | — | matches name, speciality and expertise |
| `specialityId` | long | — | facet |
| `facilityId` | long | — | facet |
| `teleconsultOnly` | boolean | `false` | video-capable doctors only |
| `page` | int | `0` | zero-based |
| `size` | int | `12` | |
| `sort` | string | `sortOrder` | `name` · `experience` · `fee` · `rating` |

`PageDto` is `{ content, page, size, totalElements, totalPages, first, last }`.
Note `page`, not Spring Data's `number` — templates must not guess which one a
given object exposes.

## Availability

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/availability/doctors/{doctorId}?days=16&visitMode=IN_HOSPITAL` | `DayAvailabilityDto[]` |
| GET | `/api/v1/availability/doctors/{doctorId}/date/{date}` | one `DayAvailabilityDto` |
| GET | `/api/v1/availability/window` | earliest and latest bookable dates |

```json
{
  "date": "2026-09-14",
  "dayName": "Mon",
  "clinicOpen": true,
  "reason": null,
  "slots": [
    { "start": "09:00:00", "end": "09:20:00", "available": true, "period": "MORNING" }
  ]
}
```

Availability is **derived**, not stored: the doctor's weekly schedule for that
weekday, minus approved leave, minus already-booked appointments. A closed day
returns `clinicOpen: false` with a human-readable `reason` so the widget can explain
itself instead of showing an empty grid.

## Bookings

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/v1/bookings` | `BookingRequest` | `201` + `BookingConfirmationDto` |
| GET | `/api/v1/bookings/{reference}` | — | `BookingConfirmationDto` |
| GET | `/api/v1/bookings/by-phone/{phone}` | — | `BookingConfirmationDto[]` |
| POST | `/api/v1/bookings/{reference}/cancel` | — | updated `BookingConfirmationDto` |

`BookingRequest`:

| Field | Required | Validation |
|---|---|---|
| `serviceType` | yes | one of `OPD_CONSULTATION`, `TELE_CONSULTATION`, `HEALTH_CHECKUP`, `LAB_TEST`, `SECOND_OPINION`, `HOME_CARE`, `SURGERY_PLANNING` |
| `visitMode` | yes | `IN_HOSPITAL`, `VIDEO`, `HOME_VISIT` |
| `appointmentDate` | yes | today or later |
| `slotStart` | yes | must be a slot the doctor actually offers |
| `patientName` | yes | ≤ 160 chars |
| `patientPhone` | yes | `^[6-9]\d{9}$` — 10-digit Indian mobile |
| `patientEmail` | no | RFC-5322 shape, ≤ 160 chars |
| `doctorId` | for consultations | — |
| `healthPackageId` | for `HEALTH_CHECKUP` | — |
| `specialityId`, `facilityId` | no | used for routing and reporting |
| `patientGender`, `patientAge`, `patientDob` | no | age 0–120 |
| `address`, `city`, `pincode` | no | `pincode` is `^\d{6}$` |
| `symptoms`, `allergies` | no | ≤ 1000 / ≤ 400 chars |
| `consentSms`, `consentEmail` | no | communication preferences |
| `paymentMode` | no | ≤ 30 chars |

`BookingConfirmationDto` carries everything the confirmation screen and the SMS
need: `referenceNo`, `status`, patient and doctor names, service and visit-mode
labels, facility name/address/phone, date, slot start and end, `amountPayable`,
`paymentMode`, a `preparationNote` (fasting guidance for check-ups, arrival advice
for OPD) and a `trackingUrl`.

Status lifecycle: `PENDING → CONFIRMED → CHECKED_IN → COMPLETED`, with
`CANCELLED`, `RESCHEDULED` and `NO_SHOW` as terminal alternatives.

## Enquiries

| Method | Path | Body | Returns |
|---|---|---|---|
| POST | `/api/v1/enquiries` | `EnquiryRequest` | `201` + `{ id, status }` |

Used by the contact form, the callback request, the report-download form and the
blood-donor registration. `enquiryType` accepts `GENERAL`, `CALLBACK`, `FEEDBACK`,
`COMPLAINT`, `INSURANCE`, `CAREER`.

## Search

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/search/suggest?q=knee&limit=8` | `SearchSuggestionDto[]` |

```json
{
  "type": "PROCEDURE",
  "id": "3",
  "label": "Total Knee Replacement",
  "sublabel": "Orthopaedics & Joint Replacement",
  "url": "/procedures/total-knee-replacement"
}
```

`type` is one of `DOCTOR`, `SPECIALITY`, `PROCEDURE`, `CONDITION`, `PACKAGE`.
Each suggestion carries a ready-made `url`, so the autocomplete never builds routes
client-side.

## Content

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/content/hero` | landing-page carousel slides |
| GET | `/api/v1/content/announcements` | latest published announcements |
| GET | `/api/v1/content/blogs` | latest published articles |
| GET | `/api/v1/content/stories` | featured patient stories |
| GET | `/api/v1/content/testimonials` | patient feedback |
| GET | `/api/v1/content/awards` | accreditation and awards |
| GET | `/api/v1/content/quick-links?group=HERO_SERVICES` | quick-service tiles |

## Facilities

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/facilities` | `FacilityDto[]` |
| GET | `/api/v1/facilities/{slug}` | `FacilityDto` |

## Admin — requires `ROLE_ADMIN` or `ROLE_RECEPTIONIST`

| Method | Path | Returns |
|---|---|---|
| GET | `/api/v1/admin/stats` | dashboard counters |
| GET | `/api/v1/admin/stats/trend?days=14` | bookings per day, zero-filled |
| GET | `/api/v1/admin/stats/specialities` | bookings grouped by department |

`/api/v1/admin/stats` response:

```json
{
  "appointmentsToday": 1, "appointmentsTomorrow": 0,
  "pending": 1, "confirmed": 0, "completed": 0, "cancelled": 0,
  "totalAppointments": 1, "last24h": 1,
  "doctors": 18, "specialities": 18, "facilities": 4, "openEnquiries": 0
}
```

Anonymous callers receive `403` with the standard envelope.
