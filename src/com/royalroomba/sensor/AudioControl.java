package com.royalroomba.sensor;

import android.content.Context;
import android.media.MediaPlayer;
import android.widget.Toast;

public class AudioControl{
	MediaPlayer[] hit = new MediaPlayer[4];
	MediaPlayer[] taunt = new MediaPlayer[5];
	MediaPlayer raygun, horn;
		
	// Initialise the audio files
	public AudioControl(Context context){
		try {
			// hit sounds
			hit[0]	= MediaPlayer.create(context, R.raw.crash1);
			hit[1]	= MediaPlayer.create(context, R.raw.crash2);
			hit[2]	= MediaPlayer.create(context, R.raw.crash3);
			hit[3]	= MediaPlayer.create(context, R.raw.crash4);

			taunt[0]= MediaPlayer.create(context, R.raw.boring);
			taunt[1]= MediaPlayer.create(context, R.raw.comeonthen);
			taunt[2]= MediaPlayer.create(context, R.raw.coward);
			taunt[3]= MediaPlayer.create(context, R.raw.illgetyou);
			taunt[4]= MediaPlayer.create(context, R.raw.stupid);
			raygun	= MediaPlayer.create(context, R.raw.shot);
			horn	= MediaPlayer.create(context, R.raw.horn); 
		}catch(Exception e) {
			Toast.makeText(context, "Error creating audio file: " + e.toString(), Toast.LENGTH_LONG).show();
		}
	}
	
	public int getNumHits(){
		return hit.length;
	}
	
	public int getNumTaunts(){
		return taunt.length;
	}
}
