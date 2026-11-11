# Project Files: Local, GitHub, Docker, Runtime

## Quick model

Think of the project in four layers:

1. `src/main` is application code and resources. Maven packages it into the runnable JAR.
2. `src/test` is test code. It belongs in Git and runs during verification, but it is not part of the production JAR.
3. Root files describe how to build, configure, and run the app. They are not all copied into the JAR.
4. `.env`, `target`, logs, and local editor state are machine-specific or generated; they should not be committed or shipped as application data.

## File map

| Path | What it is | GitHub | App JAR / image | Used by |
|---|---|---:|---:|---|
| `src/main/java/` | Spring controllers, services, entities, security, assistant | Yes | Yes | Local run and deployed app |
| `src/main/resources/templates/` | Thymeleaf HTML templates | Yes | Yes | App runtime |
| `src/main/resources/static/` | CSS, JavaScript, icons, images | Yes | Yes | Browser at runtime |
| `src/main/resources/knowledge/` | Assistant's curated JSON knowledge | Yes | Yes | Assistant retrieval at runtime |
| `src/main/resources/application.yml` | Common Spring settings and safe defaults | Yes | Yes | Every profile |
| `src/main/resources/application-dev.yml` | H2 database and demo-data profile | Yes | Yes | Only when `dev` profile is active |
| `src/main/resources/application-mysql.yml` | MySQL connection and production-shaped settings | Yes | Yes | Only when `mysql` profile is active |
| `src/test/` | Unit and integration tests | Yes | No | `mvn test` / CI only |
| `pom.xml` | Dependencies, Java target, Maven build | Yes | No | Build time; runtime dependencies are bundled into the executable JAR |
| `Dockerfile` | Instructions to build the production container image | Yes | No | Docker image build |
| `docker-compose.yml` | Local MySQL, Adminer, Mailpit, and optional app/Ollama services | Yes | No | Docker Compose only |
| `docs/sql/schema-mysql.sql` | Initial MySQL schema script | Yes | No | MySQL container's first initialization of an empty data volume |
| `docs/sql/migrations/` | One-time SQL patches for existing databases; no Flyway runner currently applies them | Yes | No | DBA/manual migration step before schema validation |
| `.env.example` | Safe template showing names of environment variables | Yes | No | Human copies it to `.env`; Compose does not read the example file itself |
| `.env` | Local passwords and environment values | **No** | **No** | Docker Compose and VS Code's launch profile |
| `.vscode/launch.json` | Local VS Code Run and Debug configuration; its `envFile` points to `.env` | No, ignored | No | VS Code only |
| `.gitignore` | Tells Git which files not to commit | Yes | No | Git |
| `.dockerignore` | Tells Docker which files not to send in the build context | Yes | No | Docker build |
| `target/` | Maven-generated classes, test reports, and JAR | No, ignored | No, rebuilt | Local Maven output only |
| `*.log`, `logs/` | Runtime logs | No, ignored | No | Local diagnostics / server log destination |

## Demo seeding

`DataSeeder` writes demo rows to the selected database; after that, the app reads
them through the normal repositories. Seeding is disabled by default in the
common, `dev`, and `mysql` configuration. Set `SEED_DEMO_DATA=true` to opt in on
a disposable database. The seeder only writes the catalogue if `speciality` is
empty; if it is already populated, it skips the catalogue and only ensures the
demo admin account. Disabling seeding does not delete rows inserted by an older
run. The source methods are in
`src/main/java/in/chinche/hospital/service/seeding/DemoDataSeeder.java`.

Demo rows are database records, not a second runtime source. To use only
database-managed content, leave `SEED_DEMO_DATA=false` and manage reviewed
catalogue rows through the admin features or approved database imports. Do not
mark invented doctor, schedule, price, service, or contact details as real.

## Demo seeding

`DataSeeder` writes demo rows to the selected database; after that, the app reads
them through the normal repositories. Seeding is disabled by default in the
common, `dev`, and `mysql` configuration. Set `SEED_DEMO_DATA=true` to opt in on
a disposable database. The seeder only writes the catalogue if `speciality` is
empty; if it is already populated, it skips the catalogue and only ensures the
demo admin account. Disabling seeding does not delete rows inserted by an older
run. The source methods are in
`src/main/java/in/chinche/hospital/service/seeding/DemoDataSeeder.java`.

Demo rows are database records, not a second runtime source. To use only
database-managed content, leave `SEED_DEMO_DATA=false` and manage reviewed
catalogue rows through the admin features or approved database imports. Do not
mark invented doctor, schedule, price, service, or contact details as real.

## How to tell what runs where

