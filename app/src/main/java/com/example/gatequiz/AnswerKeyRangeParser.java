package com.example.gatequiz;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the numeric answer formats supported by the bundled question banks. */
final class AnswerKeyRangeParser {
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:to|-)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private AnswerKeyRangeParser() {
    }

    static ParsedAnswer parse(
            double numericAnswer,
            double minAccepted,
            double maxAccepted,
            Object answerKey) {
        if (isFinite(minAccepted) && isFinite(maxAccepted)) {
            return new ParsedAnswer(numericAnswer, minAccepted, maxAccepted);
        }

        // A partial or non-finite acceptedRange is not usable. Clear it before
        // trying the legacy answerKeyRange field as a fallback.
        minAccepted = Double.NaN;
        maxAccepted = Double.NaN;

        if (answerKey instanceof Number) {
            numericAnswer = ((Number) answerKey).doubleValue();
        } else if (answerKey instanceof String) {
            String answerText = ((String) answerKey).trim()
                    .replace("−", "-").replace("–", "-").replace("—", "-");
            Matcher matcher = RANGE_PATTERN.matcher(answerText);
            if (matcher.matches()) {
                minAccepted = Double.parseDouble(matcher.group(1));
                maxAccepted = Double.parseDouble(matcher.group(2));
            } else if (!answerText.isEmpty()) {
                try {
                    numericAnswer = Double.parseDouble(answerText);
                } catch (NumberFormatException ignored) {
                    // Leave the numeric answer unset; the evaluator will reject it.
                }
            }
        }

        return new ParsedAnswer(numericAnswer, minAccepted, maxAccepted);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    static final class ParsedAnswer {
        final double numericAnswer;
        final double minAccepted;
        final double maxAccepted;

        ParsedAnswer(double numericAnswer, double minAccepted, double maxAccepted) {
            this.numericAnswer = numericAnswer;
            this.minAccepted = minAccepted;
            this.maxAccepted = maxAccepted;
        }
    }
}
