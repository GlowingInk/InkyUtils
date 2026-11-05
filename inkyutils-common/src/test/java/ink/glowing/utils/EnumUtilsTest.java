package ink.glowing.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class EnumUtilsTest {
    public enum Mode { FAST, SLOW, camelCase }

    private static Stream<Arguments> parseEnumData() {
        return Stream.of(
                arguments("FAST", Mode.SLOW, Mode.FAST),
                arguments("fAsT", Mode.SLOW, Mode.FAST),
                arguments("CAMELCASE", Mode.SLOW, Mode.camelCase),
                arguments("warp", Mode.SLOW, Mode.SLOW),
                arguments("", Mode.SLOW, Mode.SLOW),
                arguments("warp", null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("parseEnumData")
    public void parseEnumTest(String name, Mode fallback, Mode expected) {
        assertEquals(expected, EnumUtils.parseEnum(name, Mode.class, fallback));
    }
}
