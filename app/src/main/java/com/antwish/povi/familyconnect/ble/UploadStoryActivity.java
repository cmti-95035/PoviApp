package com.antwish.povi.familyconnect.ble;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.antwish.povi.familyconnect.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UploadStoryActivity extends AppCompatActivity implements BleWrapperUiCallbacks{

    private static final String TAG = UploadStoryActivity.class.getSimpleName();
    private PeripheralActivity.ListType mListType = PeripheralActivity.ListType.POVI_STORY;

    private BleWrapper mBleWrapper;
    private ListView mListView;
    private View mListViewHeader;

    private Toolbar mToolbar;
    private List<String> poviStories;
    private SimpleAdapter storyAdapter;
    private String mDeviceAddress;
    private static boolean failedWrite = false;
    private static boolean startWriting = false;
    private static int lastCount = 5;  // the last one that's uploaded
    private static final byte START = 0x40;
    private static final byte STOP = 0x00;
    private static int byteCount = 0;
    private static int byteWritten = 0;

    private ProgressDialog mDialog;

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
        byteWritten++;
        Log.e(TAG, "Successfully written byte#" + byteWritten );
        if(byteWritten >= byteCount){
            if(mDialog.isShowing())
                mDialog.dismiss();
        }
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
                Log.e(TAG, "failed to write byte#" + byteWritten);
//                Toast.makeText(getApplicationContext(), "Writing to " + description + " FAILED!", Toast.LENGTH_LONG).show();
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

    private class UploadTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.show();
        }

        @Override
        protected void onPostExecute(String result){
            if(mDialog.isShowing())
                mDialog.dismiss();
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params){
            try {
                if(params == null || params.length < 2){
                    return "Missing parameters for uploading...";
                }
                String fileName = params[0];    // find the mp3 file locally by the full path here
                String fileIndex = params[1];

                int index = Integer.parseInt(fileIndex);
                byteCount = 0;
                // step 1. write to the board to start uploading
                byte[] bytes = new byte[1];
                bytes[0] = START;
                byteCount++;
                // force it to connect
                mBleWrapper.connect(mDeviceAddress);
                if (PeripheralActivity.characteristicList != null && PeripheralActivity.characteristicList.size() > 5)
                    mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(4), bytes);
                else
                    return "BLE connection error!";

                //step 2. the index number of the mp3 files to be uploaded
                bytes[0] = (byte) (1 + lastCount + index);
                byteCount++;
                mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(5), bytes);

                byte[] fileData = new byte[4];
                // step 3. write the bytes from file
//                for (int i = 0; i < 256; i++) {
//                    byteCount++;
//                    fileData[0] = (byte) i;
//                    fileData[1] = (byte) i;
//                    fileData[2] = (byte) i;
//                    fileData[3] = (byte) i;
//                    mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(5), fileData);
//                    Thread.sleep(0, 200000);
//                }

                InputStream inputStream = getResources().openRawResource(R.raw.treasurehunt);

                int count = 0;
                do{
                    count = inputStream.read(fileData);
                    if(count < 4)
                    {
                        // end of file reached
                        if(count > 0){
                            byte[] finalBytes = new byte[count];
                            for(int i = 0; i < count; i++)
                                finalBytes[i] = fileData[i];
                                                mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(5), finalBytes);

                        }
                    }else
                        mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(5), fileData);
                    Thread.sleep(0, 200000);
                }while(count == 4);
                // stop 4. write to the board to stop uploading and save file
                bytes[0] = STOP;
                byteCount++;
                mBleWrapper.writeDataToCharacteristic(PeripheralActivity.characteristicList.get(4), bytes);
                return "Successfully uploaded";
            } catch (Exception ex) {
                return "Something wrong..." + ex.getLocalizedMessage();
            }
        }
    }
    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            --position; // we have header so we need to handle this by decrementing by one


            //TODO: get the last count from server so it knows how many already uploaded
            new UploadTask().execute(" ", new Integer(position).toString());

        }
    };

    private void failedUpload(){
        Toast.makeText(getApplicationContext(), "Something wrong when uploading story via BLE", Toast.LENGTH_SHORT).show();
    }
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

        setContentView(R.layout.activity_upload_story);
        setUpToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        mListViewHeader = (View) getLayoutInflater().inflate(R.layout.peripheral_list_services_header, null, false);


        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(PeripheralActivity.EXTRAS_DEVICE_NAME);

//        mListView.addHeaderView(mListViewHeader);

        poviStories = populateStories();
        populateListView(poviStories);

        // create a ProgressDialog
        mDialog = new ProgressDialog(this);
        // set indeterminate style
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // set title and message
        mDialog.setTitle("Please wait");
        mDialog.setMessage("Uploading...");
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
        mListView = (ListView) findViewById(R.id.upload_povistory_listView);
        mListView.setAdapter(storyAdapter);

        mListView.setOnItemClickListener(listClickListener);
    }
    // TODO: in the future this has to be retrieved from server
    // it may cache in the app as well to avoid pulling this content too frequently
    private List<String> populateStories(){
        List<String> stories = new ArrayList<>();
        stories.add("1");
        stories.add("2");

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

//        mBleWrapper.stopMonitoringRssiValue();
//        mBleWrapper.diconnect();
//        mBleWrapper.close();
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

