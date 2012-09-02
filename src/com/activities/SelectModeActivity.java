package com.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class SelectModeActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.options);
	}
	
	public void onPracticeSelect(View view){
		Intent loadLevelSelect = new Intent(this, LevelSelectActivity.class);
		loadLevelSelect.putExtra("LEVEL_NUMBER", 1);
		
		startActivity(loadLevelSelect);
	}
	
	public void onBackPressed(){
		Intent intent = new Intent(this, SelectModeActivity.class);
		startActivity(intent);
	}
}
