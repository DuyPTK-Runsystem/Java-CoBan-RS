package com.JavaTraining.BaiTap_RS.timetable.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqClosedDateDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableClosedDate;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableCalendarClosedDateService {

    private final TimetableClosedDateRepository closedDateRepository;

    public void guardChanges(Long calendarId, List<ReqClosedDateDTO> requestedDates,
            List<LessonLogEntry> loggedEntries) {
        Set<LocalDate> existingDates = findClosedDates(calendarId).stream().map(TimetableClosedDate::getClosedDate)
                .collect(Collectors.toSet());
        Set<LocalDate> requestedClosedDates = requestedDates.stream().map(ReqClosedDateDTO::closedDate)
                .collect(Collectors.toSet());
        loggedEntries.stream().map(LessonLogEntry::getLessonDate)
                .filter(date -> existingDates.contains(date) != requestedClosedDates.contains(date)).findFirst()
                .ifPresent(date -> {
                    throw new AppException(HttpStatus.CONFLICT,
                            "Không thể thay đổi trạng thái ngày đã có sổ đầu bài: " + date);
                });
    }

    public List<TimetableClosedDate> findClosedDates(Long calendarId) {
        return closedDateRepository.findByCalendarIdOrderByClosedDateAsc(calendarId);
    }

    public void saveClosedDates(Long calendarId, List<ReqClosedDateDTO> requestedDates) {
        closedDateRepository.deleteByCalendarId(calendarId);
        List<TimetableClosedDate> closedDates = requestedDates.stream()
                .map(date -> new TimetableClosedDate(calendarId, date.closedDate(), date.reason())).toList();
        closedDateRepository.saveAll(closedDates);
    }
}
