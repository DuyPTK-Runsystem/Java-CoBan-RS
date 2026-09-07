# Dev Note: Lombok Annotation Guidance

## Related Developer Plan

- Plan: `document/dev-impl-plan/be/workflow-skill/004-lombok-annotation-guidance-2026-08-17.md`
- Approval status: Approved by user via agent on 2026-08-17.

## Actual Scope Completed

- Created project skill `.agents/skills/lombok-usage/`.
- Added Lombok annotation rules for JPA entities, DTO classes, services, controllers, components, and tests.
- Updated backend implementation skills to reference `@lombok-usage`.
- Updated Developer Plan status and summaries for plan 004.
- No backend source code or API behavior was changed.

## Files Changed

### Skill

- `.agents/skills/lombok-usage/SKILL.md`
- `.agents/skills/lombok-usage/agents/openai.yaml`

### Existing Backend Skills

- `.agents/skills/entity-impl/SKILL.md`
- `.agents/skills/service-impl/SKILL.md`
- `.agents/skills/controller-impl/SKILL.md`
- `.agents/skills/module-impl/SKILL.md`

### Developer Plan

- `document/dev-impl-plan/be/workflow-skill/004-lombok-annotation-guidance-2026-08-17.md`
- `document/dev-impl-plan/be/BE_DEV_PLAN_SUMMARY.md`
- `document/dev-impl-plan/summary/DEV_PLAN_SUMMARY.md`

### Dev Note

- `document/dev-note/be/workflow-skill/004-lombok-annotation-guidance-2026-08-17.md`
- `document/dev-note/be/BE_DEV_NOTE_SUMMARY.md`
- `document/dev-note/summary/DEV_NOTE_SUMMARY.md`

## Decisions

- Lombok guidance lives in a reusable project skill named `lombok-usage`.
- Entity guidance keeps the existing safety rule: avoid `@Data` on JPA entities.
- Entity guidance prefers `@Getter`, `@Setter`, and `@NoArgsConstructor`.
- Service/controller/component guidance prefers `@RequiredArgsConstructor` with `private final` dependencies.
- DTO guidance keeps Java records as records and allows Lombok on mutable DTO classes when useful.
- The skill explicitly corrects `@RequireArgsConstructor` to `@RequiredArgsConstructor`.
- Existing backend skills reference `@lombok-usage` instead of duplicating the full rule set.

## Validation

| Command | Result | Notes |
|---|---|---|
| `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/init_skill.py lombok-usage --path .agents/skills --interface display_name="Lombok Usage" --interface short_description="Guide Lombok annotations in Spring Boot code." --interface default_prompt="Apply the project Lombok annotation guidance."` | PASS | Needed escalated execution because `.agents` was read-only in sandbox. |
| `python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/lombok-usage` | PASS | Output: `Skill is valid!` |
| `find .agents/skills/lombok-usage -maxdepth 3 -type f` | PASS | Confirmed `SKILL.md` and `agents/openai.yaml`. |
| `rg -n '@lombok-usage\|RequiredArgsConstructor\|lombok-usage' .agents/skills/lombok-usage .agents/skills/entity-impl .agents/skills/service-impl .agents/skills/controller-impl .agents/skills/module-impl` | PASS | Confirmed references in the new and existing skills. |
| Backend test/build | NOT RUN | Not applicable; no backend code changed. |

## Deviations

- None.

## Risks

- The skill has not yet been exercised during a real Java code refactor.
- A future agent may still need to inspect project-specific DTO style before choosing Lombok annotations.

## Next Steps

- Use `lombok-usage` when creating, updating, or reviewing Spring Boot Java code that can use Lombok.
