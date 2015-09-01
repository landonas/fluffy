package edu.ucsc.fluffy;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        sb = (SeekBar) findViewById(R.id.seekBar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "Pain: " + Float.toString(painLevel));
                long cur_time = System.currentTimeMillis();
                p.setPain(cur_time, painLevel);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                painLevel = progress / 10.0f;
            }
        });
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();

        // we need at least a patient ID to start
        if (patientID == 0)
            showPatientIDDialog();
    }

    public void finishPatient(View view) {
        if (f != null && p != null) {
            Log.i(TAG, "Saved " + f.getAbsolutePath());
            p.serialize(f.getAbsolutePath());
            Intent intent = new Intent(this, FileManager.class);
            ArrayList<String> files = new ArrayList<String>();
            files.add(f.getAbsolutePath());
            intent.putStringArrayListExtra("patientFile", files);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_patient, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
