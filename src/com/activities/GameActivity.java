package com.activities;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.components.GlobalState;

import fitnessapps.acceltest.activity.IAccelRemoteService;

public class GameActivity extends Activity implements SensorEventListener {

	private static final int MOLE_INTERVAL = 30000;
	private static final int MIN_GPS_TIME_INTERVAL = 500;
	private static final int MIN_GPS_DISTANCE_INTERVAL = 0;
	private static final int WHACK_THRESHOLD = 60;
	private static final double RANGE_THRESHOLD = 0.00005;

	private LocationManager manager;

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

	public boolean onTarget = false;
	private SensorManager sensorManager;
	private Sensor sensorAccelerometer;
	public GameActivity parent;

	private String code = "";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gps_whackamole);
		parent = this;
	}

	public void onStart() {
		super.onStart();

		if(isAccelServiceRunning()){
			bindService();
		}
		
		hideCodeFields();
		setHillHandling();
		setupSensors();
		setNewCode();
		initLeader();
	}
	
	private void hideCodeFields(){
		hideCodeInput();
		hideMyCode();
	}
	
	private void setHillHandling(){
		setHillAdvancementAmount();
		setCurrentHill();
	}
	
	private void setHillAdvancementAmount(){
		int numHills = GlobalState.hills.size();
		Calendar c = Calendar.getInstance();
		hillAdvancementAmount = c.get(Calendar.DAY_OF_MONTH);

		if (hillAdvancementAmount >= numHills
				&& hillAdvancementAmount % numHills == 0) {
			hillAdvancementAmount++;
		}
	}
	
	private void setCurrentHill(){
		int numHills = GlobalState.hills.size();
		hillIndex += getHillIndex();
		hillIndex = hillIndex % numHills;
		GlobalState.currentHill = GlobalState.hills.get(hillIndex);
	}
	
	private void setupSensors(){
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorAccelerometer = (Sensor) sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, sensorAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
	}

	private void advanceLevel() {
		releaseService();
		level++;
		GlobalState.level++;
		setNewRequiredScore();
		setNewCode();
		setLevelText();
		
		if(isAccelServiceRunning()){
			bindService();
		}
	}
	
	private void setNewRequiredScore(){
		requiredScore += level * 50;
	}
	
	private void setNewCode(){
		code = "";
		
		code += getFirstCodePortion();
		code += getSecondCodePortion();
		code += getThirdCodePortion();
		code += getPadding(level);
		
		code = this.code.substring(0, GlobalState.numPlayers);
	}

	private String getFirstCodePortion() {
		int codeValue = level * 7 - 2;
		String codeFragment = getPadding(codeValue);

		return codeFragment;
	}
	
	private String getSecondCodePortion(){
		Calendar c = Calendar.getInstance();
		int codeValue = c.get(Calendar.MONTH) * level;
		String codeFragment = getPadding(codeValue);
		return codeFragment;
	}
	
	private String getThirdCodePortion(){
		Calendar c = Calendar.getInstance();
		int codeValue = c.get(Calendar.DAY_OF_MONTH) * level;
		String codeFragment = getPadding(codeValue);
		return codeFragment;
	}
	
	private String getPadding(int codeValue){
		String codeFragment = "";
		if (codeValue < 100) {
			codeFragment += "0";
		}
		if (codeValue < 10) {
			codeFragment += "0";
		}
		return codeFragment + codeValue;
	}
	
	private void setLevelText(){
		TextView levelText = (TextView) findViewById(R.id.levelField);
		levelText.setText("Level " + level);
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

	private TextView getScoreField() {
		TextView scoreView = (TextView) findViewById(R.id.scoreField);
		return scoreView;
	}

	private void setScoreField(int newScore) {
		TextView scoreView = getScoreField();
		scoreView.setText(Integer.toString(newScore));
	}

	private boolean hasRequiredScore() {
		return score >= requiredScore;
	}

	private boolean isWhack(SensorEvent event) {
		return (event.values[0] > WHACK_THRESHOLD
				|| event.values[1] > WHACK_THRESHOLD || event.values[2] > WHACK_THRESHOLD);
	}

	private void vibratePulse() {
		Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vib.vibrate(100);
	}

	private void showFinishedText() {
		TextView indicator = getScoreField();
		indicator.setText("Help your friends finish the level!");
	}

	private void showCodeInput() {
		EditText codeField = getCodeInput();
		codeField.setVisibility(View.VISIBLE);
	}

	private void showMyCode() {
		TextView code = (TextView) findViewById(R.id.myCode);
		code.setVisibility(View.VISIBLE);
		code.setText(getMyCodeSegment());
	}
	
	private void hideCodeInput(){
		EditText codeField = (EditText) findViewById(R.id.codeField);
		codeField.setVisibility(View.INVISIBLE);
	}
	
	private void hideMyCode(){
		TextView code = (TextView) findViewById(R.id.myCode);
		code.setVisibility(View.INVISIBLE);
	}

	private String getMyCodeSegment() {
		return parent.code.substring(GlobalState.playerNum - 1,
				GlobalState.playerNum);
	}

	private boolean isMatchingCode() {
		EditText codeField = getCodeInput();
		return code.equals(codeField.getText().toString());
	}

	private boolean canAdvanceToNextLevel() {
		return isMatchingCode() && hasRequiredScore();
	}

	private EditText getCodeInput() {
		return (EditText) findViewById(R.id.codeField);
	}

	private boolean isOnHill(Location loc) {
		return Math.abs(loc.getLatitude()
				- GlobalState.currentHill.getLatitude()) < RANGE_THRESHOLD
				&& Math.abs(loc.getLongitude()
						- GlobalState.currentHill.getLongitude()) < RANGE_THRESHOLD;
	}

	public void codeSubmit(View view) {
		if (canAdvanceToNextLevel()) {
			advanceLevel();
		}
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
		   conn.serviceAppendEndGame();
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
		    remoteService.setGameNameFromService("Whack-A-Mole " + " Level: "
		      + GlobalState.level);
		   } catch (RemoteException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		   }
		   Log.d(getClass().getSimpleName(), "onServiceConnected()");
		  }

		  public void onServiceDisconnected(ComponentName className) {

		   remoteService = null;
		   Log.d(getClass().getSimpleName(), "onServiceDisconnected");
		  }

		  public void serviceAppendEndGame() {
		   try {
		    remoteService.setEndGameFlagFromService(true);
		    remoteService.setGameNameFromService("Whack-A-Mole " + "Level: "
		      + GlobalState.level);
		   } catch (RemoteException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		   }
		  }
		 };



	/***************** End Remote Service ******************************/
	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {

			if (hasRequiredScore()) {
				showFinishedText();
				showMyCode();
				showCodeInput();
			}else{
				hideCodeInput();
				hideMyCode();
			}

			if (isOnHill(loc)) {
				vibratePulse();
				onTarget = true;
			} else {
				onTarget = false;
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
			hillIndex += getHillIndex();
			hillIndex = hillIndex % GlobalState.hills.size();
			GlobalState.currentHill = GlobalState.hills.get(hillIndex);
		}

	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void onSensorChanged(SensorEvent event) {
		if (isWhack(event)) {
			if (!hasRequiredScore() && onTarget) {
				score += 20;
				setScoreField(score);
			} else if (hasRequiredScore()) {
				score = requiredScore;
				setScoreField(score);
			}
		}

	}

}
