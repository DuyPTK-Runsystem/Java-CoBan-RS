package com.JavaTraining.BaiTap_RS.placement.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementCandidate;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementClassProfile;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementIssueSeverity;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResult;
import com.JavaTraining.BaiTap_RS.placement.domain.entity.PlacementResultStatus;
import org.springframework.stereotype.Component;

@Component
public class PlacementAllocationEngine {
    private static final String CAPACITY_CODE = "CAPACITY_EXCEEDED";
    private static final String MALE = "MALE";

    public List<PlacementResult> allocate(Long sessionId, List<PlacementCandidate> candidates,
            List<PlacementTarget> targets, Map<Long, Integer> available) {
        List<PlacementCandidate> eligible = candidates.stream().filter(this::isEligible)
                .collect(Collectors.toCollection(ArrayList::new));
        AllocationState state = new AllocationState(sessionId, targets, available, eligible);
        candidates.stream().filter(candidate -> !eligible.contains(candidate))
                .forEach(candidate -> state.manual(candidate, missingCode(candidate), PlacementIssueSeverity.WARNING,
                        missingExplanation(candidate)));
        state.allocateRanked(eligible, PlacementClassProfile.ADVANCED, true);
        state.allocateRanked(eligible, PlacementClassProfile.SUPPORT, false);
        state.allocateRegular(eligible);
        eligible.stream().filter(candidate -> !state.handledStudentIds.contains(candidate.getStudentId()))
                .forEach(candidate -> state.manual(candidate,
                CAPACITY_CODE, PlacementIssueSeverity.BLOCKING, "Không còn chỗ trong profile lớp đích"));
        return state.results();
    }

    private boolean isEligible(PlacementCandidate candidate) {
        return candidate.getScore() != null && validGender(candidate.getGenderSnapshot());
    }

    private String missingCode(PlacementCandidate candidate) {
        return candidate.getScore() == null ? "MISSING_OFFICIAL_RESULT" : "MISSING_DATA";
    }

    private String missingExplanation(PlacementCandidate candidate) {
            return candidate.getScore() == null
                    ? "Chưa có kết quả tổng kết chính thức; giáo vụ xếp thủ công"
                    : "Thiếu giới tính; giáo vụ xếp thủ công";
    }

    private boolean validGender(String gender) {
        return MALE.equals(gender) || "FEMALE".equals(gender);
    }

    private static final class AllocationState {
        private final Long sessionId;
        private final List<PlacementTarget> targets;
        private final Map<Long, Integer> available;
        private final Map<Long, Integer> assigned;
        private final Map<Long, Integer> maleAssigned;
        private final Map<Long, Integer> maleQuota;
        private final Map<Long, BigDecimal> scoreTotals;
        private final Map<Long, PlacementTarget> targetsById;
        private final Set<Long> handledStudentIds = new HashSet<>();
        private final List<PlacementResult> allocatedResults = new ArrayList<>();

        private AllocationState(Long sessionId, List<PlacementTarget> targets, Map<Long, Integer> available,
                List<PlacementCandidate> eligibleCandidates) {
            this.sessionId = sessionId;
            this.targets = targets;
            this.available = new HashMap<>(available);
            this.assigned = targets.stream().collect(Collectors.toMap(PlacementTarget::classId, target -> 0));
            this.maleAssigned = targets.stream().collect(Collectors.toMap(PlacementTarget::classId, target -> 0));
            long male = eligibleCandidates.stream()
                    .filter(candidate -> MALE.equals(candidate.getGenderSnapshot())).count();
            long known = eligibleCandidates.size();
            this.maleQuota = targets.stream().collect(Collectors.toMap(PlacementTarget::classId,
                    target -> known == 0 ? 0
                            : (int) Math.round((target.capacity() == null ? 0 : target.capacity())
                                    * (double) male / known)));
            this.scoreTotals = targets.stream().collect(Collectors.toMap(PlacementTarget::classId,
                    target -> BigDecimal.ZERO));
            this.targetsById = targets.stream().collect(Collectors.toMap(PlacementTarget::classId,
                    Function.identity()));
        }

        private void allocateRanked(List<PlacementCandidate> pool, PlacementClassProfile profile, boolean descending) {
            List<PlacementTarget> profileTargets = targets.stream()
                    .filter(target -> target.profile() == profile).toList();
            if (profileTargets.isEmpty()) {
                return;
            }
            int limit = profileTargets.stream().mapToInt(target -> Math.max(0, available.get(target.classId()))).sum();
            if (limit == 0) {
                return;
            }
            pool.sort(Comparator.comparing(PlacementCandidate::getScore,
                    descending ? Comparator.reverseOrder() : Comparator.naturalOrder())
                    .thenComparing(PlacementCandidate::getStudentId));
            List<PlacementCandidate> selected = selectWithoutBoundaryTie(pool, limit);
            selected.forEach(candidate -> assign(candidate, profileTargets));
            pool.removeAll(selected);
        }

