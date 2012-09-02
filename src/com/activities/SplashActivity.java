package com.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SplashActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
	}

	public void onOkSelect(View view) {
		Intent loadGame = new Intent(this, GameActivity.class);
		loadGame.putExtra("LEVEL", 1);
		startActivity(loadGame);
	}
}
