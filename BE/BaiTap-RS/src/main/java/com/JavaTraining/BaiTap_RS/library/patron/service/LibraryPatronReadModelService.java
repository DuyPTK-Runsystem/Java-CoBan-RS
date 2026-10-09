package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.JavaTraining.BaiTap_RS.library.card.domain.DTOs.response.ResLibraryCardDTO;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryActivationCandidateDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.DTOs.response.ResLibraryPatronDTO;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronSuspension;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronSuspensionRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryPatronReadModelService {

    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final LibraryPatronSuspensionRepository suspensionRepository;
    private final LibraryCardRepository cardRepository;
    private final LibraryPatronDisplayNameService displayNameService;

    @Transactional(readOnly = true)
    public List<ResLibraryPatronDTO> patrons(List<LibraryPatron> patrons) {
        List<Long> userIds = patrons.stream().map(LibraryPatron::getUserId).toList();
        Map<Long, String> names = displayNameService.displayNames(userIds);
        Map<Long, String> usernames = displayNameService.usernames(userIds);
        List<Long> patronIds = patrons.stream().map(LibraryPatron::getId).toList();
        Map<Long, List<String>> reasonsByPatron = patronIds.isEmpty() ? Map.of() : suspensionRepository
                .findAllByPatronIdInAndResolvedAtIsNullOrderByPatronIdAscSuspendedAtAsc(patronIds).stream()
                .collect(Collectors.groupingBy(LibraryPatronSuspension::getPatronId,
                        Collectors.mapping(LibraryPatronSuspension::getReason, Collectors.toList())));
        Map<Long, LibraryCard> cardsByPatron = new HashMap<>();
        if (!patronIds.isEmpty()) {
            cardRepository.findAllByPatronIdInAndStatusOrderByIssuedAtDesc(patronIds, LibraryCardStatus.ACTIVE)
                    .forEach(card -> cardsByPatron.putIfAbsent(card.getPatronId(), card));
        }
        return patrons.stream().map(patron -> toResponse(patron,
                names.getOrDefault(patron.getUserId(), usernames.getOrDefault(patron.getUserId(), "")),
                reasonsByPatron.getOrDefault(patron.getId(), List.of()), cardsByPatron.get(patron.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ResLibraryPatronDTO patron(LibraryPatron patron) {
        return toResponse(patron, displayNameService.displayName(patron.getUserId()), suspensionReasons(patron.getId()),
                cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(patron.getId(), LibraryCardStatus.ACTIVE)
                        .orElse(null));
    }

    @Transactional(readOnly = true)
    public List<ResLibraryActivationCandidateDTO> candidates(List<User> candidates) {
        List<Long> userIds = candidates.stream().map(User::getId).toList();
        Map<Long, String> names = displayNameService.displayNames(userIds);
        return candidates.stream().map(user -> new ResLibraryActivationCandidateDTO(user.getId(),
                names.getOrDefault(user.getId(), user.getUsername()), user.getUsername(), roleCode(user))).toList();
    }

    @Transactional(readOnly = true)
    public List<String> suspensionReasons(Long patronId) {
        return suspensionRepository.findAllByPatronIdAndResolvedAtIsNullOrderBySuspendedAtAsc(patronId).stream()
                .map(LibraryPatronSuspension::getReason).toList();
    }

    private ResLibraryPatronDTO toResponse(LibraryPatron patron, String displayName, List<String> reasons,
            LibraryCard card) {
        ResLibraryCardDTO currentCard = card == null ? null : cardResponse(card);
        return new ResLibraryPatronDTO(patron.getId(), patron.getUserId(), displayName, patron.getStatus(),
                patron.getJoinedAt(), reasons, currentCard);
    }

    private ResLibraryCardDTO cardResponse(LibraryCard card) {
        LibraryCardStatus status = card.getStatus();
        if (status == LibraryCardStatus.ACTIVE && card.getExpiresAt().isBefore(LocalDate.now(LIBRARY_ZONE))) {
            status = LibraryCardStatus.EXPIRED;
        }
        return new ResLibraryCardDTO(card.getCardNo(), card.getPatronId(), status, card.getIssuedAt(),
                card.getExpiresAt(), card.getPayloadVersion(), card.getPolicyVersion(), card.getRevokedAt(),
                card.getRevokedReason());
    }

    private String roleCode(User user) {
        return user.getRoles().stream().map(Role::getCode).filter(Objects::nonNull).sorted().findFirst().orElse(null);
    }
}
