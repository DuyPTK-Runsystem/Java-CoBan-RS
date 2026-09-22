package com.JavaTraining.BaiTap_RS.placement.service.support;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementIssueSeverity;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementSourceType;
import org.junit.jupiter.api.Test;

class PlacementAllocationEngineTest {
    @Test
    void allocatesAtMostThirtySevenOfFortyCandidatesAndLeavesOverflowManual() {
        List<PlacementTarget> targets = List.of(
                new PlacementTarget(101L, "7A1", "7A1", PlacementClassProfile.REGULAR, 7),
                new PlacementTarget(102L, "7A2", "7A2", PlacementClassProfile.REGULAR, 10),
                new PlacementTarget(103L, "7A3", "7A3", PlacementClassProfile.REGULAR, 10),
                new PlacementTarget(104L, "7A4", "7A4", PlacementClassProfile.REGULAR, 10));
        Map<Long, Integer> available = Map.of(101L, 7, 102L, 10, 103L, 10, 104L, 10);
        List<PlacementCandidate> candidates = IntStream.rangeClosed(1, 40)
                .mapToObj(id -> new PlacementCandidate(1L, (long) id, 8L, PlacementSourceType.CONTINUING,
                        BigDecimal.valueOf(100L - id, 2), "transcript", id % 2 == 0 ? "FEMALE" : "MALE",
                        null, null))
                .toList();

        List<PlacementResult> results = new PlacementAllocationEngine().allocate(1L, candidates, targets, available);
        Map<Long, Long> assignedByClass = results.stream()
                .filter(result -> result.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED)
                .collect(Collectors.groupingBy(PlacementResult::getTargetClassId, Collectors.counting()));

        assertAll("37/40 capacity boundary",
                () -> assertEquals(37, results.stream()
                        .filter(result -> result.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED).count()),
                () -> assertEquals(3, results.stream()
                        .filter(result -> result.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED).count()),
                () -> assertEquals(7L, assignedByClass.getOrDefault(101L, 0L)),
                () -> assertEquals(10L, assignedByClass.getOrDefault(102L, 0L)),
                () -> assertEquals(10L, assignedByClass.getOrDefault(103L, 0L)),
                () -> assertEquals(10L, assignedByClass.getOrDefault(104L, 0L)));
        assertTrue(results.stream().filter(result -> result.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED)
                .allMatch(result -> "CAPACITY_EXCEEDED".equals(result.getIssueCode())
                        && result.getIssueSeverity() == PlacementIssueSeverity.WARNING
                        && result.getTargetClassId() == null));
        assertTrue(assignedByClass.entrySet().stream().allMatch(entry -> entry.getValue() <= available.get(entry.getKey())));
        assertTrue(results.stream().filter(result -> result.getResultStatus() == PlacementResultStatus.AUTO_ASSIGNED)
                .allMatch(result -> result.getTargetClassId() != null));
    }

    @Test
    void doesNotChooseBoundaryTieImplicitly() {
        List<PlacementTarget> targets = List.of(
                new PlacementTarget(101L, "7A1", "7A1", PlacementClassProfile.ADVANCED, 1));
        List<PlacementCandidate> candidates = List.of(
                candidate(1L, "9.0", "MALE"), candidate(2L, "9.0", "FEMALE"));

        List<PlacementResult> results = new PlacementAllocationEngine().allocate(1L, candidates, targets,
                Map.of(101L, 1));

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(result -> result.getResultStatus() == PlacementResultStatus.MANUAL_REQUIRED
                && "SCORE_TIE".equals(result.getIssueCode())
                && result.getIssueSeverity() == PlacementIssueSeverity.WARNING));
        assertTrue(results.stream().allMatch(result -> result.getTargetClassId() == null));
    }

    private PlacementCandidate candidate(Long studentId, String score, String gender) {
        return new PlacementCandidate(1L, studentId, 8L, PlacementSourceType.CONTINUING,
                new BigDecimal(score), "transcript", gender, null, null);
    }
}
