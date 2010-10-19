
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= flash.c
LOCAL_MODULE := flash
LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)


include $(BUILD_SHARED_LIBRARY)
