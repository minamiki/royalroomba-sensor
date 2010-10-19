package com.royalroomba.sensor;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class SensorMain extends Activity implements SensorEventListener {

	SensorManager sensorManager;
	Sensor proximitySensor, accelerometerSensor;
	
	// Proximity
	public FlashDevice device;
	int i, count = 0;
	private int sensorstate = 0;
	MediaPlayer[] hit = new MediaPlayer[7];
	MediaPlayer raygun;
	Intent ledIntent;
	// Represents a 'su' instance
	public Su su_command;
	public boolean has_root;
	private Context context;
	TimerTask turnOffLED;
	Timer waitTimer;
	boolean ledOn = false;
	
	// Accelerometer
	private static double threshold = 11.500;
	private static int interval = 10000;
	
	private long now = 0;
	private long timeDiff = 0;
	private long lastUpdate = 0;
	
	private float x = 0;
	private float y = 0;
	private float z = 0;
	private float magnitude = 0;
	private float maxMagnitude = 0;
	
	// Hit sounds
	Random generator;
	
	// Send server
	DefaultHttpClient hc;
	ResponseHandler <String> res;
	HttpPost postMethod;
	List<NameValuePair> nameValuePairs;
	
	// Interface elements
	TextView proxCount, proxState, accelX, accelY, accelZ, accelMag, accelMaxMag;
	NumberFormat format = new DecimalFormat("0.00"); 

    // Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
        	updateNotify();
        }
    };

	
	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// prepare the random number generator
		generator = new Random();
		
		has_root = false;
		device = FlashDevice.getInstance();
		
		// Obtain a reference to the system-wide sensor event manager. 
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		// Interface elements
		proxCount = (TextView) findViewById(R.id.count);
		proxState = (TextView) findViewById(R.id.proxState);
		accelX = (TextView) findViewById(R.id.accelX);
		accelY = (TextView) findViewById(R.id.accelY);
		accelZ = (TextView) findViewById(R.id.accelZ);
		accelMag = (TextView) findViewById(R.id.magnitude);
		accelMaxMag = (TextView) findViewById(R.id.maxMagnitude);
		 
		// Init the audio files
		prepareAudio();

		if (new File("/dev/msm_camera/config0").exists() == false) {
			Toast.makeText(context, "Only Nexus One is supported, sorry!", Toast.LENGTH_LONG).show();
			has_root = false;
		}

		if(!device.Writable()){
			Log.d("Torch", "Cant open flash RW");
			su_command = new Su();
			has_root = this.su_command.can_su;
			if(!has_root){
				Toast.makeText(this, "No Root!", Toast.LENGTH_SHORT).show();
			}else{
				su_command.Run("chmod 666 /dev/msm_camera/config0");
			}
		}

		// led
		ledIntent = new Intent(SensorMain.this, RootTorchService.class);
	}

	public void prepareAudio() {
		try {
			hit[0] = MediaPlayer.create(SensorMain.this, R.raw.ouch1);
			hit[1] = MediaPlayer.create(SensorMain.this, R.raw.ouch2);
			hit[2] = MediaPlayer.create(SensorMain.this, R.raw.ouch3);
			hit[3] = MediaPlayer.create(SensorMain.this, R.raw.ouch4);
			hit[4] = MediaPlayer.create(SensorMain.this, R.raw.ouch5);
			hit[5] = MediaPlayer.create(SensorMain.this, R.raw.ouch6);
			hit[6] = MediaPlayer.create(SensorMain.this, R.raw.ouch7);
			raygun = MediaPlayer.create(SensorMain.this, R.raw.raygun);
		}catch(Exception e) {
			Toast.makeText(SensorMain.this, "Error creating audio file: " + e.toString(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		// TODO Auto-generated method stub

	}
	
	public void updateServer(){
		hc = new DefaultHttpClient();
		res = new BasicResponseHandler();
		postMethod = new HttpPost("http://hci-apps.ddns.comp.nus.edu.sg/yitchun/hitcount.php");
		nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("hit", "hit"));
		try{
			postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			hc.execute(postMethod,res);
		}catch(UnsupportedEncodingException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(ClientProtocolException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void updateNotify(){
		Toast.makeText(this, "Server Updated", Toast.LENGTH_SHORT).show();
	}

	public void manageSensorState(float distance){
		// check state to count only on entry
		if(sensorstate == 0){
			sensorstate = 1;
		}else{
			// post update
	        Thread updateThread = new Thread() {
	            public void run() {
	            	updateServer();
	                //mHandler.post(mUpdateResults);
	            }
	        };
	        updateThread.start();

			sensorstate = 0;
			
           	toggleLED();			
			raygun.start();
			
			// update the interface
			count++;
			proxCount.setText(count + " hits");
			proxState.setText(distance + "cm");
		}
	}
	
	public class turnOffLED extends TimerTask {
		public void run() {
			if(ledOn){
				stopService(ledIntent);		
				ledOn = false;
			}
		}
	}

	
	public void toggleLED(){
		if(ledOn){
			Log.i("RR-Sensor", "LED is ON, turn it off");
			stopService(ledIntent);
			ledOn = false;
		}else{
			Log.i("RR-Sensor", "LED is OFF, turn it on");
			startService(ledIntent);
			ledOn = true;
			new Timer().schedule(new turnOffLED(), 1000);			
		}
	}
	
	@Override
	public void onSensorChanged(SensorEvent event){
	 	synchronized (this) {
	 		switch(event.sensor.getType()){
	 		case Sensor.TYPE_ACCELEROMETER:
	 			// update the interface
	 			accelX.setText(String.valueOf(event.values[0]));
	 			accelY.setText(String.valueOf(event.values[1]));
	 			accelZ.setText(String.valueOf(event.values[2]));
	 			
	 			// check for collision
				now = event.timestamp;
				//Log.i("RoyalRoomba-sensor", "Time: " + now);
				
	    		x = event.values[0];
				y = event.values[1];
				z = event.values[2];
	    		
				// find the relative magnitude
				magnitude = (float) Math.sqrt(x * x + y * y + z * z);
				if(magnitude > maxMagnitude){
					maxMagnitude = magnitude;
				}
				accelMag.setText(String.valueOf(magnitude));
				accelMaxMag.setText(String.valueOf(maxMagnitude));
				
				// simple check for hits
				if(magnitude > threshold){
					// check if just hit
					timeDiff = now - lastUpdate;
					if(timeDiff > interval * 100000){
						
						lastUpdate = now;
						// get a sound
						i = generator.nextInt(7);
						// play it
						hit[i].start();
						Log.i("RoyalRoomba-sensor", "Gap: " + timeDiff);
						Toast.makeText(this, "A hit is detected at with magnitude of " + format.format(magnitude) + " with an interval of " + format.format(timeDiff/1000000000.0) + " secs", Toast.LENGTH_SHORT).show();
					}
				}
				
				
	    		break;
	 		case Sensor.TYPE_PROXIMITY:
	 			manageSensorState(event.values[0]);
	 			break;
	 		}
	 	}
	 	//Toast.makeText(this, "Sensor Value Changed", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sensorManager.unregisterListener(SensorMain.this, proximitySensor);
		sensorManager.unregisterListener(SensorMain.this, accelerometerSensor);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		sensorManager.registerListener(SensorMain.this, proximitySensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(SensorMain.this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
	}
}