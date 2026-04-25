package ink.glowing.params;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ParameterParserTest {
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
                }
        };
    }

    @Test(dataProvider = "parseData")
    public void parseTest(String input, String expected) {
        String result = Parameter.asParameterValue(ParametersParser.parseMap(input), true);
        assertEquals(
                result,
                expected
        );
        assertEquals(
                Parameter.asParameterValue(ParametersParser.parseMap(result), true),
                expected,
                "Double-parsing input lead to another result"
        );
    }

    @Test
    public void test() {
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
            String result = Parameter.asParameterValue(ParametersParser.parseMap(ex), true);
            IO.println(result);
            IO.println(Parameter.asParameterValue(ParametersParser.parseMap(result), true));
            IO.println();
        }
    }
}
