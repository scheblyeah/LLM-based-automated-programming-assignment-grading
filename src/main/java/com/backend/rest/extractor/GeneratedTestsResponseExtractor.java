package com.backend.rest.extractor;

import com.backend.rest.dto.GeneratedTestResults;
import com.backend.rest.dto.GradingResponse;

public interface GeneratedTestsResponseExtractor {
    @dev.langchain4j.service.SystemMessage("""
                For the given exercise, generate JUNIT java tests that should test the functionality of the code.
                The tests should be as short and as few as possible but still test the core functionality.
                The class data that you generate will be a List of SingleUnitTest Objects and a testClassSourceCode String.
                The full source code of your java JUNIT5 tests as string is saved in the testClassSourceCode String and must have all double quotes escaped.
                For each test, also generate a SingleUnitTest Object and save them in the generatedTest List, where the String testName is the testName and the String testSourceCode is the sourceCode with all doublequotes escaped.
                the boolean passedTest should always be false.
                """)
    GeneratedTestResults extractGeneratedTestsResponseFrom(String text);
}
