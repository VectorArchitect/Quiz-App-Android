package com.example.gatequiz;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class AnswerEvaluatorTest {
    private static final double UNSET = Double.NaN;

    private boolean evaluate(String type, List<String> selected, String[] answers) {
        return AnswerEvaluator.isCorrect(
                type, selected, answers, UNSET, UNSET, UNSET, 0.0001);
    }

    @Test
    public void mcqAcceptsTheSingleCorrectOption() {
        assertTrue(evaluate("MCQ", Arrays.asList("B"), new String[]{"B"}));
    }

    @Test
    public void mcqRejectsAnIncorrectOption() {
        assertFalse(evaluate("MCQ", Arrays.asList("A"), new String[]{"B"}));
    }

    @Test
    public void mcqRejectsMultipleSelections() {
        assertFalse(evaluate("MCQ", Arrays.asList("A", "B"), new String[]{"A"}));
    }

    @Test
    public void msqAcceptsTheExactSetRegardlessOfSelectionOrder() {
        assertTrue(evaluate("MSQ", Arrays.asList("C", "A"), new String[]{"A", "C"}));
    }

    @Test
    public void msqRejectsMissingCorrectOptions() {
        assertFalse(evaluate("MSQ", Arrays.asList("A"), new String[]{"A", "C"}));
    }

    @Test
    public void msqRejectsAdditionalIncorrectOptions() {
        assertFalse(evaluate("MSQ", Arrays.asList("A", "B", "C"), new String[]{"A", "C"}));
    }

    @Test
    public void duplicateAnswerKeysAreRejectedRatherThanMakingScoringAmbiguous() {
        assertFalse(evaluate("MSQ", Arrays.asList("A"), new String[]{"A", "A"}));
    }

    @Test
    public void duplicateSelectionsAreRejected() {
        assertFalse(evaluate("MSQ", Arrays.asList("A", "A"), new String[]{"A"}));
    }

    @Test
    public void missingSelectionsOrAnswerKeysAreIncorrect() {
        assertFalse(evaluate("MCQ", Collections.<String>emptyList(), new String[]{"A"}));
        assertFalse(evaluate("MCQ", Arrays.asList("A"), null));
    }

    @Test
    public void natAcceptsAnExactNumericAnswer() {
        assertTrue(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("12.5"), null, 12.5, UNSET, UNSET, 0.0001));
    }

    @Test
    public void natAcceptsValuesWithinConfiguredTolerance() {
        assertTrue(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("12.50005"), null, 12.5, UNSET, UNSET, 0.0001));
    }

    @Test
    public void natAcceptsRangeEndpointsWithinTolerance() {
        assertTrue(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("1.99995"), null, UNSET, 2.0, 4.0, 0.0001));
        assertTrue(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("4.00005"), null, UNSET, 2.0, 4.0, 0.0001));
    }

    @Test
    public void natRejectsValuesOutsideTolerance() {
        assertFalse(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("4.0002"), null, UNSET, 2.0, 4.0, 0.0001));
    }

    @Test
    public void natRejectsInvalidAndNonFiniteInput() {
        for (String value : Arrays.asList("not-a-number", "NaN", "Infinity", "-Infinity")) {
            assertFalse(AnswerEvaluator.isCorrect(
                    "NAT", Arrays.asList(value), null, 12.5, UNSET, UNSET, 0.0001));
        }
    }

    @Test
    public void natRequiresExactlyOneValueAndNonNegativeFiniteTolerance() {
        assertFalse(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("12.5", "12.5"), null, 12.5, UNSET, UNSET, 0.0001));
        assertFalse(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("12.5"), null, 12.5, UNSET, UNSET, -0.01));
        assertFalse(AnswerEvaluator.isCorrect(
                "NAT", Arrays.asList("12.5"), null, 12.5, UNSET, UNSET, Double.NaN));
    }

    @Test
    public void unsupportedQuestionTypesAreNeverScoredAsCorrect() {
        assertFalse(evaluate("UNKNOWN", Arrays.asList("A"), new String[]{"A"}));
    }
}
