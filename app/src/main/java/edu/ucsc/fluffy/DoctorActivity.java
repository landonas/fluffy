package edu.ucsc.fluffy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class DoctorActivity extends ActionBarActivity
        implements PatientDialogFragment.PatientIDInterface {
    final private String TAG = DoctorActivity.class.getSimpleName();
    private ArrayList<String> procedureSteps = null;

    static final int LOAD_UPDATED_PATIENT = 1;

    private Button buttonNextStep;
    private Button buttonEditPatient;
    private TextView textNextStep;
    private TextView textCurrentStep;
    private int currentStepIndex;

    private int patientID;
    private Patient p = null;
    private File f = null;

    @Override
    public void loadOrCreatePatient(int id) {
        if (id == 0)
            showPatientIDDialog();

        patientID = id;
        TextView textID = (TextView) findViewById(R.id.tvPatient);
        textID.setText(Integer.toString(id));

        f = new File(getApplicationContext().getExternalFilesDir(null), "doctor_" + patientID + ".ser");

        // The file should always exist since we at least need a patient ID and that saved the file
        if (f.exists()) {
            Log.i(TAG, "Opening file" + f.getAbsolutePath());
            p = Patient.deserialize(f.getAbsolutePath(),patientID);

        } else {
            Log.i(TAG, "Creating new patient..." + patientID);
            p = new Patient(patientID);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);

        if (getIntent().getExtras() == null)
            Log.e(TAG,"No procedure steps available.");
        else
            procedureSteps = getIntent().getExtras().getStringArrayList(ChooserActivity.PROCEDURE_STEPS);

        textNextStep = (TextView) findViewById(R.id.textNextStep);
        textCurrentStep = (TextView) findViewById(R.id.textCurrentStep);
        currentStepIndex = 0;
        textNextStep.setText(procedureSteps.get(currentStepIndex));

    }


    public void showPatientIDDialog() {
        // Create an instance of the dialog fragment and show it
        PatientDialogFragment dialog = new PatientDialogFragment();
        dialog.show(getSupportFragmentManager(), "PatientDialogFragment");
    }




    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();

        // we need at least a patient ID to start
        if (patientID == 0)
            showPatientIDDialog();
    }


    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

    }



    @Override
    protected void onPause() {
        Log.i(TAG,"onPause");
        super.onPause();

        // save the patient in case we are in the process of updating times
        savePatient();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_doctor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;

        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_file_manager:
                intent = new Intent(this, FileManager.class);
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(this, Preferences.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    public void nextStep(View view) {
        if (patientID==0)
            showPatientIDDialog();

        // if we start a procedure clear all the data
        if (currentStepIndex==0)
            p.steps.clear();

        // mark the current step time
        long cur_time = System.currentTimeMillis();
        p.setStep(cur_time, procedureSteps.get(currentStepIndex));
        Log.i(TAG, "Current Step: " + procedureSteps.get(currentStepIndex) + "(" + currentStepIndex + ") " + cur_time);

        // procede to next step
        currentStepIndex++;

        if (currentStepIndex >= procedureSteps.size()) {
            Toast.makeText(this, "Procedure done.", Toast.LENGTH_SHORT).show();
            editPatient();

            finish();
        } else {
            String currentStep = procedureSteps.get(currentStepIndex);
            currentStep.replace("_", " ");
            textCurrentStep.setText(currentStep);

            if (currentStepIndex+1 < procedureSteps.size()) {
                currentStep = procedureSteps.get(currentStepIndex + 1);
                currentStep.replace("_", " ");
                textNextStep.setText(currentStep);
            } else
                textNextStep.setText("Finish");
        }
    }



    private void resetProcedure() {
        patientID=0;
        currentStepIndex=0;
        textNextStep.setText(procedureSteps.get(currentStepIndex));
        textCurrentStep.setText(getString(R.string.waiting));
    }

    private void savePatient() {
        if (f!=null && p!= null) {
            Log.i(TAG, "Saved " + f.getAbsolutePath());
            p.serialize(f.getAbsolutePath());
            Intent intent = new Intent(this, FileManager.class);
            ArrayList<String> files = new ArrayList<String>();
            files.add(f.getAbsolutePath());
            intent.putStringArrayListExtra("patientFile", files);
            startActivity(intent);
        }
    }

    private void editPatient() {
        // Finalize patient data
        Intent intent = new Intent(this, PatientData.class);
        intent.putExtra("patientID", patientID);
        intent.putExtra("patientFile", f.getAbsolutePath());
        startActivityForResult(intent, LOAD_UPDATED_PATIENT);
    }

    public void startEditPatient(View view) {
        editPatient();
    }
}