- **Local VS Code F5:** `.vscode/launch.json` selects the main class and `mysql` profile. Its `envFile` entry loads `.env` for that launch.
- **Local terminal Maven:** when launched with `chinche-hospital` as the working directory, Spring Boot imports the optional `.env` as properties. Existing terminal environment variables take precedence. If Maven is launched from the parent folder with `-f`, the relative `.env` import points at the parent; run from the project folder or set variables in that terminal. The active profile defaults to `mysql` unless overridden.
- **Docker Compose:** Compose reads `.env` for variable substitution and passes selected values to containers. The app container connects to MySQL and Mailpit by their Compose service names, such as `mysql` and `mailpit`.
- **Docker image:** `Dockerfile` copies `pom.xml` and `src/` into a temporary Maven build stage, then copies only the built JAR into the final JRE image. Compose files, `.env`, VS Code settings, docs, and tests are not runtime files in the final image.
- **GitHub:** `.gitignore` is the commit boundary; `.dockerignore` is a separate Docker build-context boundary. A file can be committed but not copied into the image, such as `docs/` or `docker-compose.yml`.

Do not open or paste `.env` into chat: it can contain database, admin, or model-service credentials. Inspect the variable names in `.env.example`; check whether `.env` exists without printing its contents.

`schema-mysql.sql` is a fresh-database bootstrap, not a migration runner. The
current VS Code MySQL launch uses Hibernate `validate`, so an existing database
must have each required column before F5. The emergency-reschedule feature adds
`appointment.reschedule_reason`; its one-time patch is
`docs/sql/migrations/V20261104_01__appointment_reschedule_reason.sql`. Run that
patch only if the column is absent. Do not execute it against a database where
the column already exists.

Doctor logins add nullable, unique `app_user.doctor_id`; the matching one-time
patch is `docs/sql/migrations/V20261104_02__doctor_account_link.sql`. Run it
only if that column is absent. The admin Doctors panel can then create a
doctor-role account linked to exactly one doctor. Doctor accounts are not demo
seeded; the admin sets the initial password and must share it through a secure
channel. `ROLE_DOCTOR` can use `/doctor/**` only; `/admin/**` remains limited to
admin/receptionist roles. The doctor portal resolves its data scope from the
authenticated account's doctor link, never a request-supplied doctor ID.

The inspected local MySQL database's `doctor_attendance_break` FK used `NO
ACTION`, while the fresh schema declares cascade deletion. The row-preserving
FK-only patch is `docs/sql/migrations/V20261104_03__attendance_break_cascade.sql`.
Apply it manually if the current FK rule is still `NO ACTION`; it changes no
attendance or break rows.

## Current AI configuration

The configured Ollama model is `qwen2.5:1.5b`. `AI_OLLAMA_ENABLED` defaults to `false` in `application.yml`. `.env.example` is only a template; copy it to the ignored `.env` and add local values there. Spring Boot imports `.env` for project-root terminal launches, VS Code's launch configuration loads it as environment variables, and Docker Compose reads it for substitutions. Ollama must separately be running, and the named model must already be pulled. The application startup log now says whether Ollama is enabled and which model is selected.

If Ollama is disabled or unreachable, assistant answers use the JSON knowledge retrieval and extractive fallback. If it is enabled, it receives retrieved text as context; it does not independently browse the internet or search the whole hospital website.

## PDF status and safe evaluation

There is currently **no production PDF ingestion path**. PDFBox is used to generate booking-receipt PDFs, and a test extracts text from that generated receipt. Downloading PDFs into the repository will not make the assistant read them.

A useful PDF evaluation requires a deliberate pipeline: a test-only corpus, PDF text extraction, chunking, retrieval, citations, and expected-answer tests. Scanned/image-only PDFs also need OCR. Start with a small set of current official sources whose reuse terms permit testing, and record each source URL, title, owner, publication/update date, and license. Do not copy hundreds of random hospitals' PDFs into the production knowledge base: their local policies differ, they may be copyrighted or outdated, and a larger noisy corpus can make retrieval worse. Keep raw source documents out of `src/main/resources`; ship reviewed, concise, hospital-approved content or permitted source references instead.

The current knowledge base is JSON, not a PDF folder. Its `approved` flag means staff reviewed the hospital-specific claim; a public health source is labelled as general guidance instead of being presented as Chinche Hospital policy.

## Booking email test

Booking emails go to the email address on the booking's patient record. Local
Mailpit captures mail for inspection at `http://localhost:8025`; it does not
deliver to the real recipient. It needs to be running on SMTP port `1025`.

For real Gmail delivery from a local VS Code or project-root Maven run, put the
following settings in the ignored `.env` and replace the sender values locally:

```properties
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_USER=your-sender@gmail.com
MAIL_FROM=your-sender@gmail.com
MAIL_PASSWORD=your-Google-App-Password
```

Use a Google App Password, not the normal account password. Do not send that
secret in chat or commit `.env`. For a test to a personal inbox, create a test
booking using that inbox as the patient email; this creates a booking record in
the selected database. Use the `dev` profile for an isolated H2 database. The
project does not expose a public test-mail endpoint.
