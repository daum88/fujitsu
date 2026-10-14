# Delivery Fee Service — Implementation Plan (Agent Build Spec)

Fujitsu Java Programming Trial Task 2026 · *“Do less, but show best.”*

This document is an executable build spec. Work through the milestones **in order**;
each milestone must be complete (`mvn verify` green, tests written, Javadoc present)
before the next starts. Graded criteria: OOP design & layering, readability, documented
public methods, error handling, test coverage, Git/project hygiene.

---

## 0. Build rules (apply to every file, no exceptions)

1. **No SQL or JPA logic in controllers.** Controllers = HTTP only: bind params, validate,
   delegate to a service, map to DTO. Services = transactions + orchestration. Repositories =
   data access only (derived queries / parameterized JPQL — never string-concatenated SQL).
   Entities never leave the service layer; DTOs in, DTOs out.
2. **Section every class** with region markers and a fixed layout (see §4). Regions make
   review fast: the interviewer should find any concept in < 30 seconds.
3. **Javadoc every public/protected method and class** (`@param`, `@return`, `@throws` when
   applicable). This is an explicit grading criterion.
4. **Errors via typed exceptions + `@RestControllerAdvice` only.** No try/catch-print,
   no returning errors with HTTP 200, no leaking stack traces or internal messages.
5. **No magic numbers** — boundary values live in the seeded rules; verbatim error messages
   live in a `BusinessMessages` constants class.
6. **Immutability first:** records for DTOs, `final` fields, constructor injection
   (Lombok `@RequiredArgsConstructor`), `BigDecimal` for all money.
7. Before finishing a milestone: `mvn verify` green (includes Spotless + JaCoCo), commit with
   a conventional message, update the README requirement-mapping table.

---

## 1. Goal, scope, non-goals

**Goal:** Spring Boot service that (1) hourly imports Estonian weather data for 3 stations,
(2) stores it append-only, (3) calculates delivery fees from **data-driven** business rules,
(4) exposes a documented REST API incl. CRUD over the rules and a historical `datetime` mode.

**Non-goals (stated in README):** auth/users, admin UI, Docker, multi-module build, caching
layers, reactive stack. Each cut saves time that goes into the graded criteria.

---

## 2. Stack & rationale

| Choice | Why |
|---|---|
| Java 21 (LTS) | Records, sealed interfaces, pattern matching |
| Spring Boot 3.5.x + Maven wrapper | Current stable, reproducible build, ubiquitous |
| Spring Data JPA + H2 **file mode** | H2 mandated; file mode → history survives restarts ("permanently stored"); in-memory for tests |
| Flyway | Versioned schema + seed data; zero code, enterprise signal |
| `RestClient` + Jackson XML (hardened) | Built-in sync HTTP client; parser locked down (§5) |
| springdoc-openapi | "Documented REST interface" = interactive Swagger UI, no hand-written docs |
| Lombok | Boilerplate removal |
| ProblemDetail (RFC 9457) | Standard machine-readable errors |
| Spotless (google-java-format) | Formatting enforced in build → readability guaranteed mechanically |
| JaCoCo | Coverage report + threshold → addresses "test coverage" criterion |
| MockWebServer (test) | Simulates weather API without Docker |
| GitHub Actions | Answers "where code is stored / how work is organized" |

Excluded deliberately: Spring Security, Redis, MapStruct, Testcontainers, auditing tables.

---

## 3. Architecture, layering, dependency rules

