package com.example.seleniumtestng.listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Automatically attaches RetryAnalyzer to all @Test methods across the suite.
 * Eliminates the need to manually declare (retryAnalyzer = RetryAnalyzer.class) on every test.
 */
public class AnnotationTransformer implements IAnnotationTransformer {

    @Override
    public void transform(
            ITestAnnotation annotation,
            Class testClass,
            Constructor testConstructor,
            Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}

