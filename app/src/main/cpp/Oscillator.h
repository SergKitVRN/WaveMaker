//
// Created by sergkit on 15.08.2022.
//

#ifndef WAVEMAKER_OSCILLATOR_H
#define WAVEMAKER_OSCILLATOR_H


#include "EchoEffect.h"
#include <atomic>
#include <stdint.h>
#include <mutex>

#define HARMONICS_COUNT 7
#define GOLDEN_RATIO 1.618033988

class Oscillator {
public:
    void init(int32_t sampleRate);
    void setSettings(double echoDurationSec, double echoVolume, int32_t waveForm);
    void generateSound(double frequency, double fadingSec, double durationSec);
    void setVolume(double volume);
    void render(float *audioData, int32_t numFrames);

private:
    std::mutex mutex_;
    EchoEffect *echo_;
    double phases_[HARMONICS_COUNT];
    double phasesIncrements_[HARMONICS_COUNT];
    double harmonicsVolumes_[HARMONICS_COUNT];
    double sampleRate_ = 44100.0;
    double volume_ = 0;
    double startVolume_ = 0;
    int32_t durationCounter_ = 0;
    double fadeExponent_ = 0;
    double fadeEndCoefficient = 1;
    int32_t waveform_ = 0;
    bool fadeIn_ = false;
};

#endif //WAVEMAKER_OSCILLATOR_H
