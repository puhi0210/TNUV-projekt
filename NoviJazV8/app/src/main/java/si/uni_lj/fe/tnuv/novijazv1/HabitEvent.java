package si.uni_lj.fe.tnuv.novijazv1;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class HabitEvent {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public boolean isGood;
    public long timestamp;
    public String note;
}
