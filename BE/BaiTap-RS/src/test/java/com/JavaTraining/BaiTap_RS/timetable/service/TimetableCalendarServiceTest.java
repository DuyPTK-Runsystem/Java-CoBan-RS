package com.JavaTraining.BaiTap_RS.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.repository.SemesterRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.lessonlog.repository.LessonLogEntryRepository;
import com.JavaTraining.BaiTap_RS.timetable.domain.DTOs.requests.ReqSaveCalendarDTO;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableCalendar;
import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableHead;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableCalendarRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableClosedDateRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetableHeadRepository;
import com.JavaTraining.BaiTap_RS.timetable.repository.TimetablePeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TimetableCalendarServiceTest {

    @Mock
    private TimetableCalendarRepository calendarRepository;

    @Mock
    private TimetablePeriodRepository periodRepository;

    @Mock
    private TimetableClosedDateRepository closedDateRepository;

    @Mock
    private TimetableHeadRepository headRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private LessonLogEntryRepository lessonLogEntryRepository;

    private TimetableCalendarService service;

    @BeforeEach
    void setUp() {
        service = new TimetableCalendarService(calendarRepository, periodRepository, closedDateRepository,
                headRepository, semesterRepository, lessonLogEntryRepository);
    }

    @Test
    void saveCalendar_locksSemesterHeadBeforeReadingLessonLogs() {
        TimetableCalendar calendar = new TimetableCalendar(1L);
        ReflectionTestUtils.setField(calendar, "id", 20L);
        TimetableHead head = new TimetableHead(1L);
        ReflectionTestUtils.setField(head, "id", 10L);
        when(semesterRepository.existsById(1L)).thenReturn(true);
        when(calendarRepository.findBySemesterId(1L)).thenReturn(Optional.of(calendar));
        when(headRepository.findBySemesterId(1L)).thenReturn(Optional.of(head));
        when(headRepository.findByIdAndSemesterIdForUpdate(10L, 1L)).thenReturn(Optional.of(head));
        when(lessonLogEntryRepository.findBySemesterId(1L)).thenReturn(List.of());
        when(calendarRepository.save(calendar)).thenReturn(calendar);
        when(periodRepository.findByCalendarIdOrderByDayOfWeekAscSessionAscPeriodIndexAsc(20L)).thenReturn(List.of());
        when(closedDateRepository.findByCalendarIdOrderByClosedDateAsc(20L)).thenReturn(List.of());

        service.saveCalendar(new ReqSaveCalendarDTO(1L, null, null, null));

        InOrder order = inOrder(headRepository, lessonLogEntryRepository);
        order.verify(headRepository).findByIdAndSemesterIdForUpdate(10L, 1L);
        order.verify(lessonLogEntryRepository).findBySemesterId(1L);
    }

    @Test
    void saveCalendar_withoutSemesterHead_throwsNotFound() {
        TimetableCalendar calendar = new TimetableCalendar(1L);
        ReflectionTestUtils.setField(calendar, "id", 20L);
        when(semesterRepository.existsById(1L)).thenReturn(true);
        when(calendarRepository.findBySemesterId(1L)).thenReturn(Optional.of(calendar));
        when(headRepository.findBySemesterId(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> service.saveCalendar(new ReqSaveCalendarDTO(1L, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }
}
