LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	src/org/abrantix/rockon/rockonnggl/IRockOnNextGenService.aidl

LOCAL_STATIC_JAVA_LIBRARIES := libGoogleAnalytics

LOCAL_PACKAGE_NAME := Music3

# LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_OVERRIDES_PACKAGES := Music

include $(BUILD_PACKAGE)

#######

include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := libGoogleAnalytics:libs/libGoogleAnalytics.jar
LOCAL_MODULE_TAGS := optional
include $(BUILD_MULTI_PREBUILT)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
