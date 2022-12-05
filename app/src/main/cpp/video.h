#ifndef FILEBROWSER_VIDEO_H
#define FILEBROWSER_VIDEO_H

#include <jni.h>

JNIEXPORT jintArray JNICALL
getFFmpegVersion(JNIEnv *env, jclass);

JNIEXPORT jobject JNICALL
getVideoInfo(JNIEnv *env, jclass, jstring path_);

void JNI_OnLoad_video(JNIEnv *env);

#endif //FILEBROWSER_VIDEO_H
