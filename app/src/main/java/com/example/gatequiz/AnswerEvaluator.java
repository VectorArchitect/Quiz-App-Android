package com.example.gatequiz;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure Java answer evaluation so the quiz scoring rules can be unit-tested
 * without constructing an Android Activity.
 */
final class AnswerEvaluator {
    private AnswerEvaluator() {
    }

    static boolean isCorrect(
            String questionType,
            List<String> selectedValues,
            String[] answers,
            double numericAnswer,
            double minAccepted,
            double maxAccepted,
            double tolerance) {
        if ("NAT".equalsIgnoreCase(questionType)) {
            return isNumericAnswerCorrect(
                    selectedValues, numericAnswer, minAccepted, maxAccepted, tolerance);
        }

        boolean isMcq = "MCQ".equalsIgnoreCase(questionType);
        boolean isMsq = "MSQ".equalsIgnoreCase(questionType);
        if (!isMcq && !isMsq) return false;
        if (selectedValues == null || selectedValues.isEmpty()
                || answers == null || answers.length == 0) {
            return false;
        }

        Set<String> expected = new HashSet<>();
        for (String answer : answers) {
            if (answer == null || answer.trim().isEmpty() || !expected.add(answer)) {
                return false;
            }
        }

        Set<String> selected = new HashSet<>();
        for (String value : selectedValues) {
            if (value == null || value.trim().isEmpty() || !selected.add(value)) {
                return false;
            }
        }

        if (isMcq && expected.size() != 1) return false;
        return expected.equals(selected);
    }

    private static boolean isNumericAnswerCorrect(
            List<String> selectedValues,
            double numericAnswer,
            double minAccepted,
            double maxAccepted,
            double tolerance) {
        if (selectedValues == null || selectedValues.size() != 1
                || selectedValues.get(0) == null
                || Double.isNaN(tolerance) || Double.isInfinite(tolerance)
                || tolerance < 0) {
            return false;
        }

        final double entered;
        try {
            entered = Double.parseDouble(selectedValues.get(0).trim());
        } catch (NumberFormatException exception) {
            return false;
        }
        if (Double.isNaN(entered) || Double.isInfinite(entered)) return false;

        boolean hasRange = !Double.isNaN(minAccepted) && !Double.isInfinite(minAccepted)
                && !Double.isNaN(maxAccepted) && !Double.isInfinite(maxAccepted);
        if (hasRange) {
            double min = Math.min(minAccepted, maxAccepted);
            double max = Math.max(minAccepted, maxAccepted);
            return entered >= min - tolerance && entered <= max + tolerance;
        }

        if (Double.isNaN(numericAnswer) || Double.isInfinite(numericAnswer)) return false;
        return Math.abs(entered - numericAnswer) <= tolerance;
    }
}
