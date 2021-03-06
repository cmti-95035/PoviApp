package com.antwish.povi.familyconnect.ble;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.antwish.povi.familyconnect.DashboardActivity;
import com.antwish.povi.familyconnect.R;

public class PeripheralActivity extends AppCompatActivity implements BleWrapperUiCallbacks {
//public class PeripheralActivity extends Activity implements BleWrapperUiCallbacks {
    public static final String EXTRAS_DEVICE_NAME    = "BLE_DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "BLE_DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_RSSI    = "BLE_DEVICE_RSSI";
    public static final String EXTRAS_TYPE    = "ACTION_TYPE";

	public static final long IOService = 0x1815L;
	public static final long DigitalOutCharacteristic = 0x2a56L;
    
    public enum ListType {
		POVI_STORY,
    	GATT_SERVICES,
    	GATT_CHARACTERISTICS,
    	GATT_CHARACTERISTIC_DETAILS
    }
    
    private ListType mListType = ListType.GATT_SERVICES;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceRSSI;

    private BleWrapper mBleWrapper;
    
    private TextView mDeviceNameView;
    private TextView mDeviceAddressView;
    private TextView mDeviceRssiView;
    private TextView mDeviceStatus;
    private ListView mListView;
    private View     mListViewHeader;
    private TextView mHeaderTitle;
    private TextView mHeaderBackButton;
    private ServicesListAdapter mServicesListAdapter = null;
    private CharacteristicsListAdapter mCharacteristicsListAdapter = null; 
    private CharacteristicDetailsAdapter mCharDetailsAdapter = null;
	public static List<BluetoothGattCharacteristic> characteristicList = new ArrayList<>();
	public static String connectedDeviceAddress = null;

	public static boolean bleConnected = false;

	private Toolbar mToolbar;

	private ProgressDialog mDialog;
	private static final Integer FAILURE = -1;
	private static final Integer SUCCESS = 0;
	private static final String TAG = PeripheralActivity.class.getSimpleName();
    
    public void uiDeviceConnected(final BluetoothGatt gatt,
			                      final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("connected");
				bleConnected = true;
				invalidateOptionsMenu();
			}
    	});
    }
    
    public void uiDeviceDisconnected(final BluetoothGatt gatt,
			                         final BluetoothDevice device)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceStatus.setText("disconnected");
				mServicesListAdapter.clearList();
				mCharacteristicsListAdapter.clearList();
				mCharDetailsAdapter.clearCharacteristic();
				bleConnected = false;
				invalidateOptionsMenu();
				
				mHeaderTitle.setText("");
				mHeaderBackButton.setVisibility(View.INVISIBLE);
				mListType = ListType.GATT_SERVICES;
				mListView.setAdapter(mServicesListAdapter);
			}
    	});    	
    }
    
    public void uiNewRssiAvailable(final BluetoothGatt gatt,
    							   final BluetoothDevice device,
    							   final int rssi)
    {
    	runOnUiThread(new Runnable() {
	    	@Override
			public void run() {
				mDeviceRSSI = rssi + " db";
				mDeviceRssiView.setText(mDeviceRSSI);
			}
		});    	
    }
    
    public void uiAvailableServices(final BluetoothGatt gatt,
    						        final BluetoothDevice device,
    							    final List<BluetoothGattService> services)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mServicesListAdapter.clearList();
				mListType = ListType.GATT_SERVICES;
				mListView.setAdapter(mServicesListAdapter);
				mHeaderTitle.setText(mDeviceName + "\'s services:");
				mHeaderBackButton.setVisibility(View.INVISIBLE);
				
    			for(BluetoothGattService service : mBleWrapper.getCachedServices()) {
					// only display the related services
					if((service.getUuid().getMostSignificantBits() >> 32) == IOService)
            			mServicesListAdapter.addService(service);
            	}
    			mServicesListAdapter.notifyDataSetChanged();
			}    		
    	});
    }
   
    public void uiCharacteristicForService(final BluetoothGatt gatt,
    				 					   final BluetoothDevice device,
    									   final BluetoothGattService service,
    									   final List<BluetoothGattCharacteristic> chars)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCharacteristicsListAdapter.clearList();
		    	mListType = ListType.GATT_CHARACTERISTICS;
		    	mListView.setAdapter(mCharacteristicsListAdapter);
		    	mHeaderTitle.setText(BleNamesResolver.resolveServiceName(service.getUuid().toString().toLowerCase(Locale.getDefault())) + "\'s characteristics:");
		    	mHeaderBackButton.setVisibility(View.VISIBLE);

				characteristicList.clear();
				for (BluetoothGattCharacteristic ch : chars) {
					// only display the related characteristics
					if ((ch.getUuid().getMostSignificantBits() >> 32) == DigitalOutCharacteristic) {
						mCharacteristicsListAdapter.addCharacteristic(ch);
						characteristicList.add(ch);
					}

				}
				mCharacteristicsListAdapter.notifyDataSetChanged();

				String actionType = getIntent().getStringExtra(EXTRAS_TYPE);
