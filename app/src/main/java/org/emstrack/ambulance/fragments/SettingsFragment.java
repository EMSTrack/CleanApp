package org.emstrack.ambulance.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.emstrack.ambulance.MainActivity;
import org.emstrack.ambulance.R;
import org.emstrack.ambulance.dialogs.AboutDialog;
import org.emstrack.ambulance.models.AmbulanceAppData;
import org.emstrack.ambulance.services.AmbulanceForegroundService;
import org.emstrack.models.Settings;

public class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private MainActivity activity;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // get activity
        activity = (MainActivity) requireActivity();

        // Find preference by key
        Preference pref = findPreference(getString(R.string.versionKey));
        if (pref != null) {
            // set version label
            pref.setSummary(getString(R.string.preferences_build_version,
                    getString(R.string.app_version)).replace('_', '.'));
            // fire dialog
            pref.setOnPreferenceClickListener(preference -> {
                AboutDialog.create(activity).show();
                return true;
            });
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        activity.setupNavigationBar();

    }

    @Override
    public void onPause() {
        super.onPause();

        // get preference
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String units = sharedPreferences.getString(getString(R.string.unitsPreferenceKey),
                getString(R.string.unitsDefault));
        String waypointEnterDetection = sharedPreferences.getString(getString(R.string.waypointEnterDetectionPreferenceKey),
                getString(R.string.waypointEnterDetectionDefault));
        String waypointExitDetection = sharedPreferences.getString(getString(R.string.waypointExitDetectionPreferenceKey),
                getString(R.string.waypointExitDetectionDefault));

        // and save them in settings
        AmbulanceAppData appData = AmbulanceForegroundService.getAppData();
        Settings settings = appData.getSettings();
        settings.setUnits(units);
        settings.setWaypointEnterDetection(waypointEnterDetection);
        settings.setWaypointExitDetection(waypointExitDetection);

    }

}
