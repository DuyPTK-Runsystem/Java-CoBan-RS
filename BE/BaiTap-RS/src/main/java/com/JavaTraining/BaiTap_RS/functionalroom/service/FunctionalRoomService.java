package com.JavaTraining.BaiTap_RS.functionalroom.service;

import java.util.List;
import java.util.Objects;

import com.JavaTraining.BaiTap_RS.academic.repository.SubjectFunctionalRoomRepository;
import com.JavaTraining.BaiTap_RS.academic.service.SubjectFunctionalRoomService;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.common.error.AppException;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests.ReqCreateFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests.ReqUpdateFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.response.ResFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.FunctionalRoom;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.repository.FunctionalRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FunctionalRoomService {

    private final FunctionalRoomRepository roomRepository;
    private final SubjectFunctionalRoomRepository subjectFunctionalRoomRepository;
    private final SubjectFunctionalRoomService subjectFunctionalRoomService;

    @Transactional(readOnly = true)
    public ResultPaginationDTO<ResFunctionalRoomDTO> pageRooms(String search, RoomStatus status, Pageable pageable) {
        Page<FunctionalRoom> page = roomRepository.searchRooms(search, status, pageable);
        List<ResFunctionalRoomDTO> dtos = page.getContent().stream()
                .map(this::toDTO)
                .toList();
        return new ResultPaginationDTO<>(
                new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                        page.getTotalPages(), page.getTotalElements()),
                dtos);
    }

    @Transactional(readOnly = true)
    public ResFunctionalRoomDTO get(Long id) {
        FunctionalRoom room = findRoom(id);
        return toDTO(room);
    }

    @Transactional
    public ResFunctionalRoomDTO create(ReqCreateFunctionalRoomDTO req) {
        if (roomRepository.existsByCode(req.code())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã phòng chức năng '" + req.code() + "' đã tồn tại");
        }
        FunctionalRoom room = new FunctionalRoom(req.code(), req.name());
        room = roomRepository.save(room);
        return toDTO(room);
    }

    @Transactional
    public ResFunctionalRoomDTO update(Long id, ReqUpdateFunctionalRoomDTO req) {
        FunctionalRoom room = findRoom(id);
        if (!Objects.equals(room.getVersion(), req.expectedVersion())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Phòng chức năng đã bị thay đổi bởi người khác. Vui lòng tải lại trang.");
        }
        if (roomRepository.existsByCodeAndIdNot(req.code(), id)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mã phòng chức năng '" + req.code() + "' đã tồn tại");
        }
        room.setCode(req.code());
        room.setName(req.name());
        room.setStatus(req.status());
        room = roomRepository.save(room);
        return toDTO(room);
    }

    @Transactional
    public void delete(Long id, Long expectedVersion) {
        FunctionalRoom room = findRoom(id);
        if (expectedVersion != null && !Objects.equals(room.getVersion(), expectedVersion)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Phòng chức năng đã bị thay đổi bởi người khác. Vui lòng tải lại trang.");
        }
        if (subjectFunctionalRoomRepository.existsByFunctionalRoomId(id)) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Phòng chức năng đang được gán cho môn học, không thể xóa.");
        }
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<ResFunctionalRoomDTO> lookup(Long subjectId, RoomStatus status) {
        return subjectFunctionalRoomService.lookupRooms(subjectId, status);
    }

    private FunctionalRoom findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy phòng chức năng"));
    }

    private ResFunctionalRoomDTO toDTO(FunctionalRoom room) {
        return new ResFunctionalRoomDTO(
                room.getId(),
                room.getCode(),
                room.getName(),
                room.getStatus(),
                room.getVersion(),
                room.getCreatedAt(),
                room.getUpdatedAt());
    }
}
