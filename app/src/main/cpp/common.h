//
// Created by Jerry on 2022/12/3.
//

#ifndef FILEBROWSER_COMMON_H
#define FILEBROWSER_COMMON_H

#include <jni.h>

class AutoReleaseString {

public:
    AutoReleaseString(JNIEnv *env, jstring *jstr, const char *c) : env(env), jstr(jstr), c(c) {

    }

    ~AutoReleaseString() {
        env->ReleaseStringUTFChars(*jstr, c);
    }

private:
    JNIEnv *env;
    jstring *jstr;
    const char *c;
};

#endif //FILEBROWSER_COMMON_H
