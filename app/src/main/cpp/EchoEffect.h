//
// Created by sergkit on 02.09.2022.
//

#ifndef WAVEMAKER_ECHOEFFECT_H
#define WAVEMAKER_ECHOEFFECT_H

#include <math.h>


#define MAX_ECHO_DELAY_SEC 5

class EchoEffect {
    public:
        explicit EchoEffect(int sampleRate) {
            maxSize_ = sampleRate * MAX_ECHO_DELAY_SEC;
            buf_ = new double[maxSize_];
            sampleRate_ = sampleRate;
        };
        double process(double val) {
            double res = volume_ * buf_[ptr_];
            res = (res + val) / 2.;
            res = fmin(res,1);
            res = fmax(res, -1);
            buf_[ptr_] = res;
            ptr_++;
            if (ptr_ >= ptrLimit_) {
                ptr_ = 0;
            }
            return res;
        };

        void configure(double duration_sec, double volume){
            duration_sec = fmax(duration_sec, 0);
            volume_ = fmin(volume, 1);
            volume_ = fmax(volume, 0);
            ptrLimit_ = fmin(maxSize_, (double)sampleRate_ * duration_sec);
        };

   private:
        double *buf_ = NULL;
        int ptr_ = 0;
        int ptrLimit_ = 0;
        double volume_ = 0;
        int maxSize_ = 0;
        int sampleRate_ = 1;
};

#endif //WAVEMAKER_ECHOEFFECT_H
