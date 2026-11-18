# Quiz App for Android

An offline-first native Android application built with Java and Android Studio for practising GATE Computer Science (CSE) and Data Science & Artificial Intelligence (DA) questions.

> **Project status:** Functional prototype. The bundled question banks and revision notes are not a substitute for checking every question, diagram, option and answer against the original examination papers and official answer keys.

## Features

- Offline quiz practice using question banks bundled in the APK.
- MCQ, MSQ and NAT question formats.
- CSE and DA branch selection, with subject-wise and mixed practice.
- Randomized question selection and navigation between questions.
- End-of-session review showing attempted answers and correct answers.
- Source metadata such as year, paper/session, question number and source paper where available.
- Offline revision notes.
- Multiple-PDF selection and import into the app's private storage.

## Requirements

- Android Studio
- JDK 17 or a compatible JDK supported by the configured Gradle version.
- Android device or emulator

## Getting started

1. Clone or download this repository.
2. Open the repository root in Android Studio.
3. Allow Gradle sync to finish.
4. Connect an Android device or start an emulator.
5. Run the `app` configuration.

If Android Studio asks for a JDK, use JDK 17, subject to the Gradle and Android Gradle Plugin versions configured in the project.

## Run Android unit tests

The MCQ, MSQ and NAT scoring rules are isolated in the pure-Java
`AnswerEvaluator` class and covered by local JVM unit tests. Run them from
the repository root:

```bash
./gradlew :app:testDebugUnitTest
```

On Windows, use `gradlew.bat :app:testDebugUnitTest`. The GitHub Actions
workflow runs this task alongside the debug APK build and question-bank
validator tests.

## Offline question banks

Question banks are stored in `app/src/main/assets/`. The current files are:

| File | Branch | Records |
| --- | --- | ---: |
| `GATE_CSE_questions_classified.json` | CSE | 520 |
| `GATE_DA_questions_2024_2026.json` | DA | 195 |
| **Total** | | **715** |

Counts reflect the data currently committed and may change as the banks are reviewed and expanded.

### How the app loads question data

`MainActivity.loadQuestions()` scans the top level of the assets directory. It reads JSON arrays other than `theory_notes.json`, and imports only array entries that contain a `question` field. Entries with an `id` are de-duplicated by that ID.

This means you can add another question-bank JSON array at the assets root without changing the Java loader. Keep non-question metadata in a separate file; records without a `question` field are ignored. The existing `questions.json` file is an empty placeholder, not the active source of the bundled questions.

### Validate question-bank structure

Run these commands from the repository root (Python 3.8+; no third-party packages required):

```bash
python tools/validate_question_banks.py
python -m unittest discover -s tools -p "test_*.py" -v
```

The validator checks required question metadata, globally unique IDs, MCQ/MSQ option values and answer references, NAT answer formats, accepted ranges, and non-negative tolerances. It follows the option-value and NAT answer formats currently supported by `MainActivity.parseQuestion()`.

The current committed banks contain **8 structural issues** that need manual review before the bank can be considered clean:

| Question ID | Finding |
|---|---|
| `cse-2022-cs-q05` | Answer value does not match the option values in the record |
| `cse-2023-cs-q02` | MCQ has no answer |
| `cse-2023-cs-q30` | NAT answer key contains two alternative ranges, a format the app does not parse |
| `GATE_DA_2024_1_55` | MSQ has answer values but no option records |
| `GATE_DA_2024_1_59` | NAT answer key text `MTA` is not a numeric value or supported range |
| `GATE_DA_2026_8_12` | Fourth option has empty display text |
| `GATE_DA_2026_8_38` | Fourth option has empty display text |

The validator exits with status 1 while these issues remain. Check the original question paper and official answer key before changing any question or answer; the script detects structural inconsistencies, **not academic correctness**. It does not verify that referenced image files exist.


### Supported question fields

Common fields include:

- `id`: stable unique question identifier (recommended).
- `branch`: `CSE` or `DA`.
- `subject`, `topic`, `type` and `question`.
- `year`, `paperSession`, `questionNumber`, `officialSection` and `marks`.
- `explanation`, `sourceQuestionPaper`, `sourceAnswerKey` and `imagePath`, where available.

