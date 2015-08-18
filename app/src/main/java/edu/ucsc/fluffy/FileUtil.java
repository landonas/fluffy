package edu.ucsc.fluffy;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.net.Uri;
import java.sql.Timestamp;

public class FileUtil {

	final private String TAG = FileUtil.class.getSimpleName();

	private FileWriter fWriter = null;
	private File f = null;
    private File rootDir = null;
	public FileUtil(File d) {
		rootDir=d;
	}

	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

    public void addHeader(){
        try{
            if(fWriter != null){
                fWriter.append("Pressure (kPa),Pleth (ADU)\n");
            }
        }
        catch(IOException e){
            Log.e(TAG,"Failed to write file "+e);
        }
    }

	public void addStep(String step, Timestamp time){
		try{
			if(fWriter != null){
				//fWriter.append(pressure+","+pleth+"\n");
				fWriter.append(step+","+time.getTime());
			}
		}
		catch(IOException e){
			Log.e(TAG,"Failed to write file "+e);
		}
	}


	public void openDataFile() {

		try {
			if (isExternalStorageWritable()) {
                Date dNow = new Date( );
                SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd-HHmmss");
                f = new File(rootDir,"valsalvaData-"+ft.format(dNow)+".csv");
				fWriter = new FileWriter(f);

                addHeader();

			}
		} catch (IOException e) {
			Log.w("ExternalStorage", "Error creating Files " + e);

		}
	}

	public Uri closeDataFiles() {
        Uri myFile = null;
		try {
			if(fWriter != null){
                myFile = Uri.fromFile(f);
				fWriter.close();
				fWriter = null;
			}

		} catch (IOException e) {
			Log.e(TAG, "Error Closing Files " + e);
		}

        return(myFile);

	}
}
