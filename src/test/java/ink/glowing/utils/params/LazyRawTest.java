package ink.glowing.utils.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LazyRawTest {
    @Test
    public void getTest() {
        ParameterImpl.LazyValue raw = new ParameterImpl.LazyValue("0123\\56789".toCharArray(), 1, 6);
        assertEquals(
                "1235",
                raw.apply(null)
        );
    }
}
