//
// Created by sergkit on 15.08.2022.
//

#include "Oscillator.h"
//#include <math.h>
#include <cstring>

#define PI 3.14159
#define FADE_END_PERCENT 1.0
#define AMPLITUDE 0.5

void Oscillator::init(int32_t sampleRate) {
    echo_ = new EchoEffect(sampleRate);
    sampleRate_ = sampleRate;
    fadeEndCoefficient = log(FADE_END_PERCENT / 100.0);
}

void Oscillator::setSettings(double echoDurationSec, double echoVolume, int32_t waveForm) {
    std::lock_guard<std::mutex> lock(mutex_);
    echo_->configure(echoDurationSec, echoVolume);
    waveform_ = waveForm;
}

void Oscillator::generateSound(double frequency, double fadingSec, double durationSec) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (durationSec > 0.001) {
        fadeIn_ = true;
        fadeExponent_ = exp(fadeEndCoefficient / ((double) sampleRate_ * fadingSec));
        durationCounter_ = durationSec * sampleRate_;
    } else {
        fadeIn_ = false;
        fadeExponent_ = 1.0;
        durationCounter_ = INT32_MAX;
        volume_ = startVolume_;
    }
    double s = 0, v = 1;
    for (int i = 0; i < HARMONICS_COUNT; i++) {
        v = v / GOLDEN_RATIO;
        s += v;
    }
    double k = 1/s;
    v = 1;
    for (int i = 0; i < HARMONICS_COUNT; i++) {
        v = v / GOLDEN_RATIO;
        harmonicsVolumes_[i] = v * k;
        phasesIncrements_[i] = (2 * PI * ((double)i + 1) * frequency) / sampleRate_;
    }

}

void Oscillator::setVolume(double volume) {
    std::lock_guard<std::mutex> lock(mutex_);
    startVolume_ = fmin(volume, 1);
    startVolume_ = fmax(volume, 0);
}

void Oscillator::render(float *audioData, int32_t numFrames){
    std::lock_guard<std::mutex> lock(mutex_);
    double output;
    for (int frameNum = 0; frameNum < numFrames; frameNum++) {
        switch(waveform_){
            case 0:
                output = sin(phases_[0]);
                break;
            case 1:
                output = 0;
                for (int i = 0; i < HARMONICS_COUNT; i++) {
                    output += harmonicsVolumes_[i] * sin(phases_[i]);
                }
                break;
            case 2:
                if (phases_[0] < PI) {
                    output = 0.7;
                } else {
                    output = -0.7;
                }
                break;
            case 3:
                if (phases_[0] < PI) {
                    output = 2 * (0.5 * PI - phases_[0]) / PI;
                } else {
                    output = 2 * (phases_[0] - 1.5 * PI) / PI;
                }
                break;
            case 4:
                if (phases_[0] < PI) {
                    output = phases_[0] / PI;
                } else {
                    output = (PI - phases_[0]) / PI;
                }
                break;
            case 5:
                output = (phases_[0] - PI) / PI;
                break;
        }
        for (int i = 0; i < HARMONICS_COUNT; i++) {
            phases_[i] += phasesIncrements_[i];
            if (phases_[i] > 2 * PI || volume_ == 0) {
                phases_[i] = 0;
            }
        }
        if (fadeIn_ && volume_ < startVolume_) {
            volume_ += 0.01;
            if (volume_ >= startVolume_) {
                fadeIn_ = false;
            }
        } else if (durationCounter_) {
            durationCounter_--;
            volume_ = volume_ * fadeExponent_;
        } else {
            volume_ = 0;
        }

        output = echo_->process(volume_ * output);
        audioData[frameNum] = (float)output;
    }
}


/*    phaseIncrement1_ = (TWO_PI * currentFrequency1_) / currentSampleRate_;
    phaseIncrement2_ = (TWO_PI * currentFrequency2_) / currentSampleRate_;
    phaseIncrementHarmonics_[0] = (TWO_PI * currentFrequency1_ / 1.618033) / currentSampleRate_;
    phaseIncrementHarmonics_[1] = (TWO_PI * currentFrequency1_ * 1.618033) / currentSampleRate_;

}

void Oscillator::setFrequency(double frequency1, double frequency2) {
    currentFrequency1_ = frequency1;
    currentFrequency2_ = frequency2;
    updatePhaseIncrement();
}

void Oscillator::setSampleRate(int32_t sampleRate) {
    currentSampleRate_ = (double)sampleRate;
    bufSize_ = (unsigned int)(delaySecond_ * (double)sampleRate);
    if (buf_ != NULL) delete(buf_);
    buf_ = new float [bufSize_];
    memset(buf_, 0, bufSize_);
    bufPtr_ = 0;
    updatePhaseIncrement();
}

void Oscillator::setWaveOn(bool isWaveOn) {
    isWaveOn_.store(isWaveOn);
}

void Oscillator::setVolume(double volume) {

    currentVolume_ = volume;
}

void Oscillator::render(float *audioData, int32_t numFrames) {
    double a1, a2;
    if (!isWaveOn_.load()) {
        phase1_ = 0;
        phase2_ = 0;
        for (int i = 0; i < HARMONICS_COUNT; i++) {
            phaseHarmonics_[i] = 0;
        }
    }

    for (int i = 0; i < numFrames; i++) {

        if (isWaveOn_.load()) {
            // Calculates the next sample value for the sine wave.
            a1 = sin(phase1_);
            a2 = sin(phase2_);

            double h0 = sin(phaseHarmonics_[0]);
            double h1 = sin(phaseHarmonics_[1]);

            float newVal = (float) (AMPLITUDE * currentVolume_ * (0.618033 * a1 + 0.381967 * (0.23607 * h0 + 0.145897 * h1)));
            audioData[i] = 0.381967 * buf_[bufPtr_] + 0.618033 * newVal;
            buf_[bufPtr_] = audioData[i];
            bufPtr_++;
            if (bufPtr_ == bufSize_) bufPtr_ = 0;

            // Increments the phase, handling wrap around.
            phase1_ += phaseIncrement1_;
            phase2_ += phaseIncrement2_;
            if (phase1_ > TWO_PI) phase1_ -= TWO_PI;
            if (phase2_ > TWO_PI) phase2_ -= TWO_PI;
            for (int i = 0; i < HARMONICS_COUNT; i++) {
                phaseHarmonics_[i] += phaseIncrementHarmonics_[i];
                if (phaseHarmonics_[i] > TWO_PI) phaseHarmonics_[i] -= TWO_PI;
            }
        } else {
            // Outputs silence by setting sample value to zero.
            audioData[i] = 0;
        }
    }
}

/*

Экспоненциальное  затухание громкости

    e = 2.71828
    N = 100
    step = 1/N
    k = pow(e, -5 * step)
    x1 = 0
    y2 = 1

    for i in range(N):
            y1 = pow(e, -5 * x1)
    print("x=%.3f \t %.3f - %.3f = %.3f" % (x1, y1, y2, y1-y2))
    x1 += step
            y2 = y2 * k


    print("=== END ====")
*/


