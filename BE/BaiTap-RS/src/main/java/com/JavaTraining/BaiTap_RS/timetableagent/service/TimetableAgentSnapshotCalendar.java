package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TeacherUnavailability;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableClosedDate;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableRevision;
import com.JavaTraining.BaiTap_RS.timetable.repository.TeacherUnavailabilityRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.DTOs.requests.ReqCreateTimetableAgentProposalDTO;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentAvailabilityOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentPeriodOption;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentRoomOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotCalendar {

    private final TimetableCalendarRepository calendarRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableClosedDateRepository closedDateRepository;
    private final TimetableAgentSnapshotRooms rooms;
    private final TeacherUnavailabilityRepository unavailabilityRepository;

    public CalendarData read(TimetableRevision revision, ReqCreateTimetableAgentProposalDTO request,
            TimetableAgentSnapshotCatalog.Catalog catalog) {
        TimetableCalendar calendar = calendarRepository.findBySemesterId(revision.getSemesterId())
                .orElseThrow(() -> invalid("The target semester has no timetable calendar."));
        List<TimetableAgentPeriodOption> periodOptions = periodRepository
                .findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(calendar.getId()).stream()
                .map(this::periodOption)
                .toList();
        if (periodOptions.isEmpty()) {
            throw invalid("The target semester has no configured timetable periods.");
        }

        TimetableAgentSnapshotRooms.RoomData roomData = rooms.read(catalog.subjectIds());

        List<TimetableAgentAvailabilityOption> availability = unavailabilityRepository
                .findApprovedInSemester(revision.getSemesterId(), request.validFrom(), request.validTo()).stream()
                .filter(item -> catalog.teacherIds().contains(item.getTeacherId()))
                .map(this::availabilityOption)
                .sorted(Comparator.comparing(TimetableAgentAvailabilityOption::teacherId)
                        .thenComparing(TimetableAgentAvailabilityOption::validFrom))
                .toList();
        List<LocalDate> closedDates = closedDateRepository.findByCalendarIdOrderByClosedDateAsc(calendar.getId())
                .stream()
                .map(TimetableClosedDate::getClosedDate)
                .filter(date -> !date.isBefore(request.validFrom()) && !date.isAfter(request.validTo()))
                .toList();

        return new CalendarData(calendar, periodOptions, roomData.roomOptions(), roomData.subjectRoomIds(),
                availability, closedDates);
    }

    private TimetableAgentPeriodOption periodOption(TimetablePeriod period) {
        return new TimetableAgentPeriodOption(period.getId(), period.getDayOfWeek(),
                period.getSession() == null ? null : period.getSession().name(),
                period.getPeriodIndex(), period.getName(), period.getStartTime().toString(),
                period.getEndTime().toString());
    }

    private TimetableAgentAvailabilityOption availabilityOption(TeacherUnavailability item) {
        String storedIndexes = item.getPeriodIndexes();
        List<Integer> indexes = storedIndexes == null || storedIndexes.isBlank() ? List.of()
                : java.util.Arrays.stream(storedIndexes.split(","))
                        .map(String::trim).filter(value -> !value.isEmpty()).map(Integer::parseInt).sorted().toList();
        return new TimetableAgentAvailabilityOption(item.getTeacherId(), item.getDayOfWeek(),
                item.getSpecificDate() == null ? null : item.getSpecificDate().toString(), item.getValidFrom(),
                item.getValidTo(), item.getSession() == null ? null : item.getSession().name(), indexes);
    }

    private AppException invalid(String message) {
        return new AppException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public record CalendarData(TimetableCalendar calendar, List<TimetableAgentPeriodOption> periodOptions,
            List<TimetableAgentRoomOption> roomOptions, Map<Long, List<Long>> subjectRoomIds,
            List<TimetableAgentAvailabilityOption> availability, List<LocalDate> closedDates) {
    }
}