//				Intent nextActivity = new Intent(getApplicationContext(), UploadStoryActivity.class);
				Intent nextActivity = new Intent(getApplicationContext(), actionType.equals("play") ? PlayPoviStoryActivity.class : UploadStoryActivity.class);
				nextActivity.putExtra(PeripheralActivity.EXTRAS_DEVICE_ADDRESS, connectedDeviceAddress);
				startActivity(nextActivity);
			}
    	});
    }
    
    public void uiCharacteristicsDetails(final BluetoothGatt gatt,
					 					 final BluetoothDevice device,
										 final BluetoothGattService service,
										 final BluetoothGattCharacteristic characteristic)
    {
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mListType = ListType.GATT_CHARACTERISTIC_DETAILS;
				mListView.setAdapter(mCharDetailsAdapter);
		    	mHeaderTitle.setText(BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault())) + "\'s details:");
		    	mHeaderBackButton.setVisibility(View.VISIBLE);
		    	
		    	mCharDetailsAdapter.setCharacteristic(characteristic);
		    	mCharDetailsAdapter.notifyDataSetChanged();
			}
    	});
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
    	if(mCharDetailsAdapter == null || mCharDetailsAdapter.getCharacteristic(0) == null) return;
    	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mCharDetailsAdapter.newValueForCharacteristic(characteristic, strValue, intValue, rawValue, timestamp);
				mCharDetailsAdapter.notifyDataSetChanged();
			}
    	});
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
				Toast.makeText(getApplicationContext(), "Writing to " + description + " was finished successfully!", Toast.LENGTH_LONG).show();
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
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				// at this moment we only need to send this "signal" do characteristic's details view
				mCharDetailsAdapter.setNotificationEnabledForService(ch);
			}			
		});
	}

	@Override
	public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
		// no need to handle that in this Activity (here, we are not scanning)
	}

	private class ConnectPoviTask extends AsyncTask<String, String, Integer> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		@Override
		protected void onPostExecute(Integer result){
			if(mDialog.isShowing())
				mDialog.dismiss();
			if(result == FAILURE){
				Toast.makeText(getApplicationContext(), "Failed to connect to a POVI buddy device", Toast.LENGTH_LONG).show();
				final Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
				startActivity(intent);
			}
		}

		@Override
		protected Integer doInBackground(String... params){
			try {
				Thread.sleep(5000);
				BluetoothGattService service = mServicesListAdapter.getService(0);
				mBleWrapper.getCharacteristicsForService(service);
				return SUCCESS;
			} catch (Exception ex) {
				Log.e(TAG, ex.getLocalizedMessage());
			}
			return FAILURE;
		}
	}
    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			--position; // we have header so we need to handle this by decrementing by one
			if(position < 0) { // user have clicked on the header - action: BACK
				if(mListType.equals(ListType.GATT_SERVICES)) return;
				if(mListType.equals(ListType.GATT_CHARACTERISTICS)) {
					uiAvailableServices(mBleWrapper.getGatt(), mBleWrapper.getDevice(), mBleWrapper.getCachedServices());
					mCharacteristicsListAdapter.clearList();
					return;
				}
				if(mListType.equals(ListType.GATT_CHARACTERISTIC_DETAILS)) {
					mBleWrapper.getCharacteristicsForService(mBleWrapper.getCachedService());
					mCharDetailsAdapter.clearCharacteristic();
					return;
				}
			}
			else { // user is going deeper into the tree (device -> services -> characteristics -> characteristic's details) 
				if(mListType.equals(ListType.GATT_SERVICES)) {
					BluetoothGattService service = mServicesListAdapter.getService(position);
					mBleWrapper.getCharacteristicsForService(service);
				}
				else if(mListType.equals(ListType.GATT_CHARACTERISTICS)) {
					BluetoothGattCharacteristic ch = mCharacteristicsListAdapter.getCharacteristic(position);
					uiCharacteristicsDetails(mBleWrapper.getGatt(), mBleWrapper.getDevice(), mBleWrapper.getCachedService(), ch);
				} 
			}
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

		setContentView(R.layout.activity_peripheral);
		setUpToolbar();
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//		getActionBar().setDisplayHomeAsUpEnabled(true);
		mListViewHeader = (View) getLayoutInflater().inflate(R.layout.peripheral_list_services_header, null, false);
		
		connectViewsVariables();
		
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		connectedDeviceAddress = mDeviceAddress;
        mDeviceRSSI = intent.getIntExtra(EXTRAS_DEVICE_RSSI, 0) + " db";
        mDeviceNameView.setText(mDeviceName);
        mDeviceAddressView.setText(mDeviceAddress);
        mDeviceRssiView.setText(mDeviceRSSI);
		getSupportActionBar().setTitle(mDeviceName);
