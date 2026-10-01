package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqTimetableAgentDemandDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAssignmentOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAvailabilityOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentRoomOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TeacherLoadPolicySnapshot;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentHomeroomPeriod;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentLoadEligibility;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentLoadRule;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentWeeklyLoad;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentWeeklyTarget;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class TimetableAgentSnapshotDocument {

    private TimetableAgentSnapshotDocument() {
    }

    public static TimetableAgentSnapshot build(Long actorId, ReqCreateTimetableAgentProposalDTO request,
            TimetableRevision revision, TimetableAgentSnapshotCatalog.Catalog catalog,
            TimetableAgentSnapshotCalendar.CalendarData calendar, TimetableAgentSnapshotEntries.EntryData entries,
            TimetableAgentSnapshotPolicy.PolicyData policy, ObjectMapper objectMapper) {
        TimetableAgentPayloadCodec codec = new TimetableAgentPayloadCodec(objectMapper);
        String snapshotId = UUID.randomUUID().toString();
        SnapshotSource source = new SnapshotSource(revision.getId(), revision.getSemesterId(), revision.getVersion(),
                policy.policy(), policy.policyRules(), policy.weeklyTargets(),
                calendar.calendar().getId(), calendar.calendar().getVersion(), request.validFrom(), request.validTo(),
                request.classIds().stream().sorted().toList(), request.demands().stream()
                        .sorted(Comparator.comparing(ReqTimetableAgentDemandDTO::assignmentId)).toList(),
                request.lockedEntryIds() == null ? List.of() : request.lockedEntryIds().stream().sorted().toList(),
                catalog.assignmentOptions(), calendar.periodOptions(), calendar.roomOptions(),
                calendar.subjectRoomIds(),
                calendar.availability(), calendar.closedDates(),
                entries.current(), entries.published());
        String sourceJson = codec.write(source);
        String fingerprint = TimetableAgentPayloadCodec.hash(codec.write(new SnapshotFingerprintSource(sourceJson,
                policy.eligibility(), policy.homeroom(), policy.loads())));
        String snapshotJson = codec.write(new SnapshotPayload(snapshotId, source));
        return new TimetableAgentSnapshot(snapshotId, fingerprint, sourceJson, snapshotJson,
                codec.write(request), actorId, revision.getId(), revision.getSemesterId(), request.expectedVersion(),
                request.validFrom(), request.validTo(), request.classIds(), request.demands(),
                request.lockedEntryIds() == null ? List.of() : request.lockedEntryIds(), entries.current(),
                entries.published(),
                catalog.assignmentOptions(), calendar.periodOptions(), calendar.roomOptions(), calendar.availability(),
                calendar.closedDates(), calendar.subjectRoomIds());
    }

    private record SnapshotSource(Long targetRevisionId, Long semesterId, Long revisionVersion,
            TeacherLoadPolicySnapshot policy, List<TimetableAgentLoadRule> policyRules,
            List<TimetableAgentWeeklyTarget> computedWeeklyTargets,
            Long calendarId, Long calendarVersion, LocalDate validFrom, LocalDate validTo,
            List<Long> classIds, List<ReqTimetableAgentDemandDTO> demands,
            List<Long> lockedEntryIds, List<TimetableAgentAssignmentOption> assignments,
            List<TimetableAgentPeriodOption> periods, List<TimetableAgentRoomOption> rooms,
            Map<Long, List<Long>> subjectRoomIds, List<TimetableAgentAvailabilityOption> approvedAvailability,
            List<LocalDate> closedDates, List<TimetableAgentSnapshotEntry> currentEntries,
            List<TimetableAgentSnapshotEntry> publishedContextEntries) {
    }

    private record SnapshotFingerprintSource(String safeSource, List<TimetableAgentLoadEligibility> eligibility,
            List<TimetableAgentHomeroomPeriod> homeroom, List<TimetableAgentWeeklyLoad> computedLoads) {
    }

    private record SnapshotPayload(String snapshotId, SnapshotSource data) {
    }
}
