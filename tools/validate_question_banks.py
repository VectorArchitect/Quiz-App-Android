#!/usr/bin/env python3
"""Validate the structure of bundled GATE question-bank JSON files.

This checks data consistency only. It cannot verify question wording or answer
correctness against official examination papers.
"""

import argparse
import json
import math
import re
import sys
from pathlib import Path

BANK_PATTERN = "GATE_*_questions*.json"
VALID_TYPES = {"MCQ", "MSQ", "NAT"}
_RANGE_PATTERN = re.compile(
    r"^\s*(-?\d+(?:\.\d+)?)\s*(?:to|-)\s*(-?\d+(?:\.\d+)?)\s*$",
    re.IGNORECASE,
)


def _is_number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _parse_numeric_answer(value):
    if _is_number(value):
        return float(value)
    if isinstance(value, str) and value.strip():
        try:
            parsed = float(value.strip())
        except ValueError:
            return None
        return parsed if math.isfinite(parsed) else None
    return None


def _parse_answer_key_range(value):
    """Mirror the numeric/range formats understood by MainActivity.parseQuestion."""
    if _is_number(value):
        number = float(value)
        return number, number
    if not isinstance(value, str) or not value.strip():
        return None

    text = value.strip().replace("−", "-").replace("–", "-").replace("—", "-")
    match = _RANGE_PATTERN.match(text)
    if match:
        return float(match.group(1)), float(match.group(2))

    number = _parse_numeric_answer(text)
    if number is not None:
        return number, number
    return None


def _option_values(question, issue):
    options = question.get("options")
    if not isinstance(options, list):
        issue("options-not-array", "options must be a JSON array")
        return []

    raw_values = question.get("optionValues")
    values = []
    for index, option in enumerate(options):
        if isinstance(option, dict):
            value = option.get("value", str(index))
            label = option.get("text", "")
            if not isinstance(label, str) or not label.strip():
                issue("empty-option-text", "option {} has no display text".format(index + 1))
        elif isinstance(option, str):
            if isinstance(raw_values, list) and index < len(raw_values):
                value = raw_values[index]
            else:
                value = str(index)
            if not option.strip():
                issue("empty-option-text", "option {} has no display text".format(index + 1))
        else:
            issue("invalid-option", "option {} must be an object or string".format(index + 1))
            continue

        if value is None or not str(value).strip():
            issue("empty-option-value", "option {} has no value".format(index + 1))
            continue
        values.append(str(value))

    if len(values) != len(set(values)):
        issue("duplicate-option-values", "option values must be unique")
    return values


def validate_question(question, location):
    """Return a list of (code, message) errors for one question record."""
    errors = []

    def issue(code, message):
        errors.append((code, message))

    if not isinstance(question, dict):
        return [("record-not-object", "question record must be a JSON object")]

    question_id = question.get("id")
    if not isinstance(question_id, (str, int)) or not str(question_id).strip():
        issue("missing-id", "question needs a non-empty id")

    prompt = question.get("question")
    if not isinstance(prompt, str) or not prompt.strip():
        issue("empty-question", "question text must be a non-empty string")

    branch = question.get("branch")
    if branch not in ("CSE", "DA"):
        issue("invalid-branch", "branch must be CSE or DA")

    question_type = str(question.get("type", "")).strip().upper()
    if question_type not in VALID_TYPES:
        issue("invalid-type", "type must be MCQ, MSQ, or NAT")
        return errors

    if question_type in ("MCQ", "MSQ"):
        values = _option_values(question, issue)
        answers = question.get("answers")
        if not isinstance(answers, list) or not answers:
            issue("missing-answers", "{} questions need at least one answer".format(question_type))
            answers = []
        else:
            normalized_answers = [str(answer) for answer in answers]
            if any(not answer.strip() for answer in normalized_answers):
                issue("empty-answer", "answers must not contain empty values")
            if len(normalized_answers) != len(set(normalized_answers)):
                issue("duplicate-answers", "answers must not contain duplicates")
            if question_type == "MCQ" and len(normalized_answers) != 1:
                issue("mcq-answer-count", "MCQ questions must have exactly one answer")
            if not set(normalized_answers).issubset(set(values)):
                issue("answer-not-in-options", "every answer must match an option value")
        if len(values) < 2:
            issue("too-few-options", "{} questions need at least two options".format(question_type))

    if question_type == "NAT":
        tolerance = _parse_numeric_answer(question.get("tolerance", 0.0001))
        if tolerance is None or tolerance < 0:
            issue("invalid-tolerance", "NAT tolerance must be a finite, non-negative number")

        accepted = question.get("acceptedRange")
        answer_range = None
        if accepted is not None:
            if not isinstance(accepted, dict):
                issue("invalid-accepted-range", "acceptedRange must be an object")
            else:
                low = _parse_numeric_answer(accepted.get("min"))
                high = _parse_numeric_answer(accepted.get("max"))
                if low is None or high is None:
                    issue("invalid-accepted-range", "acceptedRange needs finite numeric min and max values")
                else:
                    answer_range = (low, high)
        if answer_range is None and accepted is None:
            answer_range = _parse_answer_key_range(question.get("answerKeyRange"))

        numeric_answer = _parse_numeric_answer(question.get("numericAnswer"))
        if answer_range is None and numeric_answer is None:
            issue("missing-nat-answer", "NAT needs numericAnswer, acceptedRange, or a parseable answerKeyRange")
        if answer_range is not None and answer_range[0] > answer_range[1]:
            issue("reversed-accepted-range", "accepted answer range minimum exceeds maximum")

    return errors


def validate_files(paths):
    """Validate bank files and return (summary, errors)."""
    summary = []
    errors = []
    seen_ids = {}

    for path in paths:
        path = Path(path)
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, UnicodeError, json.JSONDecodeError) as exc:
            errors.append((str(path), "invalid-json", str(exc)))
            continue

        if not isinstance(data, list):
            errors.append((str(path), "bank-not-array", "question bank must be a JSON array"))
            continue

        summary.append((str(path), len(data)))
        for index, question in enumerate(data):
            location = "{}[{}]".format(path, index)
            if isinstance(question, dict):
                question_id = question.get("id")
                if isinstance(question_id, (str, int)) and str(question_id).strip():
                    key = str(question_id)
                    if key in seen_ids:
                        errors.append((location, "duplicate-id", "id {!r} also appears at {}".format(key, seen_ids[key])))
                    else:
                        seen_ids[key] = location
            for code, message in validate_question(question, location):
                errors.append((location, code, message))

    return summary, errors


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "directory",
        nargs="?",
        default="app/src/main/assets",
        help="directory containing GATE_*_questions*.json files (default: %(default)s)",
    )
    args = parser.parse_args(argv)

    directory = Path(args.directory)
    paths = sorted(directory.glob(BANK_PATTERN)) if directory.is_dir() else []
    if not paths:
        print("No question-bank files matching {} found in {}".format(BANK_PATTERN, directory), file=sys.stderr)
        return 2

    summary, errors = validate_files(paths)
    total = sum(count for _, count in summary)
    print("Validated {} question(s) across {} bank file(s).".format(total, len(summary)))
    for path, count in summary:
        print("  {}: {} question(s)".format(path, count))

    if errors:
        print("\n{} validation issue(s):".format(len(errors)), file=sys.stderr)
        for location, code, message in errors:
            print("  {} [{}] {}".format(location, code, message), file=sys.stderr)
        return 1

    print("Question-bank structure is valid.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
