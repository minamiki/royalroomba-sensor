package com.royalroomba.sensor;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.Toast;

public class AudioControl{
	MediaPlayer[] hit = new MediaPlayer[7];
	MediaPlayer raygun;
	
	// Initialise the audio files
	public AudioControl(Context context){
		try {
			hit[0] = MediaPlayer.create(context, R.raw.ouch1);
			hit[1] = MediaPlayer.create(context, R.raw.ouch2);
			hit[2] = MediaPlayer.create(context, R.raw.ouch3);
			hit[3] = MediaPlayer.create(context, R.raw.ouch4);
			hit[4] = MediaPlayer.create(context, R.raw.ouch5);
			hit[5] = MediaPlayer.create(context, R.raw.ouch6);
			hit[6] = MediaPlayer.create(context, R.raw.ouch7);
			raygun = MediaPlayer.create(context, R.raw.raygun);
		}catch(Exception e) {
			Toast.makeText(context, "Error creating audio file: " + e.toString(), Toast.LENGTH_LONG).show();
		}
	}
}
