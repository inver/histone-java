package ru.histone.acceptance.tests.functions.string;

import org.junit.runner.RunWith;
import ru.histone.acceptance.support.AcceptanceTest;
import ru.histone.acceptance.support.AcceptanceTestsRunner;

@RunWith(AcceptanceTestsRunner.class)
public class CharCodeAtTest extends AcceptanceTest {
    @Override
    public String getFileName() {
        return "/functions/string.charCodeAt.json";
    }
}