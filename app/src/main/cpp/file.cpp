#include <jni.h>
#include <string>
#include <sys/stat.h>
#include <unistd.h>
#include <android/log.h>
#include <dirent.h>
#include <vector>
#include <fcntl.h>
#include <sys/statvfs.h>
#include <sys/types.h>
#include <sys/system_properties.h>
#include <pwd.h>
#include <grp.h>
#include <cerrno>

using namespace std;

jboolean delete_dir(const char *path, struct stat64 *bu) {

    jboolean result = JNI_TRUE;
    DIR *dir = opendir(path);

    if (dir == nullptr) {
        return JNI_FALSE;
    }

    char a[PATH_MAX];
    if (getcwd(a, PATH_MAX) != nullptr) {
        __android_log_print(ANDROID_LOG_INFO, "delete_dir", "当前的目录:%s", a);
    } else {
        closedir(dir);
        return JNI_FALSE;
    }

    if (chdir(path) != 0) {
        closedir(dir);
        return JNI_FALSE;
    }

    if (getcwd(a, PATH_MAX) != nullptr) {
        __android_log_print(ANDROID_LOG_INFO, "delete_dir", "切换到目录:%s", a);
    } else {
        chdir("..");
        closedir(dir);
        return JNI_FALSE;
    }


    struct dirent64 *ptr;

    while ((ptr = readdir64(dir)) != nullptr) {
        if (strcmp(ptr->d_name, ".") == 0 || strcmp(ptr->d_name, "..") == 0) {
            continue;
        }
        if (ptr->d_type != DT_DIR) {
            if (unlink(ptr->d_name) == 0) {
                __android_log_print(ANDROID_LOG_INFO, "delete_dir", "删除文件:%s成功", ptr->d_name);
            } else {
                __android_log_print(ANDROID_LOG_INFO, "delete_dir", "删除文件:%s失败", ptr->d_name);
            }
        } else {
            __android_log_print(ANDROID_LOG_INFO, "delete_dir", "开始删除目录:%s", ptr->d_name);
            delete_dir(ptr->d_name, bu);
        }
    }
    closedir(dir);
    chdir("..");
    getcwd(a, PATH_MAX);
    __android_log_print(ANDROID_LOG_INFO, "delete_dir", "返回上级到目录:%s", a);

    rmdir(path);

    return result;
}

JNIEXPORT jboolean JNICALL
delete0(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    struct stat64 bu;

    jboolean result = JNI_FALSE;

    if (lstat64(path, &bu) != 0) {
        env->ReleaseStringUTFChars(path_, path);
        return JNI_FALSE;
    }

    if ((bu.st_mode & S_IFMT) == S_IFDIR) {
        __android_log_print(ANDROID_LOG_INFO, "delete0", "开始删除目录:%s", path);
        delete_dir(path, &bu);
    } else {
        if (unlink(path) == 0) {
            __android_log_print(ANDROID_LOG_INFO, "delete0", "删除文件:%s成功", path);
            result = JNI_TRUE;
        }
    }
    if (access(path, F_OK) != 0) {
        result = JNI_TRUE;
    }

    env->ReleaseStringUTFChars(path_, path);
    return result;
}


JNIEXPORT jboolean JNICALL
createFile0(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    jboolean result = JNI_FALSE;
    int a = creat64(path, 0666);
    if (a >= 0 && close(a) == 0) {
        result = JNI_TRUE;
    }
    env->ReleaseStringUTFChars(path_, path);
    return result;
}

JNIEXPORT jboolean JNICALL
createDirectory0(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    jboolean result = JNI_FALSE;
    if (mkdir(path, 0777) == 0) {
        result = JNI_TRUE;
    }

    env->ReleaseStringUTFChars(path_, path);
    return result;
}


JNIEXPORT jboolean JNICALL
isExist0(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    jboolean result = JNI_FALSE;
    if (access(path, F_OK) == 0) {
        result = JNI_TRUE;
    }
    env->ReleaseStringUTFChars(path_, path);
    return result;
}

JNIEXPORT jboolean JNICALL
rename0(JNIEnv *env, jclass type, jstring oldPath_, jstring newPath_) {
    const char *oldPath = env->GetStringUTFChars(oldPath_, nullptr);
    const char *newPath = env->GetStringUTFChars(newPath_, nullptr);
    jboolean result = JNI_FALSE;
    __android_log_write(ANDROID_LOG_INFO, "rename0", oldPath);
    __android_log_write(ANDROID_LOG_INFO, "rename0", newPath);
    if (rename(oldPath, newPath) == 0) {
        result = JNI_TRUE;
    }
    env->ReleaseStringUTFChars(oldPath_, oldPath);
    env->ReleaseStringUTFChars(newPath_, newPath);
    return result;
}

