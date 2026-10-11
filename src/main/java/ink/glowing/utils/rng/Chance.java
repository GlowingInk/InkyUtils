package ink.glowing.utils.rng;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * A probability of something happening, tested against a random generator.
 */
public interface Chance extends Predicate<RandomGenerator> {
    /**
     * Returns the probability of {@link #test(RandomGenerator)} returning {@code true}.
     * @return the probability, in {@code [0, 1]}
     */
    @Contract(pure = true)
    double chance();

    /**
     * Tests this chance using the specified random generator.
     * @param rng the random generator
     * @return {@code true} with the probability of {@link #chance()}
     */
    @Override
    boolean test(@NotNull RandomGenerator rng);

    /**
     * Tests this chance using {@link RngUtils#threadRandom()}.
     * @return {@code true} with the probability of {@link #chance()}
     * @see #test(RandomGenerator)
     */
    default boolean test() {
        return test(RngUtils.threadRandom());
    }

    /**
     * Returns a chance with the specified probability. Probabilities of {@code 0} or below, {@code NaN},
     * {@code 0.5} and {@code 1} or above return a {@link Constant}.
     * @param chance the probability of {@code true}
     * @return the chance
     */
    @Contract(pure = true)
    static @NotNull Chance of(double chance) {
        if (chance <= 0 || Double.isNaN(chance)) {
            return Constant.NEVER;
        } else if (chance == 0.5) {
            return Constant.COIN_FLIP;
        } else if (chance >= 1) {
            return Constant.ALWAYS;
        } else {
            return new Straight(chance);
        }
    }

    /**
     * A chance that compares its probability to {@link RandomGenerator#nextDouble()}.
     * @param chance the probability of {@code true}
     */
    record Straight(double chance) implements Chance {
        @Override
        public boolean test(@NotNull RandomGenerator rng) {
            return chance > rng.nextDouble();
        }
    }

    /**
     * Chances with fixed probabilities.
     */
    enum Constant implements Chance {
        /**
         * Always {@code true}.
         */
        ALWAYS {
            @Override
            public boolean test(@NotNull RandomGenerator rng) {
                return true;
            }

            @Override
            public double chance() {
                return 1;
            }
        },
        /**
         * {@code true} half of the time, using {@link RandomGenerator#nextBoolean()}.
         */
        COIN_FLIP {
            @Override
            public boolean test(@NotNull RandomGenerator rng) {
                return rng.nextBoolean();
            }

            @Override
            public double chance() {
                return 0.5;
            }
        },
        /**
         * Never {@code true}.
         */
        NEVER {
            @Override
            public boolean test(@NotNull RandomGenerator rng) {
                return false;
            }

            @Override
            public double chance() {
                return 0;
            }
        }
    }
}