//        getActionBar().setTitle(mDeviceName);

        mListView.addHeaderView(mListViewHeader);
        mListView.setOnItemClickListener(listClickListener);

		// create a ProgressDialog
		mDialog = new ProgressDialog(this);
		// set indeterminate style
		mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		// set title and message
		mDialog.setTitle("Please wait");
		mDialog.setMessage("Connecting POVI buddy...");
		mDialog.show();

		new ConnectPoviTask().execute();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mBleWrapper == null) mBleWrapper = new BleWrapper(this, this);
		
		if(mBleWrapper.initialize() == false) {
			finish();
		}
		
		if(mServicesListAdapter == null) mServicesListAdapter = new ServicesListAdapter(this);
		if(mCharacteristicsListAdapter == null) mCharacteristicsListAdapter = new CharacteristicsListAdapter(this);
		if(mCharDetailsAdapter == null) mCharDetailsAdapter = new CharacteristicDetailsAdapter(this, mBleWrapper);
		
		mListView.setAdapter(mServicesListAdapter);
		mListType = ListType.GATT_SERVICES;
		mHeaderBackButton.setVisibility(View.INVISIBLE);
		mHeaderTitle.setText("");
		
		// start automatically connecting to the device
    	mDeviceStatus.setText("connecting ...");
    	mBleWrapper.connect(mDeviceAddress);
	};
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mServicesListAdapter.clearList();
		mCharacteristicsListAdapter.clearList();
		mCharDetailsAdapter.clearCharacteristic();
		
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
            	mDeviceStatus.setText("connecting ...");
            	mBleWrapper.connect(mDeviceAddress);
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
    	mDeviceNameView = (TextView) findViewById(R.id.peripheral_name);
		mDeviceAddressView = (TextView) findViewById(R.id.peripheral_address);
		mDeviceRssiView = (TextView) findViewById(R.id.peripheral_rssi);
		mDeviceStatus = (TextView) findViewById(R.id.peripheral_status);
		mListView = (ListView) findViewById(R.id.listView);
		mHeaderTitle = (TextView) mListViewHeader.findViewById(R.id.peripheral_service_list_title);
		mHeaderBackButton = (TextView) mListViewHeader.findViewById(R.id.peripheral_list_service_back);
    }

}
