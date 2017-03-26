package com.skyguy126.soundclouddownloader;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private int saveLoc;
    private boolean isHiddenFromLauncher;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.prefs = getSharedPreferences(Shared.PREFS_FILE_NAME, MODE_WORLD_READABLE);
        this.isHiddenFromLauncher = prefs.getBoolean(Shared.PREFS_CHECKBOX_KEY, true);
        this.saveLoc = prefs.getInt(Shared.PREFS_SPINNER_KEY, 1);

        CheckBox checkBox = (CheckBox) findViewById(R.id.disp_launcher_icon_checkbox);
        checkBox.setChecked(this.isHiddenFromLauncher);

        Spinner saveLocSpinner = (Spinner) findViewById(R.id.save_loc_spinner);
        ArrayAdapter<CharSequence> saveLocSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.save_loc_spinner_strings, android.R.layout.simple_spinner_item);
        saveLocSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        saveLocSpinner.setAdapter(saveLocSpinnerAdapter);
        saveLocSpinner.setOnItemSelectedListener(this);
        saveLocSpinner.setSelection(this.saveLoc);

        FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor prefsEditor = prefs.edit();

                prefsEditor.putInt(Shared.PREFS_SPINNER_KEY, saveLoc);
                prefsEditor.putBoolean(Shared.PREFS_CHECKBOX_KEY, isHiddenFromLauncher);
                prefsEditor.apply();

                ComponentName alias = new ComponentName(getApplicationContext(), Shared.PACKAGE_NAME + ".MainActivity-Alias");

                if (!isHiddenFromLauncher) {
                    getPackageManager().setComponentEnabledSetting(alias,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                } else {
                    getPackageManager().setComponentEnabledSetting(alias,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }

                Snackbar.make(v, "Saved preferences.", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    public void sourceButtonFunc(View v) {
        Shared.openWebsite(v.getContext(), Shared.SOURCE_LINK);
    }

    public void dispLauncherIconFunc(View v) {
        CheckBox checkBox = (CheckBox) v;
        this.isHiddenFromLauncher = checkBox.isChecked();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        CharSequence item = (CharSequence) parent.getItemAtPosition(position);
        this.saveLoc = Shared.getSpinnerPosition(item.toString());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
