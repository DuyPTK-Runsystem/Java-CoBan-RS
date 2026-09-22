package com.JavaTraining.BaiTap_RS.bootstrap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.academic.domain.entity.Subject;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectFunctionalRoom;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoSubjectFunctionalRoomSeeder {

    private static final Map<String, List<String>> ROOM_MAPPINGS = Map.of(
            "TIN_HOC", List.of("TIN-1", "TIN-2"),
            "NGHE_DIEN", List.of("NGHE-1"),
            "NGHE_NONG_NGHIEP", List.of("NGHE-1"));

    private final FunctionalRoomRepository roomRepository;
    private final SubjectFunctionalRoomRepository mappingRepository;

    public DemoSubjectFunctionalRoomSeeder(
            FunctionalRoomRepository roomRepository,
            SubjectFunctionalRoomRepository mappingRepository) {
        this.roomRepository = roomRepository;
        this.mappingRepository = mappingRepository;
    }

    @Transactional
    public void seed(List<Subject> subjects) {
        Map<String, Subject> subjectsByCode = subjects.stream()
                .collect(Collectors.toMap(Subject::getCode, subject -> subject));
        Map<String, FunctionalRoom> roomsByCode = roomRepository.findAll().stream()
                .filter(room -> ROOM_MAPPINGS.values().stream()
                        .flatMap(List::stream)
                        .anyMatch(code -> code.equals(room.getCode())))
                .collect(Collectors.toMap(FunctionalRoom::getCode, room -> room));
        ROOM_MAPPINGS.forEach((subjectCode, roomCodes) -> {
            Subject subject = subjectsByCode.get(subjectCode);
            if (subject == null) {
                throw new IllegalStateException("Missing subject for room mapping: " + subjectCode);
            }
            roomCodes.forEach(roomCode -> {
                FunctionalRoom room = roomsByCode.get(roomCode);
                if (room == null) {
                    throw new IllegalStateException("Missing functional room for mapping: " + roomCode);
                }
                if (!mappingRepository.existsBySubjectIdAndFunctionalRoomId(
                        subject.getId(), room.getId())) {
                    mappingRepository.save(new SubjectFunctionalRoom(subject.getId(), room.getId()));
                }
            });
        });
    }
}
