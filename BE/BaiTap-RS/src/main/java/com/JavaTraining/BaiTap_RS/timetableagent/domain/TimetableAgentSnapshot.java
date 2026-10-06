package com.JavaTraining.BaiTap_RS.timetableagent.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;

public record TimetableAgentSnapshot(
        String snapshotId,
        String sourceFingerprint,
        String sourceJson,
        String snapshotJson,
        String requestJson,
        Long actorId,
        Long targetRevisionId,
        Long semesterId,
        Long expectedVersion,
        LocalDate validFrom,
        LocalDate validTo,
        List<Long> classIds,
        List<ReqTimetableAgentDemandDTO> demands,
        List<Long> lockedEntryIds,
        List<TimetableAgentSnapshotEntry> currentEntries,
        List<TimetableAgentSnapshotEntry> publishedContextEntries,
        List<TimetableAgentAssignmentOption> assignments,
        List<TimetableAgentPeriodOption> periods,
        List<TimetableAgentRoomOption> rooms,
        List<TimetableAgentAvailabilityOption> approvedAvailability,
        List<LocalDate> closedDates,
        Map<Long, List<Long>> subjectRoomIds) {

    public record TimetableAgentSnapshotEntry(
            Long entryId,
            Long assignmentId,
            Long periodId,
            Long functionalRoomId,
            LocalDate validFrom,
            LocalDate validTo) {
    }

    public record TimetableAgentAssignmentOption(
            Long assignmentId,
            Long classId,
            String className,
            Long subjectId,
            String subjectName,
            Long teacherId,
            String teacherName,
            LocalDate validFrom,
            LocalDate validTo,
            Integer periodsPerWeek) {
    }

    public record TimetableAgentPeriodOption(
            Long periodId,
            Integer dayOfWeek,
            String session,
            Integer periodIndex,
            String name,
            String startTime,
            String endTime) {
    }

    public record TimetableAgentRoomOption(Long roomId, String code, String name) {
    }

    public record TimetableAgentAvailabilityOption(
            Long teacherId,
            Integer dayOfWeek,
            String specificDate,
            LocalDate validFrom,
            LocalDate validTo,
            String session,
            List<Integer> periodIndexes) {
    }
}
