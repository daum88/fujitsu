# Delivery Fee Service

Spring Boot service that hourly imports Estonian weather data, stores it append-only, and
calculates delivery fees from **data-driven** business rules (regional base fee + weather
extra fees). Exposes a documented REST API with full CRUD over the rules and a historical
`datetime` mode.

> Fujitsu Java Programming Trial Task 2026 — "Do less, but show best."

## Status

- [x] M0 — Skeleton (build, schema, seed rules, CI)
- [ ] M1 — Weather import
- [ ] M2 — Domain + fee engine
- [ ] M3 — Fee endpoint
- [ ] M4 — Rules CRUD
- [ ] M5 — Historical datetime
- [ ] M6 — Polish

*(This table is kept up to date as milestones complete.)*

## Quickstart

```bash
./mvnw spring-boot:run
# Swagger UI: http://localhost:8080/swagger-ui.html
```

## Non-goals

Authentication/authorization, admin UI, Docker, multi-module build, caching layer, reactive
stack. Each cut saves time that goes into the graded criteria (OOP design, layering,
readability, documented public methods, error handling, test coverage, project hygiene).

## Requirement → implementation mapping

*(Completed incrementally per milestone.)*

| Requirement | Implementation | Verified by |
|---|---|---|
| Hourly weather import | `WeatherImportJob` + `WeatherImportService` | M1 tests |
| Append-only history | `UNIQUE(station, observed_at)` + latest-per-station skip | M1 tests |
| Data-driven rules | `base_fee_rule` + `extra_fee_rule` (Flyway seeds) | M2 tests |
| Documented REST API | springdoc Swagger UI | M3 tests |
| Rules CRUD | `BaseFeeRuleController` / `ExtraFeeRuleController` | M4 tests |
| Historical datetime | half-open validity windows + weather lookup at/before | M5 tests |
