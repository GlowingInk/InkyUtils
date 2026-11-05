package ink.glowing.utils.bool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static ink.glowing.utils.bool.TriState.*;
import static org.junit.jupiter.api.Assertions.*;

public class TriStateTest {
    @Test
    public void testOf() {
        assertEquals(TRUE, TriState.of(true));
        assertEquals(FALSE, TriState.of(false));
        assertEquals(TRUE, TriState.of(Boolean.TRUE));
        assertEquals(UNSET, TriState.of(null));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "true, TRUE", "On, TRUE", "YES, TRUE", "enabled, TRUE",
            "false, FALSE", "off, FALSE", "No, FALSE", "DISABLED, FALSE",
            "maybe, UNSET", "'', UNSET", "NULL, UNSET"
    })
    public void testParse(String str, TriState expected) {
        assertEquals(expected, TriState.parseNatural(str));
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

    @ParameterizedTest
    @CsvSource({
            "TRUE, TRUE, TRUE, TRUE",
            "TRUE, FALSE, FALSE, TRUE",
            "TRUE, UNSET, TRUE, TRUE",
            "FALSE, TRUE, FALSE, TRUE",
            "FALSE, FALSE, FALSE, FALSE",
            "FALSE, UNSET, FALSE, FALSE",
            "UNSET, TRUE, TRUE, TRUE",
            "UNSET, FALSE, FALSE, FALSE",
            "UNSET, UNSET, UNSET, UNSET"
    })
    public void testAndOr(TriState left, TriState right, TriState and, TriState or) {
        assertEquals(and, left.and(right));
        assertEquals(or, left.or(right));
        assertEquals(and, left.and(right.asBoolean()));
        assertEquals(or, left.or(right.asBoolean()));
        assertEquals(and, left.and(() -> right));
        assertEquals(or, left.or(() -> right));
        if (right.isPresent()) {
            assertEquals(and, left.and(right.isTrue()));
            assertEquals(or, left.or(right.isTrue()));
            assertEquals(and, left.and(right::isTrue));
            assertEquals(or, left.or(right::isTrue));
        }
    }

    @Test
    public void testAndOrLazy() {
        BooleanSupplier failingValue = () -> fail("Supplier must be lazy");
        Supplier<TriState> failingState = () -> fail("Supplier must be lazy");
        assertEquals(FALSE, FALSE.and(failingValue));
        assertEquals(FALSE, FALSE.and(failingState));
        assertEquals(TRUE, TRUE.or(failingValue));
        assertEquals(TRUE, TRUE.or(failingState));
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
        List<Boolean> present = new ArrayList<>();
        TRUE.ifPresent(present::add);
        FALSE.ifPresent(present::add);
        UNSET.ifPresent(present::add);
        assertEquals(List.of(true, false), present);

        List<Boolean> presentOrElse = new ArrayList<>();
        TRUE.ifPresentOrElse(presentOrElse::add, Assertions::fail);
        FALSE.ifPresentOrElse(presentOrElse::add, Assertions::fail);
        UNSET.ifPresentOrElse(_ -> fail(), () -> presentOrElse.add(null));
        assertEquals(Arrays.asList(true, false, null), presentOrElse);
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
    public void testMapperCustom() {
        Mapper mapper = Mapper.builder()
                .name(TRUE, "Enabled").addVariants(true, "on")
                .name(FALSE, "Disabled").addVariants(false, "off")
                .name(UNSET, "Any")
                .fallback(FALSE)
                .build();

        assertEquals(TRUE, mapper.parse("ENABLED"));
        assertEquals(TRUE, mapper.parse("On"));
        assertEquals(FALSE, mapper.parse("disabled"));
        assertEquals(UNSET, mapper.parse("any"));
        assertEquals(FALSE, mapper.parse("garbage"));
        assertEquals(FALSE, mapper.parse(null));
        assertEquals("Enabled", mapper.name(TRUE), "Main name keeps its casing");
    }

    @Test
    public void testMapperClearVariants() {
        Mapper mapper = Mapper.builder()
                .addVariants(true, "yes").clearVariants(true)
                .addVariants(false, "no").clearVariants(false)
                .build();

        assertEquals(UNSET, mapper.parse("yes"));
        assertEquals(TRUE, mapper.parse("true"), "Main name survives clearing");
    }

    @Test
    public void testMapperConflicts() {
        assertThrows(IllegalStateException.class, () -> Mapper.builder().addVariants(false, "true").build());
        assertThrows(IllegalStateException.class, () -> Mapper.builder().addVariants(true, "yes").addVariants(false, "YES").build());
        assertThrows(IllegalStateException.class, () -> Mapper.builder().name(TRUE, "").name(FALSE, "").build());
        assertDoesNotThrow(() -> Mapper.builder().addVariants(true, "true", "TRUE", "yes", "Yes").build());
    }

    @Test
    public void testMapperBuilderReuse() {
        Mapper.Builder builder = Mapper.builder();
        Mapper first = builder.build();
        Mapper second = builder.addVariants(false, "nope").build();

        assertEquals(UNSET, first.parse("nope"));
        assertEquals(FALSE, second.parse("nope"));
    }
}
