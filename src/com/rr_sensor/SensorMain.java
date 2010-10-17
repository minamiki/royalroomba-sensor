package com.rr_sensor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class SensorMain extends Activity implements OnClickListener {
	private static final String TAG = "ServicesDemo";
	Button buttonStart, buttonStop;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		buttonStart = (Button) findViewById(R.id.startBtn);
		buttonStop = (Button) findViewById(R.id.stopBtn);

		buttonStart.setOnClickListener(this);
		buttonStop.setOnClickListener(this);
	}

	
	public void onClick(View src) {
		switch (src.getId()) {
		case R.id.startBtn:
			Log.d(TAG, "onClick: starting srvice");
			startService(new Intent(SensorMain.this, SensorService.class));
			break;
		case R.id.stopBtn:
			Log.d(TAG, "onClick: stopping srvice");
			stopService(new Intent(SensorMain.this, SensorService.class));
			break;
		}
	}
}