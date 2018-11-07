package bach.jianxu.watchsense;

import android.Manifest;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "WatchSense";
    public static final int PERMISSIONS_REQUEST_CODE = 1;
    private ArrayList<Metaprogram> metaprograms = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent aint = new Intent(this, SensingService.class);
        startService(aint);
        checkPermissions();

        try {
            File SDCardRoot = Environment.getExternalStorageDirectory()
                    .getAbsoluteFile();
            File myDir = new File(SDCardRoot.getAbsolutePath() + "/temp/");
            File file = new File(myDir, "config.xml");

            InputStream is = new FileInputStream(file.getPath());

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, null);
            int eventType = parser.getEventType();
            String text = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        break;
                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;
                    case XmlPullParser.END_TAG:
                        if (name.equalsIgnoreCase("appname")) {
                            Metaprogram metaprogram = new Metaprogram();
                            metaprogram.app_name = text;
                            metaprograms.add(metaprogram);

                        } else if (name.equalsIgnoreCase("op")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.op = text;
                        } else if (name.equalsIgnoreCase("sensor_type")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.sensors.add(text);
                        } else if (name.equalsIgnoreCase("freq")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.freq.add(Integer.parseInt(text));
                        } else if (name.equalsIgnoreCase("x-calibrate")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size()-1);
                            meta.xcal.add(Integer.parseInt(text));
                        } else if (name.equalsIgnoreCase("y-calibrate")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size() - 1);
                            if (!text.equals(""))
                                meta.ycal.add(Integer.parseInt(text));


                        } else if (name.equalsIgnoreCase("z-calibrate")) {
                            Metaprogram meta = metaprograms.get(metaprograms.size() - 1);
                            meta.zcal.add(Integer.parseInt(text));
                        }
                        break;

                     default:
                         break;

                }
                eventType = parser.next();
            }

        } catch (Exception e) {
            Log.e(TAG, "XML Pasing Excpetion = " + e);
        }
        for (Metaprogram meta: metaprograms) {
            meta.dump();
        }

    }

    private void checkPermissions() {
        boolean writeExternalStoragePermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (!writeExternalStoragePermissionGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

}
