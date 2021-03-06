package com.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class EnterIDActivity extends Activity {
	public final static String ID_NUMBER_STRING = "com.activities.EnterIDActivity.IDNUMBER";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_id);
    }
    
    public void enterID(View view){
    	Intent loadOptions = new Intent(this, SelectModeActivity.class);
    	EditText inputField = (EditText) findViewById(R.id.editview);
    	String idNumber = inputField.getText().toString();
    	loadOptions.putExtra(ID_NUMBER_STRING, idNumber);
    	
    	startActivity(loadOptions);
    }
    
    public void onBackPressed(){
    	Intent intent = new Intent(Intent.ACTION_MAIN);
    	intent.addCategory(Intent.CATEGORY_HOME);
    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(intent);
    }
    
    
}