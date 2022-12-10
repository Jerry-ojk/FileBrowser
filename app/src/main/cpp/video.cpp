#include <jni.h>
#include <android/log.h>

extern "C" {
#include <libavcodec/version.h>
#include <libavdevice/version.h>
#include <libavfilter/version.h>
#include <libavformat/version.h>
#include <libavutil/version.h>
#include <libpostproc/version.h>
#include <libswresample/version.h>
#include <libswscale/version.h>
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
}

#include "common.h"


JNIEXPORT jintArray JNICALL
getFFmpegVersion(JNIEnv *env, jclass) {
    jintArray versions = env->NewIntArray(8);
    jboolean copy = JNI_FALSE;
    jint *array = env->GetIntArrayElements(versions, &copy);
    array[0] = LIBAVCODEC_VERSION_INT;
    array[1] = LIBAVDEVICE_VERSION_INT;
    array[2] = LIBAVFILTER_VERSION_INT;
    array[3] = LIBAVFORMAT_VERSION_INT;
    array[4] = LIBAVUTIL_VERSION_INT;
    array[5] = LIBPOSTPROC_VERSION_INT;
    array[6] = LIBSWRESAMPLE_VERSION_INT;
    array[7] = LIBSWSCALE_VERSION_INT;
    env->ReleaseIntArrayElements(versions, array, 0);
    return versions;
}

JNIEXPORT jobject JNICALL
getVideoInfo(JNIEnv *env, jclass, jstring path_) {
    const char *in_path = env->GetStringUTFChars(path_, nullptr);
    AutoReleaseString _(env, &path_, in_path);

    jclass videoIndoClass = env->FindClass("jerry/filebrowser/video/VideoInfo");
    jmethodID conId = env->GetMethodID(videoIndoClass, "<init>", "(IIFFJLjava/lang/String;)V");

    AVFormatContext *fmt_ctx = nullptr;
    if (avformat_open_input(&fmt_ctx, in_path, nullptr, nullptr) != 0) return nullptr;
//    fmt_ctx->data_codec->name
    jfloat fps = -1;
    jstring codecName;
    // 单位为秒
    jfloat during = static_cast<jfloat>(fmt_ctx->duration) / AV_TIME_BASE;

    int videoIndex = -1;
    for (int i = 0; fmt_ctx->nb_streams; i++) {
        //寻找到视频流
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }
    if (videoIndex == -1) {
        return nullptr;
    }

    //获取到视频流
    AVStream *streams = fmt_ctx->streams[videoIndex];
    // streams->r_frame_rate
    // 帧率
    fps = (float) streams->avg_frame_rate.num / (float) streams->avg_frame_rate.den;
    //视频编码类型
    const char *name = avcodec_get_name(streams->codecpar->codec_id);
    codecName = env->NewStringUTF(name);

    const AVCodec *codec = avcodec_find_decoder(streams->codecpar->codec_id);
    if (!codec) {
        avformat_close_input(&fmt_ctx);
        return nullptr;
    }

    //分配AVCodecContext空间
//    AVCodecContext *cd_ctx = avcodec_alloc_context3(codec);
    //填充数据
//    if (avcodec_parameters_to_context(cd_ctx, streams->codecpar) < 0) {
//        avcodec_free_context(&cd_ctx);
//        avformat_close_input(&fmt_ctx);
//        return nullptr;
//    }
    // 视频尺寸
    jint width = streams->codecpar->width;
    jint height = streams->codecpar->height;
    jlong bit_rate = streams->codecpar->bit_rate;
//    int format = streams->codecpar->format;

//    avcodec_free_context(&cd_ctx);
    avformat_close_input(&fmt_ctx);

    jobject res = env->NewObject(videoIndoClass, conId,
                                 width, height, during, fps, bit_rate, codecName);
    return res;
}

void JNI_OnLoad_video(JNIEnv *env) {
    jclass videoClazz = env->FindClass("jerry/filebrowser/util/VideoUtil");

    JNINativeMethod videoMethods[] = {
            {"getFFmpegVersion", "()[I",                                                    reinterpret_cast<void *>(getFFmpegVersion)},
            {"getVideoInfo",     "(Ljava/lang/String;)Ljerry/filebrowser/video/VideoInfo;", reinterpret_cast<void *>(getVideoInfo)},
    };
    if (env->RegisterNatives(videoClazz, videoMethods,
                             sizeof(videoMethods) / sizeof(videoMethods[0])) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "JNI_OnLoad", "VideoUtil动态注册失败");
    }
}