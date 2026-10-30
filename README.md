# GATE CSE Quiz — Android Studio starter

An offline-first native Android starter project in Java. The included questions are demo questions only, not verified GATE PYQs.

## Open in Android Studio
1. Extract `GATE_CSE_Quiz_Android.zip`.
2. Open Android Studio → **Open** → select the extracted `GATE_CSE_Quiz_Android` folder.
3. Allow Gradle sync. Use JDK 17 if Android Studio asks.
4. Connect an Android phone with USB debugging enabled, or create an emulator.
5. Press **Run**.

## Offline behavior
The question bank is bundled at `app/src/main/assets/questions.json`. Quiz-taking needs no network permission or backend.

## Importing your own papers
Do not paste raw PDF text directly into Java. Convert each verified question into the JSON structure in `questions.json`:
- `branch`: `CSE` or `DA`
- `subject`, `topic`
- `type`: `MCQ`, `MSQ`, or `NAT`
- `question`
- For MCQ/MSQ: `options`, matching `optionValues`, and `answers`
- For NAT: `numericAnswer` and optional `tolerance`
- `explanation`, `year`, `source`

Example MCQ:
```json
{
  "id": "cse-2024-session1-q01",
  "branch": "CSE",
  "subject": "Operating Systems",
  "topic": "Scheduling",
  "type": "MCQ",
  "question": "Question text...",
  "options": ["Option one", "Option two", "Option three", "Option four"],
  "optionValues": ["A", "B", "C", "D"],
  "answers": ["B"],
  "explanation": "Verified explanation...",
  "year": "2024",
  "source": "GATE CSE 2024, session 1, Q1"
}
```

For MSQ, `answers` contains every correct option value. For NAT, use `numericAnswer` instead of `options`/`answers`.

## Important starter limitations
- This is a functional prototype, not a release-ready Play Store app.
- Quiz history and bookmarks are not yet persisted.
- Current prototype scores a question when you advance; review is shown at the end, but it does not yet store each user's submitted response in the review.
- Subject options are derived from questions currently present in the bundled JSON.
- PDF extraction/OCR and answer-key validation are a separate import pipeline. Verify extracted questions, diagrams, options, keys and marks before publishing.
- This starter does not include any internet permission or online service.

## Project layout
- `app/src/main/java/com/example/gatequiz/MainActivity.java`
- `app/src/main/assets/questions.json`
- `app/src/main/AndroidManifest.xml`


## Offline theory / revision notes
The home screen has a **Browse revision notes** button. Notes are loaded from `app/src/main/assets/theory_notes.json`.

Each note object has `id`, `branch` (`CSE` or `DA`), `subject`, `topic`, `title`, and `content`. The included notes are demonstrations only. Replace or expand them with your syllabus-aligned notes after checking accuracy and source rights. Notes are bundled into the app and available offline.

Example:
```json
{
  "id": "cse-os-deadlocks-01",
  "branch": "CSE",
  "subject": "Operating Systems",
  "topic": "Deadlocks",
  "title": "Deadlocks — quick revision",
  "content": "Definition, four necessary conditions, prevention, avoidance, detection and recovery..."
}
```


## PDF import button
The home screen includes **Import PDFs**. It opens Android's file picker, supports selecting multiple PDF files, and copies them to the app-private `files/pdf_inbox/` directory. No network connection is required.

This is an intake/staging feature only: it does not yet parse the PDF into individual questions. The verified question data still needs to be converted into `questions.json`. See `IMPORT_WORKFLOW.md`.

## General Aptitude
General Aptitude is included as an additional CSE quiz subject. For this project, classify the first 15 questions of each complete GATE CSE paper as General Aptitude only after confirming the PDF is a complete paper in original order.


## General Aptitude classification update
For this project, the first **10 questions** of each complete GATE paper are classified as General Aptitude, after confirming the paper is complete and in original question order.
