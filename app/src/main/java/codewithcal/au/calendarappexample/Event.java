package codewithcal.au.calendarappexample;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity(tableName = "events")
public class Event {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private LocalDate date;
    private LocalDate end_date;
    private LocalTime time;
    private int frequency;
    private int dosage;
    private String comments;
    private String medicationPackPhoto; // Foto del empaque de la medicación
    private String pillPhoto; // Foto de la pastilla

    // Constructor, getters, and setters
    public Event(String name, LocalDate date, LocalDate end_date, LocalTime time, int frequency, int dosage, String comments, String medicationPackPhoto, String pillPhoto) {
        this.name = name;
        this.date = date;
        this.end_date = end_date;
        this.time = time;
        this.frequency = frequency;
        this.dosage = dosage;
        this.comments = comments;
        this.medicationPackPhoto = medicationPackPhoto;
        this.pillPhoto = pillPhoto;
    }

    // Getters y setters para las nuevas variables

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDate getEnd_date() {
        return end_date;
    }

    public LocalTime getTime() {
        return time;
    }
    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getDosage() {
        return dosage;
    }

    public void setDosage(int dosage) {
        this.dosage = dosage;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getMedicationPackPhoto() {
        return medicationPackPhoto;
    }

    public void setMedicationPackPhoto(String medicationPackPhoto) {
        this.medicationPackPhoto = medicationPackPhoto;
    }

    public String getPillPhoto() {
        return pillPhoto;
    }

    public void setPillPhoto(String pillPhoto) {
        this.pillPhoto = pillPhoto;
    }
}
