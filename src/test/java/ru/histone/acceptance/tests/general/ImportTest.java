package ru.histone.acceptance.tests.general;

import org.junit.runner.RunWith;
import ru.histone.acceptance.support.AcceptanceTest;
import ru.histone.acceptance.support.AcceptanceTestsRunner;

@RunWith(AcceptanceTestsRunner.class)

public class ImportTest extends AcceptanceTest {

    @Override
    public String getFileName() {
        return "/general/import.json";
    }
}