```
┌──────────────────────── REST layer (controllers, DTOs, validation) ────────────────────────┐
│ DeliveryFeeController      BaseFeeRuleController      ExtraFeeRuleController               │
└──────────────────────────────┬─────────────────────────────────────────────────────────────┘
                               ▼
┌──────────────────────── Application services (transactions, orchestration) ───────────────┐
│ DeliveryFeeService        WeatherImportService        FeeRuleService (CRUD + validity)    │
└──────────────────┬──────────────────────────────┬──────────────────────────────────────────┘
                   ▼                              ▼
┌────────── Domain: fee engine ─────────┐  ┌────── Domain: rules & weather ─────────────────┐
│ DeliveryFeeCalculator (composite)     │  │ AbstractFeeRule ← BaseFeeRule, ExtraFeeRule    │
│ ├─ RegionalBaseFeeComponent           │  │ WeatherObservation                            │
│ └─ ExtraFeesComponent ────────────────┤  │ City · VehicleType · ConditionType ·           │
│    ├─ NumericRangeMatcher             │  │ PhenomenonCategory (enums w/ behavior)         │
│    └─ PhenomenonMatcher  (ConditionMatcher) └──────────────┬───────────────────────────────┘
└───────────────────────────────────────────┐  Spring Data repositories (derived queries only)
                                            │  H2 (file) · Flyway schema + seed rules
Weather import: Cron → WeatherImportService → RestClient → XML DTOs → entity → append-only save
```

**Package tree (feature-based) and import direction:**

```
com.deliveryfee
├── DeliveryFeeApplication
├── config/            AppProperties (@ConfigurationProperties, validated), OpenApiConfig,
│                      HttpClients (RestClient + hardened XmlMapper beans)
├── common/            GlobalExceptionHandler, exceptions (ForbiddenVehicleUsageException,
│                      NoWeatherDataException, NotFoundException, ConflictException,
│                      BadRequestException), BusinessMessages, IntervalUtils
├── weather/           WeatherObservation, WeatherObservationRepository,
│                      WeatherObservationService, WeatherImportService, WeatherImportJob,
│                      xml/ObservationsXml, xml/StationXml
├── rules/             AbstractFeeRule, BaseFeeRule, ExtraFeeRule, repositories,
│                      FeeRuleService, dto/, web/ (controllers)
└── delivery/          DeliveryFeeController, DeliveryFeeService, DeliveryFeeCalculator,
                       DeliveryContext, FeeComponent, FeeResult (sealed), components/,
                       matcher/, dto/
```

**Dependency rules:** `delivery → {rules, weather}` only; `common ← all`; controllers know
only service interfaces + DTOs; repositories know only entities; no package cycles.

---

## 4. Code organization & readability conventions

### Class layout template (regions in this order)

```java
//region Constants
private static final String LOG_PREFIX = "weather-import";
//endregion

//region Dependencies
private final WeatherObservationRepository weatherRepository;
private final ExtraFeeRuleRepository extraFeeRuleRepository;
//endregion

//region Public API
/**
 * Calculates the delivery fee for the given city, vehicle and point in time.
 * @param city    city of delivery (determines station + regional rules)
 * @param vehicle vehicle used for delivery
 * @param at      calculation timestamp (must not be in the future)
 * @return full fee breakdown, never null
 * @throws ForbiddenVehicleUsageException if an active rule forbids the vehicle
 * @throws NoWeatherDataException         if no observation exists at or before {@code at}
 */
public DeliveryFeeResponse calculate(City city, VehicleType vehicle, Instant at) { ... }
//endregion

//region Private helpers
private FeeResult resolveBaseFee(DeliveryContext ctx) { ... }
//endregion
```

### Rules (enforced by Spotless where possible, by review otherwise)

- **Layout order:** constants → dependencies (`final`) → constructor (Lombok) → public API
  (Javadoc) → overridden methods → private helpers. Private helpers grouped by concern.
- **Methods ≤ ~25 lines**, single responsibility, guard clauses early, no nested ternaries,
  no deeply nested `if`/`else` (extract helpers instead).
- **Controllers stay thin:** one service call per endpoint; validation via annotations;
  no `if`-chains for errors — throw typed exceptions, let the advice render.
- **Services:** `@Transactional` on write operations only; reads stay cheap.
- **Naming:** methods verb-first (`calculateFee`, `importLatestWeather`); test names
  `methodUnderTest_condition_expectedBehavior`.
