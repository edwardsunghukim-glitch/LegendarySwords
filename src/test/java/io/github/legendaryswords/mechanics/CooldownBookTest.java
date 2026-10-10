package io.github.legendaryswords.mechanics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class CooldownBookTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void cooldownsAreIndependentForEachPlayerAndAbility() {
        AtomicLong clock = new AtomicLong(500);
        CooldownBook book = new CooldownBook(clock::get);

        assertTrue(book.tryUse(ALICE, "master_sword:ability", Duration.ofSeconds(5)));
        assertFalse(book.tryUse(ALICE, "master_sword:ability", Duration.ofSeconds(5)));
        assertTrue(book.tryUse(BOB, "master_sword:ability", Duration.ofSeconds(5)));
        assertTrue(book.tryUse(ALICE, "master_sword:ultimate", Duration.ofSeconds(5)));
        assertTrue(book.tryUse(ALICE, "flameblade:ability", Duration.ofSeconds(5)));
        assertEquals(5_000, book.remainingMillis(ALICE, "master_sword:ability"));
        assertEquals(0, book.remainingMillis(BOB, "unseen"));
    }

    @Test
    void timeIsRoundedUpAndUseBecomesAvailableAtExactExpiry() {
        AtomicLong clock = new AtomicLong(10_000);
        CooldownBook book = new CooldownBook(clock::get);
        assertTrue(book.tryUse(ALICE, "beam", Duration.ofNanos(1_000_001)));
        assertEquals(2, book.remainingMillis(ALICE, "beam"));

        clock.incrementAndGet();
        assertEquals(1, book.remainingMillis(ALICE, "beam"));
        clock.addAndGet(999_999);
        assertEquals(1, book.remainingMillis(ALICE, "beam"));
        assertFalse(book.tryUse(ALICE, "beam", Duration.ofSeconds(1)));

        clock.incrementAndGet();
        assertEquals(0, book.remainingMillis(ALICE, "beam"));
        assertTrue(book.tryUse(ALICE, "beam", Duration.ofSeconds(1)));
        assertEquals(1_000, book.remainingMillis(ALICE, "beam"));
    }

    @Test
    void nanoTimeWrappingDoesNotUnlockAnActiveCooldownEarly() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 4);
        CooldownBook book = new CooldownBook(clock::get);
        assertTrue(book.tryUse(ALICE, "beam", Duration.ofNanos(10)));

        clock.addAndGet(6);
        assertEquals(1, book.remainingMillis(ALICE, "beam"));
        assertFalse(book.tryUse(ALICE, "beam", Duration.ofNanos(10)));
        clock.addAndGet(4);
        assertEquals(0, book.remainingMillis(ALICE, "beam"));
        assertTrue(book.tryUse(ALICE, "beam", Duration.ofNanos(10)));
    }

    @Test
    void hugeDurationsDoNotOverflowOrBecomeImmediatelyReusable() {
        AtomicLong clock = new AtomicLong(123);
        CooldownBook book = new CooldownBook(clock::get);
        assertTrue(book.tryUse(ALICE, "beam", Duration.ofSeconds(Long.MAX_VALUE)));
        assertEquals(9_223_372_036_855L, book.remainingMillis(ALICE, "beam"));
        assertFalse(book.tryUse(ALICE, "beam", Duration.ofSeconds(1)));
    }

    @Test
    void purgingExpiredEntriesPreservesStillActiveCooldowns() {
        AtomicLong clock = new AtomicLong();
        CooldownBook book = new CooldownBook(clock::get);
        book.tryUse(ALICE, "short", Duration.ofSeconds(1));
        book.tryUse(ALICE, "long", Duration.ofSeconds(5));
        clock.set(1_000_000_000);

        book.purgeExpired();

        assertEquals(0, book.remainingMillis(ALICE, "short"));
        assertTrue(book.tryUse(ALICE, "short", Duration.ofSeconds(1)));
        assertEquals(4_000, book.remainingMillis(ALICE, "long"));
        assertFalse(book.tryUse(ALICE, "long", Duration.ofSeconds(1)));
    }

    @Test
    void zeroDurationDoesNotCreateOrBypassAnActiveCooldown() {
        AtomicLong clock = new AtomicLong();
        CooldownBook book = new CooldownBook(clock::get);
        assertTrue(book.tryUse(ALICE, "beam", Duration.ZERO));
        assertTrue(book.tryUse(ALICE, "beam", Duration.ZERO));
        assertEquals(0, book.remainingMillis(ALICE, "beam"));
        assertTrue(book.tryUse(ALICE, "beam", Duration.ofSeconds(1)));
        assertFalse(book.tryUse(ALICE, "beam", Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> book.tryUse(BOB, "beam", Duration.ofNanos(-1)));
    }

    @Test
    void adminResetClearsOnlyTheChosenPlayersCooldowns() {
        AtomicLong clock = new AtomicLong();
        CooldownBook book = new CooldownBook(clock::get);
        book.tryUse(ALICE, "master_sword:ultimate", Duration.ofSeconds(75));
        book.tryUse(ALICE, "pose", Duration.ofSeconds(2));
        book.tryUse(BOB, "master_sword:ultimate", Duration.ofSeconds(75));

        book.clear(ALICE);

        assertEquals(0, book.remainingMillis(ALICE, "master_sword:ultimate"));
        assertEquals(0, book.remainingMillis(ALICE, "pose"));
        assertEquals(75_000, book.remainingMillis(BOB, "master_sword:ultimate"));
    }
}
