package ink.glowing.collections;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class HashListTest {
    @DataProvider
    public Object[][] hashListData() {
        return new Object[][]{
                {new String[]{}, 0},
                {new String[]{"a"}, 1},
                {new String[]{"a", "b", "c"}, 3},
                {new String[]{"a", "b", "a"}, 3}
        };
    }

    @Test(dataProvider = "hashListData")
    public void testSize(String[] input, int expectedSize) {
        HashList<String> list = HashList.of(input);
        assertEquals(list.size(), expectedSize);
    }

    @DataProvider
    public Object[][] containsData() {
        return new Object[][]{
                {new String[]{}, "x", false},
                {new String[]{"a"}, "a", true},
                {new String[]{"a"}, "b", false},
                {new String[]{"a", "b", "c"}, "b", true},
                {new String[]{"a", "b", "c"}, "d", false},
                {new String[]{"a", "b", "a"}, "a", true},
                {new String[]{"a", "b", "a"}, "c", false}
        };
    }

    @Test(dataProvider = "containsData")
    public void testContains(String[] input, String query, boolean expected) {
        HashList<String> list = HashList.of(input);
        assertEquals(list.contains(query), expected,
                String.format("contains('%s') should be %b", query, expected));
    }

    @DataProvider
    public Object[][] indexOfData() {
        return new Object[][]{
                {new String[]{}, "x", -1},
                {new String[]{"a"}, "a", 0},
                {new String[]{"a"}, "b", -1},
                {new String[]{"a", "b", "c"}, "c", 2},
                {new String[]{"a", "b", "a"}, "a", 0},
                {new String[]{"a", "b", "a"}, "b", 1}
        };
    }

    @Test(dataProvider = "indexOfData")
    public void testIndexOf(String[] input, String query, int expected) {
        HashList<String> list = HashList.of(input);
        assertEquals(list.indexOf(query), expected);
    }

    @DataProvider
    public Object[][] lastIndexOfData() {
        return new Object[][]{
                {new String[]{}, "x", -1},
                {new String[]{"a"}, "a", 0},
                {new String[]{"a"}, "b", -1},
                {new String[]{"a", "b", "c"}, "c", 2},
                {new String[]{"a", "b", "a"}, "a", 2},
                {new String[]{"a", "b", "a"}, "b", 1}
        };
    }

    @Test(dataProvider = "lastIndexOfData")
    public void testLastIndexOf(String[] input, String query, int expected) {
        HashList<String> list = HashList.of(input);
        assertEquals(list.lastIndexOf(query), expected);
    }

    @DataProvider
    public Object[][] getAndToArrayData() {
        return new Object[][]{
                {new String[]{}, new String[]{}},
                {new String[]{"a"}, new String[]{"a"}},
                {new String[]{"a", "b"}, new String[]{"a", "b"}},
                {new String[]{"a", "b", "c"}, new String[]{"a", "b", "c"}},
                {new String[]{"a", "b", "a"}, new String[]{"a", "b", "a"}},
        };
    }

    @Test(dataProvider = "getAndToArrayData")
    public void testGetAndToArray(String[] input, String[] expectedArray) {
        HashList<String> list = HashList.of(input);
        assertEqualsNoOrder(list.toArray(), expectedArray);

        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(list.get(i), expectedArray[i]);
        }
    }

    @DataProvider
    public Object[][] nullBehaviorData() {
        return new Object[][]{
                {new String[]{"a", "b"}, -1, -1},
                {new String[]{}, -1, -1},
                {new String[]{"a", null, "b"}, 1, 1},
        };
    }

    @Test(dataProvider = "nullBehaviorData")
    public void testNullBehavior(String[] input, int expectedIndexOfNull, int expectedLastIndexOfNull) {
        HashList<String> list = HashList.of(input);

        boolean inputContainsNull = false;
        for (String s : input) {
            if (s == null) {
                inputContainsNull = true;
                break;
            }
        }

        assertEquals(list.contains(null), inputContainsNull);
        assertEquals(list.indexOf(null), expectedIndexOfNull);
        assertEquals(list.lastIndexOf(null), expectedLastIndexOfNull);
    }

    @Test
    public void testCustomStrategy() {
        var composer = new HashList.Composer<String>().containsStrategy(CaseInsensitive.strategy());
        List.of("Alpha", "Beta", "Gamma").forEach(composer::add);
        HashList<String> list = composer.finish();

        assertTrue(list.contains("alpha"), "Should find case-insensitive 'alpha'");
        assertTrue(list.contains("BETA"), "Should find case-insensitive 'BETA'");
        assertFalse(list.contains("delta"), "Should not find 'delta'");

        assertEquals(list.size(), 3, "Size should be 3");

        assertEquals(list.get(0), "Alpha", "First element should be 'Alpha'");
        assertEquals(list.get(1), "Beta", "Second element should be 'Beta'");
        assertEquals(list.get(2), "Gamma", "Third element should be 'Gamma'");

        assertEquals(list.indexOf("alpha"), 0, "indexOf('alpha') should return 0");
        assertEquals(list.indexOf("BETA"), 1, "indexOf('BETA') should return 1");
        assertEquals(list.indexOf("gamma"), 2, "indexOf('gamma') should return 2");
    }
}