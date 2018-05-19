package bach.jianxu.watchsense;

import android.os.Bundle;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class MainActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WatchViewStub stub = findViewById(R.id.watch_view_stub);

//        final GridViewPager pager = findViewById(R.id.pager);
//        pager.setAdapter(new SensorFragmentPagerAdapter(getFragmentManager()));
//        stub.addView(pager);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override public void onLayoutInflated(WatchViewStub stub) {
                final GridViewPager pager = findViewById(R.id.pager);
                pager.setAdapter(new SensorFragmentPagerAdapter(getFragmentManager()));

                DotsPageIndicator indicator = findViewById(R.id.page_indicator);
                indicator.setPager(pager);
            }
        });


        // Enables Always-on
        setAmbientEnabled();
    }
}
