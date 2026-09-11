package com.JavaTraining.BaiTap_RS.functionalroom.service;

import java.util.List;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests.ReqCreateFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests.ReqUpdateFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.response.ResFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.service.SubjectFunctionalRoomService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD")
class FunctionalRoomServiceTest {

    @Mock
    private FunctionalRoomRepository roomRepository;

    @Mock
    private SubjectFunctionalRoomRepository mappingRepository;

    @Mock
    private SubjectFunctionalRoomService subjectFunctionalRoomService;

    private FunctionalRoomService service;

    @BeforeEach
    void setUp() {
        service = new FunctionalRoomService(roomRepository, mappingRepository, subjectFunctionalRoomService);
    }

    @Test
    void create_success() {
        ReqCreateFunctionalRoomDTO req = new ReqCreateFunctionalRoomDTO("LAB_01", "Phòng Tin học 1");
        Mockito.when(roomRepository.existsByCode("LAB_01")).thenReturn(false);

        FunctionalRoom saved = new FunctionalRoom("LAB_01", "Phòng Tin học 1");
        ReflectionTestUtils.setField(saved, "id", 1L);
        Mockito.when(roomRepository.save(Mockito.any(FunctionalRoom.class))).thenReturn(saved);

        ResFunctionalRoomDTO res = service.create(req);
        Assertions.assertNotNull(res);
        Assertions.assertEquals("LAB_01", res.code());
        Assertions.assertEquals("Phòng Tin học 1", res.name());
        Assertions.assertEquals(RoomStatus.ACTIVE, res.status());
    }

    @Test
    void create_duplicateCode_throwsBadRequest() {
        ReqCreateFunctionalRoomDTO req = new ReqCreateFunctionalRoomDTO("LAB_01", "Phòng Tin học 1");
        Mockito.when(roomRepository.existsByCode("LAB_01")).thenReturn(true);

        AppException ex = Assertions.assertThrows(AppException.class, () -> service.create(req));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void update_success() {
        FunctionalRoom existing = new FunctionalRoom("LAB_01", "Phòng Tin học 1");
        ReflectionTestUtils.setField(existing, "id", 1L);
        ReflectionTestUtils.setField(existing, "version", 0L);

        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(roomRepository.save(Mockito.any(FunctionalRoom.class))).thenReturn(existing);

        ReqUpdateFunctionalRoomDTO req = new ReqUpdateFunctionalRoomDTO("LAB_01", "Phòng Tin Mới", RoomStatus.INACTIVE,
                0L);
        ResFunctionalRoomDTO res = service.update(1L, req);

        Assertions.assertNotNull(res);
        Assertions.assertEquals("Phòng Tin Mới", res.name());
        Assertions.assertEquals(RoomStatus.INACTIVE, res.status());
    }

    @Test
    void update_versionConflict_throwsConflict() {
        FunctionalRoom existing = new FunctionalRoom("LAB_01", "Phòng Tin học 1");
        ReflectionTestUtils.setField(existing, "id", 1L);
        ReflectionTestUtils.setField(existing, "version", 1L);

        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));

        ReqUpdateFunctionalRoomDTO req = new ReqUpdateFunctionalRoomDTO("LAB_01", "Phòng Tin Mới", RoomStatus.ACTIVE,
                0L);
        AppException ex = Assertions.assertThrows(AppException.class, () -> service.update(1L, req));
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void delete_referencedInSubject_throwsConflict() {
        FunctionalRoom existing = new FunctionalRoom("LAB_01", "Phòng Tin học 1");
        ReflectionTestUtils.setField(existing, "id", 1L);
        ReflectionTestUtils.setField(existing, "version", 0L);
        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(mappingRepository.existsByFunctionalRoomId(1L)).thenReturn(true);

        AppException ex = Assertions.assertThrows(AppException.class, () -> service.delete(1L, 0L));
        Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void delete_success() {
        FunctionalRoom existing = new FunctionalRoom("LAB_01", "Phòng Tin học 1");
        ReflectionTestUtils.setField(existing, "id", 1L);
        ReflectionTestUtils.setField(existing, "version", 0L);
        Mockito.when(roomRepository.findById(1L)).thenReturn(Optional.of(existing));
        Mockito.when(mappingRepository.existsByFunctionalRoomId(1L)).thenReturn(false);

        service.delete(1L, 0L);
        Mockito.verify(roomRepository).delete(existing);
    }

    @Test
    void lookup_delegatesToSubjectFunctionalRoomService() {
        List<ResFunctionalRoomDTO> expected = List.of(
                new ResFunctionalRoomDTO(1L, "LAB_01", "Lab 1", RoomStatus.ACTIVE, 0L, null, null));
        Mockito.when(subjectFunctionalRoomService.lookupRooms(10L, RoomStatus.ACTIVE)).thenReturn(expected);
        List<ResFunctionalRoomDTO> result = service.lookup(10L, RoomStatus.ACTIVE);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void pageRooms_returnsMappedPage() {
        FunctionalRoom r1 = new FunctionalRoom("LAB_01", "Lab 1");
        ReflectionTestUtils.setField(r1, "id", 1L);
        FunctionalRoom r2 = new FunctionalRoom("LAB_02", "Lab 2");
        ReflectionTestUtils.setField(r2, "id", 2L);

        Page<FunctionalRoom> page = new PageImpl<>(List.of(r1, r2));
        Mockito.when(roomRepository.searchRooms(Mockito.isNull(), Mockito.eq(RoomStatus.ACTIVE), Mockito.any()))
                .thenReturn(page);

        ResultPaginationDTO<ResFunctionalRoomDTO> result = service.pageRooms(null, RoomStatus.ACTIVE,
                PageRequest.of(0, 10));
        Assertions.assertEquals(2, result.result().size());
        Assertions.assertEquals("LAB_01", result.result().get(0).code());
        Assertions.assertEquals("LAB_02", result.result().get(1).code());
    }
}
