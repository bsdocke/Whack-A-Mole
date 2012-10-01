package com.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class SplashActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		default:
			Intent settings = new Intent(this, SettingsActivity.class);
			startActivity(settings);
			return true;
		}
	}

	public void onOkSelect(View view) {
		Intent loadGame = new Intent(this, GameActivity.class);
		loadGame.putExtra("LEVEL", 1);
		startActivity(loadGame);
	}
}
