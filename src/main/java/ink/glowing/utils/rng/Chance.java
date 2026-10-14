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
     * @return the probability
     */
    @Contract(pure = true)
    double probability();

    /**
     * Tests this chance using the specified random generator.
     * @param rng the random generator
     * @return {@code true} with the probability of {@link #probability()}
     */
    @Override
    boolean test(@NotNull RandomGenerator rng);

    /**
     * Tests this chance using thread's local random.
     * @return {@code true} with the probability of {@link #probability()}
     * @see #test(RandomGenerator)
     * @see RngUtils#threadRandom()
     */
    default boolean test() {
        return test(RngUtils.threadRandom());
    }

    /**
     * Returns a chance with the specified probability. Probabilities of {@code 0} or below, {@code NaN},
     * {@code 0.5} and {@code 1} or above return a {@link Constant}.
     * @param probability the probability of {@code true}
     * @return the chance
     */
    @Contract(pure = true)
    static @NotNull Chance of(double probability) {
        if (probability <= 0 || Double.isNaN(probability)) {
            return Constant.NEVER;
        } else if (probability == 0.5) {
            return Constant.COIN_FLIP;
        } else if (probability >= 1) {
            return Constant.ALWAYS;
        } else {
            return new Straight(probability);
        }
    }

    /**
     * Returns a chance with the specified percent probability.
     * @param percent the probability of {@code true}, in percent
     * @return the chance
     * @see #of(double)
     */
    @Contract(pure = true)
    static @NotNull Chance ofPercent(double percent) {
        return of(percent / 100.0);
    }

    /**
     * A chance that compares its probability to {@link RandomGenerator#nextDouble()}.
     * @param probability the probability of {@code true}
     */
    record Straight(double probability) implements Chance {
        @Override
        public boolean test(@NotNull RandomGenerator rng) {
            return probability > rng.nextDouble();
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
            public double probability() {
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
            public double probability() {
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
            public double probability() {
                return 0;
            }
        }
    }
}
