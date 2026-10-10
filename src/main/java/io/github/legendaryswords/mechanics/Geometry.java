package io.github.legendaryswords.mechanics;

import org.bukkit.util.Vector;

public final class Geometry {
    private Geometry() {}

    public static double distanceSquaredToSegment(Vector point, Vector start, Vector end) {
        Vector segment = end.clone().subtract(start);
        double lengthSquared = segment.lengthSquared();
        if (lengthSquared < 1.0e-12) return point.distanceSquared(start);
        double t = Math.clamp(point.clone().subtract(start).dot(segment) / lengthSquared, 0.0, 1.0);
        return point.distanceSquared(start.clone().add(segment.multiply(t)));
    }

    public static boolean inCone(Vector delta, Vector direction, double maxRange, double halfAngleDegrees) {
        if (!Double.isFinite(maxRange) || maxRange < 0 || !Double.isFinite(halfAngleDegrees)
                || halfAngleDegrees < 0 || halfAngleDegrees > 180) return false;
        double distanceSquared = delta.lengthSquared();
        double directionSquared = direction.lengthSquared();
        if (!Double.isFinite(distanceSquared) || !Double.isFinite(directionSquared)
                || directionSquared < 1.0e-12 || distanceSquared > maxRange * maxRange + 1.0e-10) return false;
        if (distanceSquared < 1.0e-12) return true;
        double cosine = delta.dot(direction) / Math.sqrt(distanceSquared * directionSquared);
        return cosine + 1.0e-10 >= Math.cos(Math.toRadians(halfAngleDegrees));
    }
}
