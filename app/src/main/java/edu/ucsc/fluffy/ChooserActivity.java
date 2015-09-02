package edu.ucsc.fluffy;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import android.widget.Button;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ChooserActivity extends ActionBarActivity
        implements ConnectionCallbacks, OnConnectionFailedListener {
    private final static String TAG = "Fluffy";
    public final String APP_ID="9f4540ae8fc66249da90e3c523efd662";

    private String CLIENT_ID = "546501280634-9ap7gnjoqfo7dmk7vf7jkujka9t931gu.apps.googleusercontent.com";
    public static String SCOPE = "oauth2:https://spreadsheets.google.com/feeds";
    public static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
    public static final String GOOGLE_ACCOUNT = "Google_Account";
    public static final String PROCEDURE_STEPS = "Procedure_Steps";

    private GoogleApiClient mGoogleApiClient;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1002;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1003;

    public static String mEmail; // Received from newChooseAccountIntent(); passed to getToken()
    private ArrayList<String> procedureSteps = null; // asynchronously pulled from Google Sheets


    private void pickUserAccount() {
        // check if the saved account exists, and use it.
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mEmail = sharedPref.getString(GOOGLE_ACCOUNT, null);
        Log.i(TAG,"mEmail read: "+mEmail);

        // if not ask for the accounts
        if (mEmail==null) {
            String[] accountTypes = new String[]{"com.google"};
            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                    accountTypes, false, null, null, null, null);
            startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                Log.i(TAG,"mEmail save: "+mEmail);
                // save the email as a preference
                SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(GOOGLE_ACCOUNT, mEmail);
                editor.commit();

                // With the account name acquired, go get the auth token
                getProcedureSteps();

            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, mEmail, Toast.LENGTH_SHORT).show();
            }
        }  else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            getProcedureSteps();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chooser);

        // Hockeyapp updates
        checkForUpdates();

    }


    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.

        Log.i(TAG, "GoogleApiClient connected");

        if (procedureSteps == null) {
            getProcedureSteps();
        }

        uploadAllFiles();

    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.i(TAG, "GoogleApiClient connection suspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());

        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            //showErrorDialog(result.getErrorCode());
            Toast.makeText(getApplicationContext(), "Error!", Toast.LENGTH_SHORT).show();
            mResolvingError = true;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chooser, menu);
        return super.onCreateOptionsMenu(menu);
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
            case R.id.action_google_chooser:
                // set it to null so we pick over
                mEmail=null;
                // clear the email preference
                SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(GOOGLE_ACCOUNT, mEmail);
                editor.commit();
                pickUserAccount();
                return true;
            case R.id.action_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.i(TAG,"onStart");

        super.onStart();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

        mGoogleApiClient.connect();

        pickUserAccount();

    }

    @Override
    protected void onStop() {
        Log.i(TAG,"onStop");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        checkForCrashes();


    }


    /**
     * Attempts to retrieve the spreadsheet data.
     * If the account is not yet known, invoke the picker. Once the account is known,
     * start an instance of the AsyncTask to get the auth token and do work with it.
     */
    private void getProcedureSteps() {
        if (mEmail == null) {
            pickUserAccount();
        }

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new GetProcedureStepsTask(ChooserActivity.this, mEmail, SCOPE).execute();
        } else {
            Toast.makeText(this, "Cannot download procedure steps. Please ensure you have Internet access.", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    // This async task will read the procedure steps from the spreadsheet
    public class GetProcedureStepsTask extends AsyncTask<String, Void, ArrayList<String>> {
        Activity mActivity;
        String mScope;
        String mEmail;

        GetProcedureStepsTask(Activity activity, String name, String scope) {
            this.mActivity = activity;
            this.mScope = scope;
            this.mEmail = name;
        }

        /**
         * Executes the asynchronous job. This runs when you call execute()
         * on the AsyncTask instance.
         */
        @Override
        protected ArrayList<String> doInBackground(String... param) {
            try {
                Log.i(TAG,"doInBackground");
                String token = fetchToken();
                if (token != null) {
                    // **Insert the good stuff here.**
                    // Use the token to access the user's Google data.
                    SpreadsheetService service = new SpreadsheetService("MySpreadsheetIntegration");
                    service.setProtocolVersion(SpreadsheetService.Versions.V3);
                    //service.setUserCredentials("username", "password");//permission required to add in Manifest
                    service.setAuthSubToken(token);

                    // Define the URL to request.  This should never change.
                    URL SPREADSHEET_FEED_URL = new URL(
                            "https://spreadsheets.google.com/feeds/spreadsheets/private/full");

                    // Make a request to the API and get all spreadsheets.
                    SpreadsheetFeed feed;
                    try {
                        feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
                        List<SpreadsheetEntry> spreadsheets = feed.getEntries();

                        // get the spreadsheet name
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String ss_name = prefs.getString("spreadsheet_name", getString(R.string.pref_default_spreadsheet_name));

                        // Iterate through all of the spreadsheets returned
                        for (SpreadsheetEntry spreadsheet : spreadsheets) {
                            // Print the title of this spreadsheet to the screen
                            if (ss_name != null && ss_name.equals(spreadsheet.getTitle().getPlainText())) {
                                Log.i(TAG, "Found Sheet: " + spreadsheet.getTitle().getPlainText());

                                for (WorksheetEntry worksheet : spreadsheet.getWorksheets()) {
                                    Log.i(TAG, "Worksheet: " + worksheet.getTitle().getPlainText() + " " + worksheet.getRowCount() + " x " + worksheet.getColCount());
                                    ArrayList<String> steps = new ArrayList<String>();

                                    if ("Procedure Steps".equals(worksheet.getTitle().getPlainText())) {
                                        URL cellFeedUrl = worksheet.getCellFeedUrl();
                                        CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

                                        // Iterate through each cell, printing its value.
                                        for (CellEntry cell : cellFeed.getEntries()) {
                                            steps.add(cell.getCell().getInputValue());
                                            Log.i(TAG, cell.getCell().getInputValue());

                                            // Print the cell's address in A1 notation
                                            //Log.i(TAG, cell.getTitle().getPlainText() + "\t");
                                            // Print the cell's address in R1C1 notation
                                            //Log.i(TAG, cell.getId().substring(cell.getId().lastIndexOf('/') + 1) + "\t");
                                            // Print the cell's formula or text value
                                            //Log.i(TAG,cell.getCell().getInputValue() + "\t");
                                            // Print the cell's calculated value if the cell's value is numeric
                                            // Prints empty string if cell's value is not numeric
                                            //Log.i(TAG,cell.getCell().getNumericValue() + "\t");
                                            // Print the cell's displayed value (useful if the cell has a formula)
                                            //Log.i(TAG,cell.getCell().getValue() + "\t\n");
                                        }
                                    }
                                    return steps;
                                }
                            }
                        }
                    } catch (ServiceException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // The fetchToken() method handles Google-specific exceptions,
                // so this indicates something went wrong at a higher level.
                // TIP: Check for network connectivity before starting the AsyncTask.
                //...
            }
            return null;
        }

        /**
         * Gets an authentication token from Google and handles any
         * GoogleAuthException that may occur.
         */
        protected String fetchToken() throws IOException {
            try {
                return GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
            } catch (UserRecoverableAuthException userRecoverableException) {
                // GooglePlayServices.apk is either old, disabled, or not present
                // so we need to show the user some UI in the activity to recover.
                ChooserActivity.this.handleException(userRecoverableException);
            } catch (GoogleAuthException e) {
                // Some other type of unrecoverable exception has occurred.
                // Report and log the error as appropriate for your app.
                //...
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> s) {
            procedureSteps = s;

            for (String str : procedureSteps) {
                if (str.contains(" ")) {
                    Toast.makeText(ChooserActivity.this, "Procedure steps may not contain spaces. Please fix in spreadsheet.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            // make the buttons active
            Button buttonAssistant = (Button) findViewById(R.id.buttonAssistant);
            buttonAssistant.setEnabled(true);

            Button buttonPatient = (Button) findViewById(R.id.buttonPatient);
            buttonPatient.setEnabled(true);

            //
        }
    }

//    // Create a local representation of the new worksheet.
//    WorksheetEntry worksheet = new WorksheetEntry();
//    worksheet.setTitle(new PlainTextConstruct("New Worksheet"));
//    worksheet.setColCount(10);
//    worksheet.setRowCount(20);
//
//    // Send the local representation of the worksheet to the API for
//    // creation.  The URL to use here is the worksheet feed URL of our
//    // spreadsheet.
//    URL worksheetFeedUrl = spreadsheet.getWorksheetFeedUrl();
//    service.insert(worksheetFeedUrl, worksheet);
//

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                if(intent != null) {
                    // User input required
                    startActivity(intent);
                } else {
                    //this.setResult("Token: " + bundle.getString(AccountManager.KEY_AUTHTOKEN));
                }
            } catch (Exception e) {
                //this.setResult(e.toString());
            }
        }
    };

    @Override
    public void onPause() {
        Log.i(TAG,"onPause");

        super.onPause();
        unregisterManagers();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterManagers();

    }

    private void checkForCrashes() {
        CrashManager.register(this, APP_ID);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, APP_ID);
    }

    private void unregisterManagers() {
        UpdateManager.unregister();
        // unregister other managers if necessary...
    }

    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }

    /**
     * This method is a hook for background threads and async tasks that need to
     * provide the user a response UI when an exception occurs.
     */
    public void handleException(final Exception e) {
        // Because this call comes from the AsyncTask, we must ensure that the following
        // code instead executes on the UI thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            ChooserActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    public void startAssistant(View view) {
        Intent intent = new Intent(this, DoctorActivity.class);
        intent.putStringArrayListExtra(PROCEDURE_STEPS, procedureSteps);
        startActivity(intent);
    }

    public void startPatient(View view) {
        Intent intent = new Intent(this, PatientActivity.class);
        startActivity(intent);
    }

    private void uploadAllFiles() {
        File fileDir = getApplicationContext().getExternalFilesDir(null);
        File file[] = fileDir.listFiles();
        ArrayList<String> fileNames = new ArrayList<String>();
        for (int i=0; i < file.length; i++) {
            fileNames.add(file[i].toString());
            Log.i(TAG,file[i].toString());
        }
        if (fileNames.size()>0) {
            Intent intent = new Intent(this, FileManager.class);
            intent.putStringArrayListExtra("patientFile", fileNames);
            startActivity(intent);
        }
    }
}


