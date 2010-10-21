package com.royalroomba.sensor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;
import java.util.Timer;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SensorMain extends Activity implements SensorEventListener {

	private static final String TAG = "rr-sensor:SensorMain";
	// Sensors
	SensorManager sensorManager;
	Sensor proximitySensor, accelerometerSensor;
	
	// RabbitMQ Connection
	ServerMQ mqConn;
	//String host = "www.vorce.net";
	//String host = "192.168.0.197";
	//String host = "169.254.5.143";
	String host = "192.168.2.100";
	
	// Proximity
	int i, count = 0;
	private int sensorstate = 0;

	// LED
	LedControl led;
	
	// Audio
	AudioControl audio;
	
	// Accelerometer
	private double threshold = 11.500;
	private int interval = 10000;
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
	
	// update type
	private static final int PROXIMITY = 1;
	private static final int CONTACT = 2;
	
	private ServerMQ conn;
	
	// Interface elements
	TextView proxCount, proxState, sensitivity, accelMag, accelMaxMag;
	Button increase, decrease, connect;
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
		sensitivity = (TextView) findViewById(R.id.sensitivity);
		accelMag = (TextView) findViewById(R.id.magnitude);
		accelMaxMag = (TextView) findViewById(R.id.maxMagnitude);
		sensitivity.setText(String.valueOf(threshold));
		connect = (Button) findViewById(R.id.connect);
		
		increase = (Button) findViewById(R.id.increase);
		increase.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	threshold -= 0.5;
            	sensitivity.setText(String.valueOf(threshold));
            }
        });
		decrease = (Button) findViewById(R.id.decrease);
		decrease.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	threshold += 0.5;
            	sensitivity.setText(String.valueOf(threshold));
            }
        });
		connect = (Button) findViewById(R.id.connect);
		connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	connectServer();
            }
        });
		 
		// Initialise the audio files
		audio = new AudioControl(this);
		
		// Initialise the led
		led = new LedControl(this);
		
		// connect to the server
		//connectServer();
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		// TODO Auto-generated method stub
	}
	
	public void connectServer(){
		final Context ctx = this.getApplicationContext();
		ctx.getApplicationContext();
		Thread connectThread = new Thread() {
            public void run() {
            	// init connection
            	conn = new ServerMQ(host, 5672, ctx);

            	//RCTask java_rc_task = new RCTask(jtests);
            	//Timer java_rc_timer = new Timer();
            	//java_rc_timer.schedule(java_rc_task, 1000, 10000);

            	// connect to server
            	if(conn.connect()){
            		Log.i(TAG, "Connected to server!");
            		conn.listen(ctx);
            	}
            	
            	//jtests.test_consume_by_standard_basic_get(mQueuename);
            	//jtests.test_consume_by_consumer(mQueuename);
            	//conn.test_consume_by_direct_lib("rubbish"); 
            	//conn.disconnect();
                mHandler.post(mServerConnected);
            }
        };
        connectThread.start();
	}
	
	public void updateServer(){
		conn.publish("proxhit");
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
	            	updateServer();
	                mHandler.post(mUpdateResults);
	            }
	        };
	        updateThread.start();

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