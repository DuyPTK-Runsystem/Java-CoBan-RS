# Entity template

```java
package <base-package>.<module>.domain.entity;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "exam",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exam_grade_name",
                columnNames = {"grade_id", "exam_name"}),
        indexes = @Index(
                name = "idx_exam_grade_status_start",
                columnList = "grade_id,status,start_time"))
public class Exam extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "exam_name", nullable = false, length = 255)
    private String examName;

    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExamStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "total_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

}
```

Add imports and adapt physical column names to the active naming strategy/migration. Use `Long id` for identifiers, an auditable base entity, string enums, explicit precision/scale, and query-driven indexes.
