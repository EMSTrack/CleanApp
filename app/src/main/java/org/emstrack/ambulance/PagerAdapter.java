package org.emstrack.ambulance;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.emstrack.ambulance.tab.fragments.DispatcherActivity;
import org.emstrack.ambulance.tab.fragments.GPSActivity;
import org.emstrack.ambulance.tab.fragments.demo_viewTransmission;

/**
 * Created by justingil1748 on 4/17/17.
 */

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                DispatcherActivity tab1 = new DispatcherActivity();
                return tab1;
            case 1:
                demo_viewTransmission tab3 = new demo_viewTransmission();
                return tab3;
            case 2:
                GPSActivity tab4 = new GPSActivity();
                return tab4;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}