#include <jni.h>
#include <android/input.h>
#include "AudioEngine.h"

static AudioEngine *audioEngine = new AudioEngine();

extern "C" {
    JNIEXPORT void JNICALL Java_com_example_wavemaker_MainActivity_generateSound(JNIEnv *env, jobject obj, double frequency, double fadingSec, double durationSec) {
        audioEngine->generateSound(frequency, fadingSec, durationSec);
    }

    JNIEXPORT void JNICALL Java_com_example_wavemaker_MainActivity_setSettings(JNIEnv *env, jobject obj, double echoDurationSec, double echoVolume, int32_t waveForm) {
        audioEngine->setSettings(echoDurationSec, echoVolume, waveForm);
    }

    JNIEXPORT void JNICALL Java_com_example_wavemaker_MainActivity_setVolume(JNIEnv *env, jobject obj, jdouble volume) {
        audioEngine->setVolume(volume);
    }

    JNIEXPORT void JNICALL Java_com_example_wavemaker_MainActivity_startEngine(JNIEnv *env, jobject /* this */) {
        audioEngine->start();
    }

    JNIEXPORT void JNICALL Java_com_example_wavemaker_MainActivity_stopEngine(JNIEnv *env, jobject /* this */) {
        audioEngine->stop();
    }

}