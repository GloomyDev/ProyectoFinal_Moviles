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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalTime;

public class EventEditActivity extends AppCompatActivity {
    private EditText eventNameET, frecuenciaET, dosisET, comentariosET;
    private TextView eventDateTV, eventTimeTV;
    private Button captureFotoEmpaqueBtn, captureFotoPastillaBtn, saveEventBtn;
    private ImageView fotoEmpaqueIV, fotoPastillaIV;
    private LocalTime time;
    private String medicationPackPhotoPath = "";
    private String pillPhotoPath = "";
    private static final int REQUEST_IMAGE_CAPTURE_EMPAQUE = 1;
    private static final int REQUEST_IMAGE_CAPTURE_PASTILLA = 2;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_edit);
        initWidgets();
        time = LocalTime.now();
//        eventDateTV.setText("Date: " + CalendarUtils.formattedDate(CalendarUtils.selectedDate));
//        eventTimeTV.setText("Time: " + CalendarUtils.formattedTime(time));

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
    }

    private void initWidgets() {
        eventNameET = findViewById(R.id.nombreMedicamentoET);
        frecuenciaET = findViewById(R.id.frecuenciaET);
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
        Event newEvent = new Event(eventName, CalendarUtils.selectedDate, time, frequency, dosage, comments, medicationPackPhotoPath, pillPhotoPath);

        // Guardar el evento en la base de datos
        new InsertEventTask().execute(newEvent);
        finish();
    }

    public void captureFotoEmpaque(View view) {
        // Verificar si se tiene el permiso CAMERA
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Si no se tiene el permiso, solicitarlo al usuario
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Si se tiene el permiso, abrir la cámara
            openCamera(REQUEST_IMAGE_CAPTURE_EMPAQUE);
        }
    }

    public void captureFotoPastilla(View view) {
        // Verificar si se tiene el permiso CAMERA
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Si no se tiene el permiso, solicitarlo al usuario
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Si se tiene el permiso, abrir la cámara
            openCamera(REQUEST_IMAGE_CAPTURE_PASTILLA);
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
        if (requestCode == REQUEST_IMAGE_CAPTURE_EMPAQUE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Guardar la imagen en el almacenamiento del dispositivo y obtener la ruta de la imagen
            // medicationPackPhotoPath = saveImageAndGetPath(imageBitmap);
            fotoEmpaqueIV.setImageBitmap(imageBitmap);
        } else if (requestCode == REQUEST_IMAGE_CAPTURE_PASTILLA && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            // Guardar la imagen en el almacenamiento del dispositivo y obtener la ruta de la imagen
            // pillPhotoPath = saveImageAndGetPath(imageBitmap);
            fotoPastillaIV.setImageBitmap(imageBitmap);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario concede el permiso, abrir la cámara
                openCamera(requestCode == REQUEST_IMAGE_CAPTURE_EMPAQUE ? REQUEST_IMAGE_CAPTURE_EMPAQUE : REQUEST_IMAGE_CAPTURE_PASTILLA);
            } else {
                // Si el usuario deniega el permiso, mostrar un mensaje o tomar alguna otra acción
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class InsertEventTask extends AsyncTask<Event, Void, Void> {
        @Override
        protected Void doInBackground(Event... events) {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.eventDao().insert(events[0]);
            return null;
        }
    }
}
