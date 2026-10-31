# Quiz App for Android

An offline-first native Android application built with Java and Android Studio for practising GATE CSE and Data Science & Artificial Intelligence (DA) questions.

> **Project Status:** This repository currently contains a functional prototype. The bundled question bank and revision notes are demonstration content and should not be considered verified GATE Previous Year Questions (PYQs) unless independently checked against the original papers and answer keys.

## Features

- Offline quiz practice using bundled question data.
- Support for MCQ, MSQ and NAT question formats.
- CSE and DA branch classification.
- Subject- and topic-based question organisation.
- Quiz review and explanations at the end of a quiz, where provided.
- Question source information, including year and source reference where available.
- Offline revision notes.
- PDF selection and import into the app's private storage.

## Requirements

- Android Studio
- JDK 17 or a compatible JDK supported by the project's Gradle configuration.
- Android device or emulator.

## Getting Started

1. Clone or download this repository.
2. Open the project root folder in Android Studio.
3. Allow Gradle sync to complete.
4. Connect an Android device or start an emulator.
5. Run the `app` configuration.

If Android Studio requests a JDK, use JDK 17, subject to the Gradle and Android Gradle Plugin versions configured in the project.

## Offline Question Bank

Question data is bundled in the app's assets directory:

```text
app/src/main/assets/questions.json
```

Quiz-taking does not require a network connection or backend service.

### Question Data Format

The documented question fields include:

- `branch`: `CSE` or `DA`
- `subject` and `topic`
- `type`: `MCQ`, `MSQ` or `NAT`
- `question`
- For MCQ/MSQ: `options`, matching `optionValues`, and `answers`
- For NAT: `numericAnswer` and optional `tolerance`
- `explanation`, `year` and `source`

Example MCQ:

```json
{
  "id": "cse-2024-session1-q01",
  "branch": "CSE",
  "subject": "Operating Systems",
  "topic": "Scheduling",
  "type": "MCQ",
  "question": "Question text...",
  "options": [
    "Option one",
    "Option two",
    "Option three",
    "Option four"
  ],
  "optionValues": ["A", "B", "C", "D"],
  "answers": ["B"],
  "explanation": "Verified explanation...",
  "year": "2024",
  "source": "GATE CSE 2024, session 1, Q1"
}
```

For MSQ, `answers` should contain every correct option value. For NAT, use `numericAnswer` and, where appropriate, `tolerance` instead of MCQ/MSQ option and answer fields.

Do not paste raw PDF text directly into Java source files. Convert questions into the supported data structure and verify question text, diagrams, options, answer keys and marks against the original paper before adding them.

## Revision Notes

The home screen provides a **Browse revision notes** option. Notes are loaded from:

```text
app/src/main/assets/theory_notes.json
```

Each note object uses these fields:

- `id`
- `branch` (`CSE` or `DA`)
- `subject`
- `topic`
- `title`
- `content`

The included notes are demonstration content. Check their accuracy and source rights before replacing or expanding them.

## PDF Import

The home screen includes an **Import PDFs** option. It opens Android's file picker, allows multiple PDF files to be selected, and copies the selected files into the app-private `files/pdf_inbox/` directory.

This is an intake and storage feature only. It does **not** automatically extract questions from PDFs or convert them into the question bank.

Questions must still be verified and added to `questions.json`. Refer to `IMPORT_WORKFLOW.md` for the project's import workflow, if included in the repository.

## General Aptitude

For a complete GATE paper in its original question order, the first **10 questions** are classified as General Aptitude, after confirming that the source PDF contains the complete paper and that its original order has been preserved.

## Current Prototype Limitations

- The bundled question bank and revision notes are demonstration content, not automatically verified PYQs.
- Quiz history and bookmarks are not persisted.
- The current prototype scores a question when the user advances. The end-of-quiz review does not yet retain each user's submitted response.
- Subject options are derived from the questions currently present in the bundled JSON data.
- PDF import only stages files in app-private storage. PDF extraction, OCR and conversion into individual questions are not implemented by this feature.
- Answer-key validation and verification of diagrams, options and marks must be performed before publishing question content.
- The documented offline quiz workflow does not require an internet connection or online service.

## Project Structure

Key files include:

```text
app/
└── src/
    └── main/
        ├── AndroidManifest.xml
        ├── assets/
        │   ├── questions.json
        │   └── theory_notes.json
        ├── java/
        │   └── com/example/gatequiz/
        │       └── MainActivity.java
        └── res/
```

The repository also contains Gradle wrapper and configuration files required to build the project.

## Acknowledgements

The question database used as a source for this project is credited to [@sanny1724](https://github.com/sanny1724), from the [GATE--PYQS-LAST-10-YEARS repository](https://github.com/sanny1724/GATE--PYQS-LAST-10-YEARS). See [CREDITS.md](CREDITS.md) for source details and attribution.

## Contributing

Suggestions, bug reports and feature requests are welcome.

Please open a GitHub Issue with steps to reproduce a bug or a clear description of the proposed improvement.

Before contributing question content, verify it against an authoritative source and ensure you have the right to redistribute the material.

## Disclaimer

This is an independent educational project and is not affiliated with, endorsed by, or officially associated with GATE, the organizing institute, or any examination authority.

GATE is referenced only to describe the exam-preparation use case. Verify all questions and answers independently before relying on them for exam preparation.
