# Chinche Care Navigator

The public assistant is a read-only, bilingual hospital information navigator. It
retrieves matching records from `src/main/resources/knowledge/hospital-knowledge.json`,
returns links to those records, and can look up public doctor profiles from the
existing catalogue. It never creates or changes an appointment; booking is handed
off to the existing booking widget and its live availability API.

## Five feature areas

1. **Bilingual RAG answers:** rank approved knowledge records by title, keywords
   and content; return the matching hospital-page links with the answer.
2. **Doctor and department navigation:** curated department aliases select a
   department, then the current active doctors are read from the application
   database. The assistant does not infer a diagnosis from symptoms.
3. **Booking co-pilot:** provide a doctor- or department-specific booking link.
   The booking page preselects the doctor/department; the existing widget still
   fetches live slots and asks the user to confirm the booking.
4. **Visit preparation:** share only reviewed hospital instructions. The sample
   lab guidance deliberately does not prescribe fasting or medication changes.
5. **Emergency-safe routing:** recognized urgent phrases bypass retrieval and
   the language model and display a prominent call-to-108 action and `/emergency`.

## Hospital content to review

`src/main/resources/knowledge/hospital-knowledge.json` is the editable content
file. Each record has English and Marathi titles/text, search keywords, an
internal source-page link, and an `approved` flag. Some records contain assumed
or sample wording. An `approved` flag only makes a record retrievable; it does
not verify its claims. Review every record (including those currently marked
`"approved": true`) before using the assistant with real patients. In the
`mysql` profile, unapproved records are excluded.
The sample corpus now covers common hours, payment, insurance, admission,
visiting, booking-change, records, diagnostic, pharmacy, tele-consultation,
home-care, parking, and feedback questions. These entries deliberately state
when a policy or service has not been verified; replace them with approved
hospital-specific facts rather than changing the approval flag alone.

Before enabling records for real patients:

1. Replace the sample contact/hours/preparation wording with hospital-approved
   content in both languages.
2. Check each `sourceUrl` points to the correct public page.
3. Set `"approved": true` only after a hospital owner has reviewed that record.
4. Add department aliases only for specialties that actually exist in the
   catalogue. `specialitySlug` must match a live speciality slug.
5. Keep clinical diagnosis, medication, treatment, and patient-specific advice
   out of this file.

No separate patient profile, booking record, report, or chat history is indexed.
The API limits a message to 500 characters and does not persist conversations.
The optional language model is restricted to a local Ollama endpoint; no
third-party model API is called.

The current configured model is `qwen2.5:1.5b`, and generation defaults to off.
It is a small local model intended for a lightweight demonstration, not a
high-concurrency production inference service. The Ollama model receives text
retrieved from the JSON knowledge base; it does not search the internet or read
PDF files. A plain `mvn spring-boot:run` process does not automatically load
`.env`. Docker Compose reads `.env`, and the repository's VS Code launch profile
loads it with `envFile`; a terminal launch must receive environment variables
from that terminal session. Startup logs state whether Ollama is enabled and
which model is configured. If Ollama is unreachable, the app logs the failure
and falls back to retrieved text.

Retrieval is an in-process lexical ranker (weighted title, keyword, and content
matches with inverse-document-frequency weighting), not an embeddings/vector
database. This keeps the first version offline and easy to review. A vector
index can be added behind the retrieval boundary if the approved corpus grows
substantially.

## Live doctor schedule questions

Questions about a named doctor's availability, today's open slots, attendance
breaks, or appointment totals are answered from the MySQL database at request
time, not from the sample knowledge file. Availability uses the same slot
calculator as the booking page. Break times and appointment totals are only
shown to signed-in Admin or Receptionist users; public visitors are asked to
sign in. The application does not currently link a doctor login to a doctor
profile, so staff should include the doctor's full name in the question.
Romanized Marathi phrases are recognized for common questions, and a generic
specialty question (for example, dermatologist availability) falls back to the
department catalogue rather than being mistaken for a missing doctor's name.

Questions asking which doctors are listed for dermatology use the active
specialty and doctor catalogue directly, even when no knowledge document matches.
Emergency-contact questions use the configured contact number. The assistant
also explains its supported tasks without requiring a knowledge-base match.
These deterministic replies do not require Ollama; model generation remains
optional for retrieved hospital information.

The appointment total is an aggregate count only. It includes pending,
confirmed, checked-in, and completed appointments for today, and excludes
cancelled, rescheduled, and no-show appointments. It is not a count of unique
people. Patient names, symptoms, booking references, and staff notes are never
sent to Ollama.

## Run locally

The deterministic cited-answer mode works without an AI model:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
mvn spring-boot:run
```

For local generative answers, install/run Ollama locally, pull a model, then:

```powershell
ollama pull qwen2.5:1.5b
$env:AI_OLLAMA_ENABLED = "true"
$env:AI_OLLAMA_BASE_URL = "http://localhost:11434"
$env:AI_OLLAMA_MODEL = "qwen2.5:1.5b"
$env:AI_OLLAMA_TIMEOUT_SECONDS = "120"
mvn spring-boot:run
```

The model is optional. If it is disabled or unavailable, the assistant returns
the retrieved hospital text with its source links instead. If enabled, the
configured endpoint must use HTTP on a local host (`localhost`, `ollama`, or
`host.docker.internal`); remote model hosts are rejected.

For Docker, set `AI_OLLAMA_ENABLED=true` in `.env`, then run:

```powershell
docker compose --profile full --profile ai up -d --build
docker compose exec ollama ollama pull qwen2.5:1.5b
```

Ollama is opt-in and its model data persists in the `chinche-ollama-data` volume.
Docker Compose allows 120 seconds for model startup by default. Increase
`AI_OLLAMA_TIMEOUT_SECONDS` in `.env` if the first response needs more time.
For a production/MySQL setup, do not set `AI_ALLOW_UNVERIFIED_KNOWLEDGE=true`;
approve the hospital content records instead.
For a local Docker demonstration only, set `AI_ALLOW_UNVERIFIED_KNOWLEDGE=true`
in `.env` to show the clearly labelled sample answers. Do not use that override
for real patients.

## Run from VS Code against local MySQL

Open the `chinche-hospital` project folder itself in VS Code, not its parent.
Keep MySQL running on `localhost:3306`, set its connection values in the
ignored `.env` file, and install the local Ollama model once with
`ollama pull qwen2.5:1.5b`. Then choose **Chinche Hospital - Local MySQL +
Ollama** in Run and Debug and press F5. This launch uses the `mysql` profile
and validates the existing database schema without changing it. The model
must be available at `http://localhost:11434`.

Open `http://localhost:8080` to ask public availability questions. To ask for
break times or appointment totals, sign in first at `/admin/login` with an
existing staff account.

## API and verification

`POST /api/v1/assistant/chat` accepts:

```json
{"message":"I need a skin specialist","language":"en"}
```

The response contains the answer, source links, optional current doctor cards,
an optional booking handoff URL, and an emergency flag. The API does not expose
appointment slots or accept booking mutations.

Run the focused checks with `mvn test`. Unit tests cover knowledge approval and
retrieval, citations, speciality/doctor booking handoff, preparation guidance,
unsupported questions, and emergency routing without invoking the model.
