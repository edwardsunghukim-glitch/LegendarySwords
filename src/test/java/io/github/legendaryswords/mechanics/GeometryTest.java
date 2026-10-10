package io.github.legendaryswords.mechanics;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    @Test
    void beamCollisionUsesTheFiniteSegmentInsteadOfAnInfiniteLine() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(4, 0, 0);

        assertEquals(9, Geometry.distanceSquaredToSegment(new Vector(2, 3, 0), start, end), 1e-9);
        assertEquals(5, Geometry.distanceSquaredToSegment(new Vector(-2, 1, 0), start, end), 1e-9);
        assertEquals(10, Geometry.distanceSquaredToSegment(new Vector(7, 1, 0), start, end), 1e-9);
        assertEquals(new Vector(0, 0, 0), start, "Collision checks must not change caller vectors");
        assertEquals(new Vector(4, 0, 0), end);
    }

    @Test
    void zeroLengthBeamHasPointDistanceInsteadOfNaN() {
        Vector origin = new Vector(1, 2, 3);
        assertEquals(25, Geometry.distanceSquaredToSegment(new Vector(4, 6, 3), origin, origin), 1e-9);
        assertEquals(0, Geometry.distanceSquaredToSegment(origin, origin, origin), 1e-9);
    }

    @Test
    void coneCollisionNormalizesDirectionAndIncludesRangeAndAngleBoundaries() {
        Vector direction = new Vector(20, 0, 0);
        assertTrue(Geometry.inCone(new Vector(2, 0, 0), direction, 2, 30));
        assertTrue(Geometry.inCone(new Vector(Math.sqrt(3), 1, 0), direction, 2, 30));
        assertTrue(Geometry.inCone(new Vector(0, 2, 0), direction, 2, 90));
        assertTrue(Geometry.inCone(new Vector(), direction, 0, 30));

        assertFalse(Geometry.inCone(new Vector(2.01, 0, 0), direction, 2, 30));
        assertFalse(Geometry.inCone(new Vector(1, 1, 0), direction, 2, 30));
        assertFalse(Geometry.inCone(new Vector(-1, 0, 0), direction, 2, 90));
        assertEquals(new Vector(20, 0, 0), direction, "Collision checks must not normalize caller vectors in place");
    }

    @Test
    void invalidConeInputsCannotProduceHits() {
        Vector forward = new Vector(1, 0, 0);
        assertFalse(Geometry.inCone(forward, new Vector(), 5, 30));
        assertFalse(Geometry.inCone(forward, forward, -1, 30));
        assertFalse(Geometry.inCone(forward, forward, 5, -1));
        assertFalse(Geometry.inCone(forward, forward, 5, 181));
        assertFalse(Geometry.inCone(new Vector(Double.NaN, 0, 0), forward, 5, 30));
        assertFalse(Geometry.inCone(forward, new Vector(Double.POSITIVE_INFINITY, 0, 0), 5, 30));
        assertFalse(Geometry.inCone(forward, forward, Double.NaN, 30));
        assertFalse(Geometry.inCone(forward, forward, 5, Double.NaN));
    }
}
