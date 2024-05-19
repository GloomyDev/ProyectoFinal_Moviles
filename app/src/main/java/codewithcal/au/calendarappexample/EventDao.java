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

    // Métodos para acceder y actualizar las nuevas variables
    @Query("UPDATE events SET frequency = :frequency, dosage = :dosage, comments = :comments, medicationPackPhoto = :medicationPackPhoto, pillPhoto = :pillPhoto WHERE id = :eventId")
    void updateEventDetails(int eventId, int frequency, int dosage, String comments, String medicationPackPhoto, String pillPhoto);

    @Query("SELECT * FROM events WHERE id = :eventId")
    Event getEventById(int eventId);
}
