package org.emstrack.ambulance;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.emstrack.ambulance.fragments.DispatcherActivity;
import org.emstrack.ambulance.fragments.GPSActivity;
import org.emstrack.ambulance.fragments.HospitalActivity;

/**
 * Created by mauricio on 2/21/18.
 */

public class Pager extends FragmentStatePagerAdapter {

    int numberOfTabs;

    public Pager(FragmentManager fm, int NumOfTabs) {

        super(fm);

        this.numberOfTabs = NumOfTabs;

    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {

            case 0:
                return new DispatcherActivity();

            case 1:
                return new HospitalActivity();

            case 2:
                return new GPSActivity();

            default:
                return null;

        }
    }

    @Override
    public int getCount() {
        return numberOfTabs;
    }

}