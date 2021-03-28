package com.example.smartphoneorientation;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private static final int REQUEST_CAMERA_PERMISSION_RESULT =5643 ;
    private Context mContext;

    private static final int REQUEST_VIDEO_CAPTURE = 00111;
    private static final int REQUEST_LOCATION = 01010;

    private static final String TAG = "MainActivity";
    private SensorManager sensorManager;
    private Sensor accelerometer, mGyro;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double mLatitude;
    private double mLongitude;
    private int REQUEST_CODE = 1;
    DatabaseHelper myDB;

    CheckBox mCheckBox;

    TextView xValue, yValue, zValue, xGyroValue, yGyroValue, zGyroValue, latitudeTextView, longitudeTextView;
    Button startButton;
    Button stopButton;
    Button exportButton;

    private Uri fileUri;
    private Uri mImageUri;
    private static String IMAGE_DIRECTORY_NAME = "codesss";
    private static String filePath;
    private File mDocFolder;
    private String mVideoFileName;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_LOCATION)
        {

            if(grantResults.length> 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                startListening();
            }
        }
    }

    public void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        }
    }


    public void updateLocation(Location location)
    {

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();

        latitudeTextView.setText("Latitude : " + mLatitude );
        longitudeTextView.setText("Longitude : " + mLongitude );

        Log.i("Latitude found", String.valueOf(mLatitude));
        Log.i("Longitude found", String.valueOf(mLongitude));
        Log.i("Location : ",location.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mContext = MainActivity.this;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            OnGPS();
        }



        myDB = new DatabaseHelper(this);
        //---------------- Acclerometer ---------------//
        xValue = findViewById(R.id.xValue);
        yValue = findViewById(R.id.yValue);
        zValue = findViewById(R.id.zValue);
        //------------------ Gyro Sensor ----------------//

        xGyroValue = (TextView) findViewById(R.id.xGyroValue);
        yGyroValue = (TextView) findViewById(R.id.yGyroValue);
        zGyroValue = (TextView) findViewById(R.id.zGyroValue);
        exportButton = (Button) findViewById(R.id.btn_export);

        //----------- GPS Cordinates -------------------------//
        latitudeTextView = (TextView) findViewById(R.id.latitiude);
        longitudeTextView = (TextView) findViewById(R.id.longitude);

        latitudeTextView.setVisibility(View.INVISIBLE);
        longitudeTextView.setVisibility(View.INVISIBLE);
        xValue.setVisibility(View.INVISIBLE);
        yValue.setVisibility(View.INVISIBLE);
        zValue.setVisibility(View.INVISIBLE);
        xGyroValue.setVisibility(View.INVISIBLE);
        yGyroValue.setVisibility(View.INVISIBLE);
        zGyroValue.setVisibility(View.INVISIBLE);

        mCheckBox = (CheckBox) findViewById(R.id.checkbox);


        startButton = findViewById(R.id.btn_start);
        stopButton = findViewById(R.id.btn_stop);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        exportButton.setOnClickListener(this);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location)
            {
                updateLocation(location);
                Log.i("Latitude found", String.valueOf(mLatitude));

            }
        };

        if(ContextCompat.checkSelfPermission(MainActivity.this , Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION);
        }
        else
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                updateLocation(lastKnownLocation);
            }
        }


        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;


        float aX = 0, aY = 0, aZ = 0, gX = 0, gY = 0, gZ = 0;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d(TAG, "onSensorChanged: X: " + sensorEvent.values[0] + "y: " + sensorEvent.values[1] + "Z: " + sensorEvent.values[2]);
            xValue.setText("xValue: " + sensorEvent.values[0]);
            yValue.setText("yValue: " + sensorEvent.values[1]);
            zValue.setText("zValue: " + sensorEvent.values[2]);
            aX = sensorEvent.values[0];
            aY = sensorEvent.values[1];
            aZ = sensorEvent.values[2];
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            xGyroValue.setText("xGyroValue: " + sensorEvent.values[0]);
            yGyroValue.setText("yGyroValue: " + sensorEvent.values[1]);
            zGyroValue.setText("zGyroValue: " + sensorEvent.values[2]);
            gX = sensorEvent.values[0];
            gY = sensorEvent.values[1];
            gZ = sensorEvent.values[2];
        }
        synchronized (locationListener){
            locationListener.notify();
        }


        myDB.saveDimensions(aX, aY, aZ, gX, gY, gZ, mLatitude, mLongitude);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.btn_start:

                myDB.delete();



                if (mCheckBox.isChecked()){

                        dispatchTakeVideoIntent();

                }else{
                    latitudeTextView.setVisibility(View.VISIBLE);
                    longitudeTextView.setVisibility(View.VISIBLE);
                    xValue.setVisibility(View.VISIBLE);
                    yValue.setVisibility(View.VISIBLE);
                    zValue.setVisibility(View.VISIBLE);
                    xGyroValue.setVisibility(View.VISIBLE);
                    yGyroValue.setVisibility(View.VISIBLE);
                    zGyroValue.setVisibility(View.VISIBLE );

                    startData();

                }
                break;

            case R.id.btn_stop:

                Toast.makeText(getApplicationContext(), "Sensor Stopped", Toast.LENGTH_SHORT).show();

                latitudeTextView.setVisibility(View.INVISIBLE);
                longitudeTextView.setVisibility(View.INVISIBLE);
                xValue.setVisibility(View.INVISIBLE);
                yValue.setVisibility(View.INVISIBLE);
                zValue.setVisibility(View.INVISIBLE);
                xGyroValue.setVisibility(View.INVISIBLE);
                yGyroValue.setVisibility(View.INVISIBLE);
                zGyroValue.setVisibility(View.INVISIBLE);

                if (sensorManager != null){
                    sensorManager.unregisterListener(this);
                }

