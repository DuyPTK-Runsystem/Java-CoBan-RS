package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectFunctionalRoomsDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectFunctionalRoomsDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.entity.SubjectFunctionalRoom;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.repository.SubjectRepository;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.response.ResFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class SubjectFunctionalRoomServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private SubjectFunctionalRoomRepository mappingRepository;

    @Mock
    private FunctionalRoomRepository roomRepository;

    private SubjectFunctionalRoomService service;

    @BeforeEach
    void setUp() {
        service = new SubjectFunctionalRoomService(subjectRepository, mappingRepository, roomRepository);
    }

    @Test
    void getRoomsForSubject_returnsRooms() {
        Mockito.when(subjectRepository.existsById(10L)).thenReturn(true);
        SubjectFunctionalRoom m1 = new SubjectFunctionalRoom(10L, 1L);
        Mockito.when(mappingRepository.findBySubjectId(10L)).thenReturn(List.of(m1));

        FunctionalRoom r1 = new FunctionalRoom("LAB_PHYSICS", "Phòng Vật Lý");
        ReflectionTestUtils.setField(r1, "id", 1L);
        Mockito.when(roomRepository.findAllById(List.of(1L))).thenReturn(List.of(r1));

        ResSubjectFunctionalRoomsDTO res = service.getRoomsForSubject(10L);
        Assertions.assertEquals(10L, res.subjectId());
        Assertions.assertEquals(1, res.rooms().size());
        Assertions.assertEquals("LAB_PHYSICS", res.rooms().get(0).code());
    }

    @Test
    void updateRoomsForSubject_subjectNotFound_throwsNotFound() {
        Mockito.when(subjectRepository.existsById(99L)).thenReturn(false);

        ReqUpdateSubjectFunctionalRoomsDTO req = new ReqUpdateSubjectFunctionalRoomsDTO(List.of(1L), null);
        AppException ex = Assertions.assertThrows(AppException.class,
                () -> service.updateRoomsForSubject(99L, req));
        Assertions.assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateRoomsForSubject_roomNotFound_throwsBadRequest() {
        Mockito.when(subjectRepository.existsById(10L)).thenReturn(true);
        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        ReqUpdateSubjectFunctionalRoomsDTO req = new ReqUpdateSubjectFunctionalRoomsDTO(List.of(1L), null);
        AppException ex = Assertions.assertThrows(AppException.class,
                () -> service.updateRoomsForSubject(10L, req));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void updateRoomsForSubject_roomInactive_throwsBadRequest() {
        Mockito.when(subjectRepository.existsById(10L)).thenReturn(true);
        FunctionalRoom r = new FunctionalRoom("LAB_01", "Lab 1");
        r.setStatus(RoomStatus.INACTIVE);
        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.of(r));

        ReqUpdateSubjectFunctionalRoomsDTO req = new ReqUpdateSubjectFunctionalRoomsDTO(List.of(1L), null);
        AppException ex = Assertions.assertThrows(AppException.class,
                () -> service.updateRoomsForSubject(10L, req));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void updateRoomsForSubject_success() {
        Mockito.when(subjectRepository.existsById(10L)).thenReturn(true);
        FunctionalRoom r1 = new FunctionalRoom("LAB_01", "Lab 1");
        ReflectionTestUtils.setField(r1, "id", 1L);
        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.of(r1));

        ReqUpdateSubjectFunctionalRoomsDTO req = new ReqUpdateSubjectFunctionalRoomsDTO(List.of(1L), null);
        service.updateRoomsForSubject(10L, req);

        Mockito.verify(mappingRepository).deleteBySubjectId(10L);
        Mockito.verify(mappingRepository).saveAll(Mockito.anyList());
    }

    @Test
    void lookupRooms_withSubjectId_returnsMappedRooms() {
        SubjectFunctionalRoom m1 = new SubjectFunctionalRoom(10L, 1L);
        Mockito.when(mappingRepository.findBySubjectId(10L)).thenReturn(List.of(m1));

        FunctionalRoom r1 = new FunctionalRoom("LAB_01", "Lab 1");
        ReflectionTestUtils.setField(r1, "id", 1L);
        Mockito.when(roomRepository.findAllById(List.of(1L))).thenReturn(List.of(r1));

        List<ResFunctionalRoomDTO> rooms = service.lookupRooms(10L, RoomStatus.ACTIVE);
        Assertions.assertEquals(1, rooms.size());
        Assertions.assertEquals("LAB_01", rooms.get(0).code());
    }
}
