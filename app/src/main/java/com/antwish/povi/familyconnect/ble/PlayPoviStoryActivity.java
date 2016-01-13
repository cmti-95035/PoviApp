package com.antwish.povi.familyconnect.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.antwish.povi.familyconnect.DashboardActivity;
import com.antwish.povi.familyconnect.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class PlayPoviStoryActivity extends AppCompatActivity implements BleWrapperUiCallbacks{

    private PeripheralActivity.ListType mListType = PeripheralActivity.ListType.POVI_STORY;

    private BleWrapper mBleWrapper;
    private ListView mListView;
    private View mListViewHeader;

    private Toolbar mToolbar;
    private List<String> poviStories;
    private SimpleAdapter storyAdapter;
    private String mDeviceAddress;

    public void uiDeviceConnected(final BluetoothGatt gatt,
                                  final BluetoothDevice device)
    {
    }

    public void uiDeviceDisconnected(final BluetoothGatt gatt,
                                     final BluetoothDevice device)
    {
    }

    public void uiNewRssiAvailable(final BluetoothGatt gatt,
                                   final BluetoothDevice device,
                                   final int rssi)
    {
    }

    public void uiAvailableServices(final BluetoothGatt gatt,
                                    final BluetoothDevice device,
                                    final List<BluetoothGattService> services)
    {
    }

    public void uiCharacteristicForService(final BluetoothGatt gatt,
                                           final BluetoothDevice device,
                                           final BluetoothGattService service,
                                           final List<BluetoothGattCharacteristic> chars)
    {
    }

    public void uiCharacteristicsDetails(final BluetoothGatt gatt,
                                         final BluetoothDevice device,
                                         final BluetoothGattService service,
                                         final BluetoothGattCharacteristic characteristic)
    {
    }

    public void uiNewValueForCharacteristic(final BluetoothGatt gatt,
                                            final BluetoothDevice device,
                                            final BluetoothGattService service,
                                            final BluetoothGattCharacteristic characteristic,
                                            final String strValue,
                                            final int intValue,
                                            final byte[] rawValue,
                                            final String timestamp)
    {
    }

    public void uiSuccessfulWrite(final BluetoothGatt gatt,
                                  final BluetoothDevice device,
                                  final BluetoothGattService service,
                                  final BluetoothGattCharacteristic ch,
                                  final String description)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Successfully sent command to device to play Povi Story", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void uiFailedWrite(final BluetoothGatt gatt,
                              final BluetoothDevice device,
                              final BluetoothGattService service,
                              final BluetoothGattCharacteristic ch,
                              final String description)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Writing to " + description + " FAILED!", Toast.LENGTH_LONG).show();
            }
        });
    }


    public void uiGotNotification(final BluetoothGatt gatt,
                                  final BluetoothDevice device,
                                  final BluetoothGattService service,
                                  final BluetoothGattCharacteristic ch)
    {
    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
        // no need to handle that in this Activity (here, we are not scanning)
    }

    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            --position; // we have header so we need to handle this by decrementing by one
//            if(position < 0) { // user have clicked on the header - action: going to dashboard
//                Intent nextActivity = new Intent(getApplicationContext(), DashboardActivity.class);
//                startActivity(nextActivity);
//            }
//            else { // user clicks on one story, send via BLE to play
//
//            }

            byte[] bytes = new byte[1];
            // the index number of the mp3 files to be played is off by 1
            bytes[0] = (byte)(position + 1);
            if(PeripheralActivity.characteristicList != null && PeripheralActivity.characteristicList.size() > 4)
                mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(3), bytes);
            else
                Toast.makeText(getApplicationContext(), "Can't play story via BLE", Toast.LENGTH_SHORT).show();
        }
    };

    private void setUpToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        //CardView cardToolbar = (CardView) findViewById(R.id.cardToolbar);
        if (mToolbar != null) {
            if (mToolbar != null) {
                // Add top padding if required
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    final float scale = getResources().getDisplayMetrics().density;
                    int top = (int) (30 * scale + 0.5f);
                    mToolbar.setPadding(0,top,0,0);
                }
                setSupportActionBar(mToolbar);
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            setSupportActionBar(mToolbar);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_povi_story);
        setUpToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        mListViewHeader = (View) getLayoutInflater().inflate(R.layout.peripheral_list_services_header, null, false);


        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(PeripheralActivity.EXTRAS_DEVICE_NAME);

//        mListView.addHeaderView(mListViewHeader);

        poviStories = populateStories();
        populateListView(poviStories);
    }

    public void populateListView( List<String> stories) {
        String[] keys = new String[]{"name"};
        int[] uiComponents = {R.id.storyname};

        List<HashMap<String, Object>> storyData = new ArrayList<HashMap<String, Object>>(stories.size());

        // build the data for the table view.
        for (String story : stories) {
            HashMap<String, Object> d = new HashMap<String, Object>();
            d.put("name", story);
            storyData.add(d);
        }

        storyAdapter = new SimpleAdapter(this, storyData, R.layout.row_povi_story, keys, uiComponents);
        mListView = (ListView) findViewById(R.id.povistory_listView);
        mListView.setAdapter(storyAdapter);

        mListView.setOnItemClickListener(listClickListener);
    }
    // TODO: in the future this has to be retrieved from server
    // it may cache in the app as well to avoid pulling this content too frequently
    private List<String> populateStories(){
        List<String> stories = new ArrayList<>();
        stories.add("Playground");
        stories.add("Oreo");
        stories.add("Math Class");
        stories.add("Library");
        stories.add("Dropping Ice Cream");

        return stories;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(mBleWrapper == null) mBleWrapper = new BleWrapper(this, this);

        if(mBleWrapper.initialize() == false) {
            finish();
        }

        mListView.setAdapter(storyAdapter);
        mBleWrapper.connect(mDeviceAddress);
    };

    @Override
    protected void onPause() {
        super.onPause();

        mBleWrapper.stopMonitoringRssiValue();
        mBleWrapper.diconnect();
        mBleWrapper.close();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.peripheral, menu);
        if (mBleWrapper.isConnected()) {
            menu.findItem(R.id.device_connect).setVisible(false);
            menu.findItem(R.id.device_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.device_connect).setVisible(true);
            menu.findItem(R.id.device_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.device_connect:
                return true;
            case R.id.device_disconnect:
                mBleWrapper.diconnect();
                return true;
            case android.R.id.home:
                mBleWrapper.diconnect();
                mBleWrapper.close();
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void connectViewsVariables() {

    }


}
