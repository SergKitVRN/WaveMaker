//
// Created by sergkit on 15.08.2022.
//

#ifndef WAVEMAKER_AUDIOENGINE_H
#define WAVEMAKER_AUDIOENGINE_H


#include <aaudio/AAudio.h>
#include "Oscillator.h"

class AudioEngine {
public:
    bool start();
    void stop();
    void restart();
    void init(int32_t sampleRate);
    void setSettings(double echoDurationSec, double echoVolume, int32_t waveForm);
    void generateSound(double frequency, double fadingSec, double durationSec);
    void setVolume(double volume);
    void render(float *audioData, int32_t numFrames);
private:
    Oscillator oscillator_;
    AAudioStream *stream_;
};

#endif //WAVEMAKER_AUDIOENGINE_H
