package com.activities;

import java.util.ArrayList;

import android.app.ListActivity;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.components.GlobalState;

public class SettingsActivity extends ListActivity {

	private Location location;
	private ArrayAdapter<Location> adapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		adapter= new ArrayAdapter<Location>(this,
	            R.layout.device_entry,
	            GlobalState.hills);
		ListView list = (ListView) findViewById(R.id.holeList);
		setListAdapter(adapter);
		
	}
	
	
	
	public void onAddPressed(View view){
		GlobalState.hills.add(location);
	}
	
	public void onClearPressed(View view){
		GlobalState.hills = new ArrayList<Location>();
	}

	private class MyLocationListener implements LocationListener {

		public void onLocationChanged(Location loc) {
			if (location == null) {
				location = loc;
			}

			String values = Double.toString(loc.getLatitude()) + "   "
					+ Double.toString(loc.getLongitude());
			TextView coord = (TextView) findViewById(R.id.currentCoord);
			coord.setText(values);
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO Auto-generated method stub

		}

		@Override
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
