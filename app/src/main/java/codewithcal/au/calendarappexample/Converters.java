package codewithcal.au.calendarappexample;

import androidx.room.TypeConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Converters {
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    @TypeConverter
    public static LocalDate fromStringToDate(String dateString) {
        return dateString == null ? null : LocalDate.parse(dateString, dateFormatter);
    }

    @TypeConverter
    public static String fromDateToString(LocalDate date) {
        return date == null ? null : date.format(dateFormatter);
    }

    @TypeConverter
    public static LocalTime fromTimestampToLocalTime(Long timestamp) {
        return timestamp == null ? null : LocalTime.ofSecondOfDay(timestamp);
    }

    @TypeConverter
    public static Long fromLocalTimeToTimestamp(LocalTime time) {
        return time == null ? null : (long) time.toSecondOfDay();
    }
}
