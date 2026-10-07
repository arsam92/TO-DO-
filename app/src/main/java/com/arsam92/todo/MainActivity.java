package com.arsam92.todo;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.res.ColorStateList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class MainActivity extends android.app.Activity {
    private final int BG = 0xFFF7F8FC;
    private final int SURFACE = 0xFFFFFFFF;
    private final int INK = 0xFF171824;
    private final int MUTED = 0xFF808394;
    private final int ACCENT = 0xFF5D5FEF;
    private final int STAR = 0xFFF4C542;
    private final int DANGER = 0xFFFF5B66;
    private final int LINE = 0xFFE9EAF1;

    private SharedPreferences prefs;
    private final Map<String, ArrayList<Task>> tasksByDay = new HashMap<>();
    private Calendar selectedDay = Calendar.getInstance();

    private LinearLayout dayStrip;
    private LinearLayout taskContainer;
    private TextView dateTitle;
    private TextView progressText;
    private ProgressBar progressBar;

    static class Task {
        String title;
        boolean done;
        boolean important;

        Task(String title, boolean done, boolean important) {
            this.title = title;
            this.done = done;
            this.important = important;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        prefs = getSharedPreferences("todo_store", MODE_PRIVATE);
        loadAll();
        buildUi();
        animateEntrance();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView label(String text, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable strokeRounded(int fill, int stroke, int strokeDp, float radiusDp) {
        GradientDrawable g = rounded(fill, radiusDp);
        g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, dp(96));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), 0);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(76)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        header.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(10), 0, 0, 0);

        TextView title = label("TO DO", 25, INK, true);
        TextView subtitle = label("Make today count", 13, MUTED, false);
        titleBox.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));
        titleBox.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(24)));

        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));

        TextView addTop = label("+", 28, 0xFFFFFFFF, true);
        addTop.setGravity(Gravity.CENTER);
        addTop.setBackground(rounded(INK, 18));
        addTop.setElevation(dp(4));
        addTop.setContentDescription("Add task");
        addTop.setOnClickListener(v -> showAddDialog());
        header.addView(addTop, new LinearLayout.LayoutParams(dp(48), dp(48)));

        FrameLayout summary = new FrameLayout(this);
        summary.setBackground(rounded(SURFACE, 24));
        summary.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout.LayoutParams summaryLp = new LinearLayout.LayoutParams(-1, dp(92));
        summaryLp.setMargins(0, dp(6), 0, dp(18));
        content.addView(summary, summaryLp);

        LinearLayout summaryLeft = new LinearLayout(this);
        summaryLeft.setOrientation(LinearLayout.VERTICAL);

        TextView weekLabel = label("WEEK AT A GLANCE", 10, MUTED, true);
        weekLabel.setLetterSpacing(0.12f);
        dateTitle = label("", 18, INK, true);

        summaryLeft.addView(weekLabel, new LinearLayout.LayoutParams(-1, dp(22)));
        summaryLeft.addView(dateTitle, new LinearLayout.LayoutParams(-1, dp(30)));

        summary.addView(summaryLeft,
                new FrameLayout.LayoutParams(dp(230), -2, Gravity.START | Gravity.CENTER_VERTICAL));

        LinearLayout progressBox = new LinearLayout(this);
        progressBox.setOrientation(LinearLayout.VERTICAL);
        progressBox.setGravity(Gravity.END);

        progressText = label("0 / 0 done", 13, ACCENT, true);
        progressText.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressDrawable(getResources().getDrawable(android.R.drawable.progress_horizontal));

        progressBox.addView(progressText, new LinearLayout.LayoutParams(-1, dp(24)));
        progressBox.addView(progressBar, new LinearLayout.LayoutParams(dp(96), dp(10)));

        summary.addView(progressBox,
                new FrameLayout.LayoutParams(dp(110), dp(48), Gravity.END | Gravity.CENTER_VERTICAL));

        TextView daysTitle = label("YOUR WEEK", 11, MUTED, true);
        daysTitle.setLetterSpacing(0.12f);
        content.addView(daysTitle, new LinearLayout.LayoutParams(-1, dp(26)));

        HorizontalScrollView daysScroll = new HorizontalScrollView(this);
        daysScroll.setHorizontalScrollBarEnabled(false);
        daysScroll.setClipToPadding(false);

        dayStrip = new LinearLayout(this);
        dayStrip.setOrientation(LinearLayout.HORIZONTAL);

        daysScroll.addView(dayStrip, new HorizontalScrollView.LayoutParams(-2, dp(78)));
        content.addView(daysScroll, new LinearLayout.LayoutParams(-1, dp(78)));

        taskContainer = new LinearLayout(this);
        taskContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams tasksLp = new LinearLayout.LayoutParams(-1, -2);
        tasksLp.setMargins(0, dp(8), 0, dp(8));
        content.addView(taskContainer, tasksLp);

        FrameLayout fab = new FrameLayout(this);
        fab.setBackground(rounded(ACCENT, 22));
        fab.setElevation(dp(8));

        TextView plus = label("+", 28, 0xFFFFFFFF, true);
        plus.setGravity(Gravity.CENTER);
        fab.addView(plus, new FrameLayout.LayoutParams(-1, -1));
        fab.setContentDescription("Add task");
        fab.setOnClickListener(v -> showAddDialog());

        FrameLayout.LayoutParams fabLp =
                new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM | Gravity.END);
        fabLp.setMargins(0, 0, dp(20), dp(22));
        root.addView(fab, fabLp);

        setContentView(root);
        renderDay();
    }

    private void buildDays() {
        dayStrip.removeAllViews();

        Calendar monday = (Calendar) selectedDay.clone();
        int dow = monday.get(Calendar.DAY_OF_WEEK);
        int shift = (dow == Calendar.SUNDAY) ? -6 : Calendar.MONDAY - dow;
        monday.add(Calendar.DAY_OF_MONTH, shift);

        for (int i = 0; i < 7; i++) {
            Calendar c = (Calendar) monday.clone();
            c.add(Calendar.DAY_OF_MONTH, i);

            boolean selected = sameDay(c, selectedDay);
            String key = key(c);
            ArrayList<Task> list = tasksByDay.get(key);
            int count = list == null ? 0 : list.size();

            String dayName = new SimpleDateFormat("EEE", Locale.US).format(c.getTime());
            String num = new SimpleDateFormat("dd", Locale.US).format(c.getTime());

            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(selected ? rounded(INK, 18) : rounded(SURFACE, 18));
            chip.setPadding(dp(12), dp(6), dp(12), dp(6));

            TextView d = label(dayName, 11, selected ? 0xFFFFFFFF : MUTED, true);
            d.setGravity(Gravity.CENTER);

            TextView n = label(num, 18, selected ? 0xFFFFFFFF : INK, true);
            n.setGravity(Gravity.CENTER);

            TextView dot = label(count == 0 ? "" : "•", 14,
                    selected ? 0xFFFFFFFF : ACCENT, true);
            dot.setGravity(Gravity.CENTER);

            chip.addView(d, new LinearLayout.LayoutParams(dp(38), dp(23)));
            chip.addView(n, new LinearLayout.LayoutParams(dp(38), dp(29)));
            chip.addView(dot, new LinearLayout.LayoutParams(dp(38), dp(12)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(66), dp(70));
            lp.setMargins(i == 0 ? 0 : dp(7), dp(2), 0, dp(2));
            dayStrip.addView(chip, lp);

            chip.setOnClickListener(v -> {
                selectedDay = c;
                buildDays();
                renderDay();
            });
        }
    }

    private void renderDay() {
        buildDays();

        String k = key(selectedDay);
        ArrayList<Task> list = tasksByDay.get(k);
        if (list == null) list = new ArrayList<>();

        dateTitle.setText(new SimpleDateFormat("EEEE, MMM d", Locale.US)
                .format(selectedDay.getTime()));

        int done = 0;
        for (Task t : list) if (t.done) done++;

        int total = list.size();
        int percent = total == 0 ? 0 : Math.round(done * 100f / total);

        progressText.setText(done + " / " + total + " done");
        progressBar.setProgress(percent);

        taskContainer.removeAllViews();

        if (total == 0) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(30), dp(24), dp(30), dp(28));
            empty.setBackground(strokeRounded(SURFACE, LINE, 1, 24));

            TextView e1 = label("Nothing planned yet", 18, INK, true);
            e1.setGravity(Gravity.CENTER);

            TextView e2 = label("Tap + and give this day a little structure.", 13, MUTED, false);
            e2.setGravity(Gravity.CENTER);

            empty.addView(e1, new LinearLayout.LayoutParams(-1, dp(30)));
            empty.addView(e2, new LinearLayout.LayoutParams(-1, dp(24)));

            taskContainer.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            addTaskRow(list, i);
        }
    }

    private void addTaskRow(ArrayList<Task> list, int index) {
        Task task = list.get(index);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(8), dp(10), dp(8));
        row.setBackground(strokeRounded(SURFACE, LINE, 1, 20));

        CheckBox box = new CheckBox(this);
        box.setChecked(task.done);
        box.setButtonTintList(ColorStateList.valueOf(ACCENT));
        box.setContentDescription("Complete task");
        row.addView(box, new LinearLayout.LayoutParams(dp(42), dp(48)));

        TextView title = label(task.title, 15, task.done ? MUTED : INK, !task.done);
        if (task.done) {
            title.setPaintFlags(title.getPaintFlags() |
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }

        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView star = label(task.important ? "★" : "☆", 24,
                task.important ? STAR : MUTED, true);
        star.setGravity(Gravity.CENTER);
        star.setContentDescription(task.important
                ? "Important. Tap to remove priority."
                : "Mark important");
        row.addView(star, new LinearLayout.LayoutParams(dp(44), dp(48)));

        TextView del = label("×", 24, DANGER, false);
        del.setGravity(Gravity.CENTER);
        del.setContentDescription("Delete task");
        row.addView(del, new LinearLayout.LayoutParams(dp(38), dp(48)));

        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, dp(66));
        rlp.setMargins(0, 0, 0, dp(10));
        taskContainer.addView(row, rlp);

        box.setOnClickListener(v -> {
            task.done = box.isChecked();
            saveAll();
            row.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(90)
                    .withEndAction(() -> {
                        row.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(90)
                                .start();
                        renderDay();
                    })
                    .start();
        });

        title.setOnClickListener(v -> {
            task.done = !task.done;
            saveAll();
            renderDay();
        });

        star.setOnClickListener(v -> {
            task.important = !task.important;
            saveAll();
            star.animate()
                    .rotationBy(180)
                    .setDuration(220)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            renderDay();
        });

        del.setOnClickListener(v -> {
            list.remove(task);
            saveAll();
            row.animate()
                    .alpha(0f)
                    .translationX(dp(40))
                    .setDuration(180)
                    .withEndAction(this::renderDay)
                    .start();
        });

        row.setAlpha(0f);
        row.setTranslationY(dp(10));
        row.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(260)
                .setStartDelay(index * 45L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void showAddDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(6), dp(22), 0);

        EditText input = new EditText(this);
        input.setHint("What needs to get done?");
        input.setTextSize(16);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setBackground(strokeRounded(SURFACE, LINE, 1, 18));
        input.setPadding(dp(15), 0, dp(15), 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, dp(54)));

        CheckBox important = new CheckBox(this);
        important.setText("Mark as important");
        important.setTextSize(14);
        important.setTextColor(INK);
        important.setButtonTintList(ColorStateList.valueOf(ACCENT));
        box.addView(important, new LinearLayout.LayoutParams(-1, dp(52)));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("New task")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = input.getText().toString().trim();

                if (text.isEmpty()) {
                    input.setError("Write a task first");
                    return;
                }

                String k = key(selectedDay);
                ArrayList<Task> list = tasksByDay.computeIfAbsent(k, kk -> new ArrayList<>());
                list.add(new Task(text, false, important.isChecked()));

                saveAll();
                dialog.dismiss();
                renderDay();
            });

            input.requestFocus();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });

        dialog.show();
    }

    private void animateEntrance() {
        View root = findViewById(android.R.id.content);
        root.setAlpha(0f);
        root.setScaleX(0.985f);
        root.setScaleY(0.985f);

        root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private String key(Calendar c) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private void loadAll() {
        String raw = prefs.getString("tasks", "{}");

        try {
            JSONObject root = new JSONObject(raw);

            for (String day : root.keySet()) {
                JSONArray arr = root.optJSONArray(day);
                if (arr == null) continue;

                ArrayList<Task> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new Task(
                            o.optString("title", ""),
                            o.optBoolean("done", false),
                            o.optBoolean("important", false)
                    ));
                }

                tasksByDay.put(day, list);
            }
        } catch (Exception ignored) {
            tasksByDay.clear();
        }
    }

    private void saveAll() {
        try {
            JSONObject root = new JSONObject();

            for (Map.Entry<String, ArrayList<Task>> e : tasksByDay.entrySet()) {
                JSONArray arr = new JSONArray();

                for (Task t : e.getValue()) {
                    JSONObject o = new JSONObject();
                    o.put("title", t.title);
                    o.put("done", t.done);
                    o.put("important", t.important);
                    arr.put(o);
                }

                root.put(e.getKey(), arr);
            }

            prefs.edit().putString("tasks", root.toString()).apply();
        } catch (Exception ignored) {
        }
    }
}
