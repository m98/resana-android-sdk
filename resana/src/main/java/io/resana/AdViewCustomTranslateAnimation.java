/******************************************************************************
 * Copyright 2015-2016 ResanaInternal
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.resana;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

class AdViewCustomTranslateAnimation extends TranslateAnimation {
    private long mElapsedAtPause = 0;
    boolean mPaused = false;
    boolean shuoldResume;


//    long tempStartTime;

    public AdViewCustomTranslateAnimation(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AdViewCustomTranslateAnimation(float fromXDelta, float toXDelta, float fromYDelta, float toYDelta) {
        super(fromXDelta, toXDelta, fromYDelta, toYDelta);
    }

    public AdViewCustomTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue, int fromYType, float fromYValue, int toYType, float toYValue) {
        super(fromXType, fromXValue, toXType, toXValue, fromYType, fromYValue, toYType, toYValue);
    }

    @Override
    public boolean getTransformation(long currentTime, Transformation outTransformation) {
        if (mPaused) {
            if (mElapsedAtPause == 0)
                if (getStartTime() < 0) // The animation is not started yet,
                    mElapsedAtPause = 0;
                else
                    mElapsedAtPause = currentTime - getStartTime();
            setStartTime(currentTime - mElapsedAtPause);
            if (shuoldResume)
                mPaused = false;
        }
        return super.getTransformation(currentTime, outTransformation);
    }

    public void pause() {
        mElapsedAtPause = 0;
        mPaused = true;
        shuoldResume = false;
    }

    public void resume() {
        shuoldResume = true;
    }

    public long getCurrentPlayTime() {
        return SystemClock.uptimeMillis() - getStartTime() - getStartOffset();
    }

    public void setStartPosition(long time) {
        setStartOffset(-time);
    }
}