JNIEXPORT jint JNICALL
getFileType0(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    struct stat64 bu;
    if (stat64(path, &bu) != 0) {
        return -1;
    }
    env->ReleaseStringUTFChars(path_, path);
    return (bu.st_mode & S_IFMT) >> 12;
}

JNIEXPORT jobject JNICALL
getFileAttribute0(JNIEnv *env, jclass type, jstring path_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    struct stat64 bu;
    if (stat64(path, &bu) != 0) {
        return nullptr;
    }


    jclass classType = env->FindClass("jerry/filebrowser/file/FileAttribute");
    jmethodID conId = env->GetMethodID(classType, "<init>",
                                       "(Ljava/lang/String;JIIILjava/lang/String;Ljava/lang/String;JJJ)V");
    passwd *p = getpwuid(bu.st_uid);
    group *g = getgrgid(bu.st_gid);

    jobject item = env->NewObject(classType, conId,
                                  path_,//path
                                  bu.st_size,//length(long)
                                  bu.st_mode,
                                  bu.st_uid,
                                  bu.st_gid,
                                  env->NewStringUTF(p->pw_name),
                                  env->NewStringUTF(g->gr_name),
                                  (jlong) (bu.st_atim.tv_sec) * 1000,
                                  (jlong) (bu.st_mtim.tv_sec) * 1000,
                                  (jlong) (bu.st_ctim.tv_sec) * 1000);//time(long)

    return item;
}


JNIEXPORT jint JNICALL
getDisplay(JNIEnv *env, jclass type) {
    char value[8] = {0};
    __system_property_get("ro.sf.lcd_density", value);
    return (jint) atoi(value);
}

JNIEXPORT jint JNICALL
exec(JNIEnv *env, jclass type, jstring path_, jstring argv_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    //const char **argv = env->GetStringUTFChars(argv_, nullptr);
    //char *argv[] = {"-a", nullptr};
    int code = fork();
    if (code == 0) {
        execl(path, "ls", "-al", "/etc");
        perror("execl错误");
    } else if (code > 0) {
        return 0;
    } else {
        perror("fork错误");
    }
    return 1;
}


JNIEXPORT jint JNICALL
exec0(JNIEnv *env, jclass type, jstring path_, jstring argv_) {
    const char *path = env->GetStringUTFChars(path_, nullptr);
    FILE *stream;
    FILE *wstream;
    char buf[1024];
    memset(buf, '\0', sizeof(buf));//初始化buf,以免后面写如乱码到文件中
    stream = popen("ls -l", "r"); //将“ls －l”命令的输出 通过管道读取（“r”参数）到FILE* stream
    wstream = fopen("test_popen.txt", "w+"); //新建一个可写的文件

    fread(buf, sizeof(char), sizeof(buf), stream); //将刚刚FILE* stream的数据流读取到buf中
    fwrite(buf, 1, sizeof(buf), wstream);//将buf中的数据写到FILE    *wstream对应的流中，也是写到文件中

    pclose(stream);
    fclose(wstream);
    return 0;
}

//
//JNIEXPORT jboolean JNICALL
//cpoy0(JNIEnv *env, jclass type, jstring fromPath_, jstring toPath_) {
//    const char *fromPath = env->GetStringUTFChars(fromPath_, nullptr);
//    const char *toPath = env->GetStringUTFChars(toPath_, nullptr);
//
//    jboolean result = JNI_FALSE;
//    __android_log_write(ANDROID_LOG_INFO, "rename0", fromPath);
//    __android_log_write(ANDROID_LOG_INFO, "rename0", toPath);
//    if (rename(fromPath, toPath) == 0) {
//        result = JNI_TRUE;
//    }
//    env->ReleaseStringUTFChars(fromPath_, fromPath);
//    env->ReleaseStringUTFChars(toPath_, toPath);
//
//    return result;
//}

bool compare(struct dirent64 &a, struct dirent64 &b) {
    return strcmp(a.d_name, b.d_name) < 0;
}

