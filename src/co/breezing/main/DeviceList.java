package co.breezing.main;

import java.util.Set;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import co.breezing.metabolism.parameter.Parameter;
import co.breezing.module.R;
import co.breezing.module.six.bluetooth.Bluetooth;
import co.breezing.module.six.bluetooth.SaveMACFile;

/**
 * Displays a page with the list of Bluetooth devices paired with the phone.
 * User can choose particular device from the list to link it to the app. If the
 * device is not present, it can be scanned and paired from this page.
 */
public class DeviceList extends Activity {

	private static final String tag = "DeviceList";

	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	public static String EXTRA_DEVICE_NAME = "device_name";

	/** ArrayAdapter for devices which have already been paired */
	private ArrayAdapter<String> pairedDevicesArrayAdapter;
	/** ArrayAdapter for devices new Devices found */
	private ArrayAdapter<String> newDevicesArrayAdapter;

	public static BluetoothAdapter btAdap = null;
	private Bluetooth bluetooth = new Bluetooth();

	private boolean connectionEstablished = false;
	private boolean start_flag = false;
	private boolean finish_flag = false;

	public int connectionTime = 0;
	public static ProgressDialog pd;

	private Button scanButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		try {
			btAdap = Bluetooth.getInstance().getBluetoothAdapter();
			requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
			setContentView(R.layout.device_list);

			// Set result CANCELED in case the user backs out
			setResult(Activity.RESULT_CANCELED);

			// Initialize array adapters. One for already paired devices and
			// one for newly discovered devices
			pairedDevicesArrayAdapter = new ArrayAdapter<String>(this,
					R.layout.device_name);
			newDevicesArrayAdapter = new ArrayAdapter<String>(this,
					R.layout.device_name);

			// Find and set up the ListView for paired devices
			ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
			pairedListView.setAdapter(pairedDevicesArrayAdapter);
			pairedListView.setOnItemClickListener(mDeviceClickListener);

			// Find and set up the ListView for newly discovered devices
			ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
			newDevicesListView.setAdapter(newDevicesArrayAdapter);
			newDevicesListView.setOnItemClickListener(mDeviceClickListener);

			// Register for broadcasts when a device is discovered
			IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			this.registerReceiver(mReceiver, filter);

			// Register for broadcasts when discovery has finished
			filter = new IntentFilter(
					BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			this.registerReceiver(mReceiver, filter);

			// Get a set of currently paired devices
			Set<BluetoothDevice> pairedDevices = btAdap.getBondedDevices();

			// Initialize the button to perform device discovery
			scanButton = (Button) findViewById(R.id.button_scan);
			scanButton.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					newDevicesArrayAdapter.clear();
					doDiscovery();
				}
			});

			// If there are paired devices, add each one to the ArrayAdapter
			if (pairedDevices.size() > 0) {
				findViewById(R.id.title_paired_devices).setVisibility(
						View.VISIBLE);
				for (BluetoothDevice device : pairedDevices) {
					pairedDevicesArrayAdapter.add(device.getName() + "\n"
							+ device.getAddress());
				}
			}
			else {
				String noDevices = getResources().getText(R.string.none_paired)
						.toString();
				pairedDevicesArrayAdapter.add(noDevices);
			}
		}
		catch (Exception e) {
			Log.d(tag, "Exception in OnCreate Activity in DeviceList " + e);
		}
	}

	@Override
	/** Stop discovery mode and unregister broadcase listeners */
	protected void onDestroy() {
		try {
			super.onDestroy();

			// Make sure we're not doing discovery anymore
			if (btAdap != null) {
				btAdap.cancelDiscovery();
			}

			// Unregister broadcast listeners
			this.unregisterReceiver(mReceiver);
		}
		catch (Exception e) {
			Log.d(tag, "Exception in onDestroy " + e);
		}
	}

	/**
	 * Start device discover with the BluetoothAdapter
	 */
	private void doDiscovery() {
		Log.d(tag, "doDiscovery()");
		try {

			setProgressBarIndeterminateVisibility(true);
			setTitle("扫描设备中....");

			// Turn on sub-title for new devices
			findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

			// If we're already discovering, stop it
			if (btAdap.isDiscovering()) {
				btAdap.cancelDiscovery();
			}

			btAdap.startDiscovery();
			scanButton.setEnabled(false);
		}
		catch (Exception e) {
			Log.d(tag, "Exception in discovering " + e);
		}
	}

	/** The on-click listener for all devices in the ListViews */
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			try {
				// Cancel discovery because it's costly and we're about to
				// connect
				btAdap.cancelDiscovery();

				String info = ((TextView) v).getText().toString();
				String address = info.substring(info.length() - 17);

				// Create the result Intent and include the MAC address
				Intent intent = new Intent();

				intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

				// Set result and finish this Activity
				SaveMACFile.saveMAC(getBaseContext(), address);
				Parameter.MAC = address;

				connectionTime = 0;
				connectWithDevice(Parameter.MAC);
			}
			catch (Exception e) {
				Log.d(tag, "Exception in selecting device " + e);
			}
		}
	};

	/**
	 * The BroadcastReceiver that listens for discovered devices and changes the
	 * title when discovery is finished
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				String action = intent.getAction();

				// When discovery finds a device
				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					// Get the BluetoothDevice object from the Intent
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

					// If it's already paired, skip it, because it's been listed
					// already
					if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
						newDevicesArrayAdapter.add(device.getName() + "\n"
								+ device.getAddress());
					}
					// When discovery is finished, change the Activity title
				}
				else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
						.equals(action)) {
					scanButton.setText("刷新");
					scanButton.setEnabled(true);
					setProgressBarIndeterminateVisibility(false);
					setTitle("选择设备进行连接");
					if (newDevicesArrayAdapter.getCount() == 0) {
						newDevicesArrayAdapter.add("没有找到蓝牙设备");
					}
				}
			}
			catch (Exception e) {
				Log.d(tag, "Exception in onReceive " + e);
			}
		}
	};

	/**
	 * functino to connect with device and show the progress dialog
	 */
	public void connectWithDevice(String address) {
		finish_flag = false;
		start_flag = false;
		BTConnectionTask btTask = new BTConnectionTask();
		btTask.execute();
		pd = new ProgressDialog(DeviceList.this);
		pd.setCanceledOnTouchOutside(false);
		pd.setMessage("正在连接设备" + " (" + address + ")");
		pd.setIndeterminate(true);
		pd.setCancelable(true);
		pd.setButton(DialogInterface.BUTTON_NEGATIVE, "停止连接",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						disconnectWithDevice();
						pd.dismiss();
						pd.cancel();
					}
				});
		if (!pd.isShowing())
			pd.show();
	}

	/**
	 * function to disconnect with device
	 */
	public void disconnectWithDevice() {
		connectionTime = 3;
		bluetooth.disconnectBTSocket();
	}

	public class BTConnectionTask extends AsyncTask<Void, Void, Boolean> {

		protected Boolean doInBackground(Void... x) {
			connectionEstablished = false;
			connectionEstablished = bluetooth.connectBTSocket(Parameter.MAC);
			return connectionEstablished;
		}

		protected void onPostExecute(Boolean result) {
			Log.d(tag, "Value of connectionetablished on postExecute: "
					+ connectionEstablished);
			if (connectionEstablished == true) {
				// initial results_flag as 0
				// metabolismAlgo.results_flag = 0;
				if (finish_flag != true) {
					Intent myIntent = new Intent(getBaseContext(),
							Metabolism.class);
					startActivityForResult(myIntent, 0);
					pd.dismiss();
					pd.cancel();
					finish();
				}
			}
			else {
				connectionTime++;
				if (connectionTime < 3) {
					pd.dismiss();
					pd.cancel();
					connectWithDevice(Parameter.MAC);
				}
				else {
					disconnectWithDevice();
					pd.dismiss();
					pd.cancel();
					Toast.makeText(getBaseContext(),
							"连接Breezing设备失败。请检查是否选择正确或是否已打开Breezing设备。",
							Toast.LENGTH_LONG).show();
				}
			}
		}
	}
}