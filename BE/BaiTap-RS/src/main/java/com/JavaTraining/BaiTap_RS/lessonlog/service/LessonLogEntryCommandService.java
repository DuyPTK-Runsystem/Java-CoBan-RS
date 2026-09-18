package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Semester;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SemesterStatus;
import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqTransitionLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.requests.ReqUpdateLessonLogDTO;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.DTOs.response.LessonLogEntryResponse;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogStatus;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;

import lombok.RequiredArgsConstructor;

/** Coordinates entry state changes after loading and request-level validation. */
@Service
@RequiredArgsConstructor
public class LessonLogEntryCommandService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LessonLogEntryRepository entries;
    private final SemesterRepository semesters;
    private final TeacherRepository teachers;
    private final LessonLogEntryLifecycleService lifecycle;

    @Transactional
    public LessonLogEntryResponse update(Long id, ReqUpdateLessonLogDTO request,
            Function<LessonLogEntry, LessonLogEntryResponse> mapper) {
        LessonLogEntry entry = load(id);
        assertOpenSemester(entry.getSemesterId());
        LessonLogActorAuthorization.assertTeacherAssignment(entry.getAssignedTeacherId(), teachers);
        version(entry.getVersion(), request.expectedVersion());
        if (entry.getStatus() == LessonLogStatus.AMENDED || entry.getStatus() == LessonLogStatus.REVIEWED) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Sổ đã được duyệt/điều chỉnh");
        }
        writable(entry.getLessonEndsAt(), entry.getEditWindowExpiresAt());
        return mapper.apply(lifecycle.update(entry, request));
    }

    @Transactional
    public LessonLogEntryResponse submit(Long id, Long expectedVersion,
            Function<LessonLogEntry, LessonLogEntryResponse> mapper) {
        LessonLogEntry entry = load(id);
        assertOpenSemester(entry.getSemesterId());
        version(entry.getVersion(), expectedVersion);
        LessonLogActorAuthorization.assertTeacherAssignment(entry.getAssignedTeacherId(), teachers);
        writable(entry.getLessonEndsAt(), entry.getEditWindowExpiresAt());
        return mapper.apply(lifecycle.submit(entry));
    }

    @Transactional
    public LessonLogEntryResponse review(Long id, ReqTransitionLessonLogDTO request,
            Function<LessonLogEntry, LessonLogEntryResponse> mapper) {
        LessonLogEntry entry = load(id);
        assertOpenSemester(entry.getSemesterId());
        version(entry.getVersion(), request.expectedVersion());
        if (entry.getStatus() != LessonLogStatus.SUBMITTED && entry.getStatus() != LessonLogStatus.AMENDED) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ duyệt sổ đã nộp hoặc đã điều chỉnh");
        }
        return mapper.apply(lifecycle.review(entry, request));
    }

    @Transactional
    public LessonLogEntryResponse amend(Long id, ReqTransitionLessonLogDTO request,
            Function<LessonLogEntry, LessonLogEntryResponse> mapper) {
        LessonLogActorAuthorization.assertManager();
        LessonLogEntry entry = load(id);
        assertOpenSemester(entry.getSemesterId());
        version(entry.getVersion(), request.expectedVersion());
        if (entry.getStatus() == LessonLogStatus.DRAFT && (entry.getEditWindowExpiresAt() == null
                || LocalDateTime.now(ZONE).isBefore(entry.getEditWindowExpiresAt()))) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Bản nháp chưa quá hạn, giáo viên phải hoàn thiện trong thời hạn");
        }
        return mapper.apply(lifecycle.amend(entry, request));
    }

    private LessonLogEntry load(Long id) {
        return entries.findById(id)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy sổ đầu bài"));
    }

    private void assertOpenSemester(Long semesterId) {
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ"));
        if (semester.getStatus() == SemesterStatus.CLOSED || semester.getStatus() == SemesterStatus.LOCKED) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Học kỳ đã đóng");
        }
    }

    private void version(Long actualVersion, Long expectedVersion) {
        if (!Objects.equals(actualVersion, expectedVersion)) {
            throw error(HttpStatus.CONFLICT, "Dữ liệu đã thay đổi, vui lòng tải lại");
        }
    }

    private void writable(LocalDateTime lessonEndsAt, LocalDateTime editWindowExpiresAt) {
        LocalDateTime currentTime = LocalDateTime.now(ZONE);
        if (lessonEndsAt.isAfter(currentTime) || !currentTime.isBefore(editWindowExpiresAt)) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Tiết chưa kết thúc hoặc đã hết hạn chỉnh sửa");
        }
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