        private List<PlacementCandidate> selectWithoutBoundaryTie(List<PlacementCandidate> ranked, int limit) {
            List<PlacementCandidate> selected = new ArrayList<>(ranked.subList(0, Math.min(limit, ranked.size())));
            if (ranked.size() <= limit || selected.isEmpty()
                    || selected.get(selected.size() - 1).getScore().compareTo(ranked.get(limit).getScore()) != 0) {
                return selected;
            }
            BigDecimal boundary = selected.get(selected.size() - 1).getScore();
            List<PlacementCandidate> tied = ranked.stream()
                    .filter(candidate -> candidate.getScore().compareTo(boundary) == 0).toList();
            selected.removeIf(candidate -> candidate.getScore().compareTo(boundary) == 0);
            tied.forEach(candidate -> manual(candidate, "SCORE_TIE", PlacementIssueSeverity.WARNING,
                    "Bằng điểm tại ngưỡng; giáo vụ xếp thủ công"));
            ranked.removeAll(tied);
            return selected;
        }

        private void allocateRegular(List<PlacementCandidate> pool) {
            List<PlacementTarget> regularTargets = targets.stream()
                    .filter(target -> target.profile() == PlacementClassProfile.REGULAR).toList();
            if (regularTargets.isEmpty() || pool.isEmpty()) {
                return;
            }
            BigDecimal cohortAverage = pool.stream().map(PlacementCandidate::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(pool.size()), 6, RoundingMode.HALF_UP);
            pool.sort(Comparator.comparing(PlacementCandidate::getScore).reversed()
                    .thenComparing(PlacementCandidate::getStudentId));
            new ArrayList<>(pool).forEach(candidate -> assignRegular(candidate, regularTargets, cohortAverage));
            pool.removeIf(candidate -> handledStudentIds.contains(candidate.getStudentId()));
        }

        private void assignRegular(PlacementCandidate candidate, List<PlacementTarget> targets,
                BigDecimal cohortAverage) {
            boolean male = MALE.equals(candidate.getGenderSnapshot());
            targets.stream().filter(target -> available.get(target.classId()) > 0).min(Comparator
                    .comparing((PlacementTarget target) -> scoreTotals.get(target.classId()).add(candidate.getScore())
                            .divide(BigDecimal.valueOf(assigned.get(target.classId()) + 1L), 6, RoundingMode.HALF_UP)
                            .subtract(cohortAverage).abs())
                    .thenComparing(Comparator.comparingInt((PlacementTarget target) -> genderNeed(target, male))
                            .reversed())
                    .thenComparingInt(target -> assigned.get(target.classId())).thenComparing(PlacementTarget::classId))
                    .ifPresent(target -> assignToTarget(candidate, target));
        }

        private void assign(PlacementCandidate candidate, List<PlacementTarget> profileTargets) {
            boolean male = MALE.equals(candidate.getGenderSnapshot());
            profileTargets.stream().filter(target -> available.get(target.classId()) > 0).min(Comparator
                    .comparingInt((PlacementTarget target) -> genderNeed(target, male)).reversed()
                    .thenComparingInt(target -> assigned.get(target.classId())).thenComparing(PlacementTarget::classId))
                    .ifPresent(target -> assignToTarget(candidate, target));
        }

        private int genderNeed(PlacementTarget target, boolean male) {
            int targetMale = maleQuota.getOrDefault(target.classId(), 0);
            int targetSize = target.capacity() == null ? 0 : target.capacity();
            int targetGender = male ? targetMale : Math.max(0, targetSize - targetMale);
            int current = male ? maleAssigned.get(target.classId())
                    : assigned.get(target.classId()) - maleAssigned.get(target.classId());
            return Math.max(0, targetGender - current);
        }

        private void assignToTarget(PlacementCandidate candidate, PlacementTarget target) {
            available.computeIfPresent(target.classId(), (key, value) -> value - 1);
            assigned.computeIfPresent(target.classId(), (key, value) -> value + 1);
            if (MALE.equals(candidate.getGenderSnapshot())) {
                maleAssigned.computeIfPresent(target.classId(), (key, value) -> value + 1);
            }
            scoreTotals.computeIfPresent(target.classId(), (key, value) -> value.add(candidate.getScore()));
            allocatedResults.add(new PlacementResult(sessionId, candidate.getStudentId(), target.classId(),
                    PlacementResultStatus.AUTO_ASSIGNED, candidate.getScore(), null, null,
                    "Phân bổ theo profile " + targetsById.get(target.classId()).profile().name()));
            handledStudentIds.add(candidate.getStudentId());
        }

        private void manual(PlacementCandidate candidate, String code, PlacementIssueSeverity severity,
                String explanation) {
            allocatedResults.add(new PlacementResult(sessionId, candidate.getStudentId(), null,
                    PlacementResultStatus.MANUAL_REQUIRED, candidate.getScore(), code, severity, explanation));
            handledStudentIds.add(candidate.getStudentId());
        }

        private List<PlacementResult> results() {
            return List.copyOf(allocatedResults);
        }
    }
}