- **Immutability:** records for DTOs with `static` factory `from(...)`; entities with
  minimal behavior (behavior lives in enums, not services).
- Spotless google-java-format (AOSP order), 120-char line limit, checked in `mvn verify`.

---

## 5. Security considerations (threat → mitigation)

| # | Threat | Mitigation (implement, don't just document) |
|---|---|---|
| 1 | XXE / entity expansion via external XML feed | Hardened `XmlMapper`: `XMLInputFactory` with `SUPPORT_DTD=false`, `IS_SUPPORTING_EXTERNAL_ENTITIES=false`, entity expansion limits; unit test parses a malicious DTD sample and asserts rejection |
| 2 | SQL injection | No native/string SQL anywhere; only Spring Data derived queries and parameterized JPQL (`:named` params) |
| 3 | Mass assignment / entity binding | DTOs only as `@RequestBody`; entities never bound or returned |
| 4 | Information disclosure | Advice returns `ProblemDetail` with fixed messages; full stack traces only in server logs; generic 500 body without internals |
| 5 | Injection via path/query params | jakarta validation on all params; strict enum binding (documented case-sensitive values); `datetime` ISO-8601 parse failures → 400 |
| 6 | SSRF | Import URL comes from validated config (`AppProperties`), never from request input; HTTPS only |
| 7 | Overexposed management endpoints | Actuator: `health,info` only; `env/beans/configprops` excluded |
| 8 | H2 console abuse | Enabled only in `dev` profile; off by default |
| 9 | Resource exhaustion (huge feed) | Client read timeout (30 s), connect timeout (5 s); import parses only the 3 needed stations (early filter) |
| 10 | Wrong HTTP semantics | GET for fee (safe), idempotent PUT for updates, DELETE removes — Spring handles 405; no state-changing GET |
| 11 | Supply chain | Versions managed by Spring Boot BOM only; stretch: OWASP dependency-check (skip if time-pressed, note in README) |

Explicit non-goal (README): authentication/authorization — out of task scope; noted as
production follow-up.

---

## 6. Domain model

### Enums (behavior carriers — the OOP showcase)

```java
public enum City {
    TALLINN("Tallinn-Harku"), TARTU("Tartu-Tõravere"), PÄRNU("Pärnu");
    private final String stationName;
    // getter; used by BOTH import filter and fee lookup → single source of truth
}

public enum PhenomenonCategory {
    // checked in declaration order; first match wins (see precedence §8)
    SNOW_SLEET("snow", "sleet"),
    GLAZE_HAIL_THUNDER("glaze", "hail", "thunder", "storm"),
    RAIN("rain", "drizzle", "shower");
    boolean matches(String phenomenon) { /* case-insensitive substring */ }
}
```

`VehicleType(CAR, SCOOTER, BIKE)`, `ConditionType(TEMPERATURE, WIND, PHENOMENON)`.

### Entities — DDL (Flyway `V1__schema.sql`)

```sql
CREATE TABLE weather_observation (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    station_name    VARCHAR(100)  NOT NULL,
    wmo_code        VARCHAR(20),                       -- API sends empty for some stations
    air_temperature NUMERIC(4,1)  NOT NULL,
    wind_speed      NUMERIC(4,1),                      -- API may omit
    phenomenon      VARCHAR(100),
    observed_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_station_observed UNIQUE (station_name, observed_at)  -- idempotent imports + lookup index
);

CREATE TABLE base_fee_rule (
    id           BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    city         VARCHAR(20)  NOT NULL,
    vehicle_type VARCHAR(20)  NOT NULL,
    fee          NUMERIC(10,2) NOT NULL,
    valid_from   TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to     TIMESTAMP WITH TIME ZONE,              -- NULL = open-ended
    CONSTRAINT uq_base_rule UNIQUE (city, vehicle_type, valid_from)
);

CREATE TABLE extra_fee_rule (
    id                  BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    condition_type      VARCHAR(20)  NOT NULL,
    vehicle_type        VARCHAR(20)  NOT NULL,
    min_value           NUMERIC(4,1), min_inclusive BOOLEAN,
    max_value           NUMERIC(4,1), max_inclusive BOOLEAN,
    phenomenon_category VARCHAR(30),
    fee                 NUMERIC(10,2),                  -- NULL when forbidden
    forbidden           BOOLEAN NOT NULL DEFAULT FALSE,
    valid_from          TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_extra_rule UNIQUE (condition_type, vehicle_type, valid_from),
    CONSTRAINT chk_extra_rule CHECK (
        (min_value IS NULL AND max_value IS NULL AND phenomenon_category IS NOT NULL) OR
        (phenomenon_category IS NULL AND (min_value IS NOT NULL OR max_value IS NOT NULL))),
    CONSTRAINT chk_fee_forbidden CHECK (forbidden = FALSE OR fee IS NULL)
);
CREATE INDEX idx_extra_lookup ON extra_fee_rule (condition_type, vehicle_type, valid_from);
CREATE INDEX idx_base_lookup   ON base_fee_rule  (city, vehicle_type, valid_from);
```

