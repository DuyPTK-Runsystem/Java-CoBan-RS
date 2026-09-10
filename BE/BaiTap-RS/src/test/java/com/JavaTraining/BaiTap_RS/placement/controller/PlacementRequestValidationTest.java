package com.JavaTraining.BaiTap_RS.placement.controller;

import java.util.List;

import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqConfirmPlacementDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqPlacementActionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.DTOs.requests.ReqUpdatePlacementSessionDTO;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PlacementRequestValidationTest {
    private static Validator validator;
    private static ValidatorFactory factory;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void actionAndUpdateAcceptInitialOptimisticLockVersionZero() {
        boolean valid = validator.validate(new ReqPlacementActionDTO(0L, null)).isEmpty()
                && validator.validate(new ReqUpdatePlacementSessionDTO(0L,
                        List.of(new ReqUpdatePlacementSessionDTO.TargetClass(10L, PlacementClassProfile.REGULAR, 30))))
                        .isEmpty();
        assertTrue(valid, "version zero must be accepted for initial placement actions");
    }

    @Test
    void actionAndUpdateRejectNegativeOptimisticLockVersion() {
        boolean valid = validator.validate(new ReqPlacementActionDTO(-1L, null)).isEmpty()
                || validator.validate(new ReqUpdatePlacementSessionDTO(-1L,
                        List.of(new ReqUpdatePlacementSessionDTO.TargetClass(10L, PlacementClassProfile.REGULAR, 30))))
                        .isEmpty();
        assertFalse(valid, "negative optimistic lock versions must be rejected");
    }

    @Test
    void confirmRequiresNonBlankIdempotencyKey() {
        boolean valid = validator.validate(new ReqConfirmPlacementDTO(0L, "confirm-1")).isEmpty();
        boolean blank = validator.validate(new ReqConfirmPlacementDTO(0L, " ")).isEmpty();
        boolean missing = validator.validate(new ReqConfirmPlacementDTO(0L, null)).isEmpty();
        assertTrue(valid && !blank && !missing, "confirm requires a non-blank idempotency key");
    }
}
