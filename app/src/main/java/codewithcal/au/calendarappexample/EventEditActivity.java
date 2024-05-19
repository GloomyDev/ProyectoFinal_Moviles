package codewithcal.au.calendarappexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalTime;

public class EventEditActivity extends AppCompatActivity {
    private EditText eventNameET, frecuenciaET, dosisET, comentariosET;
    private CalendarView calendarView;
    private Button captureFotoEmpaqueBtn, captureFotoPastillaBtn, saveEventBtn;
    private ImageView fotoEmpaqueIV, fotoPastillaIV;
    private LocalTime time;
    private String medicationPackPhotoPath = "";
    private String pillPhotoPath = "";
    private LocalDate endDate = LocalDate.now(); // Default to today
    private static final int REQUEST_IMAGE_CAPTURE_EMPAQUE = 1;
    private static final int REQUEST_IMAGE_CAPTURE_PASTILLA = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_edit);
        initWidgets();
        time = LocalTime.now();

        captureFotoEmpaqueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFotoEmpaque(v);
            }
        });

        captureFotoPastillaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureFotoPastilla(v);
            }
        });

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                endDate = LocalDate.of(year, month + 1, dayOfMonth);
            }
        });

        saveEventBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEventAction(v);
            }
        });
    }

    private void initWidgets() {
        eventNameET = findViewById(R.id.nombreMedicamentoET);
        frecuenciaET = findViewById(R.id.frecuenciaET);
        calendarView = findViewById(R.id.calendarView);
        dosisET = findViewById(R.id.dosisET);
        comentariosET = findViewById(R.id.comentariosET);
        captureFotoEmpaqueBtn = findViewById(R.id.captureFotoEmpaqueBtn);
        captureFotoPastillaBtn = findViewById(R.id.captureFotoPastillaBtn);
        fotoEmpaqueIV = findViewById(R.id.fotoEmpaqueIV);
        fotoPastillaIV = findViewById(R.id.fotoPastillaIV);
        saveEventBtn = findViewById(R.id.saveEventBtn);
    }

    public void saveEventAction(View view) {
        String eventName = eventNameET.getText().toString();
        int frequency = Integer.parseInt(frecuenciaET.getText().toString());
        int dosage = Integer.parseInt(dosisET.getText().toString());
        String comments = comentariosET.getText().toString();

        // Crear un nuevo objeto Event con los datos ingresados por el usuario
        Event newEvent = new Event(eventName, CalendarUtils.selectedDate, endDate, time, frequency, dosage, comments, medicationPackPhotoPath, pillPhotoPath);

        // Guardar el evento en la base de datos
        new InsertEventTask().execute(newEvent);
        finish();
    }

    public void captureFotoEmpaque(View view) {
        requestCameraPermission(REQUEST_IMAGE_CAPTURE_EMPAQUE);
    }

    public void captureFotoPastilla(View view) {
        requestCameraPermission(REQUEST_IMAGE_CAPTURE_PASTILLA);
    }

    private void requestCameraPermission(int requestCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            openCamera(requestCode);
        }
    }

    private void openCamera(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            if (requestCode == REQUEST_IMAGE_CAPTURE_EMPAQUE) {
                fotoEmpaqueIV.setImageBitmap(imageBitmap);
                // medicationPackPhotoPath = saveImageAndGetPath(imageBitmap);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE_PASTILLA) {
                fotoPastillaIV.setImageBitmap(imageBitmap);
                // pillPhotoPath = saveImageAndGetPath(imageBitmap);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(REQUEST_IMAGE_CAPTURE_EMPAQUE);
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class InsertEventTask extends AsyncTask<Event, Void, Void> {
        @Override
        protected Void doInBackground(Event... events) {
            Event originalEvent = events[0];
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            db.eventDao().insert(originalEvent);

            LocalDate startDate = originalEvent.getDate();
            int frequency = originalEvent.getFrequency();
            LocalDate finalEndDate = endDate;

            LocalDate nextDate = startDate.plusDays(frequency);
            while (!nextDate.isAfter(finalEndDate)) {
                Event recurringEvent = new Event(
                        originalEvent.getName(),
                        nextDate,
                        endDate,
                        originalEvent.getTime(),
                        originalEvent.getFrequency(),
                        originalEvent.getDosage(),
                        originalEvent.getComments(),
                        originalEvent.getMedicationPackPhoto(),
                        originalEvent.getPillPhoto()
                );
                db.eventDao().insert(recurringEvent);
                nextDate = nextDate.plusDays(frequency);
            }

            return null;
        }
    }
}
