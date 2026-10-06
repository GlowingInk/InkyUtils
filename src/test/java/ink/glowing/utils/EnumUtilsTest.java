package ink.glowing.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumUtilsTest {
    public enum Mode { FAST, SLOW, camelCase }

    private static Stream<Arguments> asEnumData() {
        return Stream.of(
                Arguments.of("FAST", Mode.SLOW, Mode.FAST),
                Arguments.of("fAsT", Mode.SLOW, Mode.FAST),
                Arguments.of("CAMELCASE", Mode.SLOW, Mode.camelCase),
                Arguments.of("warp", Mode.SLOW, Mode.SLOW),
                Arguments.of("", Mode.SLOW, Mode.SLOW),
                Arguments.of("warp", null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("asEnumData")
    public void asEnumTest(String name, Mode fallback, Mode expected) {
        assertEquals(expected, EnumUtils.asEnum(name, Mode.class, fallback));
    }
}
