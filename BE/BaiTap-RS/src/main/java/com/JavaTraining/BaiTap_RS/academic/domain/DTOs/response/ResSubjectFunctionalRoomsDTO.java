package com.JavaTraining.BaiTap_RS.academic.domain.DTOs.response;

import java.util.List;

import com.JavaTraining.BaiTap_RS.functionalroom.domain.DTOs.response.ResFunctionalRoomDTO;

public record ResSubjectFunctionalRoomsDTO(
                Long subjectId,
                List<ResFunctionalRoomDTO> rooms) {
}
