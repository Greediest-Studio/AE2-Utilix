package com.ae2utilix;

public enum CpuAccessMode {
    ALL(0),
    PLAYER_ONLY(1),
    AUTO_ONLY(2);

    public static final CpuAccessMode[] VALUES = values();
    public final int id;

    CpuAccessMode(int id) {
        this.id = id;
    }

    public CpuAccessMode next() {
        return VALUES[(this.id + 1) % VALUES.length];
    }

    public CpuAccessMode previous() {
        return VALUES[(this.id - 1 + VALUES.length) % VALUES.length];
    }

    public static CpuAccessMode fromId(int id) {
        if (id >= 0 && id < VALUES.length) return VALUES[id];
        return ALL;
    }

    public boolean allowsPlayer() {
        return this == ALL || this == PLAYER_ONLY;
    }

    public boolean allowsAutomation() {
        return this == ALL || this == AUTO_ONLY;
    }
}
