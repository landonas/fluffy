package edu.ucsc.fluffy;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.app.Dialog;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;
import android.view.View.OnKeyListener;
import android.view.KeyEvent;
import android.util.Log;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;
/**
 * Created by mrg on 8/21/15.
 */
public class PatientDialogFragment extends DialogFragment {
    final private String TAG = PatientDialogFragment.class.getSimpleName();

    public interface PatientIDInterface {
        public void loadOrCreatePatient(int id);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();


        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_patientid, null))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int pid = 0;
                        EditText mPatientID = (EditText) getDialog().findViewById(R.id.dialog_patientID);
                        if (mPatientID.getText().toString().length() > 0)
                            pid = Integer.parseInt(mPatientID.getText().toString());
                        dataPasser.loadOrCreatePatient(pid);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dataPasser.loadOrCreatePatient(0);
                    }
                });
        AlertDialog d = builder.create();
        d.show();

        EditText mPatientID = (EditText) d.findViewById(R.id.dialog_patientID);
        mPatientID.setOnEditorActionListener(onEnterSubmitAction);

        return d;
    }



    protected OnEditorActionListener onEnterSubmitAction= new OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            //Log.i(TAG,"Key: " + actionId + " Keyevent: " + event);

            if ((actionId & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE) {
                // Perform action on key press
                int pid = 0;
                EditText mPatientID = (EditText) getDialog().findViewById(R.id.dialog_patientID);
                if (mPatientID.getText().toString().length() > 0)
                    pid = Integer.parseInt(mPatientID.getText().toString());
                dataPasser.loadOrCreatePatient(pid);
                dismiss();
                return true;
            }
            return false;
        }
    };

    PatientIDInterface dataPasser;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        dataPasser = (PatientIDInterface) a;
    }

}
