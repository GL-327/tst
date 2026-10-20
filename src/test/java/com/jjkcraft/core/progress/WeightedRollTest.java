package com.jjkcraft.core.progress;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WeightedRollTest {
    @Test void millionRollsFollowConfiguredWeights() {
        List<Integer> weights = List.of(4, 3, 35, 35, 35);
        int[] counts = new int[weights.size()]; Random random = new Random(79631);
        for (int n = 0; n < 1_000_000; n++) counts[WeightedRoll.index(weights, random.nextDouble())]++;
        double total = weights.stream().mapToInt(Integer::intValue).sum();
        for (int n = 0; n < counts.length; n++) assertEquals(weights.get(n) / total, counts[n] / 1_000_000.0, 0.01);
    }
    @Test void emptyAndZeroPoolsReturnFallbackSignal() {
        assertEquals(-1, WeightedRoll.index(List.of(), 0.5));
        assertEquals(-1, WeightedRoll.index(List.of(0, 0), 0.5));
    }
    @Test void boundariesNeverChooseZeroWeightOrOverflow() {
        assertEquals(1, WeightedRoll.index(List.of(0, 5, 0), 0));
        assertEquals(1, WeightedRoll.index(List.of(0, 5, 0), 1));
        assertEquals(1, WeightedRoll.index(List.of(Integer.MAX_VALUE, Integer.MAX_VALUE), 0.9));
    }
}
