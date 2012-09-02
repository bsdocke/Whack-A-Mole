package com.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class LevelSelectActivity extends Activity {
	private int level;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.levelselect);

		level = getIntent().getExtras().getInt("LEVEL_NUMBER", 0);
	}

	public void onLevelSelect(View view) {
		Intent loadDirections = new Intent(this, DirectionsActivity.class);
		Button selectedButton = (Button) view;
		int levelSelected = Integer.parseInt(selectedButton.getText()
				.toString());
		
		if (level < levelSelected) {
			Context context = getApplicationContext();
			CharSequence text = "You must complete level " + level + " first.";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		} else {
			loadDirections.putExtra("LEVEL_NUMBER",
					Integer.parseInt(selectedButton.getText().toString()));
			startActivity(loadDirections);
		}
	}

}
