package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResTeacherLoadDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.service.TeacherLoadEvaluator;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.service.TimetableAgentSnapshotPolicy.TimetableAgentWeeklyLoad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotLoads {

    private final TeacherLoadEvaluator teacherLoadEvaluator;

    public List<TimetableAgentWeeklyLoad> computedLoadSnapshot(TimetableRevision revision,
            ReqCreateTimetableAgentProposalDTO request, List<TimetableAgentSnapshotEntry> current,
            List<TimetableAgentSnapshotEntry> publishedContext) {
        List<TimetableEntry> entries = new ArrayList<>();
        long syntheticId = -500_000L;
        for (TimetableAgentSnapshotEntry item : current) {
            entries.add(snapshotEntity(revision.getId(), item, syntheticId--));
        }
        for (TimetableAgentSnapshotEntry item : publishedContext) {
            entries.add(snapshotEntity(revision.getId(), item, syntheticId--));
        }
        List<TimetableAgentWeeklyLoad> result = new ArrayList<>();
        LocalDate weekStart = request.validFrom().with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY));
        while (!weekStart.isAfter(request.validTo())) {
            for (ResTeacherLoadDTO load : teacherLoadEvaluator.evaluateLoads(
                    TimetableAgentOccupiedContext.normalizedEntities(entries), weekStart, weekStart.plusDays(6),
                    revision.getPolicyId())) {
                result.add(new TimetableAgentWeeklyLoad(load.teacherId(), weekStart, load.assignedPeriods(),
                        load.targetPeriods(), load.basePeriods(), load.reductions(), load.evaluationStatus()));
            }
            weekStart = weekStart.plusWeeks(1);
        }
        return List.copyOf(result);
    }

    private TimetableEntry snapshotEntity(Long revisionId, TimetableAgentSnapshotEntry item, long id) {
        TimetableEntry entry = new TimetableEntry(revisionId, item.assignmentId(), item.periodId(),
                item.functionalRoomId(), item.validFrom(), item.validTo());
        entry.setId(id);
        return entry;
    }

}
