#include <jni.h>
#include "common.h"

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

    jclass videoIndoClass = env->FindClass("jerry/filebrowser/vedio/VideoInfo");
    jmethodID conId = env->GetMethodID(videoIndoClass, "<init>", "(IIFFLjava/lang/String;)V");

    AVFormatContext *fmt_ctx = nullptr;
    if (avformat_open_input(&fmt_ctx, in_path, nullptr, nullptr) != 0) return nullptr;

    jfloat fps = -1;
    jstring codecName;
    jint width = -1;
    jint height = -1;
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
    if (codec) {
        //分配AVCodecContext空间
        AVCodecContext *cd_ctx = avcodec_alloc_context3(codec);
        //填充数据
        avcodec_parameters_to_context(cd_ctx, streams->codecpar);
        // 视频尺寸
        width = streams->codecpar->width;
        height = streams->codecpar->height;

        avcodec_free_context(&cd_ctx);
    }

    jobject res = env->NewObject(videoIndoClass, conId,
                                 width, height, during, fps, codecName);

    avformat_close_input(&fmt_ctx);
    return res;
}