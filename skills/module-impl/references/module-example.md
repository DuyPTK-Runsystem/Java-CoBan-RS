# Module example

```text
exam/
├── controller/ExamController.java
├── service/ExamService.java
├── repository/ExamRepository.java
└── domain/
    ├── entity/Exam.java
    └── DTOs/
        ├── response/ResExamDTO.java
        └── requests/
            ├── ReqCreateExamDTO.java
            ├── ReqUpdateExamDTO.java
            └── ReqExamStatusDTO.java
```

```java
public interface ExamRepository extends JpaRepository<Exam, Long>, JpaSpecificationExecutor<Exam> {
}
```

```java
public record ReqCreateExamDTO(
        @NotBlank @Size(max = 255) String examName,
        @NotNull Long gradeId,
        @NotNull @Positive Integer durationMinutes,
        @NotNull @DecimalMin("0.01") BigDecimal totalScore) {}
```

```java
public record ResExamDTO(Long id, String examName, Long gradeId,
        Integer durationMinutes, BigDecimal totalScore, String status) {}
```

Mirror production packages in tests:

```text
src/test/java/<base-package>/exam/
├── controller/ExamControllerTest.java
├── service/ExamServiceTest.java
└── repository/ExamRepositoryTest.java
```
