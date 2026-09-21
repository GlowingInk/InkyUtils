package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class LazyRawTest {
    @Test
    public void getTest() {
        ParameterImpl.LazyValue raw = new ParameterImpl.LazyValue("0123\\56789".toCharArray(), 1, 6);
        String value = raw.apply(null);

        assertEquals("123\\5", value);
        assertSame(value, raw.apply(null), "The value should be computed only once");
    }
}
