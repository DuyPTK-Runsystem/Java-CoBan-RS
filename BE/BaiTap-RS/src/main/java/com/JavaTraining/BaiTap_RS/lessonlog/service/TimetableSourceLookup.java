package com.JavaTraining.BaiTap_RS.lessonlog.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevisionStatus;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableSourceLookup {
    private final TimetableEntryRepository timetableEntries;
    private final TimetableRevisionRepository timetableRevisions;
    private final TimetableHeadRepository timetableHeads;
    private final TimetableAuditRepository timetableAudits;
    private final TimetablePeriodRepository periods;

    public TimetableEntry findEntry(Long timetableEntryId) {
        return timetableEntries.findById(timetableEntryId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy tiết thời khóa biểu"));
    }

    public TimetableRevision findRevision(Long revisionId) {
        return timetableRevisions.findById(revisionId)
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "Revision nguồn không tồn tại"));
    }

    public void lockAndValidateRevision(TimetableRevision revision) {
        timetableHeads.findByIdAndSemesterIdForUpdate(revision.getTimetableId(), revision.getSemesterId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy đầu thời khóa biểu"));
        if (revision.getStatus() != TimetableRevisionStatus.PUBLISHED
                && revision.getStatus() != TimetableRevisionStatus.ARCHIVED) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Nguồn tiết chưa được công bố");
        }
        if (revision.getStatus() == TimetableRevisionStatus.ARCHIVED
                && !timetableAudits.existsByRevisionIdAndAction(revision.getId(), "PUBLISH")) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Revision lưu trữ chưa có lịch sử công bố");
        }
    }

    public void validateDate(TimetableEntry timetable, TimetableRevision revision, LocalDate lessonDate) {
        if (revision.getEffectiveFrom() == null || lessonDate.isBefore(timetable.getValidFrom())
                || lessonDate.isAfter(timetable.getValidTo()) || lessonDate.isBefore(revision.getEffectiveFrom())
                || (revision.getEffectiveTo() != null && lessonDate.isAfter(revision.getEffectiveTo()))) {
            throw error(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày không thuộc hiệu lực tiết");
        }
    }

    public TimetablePeriod findPeriod(Long periodId) {
        return periods.findById(periodId)
                .orElseThrow(() -> error(HttpStatus.UNPROCESSABLE_ENTITY, "Tiết nguồn không tồn tại"));
    }

    public void lockForEntry(LessonLogEntry entry) {
        TimetableRevision revision = timetableRevisions.findById(entry.getTimetableRevisionId())
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy revision thời khóa biểu"));
        lockHead(revision.getTimetableId(), entry.getSemesterId());
    }

    public void lockForEntries(List<LessonLogEntry> entries, Long semesterId) {
        entries.stream()
                .map(LessonLogEntry::getTimetableRevisionId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(revisionId -> {
                    TimetableRevision revision = timetableRevisions.findById(revisionId)
                            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy revision thời khóa biểu"));
                    lockHead(revision.getTimetableId(), semesterId);
                });
    }

    private void lockHead(Long timetableId, Long semesterId) {
        timetableHeads.findByIdAndSemesterIdForUpdate(timetableId, semesterId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Không tìm thấy đầu thời khóa biểu"));
    }

    private AppException error(HttpStatus status, String message) {
        return new AppException(status, message);
    }
}
