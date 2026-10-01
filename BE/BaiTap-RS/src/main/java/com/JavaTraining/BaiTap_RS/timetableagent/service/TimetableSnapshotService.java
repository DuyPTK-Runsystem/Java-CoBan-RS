package com.JavaTraining.BaiTap_RS.timetableagent.service;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherLoadPolicy;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableSnapshotService {

    private final TimetableAgentSnapshotEntries entries;
    private final TimetableAgentSnapshotCatalog catalog;
    private final TimetableAgentSnapshotCalendar calendar;
    private final TimetableAgentSnapshotPolicy policy;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
    public TimetableAgentSnapshot create(Long actorId, ReqCreateTimetableAgentProposalDTO request) {
        TimetableRevision revision = entries.revision(request);
        TeacherLoadPolicy pinnedPolicy = policy.requireCurrentPinnedPolicy(revision);
        TimetableAgentSnapshotCatalog.Catalog catalogData = catalog.read(revision, request);
        TimetableAgentSnapshotCalendar.CalendarData calendarData = calendar.read(revision, request, catalogData);
        TimetableAgentSnapshotEntries.EntryData entryData = entries.read(revision, request, catalogData);
        TimetableAgentSnapshotPolicy.PolicyData policyData = policy.read(revision, request, pinnedPolicy,
                catalogData.teacherIds(), entryData);
        return TimetableAgentSnapshotDocument.build(actorId, request, revision, catalogData, calendarData,
                entryData, policyData, objectMapper);
    }
}
