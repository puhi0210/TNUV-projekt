package si.uni_lj.fe.tnuv.novijazv1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    public interface onEmptyDayClickListener {
        void onEmptyDayClick(LocalDate date);
    }


    private final ArrayList<LocalDate> days;
    private final Map<LocalDate, Boolean> eventMap; // true = bad, false = good
    private final Map<LocalDate, Integer> eventCounts;
    private final OnDayClickListener onDayClickListener;
    private final onEmptyDayClickListener onEmptyDayClickListener;


    public CalendarAdapter(ArrayList<LocalDate> days,
                           Map<LocalDate, Boolean> eventMap,
                           Map<LocalDate, Integer> eventCounts,
                           OnDayClickListener onDayClickListener,
                           onEmptyDayClickListener onEmptyDayClickListener) {
        this.days = days;
        this.eventMap = eventMap;
        this.eventCounts = eventCounts;
        this.onDayClickListener = onDayClickListener;
        this.onEmptyDayClickListener = onEmptyDayClickListener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_cell, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        LocalDate day = days.get(position);

        if (day == null) {
            holder.dayText.setText("");
            holder.eventCount.setText("");
            holder.fireIcon.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.dayText.setText(String.valueOf(day.getDayOfMonth()));

            // Ikona ognja
            Boolean isBad = eventMap.get(day);
            if (isBad != null) {
                holder.fireIcon.setVisibility(View.VISIBLE);
                holder.fireIcon.setImageResource(
                        isBad ? R.drawable.baseline_local_fire_department_24_red
                                : R.drawable.baseline_local_fire_department_24_blue
                );
            } else {
                holder.fireIcon.setVisibility(View.GONE);
            }

            // Število dogodkov
            Integer count = eventCounts.get(day);
            if (count != null && count > 0) {
                holder.eventCount.setText(String.valueOf(count));

                // Samo dnevi z vsaj enim dogodkom reagirajo na klik
                holder.itemView.setOnClickListener(v -> {
                    if (onDayClickListener != null) {
                        onDayClickListener.onDayClick(day);
                    }
                });
            } else {
                holder.eventCount.setText("");
                holder.itemView.setOnClickListener(v -> {
                    if (onEmptyDayClickListener != null) {
                        onEmptyDayClickListener.onEmptyDayClick(day);
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        public final TextView dayText;
        public final TextView eventCount;
        public final ImageView fireIcon;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.cellDayText);
            eventCount = itemView.findViewById(R.id.eventCountText);
            fireIcon = itemView.findViewById(R.id.fireIcon);
        }
    }
}
