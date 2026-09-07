# Developer Plan: Lombok Annotation Guidance

## 1. Mục tiêu

- Chuẩn hóa hướng dẫn cho agent sử dụng Lombok annotation trong backend Spring Boot.
- Dựa trên các skill hiện có để agent biết khi nào dùng `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, và `@Builder`.
- Giữ an toàn cho JPA entity, service, controller, DTO và test code.

## 2. Requirement liên quan

- Yêu cầu trực tiếp:
  - Dùng `skill-creator`.
  - Hướng dẫn agent dùng Lombok annotation như `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@Builder`.
  - Dựa trên các skill hiện có.
- Quy tắc project:
  - Thay đổi skill/workflow cần Developer Plan và approval trước khi sửa.

## 3. Hiện trạng

- Backend đã có Lombok dependency trong `BE/BaiTap-RS/build.gradle.kts`.
- `entity-impl` hiện đã có rule tránh Lombok `@Data` trên entity và template dùng `@Getter`, `@Setter`, `@NoArgsConstructor`.
- `service-impl` template dùng `@RequiredArgsConstructor`.
- `controller-impl` template dùng `@RequiredArgsConstructor`.
- Chưa có skill riêng hoặc guideline tổng quát về Lombok usage cho agent.

## 4. Phạm vi

### In-scope

- Tạo skill mới `.agents/skills/lombok-usage/` để gom Lombok rules dùng chung.
- Viết `SKILL.md` ngắn gọn, gồm:
  - Trigger khi agent tạo/update/review Java Spring Boot code có thể dùng Lombok.
  - Quy tắc dùng Lombok theo layer:
    - Entity: dùng `@Getter`, `@Setter`, `@NoArgsConstructor`; tránh `@Data`; chỉ dùng `@Builder` khi thật sự cần và không phá JPA constructor.
    - Service/controller/config component: dùng `@RequiredArgsConstructor` với `final` dependencies.
    - DTO/class dữ liệu thường: có thể dùng `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder` nếu project dùng class DTO; nếu đang dùng Java record thì không đổi sang class chỉ để dùng Lombok.
    - Test fixture: có thể dùng builder nếu giảm noise test.
  - Quy tắc import và không dùng annotation thừa.
  - Quy tắc kiểm tra compile/test sau khi đổi Lombok.
- Tạo `agents/openai.yaml` bằng script của `skill-creator`.
- Cập nhật các skill liên quan để tham chiếu guideline mới khi phù hợp:
  - `.agents/skills/entity-impl/SKILL.md`
  - `.agents/skills/service-impl/SKILL.md`
  - `.agents/skills/controller-impl/SKILL.md`
  - `.agents/skills/module-impl/SKILL.md`
- Có thể cập nhật template reference nếu cần làm rõ Lombok imports/rules:
  - `.agents/skills/entity-impl/references/entity-template.md`
  - `.agents/skills/service-impl/references/service-template.md`
  - `.agents/skills/controller-impl/references/controller-template.md`
- Validate skill bằng `quick_validate.py`.

### Out-of-scope

- Không refactor application code hiện tại sang Lombok trong task này.
- Không đổi Gradle dependency vì Lombok đã tồn tại.
- Không ép dùng `@Data` cho mọi class.
- Không tạo migration/database change.
- Không chạy backend full validation nếu chỉ đổi skill/docs, trừ khi có code backend bị thay đổi.

## 5. Phương án triển khai

- Dùng `skill-creator/scripts/init_skill.py` để tạo skill `lombok-usage`.
- Không thêm resources/scripts riêng nếu chưa cần; guideline chính nằm trong `SKILL.md`.
- Skill mới sẽ được viết như một style/decision guide, không phải generator.
- Các skill hiện có sẽ tham chiếu ngắn gọn đến `lombok-usage` để agent nhớ dùng guideline khi tạo entity/service/controller/module.

## 6. Quy tắc Lombok dự kiến

- Entity:
  - Prefer `@Getter`, `@Setter`, `@NoArgsConstructor`.
  - Avoid `@Data` because it generates `toString`, `equals`, and `hashCode` that can be unsafe with JPA identity and relationships.
  - Avoid `@AllArgsConstructor` unless there is a clear non-JPA construction use.
  - Use `@Builder` only for explicit fixture/factory needs and keep JPA no-arg constructor.
- Service/controller:
  - Prefer `@RequiredArgsConstructor` with `private final` dependencies.
  - Do not keep manual constructors when Lombok constructor is used.
- DTO:
  - If DTO is a Java record, keep record and do not add Lombok.
  - If DTO is a mutable class, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, and `@Builder` may be used when useful.
- General:
  - Do not combine annotations that duplicate generated constructors.
  - Remove unused imports and manual boilerplate replaced by Lombok.
  - Run compile/tests appropriate to the changed code.

## 7. Phạm vi file dự kiến

### Tạo mới

- `.agents/skills/lombok-usage/SKILL.md`
- `.agents/skills/lombok-usage/agents/openai.yaml`

### Chỉnh sửa

- `.agents/skills/entity-impl/SKILL.md`
- `.agents/skills/service-impl/SKILL.md`
- `.agents/skills/controller-impl/SKILL.md`
- `.agents/skills/module-impl/SKILL.md`
- Có thể chỉnh template reference nếu cần.
- Dev Note sau khi implementation được approve.

## 8. Test và validation

- Validate skill:

```text
python3 /home/duyptk/.codex/skills/.system/skill-creator/scripts/quick_validate.py .agents/skills/lombok-usage
```

- Kiểm tra file sinh ra:

```text
find .agents/skills/lombok-usage -maxdepth 3 -type f
```

- Backend test/build:
  - `NOT RUN` nếu chỉ đổi skill/docs.
  - Chạy backend validation nếu task sau này refactor Java source sang Lombok.

## 9. Rủi ro

- Agent dùng `@Data` trên entity gây `equals/hashCode/toString` không mong muốn.
  - Giảm thiểu: rule entity nêu rõ tránh `@Data`.
- Agent đổi record DTO sang class chỉ để dùng Lombok.
  - Giảm thiểu: rule giữ record nếu đang phù hợp.
- Skill mới bị bỏ qua khi agent dùng `entity-impl`/`service-impl`.
  - Giảm thiểu: cập nhật các skill hiện có để tham chiếu Lombok guideline.

## 10. Output dự kiến

- Có skill `lombok-usage` hợp lệ.
- Các skill backend liên quan nhắc agent dùng Lombok guideline đúng layer.
- Không thay đổi source code application.

## 11. Approval status

- Trạng thái: Approved.
- Approved by user via agent on 2026-08-17.
