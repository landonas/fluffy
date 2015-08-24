package edu.ucsc.fluffy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;
import android.app.Activity;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import java.io.File;

public class PatientData extends Activity {
    final private String TAG = PatientData.class.getSimpleName();

    private int patientID = 0;
    private Patient p = null;

    private EditText mPatientID;
    private EditText mPatientAge;
    private EditText mPatientVAS;
    private Spinner mPatientRace;
    private EditText mPatientBMI;

    private TextView mPatientSteps;

    private File f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_data);

        patientID = getIntent().getIntExtra("patientID", 0);
        Log.i(TAG, "PatientID data: " + patientID);
        String myFile = getIntent().getStringExtra("patientFile");
        f = new File(myFile);
        Log.i(TAG, "PatientID File: " + f.getAbsolutePath());

        mPatientID   = (EditText)findViewById(R.id.patientID);
        mPatientAge   = (EditText)findViewById(R.id.patientAge);
        mPatientVAS   = (EditText)findViewById(R.id.patientVAS);
        mPatientRace   = (Spinner)findViewById(R.id.patientRace);
        mPatientBMI   = (EditText)findViewById(R.id.patientBMI);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.race_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPatientRace.setAdapter(adapter);

        mPatientSteps   = (TextView)findViewById(R.id.stepView);


        loadPatient();

    }


    private void loadPatient() {
            if (f.exists()) {
                Log.i(TAG, "Opening file" + f.getAbsolutePath());
                p = Patient.deserialize(f.getAbsolutePath(),patientID);

                if (p.ID != null)
                    mPatientID.setText(Integer.toString(p.ID));
                if (p.age != null)
                    mPatientAge.setText(Integer.toString(p.age));
                if (p.VAS != null)
                    mPatientVAS.setText(Float.toString(p.VAS));
                if (p.race != null)
                    mPatientRace.setSelection(((ArrayAdapter<String>) mPatientRace.getAdapter()).getPosition(p.race));
                if (p.BMI != null)
                    mPatientBMI.setText(Float.toString(p.BMI));
                if (p.steps != null) {
                    mPatientSteps.setText(p.getSteps());
                }

            } else {
                Log.e(TAG, "Patient should have been created by calling activity:\n" + f.getAbsolutePath());
                finish();
            }



    }

    public void savePatient(View view) {

        if (mPatientID.getText().toString().length()>0)
            patientID = Integer.parseInt(mPatientID.getText().toString());
        else
            patientID = 0;

        // ensure they at least entered a patient ID
        if (patientID == 0) {
            Toast.makeText(this, "Patient ID is required.", Toast.LENGTH_SHORT).show();

        } else {

            if (p==null) {
                p = new Patient(patientID);
                f = new File(getApplicationContext().getExternalFilesDir(null),"doctor_"+patientID+".ser" );
            }

            // everything is loaded in onCreate
            p.ID = patientID;
            if (mPatientAge.getText().toString().length()>0)
                p.age = Integer.parseInt(mPatientAge.getText().toString());
            if (mPatientVAS.getText().toString().length()>0)
                p.VAS = Float.parseFloat(mPatientVAS.getText().toString());
            p.race = mPatientRace.getSelectedItem().toString();
            if (mPatientBMI.getText().toString().length()>0)
                p.BMI = Float.parseFloat(mPatientBMI.getText().toString());

            Log.i(TAG, "Saving file" + f.getAbsolutePath());
            p.serialize(f.getAbsolutePath());

            Intent intent = this.getIntent();
            this.setResult(RESULT_OK, intent);

            finish();
        }
    }


}
