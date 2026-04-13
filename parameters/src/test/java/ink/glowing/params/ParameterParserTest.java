package ink.glowing.params;

import org.testng.annotations.Test;

import java.util.List;

public class ParameterParserTest {
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
            String result = ParameterParser.parse(ex).toString();
            IO.println(result);
            IO.println(ParameterParser.parse(result));
            IO.println();
        }
    }
}
