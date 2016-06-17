package fi.oulu.tol.esde_2016_014.ohapclientesde_2016_014;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;

import java.net.MalformedURLException;
import java.net.URL;

import fi.oulu.tol.esde_2016_014.ohap.CentralUnitConnection;

public class ContainerActivity extends ActionBarActivity{

    private static final String TAG = "ContainerActivity";
    public static final String EXTRA_CENTRAL_UNIT_URL = "fi.oulu.tol.esde.esde_2016_014.CENTRAL_UNIT_URL";
    public static final String EXTRA_CONTAINER_ID = "fi.oulu.tol.esde_2016_014.CONTAINER_ID";
    private URL url;
    private ContainerListAdapter containerListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG, "Starting activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        final ListView listView = (ListView) findViewById(R.id.listView1);
        final TextView textViewHeader = (TextView) findViewById(R.id.textView_container_header);

        // Check whether container is root or container through intent
        Intent itemIntent = getIntent();
        long intentContainerId = itemIntent.getLongExtra(EXTRA_CONTAINER_ID, -1);
        String intentContainerUrl = itemIntent.getStringExtra(EXTRA_CENTRAL_UNIT_URL);

        if(intentContainerId != 0 && intentContainerId != -1){
            Log.d(TAG, "Container not seems to be root, starting to find information with id: " + intentContainerId);
            if(CentralUnitConnection.getInstance().getItemById(intentContainerId) instanceof Container){
                Log.d(TAG, "Container found: "+ CentralUnitConnection.getInstance().getItemById(intentContainerId).getName());
                Log.d(TAG, "Intent id: " +intentContainerId);
                Log.d(TAG, "Container id: " + CentralUnitConnection.getInstance().getItemById(intentContainerId).getId());
                Long id = intentContainerId;
                Container childContainer = (Container) CentralUnitConnection.getInstance().getItemById(id);

                Log.d(TAG, "Adding container to listadapter...");
                containerListAdapter = new ContainerListAdapter( childContainer );

                childContainer.startListening();
                textViewHeader.setText(childContainer.getName());

                Log.d(TAG, "Done.");
            }else{
                Log.d(TAG, "Was not instance of Container ... Get the root back");
                containerListAdapter = new ContainerListAdapter(CentralUnitConnection.getInstance());
            }
        }else{
            Log.d(TAG, "Set the root container in listadapter");
            containerListAdapter = new ContainerListAdapter(CentralUnitConnection.getInstance());
            textViewHeader.setText(CentralUnitConnection.getInstance().getName());
        }

        listView.setAdapter(containerListAdapter);
        containerListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
            }
        });

        if (itemIntent != null && intentContainerId != -1 && intentContainerUrl != null) {
            Log.d(TAG, "Using intents url");
            try {
                //Try using url that was in intents extra
                url = new URL(intentContainerUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.d(TAG, "There was a malformed url in intents extra, try using default value");
                try {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                    String settingsUrl = preferences.getString("setting_ip_address", getString(R.string.default_address));
                    if (null != settingsUrl && settingsUrl.length() > 0) {
                        Log.d(TAG, "Address from settings: " + settingsUrl);
                    }
                    url = new URL(settingsUrl);
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
            //( (Container) CentralUnitConnection.getInstance().getItemById(intentContainerId)).startListening();
        }else{
            Log.d(TAG, "Container was launched from launcher - try to fetch url from settings");
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String settingsUrl = preferences.getString("setting_ip_address", getString(R.string.default_address));
                if (null != settingsUrl && settingsUrl.length() > 0) {
                    Log.d(TAG, "Address from settings: " + settingsUrl);
                }
                //url = new URL(settingsUrl);
                url = new URL("http://10.0.2.2:18001");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.d(TAG, "There was a malformed url in intents extra, try using default value");
                try {
                    url = new URL("");
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
            Log.d(TAG, "........ INITIALIZE INSTANCE ..........");
            CentralUnitConnection.getInstance().initialize(url);
            Log.d(TAG, "........ START LISTENING! ..........");
            CentralUnitConnection.getInstance().startListening();
        }

        //Checking the clicks of list view
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("ContainerActivity", "Clicked id: " + id);
                if (CentralUnitConnection.getInstance().getItemById(id) instanceof Device) {
                    Intent deviceIntent = new Intent(ContainerActivity.this, DeviceActivity.class);
                    deviceIntent.putExtra(DeviceActivity.EXTRA_DEVICE_ID, id);
                    deviceIntent.putExtra(DeviceActivity.EXTRA_CENTRAL_UNIT_URL, "http://10.0.2.2:18001");
                    Log.d(TAG, "Added extras - id: " + id + " and url: " + "http://10.0.2.2:18001");
                    startActivity(deviceIntent);
                } else if (CentralUnitConnection.getInstance().getItemById(id) instanceof Container) {
                    Log.d(TAG, "-- Clicked another container --");
                    Intent containerIntent = new Intent(ContainerActivity.this, ContainerActivity.class);
                    containerIntent.putExtra(EXTRA_CONTAINER_ID, id);
                    containerIntent.putExtra(EXTRA_CENTRAL_UNIT_URL, "http://10.0.2.2:18001");
                    Log.d(TAG, "Container extras - id: " + id + " and url: " + "http://10.0.2.2:18001");
                    startActivity(containerIntent);
                } else {
                    //Not container nor device, huh?
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent itemIntent = getIntent();
        long intentContainerId = itemIntent.getLongExtra(EXTRA_CONTAINER_ID, -1);

        Log.d(TAG, "onDestroy activity");
        Log.d(TAG, "onDestroy Intent received" + intentContainerId);
        Log.d(TAG, "Stopping listening the instance.");
        if(intentContainerId != -1){
            if(intentContainerId != 0){
                ((Container) CentralUnitConnection.getInstance().getItemById(intentContainerId)).stopListening();
            }else{
                CentralUnitConnection.getInstance().stopListening();
            }
        }else{
            CentralUnitConnection.getInstance().stopListening();
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
                Log.w("ContainerActivity", "Error, Unable to start action_settings activity");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
