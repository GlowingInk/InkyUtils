package ink.glowing.utils.hash;

import it.unimi.dsi.fastutil.Hash;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CaseInsensitiveTest {
    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "abc, abc, true",
            "abc, ABC, true",
            "Hello, hELLO, true",
            "abc, abd, false",
            "abc, abcd, false",
            "ı, I, true",
            "ı, i, true",
            "I, i, true",
            "NULL, NULL, true",
            "NULL, a, false",
            "a, NULL, false"
    })
    public void testStrategy(String left, String right, boolean expected) {
        Hash.Strategy<String> strategy = CaseInsensitive.strategy();

        assertEquals(expected, strategy.equals(left, right));
        if (expected) {
            assertEquals(strategy.hashCode(left), strategy.hashCode(right), "Equal strings must have equal hashes");
        }
    }

    @Test
    public void testMap() {
        Map<String, Integer> map = CaseInsensitive.newMap();
        map.put("Key", 1);
        map.put("KEY", 2);

        assertEquals(1, map.size());
        assertEquals(2, map.get("key"));
        assertTrue(map.containsKey("kEy"));
        assertFalse(map.containsKey("other"));
    }

    @Test
    public void testMapDotlessI() {
        Map<String, Integer> map = CaseInsensitive.newMap();
        map.put("ı", 1);

        assertEquals(1, map.get("I"));
        assertEquals(1, map.get("i"));
    }

    @Test
    public void testSets() {
        for (Set<String> set : List.of(CaseInsensitive.newSet(), CaseInsensitive.newLinkedSet())) {
            assertTrue(set.add("Alpha"));
            assertFalse(set.add("ALPHA"));

            assertEquals(1, set.size());
            assertTrue(set.contains("alpha"));
        }
    }
}
