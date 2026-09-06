# kindle-sideload

> Unfortunately, Kindle reading devices do not support FB2, and most EPUB files from bookstores are converted by Amazon services with significant losses
>
> I created this service to solve both problems at once:
> now you can quickly get quality converted books on Kindle, with preserved page layout, margins, indents, and fonts
>
> I tried to make the service's use not too different from using the official Kindle app from Google Play or App Store, and I hope I succeeded
>
> The service will remain free and available to all users without restrictions as long as possible
>
> The project uses the <a href="https://github.com/rupor-github/fb2converter">fb2converter</a> under the GPL-3.0, and I would like to thank the author for his work
> Please use the service for good and do not deprive authors of the opportunity to earn from their books

---

## What it is

`kindle-sideload` is a Telegram bot that takes an **FB2** (or **EPUB**) file, converts it into a
Kindle-ready format (**EPUB / KEPUB / AZW3**) using the external `fb2converter` binary, and lets the
user obtain the result either through a web "shelf" (download in the Kindle browser) or by sending it
to an e-mail (Amazon's *Send-to-Kindle* / STK mechanism).

## Tech stack

- **Language / build**: Kotlin 2.4, Gradle (Kotlin DSL), Java 25 toolchain.
- **Framework**: Spring Boot 4.1 — `spring-boot-starter-web`, `jooq`, `mail`, `thymeleaf`, `flyway`, `restclient`.
- **Bot**: `java-telegram-bot-api` (pengrad).
- **Functionality / error handling**: Arrow (`Either`) for total, exception-free results.
- **Persistence**: jOOQ (code-generated from the live schema) + PostgreSQL (Flyway migrations).
- **Conversion**: the external `fb2c` binary, executed through a shell (`/bin/bash -c`).
- **Zip handling**: zip4j (default configuration + converter-binary distribution archives).
- **Text / localization**: commons-text (`StringSubstitutor`), ICU4J (transliteration), Jackson.
- **Async**: kotlinx-coroutines.

## Features

- **Telegram UX** — a menu-driven bot (main screen, settings, project info, metrics) with inline
  buttons, multi-step "prompts" (e.g. "send a file now") and per-user language (en/ru).
- **Book conversion** — upload an FB2/EPUB; an EPUB is *passed through* unchanged, an FB2 is run
  through `fb2converter`. Output format is user-selectable (EPUB / KEPUB / AZW3).
- **Per-user converter configuration** — the user can upload/replace/remove a `configuration.zip`
  (a `fb2converter` profile). A default configuration is shipped as a classpath asset.
- **Temporary per-conversion environments** — each conversion runs in an isolated temp folder
  (unpacked user config + the book), which is terminated after a TTL and whose output files become
  downloadable.
- **Web shelf** — a Thymeleaf page (`/{shortId}`) lists a user's finished books and serves file
  downloads (`/download/{environmentId}/{fileUrl}`) with sanitized file names.
- **Send-to-Kindle (STK)** — finished books can be mailed to a user-configured address; a per-user
  **daily limit** applies, and an **auto-STK** option mails the book as soon as conversion finishes.
- **Self-updating converter** — the bot polls GitHub releases of `fb2converter`, detects the host
  platform, and downloads/unpacks a newer binary.
- **Activity metrics** — a super-user screen with today/week/year counts of users and conversions.
- **Localization & ads** — message/button/enum templates in `locale/*.json` (en/ru), plus an
  optional per-message advertisement block.
- **Operational logging** — errors are batched and forwarded to super users via Telegram
  (`AsyncLoggerAppender`).

## Architecture

### Layering

The codebase follows a layered, feature-oriented layout under `org.grakovne.sideload.kindle`:

- **`telegram`** — the presentation/adapter layer. `MessageListenersConfiguration` is the entry
  point: it registers the bot listener and turns every `Update` into a `ButtonPressedEvent`. Screen
  handlers (`handlers/screens/...`) react to that event or to internal events.
- **`events`** — a small **in-process publish/subscribe bus**. `EventSender` broadcasts an `Event` to
  all registered `EventHandler`s whose `acceptableEvents()` matches the event's `EventType`. This is
  what decouples the "a book was converted" moment from its many reactions.
- **Domain features** (each with its own `service` / `dao` / `domain` / `configuration`):
  - **`converter`** — the conversion pipeline plus the binary lifecycle (`binary/...`) and the queued
    conversion task (`task/...`).
  - **`environment`** — temporary per-conversion folders (deploy / terminate / TTL).
  - **`stk`** — Send-to-Kindle: the e-mail task queue, the periodic worker, and the auto-STK event
    handler.
  - **`shelf`** — the web shelf: domain model, services, the REST/`@Controller` endpoint, and the
    event handlers that attach/terminate shelf items.
  - **`user`** — user reference, per-user preferences (output format, debug, e-mail, auto-STK),
    converter configuration assets, and message reports.
  - **`metrics`** — activity aggregation over users and conversion tasks.
- **`common`** — cross-cutting utilities: `CliRunner`, `FileDownloadService` (with retry),
  `ZipArchiveService`, `PlatformService`, the `ValidationService`/`ValidationRule` framework,
  `ButtonService` (reflection-based button lookup) and the `Message`/`Button` domain.

### Key patterns

- **`Either<L, R>` everywhere** — services return `Either<SomeError, Result>` instead of throwing;
  callers chain with `fold` / `map` / `flatMap`. `EventProcessingError` is the marker that all
  handler-level errors implement.
- **Event bus (in-process)** — `EventSender` + `EventHandler<E : Event, T : EventProcessingError>`.
  `ReplyingEventHandler` adds the "send a success/failure reply to Telegram" behaviour on top of the
  base handler.
- **Two kinds of Telegram handlers**:
  - `ButtonPressedEventHandler` — reacts to a specific *button* being pressed
    (`getOperatingButtons()`), and records the user's activity state.
  - `InputRequiredEventHandler` — reacts to free-text/file input *after* the user pressed a known
    prompt button (tracked via `UserActivityStateService`).
- **Rule-based validation** — `ValidationService<T, E>` runs a `List<ValidationRule<T, E>>` in
  parallel and sequences the `Either`s. Used for book files, configuration archives, and e-mail
  addresses.
- **Periodic tasks** (`@Scheduled`): conversion queue worker, STK e-mail worker, environment TTL
  sweeper, and the hourly converter-binary updater.
- **Reflection-driven localization** — `LocalizationService` reflects over a `Message`'s
  properties to collect template variables and fills the matching `locale/*.json` template.

### The core business flow

```
Telegram Update
   └─> MessageListenersConfiguration  (dedupe, resolve/create user, log raw message)
         └─> ButtonPressedEvent  ──(EventSender)──>  screen handler
               · "Convert Book" button  ──> ConversationPromptRequestedEventHandler
                     · user sends FB2/EPUB  ──> BookConversionRequestHandler
                            └─> ConvertationTaskService.submitTask (DB queue, ACTIVE)

ConvertSourceFilePeriodicService (every 100ms)
   └─> downloads file, ConverterService.convertAndCollect
         · EPUB  ─> EpubBypassConverterService (pass-through)
         · FB2   ─> Fb2ConverterService (CliRunner runs fb2c)
   └─> ConvertationFinishedEvent  ──(EventSender)──>
         · BookConversionFinishHandler  (reply to user, send docs)
         · ConvertationFinishedShelfEventHandler (attach to user's shelf)
         · ConvertationFinishedAutoStkEventHandler (queue STK if auto-enabled)

StkEmailPeriodicService (every 5s)
   └─> MailSendingService.sendFile  ─> StkFinishedEvent  ─> StkFinishHandler (reply to user)

EnvironmentTerminationPeriodicTask (every 5s)
   └─> UserEnvironmentUnnecessaryEvent  ─> UserEnvironmentUnnecessaryHandler + UserShelfItemTerminatedEventHandler
```

The **shelf** is the durable side-effect of a successful conversion: `ShelfService.fetchShelfLink`
yields a stable `/{shortId}` URL that the conversion-finished message shares, and the endpoint
serves the (still-present) converted files until their environment expires.

### Persistence

PostgreSQL, migrated by Flyway (`src/main/resources/db/migration/V1…V11`). Tables:
`user`, `user_message_report`, `converter_binary_reference`, `user_activity_state`,
`convertation_task`, `message_reference`, `user_preferences`, `transfer_email_task`,
`shelf_reference`, `shelf_item`.

jOOQ classes are code-generated at build time straight from the Flyway migration scripts — the
`jooq { ... }` block in `build.gradle.kts` points jOOQ's `DDLDatabase` at `db/migration/*.sql`
with `sort = flyway`, so `db/migration` is the single source of truth and no database is needed
to generate. The output lands in `build/generated/jooq/`, is added to the `compileKotlin` task
rather than to a source set (which keeps it out of kotlinter), and is not committed. Each table
has a hand-written `*Dao` (`dslContext`-backed) that services depend on instead of the generated
types directly.

### External integrations (must be mocked in tests)

- **Telegram** — the `TelegramBot` bean and the bot's outgoing calls.
- **GitHub** — `RestTemplate` calls to fetch release metadata and the binary archive.
- **SMTP** — `JavaMailSender` for STK e-mails.
- **The `fb2c` binary** — executed via `CliRunner` (`ProcessBuilder`).
- **Filesystem** — temporary environments, config assets, downloaded files.

## Testing

See [TEST_PLAN.md](TEST_PLAN.md) for the coverage strategy and test pyramid.

- Tests run against an **embedded PostgreSQL** (started via `io.zonky.test:embedded-postgres`,
  Flyway migrations still apply) — no external database or Docker needed.
- All external integrations above are **mocked**; no test performs a real network call.
- Line coverage is reported by **JaCoCo** (`./gradlew test jacocoTestReport`),
  report at `build/reports/jacoco/test/index.html`.

## Running

```
./gradlew bootRun          # requires PostgreSQL + a real telegram token in application.yml
./gradlew test             # runs the (offline) test suite
```

> ⚠️ `src/main/resources/application.yml` contains real credentials (Telegram token, SMTP, DB).
> Do not commit changes that make these worse; prefer environment overrides in production.
