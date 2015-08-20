package edu.ucsc.fluffy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.EditText;
import android.app.Activity;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import java.io.File;

public class PatientData extends ActionBarActivity {
    private int patientID = 0;
    private Patient p = null;

    private EditText mPatientID;
    private EditText mPatientAge;
    private EditText mPatientVAS;
    private Spinner mPatientRace;
    private EditText mPatientBMI;

    private File f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_data);

        patientID = getIntent().getIntExtra("patientID", 0);

        mPatientID   = (EditText)findViewById(R.id.patientID);
        mPatientAge   = (EditText)findViewById(R.id.patientAge);
        mPatientVAS   = (EditText)findViewById(R.id.patientVAS);
        mPatientRace   = (Spinner)findViewById(R.id.patientRace);
        mPatientBMI   = (EditText)findViewById(R.id.patientBMI);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.race_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPatientRace.setAdapter(adapter);


        if (patientID != 0) {
            f = new File(getApplicationContext().getExternalFilesDir(null),"patient_"+patientID+".ser" );

            if (f.exists()) {
                p = Patient.deserialize(f.getAbsolutePath());
            } else {
                p = new Patient(patientID);
            }

            mPatientID.setText(Integer.toString(p.ID));
            if (p.age != null)
                mPatientAge.setText(Integer.toString(p.age));
            if (p.VAS != null)
                mPatientVAS.setText(Float.toString(p.VAS));
            if (p.race != null)
                mPatientRace.setSelection(((ArrayAdapter<String>)mPatientRace.getAdapter()).getPosition(p.race));
            if (p.BMI != null)
                mPatientBMI.setText(Float.toString(p.BMI));
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_patient_data, menu);
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
                f = new File(getApplicationContext().getExternalFilesDir(null),"patient_"+patientID+".ser" );
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

            savePatient();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("patientID", patientID);
            if (patientID > 0)
                setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    private void loadOrCreatePatient() {

    }
    private void savePatient() {
        p.serialize(f.getAbsolutePath());
    }

}
