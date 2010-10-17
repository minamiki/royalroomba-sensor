package com.rr_sensor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

public class SensorService extends Service implements SensorEventListener {	
	@Override
	public IBinder onBind(Intent intent) {
		// Don't need binder
		return null;
	}
	
	SensorManager sensorManager;
	Sensor sensor;
	TextView lightValue;
	int i;
	private int sensorstate = 0;
	MediaPlayer[] hit = new MediaPlayer[7];
	Random generator;
	DefaultHttpClient hc;
	ResponseHandler <String> res;
	HttpPost postMethod;
	List<NameValuePair> nameValuePairs;

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState){
		super.onCreate();
		// prepare the random number generator
		generator = new Random();
		
		// Obtain a reference to the system-wide sensor event manager. 
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		//sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		
		// Register for events.
		
		
		// Init the audio files
		prepareAudio();

		// sending
		hc = new DefaultHttpClient();
		res = new BasicResponseHandler();
		postMethod = new HttpPost("http://hci-apps.ddns.comp.nus.edu.sg/yitchun/hitcount.php");
		nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("hit", "hit"));
		try{
			postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		}catch(UnsupportedEncodingException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void prepareAudio() {
		try {
			hit[0] = MediaPlayer.create(SensorService.this, R.raw.ouch1);
			hit[1] = MediaPlayer.create(SensorService.this, R.raw.ouch2);
			hit[2] = MediaPlayer.create(SensorService.this, R.raw.ouch3);
			hit[3] = MediaPlayer.create(SensorService.this, R.raw.ouch4);
			hit[4] = MediaPlayer.create(SensorService.this, R.raw.ouch5);
			hit[5] = MediaPlayer.create(SensorService.this, R.raw.ouch6);
			hit[6] = MediaPlayer.create(SensorService.this, R.raw.ouch7);
		}catch(Exception e) {
			Toast.makeText(SensorService.this, "Error creating audio file: " + e.toString(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy){
		// TODO Auto-generated method stub

	}

	public void manageSensorState(){
		if(sensorstate == 0){
			sensorstate = 1;
		}else{
			try{
				hc.execute(postMethod,res);
			}catch(ClientProtocolException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch(IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sensorstate = 0;
			i = generator.nextInt(7);
			hit[i].start();
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event){
		synchronized(this){
		 	manageSensorState();
		 	Toast.makeText(this, "Sensor Value Changed", Toast.LENGTH_LONG).show();
			//lightValue.setText(Float.toString(event.values[0]));
			//lightValue.setText("Hits: "+counter);
		}
	}

	@Override
	public void onDestroy() {
		sensorManager.unregisterListener(SensorService.this, sensor);
		Toast.makeText(this, "Proximity Service Stopped", Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		sensorManager.registerListener(SensorService.this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
		Toast.makeText(this, "Proximity Service Started", Toast.LENGTH_LONG).show();
	}

}