For MCQ/MSQ questions, `options` may be an array of objects with `value` and `text` fields. The loader also supports string options paired with an `optionValues` array. `answers` contains the correct option values; for MSQ it must contain every correct option.

Example MCQ:

```json
{
  "id": "cse-2024-session1-q01",
  "branch": "CSE",
  "year": "2024",
  "paperSession": "Session 1",
  "questionNumber": 1,
  "subject": "Operating Systems",
  "topic": "Scheduling",
  "type": "MCQ",
  "question": "Question text...",
  "options": [
    { "value": "A", "text": "Option one" },
    { "value": "B", "text": "Option two" },
    { "value": "C", "text": "Option three" },
    { "value": "D", "text": "Option four" }
  ],
  "answers": ["B"],
  "explanation": "Explanation text..."
}
```

For NAT questions, provide `numericAnswer` and an optional `tolerance`, or an `acceptedRange` object with `min` and `max`. The loader also understands the existing `answerKeyRange` field used in the bundled data.

Example NAT:

```json
{
  "id": "cse-2024-session1-q31",
  "branch": "CSE",
  "type": "NAT",
  "question": "Enter the numerical answer...",
  "numericAnswer": 12.5,
  "tolerance": 0.01
}
```

Verify question wording, mathematical notation, diagrams, option boundaries, marks and answer keys against the original paper before publishing changes. Do not paste raw PDF text directly into Java source files.

### General Aptitude and question order

The bank preserves paper metadata and uses `officialSection`/subject classification where available. In a standard GATE paper, the first 10 questions are General Aptitude, but the app shuffles the selected questions when a practice session starts; it does not currently reproduce a full paper in its original order.

## Revision notes

The **Browse revision notes** screen reads `app/src/main/assets/theory_notes.json`. Each note uses:

- `id`
- `branch` (`CSE` or `DA`)
- `subject`
- `topic`
- `title`
- `content`

The notes currently included are demonstration material. Check their accuracy and source rights before replacing or expanding them.

## PDF import

The **Import PDFs** action opens Android's document picker, supports multiple selections, and copies the selected files into the app-private `files/pdf_inbox/` directory.

This is an intake and storage feature only. It does **not** extract text, perform OCR, or convert PDF content into quiz questions. Imported questions must be checked and added to a question-bank JSON file in the assets directory.

## Current prototype limitations

- Question-bank content and revision notes still require independent academic and answer-key verification.
- Quiz history and bookmarks are not persisted between sessions.
- Responses are retained in memory for the current quiz and shown in the end-of-session review for attempted questions; unanswered questions are omitted from that review.
- Results are calculated when the results screen is displayed; no long-term score history is stored.
- Subject options are derived from the questions currently loaded for the selected branch.
- PDF extraction, OCR and automatic question conversion are not implemented.
- The quiz workflow is offline and does not require a backend or network connection.

## Project structure

```text
app/
└── src/
    └── main/
        ├── AndroidManifest.xml
        ├── assets/
        │   ├── GATE_CSE_questions_classified.json
        │   ├── GATE_DA_questions_2024_2026.json
        │   ├── image_manifest_CS.json
        │   ├── theory_notes.json
        │   └── images_CS/ and images_DA/
        ├── java/
        │   └── com/example/gatequiz/
        │       └── MainActivity.java
        └── res/
```

The repository also includes the Gradle wrapper and project configuration needed to build the Android app.

## Acknowledgements

The question database used as a source for this project is credited to [@sanny1724](https://github.com/sanny1724), from the [GATE--PYQS-LAST-10-YEARS repository](https://github.com/sanny1724/GATE--PYQS-LAST-10-YEARS). See [CREDITS.md](CREDITS.md) for source details and attribution.

## Contributing

Bug reports, suggestions and feature requests are welcome through GitHub Issues. Include steps to reproduce a bug and the device/Android version where relevant.

Before contributing question content, verify it against an authoritative source and ensure you have the right to redistribute the material.

## Disclaimer

This is an independent educational project and is not affiliated with, endorsed by, or officially associated with GATE, the organizing institute, or any examination authority.

GATE is referenced only to describe the exam-preparation use case. Verify all questions and answers independently before relying on them for exam preparation.
