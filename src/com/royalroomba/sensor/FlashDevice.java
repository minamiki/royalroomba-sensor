package com.royalroomba.sensor;

import android.util.Log;

public class FlashDevice {
	private static FlashDevice currentInstance;
	public boolean open;
	public boolean on;
	
	static synchronized FlashDevice getInstance() {
		if (currentInstance == null) {
			currentInstance = new FlashDevice();
			currentInstance.on = currentInstance.open = false;
		}
		return currentInstance;
	}

	
	public void Open() {

		if (!open && "Failed".equals(openFlash()))
			open = false;
		else
			open = true;

		Log.d("Torch", "flash opened: " + open);

	}
	
	public boolean Writable() {
		String result = flashWritable();
		Log.d("Torch", "Writable: " + result);
		return "OK".equals(result);
	}
	
	public void Close() {
		Log.d("Torch", "Closing: " + closeFlash());
		open = false;
	}
	
	public String FlashOn() {
		on = true;
		return setFlashOn();
	}
	
	public String FlashOff() {
		on = false;
		return setFlashOff();
	}
    
    // These functions are defined in the native libflash library.
    public static native String  openFlash();
    public static native String  setFlashOff();
    public static native String  setFlashOn();
    public static native String  setFlashFlash();
    public static native String  closeFlash();
    public static native String  flashWritable();
   	// Load libflash once on app startup.
   	static {
   		System.loadLibrary("flash");
   	}
}