- **One generic `extra_fee_rule` table** models all three conditions (temperature/wind/
  phenomenon) and both outcomes (fee / forbidden) → maximum reuse, CRUD satisfied structurally.
- **Validity windows are half-open `[valid_from, valid_to)`** — documented on the entity,
  tested at the edges. Null `valid_to` = open-ended.
- Entities share `@MappedSuperclass AbstractFeeRule { id, validFrom, validTo }` — reuse.

### Seed data (`V2__seed_rules.sql`) — 23 rows

- 9 × `base_fee_rule` (3 cities × 3 vehicles, fees from spec), `valid_from = 2000-01-01`.
- 14 × `extra_fee_rule`:
  - TEMPERATURE × {SCOOTER, BIKE}: `t < -10 → 1.00` (`max=-10, max_inclusive=false`) ·
    `-10 ≤ t ≤ 0 → 0.50` (`min=-10,min_incl=true; max=0,max_incl=true`)
  - WIND × BIKE only: `10 ≤ w ≤ 20 → 0.50` · `w > 20 → forbidden` (`min=20,min_inclusive=false`)
  - PHENOMENON × {SCOOTER, BIKE}: SNOW_SLEET → 1.00 · RAIN → 0.50 · GLAZE_HAIL_THUNDER → forbidden
- Boundary encoding is deliberate and disjoint: 20 m/s pays 0.50, >20 forbidden; −10 °C pays
  0.50, <−10 pays 1.00. Tests lock these in.

---

## 7. Weather import pipeline

### Config (`application.yml`, validated `AppProperties`)

```yaml
app:
  weather-import:
    enabled: true
    cron: "0 15 * * * ?"        # HH:15:00 hourly — required default
    url: "https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php"
    connect-timeout: 5s
    read-timeout: 30s
    batch-size: 50
```

### Algorithm — `WeatherImportService.importLatest()`

```
1. GET url via RestClient (timeouts from config)
2. Parse → ObservationsXml{timestamp(epoch sec), stations[]} with hardened XmlMapper
   (ignore unknown fields; blank strings ⇒ null)
3. Filter to City.stationName values FIRST (skip everything else before mapping)   ← early exit, cheap
4. Map: epoch seconds ⇒ Instant observedAt
5. One grouped query: latest stored observedAt per station → skip feed entries with
   observedAt ≤ latest per station                                              ← append-only + idempotent
6. saveAll(newOnes) in ONE transaction (batch size hint: order_inserts + batch_size=50)
7. Log: info(count imported / skipped / missing stations), warn(missing station), 
   error(any failure → job survives, app serves last-known data)
```

- `WeatherImportJob` = `@Scheduled(cron = "${app.weather-import.cron}")` + an
  `ApplicationReadyEvent` listener running a **best-effort initial import at startup**
  (demo-friendly: fresh data immediately; failures logged, never block boot).
