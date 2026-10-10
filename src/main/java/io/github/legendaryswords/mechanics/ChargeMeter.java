package io.github.legendaryswords.mechanics;

/** Charge cannot exceed one ultimate, even after an oversized reward. */
public final class ChargeMeter {
    public static final int MAX = 100;
    private int value;

    public ChargeMeter(int initial) { set(initial); }
    public int value() { return value; }
    public boolean isFull() { return value == MAX; }
    public int add(int amount) {
        value = (int) Math.clamp((long) value + amount, 0L, MAX);
        return value;
    }
    public void set(int amount) { value = Math.clamp(amount, 0, MAX); }
    public boolean consumeUltimate() {
        if (!isFull()) return false;
        value = 0;
        return true;
    }
}
