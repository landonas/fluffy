package edu.ucsc.fluffy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class PatientActivity extends ActionBarActivity
        implements PatientDialogFragment.PatientIDInterface {
    final private String TAG = PatientActivity.class.getSimpleName();

    static final int LOAD_UPDATED_PATIENT = 1;

    private int patientID;
    private Patient p = null;
    private File f = null;
    private Float painLevel;
    private SeekBar sb;

    private long lastTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Log.i(TAG, "Height x Width: " + dm.widthPixels + "," + dm.xdpi + "," + dm.heightPixels + "," + dm.ydpi);
        // we need a device that has 10cm width for this app
        if (dm.widthPixels/dm.xdpi < 3.9371)
            Toast.makeText(this, "Warning: Display is required to be more than 10cm.", Toast.LENGTH_LONG).show();

        // we want it to be 10cm (3.9371 inches), so convert to pixels
        double pixelWidth = 3.9371 * dm.xdpi;

        sb = (SeekBar) findViewById(R.id.seekBar);
        View sb_view = (View)findViewById(R.id.seekBar);
        LayoutParams params=sb_view.getLayoutParams();
        params.width=(int)pixelWidth;
        sb_view.setLayoutParams(params);


        sb.setProgressDrawable(getResources().getDrawable(R.drawable.seek_bar));

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // always set the pain level when they stop touching
                //Log.i(TAG, "Pain: " + Float.toString(painLevel));
                long curTime = System.currentTimeMillis();
                p.setPain(curTime, painLevel);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                long curTime = System.currentTimeMillis();
                painLevel = progress / 10.0f;
                // only set the pain level every 1 second approximately while they are touching
                if (lastTime == 0 || (curTime - lastTime) > 1000) {
                    p.setPain(curTime, painLevel);
                    lastTime = curTime;
                    //Log.i(TAG, "Progress: " + Float.toString(painLevel));
                }


            }
        });


    }


    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();

        finishPatient();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                        //| View.SYSTEM_UI_FLAG_FULLSCREEN
                       // | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();

        // we need at least a patient ID to start
        if (patientID == 0)
            showPatientIDDialog();

    }

    private void finishPatient() {
        if (f != null && p != null) {
            Log.i(TAG, "Saved " + f.getAbsolutePath());
            p.serialize(f.getAbsolutePath());
            Intent intent = new Intent(this, FileManager.class);
            ArrayList<String> files = new ArrayList<String>();
            files.add(f.getAbsolutePath());
            intent.putStringArrayListExtra("patientFile", files);
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_patient, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_email:
                intent = new Intent(this, FileManager.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(this, Preferences.class);
                startActivity(intent);
                return true;
            case R.id.action_file_manager:
                intent = new Intent(this, FileManager.class);
                startActivity(intent);
                return true;
            case R.id.action_exit:
                finish();
                return true;
            case R.id.action_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showPatientIDDialog() {
        // Create an instance of the dialog fragment and show it
        PatientDialogFragment dialog = new PatientDialogFragment();
        dialog.show(getSupportFragmentManager(), "PatientDialogFragment");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == LOAD_UPDATED_PATIENT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // restore the patient so that we have the new patient data
                loadOrCreatePatient(patientID);


            }
        }
    }

    @Override
    public void loadOrCreatePatient(int id) {
        if (id == 0)
            showPatientIDDialog();

        patientID = id;
        // always create a new one since this is not editable
        Log.i(TAG, "Creating new patient..." + patientID);
        f = new File(getApplicationContext().getExternalFilesDir(null), "patient_" + patientID + ".ser");
        p = new Patient(patientID);
    }
}
