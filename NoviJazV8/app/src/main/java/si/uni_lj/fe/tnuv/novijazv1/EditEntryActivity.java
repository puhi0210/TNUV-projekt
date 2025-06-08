package si.uni_lj.fe.tnuv.novijazv1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditEntryActivity extends AppCompatActivity {

    private HabitEvent event;
    private CheckBox checkboxSuccess, checkboxFailure;
    private Button buttonSave, buttonDelete;
    private TextView selectedDateText;
    private EditText notesEditText;
    private Toolbar toolbar;
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_entry);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Uredi vnos");
        }

        checkboxSuccess = findViewById(R.id.checkboxSuccess);
        checkboxFailure = findViewById(R.id.checkboxFailure);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);
        selectedDateText = findViewById(R.id.editTextTime);
        notesEditText = findViewById(R.id.editTextNote);

        int eventId = getIntent().getIntExtra("event_id", -1);
        if (eventId == -1) {
            finish();
            return;
        }

        new Thread(() -> {
            event = AppDatabase.getInstance(this).habitEventDao().getById(eventId);
            if (event == null) {
                runOnUiThread(this::finish);
                return;
            }

            runOnUiThread(() -> {
                SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
                selectedDate = sdf.format(event.timestamp);
                selectedDateText.setText(selectedDate);
                notesEditText.setText(event.note);
                checkboxSuccess.setChecked(event.isGood);
                checkboxFailure.setChecked(!event.isGood);
                buttonSave.setBackgroundTintList(ContextCompat.getColorStateList(this,
                        event.isGood ? R.color.blue_compat : R.color.red_compat));
            });
        }).start();

        selectedDateText.setOnClickListener(v -> showDateTimePicker());

        checkboxSuccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxFailure.setChecked(false);
                buttonSave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_compat));
            }
        });

        checkboxFailure.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkboxSuccess.setChecked(false);
                buttonSave.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red_compat));
            }
        });

        buttonSave.setOnClickListener(v -> saveChanges());

        buttonDelete.setOnClickListener(v -> confirmDelete());
    }

    private void showDateTimePicker() {
        Calendar now = Calendar.getInstance();

        SharedPreferences prefs = getSharedPreferences("tracker_data", MODE_PRIVATE);
        String startDateStr = prefs.getString("start_date", null);

        Calendar startCalendar = Calendar.getInstance();
        if (startDateStr != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
                startCalendar.setTime(sdf.parse(startDateStr));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
            calendar.setTime(sdf.parse(selectedDate));
        } catch (Exception ignored) {}

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, y, m, d) -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (tv, h, min) -> {
                selectedDate = d + ". " + (m + 1) + ". " + y + " " +
                        String.format(Locale.getDefault(), "%02d:%02d", h, min);
                selectedDateText.setText(selectedDate);
            }, hour, minute, true);
            timePicker.show();
        }, year, month, day);

        datePicker.getDatePicker().setMinDate(startCalendar.getTimeInMillis());
        datePicker.getDatePicker().setMaxDate(now.getTimeInMillis());

        datePicker.show();
    }

    private void saveChanges() {
        boolean isSuccess = checkboxSuccess.isChecked();
        boolean isFailure = checkboxFailure.isChecked();

        if (!isSuccess && !isFailure) {
            Toast.makeText(this, "Izberi tip vnosa!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
            long timestamp = sdf.parse(selectedDate).getTime();
            long now = System.currentTimeMillis();

            // Preveri začetni datum iz SharedPreferences
            SharedPreferences prefs = getSharedPreferences("tracker_data", MODE_PRIVATE);
            String startDateStr = prefs.getString("start_date", null);
            if (startDateStr != null) {
                long startTimestamp = sdf.parse(startDateStr).getTime();

                if (timestamp < startTimestamp) {
                    Toast.makeText(this, "Ne moreš urediti vnosa pred začetkom navade!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (timestamp > now) {
                    Toast.makeText(this, "Ne moreš urediti vnosa v prihodnosti!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }


            event.timestamp = timestamp;
            event.isGood = isSuccess;
            event.note = notesEditText.getText().toString().trim();

            new Thread(() -> {
                AppDatabase.getInstance(this).habitEventDao().update(event);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Vnos posodobljen", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Napaka pri posodabljanju", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Izbriši vnos")
                .setMessage("Ali res želiš izbrisati ta vnos?")
                .setPositiveButton("Da", (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(this).habitEventDao().delete(event);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Vnos izbrisan", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton("Prekliči", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
