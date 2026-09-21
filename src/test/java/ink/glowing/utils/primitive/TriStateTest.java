package ink.glowing.utils.primitive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.NoSuchElementException;

import static ink.glowing.utils.primitive.TriState.*;
import static org.junit.jupiter.api.Assertions.*;

public class TriStateTest {
    @Test
    public void testOf() {
        assertEquals(TRUE, TriState.of(true));
        assertEquals(FALSE, TriState.of(false));
        assertEquals(TRUE, TriState.of(Boolean.TRUE));
        assertEquals(UNSET, TriState.of((Boolean) null));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "true, TRUE", "On, TRUE", "YES, TRUE", "enabled, TRUE",
            "false, FALSE", "off, FALSE", "No, FALSE", "DISABLED, FALSE",
            "maybe, UNSET", "'', UNSET", "NULL, UNSET"
    })
    public void testOfString(String str, TriState expected) {
        assertEquals(expected, TriState.of(str));
    }

    @Test
    public void testPresence() {
        assertTrue(TRUE.isTrue() && FALSE.isFalse());
        assertFalse(TRUE.isFalse() || FALSE.isTrue() || UNSET.isTrue() || UNSET.isFalse());
        assertTrue(TRUE.isPresent() && FALSE.isPresent() && UNSET.isEmpty());
        assertFalse(TRUE.isEmpty() || FALSE.isEmpty() || UNSET.isPresent());
    }

    @Test
    public void testNot() {
        assertEquals(FALSE, TRUE.not());
        assertEquals(TRUE, FALSE.not());
        assertEquals(UNSET, UNSET.not());
    }

    @Test
    public void testAsBoolean() {
        assertEquals(Boolean.TRUE, TRUE.asBoolean());
        assertEquals(Boolean.FALSE, FALSE.asBoolean());
        assertNull(UNSET.asBoolean());

        assertTrue(TRUE.asBoolean(false));
        assertTrue(UNSET.asBoolean(true));
        assertFalse(UNSET.asBoolean(() -> false));
        assertTrue(TRUE.asBoolean(() -> fail("Fallback must be lazy")));
    }

    @Test
    public void testOrElseThrow() {
        assertTrue(TRUE.orElseThrow());
        assertFalse(FALSE.orElseThrow());
        assertThrows(NoSuchElementException.class, UNSET::orElseThrow);
        assertThrows(IllegalStateException.class, () -> UNSET.orElseThrow(IllegalStateException::new));
    }

    @Test
    public void testIfPresentOrElse() {
        StringBuilder log = new StringBuilder();
        for (TriState state : TriState.values()) {
            state.ifPresent(log::append);
            state.ifPresentOrElse(log::append, () -> log.append("-"));
        }
        assertEquals("truetruefalsefalse-", log.toString());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "TRUE, true, true, true",
            "TRUE, false, false, false",
            "TRUE, NULL, false, false",
            "FALSE, false, true, true",
            "FALSE, true, false, false",
            "FALSE, NULL, false, false",
            "UNSET, true, true, false",
            "UNSET, false, true, false",
            "UNSET, NULL, true, true"
    })
    public void testValidity(TriState state, Boolean value, boolean valid, boolean exact) {
        assertEquals(valid, state.isValidFor(value));
        assertEquals(exact, state.isExactly(value));
        if (value != null) {
            assertEquals(valid, state.isValidFor(value.booleanValue()));
            assertEquals(exact, state.isExactly(value.booleanValue()));
        }
    }

    @Test
    public void testMapperDefaults() {
        Mapper mapper = Mapper.builder().build();
        assertEquals(TRUE, mapper.byString("true"));
        assertEquals(FALSE, mapper.byString("False"));
        assertEquals(UNSET, mapper.byString("unset"));
        assertEquals(UNSET, mapper.byString("yes"), "Synonyms belong to Mapper.DEFAULT only");
        assertEquals(UNSET, mapper.byString(null));
        assertEquals("TRUE", mapper.toString(TRUE));
        assertEquals("UNSET", mapper.toString(UNSET));
    }

    @Test
    public void testMapperCustom() {
        Mapper mapper = Mapper.builder()
                .main(TRUE, "Enabled").addVariants(TRUE, "on")
                .main(FALSE, "Disabled").addVariants(FALSE, "off")
                .main(UNSET, "Any").addVariants(UNSET, "*")
                .fallback(FALSE)
                .build();

        assertEquals(TRUE, mapper.byString("ENABLED"));
        assertEquals(TRUE, mapper.byString("On"));
        assertEquals(FALSE, mapper.byString("disabled"));
        assertEquals(UNSET, mapper.byString("any"));
        assertEquals(UNSET, mapper.byString("*"));
        assertEquals(FALSE, mapper.byString("garbage"));
        assertEquals(FALSE, mapper.byString(null));
        assertEquals("Enabled", mapper.toString(TRUE), "Main name keeps its casing");
    }

    @Test
    public void testMapperClearVariants() {
        Mapper mapper = Mapper.builder()
                .addVariants(TRUE, "yes").clearVariants(TRUE)
                .addVariants(FALSE, "no").clearVariants(FALSE)
                .addVariants(UNSET, "any").clearVariants(UNSET)
                .build();

        assertEquals(UNSET, mapper.byString("yes"));
        assertEquals(UNSET, mapper.byString("any"));
        assertEquals(TRUE, mapper.byString("true"), "Main name survives clearing");
    }

    @Test
    public void testMapperConflicts() {
        assertThrows(IllegalStateException.class, () -> Mapper.builder().addVariants(FALSE, "true").build());
        assertThrows(IllegalStateException.class, () -> Mapper.builder().addVariants(TRUE, "yes").addVariants(FALSE, "YES").build());
        assertThrows(IllegalStateException.class, () -> Mapper.builder().main(TRUE, "").main(FALSE, "").build());
        assertDoesNotThrow(() -> Mapper.builder().addVariants(TRUE, "true", "TRUE", "yes", "Yes").build());
    }

    @Test
    public void testMapperBuilderReuse() {
        Mapper.Builder builder = Mapper.builder();
        Mapper first = builder.build();
        Mapper second = builder.addVariants(FALSE, "nope").build();

        assertEquals(UNSET, first.byString("nope"));
        assertEquals(FALSE, second.byString("nope"));
    }
}
