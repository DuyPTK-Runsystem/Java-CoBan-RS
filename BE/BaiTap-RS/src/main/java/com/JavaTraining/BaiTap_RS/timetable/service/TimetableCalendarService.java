package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqClosedDateDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqPeriodDefinitionDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqSaveCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResClosedDateDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.response.ResPeriodDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableClosedDate;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimetableCalendarService {

        private final TimetableCalendarRepository calendarRepository;
        private final TimetablePeriodRepository periodRepository;
        private final TimetableClosedDateRepository closedDateRepository;
        private final SemesterRepository semesterRepository;

        @Transactional
        public ResCalendarDTO getOrCreateCalendar(Long semesterId) {
                if (!semesterRepository.existsById(semesterId)) {
                        throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ");
                }
                TimetableCalendar calendar = calendarRepository.findBySemesterId(semesterId)
                                .orElseGet(() -> createDefaultCalendar(semesterId));
                return toCalendarDTO(calendar);
        }

        @Transactional(readOnly = true)
        public ResCalendarDTO getCalendar(Long semesterId) {
                TimetableCalendar calendar = calendarRepository.findBySemesterId(semesterId)
                                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                                                "Chưa thiết lập lịch cho học kỳ này"));
                return toCalendarDTO(calendar);
        }

        @Transactional
        public ResCalendarDTO saveCalendar(ReqSaveCalendarDTO req) {
                if (!semesterRepository.existsById(req.semesterId())) {
                        throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy học kỳ");
                }
                TimetableCalendar calendar = calendarRepository.findBySemesterId(req.semesterId())
                                .orElseGet(() -> calendarRepository.save(new TimetableCalendar(req.semesterId())));

                if (req.expectedVersion() != null && !Objects.equals(calendar.getVersion(), req.expectedVersion())) {
                        throw new AppException(HttpStatus.CONFLICT,
                                        "Lịch học kỳ đã bị cập nhật bởi người khác. Vui lòng tải lại.");
                }

                if (req.periods() != null && !req.periods().isEmpty()) {
                        savePeriods(calendar.getId(), req.periods());
                }

                if (req.closedDates() != null) {
                        saveClosedDates(calendar.getId(), req.closedDates());
                }

                calendar = calendarRepository.save(calendar);
                return toCalendarDTO(calendar);
        }

        private void savePeriods(Long calendarId, List<ReqPeriodDefinitionDTO> periodDtos) {
                periodRepository.deleteByCalendarId(calendarId);
                List<TimetablePeriod> periods = new ArrayList<>();
                for (ReqPeriodDefinitionDTO p : periodDtos) {
                        periods.add(new TimetablePeriod(
                                        calendarId,
                                        p.dayOfWeek(),
                                        p.session(),
                                        p.periodIndex(),
                                        p.name(),
                                        p.startTime(),
                                        p.endTime()));
                }
                periodRepository.saveAll(periods);
        }

        private void saveClosedDates(Long calendarId, List<ReqClosedDateDTO> closedDateDtos) {
                closedDateRepository.deleteByCalendarId(calendarId);
                List<TimetableClosedDate> closedDates = closedDateDtos.stream()
                                .map(c -> new TimetableClosedDate(calendarId, c.closedDate(), c.reason()))
                                .toList();
                closedDateRepository.saveAll(closedDates);
        }

        private TimetableCalendar createDefaultCalendar(Long semesterId) {
                TimetableCalendar calendar = calendarRepository.save(new TimetableCalendar(semesterId));
                List<TimetablePeriod> defaultPeriods = new ArrayList<>();
                // 6 days: Monday (2) to Saturday (7)
                for (int day = 2; day <= 7; day++) {
                        // Morning: 4 periods
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.MORNING, 1,
                                        "Sáng Tiết 1", LocalTime.of(7, 0), LocalTime.of(7, 45)));
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.MORNING, 2,
                                        "Sáng Tiết 2", LocalTime.of(7, 50), LocalTime.of(8, 35)));
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.MORNING, 3,
                                        "Sáng Tiết 3", LocalTime.of(8, 50), LocalTime.of(9, 35)));
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.MORNING, 4,
                                        "Sáng Tiết 4", LocalTime.of(9, 40), LocalTime.of(10, 25)));
                        // Afternoon: 4 periods
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.AFTERNOON, 1,
                                        "Chiều Tiết 1", LocalTime.of(13, 0), LocalTime.of(13, 45)));
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.AFTERNOON, 2,
                                        "Chiều Tiết 2", LocalTime.of(13, 50), LocalTime.of(14, 35)));
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.AFTERNOON, 3,
                                        "Chiều Tiết 3", LocalTime.of(14, 50), LocalTime.of(15, 35)));
                        defaultPeriods.add(new TimetablePeriod(calendar.getId(), day, SessionType.AFTERNOON, 4,
                                        "Chiều Tiết 4", LocalTime.of(15, 40), LocalTime.of(16, 25)));
                }
                periodRepository.saveAll(defaultPeriods);
                return calendar;
        }

        private ResCalendarDTO toCalendarDTO(TimetableCalendar calendar) {
                List<ResPeriodDTO> periods = periodRepository
                                .findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(calendar.getId())
                                .stream()
                                .map(p -> new ResPeriodDTO(
                                                p.getId(),
                                                p.getCalendarId(),
                                                p.getDayOfWeek(),
                                                p.getSession(),
                                                p.getPeriodIndex(),
                                                p.getName(),
                                                p.getStartTime(),
                                                p.getEndTime()))
                                .toList();

                List<ResClosedDateDTO> closedDates = closedDateRepository
                                .findByCalendarIdOrderByClosedDateAsc(calendar.getId())
                                .stream()
                                .map(c -> new ResClosedDateDTO(c.getId(), c.getCalendarId(), c.getClosedDate(),
                                                c.getReason()))
                                .toList();

                return new ResCalendarDTO(calendar.getId(), calendar.getSemesterId(), calendar.getVersion(),
                                periods, closedDates);
        }
}
