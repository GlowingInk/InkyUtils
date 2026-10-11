package ink.glowing.utils.rng;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChanceTest {
    @Test
    public void testOf() {
        assertEquals(Chance.Constant.NEVER, Chance.of(-1));
        assertEquals(Chance.Constant.NEVER, Chance.of(0));
        assertEquals(Chance.Constant.NEVER, Chance.of(Double.NaN));
        assertEquals(Chance.Constant.COIN_FLIP, Chance.of(0.5));
        assertEquals(Chance.Constant.ALWAYS, Chance.of(1));
        assertEquals(Chance.Constant.ALWAYS, Chance.of(2));
        assertEquals(new Chance.Straight(0.25), Chance.of(0.25));
        assertEquals(new Chance.Straight(0.25), Chance.ofPercent(25));
    }
}
