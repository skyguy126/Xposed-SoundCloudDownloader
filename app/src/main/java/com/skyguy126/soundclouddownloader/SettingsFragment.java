package com.skyguy126.soundclouddownloader;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.List;

import static android.content.Context.MODE_WORLD_READABLE;

public class SettingsFragment extends PreferenceFragment {

    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);

        this.prefs = getActivity().getSharedPreferences(Shared.PREFS_FILE_NAME, MODE_WORLD_READABLE);

        CheckBoxPreference showLauncherIconCheckbox = (CheckBoxPreference) findPreference(Shared.PREFS_CHECKBOX_KEY);
        showLauncherIconCheckbox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                ComponentName alias = new ComponentName(getActivity(), Shared.PACKAGE_NAME + ".MainActivity-Alias");

                if (checkBoxPreference.isChecked()) {
                    getActivity().getPackageManager().setComponentEnabledSetting(alias,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                } else {
                    getActivity().getPackageManager().setComponentEnabledSetting(alias,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }

                return true;
            }
        });

        ListPreference saveLocationSpinner = (ListPreference) findPreference(Shared.PREFS_SPINNER_KEY);
        saveLocationSpinner.setSummary(Shared.getSpinnerDescription(Integer.valueOf(saveLocationSpinner.getValue())));
        saveLocationSpinner.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference listPreference = (ListPreference) preference;
                Integer value = Integer.valueOf((String) newValue);

                listPreference.setSummary(Shared.getSpinnerDescription(value));
                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putInt(Shared.PREFS_SPINNER_KEY, value);
                prefsEditor.apply();

                return true;
            }
        });

        Preference sourceButton = findPreference("source_button");
        sourceButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Shared.openWebsite(getActivity(), Shared.SOURCE_LINK);
                return true;
            }
        });
    }
}