//                if(locationManager != null){
//                    locationManager.removeUpdates(locationListener);
//                }


                Log.d(TAG, "onCreate: Registered acceleromer listner");

                break;

            case R.id.btn_export:
                if (isReadStoragePermissionGranted() && isWriteStoragePermissionGranted()){
                    new ExportDatabaseCSVTask().execute();
                }

                break;
        }
    }

    public void startData(){
        Toast.makeText(getApplicationContext(), "Sensor Started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "onCreate: Registered acceleromer listner");
        } else {
            xValue.setText("Accelerometer not Supported ");
            yValue.setText("Accelerometer not Supported ");
            zValue.setText("Accelerometer not Supported ");
        }
        mGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mGyro != null) {
            sensorManager.registerListener(MainActivity.this, mGyro, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "onCreate: Registered Gyro listner");
        } else {
            xGyroValue.setText("Gyroscope not Supported ");
            yGyroValue.setText("Gyroscope not Supported ");
            zGyroValue.setText("Gyroscope not Supported ");
        }
    }

    /************* FOR VIDEO INTENT **********************/

    /** Check if this device has a camera **/
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    private void dispatchTakeVideoIntent() {
//        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(takeVideoIntent,REQUEST_VIDEO_CAPTURE);
//        }

        Intent intent = new Intent(MainActivity.this,CameraActivity.class);
        startActivity(intent);
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new  DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public class ExportDatabaseCSVTask extends AsyncTask<String, Void, Boolean> {

        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        DatabaseHelper dbhelper;
        @Override
        protected void onPreExecute() {
            this.dialog.setMessage("Exporting database...");
            this.dialog.show();
            dbhelper = new DatabaseHelper(MainActivity.this);
        }

        protected Boolean doInBackground(final String... args) {

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH;mm;ss", Locale.getDefault()).format(new Date());

            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

            File exportDir = new File(root, "SmartBike");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            File file = new File(exportDir,timeStamp + ".csv");



            try {
                //File file = File.createTempFile(timeStamp,".csv",exportDir);
                file.createNewFile();
                CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                Cursor curCSV = dbhelper.raw();
                csvWrite.writeNext(curCSV.getColumnNames());
                while(curCSV.moveToNext()) {
                    String arrStr[]=null;
                    String[] mySecondStringArray = new String[curCSV.getColumnNames().length];
                    for(int i=0;i<curCSV.getColumnNames().length;i++)
                    {
                        mySecondStringArray[i] =curCSV.getString(i);
                    }
                    csvWrite.writeNext(mySecondStringArray);
                }
                csvWrite.close();

                //Uri uri = FileProvider.getUriForFile(MainActivity.this,"com.example.smartphoneorientation.fileprovider",file);

                //Uri uri = Uri.fromFile(file);
                Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider",file);
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("application/plain");
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(sendIntent,"Send Report"));
                //Toast.makeText(mContext, "Saved : "+ exportDir.getAbsolutePath(), Toast.LENGTH_SHORT).show();


                return true;
            } catch (Exception e) {
                Log.i("expception here","is");
                Log.i("exception is here",e.getMessage());
                return false;
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (this.dialog.isShowing()) { this.dialog.dismiss(); }



            if (success) {
                Toast.makeText(MainActivity.this, "Export successful!", Toast.LENGTH_SHORT).show();

                //ShareGif();
            } else {
                Toast.makeText(MainActivity.this, "Export failed", Toast.LENGTH_SHORT).show();

            }
        }
    }


//    private void sendFile(){
//
//        sendIntent.setType("application/csv");
//
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
//        File mediaFile;
//
//        mediaFile = new File(exportDir.getPath() + File.separator+ "person_" + timeStamp + ".csv");
//
//        Uri U = Uri.fromFile(mediaFile);
//
//        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
//        startActivity(Intent.createChooser(sendIntent, "Send Report"));
//
//    }





    //////////////////////////////////////////////////// Permission for read and writer files /////////////////////////////////

    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted1");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted1");
            return true;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted2");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }

