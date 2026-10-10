package io.github.legendaryswords.mechanics;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Monotonic time avoids wall-clock changes and keeps cooldowns across reconnects. */
public final class CooldownBook {
    private record Key(UUID player, String ability) {}
    private final Map<Key, Long> expirations = new HashMap<>();
    private final LongSupplier clock;

    public CooldownBook() { this(System::nanoTime); }
    public CooldownBook(LongSupplier clock) { this.clock = Objects.requireNonNull(clock); }

    public boolean tryUse(UUID player, String ability, Duration duration) {
        Objects.requireNonNull(duration);
        if (duration.isNegative()) throw new IllegalArgumentException("Cooldown cannot be negative");
        if (remainingMillis(player, ability) > 0) return false;
        long nanos;
        try { nanos = duration.toNanos(); }
        catch (ArithmeticException overflow) { nanos = Long.MAX_VALUE; }
        if (nanos > 0) expirations.put(new Key(player, ability), clock.getAsLong() + nanos);
        return true;
    }

    public long remainingMillis(UUID player, String ability) {
        Key key = new Key(player, ability);
        Long expiration = expirations.get(key);
        if (expiration == null) return 0;
        long remaining = expiration - clock.getAsLong();
        if (remaining <= 0) { expirations.remove(key); return 0; }
        return remaining / 1_000_000 + (remaining % 1_000_000 == 0 ? 0 : 1);
    }

    public void purgeExpired() {
        long now = clock.getAsLong();
        expirations.entrySet().removeIf(entry -> entry.getValue() - now <= 0);
    }
}
