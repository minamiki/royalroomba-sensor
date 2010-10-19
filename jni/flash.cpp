


#include <fcntl.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <jni.h>

int dev;
  int led_mode;
  int ioctlRetVal = 1;

JNIEXPORT jstring JNICALL Java_com_royalroomba_sensor_SensorMain_openFlash(JNIEnv* env)
{

  dev = open("/dev/msm_camera/config0", O_RDWR);
  if (dev < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_com_royalroomba_sensor_SensorMain_setFlashOff(JNIEnv *env)
{

  led_mode = 0;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_com_royalroomba_sensor_SensorMain_setFlashOn(JNIEnv *env)
{

  led_mode = 1;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_com_royalroomba_sensor_SensorMain_setFlashFlash(JNIEnv *env)
{

  led_mode = 2;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_com_royalroomba_sensor_SensorMain_closeFlash(JNIEnv *env)
{

  dev = open("/dev/msm_camera/config0", O_RDWR);
  if (dev > 0) {
    close(dev);
    return (*env)->NewStringUTF(env, "OK");
  }
  return (*env)->NewStringUTF(env, "Failed");
}

