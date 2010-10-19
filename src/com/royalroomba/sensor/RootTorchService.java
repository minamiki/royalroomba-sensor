package com.royalroomba.sensor;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class RootTorchService extends Service {

  public static final String MSG_TAG = "TorchRoot";
  private FlashDevice mDevice;
  public Handler mHandler;
  
  public boolean mTorchOn;
  
  public void onCreate() {    
    this.mHandler = new Handler() {};   
  }
  
  public int onStartCommand(Intent intent, int flags, int startId) {
    
    this.mDevice = new FlashDevice();
    
    if (!this.mDevice.Writable()) {
      Log.d("Torch", "Cant open flash RW");
      Su su = new Su();
      if (!su.can_su) {
        Toast.makeText(this, "Torch - cannot get root", Toast.LENGTH_SHORT).show();
        this.stopSelf();
      }
      su.Run("chmod 666 /dev/msm_camera/config0");
    }
    
    this.mDevice.Open();
    Log.d(MSG_TAG, "Starting torch");

    
    this.mDevice.FlashOn();
  
    return START_STICKY;
  }
  
  public void onDestroy() {
    //this.unregisterReceiver(this.mReceiver);
    stopForeground(true);
    this.mDevice.FlashOff();
    this.mDevice.Close();
  }
  
  @Override
  public IBinder onBind(Intent intent) {
    // TODO Auto-generated method stub
    return null;
  }
}
