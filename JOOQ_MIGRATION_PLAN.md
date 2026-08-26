# План миграции: JPA → jOOQ

## Цель
Убрать `spring-boot-starter-data-jpa` и `kotlin("plugin.jpa")`, заменить JPA-репозитории на jOOQ DSL с автогенерацией кода из схемы Flyway.

## Оценка
- Объём: ~40–50 небольших файлов, механическая замена, бизнес-логика не меняется.
- Срок: 1–2 дня + полдня на доводку тестов.

---

## Этап 0. Пилот (1 день) — `ConvertationTaskRepository`

Самый частый репозиторий (3 derived-запроса). Цель — оценить реальные издержки на одном срезе.

1. Добавить в `build.gradle.kts`:
   ```kotlin
   implementation("org.jooq:jooq")
   implementation("org.jooq:kotlin")
   // + конфигурация jooqGen и task generateJooq
   ```
2. Настроить `jooq-config.xml` (или inline в gradle):
   - JDBC URL + credentials (env vars / `local.properties`, не коммитить).
   - `defaultNameVersioning: NONE` (имена таблиц/колонок уже совпадают со схемой).
   - `generatedPackageName = org.grakovne.sideload.kindle.generated`.
3. В CI/локально: `flyway migrate` (на H2 в PG-режиме или Postgres) → `./gradlew generateJooq`.
4. Заменить `ConvertationTaskRepository` на `ConvertationTaskDao` (jOOQ):
   - `save(entity)` → `dsl.insertInto(CONVERTATION_TASK).set(...).execute()`
   - `findByCreatedAtGreaterThanAndCreatedAtLessThan` → `dsl.selectFrom(...).where(...).fetch()`
   - `findByStatusInAndCreatedAtLessThan` → `.where(CONVERTATION_TASK.STATUS.in(...))`
   - JPQL `touchLastActivity` → `dsl.update(USER).set(...).where(...).execute()`
5. `ConvertationTask` (data class) — убрать `@Entity`, `@Id`, `@Enumerated`.
6. `application.yml`: убрать `spring.jpa.*`, оставить `spring.flyway.*`.
7. Удалить из `build.gradle.kts`:
   - `implementation("org.springframework.boot:spring-boot-starter-data-jpa")`
   - `kotlin("plugin.jpa") version "2.3.21"`
8. Переписать `ConvertationTaskRepositoryTest` (`@DataJpaTest`) → `@JdbcTest` + jOOQ DSL, либо чистый `DataSource` + DSL.
9. Прогнать тесты, убедиться, что `AcceptanceScenarioTest` поднимается.

**Выход по этапу 0:** пилот работает, CI зелёный, принято решение: массово или нет.

---

## Этап 1. Массовая замена (1 день)

Для каждого из оставшихся 9 репозиториев — та же механика:

| Репозиторий | Файл | Derived-запросы |
|---|---|---|
| `TransferEmailTaskRepository` | stk/email/task/repository | 5 |
| `ShelfItemRepository` | shelf/repository | 2 |
| `ShelfReferenceRepository` | shelf/repository | 2 |
| `UserRepository` | user/reference/repository | 2 + 1 JPQL |
| `UserPreferencesRepository` | user/preferences/repository | ~1 |
| `UserActivityStateRepository` | telegram/state/repository | ~1 |
| `UserMessageReportRepository` | user/message/report/repository | ~1 |
| `MessageReferenceRepository` | telegram/message/reference/repository | ~1 |
| `ConverterBinaryReferenceRepository` | converter/binary/reference/repository | ~1 |

Для каждого:
1. Создать `<X>Dao` (jOOQ DSL), сигнатуры методов 1-в-1 с репозиторием.
2. Обновить все места использования (сервисы) — import и тип поля.
3. Удалить старый репозиторий.
4. Убрать JPA-аннотации из доменной модели.

**Ключевое:** оставить **интерфейсы-обёртки** (или классы) с теми же сигнатурами, что были у репозиториев, — так тесты сервисов с mockito-mock'ами **не трогаются**.

---

## Этап 2. Тесты (полдня)

1. Переписать `@DataJpaTest`-тесты репозиториев → `@JdbcTest` / чистый DSL.
   - `ConvertationTaskRepositoryTest` (если ещё не сделано на пилоте)
   - `MetricsRepositoryTest`
   - остальные, где есть `@DataJpaTest`
2. Проверить `AcceptanceScenarioTest` — `@SpringBootTest` должен подняться.
3. Прогнать весь стек: `./gradlew test jacocoTestReport`.
4. Убедиться, что JaCoCo-покрытие не просело.

---

## Этап 3. Доверие и чистка (1–2 часа)

1. `application.yml` — убрать всё `spring.jpa.*`, оставить `spring.flyway.*`.
2. `build.gradle.kts` — финальная чистка зависимостей.
3. `.gitignore` — проверить, что `generated/` (или куда выводит кодогенерация) **в git не попадает** (или попадает, как принято в проекте).
4. `README.md` — обновить раздел Tech stack (JPA → jOOQ), Persistence.
5. `TEST_PLAN.md` — обновить, если там упоминается JPA/H2-модель.

---

## Риски

| Риск | Митигация |
|---|---|
| jOOQ codegen требует БД | Генерация из H2 в PG-режиме (уже используется в тестах) или локальный Postgres + env vars |
| CI не знает пароль БД для кодогенерации | Codegen-задача читает `local.properties` / env; в CI — secrets |
| `fetchOrCreateShelf` — race condition | jOOQ даёт `insert ... on conflict do nothing` — **исправление бага как побочный эффект** |
| Тесты сервисов ломаются | Оставить интерфейсы-обёртки с теми же сигнатурами → mockito-мocks работают без изменений |
| `@Transactional` в JPA «закрывал» конкурентность | В текущем коде `@Transactional` и так нет → никакого регресса |

---

## Что НЕ трогаем
- Flyway-миграции (`V1…V11`) — как есть.
- Бизнес-логика (сервисы, event bus, конвертация, STK, shelf, метрики).
- Telegram-адаптер, локализация, CLI.
- `fb2_converter` — внешний бинарник.

---

## Файлы, которые изменятся
- `build.gradle.kts` — зависимости + кодогенерация.
- `src/main/resources/application.yml` — убрать JPA.
- 10 доменных классов — убрать JPA-аннотации.
- 10 репозиториев → 10 DAO (jOOQ).
- ~4–6 сервисов — update import/типов.
- 4–6 тестов репозиториев — переписать на `@JdbcTest`.
- `README.md`, `TEST_PLAN.md` — docs.
