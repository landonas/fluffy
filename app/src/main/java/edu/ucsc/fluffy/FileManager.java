package edu.ucsc.fluffy;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.CheckBox;

import android.os.Environment;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

public class FileManager extends AppCompatActivity {
    final private String TAG = FileManager.class.getSimpleName();

    private MySimpleArrayAdapter dataAdapter = null;
    private String mEmail; // Received from newChooseAccountIntent(); passed to getToken()

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filemanager);

        displayListView();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_file_manager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_email) {
            emailFiles();
            return true;
        } else if (id == R.id.action_delete) {
            deleteFiles();
            return true;
        } else if (id == R.id.action_clear) {
            clearFiles();
            return true;
        } else if (id == R.id.action_edit) {
            editPatient();
            return true;
        } else if (id == R.id.action_sync) {
            syncFiles();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class MySimpleArrayAdapter extends ArrayAdapter<DataFile> {
        private final Context context;
        private ArrayList<DataFile> fileList;

        public MySimpleArrayAdapter(Context context, int textViewResourceID, ArrayList<DataFile> files) {
            super(context, textViewResourceID, files);
            this.context = context;
            this.fileList = new ArrayList<DataFile>();
            this.fileList.addAll(files);

        }

        private class ViewHolder {
            TextView textView;
            CheckBox cbView;
        }

        @Override
        public int getCount() {
            return fileList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.filemanager_listitem, null);

                holder = new ViewHolder();
                holder.cbView = (CheckBox) convertView.findViewById(R.id.file_name);
                holder.textView = (TextView) convertView.findViewById(R.id.file_details);
                convertView.setTag(holder);


                holder.cbView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v;
                        DataFile f = (DataFile) cb.getTag();
                        /*Toast.makeText(getApplicationContext(),
                                "Clicked on Checkbox: " + cb.getText() +
                                        " is " + cb.isChecked(),
                                Toast.LENGTH_SHORT).show();*/
                        f.setSelected(cb.isChecked());
                    }
                });
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            DataFile f = fileList.get(position);
            File vf = f.getFile();
            if (vf.exists())
                holder.textView.setText("File size: " + vf.length() + " bytes");
            else
                holder.textView.setText("Path problem...");
            holder.cbView.setText(f.getName());
            holder.cbView.setChecked(f.isSelected());
            holder.cbView.setTag(f);

            return convertView;
        }
    }

    private void displayListView() {
        final ListView listview = (ListView) findViewById(R.id.filemanager_listview);

        File rootDir = getApplicationContext().getExternalFilesDir(null);

        ArrayList<DataFile> files = new ArrayList<DataFile>();
        for (File f : rootDir.listFiles()) {
            if (f.isFile())
                files.add(new DataFile(f, false));
        }

        dataAdapter = new MySimpleArrayAdapter(this, R.layout.filemanager_listitem, files);
        listview.setAdapter(dataAdapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                /*Toast.makeText(getApplicationContext(),
                        "Click ListItem Number " + position, Toast.LENGTH_SHORT)
                        .show();*/
            }

        });
    }

    private void deleteFiles() {

        ArrayList<DataFile> fileList = dataAdapter.fileList;
        Iterator<DataFile> listIterator = fileList.listIterator();
        int count = 0;
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            if (f.isSelected()) {
                count++;
            }
        }

        if (count == 0) {
            Toast.makeText(getApplicationContext(),
                    "Please select at least one file.", Toast.LENGTH_SHORT).show();
        } else {
            listIterator = fileList.listIterator();
            while (listIterator.hasNext()) {
                DataFile f = listIterator.next();
                if (f.isSelected()) {
                    f.getFile().delete(); // delete the file in the filesystem
                    listIterator.remove(); // remove the file from the listview
                }
            }
            Toast.makeText(getApplicationContext(),
                    "Deleting the files...", Toast.LENGTH_SHORT).show();

            dataAdapter.notifyDataSetChanged();
        }


    }


    private void clearFiles() {

        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            f.setSelected(false);
        }
        dataAdapter.notifyDataSetChanged();

    }

    private void emailFiles() {


        ArrayList<Uri> uris = new ArrayList<Uri>();
        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        int count = 0;
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            if (f.isSelected()) {
                //convert from paths to Android friendly Parcelable Uri's
                Uri u = Uri.fromFile(f.getFile());
                uris.add(u);
                count++;
            }
        }

        if (count == 0) {
            Toast.makeText(getApplicationContext(),
                    "Please select at least one file.", Toast.LENGTH_SHORT).show();
        } else {
            //need to "send multiple" to get more than one attachment
            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            emailIntent.setType("text/plain");
            //emailIntent.setType("message/rfc822");
            emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                    new String[]{"mrg@ucsc.edu"});
            //emailIntent.putExtra(android.content.Intent.EXTRA_CC,
            //       new String[]{emailCC});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Valsalva Files");
            //emailIntent.putExtra(Intent.EXTRA_TEXT, "Here are the Valsalva files that you wanted.");
            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            Context context = getApplicationContext();
            Intent chooser = Intent.createChooser(emailIntent, "Send mail...");
            chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        }

        listIterator = dataAdapter.fileList.listIterator();
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            if (f.isSelected())
                f.setSelected(false); // clear them after send
        }
        dataAdapter.notifyDataSetChanged();

    }


    private void syncFiles() {

        Uri myFile = null;
        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        int count = 0;
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            if (f.isSelected()) {
                //convert from paths to Android friendly Parcelable Uri's
                myFile = Uri.fromFile(f.getFile());
                count++;

                if (f.isSelected())
                    f.setSelected(false); // clear them
            }
        }

        if (count == 0) {
            Toast.makeText(getApplicationContext(),
                    "Please select one file.", Toast.LENGTH_SHORT).show();
        } else if (count > 1) {
            Toast.makeText(getApplicationContext(),
                    "Please select only one file.", Toast.LENGTH_SHORT).show();
        } else {

            // sync each file

            // sync it here
        }

        dataAdapter.notifyDataSetChanged();

    }

    private void editPatient() {

        File myFile = null;
        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        int count = 0;
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            if (f.isSelected()) {
                //convert from paths to Android friendly Parcelable Uri's
                myFile = f.getFile();
                count++;

                if (f.isSelected())
                    f.setSelected(false); // clear them
            }
        }

        if (count == 0) {
            Toast.makeText(getApplicationContext(),
                    "Please select one file.", Toast.LENGTH_SHORT).show();
        } else if (count > 1) {
            Toast.makeText(getApplicationContext(),
                    "Please select only one file.", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, PatientData.class);
            intent.putExtra("patientID", 0); // just empty since we'll read everything
            intent.putExtra("patientFile", myFile.getAbsolutePath());
            startActivity(intent);
        }

    }



    /**
     * Attempts to retrieve the spreadsheet data.
     * If the account is not yet known, invoke the picker. Once the account is known,
     * start an instance of the AsyncTask to get the auth token and do work with it.
     */
    private void putPatientData() {
        // check if the saved account exists, and use it.
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mEmail = sharedPref.getString(ChooserActivity.GOOGLE_ACCOUNT, null);
        Log.i(TAG, "mEmail read: " + mEmail);


        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new putProcedureTask(FileManager.this, mEmail, ChooserActivity.SCOPE).execute();
        } else {
            Toast.makeText(this, "Not online", Toast.LENGTH_LONG).show();
        }
    }

    // This async task will read the procedure steps from the spreadsheet
    public class putProcedureTask extends AsyncTask<String, Void, Void> {
        Activity mActivity;
        String mScope;
        String mEmail;

        putProcedureTask(Activity activity, String name, String scope) {
            this.mActivity = activity;
            this.mScope = scope;
            this.mEmail = name;
        }

        /**
         * Executes the asynchronous job. This runs when you call execute()
         * on the AsyncTask instance.
         */
        @Override
        protected void doInBackground(String... param) {
            try {
                Log.i(TAG, "doInBackground");
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

                                    if ("Patient Summary".equals(worksheet.getTitle().getPlainText())) {
                                       // create a new row
                                        Log.i(TAG, "Creating row...");
                                        // Fetch the list feed of the worksheet.
                                        URL listFeedUrl = worksheet.getListFeedUrl();
                                        ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);

                                        // Create a local representation of the new row.
                                        ListEntry row = new ListEntry();
                                        row.getCustomElements().setValueLocal("Patient ID", p.iD);
                                        row.getCustomElements().setValueLocal("Age", p.age);
                                        row.getCustomElements().setValueLocal("Anxiety VAS", p.vas);
                                        row.getCustomElements().setValueLocal("Race", p.race);
                                        row.getCustomElements().setValueLocal("BMI", p.bmi);


                                        // Send the new row to the API for insertion.
                                        row = service.insert(listFeedUrl, row);

                                    }
                                    return;
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
            return;
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
                FileManager.this.handleException(userRecoverableException);
            } catch (GoogleAuthException fatalException) {
                // Some other type of unrecoverable exception has occurred.
                // Report and log the error as appropriate for your app.
                //...
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void) {
            Log.i(TAG,"Posted new results");
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


}
