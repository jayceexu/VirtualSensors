package bach.jianxu.watchsense;

import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "WatchSense";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent aint = new Intent(this, SensingService.class);
        startService(aint);

    }

}
