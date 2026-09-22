package com.JavaTraining.BaiTap_RS.bootstrap;

import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableAuditRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableRevisionRepository;
import org.springframework.stereotype.Component;

@Component
public class DemoTimetablePersistenceGateway {

    private final TimetableCalendarRepository calendarRepo;
    private final TimetablePeriodRepository periodRepo;
    private final TimetableHeadRepository headRepo;
    private final TimetableRevisionRepository revisionRepo;
    private final TimetableEntryRepository entryRepo;
    private final TimetableAuditRepository auditRepo;

    public DemoTimetablePersistenceGateway(
            TimetableCalendarRepository calendarRepository,
            TimetablePeriodRepository periodRepository,
            TimetableHeadRepository headRepository,
            TimetableRevisionRepository revisionRepository,
            TimetableEntryRepository entryRepository,
            TimetableAuditRepository auditRepository) {
        this.calendarRepo = calendarRepository;
        this.periodRepo = periodRepository;
        this.headRepo = headRepository;
        this.revisionRepo = revisionRepository;
        this.entryRepo = entryRepository;
        this.auditRepo = auditRepository;
    }

    public TimetableCalendarRepository calendarRepository() {
        return calendarRepo;
    }

    public TimetablePeriodRepository periodRepository() {
        return periodRepo;
    }

    public TimetableHeadRepository headRepository() {
        return headRepo;
    }

    public TimetableRevisionRepository revisionRepository() {
        return revisionRepo;
    }

    public TimetableEntryRepository entryRepository() {
        return entryRepo;
    }

    public TimetableAuditRepository auditRepository() {
        return auditRepo;
    }
}
