package com.JavaTraining.BaiTap_RS.timetable.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.domain.entity.LessonLogEntry;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqPeriodDefinitionDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.SessionType;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetablePeriod;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableCalendarPeriodService {

    private final TimetablePeriodRepository periodRepository;

    public void savePeriods(TimetableCalendar calendar, List<ReqPeriodDefinitionDTO> periodDefinitions,
            List<LessonLogEntry> loggedEntries) {
        Map<String, TimetablePeriod> existingPeriods = existingPeriods(calendar.getId());
        Map<String, ReqPeriodDefinitionDTO> requestedPeriods = requestedPeriods(periodDefinitions);
        rejectLoggedPeriodChanges(existingPeriods, requestedPeriods, loggedEntries);
        saveRequestedPeriods(calendar, requestedPeriods, existingPeriods);
        deleteRemovedPeriods(existingPeriods, requestedPeriods.keySet());
    }

    public List<TimetablePeriod> findPeriods(Long calendarId) {
        return periodRepository.findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(calendarId);
    }

    private Map<String, TimetablePeriod> existingPeriods(Long calendarId) {
        Map<String, TimetablePeriod> periods = new HashMap<>();
        findPeriods(calendarId).forEach(period -> periods.put(periodKey(period.getDayOfWeek(), period.getSession(),
                period.getPeriodIndex()), period));
        return periods;
    }

    private Map<String, ReqPeriodDefinitionDTO> requestedPeriods(List<ReqPeriodDefinitionDTO> definitions) {
        Map<String, ReqPeriodDefinitionDTO> periods = new HashMap<>();
        definitions.forEach(definition -> periods.put(periodKey(definition.dayOfWeek(), definition.session(),
                definition.periodIndex()), definition));
        return periods;
    }

    private void rejectLoggedPeriodChanges(Map<String, TimetablePeriod> existingPeriods,
            Map<String, ReqPeriodDefinitionDTO> requestedPeriods, List<LessonLogEntry> loggedEntries) {
        Set<String> loggedPeriodKeys = loggedEntries.stream().map(this::loggedPeriodKey).collect(Collectors.toSet());
        for (Map.Entry<String, TimetablePeriod> existingEntry : existingPeriods.entrySet()) {
            ReqPeriodDefinitionDTO requested = requestedPeriods.get(existingEntry.getKey());
            if ((requested == null || !sameDefinition(existingEntry.getValue(), requested))
                    && loggedPeriodKeys.contains(existingEntry.getKey())) {
                throw new AppException(HttpStatus.CONFLICT,
                        "Không thể thay đổi tiết đã có sổ đầu bài: " + existingEntry.getKey());
            }
        }
    }

    private void saveRequestedPeriods(TimetableCalendar calendar, Map<String, ReqPeriodDefinitionDTO> requestedPeriods,
            Map<String, TimetablePeriod> existingPeriods) {
        List<TimetablePeriod> periodsToSave = new ArrayList<>();
        requestedPeriods.forEach((key, requested) -> {
            TimetablePeriod period = existingPeriods.get(key);
            if (period == null) {
                period = new TimetablePeriod(calendar.getId(), requested.dayOfWeek(), requested.session(),
                        requested.periodIndex(), requested.name(), requested.startTime(), requested.endTime());
            } else {
                period.setName(requested.name());
                period.setStartTime(requested.startTime());
                period.setEndTime(requested.endTime());
            }
            periodsToSave.add(period);
        });
        periodRepository.saveAll(periodsToSave);
    }

    private void deleteRemovedPeriods(Map<String, TimetablePeriod> existingPeriods, Set<String> retainedKeys) {
        existingPeriods.forEach((key, period) -> {
            if (!retainedKeys.contains(key)) {
                periodRepository.delete(period);
            }
        });
    }

    private boolean sameDefinition(TimetablePeriod existing, ReqPeriodDefinitionDTO requested) {
        return Objects.equals(existing.getName(), requested.name())
                && Objects.equals(existing.getStartTime(), requested.startTime())
                && Objects.equals(existing.getEndTime(), requested.endTime());
    }

    private String loggedPeriodKey(LessonLogEntry entry) {
        return periodKey(entry.getLessonDate().getDayOfWeek().getValue(), SessionType.valueOf(entry.getSession()),
                entry.getPeriodIndex());
    }

    private String periodKey(Integer dayOfWeek, SessionType session, Integer periodIndex) {
        return dayOfWeek + "|" + session + "|" + periodIndex;
    }
}
