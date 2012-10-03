package com.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.components.GPSData;
import com.components.GlobalState;

public class SettingsActivity extends Activity {

	private static final long MIN_GPS_TIME_INTERVAL = 500;
	private static final float MIN_GPS_DISTANCE_INTERVAL = 0;
	private Location location;
	private ArrayAdapter<GPSData> adapter;
	private MyLocationListener locListener;
	private LocationManager manager;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		GlobalState.hills = new ArrayList<GPSData>();

		adapter = new ArrayAdapter<GPSData>(this, R.layout.device_entry,
				GlobalState.hills);
		ListView list = (ListView) findViewById(R.id.coordList);
		list.setAdapter(adapter);

	}

	public void onStart() {
		super.onStart();
		locListener = new MyLocationListener();
		manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				MIN_GPS_TIME_INTERVAL, MIN_GPS_DISTANCE_INTERVAL, locListener);
	}

	public void onBackPressed() {
		super.onBackPressed();
		EditText num = (EditText) findViewById(R.id.editview);
		EditText playerNum = (EditText) findViewById(R.id.editview2);
		
		if (num.getText().length() > 0) {
			GlobalState.numPlayers = Integer.parseInt(num.getText().toString());
		}
		if(playerNum.getText().length() > 0){
			GlobalState.playerNum = Integer.parseInt(playerNum.getText().toString());
		}
		stopGPS();
	}

	private void stopGPS() {
		manager.removeUpdates(locListener);
	}

	public void onAddPressed(View view) {
		if (location != null) {
			GPSData data = new GPSData();
			data.setLatitude(location.getLatitude());
			data.setLongitude(location.getLongitude());
			GlobalState.hills.add(data);
			adapter.notifyDataSetChanged();
		}
	}

	public void onClearPressed(View view) {
		GlobalState.hills = new ArrayList<GPSData>();
		adapter.clear();
		adapter.notifyDataSetChanged();
		onBackPressed();
	}

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			
			location = loc;

			String values = Double.toString(loc.getLatitude()) + "   "
					+ Double.toString(loc.getLongitude());
			TextView coord = (TextView) findViewById(R.id.currentCoord);
			coord.setText(values);
		}

		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub

		}

		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub

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
