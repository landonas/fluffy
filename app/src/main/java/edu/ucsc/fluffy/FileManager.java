package edu.ucsc.fluffy;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.InvalidEntryException;
import com.google.gdata.util.ServiceException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class FileManager extends AppCompatActivity {
    final private String TAG = FileManager.class.getSimpleName();

    private MySimpleArrayAdapter dataAdapter = null;
    private String mEmail; // Received from newChooseAccountIntent(); passed to getToken()

    public static final String GOOGLE_ACCOUNT = "Google_Account";
    public static String SCOPE = "oauth2:https://spreadsheets.google.com/feeds";

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1002;
    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1003;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the file is passed as an argument, we don't actually want to prompt
        // for which file to upload. Instead, just upload it and finish.
        ArrayList<String> myFiles = getIntent().getStringArrayListExtra("patientFile");
        if (myFiles != null) {
            Log.i(TAG, "PatientID File: " + myFiles.toString());
            uploadFiles(myFiles);
            finish();
        }

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
            clearAllFiles();
            return true;
        } else if (id == R.id.action_select) {
            selectAllFiles();
            return true;
        } else if (id == R.id.action_edit) {
            editPatient();
            return true;
        } else if (id == R.id.action_sync) {
            uploadFiles();
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


    private void clearAllFiles() {

        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            f.setSelected(false);
        }
        dataAdapter.notifyDataSetChanged();

    }

    private void selectAllFiles() {

        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            f.setSelected(true);
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
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "VAS Files");
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


    private void uploadFiles() {

        File myFile = null;
        Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
        int count = 0;
        while (listIterator.hasNext()) {
            DataFile f = listIterator.next();
            if (f.isSelected())
                count++;
        }

        if (count == 0) {
            Toast.makeText(getApplicationContext(),
                    "Please select one file.", Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<String> files = new ArrayList<String>();
            listIterator = dataAdapter.fileList.listIterator();
            while (listIterator.hasNext()) {
                DataFile f = listIterator.next();
                if (f.isSelected()) {
                    files.add(f.getFile().getAbsolutePath());
                    f.setSelected(false); // clear them after send
                }
            }
            uploadFiles(files);

            dataAdapter.notifyDataSetChanged();
        }


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

    private void pickUserAccount() {
        // check if the saved account exists, and use it.
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mEmail = sharedPref.getString(GOOGLE_ACCOUNT, null);
        Log.i(TAG, "mEmail read: " + mEmail);

        // if not ask for the accounts
        if (mEmail == null) {
            String[] accountTypes = new String[]{"com.google"};
            Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                    accountTypes, false, null, null, null, null);
            startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
        }
    }


    /**
     * Attempts to retrieve the spreadsheet data.
     * If the account is not yet known, invoke the picker. Once the account is known,
     * start an instance of the AsyncTask to get the auth token and do work with it.
     */
    public void uploadFiles(ArrayList<String> fnames) {
        if (mEmail == null) {
            pickUserAccount();
        }
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new putProcedureTask(FileManager.this, mEmail, SCOPE).execute(fnames);
        } else {
            Toast.makeText(this, "Not online. Please upload later.", Toast.LENGTH_LONG).show();
        }

    }

    // This async task will read the procedure steps from the spreadsheet
    public class putProcedureTask extends AsyncTask<ArrayList<String>, Void, ArrayList<String>> {
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
        protected ArrayList<String> doInBackground(ArrayList<String>... fnames) {
            Log.i(TAG, "doInBackground");
            ArrayList<String> files = new ArrayList<String>();

            try {
                String token = fetchToken();
                if (token != null) {
                    // **Insert the good stuff here.**
                    // Use the token to access the user's Google data.
                    SpreadsheetService service = new SpreadsheetService("MySpreadsheetIntegration");
                    service.setProtocolVersion(SpreadsheetService.Versions.V3);
                    //service.setUserCredentials("username", "password");//permission required to add in Manifest
                    service.setAuthSubToken(token);

                    SpreadsheetEntry spreadsheet = findSpreadsheet(service);

                    for (String f : fnames[0]) {
                        Log.i(TAG, f);
                        // sometimes there is a lag from the async task exiting the activity
                        // before the file is actually removed. If we get here in a second call,
                        // the file is already deleted.
                        File curfile = new File(f);
                        if (!curfile.exists())
                            continue;
                        Patient p = Patient.deserialize(f, 0);
                        //hack for now, if file is doctor_*.ser it is a doctor file
                        // if it is patient_*.ser it is a patient file
                        if (f.contains("doctor")) {
                            doctorUpdate(service, spreadsheet, p);
                            files.add(f);
                        } else if (f.contains("patient")) {
                            patientUpdate(service, spreadsheet, p);
                            files.add(f);
                        } else
                            Log.e(TAG, "Error, not a patient or doctor file!");

                    }
                } else
                    Log.e(TAG, "Could not get Google authentication token.");
            } catch (IOException e) {
                // The fetchToken() method handles Google-specific exceptions,
                // so this indicates something went wrong at a higher level.
                // TIP: Che
                // ck for network connectivity before starting the AsyncTask.
                //...
                e.printStackTrace();

            }

            return files;
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
            } catch (GoogleAuthException e) {
                // Some other type of unrecoverable exception has occurred.
                // Report and log the error as appropriate for your app.
                //...
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(ArrayList<String> files) {
            Log.i(TAG, "Posted new results. Removing file(s): ");
            for (String f : files) {
                File myFile = new File(f);
                myFile.delete();
            }

            dataAdapter.notifyDataSetChanged();

        }
    }

    // This creates the patient worksheet and also adds a blank row
    void patientUpdate(SpreadsheetService service, SpreadsheetEntry spreadsheet, Patient p) {
        if (p == null || p.pain == null) {
            Log.e(TAG, "No pain levels recorded to upload.");
            return;
        } else {

            WorksheetEntry patientWorksheet = createPatientWorksheet(service, spreadsheet, p);
            addPatientPainLevels(service, patientWorksheet, p);
            addPatientPainFormulas(service, patientWorksheet);

            WorksheetEntry worksheet = findWorksheet(service, spreadsheet, "Patient Summary");
            // This will create an empty row if it isn't created already.
            addPatientDataRow(service, worksheet, p);
            // this re-writes the pain formulas to refresh them if they were previously written
            addSummaryPainFormulas(service, worksheet, p);
        }

    }

    // Creates a new patient worksheet. Will delete the old one if it already exists.
    WorksheetEntry createPatientWorksheet(SpreadsheetService service, SpreadsheetEntry spreadsheet, Patient p) {
        String ws_name = "Patient " + p.ID;

        // check if the worksheet exists, if so, delete it and start fresh
        WorksheetEntry worksheet = findWorksheet(service, spreadsheet, ws_name);
        try {
            if (worksheet != null)
                worksheet.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create a local representation of the new worksheet.
        worksheet = new WorksheetEntry();
        worksheet.setTitle(new PlainTextConstruct(ws_name));
        worksheet.setColCount(4); // extra +2 for delta time column and weighted pain column
        worksheet.setRowCount(p.pain.size() + 1); // extra +1 for headers

        URL worksheetFeedUrl = spreadsheet.getWorksheetFeedUrl();
        try {
            service.insert(worksheetFeedUrl, worksheet);
        } catch (InvalidEntryException e) {
            Log.e(TAG, "Worksheet already exists.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // query the worksheet again to not use the local one
        worksheet = findWorksheet(service, spreadsheet, ws_name);
        return (worksheet);

    }

    // This creates the patient summary line in the main worksheet, populates it with patient data
    // and then computes (or rewrites) the formulas.
    void doctorUpdate(SpreadsheetService service, SpreadsheetEntry spreadsheet, Patient p) {
        WorksheetEntry worksheet = findWorksheet(service, spreadsheet, "Patient Summary");
        if (p == null) {
            Log.e(TAG, "Invalid patient.");
        } else {
            addPatientDataRow(service, worksheet, p);
            populatePatientDataRow(service, worksheet, p);
            // this re-writes the pain formulas to refresh them if they were previously written
            addSummaryPainFormulas(service, worksheet, p);
        }
    }

    // This creates a worksheet for the patient uploads the patient pain levels to an individual patient worksheet.
    // If the worksheet already exists, it deletes and replaces the worksheet.
    void addPatientPainLevels(SpreadsheetService service, WorksheetEntry worksheet, Patient p) {
        try {
            // add the header rows
            URL cellFeedUrl = worksheet.getCellFeedUrl();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);
            CellEntry cell = new CellEntry(1, 1, "Time");
            service.insert(cellFeedUrl, cell);
            cell = new CellEntry(1, 2, "Pain");
            service.insert(cellFeedUrl, cell);
            cell = new CellEntry(1, 3, "Delta_Time");
            service.insert(cellFeedUrl, cell);
            cell = new CellEntry(1, 4, "Weighted_Pain");
            service.insert(cellFeedUrl, cell);

//            // update the first row to have the correct titles
//            for (CellEntry c : cellFeed.getEntries()) {
//                Log.i(TAG, "Cell: " + c.getTitle().getPlainText());
//            }

            URL listFeedUrl = worksheet.getListFeedUrl();

            ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);
            // Iterate through each row, printing its cell values.
            for (ListEntry row : listFeed.getEntries()) {
                // Print the first column's cell value
                System.out.print(row.getTitle().getPlainText() + "\t");
                // Iterate over the remaining columns, and print each cell value
                for (String tag : row.getCustomElements().getTags()) {
                    System.out.print(row.getCustomElements().getValue(tag) + "\t");
                }
                System.out.println();
            }

            URL listFeedUrl2 = worksheet.getListFeedUrl();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Boolean firstRow = true;
            for (Pair<Long, Float> pit : p.pain) {
                ListEntry row = new ListEntry();
                Date resultdate = new Date(pit.getFirst());
                row.getCustomElements().setValueLocal("Time", sdf.format(resultdate));
                row.getCustomElements().setValueLocal("Pain", Float.toString(pit.getSecond()));

                service.insert(listFeedUrl2, row);
            }
        } catch (
                ServiceException e
                )

        {
            e.printStackTrace();
        } catch (
                Exception e
                )

        {
            e.printStackTrace();
        }
    }


    void addPatientPainFormulas(SpreadsheetService service, WorksheetEntry worksheet) {
        Log.i(TAG, "addPatientPainFormulas");

        Integer col_delta = findColumn(service, worksheet, "Delta_Time");
        Integer col_weighted = findColumn(service, worksheet, "Weighted_Pain");
        try {
            URL cellFeedUrl = worksheet.getCellFeedUrl();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);
            // Ignore the header row, rows start at 1, so data rows start at 2
            // The first data row should be 0
            CellEntry delta_cell = new CellEntry(2, col_delta, "0");
            service.insert(cellFeedUrl, delta_cell);
            CellEntry weighted_cell = new CellEntry(2, col_weighted, "0");
            service.insert(cellFeedUrl, weighted_cell);
            // The remaining data rows
            for (int row = 3; row < worksheet.getRowCount()+1; row++) {
                CellEntry delta_cell = new CellEntry(row, col_delta, "=R[0]C[-2]-R[-1]C[-2]");
                service.insert(cellFeedUrl, delta_cell);
                CellEntry weighted_cell = new CellEntry(row, col_weighted, "=R[0]C[-1]*R[0]C[-2]");
                service.insert(cellFeedUrl, weighted_cell);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This adds a blank patient data row in the summary sheet.
    void addPatientDataRow(SpreadsheetService service, WorksheetEntry worksheet, Patient p) {
        Log.i(TAG, "addPatientDataRow");

        try {
            URL listFeedUrl = new URI(worksheet.getListFeedUrl().toString()
                    + "?sq=patientid=" + p.ID).toURL();
            ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);
            List<ListEntry> l = listFeed.getEntries();

            ListEntry row = null;
            if (l.size() > 1) {
                Log.e(TAG, "Multiple matching rows: " + l.size());
                row = l.get(0);
            } else if (l.size() == 1) {
                Log.i(TAG, "Updating row...");
                row = l.get(0);
            } else {
                Log.i(TAG, "Creating row...");
                row = new ListEntry();
            }

            if (p.ID != null)
                row.getCustomElements().setValueLocal("PatientID", Integer.toString(p.ID));

            if (l.size() > 0)
                row.update();
            else {
                URL listFeedUrl2 = worksheet.getListFeedUrl();
                service.insert(listFeedUrl2, row);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Adds the basic stats to a new or existing patient data row.
    void populatePatientDataRow(SpreadsheetService service, WorksheetEntry worksheet, Patient p) {
        Log.i(TAG, "populatePatientDataRow");

        try {
            URL listFeedUrl = new URI(worksheet.getListFeedUrl().toString()
                    + "?sq=patientid=" + p.ID).toURL();
            ListFeed listFeed = service.getFeed(listFeedUrl, ListFeed.class);
            List<ListEntry> l = listFeed.getEntries();

            ListEntry row = null;
            if (l.size() > 1) {
                Log.e(TAG, "Multiple matching rows: " + l.size());
                row = l.get(0);
                populateSummaryRow(row, p);
                row.update();
            } else if (l.size() == 1) {
                Log.i(TAG, "Updating row...");
                row = l.get(0);
                populateSummaryRow(row, p);
                row.update();
            } else {
                Log.e(TAG, "Row not found!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    Integer findColumn(SpreadsheetService service, WorksheetEntry worksheet, String name) {
        Log.i(TAG, "findColumn");

        try {
            // get the column numbers of each Pain* by removing R1 from each
            URL cellFeedUrl = new URI(worksheet.getCellFeedUrl().toString()
                    + "?max-row=1").toURL();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

            for (CellEntry cell : cellFeed.getEntries()) {
                if (cell.getCell().getInputValue().equals(name))
                    return (cell.getCell().getCol());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }


    Integer findRow(SpreadsheetService service, WorksheetEntry worksheet, String name) {
        Log.i(TAG, "findRow");

        try {
            // get the row number by finding the ID that matches
            // get the column numbers of each Pain* by removing R1 from each
            URL cellFeedUrl = new URI(worksheet.getCellFeedUrl().toString()
                    + "?max-col=1").toURL();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

            // Find the correct patient row number
            Integer row = 0;
            for (CellEntry cell : cellFeed.getEntries()) {
//                Log.i(TAG, cell.getId().substring(cell.getId().lastIndexOf('/') + 1));
//                Log.i(TAG, cell.getCell().getInputValue() + "\n");
                if (cell.getCell().getInputValue().equals(name))
                    return (cell.getCell().getRow());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e(TAG, "Couldn't find correct row!");
        return 0;

    }

    // fills in the cell formulas to look up the pain at different times
    void addSummaryPainFormulas(SpreadsheetService service, WorksheetEntry worksheet, Patient
            p) {
        Log.i(TAG, "addSummaryPainFormulas");

        try {
            // get the column numbers of each Pain* by removing R1 from each
            URL cellFeedUrl = new URI(worksheet.getCellFeedUrl().toString()
                    + "?max-row=1").toURL();
            CellFeed cellFeed = service.getFeed(cellFeedUrl, CellFeed.class);

            ArrayList<Integer> colNums = new ArrayList<Integer>();
            for (CellEntry cell : cellFeed.getEntries()) {
//                Log.i(TAG, cell.getId().substring(cell.getId().lastIndexOf('/') + 1));
//                Log.i(TAG, cell.getCell().getInputValue() + "\n");
                if (cell.getCell().getInputValue().startsWith("Pain"))
                    colNums.add(cell.getCell().getCol());
            }

            Integer col_area_pain = findColumn(service, worksheet, "Area_Pain");
            Integer col_avg_pain = findColumn(service, worksheet, "Avg_Pain");
            Integer col_max_pain = findColumn(service, worksheet, "Max_Pain");
            Integer col_min_pain = findColumn(service, worksheet, "Min_Pain");

            Integer row = findRow(service, worksheet, Integer.toString(p.ID));

            // for each RmCn insert a formula with =LOOKUP
            for (Integer col : colNums) {
                String cellContents = String.format("=LOOKUP(%s,'Patient %s'!$A:$A,'Patient %s'!$B:$B)",
                        R1C1toA1(row, col + 1), p.ID, p.ID);
                CellEntry cell = new CellEntry(row, col, cellContents);
                service.insert(cellFeedUrl, cell);
            }


            String rowB = new String("'Patient" + Integer.toString(p.ID) + "'!B:B");
            String rowC = new String("'Patient" + Integer.toString(p.ID) + "'!C:C");

            CellEntry area_cell = new CellEntry(row, col_area_pain, "=SUMPRODUCT(" + rowC + "," + rowB + ")");
            service.insert(cellFeedUrl, area_cell);
            CellEntry avg_cell = new CellEntry(row, col_avg_pain, "=R[0]C[-1]/ SUM(" + rowC + ")");
            service.insert(cellFeedUrl, avg_cell);
            CellEntry max_cell = new CellEntry(row, col_max_pain, "=MAX(" + rowB + ")");
            service.insert(cellFeedUrl, max_cell);
            CellEntry min_cell = new CellEntry(row, col_min_pain, "=MIN(" + rowB + ")");
            service.insert(cellFeedUrl, min_cell);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String R1C1toA1(Integer row, Integer col) {
        char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        String name = row.toString();
        Integer value = col;
        while (value > 0) {
            Integer remainder = (value - 1) % 26;
            name = alphabet[remainder] + name;

            value = ((value - remainder) / 26);
        }

        return name;
    }


    // Adds the patient demographic information and the analysis formulas to the summary row.
    protected void populateSummaryRow(ListEntry row, Patient p) {
        Log.i(TAG, "addSummaryRow");

        // Iterate over the remaining columns, and print each cell value
        for (String tag : row.getCustomElements().getTags()) {
            Log.i(TAG, "Row: " + tag + " : " + row.getCustomElements().getValue(tag));
        }

        if (p.ID != null)
            row.getCustomElements().setValueLocal("PatientID", Integer.toString(p.ID));
        if (p.age != null)
            row.getCustomElements().setValueLocal("Age", Integer.toString(p.age));
        if (p.age != null)
            row.getCustomElements().setValueLocal("AnxietyVAS", Float.toString(p.VAS));
        if (p.race != null)
            row.getCustomElements().setValueLocal("Race", p.race);
        if (p.BMI != null)
            row.getCustomElements().setValueLocal("BMI", Float.toString(p.BMI));

        if (p.steps != null & p.steps.size() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

            for (Pair<Long, String> pair : p.steps) {
                //Log.i(TAG, "Time : " + pair.getSecond());
                String key1 = "Time_" + pair.getSecond();
                key1 = key1.replaceAll("_", "");
                Date resultdate = new Date(pair.getFirst());
                String value1 = sdf.format(resultdate);

                row.getCustomElements().setValueLocal(key1, value1);

            }
        }


    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
                Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (intent != null) {
                    // User input required
                    startActivity(intent);
                } else {
                    //this.setResult("Token: " + bundle.getString(AccountManager.KEY_AUTHTOKEN));
                }
            } catch (Exception e) {
                //this.setResult(e.toString());
            }
        }

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
                    int statusCode = ((GooglePlayServicesAvailabilityException) e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            FileManager.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException) e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            // Receiving a result from the AccountPicker
            if (resultCode == RESULT_OK) {
                mEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                Log.i(TAG, "mEmail save: " + mEmail);
                // save the email as a preference
                SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(GOOGLE_ACCOUNT, mEmail);
                editor.commit();

                // With the account name acquired, go get the auth token
                uploadFiles();

            } else if (resultCode == RESULT_CANCELED) {
                // The account picker dialog closed without selecting an account.
                // Notify users that they must pick an account to proceed.
                Toast.makeText(this, mEmail, Toast.LENGTH_SHORT).show();
            }
        } else if ((requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR ||
                requestCode == REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR)
                && resultCode == RESULT_OK) {
            // Receiving a result that follows a GoogleAuthException, try auth again
            uploadFiles();
        }
    }


    // Helper function to locate a spreadsheet with a fixed name in the preferences.
    SpreadsheetEntry findSpreadsheet(SpreadsheetService service) {

        // get the spreadsheet name
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String ss_name = prefs.getString("spreadsheet_name", getString(R.string.pref_default_spreadsheet_name));


        // Make a request to the API and get all spreadsheets.
        SpreadsheetFeed feed;
        try {
            // Define the URL to request.  This should never change.
            URL SPREADSHEET_FEED_URL = new URL(
                    "https://spreadsheets.google.com/feeds/spreadsheets/private/full");

            feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
            List<SpreadsheetEntry> spreadsheets = feed.getEntries();


            // Iterate through all of the spreadsheets returned
            for (SpreadsheetEntry spreadsheet : spreadsheets) {
                // Print the title of this spreadsheet to the screen
                if (ss_name != null && ss_name.equals(spreadsheet.getTitle().getPlainText())) {
                    Log.i(TAG, "Found Sheet: " + spreadsheet.getTitle().getPlainText());
                    return (spreadsheet);
                }
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper function to locate a worksheet with a given name.
    WorksheetEntry findWorksheet(SpreadsheetService service, SpreadsheetEntry
            spreadsheet, String ws_name) {
        try {
            for (WorksheetEntry worksheet : spreadsheet.getWorksheets()) {

                if (ws_name.equals(worksheet.getTitle().getPlainText())) {
                    Log.i(TAG, "Found Worksheet: " + worksheet.getTitle().getPlainText() + " " + worksheet.getRowCount() + " x " + worksheet.getColCount());
                    return (worksheet);
                }
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}