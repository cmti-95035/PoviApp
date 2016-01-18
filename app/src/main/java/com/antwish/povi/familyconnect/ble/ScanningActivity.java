package com.antwish.povi.familyconnect.ble;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.antwish.povi.familyconnect.DashboardActivity;
import com.antwish.povi.familyconnect.R;

import java.io.InputStream;

public class ScanningActivity extends ListActivity {
	
	private static final long SCANNING_TIMEOUT = 5 * 1000; /* 5 seconds */
	private static final int ENABLE_BT_REQUEST_ID = 1;
	
	private boolean mScanning = false;
	private Handler mHandler = new Handler();
	private DeviceListAdapter mDevicesListAdapter = null;
	private BleWrapper mBleWrapper = null;
    public static final String POVI = "POVI";
    private static final Integer FAILURE = -1;
    private static final Integer SUCCESS = 0;
    private static final String TAG = ScanningActivity.class.getSimpleName();

    private ProgressDialog mDialog;
    private DetectPoviTask detectPoviTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // create BleWrapper with empty callback object except uiDeficeFound function (we need only that here) 
        mBleWrapper = new BleWrapper(this, new BleWrapperUiCallbacks.Null() {
        	@Override
        	public void uiDeviceFound(final BluetoothDevice device, final int rssi, final byte[] record) {
        		handleFoundDevice(device, rssi, record);
        	}
        });
        
        // check if we have BT and BLE on board
        if(mBleWrapper.checkBleHardwareAvailable() == false) {
        	bleMissing();
        }