- Scheduling gated by `@ConditionalOnProperty(app.weather-import.enabled)`; disabled in tests.
- Repositories expose exactly two queries:
  - `findTop1ByStationNameAndObservedAtLessThanEqualOrderByObservedAtDesc(...)`
  - `@Query` grouped max-observedAt-per-station (one round-trip for step 5).

---

## 8. Fee engine

```java
public sealed interface FeeResult permits FeeResult.Fee, FeeResult.Forbidden {
    record Fee(BigDecimal amount, String label) implements FeeResult {}
    record Forbidden(String label, String message) implements FeeResult {}
}

public interface FeeComponent { FeeResult evaluate(DeliveryContext ctx); }

// DeliveryContext(City city, VehicleType vehicle, Instant requestedAt, WeatherObservation observation)
// DeliveryFeeCalculator = composite: [RegionalBaseFeeComponent, ExtraFeesComponent];
//   folds results in order; any Forbidden → throw ForbiddenVehicleUsageException(message verbatim)
```

- **Rule lookup (shared semantics for both rule types, one JPQL pattern):**
  active = `valid_from ≤ at AND (valid_to IS NULL OR valid_to > at)`, newest `valid_from` wins.
  → historical `datetime` automatically uses historical rules. No special-casing.
- **Weather lookup:** `WeatherObservationService.latestFor(City, Instant at)` → newest
  `observed_at ≤ at` (top-1 indexed query). The heart of the datetime feature.
- **`RegionalBaseFeeComponent`:** active base rule for (city, vehicle, at) → `Fee(rbf)`.
- **`ExtraFeesComponent`:** for each active extra rule matching the vehicle:
  - `NumericRangeMatcher` (one class serves TEMPERATURE **and** WIND): range checks with
    inclusivity flags; null wind speed ⇒ no match (defensive).
  - `PhenomenonMatcher`: `PhenomenonCategory` classification with **precedence order
    SNOW_SLEET → GLAZE_HAIL_THUNDER → RAIN** so “Light snow shower” ⇒ snow, “rain shower” ⇒
    rain; no category ⇒ no match.
  - `forbidden` match → `Forbidden` (short-circuit); else sum fees.
- **Breakdown** `{regionalBaseFee, airTemperatureExtraFee, windSpeedExtraFee,
  weatherPhenomenonExtraFee}` → the spec example is assertable 1:1 in tests.
- `BusinessMessages.FORBIDDEN_VEHICLE = "Usage of selected vehicle type is forbidden"` —
  single constant used by engine, advice and tests.

---

## 9. REST API (`/api/v1`, OpenAPI-documented)

### Delivery fee

`GET /api/v1/delivery-fee?city=TARTU&vehicleType=BIKE[&datetime=2026-10-14T12:15:00Z]`

```json
200 OK
{
  "city": "TARTU", "vehicleType": "BIKE", "currency": "EUR",
  "totalFee": 4.00, "calculatedAt": "…", "weatherObservedAt": "…",
  "breakdown": { "regionalBaseFee": 2.50, "airTemperatureExtraFee": 0.50,
                 "windSpeedExtraFee": 0.00, "weatherPhenomenonExtraFee": 1.00 }
}
```

- `city`, `vehicleType` required (`@NotNull`, strict case-sensitive enums — Swagger lists
  values); `datetime` optional ISO-8601 `Instant`, **`@PastOrPresent`** (future time ⇒ 400).
- Errors — all `application/problem+json` (`ProblemDetail`):

| Status | Case |
|---|---|
| 400 | validation failure / bad enum / malformed or future datetime |
| 400 | **“Usage of selected vehicle type is forbidden”** (verbatim `detail`) |
| 422 | no weather data for the city at/ before the requested time |
| 404 | unknown rule id |
| 409 | overlapping active rule on create/update |
| 500 | fallback, internals hidden |

### Rules CRUD

