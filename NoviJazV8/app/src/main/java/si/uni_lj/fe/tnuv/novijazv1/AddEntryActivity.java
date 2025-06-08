package si.uni_lj.fe.tnuv.novijazv1;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

public class AddEntryActivity extends AppCompatActivity {

    private CheckBox checkboxSuccess, checkboxFailure;
    private Button buttonSave;
    private TextView selectedDateText;
    private Toolbar toolbar;
    private String selectedDate = "";
    private EditText editTextNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);


        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Dodaj vnos");
        }

        selectedDateText = findViewById(R.id.editTextTime);
        checkboxSuccess = findViewById(R.id.checkboxSuccess);
        checkboxFailure = findViewById(R.id.checkboxFailure);
        buttonSave = findViewById(R.id.buttonSave);
        editTextNote = findViewById(R.id.editTextNote);

        // Preveri, če je aktivnost dobila datum iz intent-a
        String passedDate = getIntent().getStringExtra("prefill_datetime");
        if (passedDate != null) {
            selectedDate = passedDate;
            selectedDateText.setText(passedDate);
        } else {
            setCurrentDateTime();
        }



        // Izbira datuma ob kliku na datum
        selectedDateText.setOnClickListener(v -> showDateTimePicker());

        // Beležka
        editTextNote = findViewById(R.id.editTextNote);

        // Nastavimo klik listener za gumb shrani
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEntry();
            }
        });

        // Poskrbimo, da je lahko izbran samo en checkbox + dinamično spreminjanje barve
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setCurrentDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
        selectedDate = dateFormat.format(calendar.getTime());
        selectedDateText.setText(selectedDate);
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
            calendar.setTime(sdf.parse(selectedDate)); // NE `now`, ampak trenutno izbran datum
        } catch (Exception ignored) {}

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Omejitve (kot v AddEntry)
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

        DatePickerDialog datePicker = new DatePickerDialog(this, (view, y, m, d) -> {
            TimePickerDialog timePicker = new TimePickerDialog(this, (tv, h, min) -> {
                selectedDate = d + ". " + (m + 1) + ". " + y + " " +
                        String.format(Locale.getDefault(), "%02d:%02d", h, min);
                selectedDateText.setText(selectedDate);
            }, hour, minute, true);
            timePicker.show();
        }, year, month, day);

        datePicker.getDatePicker().setMinDate(startCalendar.getTimeInMillis());
        datePicker.getDatePicker().setMaxDate(Calendar.getInstance().getTimeInMillis());

        datePicker.show();
    }



    private void saveEntry() {
        boolean isSuccess = checkboxSuccess.isChecked();
        boolean isFailure = checkboxFailure.isChecked();

        if (!isSuccess && !isFailure) {
            Toast.makeText(this, "Izberi tip vnosa!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
            long selectedTimestamp = sdf.parse(selectedDate).getTime();

            // Preveri začetni datum iz SharedPreferences
            long now = System.currentTimeMillis();
            String startDateStr = getSharedPreferences("tracker_data", MODE_PRIVATE)
                    .getString("start_date", null);

            if (startDateStr != null) {
                long startTimestamp = sdf.parse(startDateStr).getTime();

                if (selectedTimestamp < startTimestamp) {
                    Toast.makeText(this, "Ne moreš dodati vnosa pred začetkom navade!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedTimestamp > now) {
                    Toast.makeText(this, "Ne moreš dodati vnosa v prihodnosti!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            HabitEvent event = new HabitEvent();
            event.isGood = isSuccess;
            event.timestamp = selectedTimestamp;
            String userNote = editTextNote.getText().toString().trim();
            if (userNote.isEmpty()) {
                event.note = null;
            } else {
                event.note = userNote;
            }

            new Thread(() -> {
                AppDatabase.getInstance(this).habitEventDao().insert(event);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Vnos shranjen", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Napaka pri shranjevanju vnosa", Toast.LENGTH_SHORT).show();
        }
    }


}