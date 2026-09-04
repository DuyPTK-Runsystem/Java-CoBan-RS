package com.JavaTraining.BaiTap_RS.scorebook.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.logging.DeveloperTrace;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.requests.ReqFilterCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.domain.DTOs.response.ResCalculationTaskDTO;
import com.JavaTraining.BaiTap_RS.scorebook.service.CalculationTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2")
@SuppressWarnings("PMD.GuardLogStatement")
public class CalculationTaskController {

    private static final String OFFICE_ROLES = "hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')";
    private static final String TASK_ID = "taskId";
    private static final String STUDENT_CODE = "studentCode";
    private static final String STUDENT_ID = "studentId";

    private final CalculationTaskService taskService;

    public CalculationTaskController(CalculationTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/scorebooks/calculation-tasks")
    @ApiMessage("Tra cứu calculation task")
    @PreAuthorize(OFFICE_ROLES)
    public Page<ResCalculationTaskDTO> findTasks(
            @Valid @ModelAttribute ReqFilterCalculationTaskDTO filter) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalculationTaskController.class,
                        "CalculationTaskController.findTasks");
        return taskService.findTasks(filter);
    }

    @GetMapping("/scorebooks/calculation-tasks/failed")
    @ApiMessage("Tra cứu calculation task lỗi")
    @PreAuthorize(OFFICE_ROLES)
    public Page<ResCalculationTaskDTO> findFailedTasks(
            @Valid @ModelAttribute ReqFilterCalculationTaskDTO filter) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalculationTaskController.class,
                        "CalculationTaskController.findFailedTasks");
        return taskService.findFailedTasks(filter);
    }

    @PostMapping("/scorebooks/calculation-tasks/{taskId}/retry")
    @ApiMessage("Retry calculation task")
    @PreAuthorize(OFFICE_ROLES)
    public ResCalculationTaskDTO retry(@PathVariable(TASK_ID) @Positive Long taskId) {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskController.class,
                "CalculationTaskController.retry");
        return taskService.retryTask(taskId);
    }

    @PostMapping("/scorebooks/calculation-tasks/retry-all-failed")
    @ApiMessage("Retry toàn bộ calculation task lỗi")
    @PreAuthorize(OFFICE_ROLES)
    public List<ResCalculationTaskDTO> retryAllFailedTasks() {
        DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                CalculationTaskController.class,
                "CalculationTaskController.retryAllFailedTasks");
        return taskService.retryAllFailedTasks();
    }

    @PostMapping("/students/{studentCode:[A-Za-z][A-Za-z0-9]*}/transcripts/recalculate")
    @ApiMessage("Yêu cầu tính lại bảng điểm")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResCalculationTaskDTO> recalculate(
            @PathVariable(STUDENT_CODE) String studentCode,
            @RequestParam("academicYearId") @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalculationTaskController.class,
                        "CalculationTaskController.recalculate");
        return ResponseEntity.accepted().body(taskService.requestRecalculation(studentCode, academicYearId));
    }

    @PostMapping("/students/{studentId:[0-9]+}/transcripts/recalculate")
    @ApiMessage("Yêu cầu tính lại bảng điểm theo studentId")
    @PreAuthorize(OFFICE_ROLES)
    public ResponseEntity<ResCalculationTaskDTO> recalculateById(
            @PathVariable(STUDENT_ID) @Positive Long studentId,
            @RequestParam("academicYearId") @Positive Long academicYearId) {
                DeveloperTrace.trace(/* NOPMD GuardLogStatement */
                        CalculationTaskController.class,
                        "CalculationTaskController.recalculateById");
        return ResponseEntity.accepted().body(taskService.requestRecalculation(studentId, academicYearId));
    }
}
