package com.JavaTraining.BaiTap_RS.academic.controller;

import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.requests.ReqUpdateSubjectFunctionalRoomsDTO;
import com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response.ResSubjectFunctionalRoomsDTO;
import com.JavaTraining.BaiTap_RS.academic.service.SubjectFunctionalRoomService;
import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/subjects/{subjectId}/functional-rooms")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
@RequiredArgsConstructor
public class SubjectFunctionalRoomController {

    private final SubjectFunctionalRoomService service;

    @GetMapping
    @ApiMessage("Lấy danh sách phòng chức năng của môn học")
    public ResSubjectFunctionalRoomsDTO getRooms(@PathVariable("subjectId") @Positive Long subjectId) {
        return service.getRoomsForSubject(subjectId);
    }

    @PutMapping
    @ApiMessage("Cập nhật danh sách phòng chức năng của môn học")
    public ResSubjectFunctionalRoomsDTO updateRooms(
            @PathVariable("subjectId") @Positive Long subjectId,
            @Valid @RequestBody ReqUpdateSubjectFunctionalRoomsDTO req) {
        return service.updateRoomsForSubject(subjectId, req);
    }
}
