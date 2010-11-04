package com.royalroomba.sensor;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SensorMain extends Activity implements SensorEventListener {

	private static final String TAG = "rr-sensor:SensorMain";
	
	private static final String HOST = "192.168.";
	private static final int PORT = 5672;
	private static final int DEVICE = 2;
	// Sensors
	SensorManager sensorManager;
	Sensor proximitySensor, accelerometerSensor;
	
	// RabbitMQ Connection
	ServerMQ mqConn;

	// Proximity
	int i, count = 0;
	private int sensorstate = 0;
	private long proxInterval = 5;
	private long proxNow = 0;
	private long proxTimeDiff = 0;
	private long proxLastUpdate = 0;

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
	private Random generator = new Random();
	
	// update states
	private static final int UPDATE = 1;
	private static final int CONNECT = 2;
	
	// update type
	private static final int PROXIMITY = 1;
	private static final int CONTACT = 2;
	
	private ServerMQ conn;
	boolean connected = false;
	
	// Interface elements
	TextView proxCount, proxState, sensitivity, accelMag, accelMaxMag, serverState;
	Button increase, decrease, connect;
	EditText serverIP;
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
		serverState = (TextView) findViewById(R.id.serverState);
		proxCount = (TextView) findViewById(R.id.count);
		proxState = (TextView) findViewById(R.id.proxState);
		sensitivity = (TextView) findViewById(R.id.sensitivity);
		accelMag = (TextView) findViewById(R.id.magnitude);
		accelMaxMag = (TextView) findViewById(R.id.maxMagnitude);
		sensitivity.setText(String.valueOf(threshold));
		connect = (Button) findViewById(R.id.connect);
		serverIP = (EditText) findViewById(R.id.server);
		
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
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		// TODO Auto-generated method stub
	}
	
	public void connectServer(){
		Toast.makeText(this, "Connecting to server...", Toast.LENGTH_SHORT).show();
		final Context ctx = this.getApplicationContext();
		ctx.getApplicationContext();
		Thread connectThread = new Thread() {
            public void run() {
            	// init connection
            	conn = new ServerMQ(HOST + serverIP.getText(), PORT, ctx, DEVICE);
            	Log.i(TAG, "Connecting to " + HOST + serverIP.getText());

            	// connect to server
            	connected = conn.connect();
            	if(connected){
            		mHandler.post(mServerConnected);
            		Log.i(TAG, "Connected to server!");
            		conn.listen(ctx);
            	}
            }
        };
        connectThread.start();
	}
	
	public void updateServer(int state){
		if(connected){
			switch(state){
			case PROXIMITY:
				Thread updateProx = new Thread() {
		            public void run() {
		            	conn.publish("proxhit");
		                mHandler.post(mUpdateResults);
		            }
		        };
		        updateProx.start();
				break;
			case CONTACT:
				Thread updateJust = new Thread() {
		            public void run() {
		            	conn.publish("justhit");
		                mHandler.post(mUpdateResults);
		            }
		        };
		        updateJust.start();
				break;		
			}
		}
	}
	
	public void updateNotify(int state){
		String message;
		switch(state){
		case 1:
			message = "Server Updated";
			break;
		case 2:
			message = "Connection established";
			serverState.setText("Connected");
			Toast.makeText(this, "Connected to server!", Toast.LENGTH_SHORT).show();
			
			break;
		default:
			message = "Error";
			break;				
		}
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	// keep track of the sensor state to register hits only on entry
	public void manageSensorState(float distance, long now){
		// check state to count only on entry
		proxNow = now;
		
		if(sensorstate == 0){
			sensorstate = 1;
		}else{
			proxTimeDiff = proxNow - proxLastUpdate;
			if(proxTimeDiff > proxInterval * 1000000000.0){
				Log.i(TAG, "Proximity Hit, time hit diff " + proxTimeDiff/1000000000.0 + "last update" + proxLastUpdate);
				proxLastUpdate = proxNow;
				// post update
				updateServer(PROXIMITY);

				sensorstate = 0;
				
	           	led.toggleLED(1000);			
	           	audio.raygun.start();
				
				// update the interface
				count++;
				proxCount.setText(count + " hits");
				proxState.setText(distance + "cm");				
			}else{
				Log.i(TAG, "Proximity Hit ignored, time hit" + proxTimeDiff/1000000000.0);
			}
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
					if(timeDiff > interval * 1000){
						updateServer(CONTACT);
						lastUpdate = now;
						// get a sound
						i = generator.nextInt(audio.getNumHits());
						// play it
						audio.hit[i].start();
						
						Toast.makeText(this, "A hit is detected at with magnitude of " + format.format(magnitude) + " with an interval of " + format.format(timeDiff/1000000000.0) + " secs", Toast.LENGTH_SHORT).show();
					}else{
						Log.i(TAG, "Direct Hit ignored");
					}
				}
				
	    		break;
	 		case Sensor.TYPE_PROXIMITY:
	 			Log.i(TAG, "Proximity Change");
	 			manageSensorState(event.values[0], event.timestamp);
	 			break;
	 		}
	 	}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		led.TurnOffLED();
		//conn.disconnect();
		connected = false;
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