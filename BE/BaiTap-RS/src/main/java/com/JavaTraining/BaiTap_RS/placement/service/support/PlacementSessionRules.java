package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqCreatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqUpdatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementIssueSeverity;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSession;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSessionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class PlacementSessionRules {
    public Map<Long, PlacementClassProfile> createProfiles(
            List<ReqCreatePlacementSessionDTO.TargetClass> targetClasses) {
        return targetClasses.stream().collect(Collectors.toMap(ReqCreatePlacementSessionDTO.TargetClass::classId,
                ReqCreatePlacementSessionDTO.TargetClass::profile));
    }

    public Map<Long, PlacementClassProfile> updateProfiles(
            List<ReqUpdatePlacementSessionDTO.TargetClass> targetClasses) {
        return targetClasses.stream().collect(Collectors.toMap(ReqUpdatePlacementSessionDTO.TargetClass::classId,
                ReqUpdatePlacementSessionDTO.TargetClass::profile));
    }

    public void validateCandidates(ReqCreatePlacementSessionDTO request) {
        long distinctStudents = request.candidates().stream()
                .map(ReqCreatePlacementSessionDTO.Candidate::studentId).distinct().count();
        if (distinctStudents != request.candidates().size()) {
            throw new AppException(HttpStatus.CONFLICT, "Danh sách ứng viên bị trùng");
        }
        if (request.candidates().stream()
                .anyMatch(candidate -> !Objects.equals(candidate.targetGradeId(), request.targetGradeId()))) {
            throw new AppException(HttpStatus.CONFLICT, "Ứng viên không thuộc khối xếp lớp");
        }
    }

    public void checkVersion(PlacementSession session, Long expectedVersion) {
        if (!Objects.equals(session.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT, "Phiên xếp lớp đã thay đổi, vui lòng tải lại");
        }
    }

    public void require(PlacementSession session, PlacementSessionStatus... allowed) {
        for (PlacementSessionStatus status : allowed) {
            if (session.getStatus() == status) {
                return;
            }
        }
        throw new AppException(HttpStatus.CONFLICT, "Trạng thái phiên không cho phép thao tác");
    }

    public void requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Khóa xác nhận là bắt buộc");
        }
    }

    public boolean hasBlockingIssue(List<PlacementResult> values) {
        return values.stream().anyMatch(value -> value.getIssueSeverity() == PlacementIssueSeverity.BLOCKING);
    }

    public void requireNoBlockingIssue(List<PlacementResult> values) {
        if (hasBlockingIssue(values)) {
            throw new AppException(HttpStatus.CONFLICT, "Phiên còn lỗi vượt sức chứa");
        }
    }
}
