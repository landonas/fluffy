package edu.ucsc.fluffy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import android.widget.Button;

public class DoctorActivity extends ActionBarActivity {
    final private String TAG = DoctorActivity.class.getSimpleName();
    private ArrayList<String> procedureSteps = null;

    private Button buttonNextStep = null;
    private Button buttonEditPatient = null;
    private TextView textNextStep = null;
    private TextView textCurrentStep = null;
    private int currentStepIndex = 0;

    private FileUtil fUtil = null;


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
            // open the database
            fUtil = new FileUtil(getApplicationContext().getExternalFilesDir(null));
            fUtil.openDataFile();
        }

        if (currentStepIndex > procedureSteps.size()) {
                // Finish
        } else {
                // mark the time step
        }




        textCurrentStep.setText(procedureSteps.get(currentStepIndex));

        if (currentStepIndex+1 > procedureSteps.size())
            textNextStep.setText("Finish");

        else
            textNextStep.setText(procedureSteps.get(currentStepIndex+1));



    }

    public void startEditPatient(View view) {
        Intent intent = new Intent(this, PatientData.class);
        startActivity(intent);
    }
}
