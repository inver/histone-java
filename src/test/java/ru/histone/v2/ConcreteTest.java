package ru.histone.v2;

import org.junit.Test;
import ru.histone.HistoneException;
import ru.histone.v2.test.dto.HistoneTestCase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by inv3r on 19/01/16.
 */
public class ConcreteTest extends BaseTest {
    @Test
    public void concreteTest() throws HistoneException {
        HistoneTestCase.Case testCase = new HistoneTestCase.Case();
        testCase.setExpectedResult("a # b");
//        testCase.setContext(getMap());
//        testCase.setExpectedAST("[31,\"a \",[25,\"x\",[30,\" b \"]],\" c\"]");
        doTest("<div>{{0.42->toJSON}} = 0.42</div>", testCase);
    }

    private Map<String, Object> getMap() {
        Map<String, Object> res = new HashMap<>();

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("foo", 1L);
        values.put("bar", 2L);
        values.put("x", 3L);

        res.put("items", values);
        return res;
    }
}