        // create a ProgressDialog
        mDialog = new ProgressDialog(this);
        // set indeterminate style
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // set title and message
        mDialog.setTitle("Please wait");
        mDialog.setMessage("Checking POVI device...");
        mDialog.show();
        new NoDeviceTask().execute();
    }

    private class NoDeviceTask extends AsyncTask<String, String, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(Integer result){
            if(mDialog.isShowing())
                mDialog.dismiss();
            if(result == FAILURE){
                Toast.makeText(getApplicationContext(), "Don't find a POVI buddy device", Toast.LENGTH_LONG).show();
                final Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(intent);
            }
        }

        @Override
        protected Integer doInBackground(String... params){
            try {
                Thread.sleep(5000);
                if(mDevicesListAdapter!= null && !mDevicesListAdapter.isEmpty())
                    return SUCCESS;
            } catch (Exception ex) {
                Log.e(TAG, ex.getLocalizedMessage());
            }
            return FAILURE;
        }
    }
    private class DetectPoviTask extends AsyncTask<String, String, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected void onPostExecute(Integer result){
            if(mDialog.isShowing())
                mDialog.dismiss();
            if(result == FAILURE){
                Toast.makeText(getApplicationContext(), "Don't find a POVI buddy device", Toast.LENGTH_SHORT).show();
                final Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
                startActivity(intent);
            } else if(result == SUCCESS){
                final BluetoothDevice device = mDevicesListAdapter.getDevice(0);
                if (device == null) return;

                final Intent intent = new Intent(getApplicationContext(), PeripheralActivity.class);
                intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_RSSI, mDevicesListAdapter.getRssi(0));
                intent.putExtra(PeripheralActivity.EXTRAS_TYPE, getIntent().getStringExtra(PeripheralActivity.EXTRAS_TYPE));
                startActivity(intent);
            }
        }

        @Override
        protected Integer doInBackground(String... params){
            try {
                if(mDevicesListAdapter!= null && !mDevicesListAdapter.isEmpty())
                return SUCCESS;
            } catch (Exception ex) {
                Log.e(TAG, ex.getLocalizedMessage());
            }
            return FAILURE;
        }
    }
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// on every Resume check if BT is enabled (user could turn it off while app was in background etc.)
    	if(mBleWrapper.isBtEnabled() == false) {
			// BT is not turned on - ask user to make it enabled
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST_ID);
		    // see onActivityResult to check what is the status of our request
		}
    	
    	// initialize BleWrapper object
        mBleWrapper.initialize();
    	
    	mDevicesListAdapter = new DeviceListAdapter(this);
        setListAdapter(mDevicesListAdapter);
    	
        // Automatically start scanning for devices
    	mScanning = true;
		// remember to add timeout for scanning to not run it forever and drain the battery
		addScanningTimeout();    	
		mBleWrapper.startScanning();
		
        invalidateOptionsMenu();
    };
    
    @Override
    protected void onPause() {
    	super.onPause();
    	mScanning = false;    	
    	mBleWrapper.stopScanning();
    	invalidateOptionsMenu();
    	
    	mDevicesListAdapter.clearList();
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scanning, menu);

        if (mScanning) {
            menu.findItem(R.id.scanning_start).setVisible(false);
            menu.findItem(R.id.scanning_stop).setVisible(true);
            menu.findItem(R.id.scanning_indicator)
                .setActionView(R.layout.progress_indicator);

        } else {
            menu.findItem(R.id.scanning_start).setVisible(true);
            menu.findItem(R.id.scanning_stop).setVisible(false);
            menu.findItem(R.id.scanning_indicator).setActionView(null);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scanning_start:
            	mScanning = true;
            	mBleWrapper.startScanning();
                break;
            case R.id.scanning_stop:
            	mScanning = false;
            	mBleWrapper.stopScanning();
                break;
            case R.id.show_hr_demo_item:
            	startHRDemo();
            	break;
        }
        
        invalidateOptionsMenu();
        return true;
    }

    private void startHRDemo() {
        startActivity(new Intent(this, HRDemoActivity.class));    	
    }
    
    /* user has selected one of the device */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mDevicesListAdapter.getDevice(position);
        if (device == null) return;
        
        final Intent intent = new Intent(this, PeripheralActivity.class);
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        intent.putExtra(PeripheralActivity.EXTRAS_DEVICE_RSSI, mDevicesListAdapter.getRssi(position));
        intent.putExtra(PeripheralActivity.EXTRAS_TYPE, getIntent().getStringExtra(PeripheralActivity.EXTRAS_TYPE));

        if (mScanning) {
            mScanning = false;
            invalidateOptionsMenu();
            mBleWrapper.stopScanning();
        }

        startActivity(intent);
    }    
    
    /* check if user agreed to enable BT */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // user didn't want to turn on BT
        if (requestCode == ENABLE_BT_REQUEST_ID) {
        	if(resultCode == Activity.RESULT_CANCELED) {
		    	btDisabled();
		        return;
		    }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

	/* make sure that potential scanning will take no longer
	 * than <SCANNING_TIMEOUT> seconds from now on */
	private void addScanningTimeout() {
		Runnable timeout = new Runnable() {
            @Override
            public void run() {
            	if(mBleWrapper == null) return;
                mScanning = false;
                mBleWrapper.stopScanning();
                invalidateOptionsMenu();
            }
        };
        mHandler.postDelayed(timeout, SCANNING_TIMEOUT);
	}    

	/* add device to the current list of devices */
    private void handleFoundDevice(final BluetoothDevice device,
            final int rssi,
            final byte[] scanRecord)
	{
		// adding to the UI have to happen in UI thread
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
                // only display POVI device(s)
                if(device != null && device.getName() != null && device.getName().startsWith(POVI)) {
                    mDevicesListAdapter.addDevice(device, rssi, scanRecord);
                    mDevicesListAdapter.notifyDataSetChanged();
                }

                if(detectPoviTask == null){
                    detectPoviTask = new DetectPoviTask();
                    detectPoviTask.execute();
                }
			}
		});
	}	

    private void btDisabled() {
    	Toast.makeText(this, "Sorry, BT has to be turned ON for us to work!", Toast.LENGTH_LONG).show();
        finish();    	
    }
    
    private void bleMissing() {
    	Toast.makeText(this, "BLE Hardware is required but not available!", Toast.LENGTH_LONG).show();
        finish();    	
    }
}
