# Dev Note: Jackson JavaTimeModule Audit and Date-Time Serialization Fix

## Related Developer Plan

- Related plan: N/A (Direct bug fix / improvement for Jackson date-time serialization).
- Approval: User request received directly on `2026-08-24`.

## Actual Scope Completed

- Registered `JavaTimeModule` in `JacksonConfiguration.java`.
- Disabled `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS` so `LocalDate` and `LocalDateTime` serialize as ISO-8601 strings.
- Added unit tests in `JacksonConfigurationTest.java` to verify serialization format for `LocalDate` (`yyyy-MM-dd`) and `LocalDateTime` (`yyyy-MM-dd'T'HH:mm:ss`).

## Files Changed

- `BE/BaiTap-RS/src/main/java/com/JavaTraining/BaiTap_RS/config/JacksonConfiguration.java`
- `BE/BaiTap-RS/src/test/java/com/JavaTraining/BaiTap_RS/config/JacksonConfigurationTest.java`
- `document/dev-note/be/033-jackson-jsr310-audit-serialization-2026-08-24.md`

## Important Decisions

- Explicitly registering `JavaTimeModule` and disabling `WRITE_DATES_AS_TIMESTAMPS` on the Spring-managed `ObjectMapper` bean ensures consistent ISO-8601 date-time format across all JSON APIs and audit logging serialization.

## Validation

| Command | Result | Notes |
| --- | --- | --- |
| Unit tests in `JacksonConfigurationTest` | PASS | Verifies ISO-8601 string serialization for `LocalDate` and `LocalDateTime`. |

## Deviations and Risks

- None.

## Next Steps

- None.
