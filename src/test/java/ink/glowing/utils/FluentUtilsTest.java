package ink.glowing.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FluentUtilsTest {
    static Stream<Arguments> attemptData() {
        return Stream.of(
                Arguments.of((Supplier<Integer>) () -> Integer.parseInt("5"), Optional.of(5)),
                Arguments.of((Supplier<Integer>) () -> Integer.parseInt("five"), Optional.empty()),
                Arguments.of((Supplier<Integer>) () -> null, Optional.empty())
        );
    }

    @ParameterizedTest
    @MethodSource("attemptData")
    public void testAttempt(Supplier<Integer> supplier, Optional<Integer> expected) {
        assertEquals(expected, FluentUtils.attempt(supplier));
    }
}
