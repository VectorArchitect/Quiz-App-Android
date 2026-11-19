package com.example.gatequiz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AnswerKeyRangeParserTest {
    private static final double UNSET = Double.NaN;

    private AnswerKeyRangeParser.ParsedAnswer parse(
            double numericAnswer, double min, double max, Object answerKey) {
        return AnswerKeyRangeParser.parse(numericAnswer, min, max, answerKey);
    }

    @Test
    public void completeAcceptedRangeTakesPrecedenceOverLegacyFallback() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(UNSET, 2.0, 4.0, "9");
        assertEquals(2.0, result.minAccepted, 0.0);
        assertEquals(4.0, result.maxAccepted, 0.0);
        assertTrue(Double.isNaN(result.numericAnswer));
    }

    @Test
    public void partialAcceptedRangeFallsBackToLegacyRange() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(UNSET, 2.0, UNSET, "3 to 5");
        assertEquals(3.0, result.minAccepted, 0.0);
        assertEquals(5.0, result.maxAccepted, 0.0);
    }

    @Test
    public void unusablePartialRangeIsClearedWhenNoFallbackExists() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(7.0, 2.0, UNSET, null);
        assertTrue(Double.isNaN(result.minAccepted));
        assertTrue(Double.isNaN(result.maxAccepted));
        assertEquals(7.0, result.numericAnswer, 0.0);
    }

    @Test
    public void parsesUnicodeDashAndNegativeRangeEndpoints() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(
                UNSET, UNSET, UNSET, "−2.1 – −1.9");
        assertEquals(-2.1, result.minAccepted, 0.0);
        assertEquals(-1.9, result.maxAccepted, 0.0);
    }

    @Test
    public void parsesScalarNumericStrings() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(UNSET, UNSET, UNSET, "12.5");
        assertEquals(12.5, result.numericAnswer, 0.0);
    }

    @Test
    public void parsesNumericAnswerKeyValues() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(
                UNSET, UNSET, UNSET, Integer.valueOf(8));
        assertEquals(8.0, result.numericAnswer, 0.0);
    }

    @Test
    public void unsupportedAlternativeRangesRemainUnset() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(
                UNSET, 1.0, UNSET, "2 to 2 OR 4 to 4");
        assertTrue(Double.isNaN(result.minAccepted));
        assertTrue(Double.isNaN(result.maxAccepted));
        assertTrue(Double.isNaN(result.numericAnswer));
    }

    @Test
    public void blankFallbackPreservesExistingNumericAnswer() {
        AnswerKeyRangeParser.ParsedAnswer result = parse(4.0, UNSET, UNSET, "  ");
        assertEquals(4.0, result.numericAnswer, 0.0);
        assertTrue(Double.isNaN(result.minAccepted));
        assertTrue(Double.isNaN(result.maxAccepted));
    }
}