```
GET|POST     /api/v1/rules/base-fees        PUT|DELETE /api/v1/rules/base-fees/{id}
GET|POST     /api/v1/rules/extra-fees       PUT|DELETE /api/v1/rules/extra-fees/{id}
```

- DTOs = records with jakarta validation; extra-fee DTO cross-field validation: exactly one
  of (numeric bounds / phenomenon category) present; `fee` required unless `forbidden=true`.
- `FeeRuleService` (one class, reused by both controllers): create/update/delete/query with
  `IntervalUtils.overlaps()` check → 409 on conflicting active windows. Deleting a rule never
  breaks historical calculations (validity queries keep them reproducible).

---

## 10. Error handling strategy

| Exception (in `common/`) | HTTP | Detail |
|---|---|---|
| `MethodArgumentNotValidException` / `MethodArgumentTypeMismatchException` / `BadRequestException` | 400 | field/param errors listed |
| `ForbiddenVehicleUsageException` | 400 | `BusinessMessages.FORBIDDEN_VEHICLE` verbatim |
| `NotFoundException` | 404 | entity + id |
| `ConflictException` | 409 | overlapping rule description |
| `NoWeatherDataException` | 422 | city + requested time |
| anything else | 500 | generic message; full stack in logs only |

One `@RestControllerAdvice` renders all bodies via a shared `ProblemDetail` builder helper —
one implementation reused by every mapping.

---

## 11. Reuse & optimization ledger

**Reused (write once, use everywhere):**
1. `City.stationName` — import filter *and* weather lookup; no duplicated station config.
2. One `extra_fee_rule` table + one `ExtraFeesComponent` — all 3 conditions; a 4th condition
   = new rows (+ matcher class only if numeric/phrase logic differs).
3. `NumericRangeMatcher` — shared by TEMPERATURE and WIND.
4. `AbstractFeeRule` (@MappedSuperclass) — id/validity fields + half-open semantics shared by
   both rule entities.
5. Validity-window JPQL pattern — same shape in both rule repositories.
6. `IntervalUtils` — validity overlap check reused by base-fee and extra-fee CRUD.
7. `PhenomenonCategory.matches()` — keyword classification in the enum, used only by the matcher.
8. Static `from(entity)` factories on DTO records — reused by controllers and tests.
9. `GlobalExceptionHandler` + ProblemDetail builder — one path for every error.
10. One `RestClient` bean + one hardened `XmlMapper` bean (config, not scattered).
11. `BusinessMessages` constants — engine, advice and tests reference identical strings.
12. Parameterized tests + a `TestFixtures` factory — test code written once per concept.

**Optimized (deliberate, explainable):**
- Unique constraint `(station_name, observed_at)` doubles as the latest-observation index.
- Import: early station filter (skip parsing 100+ stations we discard), one grouped
  max-observedAt query, single transactional `saveAll` with JDBC batch hints.
- Fee path: 3 indexed queries total (weather + base rule + extra rules) — no N+1.
- XML parse ignores unknown fields; blank ⇒ null; timeouts bound worst case.

**Deliberately NOT optimized (README decision log):** no caching layer (H2 + indexes is fast
at this scale; a cache invites stale-data bugs and complexity), no async import, no reactive
stack, no MapStruct — framework reuse beats framework sprawl.

---

## 12. Testing strategy

### Conventions
- Names: `methodUnderTest_condition_expectedBehavior`.
- Shared `TestFixtures` factory for observations/rules; parameterized tests for boundary
  matrices; assertions on exact fee values (`BigDecimal` compare, not doubles).

### Matrix

