package fi.oulu.tol.esde_2016_014.ohapclientesde_2016_014;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.opimobi.ohap.Device;

import fi.oulu.tol.esde_2016_014.ohap.CentralUnitConnection;

public class DeviceActivity extends ActionBarActivity {

    private static final String TAG = "DeviceActivity";
    public static final String EXTRA_CENTRAL_UNIT_URL = "fi.oulu.tol.esde.esdeNN.CENTRAL_UNIT_URL";
    public static final String EXTRA_DEVICE_ID = "fi.oulu.tol.esde_2016_014.DEVICE_ID";

    Device activeDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        CentralUnitConnection central_unit_connection = CentralUnitConnection.getInstance();

        Log.d(TAG, "Activity started");

        long deviceId;
        String url;

        Intent deviceIntent = getIntent();
        url = deviceIntent.getStringExtra(EXTRA_CENTRAL_UNIT_URL);
        deviceId = deviceIntent.getLongExtra(EXTRA_DEVICE_ID, -1);

        Log.d(TAG, "Received extras: " + deviceId + " and url: " + url);


        //The actual device - Item is casted to Device so that we can use characterized methods
        activeDevice = (Device) central_unit_connection.getItemById(deviceId);
        Log.d(TAG, "Got item: " + activeDevice.getName());

        //Check that there is central unit connection otherwise create error and display view
        if(central_unit_connection != null) {

            Log.d(TAG, "Device name: " + central_unit_connection.getItemById(deviceId).getName());
            final TextView nameTextView = (TextView) findViewById(R.id.textView_name);
            nameTextView.setText(central_unit_connection.getItemById(deviceId).getName());

            final TextView descriptionTextView = (TextView) findViewById(R.id.textView_description);
            descriptionTextView.setText(central_unit_connection.getItemById(deviceId).getDescription());

            final Switch switch1 = (Switch) findViewById(R.id.switch_value);
            switch1.setChecked(activeDevice.getBinaryValue());
            switch1.setVisibility(View.GONE);

            final SeekBar seekBar1 = (SeekBar) findViewById(R.id.seekBar_value);
            seekBar1.setVisibility(View.GONE);

            //Set decimal slider value correctly
            seekBar1.setProgress((int) activeDevice.getDecimalValue());

            final TextView decimalProgressTextView = (TextView) findViewById(R.id.textView_decimal_progress);
            decimalProgressTextView.setText(Integer.toString(seekBar1.getProgress()) + "%");

            //Check the device's value type and set the visibility of the buttons accordingly
            if (activeDevice.getValueType() == Device.ValueType.BINARY) {

                //Show the Binary on / off button
                switch1.setVisibility(View.VISIBLE);
                if (activeDevice.getType() == Device.Type.SENSOR) {
                    switch1.setClickable(false);
                } else {
                    switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            activeDevice.setBinaryValue(isChecked);
                            /* Show in log that it is changed */
                            Log.d(TAG, "Item: " + activeDevice.getName() + " sending server a new binary value: " + activeDevice.getBinaryValue());
                            CentralUnitConnection.getInstance().changeBinaryValue(activeDevice, isChecked);
                        }
                    });
                }

                //Hide the decimal progress textview
                decimalProgressTextView.setVisibility(View.GONE);

            } else if (activeDevice.getValueType() == Device.ValueType.DECIMAL) {

                //Show the Decimal slider (seekbar)
                seekBar1.setVisibility(View.VISIBLE);

                if (activeDevice.getType() == Device.Type.SENSOR) {
                    seekBar1.setEnabled(false);
                } else {
                    //Set new listener for seekbar and set values to textview accordingly
                    seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            decimalProgressTextView.setText(progress + "%");
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            //Nothing to track for now
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            /* Change also the device value when value setting is stopped, not working correctly because the central unit is loaded here again with initialized devices so the "new" value will disappear */
                            activeDevice.setDecimalValue(seekBar.getProgress());
                            /* Show in log that it is really changed */
                            Log.d(TAG, "Item: " + activeDevice.getName() + " with new decimal value: " + activeDevice.getDecimalValue());
                            CentralUnitConnection.getInstance().changeDecimalValue(activeDevice, activeDevice.getDecimalValue());
                        }
                    });
                }
            } else {
                //nothing now
            }

        } else{
            //If Central Unit connection is null
            //There was probably problem in connection, display error and throw back to main menu
            // Toast was generated when error was catched
            Log.d(TAG, "Failed at getting central_unit_connection");
            Intent containerIntent = new Intent(this, ContainerActivity.class);
            startActivity(containerIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            try {
                startActivity(settingsIntent);
            } catch (Exception e) {
                Log.w(TAG, "Error, Unable to start action_settings activity");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
