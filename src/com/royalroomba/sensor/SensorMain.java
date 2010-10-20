package com.royalroomba.sensor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class SensorMain extends Activity implements SensorEventListener {

	// Sensors
	SensorManager sensorManager;
	Sensor proximitySensor, accelerometerSensor;
	
	// RabbitMQ Connection
	ServerMQ mqConn;
	String host = "www.vorce.net";
	
	// Proximity
	int i, count = 0;
	private int sensorstate = 0;

	// LED
	LedControl led;
	
	// Audio
	AudioControl audio;
	
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
	private Random generator = new Random();;
	
	// update states
	private static final int UPDATE = 1;
	private static final int CONNECT = 2;
	
	// Interface elements
	TextView proxCount, proxState, accelX, accelY, accelZ, accelMag, accelMaxMag;
	private NumberFormat format = new DecimalFormat("0.00"); 

    // Need handler for callback to the UI thread
    final Handler mHandler = new Handler();

    // Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
        	updateNotify(UPDATE);
        }
    };
    final Runnable mServerConnected = new Runnable() {
        public void run() {
        	updateNotify(CONNECT);
        }
    };

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
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
		 
		// Initialise the audio files
		audio = new AudioControl(this);
		
		// Initialise the led
		led = new LedControl(this);
		
		// connect to the server
		//mqConn = ServerMQ.getInstance();
		//connectSever();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		// TODO Auto-generated method stub
	}
	
	public void connectSever(){
        /*
		Thread connectThread = new Thread() {
            public void run() {
            	mqConn.connect(host);
                mHandler.post(mServerConnected);
            }
        };
        connectThread.start();
        */
		mqConn.connect(host);
	}
	
	public void updateServer(){
		mqConn.publish("proxhit", "hit");
	}
	
	public void updateNotify(int state){
		String message;
		switch(state){
		case 1:
			message = "Server Updated";
			break;
		case 2:
			message = "Connection established";
			break;
		default:
			message = "Error";
			break;				
		}
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	// keep track of the sensor state to register hits only on entry
	public void manageSensorState(float distance){
		// check state to count only on entry
		if(sensorstate == 0){
			sensorstate = 1;
		}else{
			// post update
	        Thread updateThread = new Thread() {
	            public void run() {
	            	//updateServer();
	                mHandler.post(mUpdateResults);
	            }
	        };
	        //updateThread.start();

			sensorstate = 0;
			
           	led.toggleLED(1000);			
           	audio.raygun.start();
			
			// update the interface
			count++;
			proxCount.setText(count + " hits");
			proxState.setText(distance + "cm");
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
						audio.hit[i].start();
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
		led.TurnOffLED();
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