package com.royalroomba.sensor;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.Toast;

public class AudioControl{
	MediaPlayer[] hit = new MediaPlayer[7];
	MediaPlayer[] taunt = new MediaPlayer[5];
	MediaPlayer raygun, horn;
		
	// Initialise the audio files
	public AudioControl(Context context){
		try {
			hit[0]	= MediaPlayer.create(context, R.raw.ouch1);
			hit[1]	= MediaPlayer.create(context, R.raw.ouch2);
			hit[2]	= MediaPlayer.create(context, R.raw.ouch3);
			hit[3]	= MediaPlayer.create(context, R.raw.ouch4);
			hit[4]	= MediaPlayer.create(context, R.raw.ouch5);
			hit[5]	= MediaPlayer.create(context, R.raw.ouch6);
			hit[6]	= MediaPlayer.create(context, R.raw.ouch7);
			taunt[0]= MediaPlayer.create(context, R.raw.boring);
			taunt[1]= MediaPlayer.create(context, R.raw.comeonthen);
			taunt[2]= MediaPlayer.create(context, R.raw.coward);
			taunt[3]= MediaPlayer.create(context, R.raw.illgetyou);
			taunt[4]= MediaPlayer.create(context, R.raw.stupid);
			raygun	= MediaPlayer.create(context, R.raw.raygun);
			horn	= MediaPlayer.create(context, R.raw.horn); 
		}catch(Exception e) {
			Toast.makeText(context, "Error creating audio file: " + e.toString(), Toast.LENGTH_LONG).show();
		}
	}
}