| Area | Tool | Cases |
|---|---|---|
| NumericRangeMatcher | JUnit parameterized | **exact boundaries −10, 0, 10, 20** × inclusivity flags; −10.1 → 1.00; −10.0 → 0.50; 0.0 → 0.50; 0.1 → none; 20.0 → 0.50; 20.1 → forbidden; null wind → no match |
| PhenomenonClassifier | JUnit | “Light snow shower” → SNOW_SLEET; “rain shower”, “Moderate drizzle” → RAIN; “glaze”, “thunderstorm” → GLAZE_HAIL_THUNDER; “Cloudy with clear spells” → none; case-insensitivity |
| FeeCalculator | Mockito | **Spec example: Tartu+Bike = 4.00**; car never pays extras; forbidden short-circuits (no partial fees); missing observation → NoWeatherDataException |
| Rule validity | Mockito | `valid_from == at` included; `valid_to == at` excluded; NULL valid_to open-ended; newest rule wins; rule created today never affects yesterday's calculation |
| Import service | MockWebServer + **real recorded XML** (captured from live API as `weather-sample.xml`) | exactly 3 stations imported, others ignored; re-run → 0 duplicates (append-only); missing station → warn + others imported; blank fields → null; negative temperature parsed; API 500 → logged, no crash, DB unchanged; malformed XML → logged, no crash; **malicious DTD → rejected (XXE test)** |
| Fee endpoint | `@WebMvcTest` + MockMvc | 200 payload matches contract JSON exactly; forbidden → 400 with **verbatim** message; invalid/lowercase enum → 400; missing param → 400; malformed datetime → 400; **future datetime → 400**; datetime before first observation → 422; OpenAPI JSON generated |
| Rules CRUD | `@WebMvcTest` + MockMvc | create/read/update/delete round-trip; update/delete unknown id → 404; overlapping rule → 409; negative fee → 400; extra-fee DTO with both bounds and category → 400 |
| Integration | `@SpringBootTest` + H2 (in-memory) | spec example end-to-end via seeded rules + inserted observations; datetime uses *older* observation **and** *older* rule version; import twice → history grows, latest wins |
| Quality gate | JaCoCo | ≥ 70% instruction coverage on `com.deliveryfee.*`; enforced in `mvn verify` |

---

## 13. Build, CI, repo hygiene

- **Maven wrapper committed.** Plugins: spring-boot-maven-plugin, **spotless** (check bound to
  `verify`), **jacoco** (threshold rule), surefire. No other plugins.
- **GitHub Actions** (`.github/workflows/ci.yml`): JDK 21 + Maven cache → `mvn -B verify` →
  upload JaCoCo report. Badge in README.
- **Git workflow:** one branch per milestone → self-reviewed PR → squash merge. Conventional
  commits (`feat: weather import cron`, `test: boundary cases for wind matcher`). Atomic
  commits per concern. History is graded — make it tell the story.
- **README.md:** purpose, architecture (Mermaid), quickstart + demo curls, Swagger link, test
  instructions, security notes, decision log, **requirement→implementation mapping table**.

---

## 14. Milestones (agent execution order — file-level deliverables)

### M0 — Skeleton (foundation; everything below depends on it)
`pom.xml`, `.mvn/` wrapper, `.gitignore`, `.editorconfig`, `application.yml` (+ `application-dev.yml`
for H2 console), `DeliveryFeeApplication`, `config/AppProperties`, `config/OpenApiConfig`,
`config/HttpClients` (RestClient + hardened XmlMapper beans), Flyway `V1__schema.sql`,
`V2__seed_rules.sql`, `.github/workflows/ci.yml`, `README.md` skeleton, `BusinessMessages`.
**Done when:** `mvn verify` green; app boots; schema + 23 seed rows applied (verify via H2
console/query in dev); CI green; spotless + jacoco enforced.

### M1 — Weather import
`weather/xml/ObservationsXml`, `weather/xml/StationXml`, `WeatherObservation`,
`WeatherObservationRepository` (2 queries), `WeatherImportService`, `WeatherImportJob`
(cron + startup import), test resource `weather-sample.xml` (real recorded payload),
`WeatherImportServiceTest` (MockWebServer).
**Done when:** import matrix tests green; append-only proven (re-run → no duplicates).

