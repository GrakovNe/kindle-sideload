# Interactive message editing — implementation plan

## Goal

Pressing an inline button **edits the pressed bot message in place** (text + keyboard),
so navigation feels like an interactive app. Anything that breaks the interaction chain —
a user text/file message, a command, or an async event (conversion finished, STK finished) —
still produces a **new** message. The whole behaviour sits behind a feature flag.

## Agreed decisions

- Target (edit vs send) is resolved **statelessly** from the incoming `Update`:
  `callbackQuery` on a text message → edit; everything else → send.
- Every callback query gets `AnswerCallbackQuery` (stop the client spinner).
- Any edit failure falls back to sending a new message.
- Stale-message presses (user scrolls up, taps an old menu) edit that old message —
  accepted as standard Telegram behaviour, no `last_message_id` state in DB.
- Flag: `telegram.interactiveMessageEditing`, default `false`.

## Phase 0 — Fix callback deduplication (blocker)

`Update.fetchUniqueIdentifier()` (`telegram/MessageDataExtractor.kt:12`) returns
`callbackQuery().message().messageId()` for callbacks. With `deduplicateMessages: true`
(`application.yml:37`) this means **the second button press on the same message is already
dropped as a duplicate today**. In-place editing makes every press happen on the same
message, so this must change.

- Use `callbackQuery().id()` (unique per press) as the dedup key for callbacks.
- Tests: `MessageDataExtractor` unit tests; two callbacks with different ids on the same
  message are both processed; the same callback id twice is skipped.

## Phase 1 — `ResponseSender`: edit + answer support

File: `telegram/sender/ResponseSender.kt`

- `editMessage(request: EditMessageText): Either<UnableSendResponse, Unit>`
  - treat `Bad Request: message is not modified` (400) as success — happens on double taps
    and re-entering the same screen;
  - on 429 / other errors return `Left` (caller falls back).
- `answerCallbackQuery(callbackQueryId: String)` — fire-and-forget, log errors only.
- Tests: mocked `TelegramBot`; not-modified → `Right`; failure → `Left`.

## Phase 2 — `MessageWithNavigationSender`: target resolution

File: `telegram/sender/MessageWithNavigationSender.kt`

- Private `fetchResponseTarget(origin: Update)`:
  - `Edit(chatId, messageId)` when the flag is on, `origin.callbackQuery() != null`,
    and the pressed message is a **text** message (`callbackQuery().message().text() != null`);
  - `Send(chatId)` otherwise.
- `sendResponse(origin: Update, ...)` builds `SendMessage` or `EditMessageText`
  (same text, inline keyboard, parse mode, link-preview options).
- Edit returns `Left` → log warn + fallback to `sendMessage`.
- The `sendResponse(chatId, ...)` overload stays **always-send** (async flows:
  `BookConversionFinishHandler`, `StkFinishHandler`, and `BookEmailSideloadRequestHandler`,
  whose button lives on a document message that cannot be edited via `EditMessageText`).
- Tests: decision matrix (callback+text → edit; callback+document → send; plain
  message/file → send; flag off → send; edit failure → send; not-modified → success).

## Phase 3 — `AnswerCallbackQuery` in the listener

File: `telegram/configuration/MessageListenersConfiguration.kt`

- After dispatching the update, if `update.callbackQuery() != null` →
  `responseSender.answerCallbackQuery(id)`.
- Tests: answered exactly once per callback update, never for plain messages.

## Phase 4 — Feature flag

- `ConfigurationProperties.interactiveMessageEditing: Boolean = false`
  (+ entry in `application.yml`).
- Gates the Phase 2 resolution only — Phases 0/3 are unconditional fixes.

## Phase 5 — Prompt navigation audit

Prompts sent with empty navigation render `ReplyKeyboardRemove` and, once they edit the
pressed message, would strip all buttons and leave the user stuck with only free text:

- `ConversationPromptRequestedEventHandler` ("send a book now")
- `UpdateStkEmailPromptRequestEventHandler` ("send your e-mail")
- `UserConfigurationUploadRequestHandler` ("send a configuration.zip")

→ add a "back to main screen" button to each prompt message.

Also audit all 29 `sendResponse` call sites: `ReplyingEventHandler` guarantees
success-XOR-failure, so one response per callback — verify no handler sends twice on one
event (current scan: all multi-call sites are success/failure branches or async events).

## Phase 6 — Rollout

1. Deploy with the flag off — behaviour unchanged (only dedup fix + AnswerCallbackQuery land).
2. Enable the flag; manual smoke: main screen, settings toggles (output type, auto-STK,
   debug), back navigation, prompts + cancel, "send to Kindle" button on a document message
   (must still send a new message), full conversion flow.
3. Watch logs for edit-fallback warnings.

## Acceptance criteria

- `./gradlew test lintKotlin` exits 0.
- New unit tests: dedup key, `ResponseSender.editMessage`, target-resolution matrix,
  `AnswerCallbackQuery` dispatch.
- No existing handler signatures change; async flows untouched.
