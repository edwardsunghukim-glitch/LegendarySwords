package io.github.legendaryswords.mechanics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChargeMeterTest {
    @Test
    void anUltimateCannotConsumePartialChargeAndCanConsumeFullChargeOnlyOnce() {
        ChargeMeter charge = new ChargeMeter(99);

        assertFalse(charge.consumeUltimate());
        assertEquals(99, charge.value(), "A rejected ultimate must preserve earned charge");
        assertEquals(100, charge.add(10));
        assertTrue(charge.isFull());
        assertTrue(charge.consumeUltimate());
        assertEquals(0, charge.value());
        assertFalse(charge.isFull());
        assertFalse(charge.consumeUltimate(), "One full meter must not pay for two ultimates");
    }

    @Test
    void earningAndRemovingChargeSaturatesWithoutIntegerOverflow() {
        ChargeMeter charge = new ChargeMeter(99);

        assertEquals(100, charge.add(Integer.MAX_VALUE));
        assertEquals(0, charge.add(Integer.MIN_VALUE));
        assertEquals(20, charge.add(20));
        assertEquals(10, charge.add(-10));
        assertEquals(0, charge.add(-20));
    }

    @Test
    void adminAndInitialValuesCannotEscapeMeterBounds() {
        ChargeMeter charge = new ChargeMeter(Integer.MIN_VALUE);
        assertEquals(0, charge.value());

        charge.set(Integer.MAX_VALUE);
        assertEquals(100, charge.value());
        charge.set(-1);
        assertEquals(0, charge.value());
        charge.set(35);
        assertEquals(35, charge.value());
        assertFalse(charge.isFull());

        assertEquals(100, new ChargeMeter(Integer.MAX_VALUE).value());
    }
}
