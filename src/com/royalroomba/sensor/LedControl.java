package com.royalroomba.sensor;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class LedControl{
	// whether device is rooted
	boolean hasRoot = false;
	// whether the led is on
	boolean ledOn = false;
	Su su_command;
	
	// init and check
	public LedControl(Context context){
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
	
	public Su getSuInstance(){
		return su_command;
	}
	
	// Turns of the LED if it's on
	public class turnOffLEDTimed extends TimerTask {
		public void run() {
			if(ledOn){
				if(hasRoot){
					su_command.Run("echo 0 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
				}	
				ledOn = false;
			}
		}
	}
	
	// turn off the led
	public void TurnOffLED(){
		if(ledOn){
			if(hasRoot){
				su_command.Run("echo 0 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
			}	
			ledOn = false;
		}
	}
	// turn on the led
	public void TurnOnLED(){
		if(!ledOn){
			if(hasRoot){
				su_command.Run("echo 1 > /sys/devices/platform/flashlight.0/leds/flashlight/brightness");
			}	
			ledOn = true;
		}
	}	

	// Toggles the LED on or off
	public void toggleLED(int time){
		if(ledOn){
			Log.i("RR-Sensor", "LED is ON, turn it off");
			this.TurnOffLED();
		}else{
			Log.i("RR-Sensor", "LED is OFF, turn it on");
			this.TurnOnLED();
			// turn it off awhile specified time
			new Timer().schedule(new turnOffLEDTimed(), time);			
		}
	}
}
