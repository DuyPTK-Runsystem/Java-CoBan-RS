package com.JavaTraining.BaiTap_RS.academic.service;

import java.util.Collections;
import java.util.List;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubjectFunctionalRoomService {

    private final SubjectRepository subjectRepository;
    private final SubjectFunctionalRoomRepository mappingRepository;
    private final FunctionalRoomRepository roomRepository;

    @Transactional(readOnly = true)
    public ResSubjectFunctionalRoomsDTO getRoomsForSubject(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học");
        }
        List<SubjectFunctionalRoom> mappings = mappingRepository.findBySubjectId(subjectId);
        List<Long> roomIds = mappings.stream().map(SubjectFunctionalRoom::getFunctionalRoomId).toList();
        List<ResFunctionalRoomDTO> rooms = roomIds.isEmpty() ? Collections.emptyList()
                : roomRepository.findAllById(roomIds).stream()
                        .map(r -> new ResFunctionalRoomDTO(r.getId(), r.getCode(), r.getName(),
                                r.getStatus(), r.getVersion(), r.getCreatedAt(), r.getUpdatedAt()))
                        .toList();
        return new ResSubjectFunctionalRoomsDTO(subjectId, rooms);
    }

    @Transactional
    public ResSubjectFunctionalRoomsDTO updateRoomsForSubject(
            Long subjectId,
            ReqUpdateSubjectFunctionalRoomsDTO req) {
        if (!subjectRepository.existsById(subjectId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học");
        }
        List<Long> roomIds = req.functionalRoomIds() != null ? req.functionalRoomIds() : Collections.emptyList();
        // Verify all rooms exist and are active
        for (Long roomId : roomIds) {
            validateRoomActive(roomId);
        }
        mappingRepository.deleteBySubjectId(subjectId);
        List<SubjectFunctionalRoom> newMappings = roomIds.stream().distinct()
                .map(roomId -> new SubjectFunctionalRoom(subjectId, roomId))
                .toList();
        mappingRepository.saveAll(newMappings);
        return getRoomsForSubject(subjectId);
    }

    @Transactional(readOnly = true)
    public List<ResFunctionalRoomDTO> lookupRooms(Long subjectId, RoomStatus status) {
        if (subjectId != null) {
            List<SubjectFunctionalRoom> mappings = mappingRepository.findBySubjectId(subjectId);
            List<Long> roomIds = mappings.stream().map(SubjectFunctionalRoom::getFunctionalRoomId).toList();
            if (roomIds.isEmpty()) {
                return Collections.emptyList();
            }
            return roomRepository.findAllById(roomIds).stream()
                    .filter(r -> status == null || r.getStatus() == status)
                    .map(r -> new ResFunctionalRoomDTO(r.getId(), r.getCode(), r.getName(),
                            r.getStatus(), r.getVersion(), r.getCreatedAt(), r.getUpdatedAt()))
                    .toList();
        }
        RoomStatus effectiveStatus = status != null ? status : RoomStatus.ACTIVE;
        return roomRepository.findByStatus(effectiveStatus).stream()
                .map(r -> new ResFunctionalRoomDTO(r.getId(), r.getCode(), r.getName(),
                        r.getStatus(), r.getVersion(), r.getCreatedAt(), r.getUpdatedAt()))
                .toList();
    }

    private void validateRoomActive(Long roomId) {
        FunctionalRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST,
                        "Phòng chức năng với ID " + roomId + " không tồn tại"));
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Phòng chức năng " + room.getCode() + " đang không hoạt động");
        }
    }
}
