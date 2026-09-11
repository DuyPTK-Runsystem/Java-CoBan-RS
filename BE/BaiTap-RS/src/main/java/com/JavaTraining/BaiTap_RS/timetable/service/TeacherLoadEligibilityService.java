package com.JavaTraining.BaiTap_RS.timetable.service;

import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.teacher.domain.entity.Teacher;
import com.JavaTraining.BaiTap_RS.teacher.repository.TeacherRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqCreateEligibilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqUpdateEligibilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadEligibilityDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadEligibility;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherLoadEligibilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherLoadEligibilityService {

    private final TeacherLoadEligibilityRepository eligibilityRepository;
    private final TeacherRepository teacherRepository;

    @Transactional
    public ResTeacherLoadEligibilityDTO create(ReqCreateEligibilityDTO req) {
        Teacher teacher = teacherRepository.findById(req.teacherId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy giáo viên"));

        if (req.validTo().isBefore(req.validFrom())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "validTo phải sau hoặc bằng validFrom");
        }

        TeacherLoadEligibility record = new TeacherLoadEligibility(
                req.teacherId(),
                req.ruleCode(),
                req.validFrom(),
                req.validTo(),
                req.evidenceReference());
        record = eligibilityRepository.save(record);
        return toDTO(record, teacher.getTeacherName());
    }

    @Transactional
    public ResTeacherLoadEligibilityDTO update(Long id, ReqUpdateEligibilityDTO req) {
        TeacherLoadEligibility record = eligibilityRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ miễn giảm"));

        if (req.expectedVersion() != null && !Objects.equals(record.getVersion(), req.expectedVersion())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Hồ sơ đã bị thay đổi bởi người khác. Vui lòng tải lại.");
        }

        applyUpdates(record, req);

        record = eligibilityRepository.save(record);
        String teacherName = teacherRepository.findById(record.getTeacherId())
                .map(Teacher::getTeacherName).orElse("N/A");
        return toDTO(record, teacherName);
    }

    private void applyUpdates(TeacherLoadEligibility record, ReqUpdateEligibilityDTO req) {
        if (req.validFrom() != null) {
            record.setValidFrom(req.validFrom());
        }
        if (req.validTo() != null) {
            record.setValidTo(req.validTo());
        }
        if (record.getValidTo().isBefore(record.getValidFrom())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "validTo phải sau hoặc bằng validFrom");
        }
        if (req.evidenceReference() != null) {
            record.setEvidenceReference(req.evidenceReference());
        }
        if (req.status() != null) {
            record.setStatus(req.status());
        }
    }

    @Transactional(readOnly = true)
    public List<ResTeacherLoadEligibilityDTO> listByTeacher(Long teacherId) {
        List<TeacherLoadEligibility> list = teacherId != null
                ? eligibilityRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                : eligibilityRepository.findAll();

        return list.stream()
                .map(e -> {
                    String teacherName = teacherRepository.findById(e.getTeacherId())
                            .map(Teacher::getTeacherName).orElse("N/A");
                    return toDTO(e, teacherName);
                })
                .toList();
    }

    private ResTeacherLoadEligibilityDTO toDTO(TeacherLoadEligibility e, String teacherName) {
        return new ResTeacherLoadEligibilityDTO(
                e.getId(),
                e.getTeacherId(),
                teacherName,
                e.getRuleCode(),
                e.getValidFrom(),
                e.getValidTo(),
                e.getEvidenceReference(),
                e.getStatus(),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
