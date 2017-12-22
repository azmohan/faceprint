LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    src/com/android/systemui/EventLogTags.logtags

LOCAL_STATIC_JAVA_LIBRARIES := Keyguard
LOCAL_STATIC_JAVA_LIBRARIES += com.mediatek.systemui.ext
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_JAVA_LIBRARIES += freeme-framework
LOCAL_JAVA_LIBRARIES += ims-common
LOCAL_JAVA_LIBRARIES += freeme-framework

LOCAL_PACKAGE_NAME := SystemUI
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

# Added by droi hanhao for customized 2016-01-08
LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res_droi
# Added End

LOCAL_RESOURCE_DIR += \
    frameworks/base/packages/Keyguard/res \
    frameworks/base/packages/Keyguard/res_ext \
	frameworks/base/packages/Keyguard/res_droi \
    $(LOCAL_PATH)/res \
    $(LOCAL_PATH)/res_ext

LOCAL_AAPT_FLAGS := --auto-add-overlay --extra-packages com.android.keyguard

ifneq ($(SYSTEM_UI_INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
endif

include frameworks/base/packages/SettingsLib/common.mk

# @{ freeme,chenming. 20171218. Faceprint
LOCAL_STATIC_JAVA_AAR_LIBRARIES := faceprint-authenui-library
LOCAL_AAPT_FLAGS += --extra-packages com.freeme.face.ui
# @}

include $(BUILD_PACKAGE)

ifeq ($(EXCLUDE_SYSTEMUI_TESTS),)
    include $(call all-makefiles-under,$(LOCAL_PATH))
endif
