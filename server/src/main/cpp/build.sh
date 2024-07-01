#!/bin/bash
#
# Copyright (C) 2024 ankio(ankio@ankio.net)
# Licensed under the Apache License, Version 3.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-3.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#   limitations under the License.
#

rm -rf CMakeCache.txt
rm -rf CMakeFiles
rm -rf cmake_install.cmake
rm -rf Makefile
rm -rf CTestTestfile.cmake

ANDROID_NDK_HOME="~/Library/Android/sdk/ndk/26.1.10909125"

if [[ "$@" =~ "-d" ]];then
        echo "----------------------------cmake debug----------------------------"
cmake -DDEBUG=ON -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
      -DANDROID_NDK=$ANDROID_NDK_HOME \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_TOOLCHAIN=clang \
      -DANDROID_PLATFORM=android-26 \
      -DANDROID_STL=c++_static \
	  .
else
        echo "----------------------------cmake release----------------------------"
cmake -DDEBUG=NO -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake \
      -DANDROID_NDK=${ANDROID_NDK_HOME} \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_TOOLCHAIN=clang \
      -DANDROID_PLATFORM=android-26 \
      -DANDROID_STL=c++_static \
	  .
fi

make

adb push ../assets/shell/arm64-v8a/auto_accounting_starter /data/local/tmp
adb shell chmod 777 /data/local/tmp/auto_accounting_starter
adb shell /data/local/tmp/auto_accounting_starter

