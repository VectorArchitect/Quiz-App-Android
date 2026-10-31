package com.example.gatequiz;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private final List<Question> allQuestions = new ArrayList<>();
    private final List<Question> quiz = new ArrayList<>();
    private int current = 0, score = 0;
    private boolean submitted = false;
    private String selectedBranch = "CSE";
    private String selectedSubject = "All subjects";
    private String selectedMode = "Subject-wise";
    private LinearLayout root, content;
    private TextView status;
    private final List<String> selectedValues = new ArrayList<>();
    private final List<List<String>> userAnswers = new ArrayList<>();
    private EditText natInput;

    static class Question {
        String id, branch, subject, topic, type, prompt, explanation, year, source;
        String paperSession = "", officialSection = "", imagePath = "";
        int questionNumber = -1, marks = 0;
        String[] options, values, answers;
        double numericAnswer = Double.NaN;
        double minAccepted = Double.NaN, maxAccepted = Double.NaN;
        double tolerance = 0.0001;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        loadQuestions();
        showHome();
    }

    private void loadQuestions() {
        try {
            String[] assets = getAssets().list("");
            java.util.Set<String> seenIds = new java.util.HashSet<>();

            for (String assetName : assets) {
                if (!assetName.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) continue;
                if ("theory_notes.json".equalsIgnoreCase(assetName)) continue;

                // Read each top-level JSON array as a question bank.
                try (InputStream in = getAssets().open(assetName);
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);

                    JSONArray arr = new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null || !o.has("question")) continue;

                        Question q = parseQuestion(o, assetName);
                        if (q == null) continue;

                        // Avoid exact duplicate records if the same bank was copied under two filenames.
                        String dedupeKey = q.id == null || q.id.trim().isEmpty()
                                ? assetName + "#" + i : q.id;
                        if (seenIds.add(dedupeKey)) allQuestions.add(q);
                    }
                } catch (Exception fileError) {
                    // Ignore unrelated JSON files or files that are not question-array banks.
                }
            }

            if (allQuestions.isEmpty()) {
                Toast.makeText(this,
                        "No questions loaded. Put question-bank JSON arrays in app/src/main/assets.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not load question bank: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private Question parseQuestion(JSONObject o, String assetName) {
        try {
            Question q = new Question();
            q.id = o.optString("id", assetName + "-" + System.identityHashCode(o));
            q.branch = o.optString("branch", "CSE");
            q.subject = o.optString("subject", "Uncategorized");
            q.topic = o.optString("topic", "");
            q.type = o.optString("type", "MCQ");
            q.prompt = o.optString("question", "");
            q.explanation = o.optString("explanation", "No explanation added yet.");
            q.year = o.optString("year", "");
            q.paperSession = o.optString("paperSession", "");
            q.questionNumber = o.optInt("questionNumber", -1);
            q.officialSection = o.optString("officialSection", "");
            q.marks = o.optInt("marks", 0);
            q.imagePath = o.optString("imagePath",
                    o.optString("image", o.optString("questionImage", "")));
            q.source = o.optString("source",
                    o.optString("sourceQuestionPaper", assetName));

            JSONArray opts = o.optJSONArray("options");
            JSONArray vals = o.optJSONArray("optionValues");
            if (opts != null) {
                q.options = new String[opts.length()];
                q.values = new String[opts.length()];
                for (int j = 0; j < opts.length(); j++) {
                    Object option = opts.opt(j);
                    if (option instanceof JSONObject) {
                        JSONObject optionObject = (JSONObject) option;
                        q.values[j] = optionObject.optString("value", String.valueOf(j));
                        q.options[j] = optionObject.optString("text", "");
                    } else {
                        q.options[j] = String.valueOf(option);
                        q.values[j] = vals != null && j < vals.length()
                                ? vals.optString(j) : String.valueOf(j);
                    }
                }
            }
            if (vals != null && (opts == null || q.values == null)) {
                q.values = new String[vals.length()];
                for (int j = 0; j < vals.length(); j++) q.values[j] = vals.optString(j);
            }

            JSONArray ans = o.optJSONArray("answers");
            if (ans != null) {
                q.answers = new String[ans.length()];
                for (int j = 0; j < ans.length(); j++) q.answers[j] = ans.optString(j);
            }

            q.numericAnswer = o.optDouble("numericAnswer", Double.NaN);
            q.tolerance = o.optDouble("tolerance", 0.0001);
            JSONObject range = o.optJSONObject("acceptedRange");
            if (range != null) {
                q.minAccepted = range.optDouble("min", Double.NaN);
                q.maxAccepted = range.optDouble("max", Double.NaN);
            }

            if (Double.isNaN(q.minAccepted) && Double.isNaN(q.maxAccepted)) {
                Object answerKey = o.opt("answerKeyRange");
                if (answerKey instanceof Number) {
                    q.numericAnswer = ((Number) answerKey).doubleValue();
                } else if (answerKey instanceof String) {
                    String answerText = ((String) answerKey).trim()
                            .replace("−", "-").replace("–", "-").replace("—", "-");
                    java.util.regex.Matcher matcher = java.util.regex.Pattern
                            .compile("^\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:to|-)\\s*(-?\\d+(?:\\.\\d+)?)\\s*$",
                                    java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(answerText);
                    if (matcher.matches()) {
                        q.minAccepted = Double.parseDouble(matcher.group(1));
                        q.maxAccepted = Double.parseDouble(matcher.group(2));
                    } else {
                        try { q.numericAnswer = Double.parseDouble(answerText); }
                        catch (NumberFormatException ignored) { }
                    }
                }
            }
            return q;
        } catch (Exception e) {
            return null;
        }
    }

    private void initScreen(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        TextView heading = new TextView(this);
        heading.setText(title);
        heading.setTextSize(24);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setTextColor(Color.WHITE);
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("Offline question bank");
        status.setTextSize(12);
        status.setTextColor(Color.LTGRAY);
        status.setPadding(0, dp(4), 0, dp(12));
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showHome() {
        initScreen("GATE PREP");
        status.setText("OFFLINE PRACTICE  •  CSE + DA");

        // Hero card
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(roundBg(Color.rgb(18, 38, 78), Color.rgb(45, 105, 210)));
        TextView eyebrow = new TextView(this);
        eyebrow.setText("YOUR PERSONAL PYQ WORKSPACE");
        eyebrow.setTextSize(11);
        eyebrow.setTypeface(null, Typeface.BOLD);
        eyebrow.setTextColor(Color.rgb(147, 197, 253));
        hero.addView(eyebrow);
        TextView heroTitle = new TextView(this);
        heroTitle.setText("Prepare smarter.\nOne question at a time.");
        heroTitle.setTextSize(25);
        heroTitle.setTypeface(null, Typeface.BOLD);
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setPadding(0, dp(7), 0, dp(6));
        hero.addView(heroTitle);
        TextView heroSub = new TextView(this);
        heroSub.setText("Practice previous-year questions, review answers, and revise anywhere — even offline.");
        heroSub.setTextSize(14);
        heroSub.setTextColor(Color.rgb(219, 234, 254));
        heroSub.setLineSpacing(dp(2), 1.0f);
        hero.addView(heroSub);
        LinearLayout.LayoutParams heroParams = matchWidth();
        heroParams.setMargins(0, dp(4), 0, dp(14));
        content.addView(hero, heroParams);

        int cseCount = countForBranch("CSE");
        int daCount = countForBranch("DA");
        addText("YOUR QUESTION BANK");
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(statCard("CSE", String.valueOf(cseCount), "questions"), new LinearLayout.LayoutParams(0, -2, 1));
        View gap = new View(this);
        stats.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        stats.addView(statCard("DA", String.valueOf(daCount), "questions"), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(stats, matchWidth());
        addSpacer(8);

        addLabel("Exam branch");
        Spinner branch = spinner(new String[]{"CSE", "DA"});
        branch.setSelection(selectedBranch.equals("DA") ? 1 : 0);
        content.addView(branch, matchWidth());

        addLabel("Practice mode");
        Spinner mode = spinner(new String[]{"Subject-wise", "Mixed quiz"});
        mode.setSelection(selectedMode.equals("Mixed quiz") ? 1 : 0);
        content.addView(mode, matchWidth());
        mode.setOnItemSelectedListener(new SimpleSelection(
                () -> selectedMode = mode.getSelectedItem().toString()));

        addLabel("Subject");
        Spinner subjects = spinner(subjectOptions());
        content.addView(subjects, matchWidth());
        subjects.setOnItemSelectedListener(new SimpleSelection(() -> {
            if (subjects.getSelectedItem() != null) {
                selectedSubject = subjects.getSelectedItem().toString();
            }
        }));
        branch.setOnItemSelectedListener(new SimpleSelection(() -> {
            selectedBranch = branch.getSelectedItem().toString();
            String previousSubject = selectedSubject;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    MainActivity.this, android.R.layout.simple_spinner_item, subjectOptions());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            subjects.setAdapter(adapter);
            int found = -1;
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).equals(previousSubject)) { found = i; break; }
            }
            subjects.setSelection(found >= 0 ? found : 0);
            selectedSubject = subjects.getSelectedItem() == null
                    ? "All subjects" : subjects.getSelectedItem().toString();
            updateBranchStats(branch);
        }));

        addLabel("Number of questions");
        EditText countInput = new EditText(this);
        countInput.setSingleLine(true);
        countInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        countInput.setHint("e.g. 30, 47, 53, 61");
        countInput.setText("20");
        countInput.setTextColor(Color.WHITE);
        countInput.setHintTextColor(Color.LTGRAY);
        countInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        countInput.setBackground(roundBg(Color.rgb(19, 27, 43), Color.rgb(47, 61, 83)));
        content.addView(countInput, matchWidth());

        CheckBox marathon = new CheckBox(this);
        marathon.setText("Marathon mode — keep going until I submit");
        marathon.setTextColor(Color.WHITE);
        marathon.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(59, 130, 246)));
        marathon.setPadding(0, dp(6), 0, dp(6));
        content.addView(marathon, matchWidth());

        Button start = button("START PRACTICE  →");
        start.setTextColor(Color.WHITE);
        start.setTextSize(16);
        start.setAllCaps(false);
        start.setBackground(roundBg(Color.rgb(37, 99, 235), Color.rgb(59, 130, 246)));
        start.setPadding(dp(8), dp(8), dp(8), dp(8));
        start.setOnClickListener(v -> {
            if (subjects.getSelectedItem() != null) selectedSubject = subjects.getSelectedItem().toString();
            selectedMode = mode.getSelectedItem().toString();
            int requested;
            if (marathon.isChecked()) {
                requested = 999999;
            } else {
                String raw = countInput.getText().toString().trim();
                try {
                    requested = Integer.parseInt(raw);
                    if (requested < 1) {
                        countInput.setError("Enter a number greater than zero");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    countInput.setError("Enter a valid whole number");
                    return;
                }
            }
            startQuiz(requested);
        });
        LinearLayout.LayoutParams startParams = matchWidth();
        startParams.setMargins(0, dp(14), 0, dp(6));
        content.addView(start, startParams);

        addLabel("More tools");
        Button notes = button("📘  Browse revision notes");
        notes.setAllCaps(false);
        notes.setOnClickListener(v -> showNotes());
        content.addView(notes);
        Button upload = button("＋  Import PDF papers");
        upload.setAllCaps(false);
        upload.setOnClickListener(v -> choosePdf());
        content.addView(upload);

        TextView footnote = new TextView(this);
        footnote.setText("Question counts show records loaded from your local JSON bank. Imported PDFs are stored locally; they are not automatically converted into quiz questions.");
        footnote.setTextSize(11);
        footnote.setTextColor(Color.rgb(148, 163, 184));
        footnote.setPadding(0, dp(12), 0, dp(8));
        content.addView(footnote);
    }

    private int countForBranch(String branch) {
        int count = 0;
        for (Question q : allQuestions) {
            if (branch.equalsIgnoreCase(q.branch)) count++;
        }
        return count;
    }

    private LinearLayout statCard(String title, String number, String caption) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(roundBg(Color.rgb(19, 27, 43), Color.rgb(47, 61, 83)));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(12);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(Color.rgb(147, 197, 253));
        card.addView(t);
        TextView n = new TextView(this);
        n.setText(number);
        n.setTextSize(24);
        n.setTypeface(null, Typeface.BOLD);
        n.setTextColor(Color.WHITE);
        n.setPadding(0, dp(3), 0, 0);
        card.addView(n);
        TextView c = new TextView(this);
        c.setText(caption);
        c.setTextSize(11);
        c.setTextColor(Color.LTGRAY);
        card.addView(c);
        return card;
    }

    private void updateBranchStats(Spinner branch) {
        // Stats are computed from the loaded bank; the dashboard refreshes on the next home render.
    }

    private JSONArray loadNotes() {
        try {
            InputStream in = getAssets().open("theory_notes.json");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (Exception e) { return new JSONArray(); }
    }

    private void showNotes() {
        JSONArray notes = loadNotes();
        initScreen("Revision notes");
        status.setText("Offline theory • " + selectedBranch);
        addText("Choose a topic to revise. The included notes are examples; replace them with your syllabus-aligned material.");
        int shown = 0;
        for (int i = 0; i < notes.length(); i++) {
            JSONObject note = notes.optJSONObject(i);
            if (note == null || !selectedBranch.equals(note.optString("branch", "CSE"))) continue;
            shown++;
            String title = note.optString("title", "Untitled topic");
            String subject = note.optString("subject", "General");
            String body = note.optString("content", "No note content.");
            Button item = button(subject + " — " + title);
            item.setOnClickListener(v -> showNoteDetail(subject, title, body));
            content.addView(item);
        }
        if (shown == 0) addText("No notes found for " + selectedBranch + ". Add entries to app/src/main/assets/theory_notes.json.");
        Button back = button("Back to home");
        back.setOnClickListener(v -> showHome());
        content.addView(back);
    }

    private void showNoteDetail(String subject, String title, String body) {
        initScreen(title);
        status.setText(selectedBranch + " • " + subject + " • Offline revision");
        TextView noteText = new TextView(this);
        noteText.setText(body);
        noteText.setTextSize(16);
        noteText.setTextColor(Color.rgb(35, 45, 60));
        noteText.setLineSpacing(dp(3), 1.05f);
        noteText.setPadding(0, dp(12), 0, dp(16));
        content.addView(noteText);
        Button back = button("Back to notes");
        back.setOnClickListener(v -> showNotes());
        content.addView(back);
        Button home = button("Back to home");
        home.setOnClickListener(v -> showHome());
        content.addView(home);
    }


    private static final int PICK_PDF = 4107;

    private void choosePdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_PDF || resultCode != RESULT_OK || data == null) return;
        int imported = 0;
        try {
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (copyPdfToInbox(uri)) imported++;
                }
            } else if (data.getData() != null && copyPdfToInbox(data.getData())) {
                imported++;
            }
            new android.app.AlertDialog.Builder(this)
                .setTitle("PDFs imported")
                .setMessage(imported + " PDF file(s) copied into the app's private import inbox. They are stored locally and remain available offline. Importing a PDF does not yet automatically turn its contents into quiz questions; the files must be processed and verified into questions.json.")
                .setPositiveButton("OK", (d,w) -> showHome()).show();
        } catch (Exception e) {
            new android.app.AlertDialog.Builder(this).setTitle("Import failed")
                .setMessage(e.getMessage() == null ? "Could not copy selected PDF." : e.getMessage())
                .setPositiveButton("OK", (d,w) -> showHome()).show();
        }
    }

    private boolean copyPdfToInbox(Uri uri) throws Exception {
        String name = "paper_" + System.currentTimeMillis() + ".pdf";
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int ix = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (ix >= 0 && cursor.moveToFirst()) {
                    String candidate = cursor.getString(ix);
                    if (candidate != null && candidate.toLowerCase().endsWith(".pdf")) name = candidate.replaceAll("[^A-Za-z0-9._-]", "_");
                }
                cursor.close();
            }
        } catch (Exception ignored) { }
        File inbox = new File(getFilesDir(), "pdf_inbox");
        if (!inbox.exists() && !inbox.mkdirs()) throw new Exception("Could not create private PDF inbox.");
        File target = new File(inbox, System.currentTimeMillis() + "_" + name);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new Exception("Could not read selected PDF.");
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
        return true;
    }

    private void refreshSubjects() {
        // The active subject Spinner is refreshed directly by showHome() when branch changes.
        // Keep selectedSubject valid for callers that switch branch elsewhere.
        boolean exists = "All subjects".equals(selectedSubject);
        for (Question q : allQuestions) {
            if (q.branch.equals(selectedBranch) && q.subject.equals(selectedSubject)) {
                exists = true;
                break;
            }
        }
        if (!exists) selectedSubject = "All subjects";
    }

    private String[] subjectOptions() {
        List<String> s = new ArrayList<>(); s.add("All subjects");
        for (Question q : allQuestions) if (q.branch.equals(selectedBranch) && !s.contains(q.subject)) s.add(q.subject);
        return s.toArray(new String[0]);
    }

    private void startQuiz(int requested) {
        quiz.clear(); selectedValues.clear(); userAnswers.clear(); current = 0; score = 0; submitted = false;
        for (Question q : allQuestions) {
            if (!q.branch.equals(selectedBranch)) continue;
            if (selectedMode.equals("Subject-wise") && !selectedSubject.equals("All subjects") && !q.subject.equals(selectedSubject)) continue;
            quiz.add(q);
        }
        Collections.shuffle(quiz);
        if (quiz.size() > requested) while (quiz.size() > requested) quiz.remove(quiz.size()-1);
        if (quiz.isEmpty()) {
            new android.app.AlertDialog.Builder(this).setTitle("No questions yet")
                .setMessage("No questions match this selection. Check the selected branch/subject and the JSON asset bundled in app/src/main/assets.")
                .setPositiveButton("OK", (d,w) -> showHome()).show();
            return;
        }
        for (int i = 0; i < quiz.size(); i++) userAnswers.add(new ArrayList<>());
        showQuestion();
    }


    /**
     * Loads a question image from app/src/main/assets.
     * JSON paths may be written as "images_CS/...jpg", "assets/images_CS/...jpg",
     * or with Windows backslashes; all are normalized to Android asset paths.
     */
    private android.graphics.Bitmap loadQuestionBitmap(String rawPath) throws Exception {
        String path = rawPath == null ? "" : rawPath.trim().replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        if (path.startsWith("assets/")) path = path.substring("assets/".length());
        if (path.isEmpty()) return null;

        try (InputStream imageStream = getAssets().open(path)) {
            return android.graphics.BitmapFactory.decodeStream(imageStream);
        }
    }

    private void showQuestion() {
        Question q = quiz.get(current);
        selectedValues.clear();
        if (current < userAnswers.size()) selectedValues.addAll(userAnswers.get(current));
        natInput = null;

        initScreen("Quiz");
        status.setText("Question " + (current + 1) + " of " + quiz.size()
                + " • " + q.branch + " / " + q.subject);
        addText(paperIdentity(q));
        addText(q.type + (q.marks > 0 ? " • " + q.marks + " marks" : "")
                + (q.topic.isEmpty() ? "" : " • " + q.topic));

        TextView prompt = new TextView(this);
        prompt.setText(q.prompt);
        prompt.setTextSize(17);
        prompt.setTypeface(null, Typeface.BOLD);
        prompt.setTextColor(Color.WHITE);
        prompt.setPadding(dp(12), dp(14), dp(12), dp(14));
        prompt.setBackground(roundBg(Color.rgb(20, 20, 20), Color.rgb(55, 55, 55)));
        content.addView(prompt, matchWidth());

        if (q.imagePath != null && !q.imagePath.trim().isEmpty()) {
            ImageView questionImage = new ImageView(this);
            questionImage.setAdjustViewBounds(true);
            questionImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            questionImage.setPadding(dp(4), dp(8), dp(4), dp(8));
            try {
                android.graphics.Bitmap bitmap = loadQuestionBitmap(q.imagePath);
                if (bitmap != null) {
                    questionImage.setImageBitmap(bitmap);
                    LinearLayout.LayoutParams imageParams = matchWidth();
                    imageParams.setMargins(0, dp(6), 0, dp(8));
                    content.addView(questionImage, imageParams);
                } else {
                    addText("Question image could not be decoded: " + q.imagePath);
                }
            } catch (Exception imageError) {
                addText("Question image unavailable: " + q.imagePath);
            }
        }

        if ("NAT".equalsIgnoreCase(q.type)) {
            natInput = new EditText(this);
            natInput.setSingleLine(true);
            natInput.setHint("Enter numerical answer");
            natInput.setTextColor(Color.WHITE);
            natInput.setHintTextColor(Color.LTGRAY);
            natInput.setTextSize(16);
            natInput.setInputType(8194);
            natInput.setPadding(dp(12), dp(10), dp(12), dp(10));
            natInput.setBackground(roundBg(Color.rgb(25, 25, 25), Color.GRAY));
            if (!selectedValues.isEmpty()) natInput.setText(selectedValues.get(0));
            content.addView(natInput, matchWidth());
        } else if (q.options != null) {
            addText("MSQ".equalsIgnoreCase(q.type)
                    ? "Select all correct answers" : "Tap an option to select or unselect it");
            for (int i = 0; i < q.options.length; i++) {
                final String value = q.values != null && i < q.values.length
                        ? q.values[i] : String.valueOf(i);
                final boolean msq = "MSQ".equalsIgnoreCase(q.type);
                CheckBox option = new CheckBox(this);
                option.setText(q.options[i]);
                option.setTextSize(14);
                option.setTextColor(Color.WHITE);
                option.setPadding(dp(8), dp(7), dp(8), dp(7));
                option.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.rgb(60, 130, 240)));
                option.setChecked(selectedValues.contains(value));
                option.setBackground(roundBg(Color.rgb(18, 18, 18), Color.rgb(55, 55, 55)));
                LinearLayout.LayoutParams op = matchWidth();
                op.setMargins(0, dp(4), 0, dp(4));
                content.addView(option, op);
                option.setOnCheckedChangeListener((button, checked) -> {
                    if (checked) {
                        if (!msq) {
                            selectedValues.clear();
                            for (int k = 0; k < content.getChildCount(); k++) {
                                View child = content.getChildAt(k);
                                if (child instanceof CheckBox && child != option) {
                                    ((CheckBox) child).setChecked(false);
                                }
                            }
                            if (!selectedValues.contains(value)) selectedValues.add(value);
                        } else if (!selectedValues.contains(value)) {
                            selectedValues.add(value);
                        }
                    } else {
                        selectedValues.remove(value);
                    }
                    userAnswers.set(current, new ArrayList<>(selectedValues));
                });
            }
        }

        addSpacer(12);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button previous = button("Previous");
        previous.setEnabled(current > 0);
        previous.setOnClickListener(v -> {
            saveCurrentAnswer(q);
            if (current > 0) current--;
            showQuestion();
        });
        nav.addView(previous, new LinearLayout.LayoutParams(0, -2, 1));

        Button next = button(current == quiz.size() - 1 ? "Submit" : "Next");
        next.setOnClickListener(v -> {
            saveCurrentAnswer(q);
            if (current == quiz.size() - 1) {
                showResults();
            } else {
                current++;
                showQuestion();
            }
        });
        nav.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(nav);

        Button finish = button("Finish session & see attempted answers");
        finish.setAllCaps(false);
        finish.setOnClickListener(v -> {
            saveCurrentAnswer(q);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Finish this session?")
                    .setMessage("Results will include only questions you attempted. Unanswered questions will be hidden.")
                    .setNegativeButton("Keep practising", null)
                    .setPositiveButton("Finish", (dialog, which) -> showResults())
                    .show();
        });
        content.addView(finish);

        Button quit = button("Quit quiz");
        quit.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
                .setMessage("Quit this quiz? Your current attempt will be discarded.")
                .setNegativeButton("Continue", null)
                .setPositiveButton("Quit", (d, w) -> showHome()).show());
        content.addView(quit);
    }

    private void saveCurrentAnswer(Question q) {
        if ("NAT".equalsIgnoreCase(q.type)) {
            selectedValues.clear();
            if (natInput != null) {
                String answer = natInput.getText().toString().trim();
                if (!answer.isEmpty()) selectedValues.add(answer);
            }
        }
        userAnswers.set(current, new ArrayList<>(selectedValues));
    }

    private boolean isCorrect(Question q, List<String> values) {
        if ("NAT".equalsIgnoreCase(q.type)) {
            if (values == null || values.isEmpty()) return false;
            try {
                double entered = Double.parseDouble(values.get(0).trim());
                if (Double.isNaN(entered) || Double.isInfinite(entered)) return false;

                if (!Double.isNaN(q.minAccepted) && !Double.isNaN(q.maxAccepted)) {
                    double min = Math.min(q.minAccepted, q.maxAccepted);
                    double max = Math.max(q.minAccepted, q.maxAccepted);
                    return entered >= min - q.tolerance && entered <= max + q.tolerance;
                }
                if (!Double.isNaN(q.numericAnswer)) {
                    return Math.abs(entered - q.numericAnswer) <= q.tolerance;
                }
                return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (values == null || q.answers == null) return false;
        if (values.size() != q.answers.length) return false;
        for (String a : q.answers) if (!values.contains(a)) return false;
        return true;
    }

    private void showResults() {
        initScreen("Your Results");
        status.setText("SESSION COMPLETE • " + selectedBranch + " • OFFLINE");

        int correctCount = 0, wrongCount = 0, skippedCount = 0, attemptedCount = 0;
        for (int i = 0; i < quiz.size(); i++) {
            List<String> given = userAnswers.get(i);
            if (given.isEmpty()) {
                skippedCount++;
            } else {
                attemptedCount++;
                if (isCorrect(quiz.get(i), given)) correctCount++;
                else wrongCount++;
            }
        }

        TextView heading = new TextView(this);
        heading.setText("Your Results");
        heading.setTextSize(24);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.WHITE);
        heading.setPadding(0, dp(4), 0, dp(16));
        content.addView(heading);

        LinearLayout summary = new LinearLayout(this);
        summary.setOrientation(LinearLayout.HORIZONTAL);
        addSummaryItem(summary, String.valueOf(correctCount), "Correct", Color.rgb(46, 204, 113));
        addSummaryItem(summary, String.valueOf(wrongCount), "Incorrect", Color.rgb(255, 85, 85));
        addSummaryItem(summary, String.valueOf(skippedCount), "Unanswered", Color.LTGRAY);
        content.addView(summary, matchWidth());
        addText("Attempted: " + attemptedCount + " of " + quiz.size()
                + " • Unattempted questions are hidden from this review.");
        addSpacer(10);
        addDivider();

        for (int i = 0; i < quiz.size(); i++) {
            Question q = quiz.get(i);
            List<String> given = userAnswers.get(i);
            if (given.isEmpty()) continue; // Marathon review shows attempted questions only.
            boolean right = isCorrect(q, given);
            int color = right ? Color.rgb(46, 204, 113) : Color.rgb(255, 85, 85);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(3), dp(15), dp(3), dp(15));

            TextView question = new TextView(this);
            question.setText("Q" + (i + 1) + ". " + q.prompt);
            question.setTextSize(15);
            question.setTypeface(null, Typeface.BOLD);
            question.setTextColor(Color.rgb(235, 235, 235));
            card.addView(question);
            addReviewLine(card, "Paper: " + paperIdentity(q), Color.LTGRAY);

            TextView badge = new TextView(this);
            badge.setText("● " + (right ? "RIGHT" : "WRONG"));
            badge.setTextSize(12);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(color);
            badge.setPadding(0, dp(7), 0, dp(5));
            card.addView(badge);

            addReviewLine(card, "Your answer: " + getUserAnswerText(q, given), Color.LTGRAY);
            addReviewLine(card, "Correct answer: " + getCorrectAnswerText(q), Color.rgb(46, 204, 113));
            if (q.explanation != null && !q.explanation.trim().isEmpty()
                    && !q.explanation.equals("No explanation added yet.")) {
                addReviewLine(card, "Explanation: " + q.explanation, Color.LTGRAY);
            }
            if (q.source != null && !q.source.isEmpty()
                    && !"Source not available".equals(q.source)) {
                addReviewLine(card, "Source paper: " + q.source, Color.GRAY);
            }
            content.addView(card, matchWidth());
            addDivider();
        }

        Button home = button("Back to Home");
        home.setOnClickListener(v -> showHome());
        content.addView(home);
    }

    private void addSummaryItem(LinearLayout row, String number, String label, int color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView value = new TextView(this);
        value.setText(number);
        value.setTextSize(26);
        value.setTypeface(null, Typeface.BOLD);
        value.setTextColor(color);
        value.setGravity(Gravity.CENTER);
        item.addView(value);
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextSize(12);
        caption.setTextColor(Color.LTGRAY);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, dp(6), 0, 0);
        item.addView(caption);
        row.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
    }

    private void addReviewLine(LinearLayout card, String text, int color) {
        TextView line = new TextView(this);
        line.setText(text);
        line.setTextSize(13);
        line.setTextColor(color);
        line.setPadding(0, dp(3), 0, dp(2));
        card.addView(line);
    }

    private void addDivider() {
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(45, 45, 45));
        content.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private String getUserAnswerText(Question q, List<String> given) {
        if (given == null || given.isEmpty()) return "Not answered";
        if ("NAT".equalsIgnoreCase(q.type)) return given.get(0);
        return getOptionText(q, given.toArray(new String[0]));
    }

    private String getCorrectAnswerText(Question q) {
        if ("NAT".equalsIgnoreCase(q.type)) {
            if (!Double.isNaN(q.minAccepted) && !Double.isNaN(q.maxAccepted)) {
                if (Double.compare(q.minAccepted, q.maxAccepted) == 0) {
                    return String.valueOf(q.minAccepted);
                }
                return q.minAccepted + " to " + q.maxAccepted;
            }
            return Double.isNaN(q.numericAnswer)
                    ? "Answer not supplied" : String.valueOf(q.numericAnswer);
        }
        if (q.answers == null || q.answers.length == 0) return "Answer not supplied";
        return getOptionText(q, q.answers);
    }

    private String paperIdentity(Question q) {
        StringBuilder label = new StringBuilder("GATE ").append(q.branch);
        if (q.year != null && !q.year.trim().isEmpty()) label.append(" ").append(q.year.trim());
        if (q.paperSession != null && !q.paperSession.trim().isEmpty()) {
            label.append(" • ").append(q.paperSession.trim());
        }
        if (q.questionNumber >= 0) label.append(" • Original Q").append(q.questionNumber);
        if (q.officialSection != null && !q.officialSection.trim().isEmpty()) {
            label.append(" • ").append(q.officialSection.trim());
        }
        return label.toString();
    }

    private String getOptionText(Question q, String[] values) {
        List<String> labels = new ArrayList<>();
        for (String value : values) {
            boolean found = false;
            if (q.values != null && q.options != null) {
                for (int i = 0; i < q.values.length; i++) {
                    if (q.values[i].equals(value)) {
                        labels.add(value + ": " + q.options[i]);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) labels.add(value);
        }
        return join(labels.toArray(new String[0]));
    }

    private String join(String[] a) { StringBuilder b=new StringBuilder(); for(int i=0;i<a.length;i++){if(i>0)b.append(", ");b.append(a[i]);} return b.toString(); }
    private android.graphics.drawable.GradientDrawable roundBg(int fill, int stroke) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    /**
     * Styled native dropdown used on the home screen.
     * Keeps Android Spinner behavior/accessibility, but uses a dark rounded
     * field, clear selected text, a visible chevron, and custom popup rows.
     */
    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this, Spinner.MODE_DROPDOWN);
        s.setPopupBackgroundDrawable(roundBg(Color.rgb(20, 29, 45), Color.rgb(59, 130, 246)));
        s.setDropDownVerticalOffset(dp(6));
        s.setDropDownWidth(-1);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values) {
            private TextView makeRow(String value, boolean selected) {
                TextView row = new TextView(MainActivity.this);
                row.setText(value);
                row.setTextSize(15);
                row.setTextColor(selected ? Color.WHITE : Color.rgb(220, 230, 245));
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(14), dp(13), dp(14), dp(13));
                row.setMinHeight(dp(48));
                if (selected) {
                    row.setTypeface(null, Typeface.BOLD);
                    row.setBackground(roundBg(Color.rgb(31, 65, 112), Color.rgb(59, 130, 246)));
                } else {
                    row.setBackgroundColor(Color.rgb(20, 29, 45));
                }
                return row;
            }

            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView row = makeRow(getItem(position), true);
                row.setText(getItem(position) + "     ▾");
                row.setBackground(roundBg(Color.rgb(19, 27, 43), Color.rgb(65, 91, 130)));
                row.setPadding(dp(14), dp(13), dp(14), dp(13));
                return row;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView row = makeRow(getItem(position), false);
                if (position == s.getSelectedItemPosition()) {
                    row.setText("✓  " + getItem(position));
                    row.setTextColor(Color.rgb(147, 197, 253));
                    row.setTypeface(null, Typeface.BOLD);
                } else {
                    row.setText("    " + getItem(position));
                }
                return row;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setPadding(0, 0, 0, 0);
        s.setMinimumHeight(dp(50));
        s.setBackground(roundBg(Color.rgb(19, 27, 43), Color.rgb(65, 91, 130)));
        return s;
    }
    private void addLabel(String s) { TextView t=new TextView(this); t.setText(s); t.setTextSize(14); t.setTypeface(null,Typeface.BOLD); t.setTextColor(Color.LTGRAY); t.setPadding(0,dp(12),0,dp(4)); content.addView(t); }
    private void addText(String s) { TextView t=new TextView(this); t.setText(s); t.setTextSize(15); t.setTextColor(Color.LTGRAY); t.setPadding(0,dp(8),0,dp(8)); content.addView(t); }
    private Button button(String s) { Button b=new Button(this); b.setText(s); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(7),0,dp(7)); b.setLayoutParams(p); return b; }
    private void addSpacer(int n) { View v=new View(this); content.addView(v,new LinearLayout.LayoutParams(1,dp(n))); }
    private int dp(int x) { return (int)(x*getResources().getDisplayMetrics().density+0.5f); }

    private class SimpleSelection implements AdapterView.OnItemSelectedListener {
        private final Runnable run; SimpleSelection(Runnable r){run=r;}
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id){run.run();}
        public void onNothingSelected(AdapterView<?> p){}
    }
}
