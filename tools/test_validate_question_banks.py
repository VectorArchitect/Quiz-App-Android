import json
import tempfile
import unittest
from pathlib import Path

from validate_question_banks import validate_files, validate_question


class ValidateQuestionTests(unittest.TestCase):
    def valid_mcq(self):
        return {
            "id": "cse-test-1",
            "branch": "CSE",
            "type": "MCQ",
            "question": "Choose the correct option.",
            "options": [
                {"value": "A", "text": "First"},
                {"value": "B", "text": "Second"},
            ],
            "answers": ["B"],
        }

    def test_valid_mcq(self):
        self.assertEqual(validate_question(self.valid_mcq(), "sample"), [])

    def test_valid_msq_accepts_multiple_answers(self):
        question = self.valid_mcq()
        question.update(type="MSQ", answers=["A", "B"])
        self.assertEqual(validate_question(question, "sample"), [])

    def test_string_options_use_option_values(self):
        question = self.valid_mcq()
        question.update(options=["First", "Second"], optionValues=["X", "Y"], answers=["Y"])
        self.assertEqual(validate_question(question, "sample"), [])

    def test_answer_must_match_option_value(self):
        question = self.valid_mcq()
        question["answers"] = ["C"]
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("answer-not-in-options", codes)

    def test_mcq_requires_exactly_one_answer(self):
        question = self.valid_mcq()
        question["answers"] = ["A", "B"]
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("mcq-answer-count", codes)

    def test_duplicate_option_values_are_rejected(self):
        question = self.valid_mcq()
        question["options"][1]["value"] = "A"
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("duplicate-option-values", codes)

    def test_nat_accepts_explicit_range(self):
        question = {
            "id": "da-nat-1", "branch": "DA", "type": "NAT",
            "question": "Enter a number.", "acceptedRange": {"min": 1.5, "max": 2.5},
        }
        self.assertEqual(validate_question(question, "sample"), [])

    def test_nat_accepts_answer_key_range(self):
        question = {
            "id": "da-nat-2", "branch": "DA", "type": "NAT",
            "question": "Enter a number.", "answerKeyRange": "1.5 to 2.5",
        }
        self.assertEqual(validate_question(question, "sample"), [])

    def test_nat_rejects_negative_tolerance(self):
        question = {
            "id": "da-nat-3", "branch": "DA", "type": "NAT",
            "question": "Enter a number.", "numericAnswer": 2, "tolerance": -0.1,
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("invalid-tolerance", codes)

    def test_nat_rejects_multiple_alternative_ranges(self):
        question = {
            "id": "da-nat-4", "branch": "DA", "type": "NAT",
            "question": "Enter a number.", "answerKeyRange": "2 to 2 OR 4 to 4",
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("missing-nat-answer", codes)

    def test_nat_rejects_unrecognized_answer_key_text(self):
        question = {
            "id": "da-nat-5", "branch": "DA", "type": "NAT",
            "question": "Enter a number.", "answerKeyRange": "MTA",
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("missing-nat-answer", codes)

    def test_empty_option_text_is_reported(self):
        question = self.valid_mcq()
        question["options"][1]["text"] = ""
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("empty-option-text", codes)

    def test_duplicate_ids_across_files_are_reported(self):
        with tempfile.TemporaryDirectory() as temp:
            first = Path(temp) / "first.json"
            second = Path(temp) / "second.json"
            record = self.valid_mcq()
            first.write_text(json.dumps([record]), encoding="utf-8")
            second.write_text(json.dumps([record]), encoding="utf-8")
            _, errors = validate_files([first, second])
        self.assertTrue(any(code == "duplicate-id" for _, code, _ in errors))

    def test_non_array_bank_is_reported(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "bank.json"
            path.write_text(json.dumps({"questions": []}), encoding="utf-8")
            _, errors = validate_files([path])
        self.assertTrue(any(code == "bank-not-array" for _, code, _ in errors))


    def test_nat_partial_accepted_range_uses_legacy_fallback(self):
        question = {
            "id": "da-nat-6", "branch": "DA", "type": "NAT",
            "question": "Enter a number.",
            "acceptedRange": {"min": 1.0},
            "answerKeyRange": "2 to 3",
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("invalid-accepted-range", codes)
        self.assertNotIn("missing-nat-answer", codes)

    def test_nat_partial_accepted_range_keeps_numeric_answer_usable(self):
        question = {
            "id": "da-nat-7", "branch": "DA", "type": "NAT",
            "question": "Enter a number.",
            "acceptedRange": {"min": 1.0},
            "numericAnswer": 2.5,
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("invalid-accepted-range", codes)
        self.assertNotIn("missing-nat-answer", codes)

    def test_nat_non_object_accepted_range_does_not_hide_fallback(self):
        question = {
            "id": "da-nat-8", "branch": "DA", "type": "NAT",
            "question": "Enter a number.",
            "acceptedRange": "invalid",
            "answerKeyRange": "4",
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("invalid-accepted-range", codes)
        self.assertNotIn("missing-nat-answer", codes)

    def test_nat_partial_accepted_range_without_fallback_is_missing(self):
        question = {
            "id": "da-nat-9", "branch": "DA", "type": "NAT",
            "question": "Enter a number.",
            "acceptedRange": {"min": 1.0},
            "answerKeyRange": "MTA",
        }
        codes = {code for code, _ in validate_question(question, "sample")}
        self.assertIn("invalid-accepted-range", codes)
        self.assertIn("missing-nat-answer", codes)


if __name__ == "__main__":
    unittest.main()
