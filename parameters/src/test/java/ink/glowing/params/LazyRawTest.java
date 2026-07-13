package ink.glowing.params;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LazyRawTest {
    @Test
    public void getTest() {
        ParameterImpl.LazyValue raw = new ParameterImpl.LazyValue("0123\\56789".toCharArray(), 1, 6);
        assertEquals(
                raw.apply(null),
                "1235"
        );
    }
}
