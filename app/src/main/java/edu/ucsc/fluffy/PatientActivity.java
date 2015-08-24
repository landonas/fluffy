package edu.ucsc.fluffy;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;

public class PatientActivity extends ActionBarActivity
        implements PatientDialogFragment.PatientIDInterface {
    final private String TAG = PatientActivity.class.getSimpleName();

    private int patientID = 0;
    private Patient p=null;
    private File f=null;

    @Override
    public void loadOrCreatePatient(int id) {
        if (id == 0)
            showPatientIDDialog();

        patientID = id;
        f = new File(getApplicationContext().getExternalFilesDir(null), "patient_" + patientID + ".ser");

        // The file should always exist since we at least need a patient ID and that saved the file
        if (f.exists()) {
            Log.i(TAG, "Opening file" + f.getAbsolutePath());
            p = Patient.deserialize(f.getAbsolutePath(),patientID);
        } else {
            Log.i(TAG, "Creating new patient..." + patientID);
            p = new Patient(patientID);
        }
    }

    public void showPatientIDDialog() {
        // Create an instance of the dialog fragment and show it
        PatientDialogFragment dialog = new PatientDialogFragment();
        dialog.show(getSupportFragmentManager(), "PatientDialogFragment");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        // prompt for a patient ID
        if (p==null) {
            p = new Patient(patientID);
            f = new File(getApplicationContext().getExternalFilesDir(null),"patient_"+patientID+".ser" );
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
}
