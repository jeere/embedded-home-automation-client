package fi.oulu.tol.esde_2016_014.ohapclientesde_2016_014;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingsActivity extends Activity {

    private static final String TAG = "SettingsActivity";
    private SettingsFragment settingsFragment = null;
    private SharedPreferences sharedPreferences;
    private static Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsFragment = new SettingsFragment();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        appContext = getApplicationContext();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(settingsFragment);
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(settingsFragment);
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            EditTextPreference pref =  (EditTextPreference) findPreference("edit_text_preference_address");
            pref.setSummary(pref.getText());

            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
            editor.putString("setting_ip_address", pref.getText());
            editor.commit();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.w(TAG, "Changing SharedPreferences");

            if(key.equals("edit_text_preference_address")){

                Log.w(TAG, "Changing SharedPreferences, found matching key. Changing values");
                EditTextPreference preference = (EditTextPreference) findPreference(key);
                preference.setSummary(sharedPreferences.getString(key, ""));

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
                editor.putString("setting_ip_address", preference.getText());
                editor.commit();

            }else if(key.equals("check_box_preference_auto_connect")){

                CheckBoxPreference preference = (CheckBoxPreference) findPreference(key);
                if (preference != null) {
                    boolean connectionPref = sharedPreferences.getBoolean(key, false);
                    if (connectionPref) {
                        preference.setSummary(preference.getSummaryOn());
                        Log.w(TAG, "Changing SharedPreferences, changing auto connection to ON");
                    } else {
                        preference.setSummary(preference.getSummaryOff());
                        Log.w(TAG, "Changing SharedPreferences, changing auto connection to OFF");
                    }
                }

            }

        }
    }
}
