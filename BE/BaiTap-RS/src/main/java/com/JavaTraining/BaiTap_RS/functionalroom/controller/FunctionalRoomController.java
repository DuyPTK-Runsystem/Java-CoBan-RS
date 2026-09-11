package com.JavaTraining.BaiTap_RS.functionalroom.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests.ReqCreateFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.requests.ReqUpdateFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.response.ResFunctionalRoomDTO;
import com.JavaTraining.BaiTap_RS.functionalroom.domain.entity.RoomStatus;
import com.JavaTraining.BaiTap_RS.functionalroom.service.FunctionalRoomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/functional-rooms")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
@RequiredArgsConstructor
public class FunctionalRoomController {

    private final FunctionalRoomService service;

    @GetMapping
    @ApiMessage("Lấy danh sách phòng chức năng phân trang")
    public ResultPaginationDTO<ResFunctionalRoomDTO> pageRooms(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) RoomStatus status,
            Pageable pageable) {
        return service.pageRooms(search, status, pageable);
    }

    @GetMapping("/lookup")
    @ApiMessage("Lookup danh sách phòng chức năng")
    public List<ResFunctionalRoomDTO> lookup(
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "status", required = false) RoomStatus status) {
        return service.lookup(subjectId, status);
    }

    @GetMapping("/{id}")
    @ApiMessage("Xem chi tiết phòng chức năng")
    public ResFunctionalRoomDTO get(@PathVariable("id") @Positive Long id) {
        return service.get(id);
    }

    @PostMapping
    @ApiMessage("Tạo mới phòng chức năng")
    public ResponseEntity<ResFunctionalRoomDTO> create(@Valid @RequestBody ReqCreateFunctionalRoomDTO req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật phòng chức năng")
    public ResFunctionalRoomDTO update(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqUpdateFunctionalRoomDTO req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa phòng chức năng")
    public ResponseEntity<Void> delete(
            @PathVariable("id") @Positive Long id,
            @RequestParam(name = "expectedVersion", required = false) Long expectedVersion) {
        service.delete(id, expectedVersion);
        return ResponseEntity.noContent().build();
    }
}
