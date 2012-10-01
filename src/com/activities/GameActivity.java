package com.activities;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.widget.TextView;

import com.components.GlobalState;

import fitnessapps.acceltest.activity.IAccelRemoteService;

public class GameActivity extends Activity implements
SensorEventListener{

	private static final int MOLE_INTERVAL = 30000;
	private static final int MIN_GPS_TIME_INTERVAL = 500;
	private static final int MIN_GPS_DISTANCE_INTERVAL = 0;

	private LocationManager manager;
	private GameActivity parentActivity;

	private int level = 1;
	private int score = 0;
	private int requiredScore = 100;
	private int hillIndex = 0;
	private int hillAdvancementAmount = 1;

	private Timer moleTimer;
	private MoleSwitchTask moleTask;
	private Location location;
	private MyLocationListener locListener;

	private RemoteServiceConnection conn;
	private IAccelRemoteService remoteService;

	private BluetoothAdapter adapter;
	public boolean onTarget = false;
	private SensorManager sensorManager;
	private Sensor sensorAccelerometer;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gps_whackamole);
	}

	public void onStart() {
		super.onStart();
		int numHills = GlobalState.hills.size();
		parentActivity = this;
		Calendar c = Calendar.getInstance();
		hillAdvancementAmount = c.get(Calendar.DAY_OF_MONTH);

		if (hillAdvancementAmount >= numHills
				&& hillAdvancementAmount % numHills == 0) {
			hillAdvancementAmount++;
		}

		hillIndex += getHillIndex();
		hillIndex = hillIndex % numHills;
		GlobalState.currentHill = GlobalState.hills.get(hillIndex);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccelerometer = (Sensor)sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, sensorAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
		
		initLeader();

	}

	public int getHillIndex() {
		return this.hillAdvancementAmount;
	}

	private void initLeader() {

		initLocationManager();
		initMoleTimers();
		if (isAccelServiceRunning()) {
			bindService();
		}
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

	public void onBackPressed() {
		super.onBackPressed();
		stopGPS();
		cancelTimers();
	}

	public void onStop() {
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
		if (GlobalState.currentHill != null) {
			TextView latView = getLatitudeView();
			latView.setText("Current Latitude: "
					+ Double.toString(GlobalState.currentHill.getLatitude()));
		}
	}

	private void setLongitudeText() {
		if (GlobalState.currentHill != null) {
			TextView longView = getLongitudeView();
			longView.setText("Current Longitude: "
					+ Double.toString(GlobalState.currentHill.getLongitude()));
		}
	}

	private void setAccuracyText() {
		TextView accView = getAccuracyView();
		Log.e("SET ACCURACY", "Accuracy view set to " + location.getAccuracy());
		accView.setText("Current Accuracy: "
				+ Float.toString(location.getAccuracy()));
	}

	private void setCurrLatitudeText(String val) {
		
		TextView latView = (TextView) findViewById(R.id.currLat);
		latView.setText("Current Latitude: " + val);
	}

	private void setCurrLongitudeText(String val) {

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

	protected void registerListener(String action) {
		IntentFilter filter = new IntentFilter(action);
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
	
	private void accelerometerHandler(SensorEvent event) {
		
	}

	

	/***************** End Remote Service ******************************/
	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {

			if (score >= requiredScore) {
				TextView indicator = (TextView) findViewById(R.id.textView1);
				indicator.setText("Help your friends finish the level!");
			}

			if (Math.abs(loc.getLatitude()
					- GlobalState.currentHill.getLatitude()) < 0.00005
					&& Math.abs(loc.getLongitude()
							- GlobalState.currentHill.getLongitude()) < 0.00005) {
				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vib.vibrate(100);
				onTarget  = true;
				//if (score < requiredScore) {
				//	score += 20;
			//	}
			}
			else{
				onTarget = false;
			}

			setLatitudeText();
			setLongitudeText();
			setCurrLatitudeText(Double.toString(loc.getLatitude()));
			setCurrLongitudeText(Double.toString(loc.getLongitude()));
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
			hillIndex += getHillIndex();
			hillIndex = hillIndex % GlobalState.hills.size();
			GlobalState.currentHill = GlobalState.hills.get(hillIndex);
		}

	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onSensorChanged(SensorEvent event) {
		if(event.values[0] > 20 || event.values[1] > 20 || event.values[2] > 20){
			if(score < requiredScore)
				score += 20;
		}
		
	}

}
