package com.royalroomba.sensor;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
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
	
	// RabbitMQ Connection
	ServerMQ mqConn;
	String host = "www.vorce.net";
	
	// Proximity
	int i, count = 0;
	private int sensorstate = 0;
	MediaPlayer[] hit = new MediaPlayer[7];
	MediaPlayer raygun;
	// led
	Su su_command;
	private Context context;
	TimerTask turnOffLED;
	Timer waitTimer;
	boolean hasRoot = false;
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
	
	/* Send server
	DefaultHttpClient hc;
	ResponseHandler <String> res;
	HttpPost postMethod;
	List<NameValuePair> nameValuePairs;
	*/
	
	// update states
	private static final int UPDATE = 1;
	private static final int CONNECT = 2;
	
	// Interface elements
	TextView proxCount, proxState, accelX, accelY, accelZ, accelMag, accelMaxMag;
	NumberFormat format = new DecimalFormat("0.00"); 

    // Need handler for callbacks to the UI thread
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
		// prepare the random number generator
		generator = new Random();
		
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
		initAudio();
		
		// connect to the server
		//mqConn = ServerMQ.getInstance();
		//connectSever();

		// Initialise the led
		checkDevice();	
	}
	
	// Initialise the audio files
	public void initAudio() {
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
		/* Update the MySQL server via http
		hc = new DefaultHttpClient();
		res = new BasicResponseHandler();
		postMethod = new HttpPost("http://hci-apps.ddns.comp.nus.edu.sg/yitchun/hitcount.php");
		nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("hit", "hit"));
		try{
			postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			hc.execute(postMethod,res);
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}catch(ClientProtocolException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}
		*/
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
			
           	toggleLED();			
			raygun.start();
			
			// update the interface
			count++;
			proxCount.setText(count + " hits");
			proxState.setText(distance + "cm");
		}
	}
	
	// check led
	public void checkDevice(){
		if (new File("/sys/devices/platform/flashlight.0/leds/flashlight/brightness").exists() == false) {
			Toast.makeText(context, "Sorry, manual LED control not supported!", Toast.LENGTH_SHORT).show();
		}else{
			// device is okie, now ask for root
			su_command = new Su();
			hasRoot = this.su_command.can_su;
			if(!hasRoot){
				Toast.makeText(context, "No root! Unable to control LEDs!", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	// Turns of the LED if it's on
	public class turnOffLED extends TimerTask {
		public void run() {
			if(ledOn){
				if(hasRoot){
					su_command.Run("echo 0 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
				}	
				ledOn = false;
			}
		}
	}

	// Toggles the LED on or off
	public void toggleLED(){
		if(ledOn){
			Log.i("RR-Sensor", "LED is ON, turn it off");
			if(hasRoot){
				su_command.Run("echo 0 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
			}
			ledOn = false;
		}else{
			Log.i("RR-Sensor", "LED is OFF, turn it on");
			if(hasRoot){
				su_command.Run("echo 1 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
			}
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
		if(hasRoot){
			su_command.Run("echo 0 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
		}
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