package edu.ucsc.fluffy;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends Activity {

    // OVERVIEW
    // This view opens a layout that shows the disclaimer and about section for the application

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version_info = (TextView) findViewById(R.id.version_info);

        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
            version_info.setText("PackageName = " + info.packageName + "\nVersionCode = "
                    + info.versionCode + "\nVersionName = "
                    + info.versionName + "\nPermissions = " + info.permissions);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

    }
}
