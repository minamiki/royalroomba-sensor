


#include <fcntl.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/ioctl.h>
#include <jni.h>

int dev;
  int led_mode;
  int ioctlRetVal = 1;

JNIEXPORT jstring JNICALL Java_net_cactii_flash_MainActivity_openFlash(JNIEnv* env)
{

  dev = open("/dev/msm_camera/config0", O_RDWR);
  if (dev < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash_MainActivity_setFlashOff(JNIEnv *env)
{

  led_mode = 0;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash_MainActivity_setFlashOn(JNIEnv *env)
{

  led_mode = 1;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash_MainActivity_setFlashFlash(JNIEnv *env)
{

  led_mode = 2;
  if ((ioctlRetVal = ioctl(dev, _IOW('m', 22, unsigned *), &led_mode)) < 0) {
    return (*env)->NewStringUTF(env, "Failed");
  }
  return (*env)->NewStringUTF(env, "OK");
}

JNIEXPORT jstring JNICALL Java_net_cactii_flash_MainActivity_closeFlash(JNIEnv *env)
{

  dev = open("/dev/msm_camera/config0", O_RDWR);
  if (dev > 0) {
    close(dev);
    return (*env)->NewStringUTF(env, "OK");
  }
  return (*env)->NewStringUTF(env, "Failed");
}

