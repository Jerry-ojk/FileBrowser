//
// Created by Jerry on 2022/12/3.
//

#ifndef FILEBROWSER_VIDEO_H
#define FILEBROWSER_VIDEO_H

#include <jni.h>

JNIEXPORT jintArray JNICALL
getFFmpegVersion(JNIEnv *env, jclass);

JNIEXPORT jobject JNICALL
getVideoInfo(JNIEnv *env, jclass, jstring path_);

#endif //FILEBROWSER_VIDEO_H
