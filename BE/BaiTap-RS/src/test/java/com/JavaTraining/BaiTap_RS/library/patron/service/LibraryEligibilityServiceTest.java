package com.JavaTraining.BaiTap_RS.library.patron.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCard;
import com.JavaTraining.BaiTap_RS.library.card.domain.entity.LibraryCardStatus;
import com.JavaTraining.BaiTap_RS.library.card.repository.LibraryCardRepository;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatron;
import com.JavaTraining.BaiTap_RS.library.patron.domain.entity.LibraryPatronStatus;
import com.JavaTraining.BaiTap_RS.library.patron.exception.LibraryPatronException;
import com.JavaTraining.BaiTap_RS.library.patron.repository.LibraryPatronRepository;
import com.JavaTraining.BaiTap_RS.user.domain.entity.Role;
import com.JavaTraining.BaiTap_RS.user.domain.entity.RoleCode;
import com.JavaTraining.BaiTap_RS.user.domain.entity.User;
import com.JavaTraining.BaiTap_RS.user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryEligibilityServiceTest {

    private static final Long USER_ID = 15L;
    private static final Long PATRON_ID = 4L;
    private static final ZoneId LIBRARY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private UserRepository userRepository;

    @Mock
    private LibraryPatronRepository patronRepository;

    @Mock
    private LibraryCardRepository cardRepository;

    @InjectMocks
    private LibraryEligibilityService service;

    private User user;
    private LibraryPatron patron;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        patron = new LibraryPatron(USER_ID, LocalDateTime.of(2026, 10, 9, 10, 0));
        ReflectionTestUtils.setField(patron, "id", PATRON_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(patronRepository.findByUserId(USER_ID)).thenReturn(Optional.of(patron));
    }

    @Test
    void permitsEligibleNonManagerRoleAndReturnsThePatronIdentity() {
        when(user.getRoles()).thenReturn(Set.of(role(RoleCode.TEACHER)));

        LibraryEligibilityService.BorrowingEligibility result = service.assertCanBorrow(USER_ID, false);

        assertEquals(new LibraryEligibilityService.BorrowingEligibility(USER_ID, PATRON_ID, null), result);
    }

    @Test
    void deniesAdminAndLibrarianRolesBeforeLookingUpPatronEligibility() {
        for (RoleCode roleCode : Set.of(RoleCode.ADMIN, RoleCode.LIBRARIAN)) {
            when(user.getRoles()).thenReturn(Set.of(role(roleCode)));

            LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                    () -> service.assertCanBorrow(USER_ID, false));

            assertEquals("LIBRARY_RESOURCE_FORBIDDEN", exception.getCode());
        }
        verifyNoInteractions(patronRepository, cardRepository);
    }

    @Test
    void rejectsSuspendedPatronWithoutCheckingForCard() {
        when(user.getRoles()).thenReturn(Set.of(role(RoleCode.STUDENT)));
        patron.setStatus(LibraryPatronStatus.BORROWING_SUSPENDED, LocalDateTime.now());

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.assertCanBorrow(USER_ID, true));

        assertEquals("PATRON_BORROWING_SUSPENDED", exception.getCode());
        verifyNoInteractions(cardRepository);
    }

    @Test
    void acceptsCardThroughItsExpiryDateAndRejectsItOnTheFollowingDay() {
        when(user.getRoles()).thenReturn(Set.of(role(RoleCode.ACADEMIC_OFFICE)));
        LocalDate today = LocalDate.now(LIBRARY_ZONE);
        LibraryCard card = card(today);
        when(cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(PATRON_ID, LibraryCardStatus.ACTIVE))
                .thenReturn(Optional.of(card));

        LibraryEligibilityService.BorrowingEligibility result = service.assertCanBorrow(USER_ID, true);

        assertEquals(card.getCardNo(), result.cardNo());
        when(cardRepository.findFirstByPatronIdAndStatusOrderByIssuedAtDesc(PATRON_ID, LibraryCardStatus.ACTIVE))
                .thenReturn(Optional.of(card(today.minusDays(1))));

        LibraryPatronException exception = assertThrows(LibraryPatronException.class,
                () -> service.assertCanBorrow(USER_ID, true));

        assertEquals("CARD_NOT_VALID", exception.getCode());
    }

    private Role role(RoleCode code) {
        return new Role(code.code(), code.name(), null);
    }

    private LibraryCard card(LocalDate expiresAt) {
        return new LibraryCard(PATRON_ID, "LC-2026-000001", LocalDateTime.now(), expiresAt, "v1", "test");
    }
}
