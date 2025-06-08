package si.uni_lj.fe.tnuv.novijazv1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class IntroActivity extends AppCompatActivity {

    private EditText inputName;
    private TextView selectedDateText;
    private Button startBtn;
    private TextView infoBtn;

    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("tracker_data", MODE_PRIVATE);
        if (prefs.contains("dependency_name") && prefs.contains("start_date")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_intro);

        inputName = findViewById(R.id.inputName);
        selectedDateText = findViewById(R.id.selectedDate);
        startBtn = findViewById(R.id.startBtn);
        infoBtn = findViewById(R.id.infoBtn);

        setCurrentDateTime();

        selectedDateText.setOnClickListener(v -> showDateTimePicker());

        startBtn.setOnClickListener(v -> {
            String name = inputName.getText().toString().trim();
            if (!name.isEmpty() && !selectedDate.isEmpty()) {
                saveData(name, selectedDate);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Prosim vnesi slabo navado in izberi začetni datum", Toast.LENGTH_SHORT).show();
            }
        });

        infoBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Kaj je Novi Jaz?")
                    .setMessage("Novi Jaz je aplikacija, s katero uporabnik sledi svojemu napredku pri odvajanju od najrazličnejših zasvojenosti. " +
                            "Koledar omogoča enostavno dnevno beleženje vzponov in padcev, kakor tudi vizualizacijo napredka.")
                    .setPositiveButton("V redu", null)
                    .show();
        });
    }

    private void setCurrentDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
        selectedDate = dateFormat.format(calendar.getTime());
        selectedDateText.setText(selectedDate);
    }

    private void showDateTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (timeView, hourOfDay, minuteOfHour) -> {
                                selectedDate = selectedDay + ". " + (selectedMonth + 1) + ". " + selectedYear + " " +
                                        String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                                selectedDateText.setText(selectedDate);
                            }, hour, minute, true);
                    timePicker.show();
                }, year, month, day);
        datePicker.show();
    }

    private void saveData(String name, String date) {
        SharedPreferences prefs = getSharedPreferences("tracker_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("dependency_name", name);
        editor.putString("start_date", date);
        editor.apply();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d. M. yyyy HH:mm", Locale.getDefault());
            long timestamp = sdf.parse(date).getTime();

            HabitEvent startEvent = new HabitEvent();
            startEvent.isGood = false;
            startEvent.timestamp = timestamp;
            startEvent.note = "Začetek poti: " + name;

            new Thread(() -> {
                AppDatabase.getInstance(this).habitEventDao().insert(startEvent);
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Napaka pri shranjevanju datuma", Toast.LENGTH_SHORT).show();
        }
    }
}