//    private int requestPermissionForPhoto = 123;
//
//    private boolean checkAndRequestPermissions() {
//        int writeExternalPermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        int readExternalPermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
//
//        List<String> listPermissionsNeeded = new ArrayList<>();
//
//        if (writeExternalPermission != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        }
//
//        if (readExternalPermission != PackageManager.PERMISSION_GRANTED) {
//            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
//        }
//
//        if (!listPermissionsNeeded.isEmpty()) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), requestPermissionForPhoto);
//            }
//            return false;
//        }
//        return true;
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == requestPermissionForPhoto) {
//
//            Map<String, Integer> perms = new HashMap<>();
//            // Initialize the map with both permissions
//
//            perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
//            perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
//            // Fill with actual results from user
//
//            if (grantResults.length > 0) {
//                for (int i = 0; i < permissions.length; i++) {
//                    perms.put(permissions[i], grantResults[i]);
//                }
//
//                    // Check for both permissions
//                    if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
//                            && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                        Log.i("exprtinggg","exporting");
//                        new ExportDatabaseCSVTask().execute();
//
//                    } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
//
//                        showDialogOK("Storage Permission required for this app",
//                                new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        switch (which) {
//                                            case DialogInterface.BUTTON_POSITIVE:
//                                                checkAndRequestPermissions();
//                                                break;
//                                            case DialogInterface.BUTTON_NEGATIVE:
//                                                // proceed with logic by disabling the related features or quit the app.
//                                                break;
//                                        }
//                                    }
//                                });
//                    } else {
//                        Toast.makeText(mContext, "Go to settings and enable permissions", Toast.LENGTH_LONG).show();
//                    }
//
//            }
//        }
//    }


//    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
//        new AlertDialog.Builder(mContext)
//                .setMessage(message)
//                .setPositiveButton("OK", okListener)
//                .setNegativeButton("Cancel", okListener)
//                .create()
//                .show();
//    }

//    /**
//     * Creating file uri to store image/video
//     */
//    public Uri getOutputMediaFileUri(int type) {
//        Uri photoUri = Uri.fromFile(getOutputMediaFile());
//        mImageUri = photoUri;
//        return photoUri;
//    }
//
//    private static File getOutputMediaFile() {
//
//        // External sdcard location
//        File mediaStorageDir = new File(
//                Environment
//                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
//                IMAGE_DIRECTORY_NAME);
//
//        // Create the storage directory if it does not exist
//        if (!mediaStorageDir.exists()) {
//            if (!mediaStorageDir.mkdirs()) {
//                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
//                        + IMAGE_DIRECTORY_NAME + " directory");
//                return null;
//            }
//        }
//
//        // Create a media file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
//                Locale.getDefault()).format(new Date());
//        File mediaFile;
//
//        mediaFile = new File(mediaStorageDir.getPath() + File.separator
//                + "person_" + timeStamp + ".csv");
//
//        filePath = mediaFile.getAbsolutePath();
//
//
//        return mediaFile;
//    }
//
//    public Uri getOutputMediaFileUri2(int type) {
////        return Uri.fromFile(getOutputMediaFile(type));
//
//
//
//        Uri photoUri;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
//            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
//                    Locale.getDefault()).format(new Date());
//
//            ContentResolver resolver = mContext.getContentResolver();
//            ContentValues contentValues = new ContentValues();
//            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "person_" + timeStamp);
//            contentValues.put(MediaStore.Files.FileColumns.MIME_TYPE, "text/*");
//            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + IMAGE_DIRECTORY_NAME );
//
//
//            photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//            photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//        } else {
//            File newFile = getOutputMediaFile();
//            Log.e("MyPath", BuildConfig.APPLICATION_ID);
//            photoUri = FileProvider.getUriForFile(mContext,
//                    BuildConfig.APPLICATION_ID + ".provider",
//                    newFile);
//        }
//        mImageUri = photoUri;
//        return photoUri;
//    }

}

