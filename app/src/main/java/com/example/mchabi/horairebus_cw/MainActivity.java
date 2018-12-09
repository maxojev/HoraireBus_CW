package com.example.mchabi.horairebus_cw;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends AppCompatActivity {
    DatabaseHelper databaseHelper;
    private String exportPath = "";
    private ProgressDialog mProgressDialog;

    public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
    public static Button btn_telecharger;
    public static Button btn_lister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        btn_telecharger = (Button)findViewById(R.id.btn_telecharger);
        btn_lister = (Button)findViewById(R.id.btn_liste);
        btn_lister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,ListeBus.class));
            }
        });
        btn_telecharger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadDezip("http://ftp.keolis-rennes.com/opendata/tco-busmetro-horaires-gtfs-versions-td/attachments/GTFS_2018.3.0.2_2018-11-26_2018-12-23.zip");
            }
        });


    }
    /**
     * Progresse Bar
     *
     * @param id
     * @return
     */
    @Override
    protected ProgressDialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_PROGRESS: //we set this to 0
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Downloading file…");
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(100);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(true);
                mProgressDialog.show();
                return mProgressDialog;
            default:
                return null;
        }
    }

    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void downloadDezip(String zipFileUrl) {
        final String sourceFilname = "" + zipFileUrl;
        AsyncHttpClient client = new AsyncHttpClient();
        String[] allowedType = {
                "application/zip","application/octect-stream"
        };
        client.get(sourceFilname, new BinaryHttpResponseHandler(allowedType) {

            @Override
            public void onStart() {
                super.onStart();
                showDialog(DIALOG_DOWNLOAD_PROGRESS);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] binaryData) {

                Log.e("STARX", "success start");

                try {
                    //Splitting a File Name from SourceFileName
                    String DestinationName = sourceFilname.substring(sourceFilname.lastIndexOf('/') + 1, sourceFilname.length());
                    String INIT_FOLDER_PATH = "star1dm/";

                    File DEVICE_ROOT_FOLDER = getExternalStorageDirectory();
                    File file = new File((DEVICE_ROOT_FOLDER + "/" + INIT_FOLDER_PATH));

                    if (! file.exists()){
                        file.mkdir();
                    }

                    DatabaseHelper.INIT_FOLDER_PATH = INIT_FOLDER_PATH + DestinationName.substring(0, DestinationName.lastIndexOf(".")) + "/";
                    //Saving a File into Download Folder


                    File _f = new File(file, DestinationName);

                    FileOutputStream output = new FileOutputStream(_f);

                    Log.e("STARX", "success try");
                    output.write(binaryData);
                    output.close();
                    Log.e("STARX", "" + _f);

                    // Debut du deziping
                    exportPath = _f.getAbsolutePath();
                    exportPath = exportPath.replace(".zip", "");
                    Log.e("STARX", "==> " + exportPath);

                    DatabaseHelper.DOWNLOAD_PATH = exportPath;
                    exportPath = exportPath + "/";

                    // decropress file in folder whith id name
                    DecompressFast df = new DecompressFast(_f.getAbsolutePath(), exportPath);
                    df.unzip();

                    /**
                     * Inserer les données télechargées
                     */
                    DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
                    databaseHelper.insertAll();
                    dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
                    mProgressDialog.dismiss();

                } catch (IOException e) {
                    Log.e("STARX", "success catch");
                    e.printStackTrace();
                }
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                int val = (int) ((bytesWritten * 100) / totalSize);
                Log.d("STARX", "downloading ..... " + val);
                mProgressDialog.setProgress(val);
                mProgressDialog.getCurrentFocus();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] binaryData, Throwable error) {

                Log.e("STARX", "==> " + error);

            }

            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }
}