JNIEXPORT jobjectArray JNICALL
listFiles0(JNIEnv *env, jclass file_class, jstring path_) {
    clock_t start = clock();

    const char *path = env->GetStringUTFChars(path_, nullptr);
    DIR *dir = opendir(path);
    if (dir == nullptr || chdir(path) != 0) {
        __android_log_print(ANDROID_LOG_INFO, "listFile(path)", "打开%s失败", path);
        return nullptr;
    }

    jmethodID conId = env->GetMethodID(file_class, "<init>", "(Ljava/lang/String;IJJ)V");

    vector<dirent64> dirs;
    vector<dirent64> files;

    struct dirent64 *ptr;

    double seconds;

    while ((ptr = readdir64(dir)) != nullptr) {
        if (ptr->d_name[0] == '.') {
            continue;
        }

        if (ptr->d_type == DT_DIR) {
            dirs.push_back(*ptr);
        } else {
            files.push_back(*ptr);
        }
    }
    seconds = ((double) (clock() - start) * 1000) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_INFO, "listFile(path)", "获取完耗时%lfms", seconds);

    size_t len_dir = dirs.size();
    size_t len_file = files.size();

    jobjectArray fileArray = env->NewObjectArray(jsize(len_dir + len_file), file_class, nullptr);
    struct stat64 bu;

    if (len_dir > 0) {
        sort(dirs.begin(), dirs.end(), compare);
        for (size_t i = 0; i < len_dir; ++i) {
            if (stat64(dirs[i].d_name, &bu) != 0) {
                __android_log_print(ANDROID_LOG_INFO, "listFile", "获取文件信息错误%s", files[i].d_name);
                continue;
            }
            jstring str = env->NewStringUTF(dirs[i].d_name);
            jlong time = (jlong) (bu.st_mtim.tv_sec) * 1000;
            jobject item = env->NewObject(file_class, conId,
                                          str,//name(String)
                                          4,//type(int)
                                          bu.st_size,//length(long)
                                          time);//time(long)

            env->SetObjectArrayElement(fileArray, (jsize) i, item);
            env->DeleteLocalRef(str);
            env->DeleteLocalRef(item);
        }
    }

    if (len_file > 0) {
        sort(files.begin(), files.end(), compare);
        for (size_t i = 0; i < len_file; ++i) {
            jstring str = env->NewStringUTF(files[i].d_name);
            jobject item;
            if (stat64(files[i].d_name, &bu) == 0) {
                jlong time = (jlong) (bu.st_mtim.tv_sec) * 1000;
                item = env->NewObject(file_class, conId,
                                      str,//name(String)
                                      files[i].d_type,//type(int)
                                      bu.st_size,//length(long)
                                      time);//time(long)
            } else {
                __android_log_print(ANDROID_LOG_INFO, "listFile", "获取文件信息错误%s", files[i].d_name);
                item = env->NewObject(file_class, conId,
                                      str,//name(String)
                                      files[i].d_type,//type(int)
                                      -1l,//length(long)
                                      -1l);//time(long)
            }
            env->SetObjectArrayElement(fileArray, (jsize) (len_dir + i), item);
            env->DeleteLocalRef(item);
            env->DeleteLocalRef(str);
        }
    }



    //__android_log_print(ANDROID_LOG_INFO, "listFile(path)", "排序耗时%lfms", seconds);
    //start = clock();

    //seconds = ((double) (clock() - start) * 1000) / CLOCKS_PER_SEC;

    //__android_log_print(ANDROID_LOG_INFO, "listFile(path)", "创建数组耗时完耗时%lfms", seconds);
    closedir(dir);
    seconds = ((double) (clock() - start) * 1000) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_INFO, "listFile(path)", "函数共耗时%lfms", seconds);

    return fileArray;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        __android_log_print(ANDROID_LOG_INFO, "linux-tool", "onLoad失败");
        return JNI_ERR;
    }
    jclass UnixFile = env->FindClass("jerry/filebrowser/file/UnixFile");

    JNINativeMethod methods[] = {
            {"isExist",          "(Ljava/lang/String;)Z",                                 reinterpret_cast<void *>(isExist0)},
            {"listFiles",        "(Ljava/lang/String;)[Ljerry/filebrowser/file/UnixFile;",reinterpret_cast<void *>(listFiles0)},
            {"rename",           "(Ljava/lang/String;Ljava/lang/String;)Z",               reinterpret_cast<void *>(rename0)},
            {"delete",           "(Ljava/lang/String;)Z",                                 reinterpret_cast<void *>(delete0)},
            {"createFile",       "(Ljava/lang/String;)Z",                                 reinterpret_cast<void *>(createFile0)},
            {"createDir",        "(Ljava/lang/String;)Z",                                 reinterpret_cast<void *>(createDirectory0)},
            {"getFileType",      "(Ljava/lang/String;)I",                                 reinterpret_cast<void *>(getFileType0)},
            {"getFileAttribute", "(Ljava/lang/String;)Ljerry/filebrowser/file/FileAttribute;", reinterpret_cast<void *>(getFileAttribute0)},
            {"getDisplay",       "()I",                                                   reinterpret_cast<void *>(getDisplay)}
    };

    if (env->RegisterNatives(UnixFile, methods, 9) != JNI_OK) {
        __android_log_print(ANDROID_LOG_INFO, "JNI_OnLoad", "动态注册失败");
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) == JNI_OK) {
        __android_log_print(ANDROID_LOG_INFO, "JNI_OnUnload", "unLoad成功");
        jclass UnixFile = env->FindClass("jerry/filebrowser/file/UnixFile");
        env->UnregisterNatives(UnixFile);
    } else {
        __android_log_print(ANDROID_LOG_INFO, "JNI_OnUnload", "unLoad失败");
    }
    //__android_log_write(ANDROID_LOG_INFO, "JNI_OnUnload", android::base::GetProperty("ro.sf.lcd_density", "666").c_str());
}