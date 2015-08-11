package edu.ucsc.fluffy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.CheckBox;
import android.util.Log;

import android.os.Environment;
import android.widget.Button;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.List;
import java.util.HashMap;
import java.util.Date;

public class FileManager extends Activity {
    private MySimpleArrayAdapter dataAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        displayListView();

        deleteButtonClick();

        emailButtonClick();

        clearButtonClick();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_manager, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public class MySimpleArrayAdapter extends ArrayAdapter<DataFile> {
        private final Context context;
        private ArrayList<DataFile> fileList;

        public MySimpleArrayAdapter(Context context, int textViewResourceID,ArrayList<DataFile> files) {
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
        setContentView(R.layout.activity_filemanager);

        final ListView listview = (ListView) findViewById(R.id.filemanager_listview);

        File rootDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File myDir = new File(rootDir, "ValsalvaData");

        ArrayList<DataFile> files = new ArrayList<DataFile>();
        for (File f : myDir.listFiles()) {
            if (f.isFile())
                files.add(new DataFile(f,false));
        }

        dataAdapter = new MySimpleArrayAdapter(this, R.layout.filemanager_listitem,files);
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

    private void deleteButtonClick() {


        Button myButton = (Button) findViewById(R.id.filemanager_delete_button);
        myButton.setOnClickListener(new AdapterView.OnClickListener() {

            @Override
            public void onClick(View v) {
                ArrayList<DataFile> fileList = dataAdapter.fileList;
                Iterator<DataFile> listIterator = fileList.listIterator();
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
        });

    }


    private void clearButtonClick() {
        Button myButton = (Button) findViewById(R.id.filemanager_clear_button);
        myButton.setOnClickListener(new AdapterView.OnClickListener() {

            @Override
            public void onClick(View v) {
                Iterator<DataFile> listIterator = dataAdapter.fileList.listIterator();
                while (listIterator.hasNext()) {
                    DataFile f = listIterator.next();
                    f.setSelected(false);
                }
                dataAdapter.notifyDataSetChanged();
            }
        });

    }
    private void emailButtonClick() {


        Button myButton = (Button) findViewById(R.id.filemanager_email_button);
        myButton.setOnClickListener(new AdapterView.OnClickListener() {

            @Override
            public void onClick(View v) {
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
                    emailIntent.putExtra(Intent.EXTRA_EMAIL,
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
        });

    }

}

