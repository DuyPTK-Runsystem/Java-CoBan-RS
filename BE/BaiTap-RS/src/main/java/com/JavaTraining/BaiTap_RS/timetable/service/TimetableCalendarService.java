package com.JavaTraining.BaiTap_RS.timetable.service;

import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqSaveCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimetableCalendarService {
        private final TimetableCalendarRepository calendarRepository;
        private final TimetableHeadRepository headRepository;
        private final SemesterRepository semesterRepository;
        private final LessonLogEntryRepository lessonLogEntryRepository;
        private final TimetableCalendarPeriodService periodService;
        private final TimetableCalendarClosedDateService closedDateService;
        private final TimetableCalendarViewMapper viewMapper;
        private final TimetableCalendarDefaultService defaultService;

        @Autowired
        public TimetableCalendarService(TimetableCalendarRepository calendarRepository,
                        TimetableHeadRepository headRepository, SemesterRepository semesterRepository,
                        LessonLogEntryRepository lessonLogEntryRepository,
                        TimetableCalendarPeriodService periodService,
                        TimetableCalendarClosedDateService closedDateService,
                        TimetableCalendarViewMapper viewMapper, TimetableCalendarDefaultService defaultService) {
                this.calendarRepository = calendarRepository;
                this.headRepository = headRepository;
                this.semesterRepository = semesterRepository;
                this.lessonLogEntryRepository = lessonLogEntryRepository;
                this.periodService = periodService;
                this.closedDateService = closedDateService;
                this.viewMapper = viewMapper;
                this.defaultService = defaultService;
        }

        public TimetableCalendarService(TimetableCalendarRepository calendarRepository,
                        TimetablePeriodRepository periodRepository,
                        TimetableClosedDateRepository closedDateRepository, TimetableHeadRepository headRepository,
                        SemesterRepository semesterRepository, LessonLogEntryRepository lessonLogEntryRepository) {
                this.calendarRepository = calendarRepository;
                this.headRepository = headRepository;
                this.semesterRepository = semesterRepository;
                this.lessonLogEntryRepository = lessonLogEntryRepository;
                this.periodService = new TimetableCalendarPeriodService(periodRepository);
                this.closedDateService = new TimetableCalendarClosedDateService(closedDateRepository);
                this.viewMapper = new TimetableCalendarViewMapper(this.periodService, this.closedDateService);
                this.defaultService = new TimetableCalendarDefaultService(calendarRepository, periodRepository);
        }

        @Transactional
        public ResCalendarDTO getOrCreateCalendar(Long semesterId) {
                if (!semesterRepository.existsById(semesterId)) {
                        throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ");
                }
                TimetableCalendar calendar = calendarRepository.findBySemesterId(semesterId)
                                .orElseGet(() -> defaultService.create(semesterId));
                return viewMapper.toResponse(calendar);
        }

        @Transactional(readOnly = true)
        public ResCalendarDTO getCalendar(Long semesterId) {
                TimetableCalendar calendar = calendarRepository.findBySemesterId(semesterId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                                                "Chưa thiết lập lịch cho học kỳ này"));
                return viewMapper.toResponse(calendar);
        }

        @Transactional
        public ResCalendarDTO saveCalendar(ReqSaveCalendarDTO req) {
                if (!semesterRepository.existsById(req.semesterId())) {
                        throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ");
                }
                TimetableCalendar calendar = calendarRepository.findBySemesterId(req.semesterId())
                                .orElseGet(() -> calendarRepository.save(new TimetableCalendar(req.semesterId())));

                lockTimetableHead(req.semesterId());

                if (req.expectedVersion() != null && !Objects.equals(calendar.getVersion(), req.expectedVersion())) {
                        throw new AppException(HttpStatus.CONFLICT,
                                        "Lịch học kỳ đã bị cập nhật bởi người khác. Vui lòng tải lại.");
                }

                List<LessonLogEntry> logged = lessonLogEntryRepository.findBySemesterId(req.semesterId());
                if (req.periods() != null && !req.periods().isEmpty()) {
                        periodService.savePeriods(calendar, req.periods(), logged);
                }

                if (req.closedDates() != null) {
                        closedDateService.guardChanges(calendar.getId(), req.closedDates(), logged);
                        closedDateService.saveClosedDates(calendar.getId(), req.closedDates());
                }

                calendar = calendarRepository.save(calendar);
                return viewMapper.toResponse(calendar);
        }

        private void lockTimetableHead(Long semesterId) {
                TimetableHead head = headRepository.findBySemesterId(semesterId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                                                "Không tìm thấy đầu thời khóa biểu"));
                headRepository.findByIdAndSemesterIdForUpdate(head.getId(), semesterId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                                                "Không tìm thấy đầu thời khóa biểu"));
        }

}
