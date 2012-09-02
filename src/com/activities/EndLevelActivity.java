package com.activities;

import com.components.PlayerSettings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class EndLevelActivity extends Activity {
	private int level = 1;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.endlevel);

		int score = getIntent().getIntExtra("SCORE", 0);
		PlayerSettings.score += score;

		TextView header = (TextView) findViewById(R.id.completionText);

		if (PlayerSettings.isMoleLeader) {
			if (score >= 100) {
				header.setText("Congratulations! Level Complete");
				level++;
			} else {
				header.setText("You need 100 points to unlock the next level. Try again!");
			}
		}
	}

	public void onBackPressed() {
		Intent intent = new Intent(this, LevelSelectActivity.class);
		intent.putExtra("LEVEL_NUMBER", 1);
		startActivity(intent);
	}
}
