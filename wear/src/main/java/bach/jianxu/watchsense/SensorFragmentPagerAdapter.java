package bach.jianxu.watchsense;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.hardware.Sensor;
import android.support.wearable.view.FragmentGridPagerAdapter;

public class SensorFragmentPagerAdapter extends FragmentGridPagerAdapter {

    private int[] sensorTypes = {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE
    };

    private Activity mAct;
    public SensorFragmentPagerAdapter(FragmentManager fm, Activity ap) {
        super(fm);
        mAct = ap;
    }

    @Override
    public Fragment getFragment(int row, int column) {
        return SensorFragment.newInstance(sensorTypes[column], mAct);
    }

    @Override
    public int getRowCount() {
        return 1; // fix to 1 row
    }

    @Override
    public int getColumnCount(int row) {
        return sensorTypes.length;
    }
}
