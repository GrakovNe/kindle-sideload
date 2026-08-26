# TEST_PLAN.md — kindle-sideload test strategy

Goal: raise and stabilise line coverage with a **test pyramid** (many unit tests → fewer
integration tests → a few acceptance tests), no network, no real services, no "tests that test mocks".

## Testing ground rules

1. **No network.** Telegram, GitHub, SMTP and the `fb2c` binary are all mocked / stubbed. The bot
   never polls in tests (the library only polls once you call `getUpdates()`, which we never do).
2. **H2 in PostgreSQL mode.** jOOQ + Flyway run against an in-memory H2
   (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`). No PostgreSQL needed.
3. **Localisation files.** `LocalizationService`/`EnumLocalizationService` read `locale/*.json`
   through `Path("locale")` (working dir = project root in the Gradle test JVM), falling back to the
   classpath `*.json`. Tests may therefore use the **real** en/ru resource files — no fixtures needed.
4. **Coverage.** JaCoCo (`./gradlew test jacocoTestReport`), read from the **report-level**
   `LINE` counter in `build/reports/jacoco/test/jacocoTestReport.xml` (the canonical instrumentable
   line total for the project, currently 2 254). Every new unit/integration test must raise the
   covered-line count; acceptance tests exercise business scenarios and may not add much mechanical
   coverage. (Do not sum `LINE` counters across the XML's nested levels — package/sourcefile/class/
   method counters repeat the same lines; summing them double-counts. An earlier draft of this plan
   did exactly that and reported inflated totals; the numbers below were re-measured per commit and
   corrected.)
5. **Isolation.** Tests use temp directories (`@TempDir`), unique user ids, and are order-independent.

## Pyramid levels used

- **Unit** — plain JUnit 5 + Mockito (or hand-rolled fakes). No Spring context. Targets stateless
  services, domain objects, converters, and pure functions.
- **Integration** — `@SpringBootTest` with H2 (shared base `TestDatabase`, which truncates all
  non-Flyway tables between tests), for services that genuinely need the jOOQ DAOs, or a single
  service wired with its collaborators. Verifies real behaviour (queries, mapping, `Either` flow)
  without a real DB.
- **Acceptance** — `@SpringBootTest`, full context, driving a real business scenario through the
  in-process event bus (`EventSender`) and the services, asserting the end-to-end outcome.

## Baseline

Before new tests, line coverage was **23.4 %** (528 / 2 256) — essentially only what the
`contextLoads` test exercises by instantiating every bean.

## Progress

Coverage is the JaCoCo **report-level** `LINE` counter (covered / instrumentable lines) re-measured
by checking out each block's commit and running the suite at that point — not a running aggregate.
Every unit/integration block strictly raises the covered-line count (the denominator drops by two
lines at Block I, because that block's null-language fix removes a dead branch).

| Step | Coverage | Covered / Total | Status |
| --- | --- | --- | --- |
| Baseline (contextLoads only) | 23.4 % | 528 / 2 256 | done |
| Block A — `common` | 28.9 % | 651 / 2 256 | committed |
| Block B — `converter` core | 34.3 % | 773 / 2 256 | committed |
| Block C — `converter.binary` | 37.4 % | 844 / 2 256 | committed |
| Block D — `converter.task` | 41.5 % | 937 / 2 256 | committed |
| Block E — `environment` | 44.0 % | 993 / 2 256 | committed |
| Block F — `events` core | 44.6 % | 1 007 / 2 256 | committed |
| Block G — `user` domain & config | 49.2 % | 1 109 / 2 256 | committed |
| Block H — `stk` (Send-to-Kindle) | 53.1 % | 1 199 / 2 256 | committed |
| Block I — `shelf` | 59.0 % | 1 330 / 2 254 | committed |
| Block J — `telegram` messaging & localisation | 67.0 % | 1 511 / 2 254 | committed |
| Block K — `telegram` state & message references | 68.4 % | 1 542 / 2 254 | committed |
| Block L — `telegram` event handlers | 82.3 % | 1 854 / 2 254 | committed |
| Blocks M + N — `metrics`, `telegram.logging` | 84.3 % | 1 901 / 2 254 | committed |
| AC-1…AC-6 — acceptance scenarios | 86.0 % | 1 939 / 2 254 | committed |

## Bugs found and fixed

1. **`LocalizedTemplateProvider` returned `"shelf_null"` for a null language**
   (Block I). `provideLocalized` checked only whether the base template exists
   and then unconditionally appended `"_$language"`, so a null language produced
   the template name `shelf_null`, which can never exist → the web view would
   fail to render. Fixed by short-circuiting to the base template when the
   language is null (the only caller, `ShelfEndpoint`, already defaults a
   missing user language to `"en"`; the fix makes the null branch correct for
   any future caller). Covered by
   `LocalizedTemplateProviderTest.uses the base template when the language is unknown`.

The uncovered surface is grouped into these blocks (per-package coverage at baseline in
parentheses). Each block gets its own pyramid.

### Block A — `common` utilities  (12.8% … 100.0%)
Real, testable logic:
- `CliRunner.runCli` — runs a real `/bin/bash` on the host (no network). Success → `Right`,
  failure → `Left`.
- `FileDownloadService.createSafeTempFile` — transliteration / sanitisation (cyrillic, whitespace,
  extension lower-casing, empty input, no-extension).
- `FileDownloadService.download` — retry loop via a mocked `RestTemplate` (success, transient
  failure then success, permanent failure → `null`).
- `ZipArchiveService.unpack` — real zip in/out (temp dirs).
- `PlatformService.fetchPlatformName` — the host here is 64-bit macOS → `Right("darwin-arm64")`;
  the Windows / 32-bit / unknown branches are documented as not host-reachable.
- `ButtonService.instance` — reflection lookup of real `Button` objects; qualified-name parsing in
  `Button` (`buildQualifiedName`, `fetchButtonName`, `fetchButtonPayload`), incl. payload `#` case.
- Validation framework: `ValidationService.validate` + sample rules (pass / fail / multiple rules).
- `MailSendingService.sendFile` — mocked `JavaMailSender`: success → `Right`, `MailException` →
  `Left(DELIVERY_ERROR)`.
- `IterableExtension.parallelMap`, `Boolean.ifTrue` — small but real.

### Block B — `converter` core  (14.0%)
- `ConverterService.convertAndCollect` — routes `.epub` → bypass, everything else → fb2 (mock both).
- `EpubBypassConverterService.convertAndCollect` — real temp environment; deploys the file, reports
  output; `UnableDeployEnvironment` when deploy fails.
- `Fb2ConverterService.convertAndCollect` — real temp environment + mocked `CliRunner`; covers
  success (with/without a config file, debug on/off verbose filtering), file-not-supported,
  unable-to-deploy, conversion failure → `UnableConvertFile`.
- `ConvertationFileValidationService` + `ConvertationFileValidationRules` — fb2/zip pass, other fail.

### Block C — `converter.binary` lifecycle  (0.0% … 100.0%)
- `ConverterBinaryProvider.provideBinaryFolder/Converter` — path building (temp dir).
- `ConverterBinaryReferenceService` — H2 integration: `updateLatestPublishedAt` (insert vs reuse
  latest), `fetchLatestPublishedAt`.
- `ArchivedBinaryUnpackService.unpack` — real zip → folder (success), corrupt zip →
  `Left(UNABLE_TO_UNPACK_BINARY)`.
- `GithubConverterBinaryFetchService` — mocked `RestTemplate`: `fetchLatestPublishedAt` (release /
  no-body); `fetchForPlatform` (match platform asset, no platform, no content).
- `ConverterBinaryUpdateService.checkAndUpdate` — branches: binaries absent → fetch; present +
  newer → refetch; present + same → `NO_NEWEST_VERSIONS`.
- `ConverterBinaryPeriodicUpdateTask` — delegates to update service.
- `GitHubRelease`/`Asset` — Jackson snake_case deserialization.

### Block D — `converter.task` queue  (14.0% … 21.4%)
- `ConvertationTaskService` — H2: `submitTask` persists ACTIVE, `fetchTasksForProcessing` returns
  active, `updateTask`.
- `ConvertSourceFilePeriodicService.convertSourceFiles` — with mocked download/converter/event
  sender: success → SUCCESS + success event; download miss → `UnableFetchFile` → FAILED; converter
  error → FAILED event with reason; thrown exception → `FatalError`.

### Block E — `environment`  (19.6% … 100.0%)
- `UserEnvironmentService` — real temp folders: `deployEnvironment` (user config present → unpack;
  absent → empty env; other error → `UnableDeployError`), `terminateEnvironment`,
  `provideEnvironmentFiles` (extension filter), `provideTemporaryEnvironmentsFolder`.
- `EnvironmentTerminationPeriodicTask.terminateOutdatedEnvironments` — old dirs trigger
  `UserEnvironmentUnnecessaryEvent`, fresh ones don't.
- `UserEnvironmentUnnecessaryHandler.onEvent` — terminates; null env id → `SKIPPED`.

### Block F — `events` core  (0.0% … 25.0%)
- `EventSender.sendEvent` — dispatches only to matching handlers, in parallel, collecting results.
- `EventHandler.handleEvent` / `ReplyingEventHandler` — success/failure reply hooks fire correctly.

### Block G — `user` domain & config  (0.0% … 100.0%)
- `UserService` — H2: `fetchOrCreateUser` (create / re-create with language), `fetchUser`,
  `fetchActiveUsers`, `fetchSuperUsers`.
- `UserPreferencesService` — H2: fetch-or-create defaults, `updateEmail` (valid/invalid),
  `updateOutputFormat`, `updateDebugMode`, `updateAutomaticStk`.
- `UserMessageReportService` — H2: `createReportEntry`.
- `UserConverterConfigurationService` — real temp asset path: fetch (user file / default / none),
  update (valid zip / non-zip → validation error / io error), remove (present / absent).
- `ConfigurationValidationService` + rules — zip pass, non-zip fail.
- `DefaultConfigurationAssetService.fetchDefaultConfiguration` — reads the real classpath asset zip,
  caches the temp file.

### Block H — `stk` (Send-to-Kindle)  (15.4% … 23.8%)
- `TransferEmailTaskService` — H2: `submitTask` (normal, and **daily-limit** → `StkLimitExhausted`
  when today's count ≥ limit), `fetchLatestForProcessing`, `updateTask`.
- `StkEmailPeriodicService.stkEmail` — mocked env/prefs/mail/task: no task → no-op; success →
  SUCCESS + `StkFinishedEvent(SUCCESS)`; no e-mail → `UserEmailAbsent`; mail error → FAILED.
- `ConvertationFinishedAutoStkEventHandler.onEvent` — failed event → SKIPPED; auto on + env id →
  submit; auto on + null env → SKIPPED; auto off → SKIPPED.

### Block I — `shelf`  (0.0% … 100.0%)
- `ShelfService` — H2: `fetchOrCreateShelf` (create once), `fetchUserId`, `fetchShelfContent`
  (joins items → env files), `fetchShelfLink`.
- `ShelfItemService` — H2: `attachToShelf` (new / already-exists), `terminateItem` (present /
  absent), `provideShelfItems` (active only).
- `ShelfContentItemConverter.apply`, `FileUrlConverter.toFileName` — name sanitisation.
- `LocalizedTemplateProvider.provideLocalized` — existing localized template → `shelf_ru`, missing
  → `shelf`, null language → base.
- `ConvertationFinishedShelfEventHandler.onEvent` — success + env → attach (and failure mapping);
  non-success → SKIPPED; null env → error.
- `UserShelfItemTerminatedEventHandler.onEvent` — present → PROCESSED; absent → `UnableTerminateItem`;
  null env → SKIPPED.
- `ShelfEndpoint` — `@WebMvcTest`-style: `index` renders (model attributes, template name),
  `downloadBinary` returns the file with the right headers.

### Block J — `telegram` messaging & localisation  (0.0% … 34.8%)
- `MessageDataExtractor` — `fetchUniqueIdentifier` (message / callback / random), `fetchUserId`
  (chat / from / callback / throw), `fetchLanguage` (message / callback / default).
- `ResponseSender.sendMessage` — mocked bot: ok → `Right`, not ok → `Left(UnableSendResponse)`.
- `MessageWithNavigationSender.sendResponse` — mocked localisation + sender: builds the
  `SendMessage` with inline keyboard, preview toggle, HTML parse mode; localisation failure →
  `Left(LocalizationError)`.
- `EnumLocalizationService.localize` — real `enums.json`/`enums_ru.json`: known value (en/ru),
  unknown → raw name, missing language → base file.
- `MessageLocalizationService` / `NavigationLocalizationService` — real templates: substitution,
  missing template → `TEMPLATE_NOT_FOUND`.
- `AdvertisingService.provideContent` — enabled creative + matching language + enabled template →
  block; any mismatch → `""`.
- `InstantFormatter.toMessage` — fixed UTC format.

### Block K — `telegram` state & references  (0.0% … 50.0%)
- `UserActivityStateService` — H2: `setCurrentState` (stores, null → ""), `fetchCurrentState`
  (latest / none).
- `MessageReferenceService` — H2: `markAsProcessed`, `fetchMessage` (present / absent).

### Block L — `telegram` handlers  (14.3% … 42.9%)
Focus on handlers with real branching (delegating "screen" handlers are covered via their
`processEvent`/`onEvent` + a representative `send*Response`):
- `ButtonPressedEventHandler.onEvent` (abstract base via a concrete subclass): matching button →
  process + set state + PROCESSED; non-matching → SKIPPED; process failure → Left.
- `InputRequiredEventHandler.onEvent`: required-button state set → process + clear state; no state /
  different state → SKIPPED.
- `UnprocessedIncomingEventService.handle` — document present → conversion handler; else main screen.
- `BookConversionRequestHandler.processEvent` — doc + ok size → submit; too large → `BookIsTooLarge`;
  no doc → no-op.
- `BookEmailSideloadRequestHandler.processEvent` — callback data → submit (payload parsing); no
  callback → `InternalError`; limit exhausted → `InternalError`.
- `StkEmailUpdateEventHandler.processEvent/onEvent` — text input → `updateEmail`; no text → SKIPPED;
  failure response path.
- `BookConversionFinishHandler.onEvent` + `sendSuccessfulResponse`/`sendFailureResponse` — success
  (auto-stk / empty output / normal) and failure branches.
- `StkFinishHandler.onEvent` + success/failure (AZW3 vs other).
- `UserConfigurationUploadSubmitHandler.processEvent` — doc present + ok → download+update; too
  large → `FileIsTooLarge`; no doc → `FileAbsent`; download null → `InternalError`.
- Representative settings handlers (`Enable/DisableAutoStkScreenEventHandler`,
  `*OutputTypeSettingsScreenEventHandler`, `*DebugModeSettingsScreenEventHandler`) — processEvent
  updates the right preference.

### Block M — `metrics`  (0.0% … 12.9%)
- `ActivityMetricService.aggregateMetrics` — mocked user/task services: correct counts are wired into
  `ActivityMetrics` for today/week/year.

### Block N — `telegram.logging`  (54.1%)
- `AsyncLoggerAppender.append` — below-threshold level ignored; above → queued and flushed to
  super-users after the timeout window (mocked bot).

## Acceptance scenarios (end-to-end, offline)

Driven through the real `EventSender` + services + H2:

- **AC-1 — Convert an FB2 book.** Submit a `ConvertationTask`, run the periodic worker with a
  mocked downloader/converter returning a `ConversionResult`; assert the task becomes SUCCESS, a
  `ConvertationFinishedEvent` is emitted, the shelf gains the item, and the user is replied to.
- **AC-2 — EPUB pass-through.** Same, but the converter is the bypass; output reported unchanged.
- **AC-3 — Auto-STK on success.** With `automaticStk` enabled, a finished event enqueues a
  `TransferEmailTask`; the STK worker mails it and emits `StkFinishedEvent(SUCCESS)`.
- **AC-4 — STK daily limit.** Exceeding the per-day limit yields `StkLimitExhausted` and the user
  gets a failure reply.
- **AC-5 — Environment TTL.** An outdated environment triggers a `UserEnvironmentUnnecessaryEvent`
  that terminates the environment and deactivates its shelf items.
- **AC-6 — Conversation prompt → book upload.** Pressing the prompt button, then uploading a file,
  submits a conversion task (state machine via `UserActivityStateService`).

## Per-block execution checklist

For each block, in order: (1) implement the pyramid, (2) run the block's tests green & isolated,
(3) run the full suite 5× to confirm stability (no flakes), (4) review & fix, (5) re-run 5×,
(6) commit with an English summary. Coverage must increase after each unit/integration block.
