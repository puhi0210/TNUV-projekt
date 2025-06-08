package si.uni_lj.fe.tnuv.novijazv1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;

import android.util.TypedValue;


import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.view.ViewGroup;


import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.YearMonth;
import org.threeten.bp.format.DateTimeFormatter;


import com.jakewharton.threetenabp.AndroidThreeTen;

import si.uni_lj.fe.tnuv.novijazv1.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private TextView textDays;
    private TextView progressLabel;
    private ProgressBar progressBar;

    private TextView monthYearTV;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;
    private LocalDate currentDialogDate = null;
    private AlertDialog currentDialog = null;
    private boolean isEventDialogVisible = false;


    private static final long[] MILESTONES_DAYS = {
            1, 2, 3, 7, 14, 21, 30, 60, 90, 180, 270, 365,
            365 * 2, 365 * 3, 365 * 5, 365 * 10, 365 * 15, 365 * 20, 365 * 25
    };

    private final android.os.Handler handler = new android.os.Handler();
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateProgressUI();
            handler.postDelayed(this, 60 * 1000); // ponovi vsako minuto
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        handler.post(progressUpdater); // začni osveževati
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(progressUpdater); // ustavi osveževanje
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidThreeTen.init(this);
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("tracker_data", MODE_PRIVATE);
        String dependency_name = prefs.getString("dependency_name", null);
        if (dependency_name == null) {
            // NI podatkov - preusmeri nazaj na IntroActivity
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);


        String dependencyName = prefs.getString("dependency_name", "Novi Jaz");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(dependencyName);
        }

        // Inicializiraj referencirane UI komponente
        textDays = findViewById(R.id.textDays);
        progressBar = findViewById(R.id.progressBar);
        progressLabel = findViewById(R.id.progressLabel);

        monthYearTV = findViewById(R.id.monthYearTV);
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);

        Button buttonPrev = findViewById(R.id.buttonPrevMonth);
        Button buttonNext = findViewById(R.id.buttonNextMonth);

        buttonPrev.setOnClickListener(v -> {
            selectedDate = selectedDate.minusMonths(1);
            setMonthView();
        });

        buttonNext.setOnClickListener(v -> {
            selectedDate = selectedDate.plusMonths(1);
            setMonthView();
        });


        selectedDate = LocalDate.now();
        setMonthView();

        // Prikaz napredka
        updateProgressUI();

        // FAB za dodajanje dogodkov
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Odpri AddEntryActivity
                Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
                startActivityForResult(intent, 1);
            }
        });


    }
    private void setMonthView() {
        monthYearTV.setText(capitalizeMonth(monthYearFromDate(selectedDate)));
        ArrayList<LocalDate> daysInMonth = daysInMonthArray(selectedDate);

        SharedPreferences prefs = getSharedPreferences("tracker_data", MODE_PRIVATE);
        String startDateStr = prefs.getString("start_date", null);

        LocalDate finalStartDate = null;
        if (startDateStr != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d. M. yyyy HH:mm", Locale.getDefault());
                finalStartDate = LocalDate.parse(startDateStr, formatter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LocalDate finalFinalStartDate = finalStartDate;

        new Thread(() -> {
            List<HabitEvent> allEvents = AppDatabase.getInstance(this).habitEventDao().getAll();
            Map<LocalDate, Boolean> eventMap = new HashMap<>();
            Map<LocalDate, Integer> eventCounts = new HashMap<>();

            for (HabitEvent event : allEvents) {
                LocalDate eventDate = Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                eventCounts.put(eventDate, eventCounts.getOrDefault(eventDate, 0) + 1);
                if (!eventMap.containsKey(eventDate)) {
                    eventMap.put(eventDate, !event.isGood);
                } else if (!event.isGood) {
                    eventMap.put(eventDate, true);
                }
            }

            if (finalFinalStartDate != null) {
                LocalDate today = LocalDate.now();
                for (LocalDate d = finalFinalStartDate; !d.isAfter(today); d = d.plusDays(1)) {
                    if (!eventMap.containsKey(d)) {
                        eventMap.put(d, false);
                    }
                }
            }

            runOnUiThread(() -> {
                CalendarAdapter calendarAdapter = new CalendarAdapter(
                        daysInMonth,
                        eventMap,
                        eventCounts,
                        clickedDate -> {
                            // Klik na dan z dogodki
                            new Thread(() -> {
                                long start = clickedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                                long end = clickedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                                List<HabitEvent> events = AppDatabase.getInstance(this).habitEventDao().getEventsBetween(start, end);

                                runOnUiThread(() -> {
                                    if (!events.isEmpty()) {
                                        Collections.sort(events, Comparator.comparingLong(e -> e.timestamp));
                                        showEventListDialog(clickedDate, events);
                                    }
                                });
                            }).start();
                        },
                        emptyDate -> {
                            // Klik na prazen dan
                            if(finalFinalStartDate != null &&
                            !emptyDate.isBefore(finalFinalStartDate) &&
                            !emptyDate.isAfter(LocalDate.now())) {

                                Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
                                SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());

                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR, emptyDate.getYear());
                                calendar.set(Calendar.MONTH, emptyDate.getMonthValue() - 1); // 0-based
                                calendar.set(Calendar.DAY_OF_MONTH, emptyDate.getDayOfMonth());

                                String prefilled = sdf.format(calendar.getTime());
                                intent.putExtra("prefill_datetime", prefilled);
                                startActivityForResult(intent, 1);
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Vnose lahko dodajate samo za dneve z označenimi ognji.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );

                calendarRecyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 7));
                calendarRecyclerView.setAdapter(calendarAdapter);
            });

        }).start();
    }

    private void showEventListDialog(LocalDate date, List<HabitEvent> events) {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
            currentDialog = null;
        }

        isEventDialogVisible = true;  // označi da je odprt
        currentDialogDate = date;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        EventListAdapter adapter = new EventListAdapter(this, events);

        // Naslov z gumbom
        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(32, 32, 32, 16);

        TextView titleText = new TextView(this);
        titleText.setText("Dogodki za " + date.getDayOfMonth() + ". " + date.getMonthValue() + ". " + date.getYear());
        titleText.setTextSize(20);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView addIcon = new ImageView(this);
        addIcon.setImageResource(android.R.drawable.ic_input_add);
        addIcon.setPadding(16, 0, 0, 0);

        // Barva ikone (colorPrimary)
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, value, true);
        addIcon.setColorFilter(value.data);

        addIcon.setOnClickListener(v -> {
            isEventDialogVisible = true;
            currentDialogDate = date;

            LocalTime now = LocalTime.now();
            String hourMin = String.format(Locale.getDefault(), "%02d:%02d", now.getHour(), now.getMinute());
            String prefillDate = date.getDayOfMonth() + ". " + date.getMonthValue() + ". " + date.getYear() + " " + hourMin;

            Intent intent = new Intent(MainActivity.this, AddEntryActivity.class);
            intent.putExtra("prefill_datetime", prefillDate);
            startActivityForResult(intent, 1);
        });


        titleLayout.addView(titleText);
        titleLayout.addView(addIcon);
        builder.setCustomTitle(titleLayout);

        builder.setAdapter(adapter, (dialog, which) -> {
            HabitEvent selected = events.get(which);
            Intent intent = new Intent(MainActivity.this, EditEntryActivity.class);
            intent.putExtra("event_id", selected.id);
            startActivityForResult(intent, 2);
        });

        builder.setNegativeButton("Zapri", null);

        currentDialog = builder.create();
        currentDialog.setOnDismissListener(d -> isEventDialogVisible = false);  // reset zastavice
        currentDialog.show();
    }




    private ArrayList<LocalDate> daysInMonthArray(LocalDate date) {
        ArrayList<LocalDate> daysInMonthArray = new ArrayList<>();

        YearMonth yearMonth = YearMonth.from(date);
        LocalDate firstOfMonth = selectedDate.withDayOfMonth(1);

        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        int startOffset = dayOfWeek - 1;

        for (int i = 1; i <= 42; i++) {
            if (i <= startOffset || i > yearMonth.lengthOfMonth() + startOffset) {
                daysInMonthArray.add(null);
            } else {
                daysInMonthArray.add(yearMonth.atDay(i - startOffset));
            }
        }
        return daysInMonthArray;
    }

    private String monthYearFromDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("sl"));
        return date.format(formatter);
    }

    private String capitalizeMonth(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private void updateProgressUI() {
        new Thread(() -> {
            List<HabitEvent> badEvents = AppDatabase.getInstance(this).habitEventDao().getAllBad();

            runOnUiThread(() -> {
                if (!badEvents.isEmpty()) {
                    HabitEvent lastBad = badEvents.get(0);
                    long millisSince = System.currentTimeMillis() - lastBad.timestamp;

                    float days = millisSince / (1000f * 60 * 60 * 24); // delni dnevi
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(millisSince);
                    long hours = TimeUnit.MILLISECONDS.toHours(millisSince);
                    long fullDays = TimeUnit.MILLISECONDS.toDays(millisSince);

                    // Formatiran prikaz časa
                    String displayTime;
                    if (minutes < 1) {
                        displayTime = "1 minuta";
                    } else if (minutes < 60) {
                        displayTime = minutes + (minutes == 1 ? " minuta" : (minutes == 2 ? " minuti" : (minutes < 5 ? " minute" : " minut")));
                    } else if (hours < 24) {
                        displayTime = hours + (hours == 1 ? " ura" : (hours == 2 ? " uri" : (hours < 5 ? " ure" : " ur")));
                    } else if (fullDays < 30) {
                        displayTime = fullDays + (fullDays == 1 ? " dan" : (fullDays == 2 ? " dneva" : (fullDays < 5 ? " dnevi" : " dni")));
                    } else if (fullDays < 365) {
                        long months = fullDays / 30;
                        long remainingDays = fullDays % 30;
                        displayTime = months + (months == 1 ? " mesec" : (months == 2 ? " meseca" : (months < 5 ? " meseci" : " mesecev")));
                        if (remainingDays > 0) {
                            displayTime += "\n" + remainingDays + (remainingDays == 1 ? " dan" : (remainingDays == 2 ? " dneva" : (remainingDays < 5 ? " dnevi" : " dni")));
                        }
                    } else {
                        long years = fullDays / 365;
                        long remainingMonths = (fullDays % 365) / 30;
                        displayTime = years + (years == 1 ? " leto" : (years == 2 ? " leti" : (years < 5 ? " leta" : " let")));
                        if (remainingMonths > 0) {
                            displayTime += "\n" + remainingMonths + (remainingMonths == 1 ? " mesec" : (remainingMonths == 2 ? " meseca" : (remainingMonths < 5 ? " meseci" : " mesecev")));
                        }
                    }

                    textDays.setText(displayTime);
                    textDays.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                    long nextGoal = getNextMilestone((long) days);
                    int scaleFactor = 1000; // omogoča tri decimalke natančnosti
                    int scaledMax = (int) (nextGoal * scaleFactor);
                    int scaledProgress = (int) Math.min(days * scaleFactor, scaledMax);
                    int percent = (int) ((scaledProgress / (float) scaledMax) * 100);

                    progressBar.setMax(scaledMax);
                    progressBar.setProgress(scaledProgress);

                    String milestoneText;
                    if (nextGoal < 7) {
                        milestoneText = nextGoal + (nextGoal == 1 ? " dan" : (nextGoal == 2 ? " dneva" : (nextGoal < 5 ? " dnevi" : " dni")));
                    } else if (nextGoal < 30) {
                        milestoneText = (nextGoal / 7) + " ted" + ((nextGoal / 7) == 1 ? "en" : ((nextGoal / 7) == 2 ? "na" : "ni"));
                    } else if (nextGoal < 365) {
                        milestoneText = (nextGoal / 30) + " mese" + ((nextGoal / 30) == 1 ? "c" : ((nextGoal / 30) == 2 ? "ca" : ((nextGoal / 30) < 5 ? "ci" : "cev")));
                    } else {
                        milestoneText = (nextGoal / 365) + " let" + ((nextGoal / 365) == 1 ? "o" : ((nextGoal / 365) == 2 ? "i" : ((nextGoal / 365) < 5 ? "a" :"")));
                    }

                    progressLabel.setText(percent + " % do cilja: " + milestoneText);

                } else {
                    textDays.setText("Ni zabeleženih prekrškov");
                    textDays.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    progressBar.setMax(1000);
                    progressBar.setProgress(0);
                    progressLabel.setText("0 % do prvega cilja");
                }
            });
        }).start();
    }

    private void loadEventsForDate(LocalDate date) {
        new Thread(() -> {
            long start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

            List<HabitEvent> updatedEvents = AppDatabase.getInstance(this)
                    .habitEventDao().getEventsBetween(start, end);

            // Sortiranje po uri
            updatedEvents.sort(Comparator.comparingLong(e -> e.timestamp));

            runOnUiThread(() -> showEventListDialog(date, updatedEvents));
        }).start();
    }


    private long getNextMilestone(long days) {
        for (long milestone : MILESTONES_DAYS) {
            if (days < milestone) return milestone;
        }
        return MILESTONES_DAYS[MILESTONES_DAYS.length - 1];
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (isEventDialogVisible && currentDialogDate != null) {
                // Osveži dogodke za isti dan (ker je bil odprt dialog)
                LocalDate date = currentDialogDate;
                new Thread(() -> {
                    long start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    List<HabitEvent> events = AppDatabase.getInstance(this).habitEventDao().getEventsBetween(start, end);
                    events.sort(Comparator.comparingLong(e -> e.timestamp));

                    runOnUiThread(() -> showEventListDialog(date, events));
                }).start();
            } else {
                // Če dialog ni bil odprt, samo osveži pogled
                setMonthView();
            }

            updateProgressUI(); // posodobi števec in progress bar
        }

        // Po vsakem vračanju iz activityja vedno ponastavi:
        isEventDialogVisible = false;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Ko uporabnik klikne na gumb, odpri restart dialog
            showRestartDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRestartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Potrditev")
                .setMessage("S tem dejanjem izbrišete vse podatke in začnete slediti novi navadi. Preusmerjeni boste na začetni zaslon.\nAli ste prepričani, da želite nadaljevati?")
                .setPositiveButton("Da", (dialogInterface, i) -> {
                    // Brisanje podatkov iz SharedPreferences
                    SharedPreferences preferences = getSharedPreferences("tracker_data", MODE_PRIVATE);
                    preferences.edit().clear().apply();

                    // Brisanje podatkov iz Room baze
                    new Thread(() -> {
                        AppDatabase.getInstance(MainActivity.this).habitEventDao().deleteAll();
                    }).start();

                    // Preusmeritev na IntroActivity
                    Intent intent = new Intent(MainActivity.this, IntroActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }
}