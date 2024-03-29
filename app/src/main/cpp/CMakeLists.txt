# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

project(file)

include_directories(
        D:/Project/FFmpeg/android/armv8-a_29/include
)

link_directories(
        ${PROJECT_SOURCE_DIR}/../../../libs/arm64-v8a
)

# 查找当前目录下的所有源文件cpp
#aux_source_directory(. DIR_SRCS)
#message(${DIR_SRCS})

file(GLOB_RECURSE DIR_SRCS "*.cpp" "*.h")
message(${DIR_SRCS})

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
add_library( # Sets the name of the library.
        file

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${DIR_SRCS})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        file

        # Links the target library to the log library
        # included in the NDK.
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        postproc
        swresample
        swscale
        ${log-lib})