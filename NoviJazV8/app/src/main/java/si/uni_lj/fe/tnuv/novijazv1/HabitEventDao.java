package si.uni_lj.fe.tnuv.novijazv1;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;


import java.util.List;

@Dao
public interface HabitEventDao {
    @Insert
    void insert(HabitEvent event);

    @androidx.room.Update
    void update(HabitEvent event);

    @Delete
    void delete(HabitEvent event);


    @Query("SELECT * FROM HabitEvent ORDER BY timestamp DESC")
    List<HabitEvent> getAll();

    @Query("SELECT * FROM HabitEvent WHERE isGood = 1 ORDER BY timestamp DESC")
    List<HabitEvent> getAllGood();

    @Query("SELECT * FROM HabitEvent WHERE isGood = 0 ORDER BY timestamp DESC")
    List<HabitEvent> getAllBad();

    @Query("SELECT * FROM HabitEvent WHERE timestamp BETWEEN :start AND :end")
    List<HabitEvent> getEventsBetween(long start, long end);

    @Query("SELECT * FROM HabitEvent WHERE id = :eventId LIMIT 1")
    HabitEvent getById(int eventId);

    @Query("DELETE FROM HabitEvent")
    void deleteAll();


}
