package ink.glowing.utils.params;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static ink.glowing.utils.params.Parameter.Mapped.parse;
import static org.testng.Assert.assertEquals;

public class ParameterTest {
    @DataProvider
    public Object[][] parseData() {
        return new Object[][]{
                {
                        "simple:value",
                        "simple:value"
                }, {
                        "first:value second:value",
                        "first:value second:value"
                }, {
                        "escaping:\\'\\ space!",
                        "escaping:'\\' space!'"
                }, {
                        "list:['of' 'values']",
                        "list:[of values]"
                }
        };
    }

    @Test(dataProvider = "parseData")
    public void parseTest(String input, String expected) {
        String result = parse(input).asParameterValue(true);
        assertEquals(
                result,
                expected
        );
        assertEquals(
                parse(result).asParameterValue(true),
                expected,
                "Double-parsing input lead to another result"
        );
    }

    @Test
    public void manualTesting() {
        String[] examples = {
                "simple:value",
                "simple:'value'",
                "first:value second:value",
                "escaping:\\'value\\'",
                "spaces:'value with spaces'",
                "spaces:value\\ with\\ spaces",
                "list:[list of values]",
                "list:['list' 'of' 'values']",
                "list:['singleton list']",
                "map:{subkey1:value subkey2:value}",
                "map:{subkey1:['list' 'of' 'values'] subkey2:value}",
                "list:[list with {key:value}]",
                "deep:{a:[1 {b:[2 3]} 4] c:d}"
        };

        for (String ex : examples) {
            IO.println(ex);
            IO.println("========================================");
            var params = parse(ex);
            String result = params.asParameterValue(true);
            IO.println(params.value());
            IO.println(result);
            IO.println(parse(result).asParameterValue(true));
            IO.println();
        }
    }
}
