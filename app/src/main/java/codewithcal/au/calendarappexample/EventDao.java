package codewithcal.au.calendarappexample;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.time.LocalDate;
import java.util.List;

@Dao
public interface EventDao {
    @Insert
    void insert(Event event);

    @Query("SELECT * FROM events WHERE date = :date")
    List<Event> getEventsForDate(LocalDate date);

    @Query("SELECT * FROM events")
    List<Event> getAllEvents();
}
