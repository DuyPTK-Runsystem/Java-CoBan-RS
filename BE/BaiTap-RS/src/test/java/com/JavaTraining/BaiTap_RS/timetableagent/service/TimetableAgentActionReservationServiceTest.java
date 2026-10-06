package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.Instant;
import java.util.Optional;

import com.JavaTraining.BaiTap_RS.timetableagent.config.TimetableAgentProperties;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.entity.TimetableAgentAction;
import com.JavaTraining.BaiTap_RS.timetableagent.repository.TimetableAgentActionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class TimetableAgentActionReservationServiceTest {

    private static final String IDEMPOTENCY_KEY = "key-1";
    private static final String REQUEST_HASH = "hash-1";
    private static final String PENDING = "PENDING";
    private final TimetableAgentActionRepository repository = Mockito.mock(TimetableAgentActionRepository.class);
    private final TimetableAgentProperties properties = new TimetableAgentProperties();
    private TimetableAgentActionReservationService service;

    @BeforeEach
    void setUp() {
        properties.setProviderTimeout(java.time.Duration.ofSeconds(30));
        service = new TimetableAgentActionReservationService(repository, properties);
    }

    @Test
    void reserveNewKeyCreatesPendingOwnerWithBoundedLease() {
        Mockito.when(repository.findByActorIdAndIdempotencyKeyForUpdate(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.empty());
        Mockito.when(repository.saveAndFlush(ArgumentMatchers.any(TimetableAgentAction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TimetableAgentActionReservationService.Reservation reservation =
                service.reserve(8L, IDEMPOTENCY_KEY, REQUEST_HASH, 21L, 77L);

        Assertions.assertTrue(reservation.owner() && PENDING.equals(reservation.action().getStatus())
                        && reservation.action().getLeaseExpiresAt().isAfter(Instant.now())
                        && reservation.action().getLeaseExpiresAt().isBefore(Instant.now().plusSeconds(45))
                        && reservation.action().getLeaseToken() != null
                        && !reservation.action().getLeaseToken().isBlank(),
                "new idempotency keys must get a bounded pending lease");
    }

    @Test
    void reserveLiveSameKeyAndHashReturnsExistingPendingOwnerWithoutRenewingLease() {
        TimetableAgentAction existing = action(PENDING, REQUEST_HASH);
        existing.setLeaseExpiresAt(Instant.now().plusSeconds(20));
        String token = existing.getLeaseToken();
        Instant expiry = existing.getLeaseExpiresAt();
        Mockito.when(repository.findByActorIdAndIdempotencyKeyForUpdate(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));

        TimetableAgentActionReservationService.Reservation reservation =
                service.reserve(8L, IDEMPOTENCY_KEY, REQUEST_HASH, 21L, 77L);

        assertLiveDuplicateRemainsUnchangedAndReadOnly(reservation, token, expiry);
    }

    @Test
    void reserveExpiredLeaseReclaimsWithNewTokenAndDeadline() {
        TimetableAgentAction existing = action(PENDING, REQUEST_HASH);
        existing.setLeaseExpiresAt(Instant.now().minusSeconds(1));
        String staleToken = existing.getLeaseToken();
        Mockito.when(repository.findByActorIdAndIdempotencyKeyForUpdate(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));
        Mockito.when(repository.saveAndFlush(existing)).thenReturn(existing);

        TimetableAgentActionReservationService.Reservation reservation =
                service.reserve(8L, IDEMPOTENCY_KEY, REQUEST_HASH, 21L, 77L);

        assertExpiredLeaseWasReclaimedAndPersisted(reservation, existing, staleToken);
    }

    @Test
    void reserveRetryableFailureRotatesLeaseTokenForSameKey() {
        TimetableAgentAction existing = action("FAILED", REQUEST_HASH);
        existing.setErrorCode("PROVIDER_TIMEOUT");
        String failedToken = existing.getLeaseToken();
        Mockito.when(repository.findByActorIdAndIdempotencyKeyForUpdate(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));
        Mockito.when(repository.saveAndFlush(existing)).thenReturn(existing);

        TimetableAgentActionReservationService.Reservation reservation =
                service.reserve(8L, IDEMPOTENCY_KEY, REQUEST_HASH, 21L, 77L);

        Assertions.assertTrue(reservation.owner() && !failedToken.equals(existing.getLeaseToken())
                        && PENDING.equals(existing.getStatus()) && existing.getErrorCode() == null,
                "retryable failures must rotate the lease token and return to pending");
    }

    @Test
    void reserveRejectsSameKeyWithDifferentRequestHash() {
        TimetableAgentAction existing = action(PENDING, REQUEST_HASH);
        existing.setLeaseExpiresAt(Instant.now().plusSeconds(20));
        Mockito.when(repository.findByActorIdAndIdempotencyKeyForUpdate(8L, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existing));

        assertMismatchedHashIsRejectedWithoutWrite();
    }

    @Test
    void failIgnoresLateFailureFromSupersededLeaseToken() {
        TimetableAgentAction existing = action(PENDING, REQUEST_HASH);
        existing.setLeaseExpiresAt(Instant.now().minusSeconds(1));
        String currentToken = existing.getLeaseToken();
        Mockito.when(repository.findByIdForUpdate(44L)).thenReturn(Optional.of(existing));

        service.fail(44L, "stale-token", "PROVIDER_TIMEOUT");

        Assertions.assertTrue(lateFailureLeftCurrentLeaseUntouched(existing, currentToken),
                "a late failure from a stale token must leave the current lease untouched");
    }

    private TimetableAgentAction action(String status, String requestHash) {
        TimetableAgentAction result = new TimetableAgentAction(21L, 8L, IDEMPOTENCY_KEY, requestHash, null);
        result.setStatus(status);
        return result;
    }

    private void assertLiveDuplicateRemainsUnchangedAndReadOnly(
            TimetableAgentActionReservationService.Reservation reservation, String token, Instant expiry) {
        Assertions.assertTrue(!reservation.owner() && token.equals(reservation.action().getLeaseToken())
                        && expiry.equals(reservation.action().getLeaseExpiresAt()),
                "a live duplicate key must preserve its owner token and deadline");
        verifyNoReservationWrite();
    }

    private void assertExpiredLeaseWasReclaimedAndPersisted(
            TimetableAgentActionReservationService.Reservation reservation,
            TimetableAgentAction action,
            String staleToken) {
        Assertions.assertTrue(reservation.owner() && PENDING.equals(action.getStatus())
                        && !staleToken.equals(action.getLeaseToken())
                        && action.getLeaseExpiresAt().isAfter(Instant.now()),
                "an expired lease must be reclaimed with a new token and deadline");
        Mockito.verify(repository).saveAndFlush(action);
    }

    private void assertMismatchedHashIsRejectedWithoutWrite() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.reserve(8L, IDEMPOTENCY_KEY, "hash-2", 21L, 77L),
                "same key with a different request hash must be rejected");
        verifyNoReservationWrite();
    }

    private boolean lateFailureLeftCurrentLeaseUntouched(TimetableAgentAction action, String token) {
        return PENDING.equals(action.getStatus()) && token.equals(action.getLeaseToken())
                && action.getErrorCode() == null;
    }

    private void verifyNoReservationWrite() {
        Mockito.verify(repository, Mockito.never()).saveAndFlush(ArgumentMatchers.any());
    }
}

