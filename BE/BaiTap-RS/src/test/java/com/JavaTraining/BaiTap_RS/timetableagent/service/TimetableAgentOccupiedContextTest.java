package com.JavaTraining.BaiTap_RS.timetableagent.service;

import java.time.LocalDate;
import java.util.List;

import com.JavaTraining.BaiTap_RS.timetable.domain.entity.TimetableEntry;
import com.JavaTraining.BaiTap_RS.timetableagent.domain.TimetableAgentSnapshot.TimetableAgentSnapshotEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TimetableAgentOccupiedContextTest {

    private static final LocalDate SEP_1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate SEP_9 = LocalDate.of(2026, 9, 9);
    private static final LocalDate SEP_10 = LocalDate.of(2026, 9, 10);
    private static final LocalDate SEP_20 = LocalDate.of(2026, 9, 20);
    private static final LocalDate SEP_21 = LocalDate.of(2026, 9, 21);
    private static final LocalDate SEP_30 = LocalDate.of(2026, 9, 30);
    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);

    @Test
    void publishedContextKeepsOnlyDatesNotAlreadyOccupiedByCurrentRows() {
        TimetableAgentSnapshotEntry current = snapshotEntry(10L, 7L, 3L, 2L, SEP_10, SEP_20);
        TimetableAgentSnapshotEntry overlappingPublished = snapshotEntry(20L, 7L, 3L, 2L, SEP_1, SEP_30);
        TimetableAgentSnapshotEntry separatedPublished = snapshotEntry(21L, 7L, 3L, 2L, OCT_1, OCT_1);
        TimetableAgentSnapshotEntry otherRoom = snapshotEntry(22L, 7L, 3L, 9L, SEP_1, SEP_30);
        TimetableAgentSnapshotEntry otherAssignment = snapshotEntry(23L, 8L, 3L, 2L, SEP_10, SEP_20);
        TimetableAgentSnapshotEntry otherPeriod = snapshotEntry(24L, 7L, 4L, 2L, SEP_10, SEP_20);

        List<TimetableAgentSnapshotEntry> normalized = TimetableAgentOccupiedContext.withoutCurrentOverlap(
                List.of(current), List.of(overlappingPublished, separatedPublished, otherRoom, otherAssignment,
                        otherPeriod));

        assertEquals(List.of(snapshotEntry(20L, 7L, 3L, 2L, SEP_1, SEP_9),
                snapshotEntry(20L, 7L, 3L, 2L, SEP_21, SEP_30), separatedPublished, otherRoom, otherAssignment,
                otherPeriod), normalized);
        assertEquals(snapshotEntry(10L, 7L, 3L, 2L, SEP_10, SEP_20), current);
        assertEquals(SEP_10, current.validFrom());
        assertEquals(SEP_30, overlappingPublished.validTo());
    }

    @Test
    void overlappingPublishedCopiesAreRepresentedOnceAndDisjointDatesRemainSeparate() {
        TimetableAgentSnapshotEntry first = snapshotEntry(20L, 7L, 3L, 2L, SEP_1, SEP_20);
        TimetableAgentSnapshotEntry second = snapshotEntry(21L, 7L, 3L, 2L, SEP_10, SEP_30);

        List<TimetableAgentSnapshotEntry> normalized = TimetableAgentOccupiedContext.withoutCurrentOverlap(
                List.of(), List.of(first, second));

        assertEquals(List.of(first, snapshotEntry(21L, 7L, 3L, 2L, SEP_21, SEP_30)), normalized);
    }

    @Test
    void loadNormalizationUnionsOverlappingRowsWithoutMutatingInputsOrMergingDifferentKeys() {
        TimetableEntry first = entity(100L, 7L, 3L, 2L, SEP_1, SEP_20);
        TimetableEntry second = entity(101L, 7L, 3L, 2L, SEP_10, SEP_30);
        TimetableEntry differentRoom = entity(102L, 7L, 3L, 9L, SEP_10, SEP_30);
        List<TimetableEntry> original = List.of(first, second, differentRoom);

        List<TimetableEntry> normalized = TimetableAgentOccupiedContext.normalizedEntities(original);

        assertEquals(2, normalized.size());
        TimetableEntry merged = normalized.stream().filter(entry -> entry.getFunctionalRoomId().equals(2L))
                .findFirst().orElseThrow();
        assertEquals(SEP_1, merged.getValidFrom());
        assertEquals(SEP_30, merged.getValidTo());
        assertEquals(100L, merged.getId());
        assertEquals(SEP_20, first.getValidTo());
        assertSame(differentRoom, original.get(2));
        assertNotSame(first, merged);
    }

    private TimetableAgentSnapshotEntry snapshotEntry(Long entryId, Long assignmentId, Long periodId, Long roomId,
            LocalDate from, LocalDate to) {
        return new TimetableAgentSnapshotEntry(entryId, assignmentId, periodId, roomId, from, to);
    }

    private TimetableEntry entity(Long id, Long assignmentId, Long periodId, Long roomId,
            LocalDate from, LocalDate to) {
        TimetableEntry entry = new TimetableEntry(4L, assignmentId, periodId, roomId, from, to);
        entry.setId(id);
        return entry;
    }
}