### M2 — Domain + fee engine
`City`, `VehicleType`, `ConditionType`, `PhenomenonCategory`, `AbstractFeeRule`,
`BaseFeeRule`, `ExtraFeeRule` + repositories (validity JPQL), `WeatherObservationService`,
`FeeResult`, `FeeComponent`, `DeliveryContext`, `components/RegionalBaseFeeComponent`,
`components/ExtraFeesComponent`, `matcher/ConditionMatcher`, `matcher/NumericRangeMatcher`,
`matcher/PhenomenonMatcher`, `DeliveryFeeCalculator`, `IntervalUtils`, `TestFixtures`,
unit tests per §12 matrix.
**Done when:** spec example = 4.00 green; boundary + precedence + validity tests green.

### M3 — Fee endpoint
`delivery/dto/DeliveryFeeResponse`, `delivery/dto/FeeBreakdown`, `DeliveryFeeService`,
`DeliveryFeeController`, `common/` exceptions + `GlobalExceptionHandler`, `@WebMvcTest`s.
**Done when:** endpoint + error table cases green; Swagger UI shows the endpoint.

### M4 — Rules CRUD
`rules/dto/*`, `FeeRuleService` (CRUD + overlap → 409), `BaseFeeRuleController`,
`ExtraFeeRuleController`, tests (round-trip, 404, 409, validation).
**Done when:** CRUD matrix green; deleting a rule leaves historical calc reproducible.

### M5 — Historical datetime
`@PastOrPresent` wiring; integration tests: older observation **and** older rule version for a
past timestamp; future → 400; before-first-observation → 422; README section.
**Done when:** historical integration tests green; decision log documents half-open windows.

### M6 — Polish
Javadoc sweep (every public method), README completion (diagram, requirement mapping,
decision log, demo script `scripts/demo.sh` with curl examples), final region/layout review,
JaCoCo threshold confirmation, commit-history cleanup.
**Done when:** repo is walkthrough-ready: clone → `mvn spring-boot:run` → demo works offline.

Estimated 15–20 focused hours. **If time runs out, stop after M3/M4** — finished-and-tested
beats broad-and-broken. M5 is the differentiator; M6 is cheap insurance.

---

## 15. Decision log (defend these at the interview)

1. **H2 file mode** — "permanently stored" history must survive restarts.
2. **Rules in DB with `[valid_from, valid_to)`** — CRUD + historical calculation require
   data-driven, time-aware rules; half-open windows avoid ambiguity; hardcoding violates both.
3. **One generic `extra_fee_rule` table + one matcher interface** — three conditions, one
   mechanism; new condition = new rows (+ matcher only if needed).
4. **`BigDecimal` scale 2 for money** — never double/float.
5. **`Instant`/UTC everywhere** — no timezone ambiguity in stored history.
6. **RestClient over WebClient** — synchronous batch import; reactivity adds zero value here.
7. **Composite + strategy over if-chains** — each fee source independently testable;
   forbidden conditions modeled as data, not code.
8. **Flyway over `data.sql`** — versioned, reviewable migrations.
9. **MockWebServer + recorded real XML over WireMock/Testcontainers** — no Docker, tests
   pinned to reality.
10. **Feature packages over layers-per-package** — matches the domain, limits sprawl.
11. **No caching layer** — indexed H2 queries are fast at this scale; correctness > micro-optimization.
12. **Regions + fixed class layout** — review speed is a graded criterion.

## 16. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Weather API down during demo | Startup import is best-effort; service serves stored history; demo script uses committed sample data path |
| API changes field names | Ignore-unknown-fields + blank⇒null mapping; recorded payload keeps tests honest |
| Duplicate imports | UNIQUE(station, observed_at) + latest-per-station skip |
| Interviewer probes 20 m/s or −10 °C boundary | Encoded via inclusivity flags; parameterized tests prove it |
| XXE-style attack on import | Hardened XmlMapper + rejection test (§5.1) |
| Future/lowercase datetime abuse | `@PastOrPresent`, strict enums, 400s tested |
| Over-engineering temptation | Non-goals section + §11 "deliberately NOT optimized" — revisit only if all M0–M6 done |
