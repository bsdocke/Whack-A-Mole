package com.activities;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.components.PlayerSettings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.IBluetooth;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.TextView;
import fitnessapps.acceltest.activity.IAccelRemoteService;

public class GameActivity extends Activity {

	private static final int MOLE_INTERVAL = 30000;
	private static final int MIN_GPS_TIME_INTERVAL = 500;
	private static final int MIN_GPS_DISTANCE_INTERVAL = 0;

	private LocationManager manager;
	private Activity parentActivity;

	private int level = 1;
	private int score;
	private String lastLocString = "";

	private Timer moleTimer;
	private MoleSwitchTask moleTask;
	private Location location;
	private MyLocationListener locListener;

	private Timer twoMinuteExtensionTimer;
	private Timer oneMinuteExtensionTimer;
	private Timer endTimer;
	private EndTask endTask;
	private TwoMinuteExtensionTask twoMinuteExtensionTask;
	private OneMinuteExtensionTask oneMinuteExtensionTask;

	private RemoteServiceConnection conn;
	private IAccelRemoteService remoteService;

	private BluetoothAdapter adapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gps_whackamole);
	}

	public void onStart() {
		super.onStart();
		level = getIntent().getIntExtra("LEVEL", Short.MIN_VALUE);

		parentActivity = this;
		adapter = BluetoothAdapter.getDefaultAdapter();
		if (!adapter.isEnabled()) {
			registerListener(BluetoothAdapter.ACTION_STATE_CHANGED);
		} else {
			Process p;
			try {
				p = Runtime.getRuntime().exec("su");
				if (isMoleLeader()) {
					ensureBluetoothDiscoverability(120);
					initLeader();
					startLevelTimeChain();
				} else {
					initNormal();
					registerListener(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
					registerListener(BluetoothDevice.ACTION_FOUND);

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private boolean isMoleLeader() {
		return PlayerSettings.isMoleLeader;
	}

	protected void ensureBluetoothDiscoverability(int duration) {

		try {

			IBluetooth mBtService = getIBluetooth();

			Log.d("TESTE", "Ensuring bluetoot is discoverable");
			if (mBtService.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
				Log.e("TESTE", "Device was not in discoverable mode");
				try {
					mBtService.setDiscoverableTimeout(duration);
					mBtService
							.setScanMode(
									BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE,
									1000);
				} catch (Exception e) {
					Log.e("TESTE", "Error setting bt discoverable", e);
				}
				Log.i("TESTE", "Device must be discoverable");
			} else {
				Log.e("TESTE", "Device already discoverable");
			}
		} catch (Exception e) {
			Log.e("TESTE", "Error ensuring BT discoverability", e);
		}

	}

	private IBluetooth getIBluetooth() {

		IBluetooth ibt = null;

		try {

			Class c2 = Class.forName("android.os.ServiceManager");

			Method m2 = c2.getDeclaredMethod("getService", String.class);
			IBinder b = (IBinder) m2.invoke(null, "bluetooth");

			Class c3 = Class.forName("android.bluetooth.IBluetooth");

			Class[] s2 = c3.getDeclaredClasses();

			Class c = s2[0];
			Method m = c.getDeclaredMethod("asInterface", IBinder.class);
			m.setAccessible(true);
			ibt = (IBluetooth) m.invoke(null, b);

		} catch (Exception e) {
			Log.e("BluetoothError", "Erroraco!!! " + e.getMessage());
		}

		return ibt;
	}

	private void initLeader() {
		initLocationManager();
		initMoleTimers();
		initLevelTiming();
		if (isAccelServiceRunning()) {
			bindService();
		}
	}

	private void initNormal() {
		initLocationManager();
		initLevelTiming();
		adapter.startDiscovery();

		endTimer.schedule(endTask, 300000);
		bindService();
	}

	private void initLocationManager() {
		locListener = new MyLocationListener();
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_GPS_TIME_INTERVAL, MIN_GPS_DISTANCE_INTERVAL, locListener);
	}

	private void initMoleTimers() {
		moleTimer = new Timer();
		moleTask = new MoleSwitchTask();
		moleTimer.scheduleAtFixedRate(moleTask, MOLE_INTERVAL, MOLE_INTERVAL);
	}

	private void initLevelTiming() {
		initLevelTimers();
		initLevelTasks();
	}

	private void initLevelTimers() {
		twoMinuteExtensionTimer = new Timer();
		oneMinuteExtensionTimer = new Timer();
		endTimer = new Timer();
	}

	private void initLevelTasks() {
		twoMinuteExtensionTask = new TwoMinuteExtensionTask();
		oneMinuteExtensionTask = new OneMinuteExtensionTask();
		endTask = new EndTask();
	}

	private void startLevelTimeChain() {
		twoMinuteExtensionTimer.schedule(twoMinuteExtensionTask, 122000);
	}

	public void onBackPressed() {
		super.onBackPressed();
		stopGPS();
		cancelTimers();
	}

	public void onStop() {
		// unregisterReceiver(discoverReceiver);
		stopGPS();
		cancelTimers();
		releaseService();

		super.onStop();
	}

	private void stopGPS() {
		manager.removeUpdates(locListener);
	}

	private void cancelTimers() {
		moleTimer.cancel();
		moleTask.cancel();

		endTimer.cancel();
		twoMinuteExtensionTimer.cancel();
		oneMinuteExtensionTimer.cancel();

		twoMinuteExtensionTask.cancel();
		oneMinuteExtensionTask.cancel();
		endTask.cancel();
	}

	private void setViewTexts() {
		setLatitudeText();
		setLongitudeText();
		setAccuracyText();
	}

	private TextView getLatitudeView() {
		return (TextView) findViewById(R.id.latitude);
	}

	private TextView getLongitudeView() {
		return (TextView) findViewById(R.id.longitude);
	}

	private TextView getAccuracyView() {
		return (TextView) findViewById(R.id.accuracy);
	}

	private void setLatitudeText() {
		Log.e("SET LATITUDE", "Latitude view set to " + location.getLatitude());
		TextView latView = getLatitudeView();
		latView.setText("Current Latitude: "
				+ Double.toString(location.getLatitude()));
	}

	private void setLongitudeText() {
		Log.e("SET LONGITUDE",
				"Longitude view set to " + location.getLongitude());
		TextView longView = getLongitudeView();
		longView.setText("Current Longitude: "
				+ Double.toString(location.getLongitude()));
	}

	private void setAccuracyText() {
		TextView accView = getAccuracyView();
		Log.e("SET ACCURACY", "Accuracy view set to " + location.getAccuracy());
		accView.setText("Current Accuracy: "
				+ Float.toString(location.getAccuracy()));
	}

	private void setCurrLatitudeText(String val) {
		Log.e("SET LATITUDE", "Latitude view set to " + location.getLatitude());
		TextView latView = (TextView) findViewById(R.id.currLat);
		latView.setText("Current Latitude: " + val);
	}

	private void setCurrLongitudeText(String val) {
		Log.e("SET LONGITUDE",
				"Longitude view set to " + location.getLongitude());
		TextView longView = (TextView) findViewById(R.id.currLong);
		longView.setText("Current Longitude: " + val);
	}

	private void setCurrAccuracyText(String val) {
		TextView accView = (TextView) findViewById(R.id.currAcc);
		Log.e("SET ACCURACY", "Accuracy view set to " + location.getAccuracy());
		accView.setText("Current Accuracy: " + val);
	}

	private boolean isInitialLocationSet() {
		return location != null;
	}

	private void updateBluetoothName() {
		if (adapter.isEnabled() && isMoleLeader()) {
			adapter.setName("MOLELEADER:" + location.getLatitude() + "_"
					+ location.getLongitude());
		}
	}

	protected void registerListener(String action) {
		IntentFilter filter = new IntentFilter(action);
		registerReceiver(discoverReceiver, filter);
	}

	protected BroadcastReceiver initReceiver() {
		return new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
					if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
							Short.MIN_VALUE) == BluetoothAdapter.STATE_ON) {
						Process p;
						try {
							p = Runtime.getRuntime().exec("su");
							ensureBluetoothDiscoverability(120);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
				} else if (action.equals(BluetoothDevice.ACTION_FOUND)
						&& !intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
								.equals(lastLocString)) {
					lastLocString = intent
							.getStringExtra(BluetoothDevice.EXTRA_NAME);
					String rawLocationString = lastLocString.split(":")[1];
					String[] coords = rawLocationString.split("_");

					location = new Location("updatedFromNonLeader");
					location.setLatitude(Double.parseDouble(coords[0]));
					location.setLongitude(Double.parseDouble(coords[1]));
					setViewTexts();
				}
			}
		};
	}

	/******************** Remote Service ***************************/
	private void bindService() {
		if (conn == null) {
			conn = new RemoteServiceConnection();
			Intent i = new Intent();
			i.setClassName("fitnessapps.acceltest.activity",
					"fitnessapps.acceltest.activity.AccelerometerService");
			bindService(i, conn, Context.BIND_AUTO_CREATE);
		}
	}

	private void releaseService() {
		if (conn != null) {
			unbindService(conn);
			conn = null;
		}
	}

	private boolean isAccelServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("fitnessapps.acceltest.activity.AccelerometerService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	class RemoteServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName className,
				IBinder boundService) {
			remoteService = IAccelRemoteService.Stub
					.asInterface((IBinder) boundService);
			try {
				remoteService.setGameNameFromService("Whack-A-Mole");
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			Log.d(getClass().getSimpleName(), "onServiceConnected()");
		}

		public void onServiceDisconnected(ComponentName className) {
			remoteService = null;
			Log.d(getClass().getSimpleName(), "onServiceDisconnected");
		}
	};

	/***************** End Remote Service ******************************/

	protected final BroadcastReceiver discoverReceiver = initReceiver();

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			// Log.e("LOCATION FOUND", "At least the thing is updating...?");
			if (!isInitialLocationSet()) {
				// Log.e("LOCATION INITIALIZED", "Set new location for all");
				if (isMoleLeader()) {
					Random rand = new Random();
					int latChange = -20 + (int) (Math.random() * ((40) + 1));
					// location = loc;
					location = new Location("computedLocation");
					location.setLatitude(loc.getLatitude()
							+ (latChange / 100000));
					location.setLongitude(loc.getLongitude()
							+ (latChange / 100000));
					location.setAccuracy(loc.getAccuracy());
					setViewTexts();
					updateBluetoothName();
				}
			} else {
				setCurrLatitudeText(Double.toString(loc.getLatitude()));
				setCurrLongitudeText(Double.toString(loc.getLongitude()));
				setCurrAccuracyText(Float.toString(loc.getAccuracy()));
				if (Math.abs(loc.getLatitude() - location.getLatitude()) < 0.00005
						&& Math.abs(loc.getLongitude()
								- location.getLongitude()) < 0.00005) {
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					vib.vibrate(100);
					score += 20;
				}
			}

		}

		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
	}

	private class MoleSwitchTask extends TimerTask {

		@Override
		public void run() {
			location = null;
		}

	}

	private class TwoMinuteExtensionTask extends TimerTask {
		@Override
		public void run() {
			Process p;
			try {
				Log.e("TIME EXTENDED", "2 minutes");
				p = Runtime.getRuntime().exec("su");
				ensureBluetoothDiscoverability(120);
				oneMinuteExtensionTimer
						.schedule(oneMinuteExtensionTask, 122000);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class OneMinuteExtensionTask extends TimerTask {
		@Override
		public void run() {
			Process p;
			try {
				Log.e("TIME EXTENDED", "1 minute");
				p = Runtime.getRuntime().exec("su");
				ensureBluetoothDiscoverability(60);
				endTimer.schedule(endTask, 62000);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class EndTask extends TimerTask {
		@Override
		public void run() {

			Intent intent = new Intent(parentActivity, EndLevelActivity.class);
			if (!isMoleLeader()) {
				adapter.cancelDiscovery();
			}
			intent.putExtra("SCORE", score);
			parentActivity.startActivity(intent);
		}
	}
}
