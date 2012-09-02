package com.activities;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class DirectionsActivity extends Activity {
	private int level;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.directions);
		
		level = getIntent().getExtras().getInt("LEVEL_NUMBER", Short.MIN_VALUE);
		Process p;
		try{
			p = Runtime.getRuntime().exec("su");
		}
		catch(IOException e){
			//TODO nothing
		}
	}
	
	public void onOkSelect(View view){
		Intent loadGame = new Intent(this, GameActivity.class);
		loadGame.putExtra("LEVEL_NUMBER", level);
		
		startActivity(loadGame);
	}
}
