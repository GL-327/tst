package com.jjkcraft.core.progress;

import java.util.List;

/** Shared deterministic weighted selection; a caller supplies its own server RNG. */
public final class WeightedRoll {
    private WeightedRoll() { }
    public static int index(List<Integer> weights, double unit) {
        long total = weights.stream().mapToLong(w -> Math.max(0, w)).sum();
        if (total == 0) return -1;
        double remaining = Math.clamp(unit, 0.0, Math.nextDown(1.0)) * total;
        for (int n = 0; n < weights.size(); n++) {
            remaining -= Math.max(0, weights.get(n));
            if (remaining < 0) return n;
        }
        return -1;
    }
}
