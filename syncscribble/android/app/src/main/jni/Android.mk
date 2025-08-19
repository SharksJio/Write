#include $(call all-subdir-makefiles)

# Note that symlinking source dirs is a terrible idea which can create a huge mess when trying to open files,
#  esp. when debugging

# Enable native UI mode by default (comment out to use SDL mode)
export ANDROID_NATIVE_UI := 1

# Removed SDL dependency - using native Android UI instead
# include /home/mwhite/styluslabs/SDL/Android.mk
include /home/mwhite/styluslabs/syncscribble/Makefile
