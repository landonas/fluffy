package edu.ucsc.fluffy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import android.widget.Button;
import android.widget.Toast;

public class DoctorActivity extends ActionBarActivity {
    final private String TAG = DoctorActivity.class.getSimpleName();
    private ArrayList<String> procedureSteps = null;

    static final int REQUEST_PATIENT_ID = 1;

    private Button buttonNextStep;
    private Button buttonEditPatient;
    private TextView textNextStep;
    private TextView textCurrentStep;
    private int currentStepIndex;
    private int patientID;

    private Patient p = null;
    private File f = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate"+ patientID);

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

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart"+ patientID);
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG,"onStop"+ patientID);
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.i(TAG,"onPause" + patientID);
        super.onPause();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        Log.i(TAG, "patientID: " + patientID);

        // we need at least a patient ID to start
        if (patientID == 0) {
            Intent intent = new Intent(this, PatientData.class);
            intent.putExtra("patientID", patientID);
            startActivityForResult(intent, REQUEST_PATIENT_ID);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PATIENT_ID && resultCode == RESULT_OK) {
            patientID = data.getIntExtra("patientID",0);
            Log.i(TAG,"Selected patientID: "+ patientID);
        } else {
            Toast.makeText(this, "Patient ID is required.", Toast.LENGTH_SHORT).show();

            Log.e(TAG, "Invalid activity result.");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_doctor, menu);
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

    public void nextStep(View view) {

        currentStepIndex++;

        if (currentStepIndex == 1) {
            //p = new Patient(patientID);
            f = new File(getApplicationContext().getExternalFilesDir(null),"patient_"+patientID+".ser" );
            loadOrCreatePatient();
                Log.i(TAG, "Opening file");
        }

        if (currentStepIndex > procedureSteps.size()) {
            savePatient();
        } else {
                // mark the time step
        }




        textCurrentStep.setText(procedureSteps.get(currentStepIndex));

        if (currentStepIndex+1 > procedureSteps.size())
            textNextStep.setText("Finish");

        else
            textNextStep.setText(procedureSteps.get(currentStepIndex+1));



    }

    private void loadOrCreatePatient() {
        if (f.exists()) {
            p = Patient.deserialize(f.getAbsolutePath());
        } else {
            p = new Patient(patientID);
        }
    }
    private void savePatient() {
        p.serialize(f.getAbsolutePath());
    }

    public void startEditPatient(View view) {
        Intent intent = new Intent(this, PatientData.class);
        intent.putExtra("patientID", patientID);
        startActivity(intent);
    }
}
