package com.skyguy126.soundclouddownloader;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import java.io.File;
import java.util.List;

import static android.content.Context.MODE_WORLD_READABLE;

public class SettingsFragment extends PreferenceFragment {

    private SharedPreferences prefs;

    @Override
    public void onPause() {
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        super.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        getPreferenceManager().setSharedPreferencesName(Shared.PREFS_FILE_NAME);
        addPreferencesFromResource(R.xml.prefs);


        this.prefs = getActivity().getSharedPreferences(Shared.PREFS_FILE_NAME, MODE_WORLD_READABLE);

        CheckBoxPreference showLauncherIconCheckbox = (CheckBoxPreference) findPreference(Shared.PREFS_LAUNCHER_ICON_KEY);
        showLauncherIconCheckbox.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
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

                if (value == 3) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Save Directory");

                    final EditText input = new EditText(getActivity());
                    input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
                    input.setText(prefs.getString(Shared.PREFS_SAVE_PATH_KEY, Shared.CUSTOM_PATH_DEFAULT));
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor prefsEditor = prefs.edit();
                            prefsEditor.putString(Shared.PREFS_SAVE_PATH_KEY, input.getText().toString());
                            prefsEditor.apply();
                        }
                    });

                    builder.show();
                }

                listPreference.setSummary(Shared.getSpinnerDescription(value));
                SharedPreferences.Editor prefsEditor = prefs.edit();
                prefsEditor.putString(Shared.PREFS_SPINNER_KEY, value.toString());
                prefsEditor.apply();

                return true;
            }
        });

        Preference sourceButton = findPreference("source_button");
        sourceButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Shared.openWebsite(getActivity(), Shared.SOURCE_LINK);
                return true;
            }
        });
    }
}
