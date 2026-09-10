package com.JavaTraining.BaiTap_RS.placement.controller;

import com.JavaTraining.BaiTap_RS.common.annotation.ApiMessage;
import com.JavaTraining.BaiTap_RS.common.contract.ResultPaginationDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqConfirmPlacementDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqCreatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqPlacementActionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqUpdatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementResultDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.response.ResPlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.service.PlacementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v3/placement-sessions")
@PreAuthorize("hasAnyRole('ADMIN', 'ACADEMIC_OFFICE')")
public class PlacementController {
    private final PlacementService placementService;

    public PlacementController(PlacementService placementService) {
        this.placementService = placementService;
    }

    @PostMapping
    @ApiMessage("Tạo phiên xếp lớp")
    public ResponseEntity<ResPlacementSessionDTO> create(@Valid @RequestBody ReqCreatePlacementSessionDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(placementService.create(request));
    }

    @GetMapping("/{id}")
    @ApiMessage("Xem phiên xếp lớp")
    public ResPlacementSessionDTO get(@PathVariable("id") @Positive Long id) { return placementService.get(id); }

    @PutMapping("/{id}")
    @ApiMessage("Cập nhật phiên xếp lớp")
    public ResPlacementSessionDTO update(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqUpdatePlacementSessionDTO request) { return placementService.update(id, request); }

    @PostMapping("/{id}/simulate")
    @ApiMessage("Mô phỏng xếp lớp")
    public ResPlacementSessionDTO simulate(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqPlacementActionDTO request) { return placementService.simulate(id, request); }

    @GetMapping("/{id}/results")
    @ApiMessage("Xem kết quả xếp lớp")
    public ResultPaginationDTO<ResPlacementResultDTO> results(@PathVariable("id") @Positive Long id,
            Pageable pageable) { return placementService.getResults(id, pageable); }

    @PostMapping("/{id}/confirm")
    @ApiMessage("Xác nhận xếp lớp")
    public ResPlacementSessionDTO confirm(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqConfirmPlacementDTO request) { return placementService.confirm(id, request); }

    @PostMapping("/{id}/cancel")
    @ApiMessage("Hủy phiên xếp lớp")
    public ResPlacementSessionDTO cancel(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody ReqPlacementActionDTO request) { return placementService.cancel(id, request); }
}
