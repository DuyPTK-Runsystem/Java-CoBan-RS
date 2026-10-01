package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectFunctionalRoom;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentRoomOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimetableAgentSnapshotRooms {

    private final FunctionalRoomRepository roomRepository;
    private final SubjectFunctionalRoomRepository subjectFunctionalRoomRepository;

    public RoomData read(Set<Long> subjectIds) {
        List<FunctionalRoom> activeRooms = roomRepository.findByStatus(RoomStatus.ACTIVE);
        List<TimetableAgentRoomOption> roomOptions = activeRooms.stream()
                .map(room -> new TimetableAgentRoomOption(room.getId(), room.getCode(), room.getName()))
                .sorted(Comparator.comparing(TimetableAgentRoomOption::roomId))
                .toList();
        Map<Long, List<Long>> subjectRoomIds = subjectFunctionalRoomRepository.findAll().stream()
                .filter(link -> subjectIds.contains(link.getSubjectId()))
                .filter(link -> activeRooms.stream()
                        .anyMatch(room -> Objects.equals(room.getId(), link.getFunctionalRoomId())))
                .collect(Collectors.groupingBy(SubjectFunctionalRoom::getSubjectId,
                        TreeMap::new,
                        Collectors.mapping(SubjectFunctionalRoom::getFunctionalRoomId,
                                Collectors.collectingAndThen(Collectors.toSet(),
                                        values -> values.stream().sorted().toList()))));

        return new RoomData(roomOptions, subjectRoomIds);
    }

    public record RoomData(List<TimetableAgentRoomOption> roomOptions, Map<Long, List<Long>> subjectRoomIds) {
    }
}
