package si.uni_lj.fe.tnuv.novijazv1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventListAdapter extends ArrayAdapter<HabitEvent> {
    public EventListAdapter(Context context, List<HabitEvent> events) {
        super(context, 0, events);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HabitEvent event = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_event_row, parent, false);
        }

        TextView text = convertView.findViewById(R.id.eventRowText);

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(event.timestamp);
        String type = event.isGood ? "Uspeh" : "Padec";

        String note = "";
        if (event.note != null && !event.note.trim().isEmpty()) {
            note = event.note.trim();
            if (note.length() > 25) {
                note = note.substring(0, 25) + " ...";
            }
        }

        text.setText(time + " • " + type + (note.isEmpty() ? "" : " • " + note));

        int colorRes = event.isGood ? R.color.blue_compat : R.color.red_compat;
        text.setTextColor(ContextCompat.getColor(getContext(), colorRes));

        return convertView;
    }
}
