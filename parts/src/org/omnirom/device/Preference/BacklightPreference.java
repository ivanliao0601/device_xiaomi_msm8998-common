/*
 * Copyright (C) 2019 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.omnirom.device.Preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.omnirom.device.R;
import org.omnirom.device.utils.FileUtils;

/**
 * Backlight Preference used to adjust led brightness of buttons.
 * <p>
 * Created by 0ranko0P <ranko0p@outlook.com> on 2019.10.30
 */
public final class BacklightPreference extends Preference implements SeekBar.OnSeekBarChangeListener {

    public static final String KEY_BTN_BRIGHTNESS = "btn_brightness";

    private static final int BACKLIGHT_MIN_BRIGHTNESS = 0;
    private static final int BACKLIGHT_MAX_BRIGHTNESS = 255;
    private static final float PROGRESS_OFFSET = BACKLIGHT_MAX_BRIGHTNESS / 100f;

    private static final String FILE_LED_LEFT = "/sys/class/leds/button-backlight/max_brightness";
    private static final String FILE_LED_RIGHT = "/sys/class/leds/button-backlight1/max_brightness";

    private TextView mValueText;

    public static final class KernelFeatureImp implements KernelFeature<Integer> {

        private KernelFeatureImp(){}

        @Override
        public boolean isSupported() {
            return FileUtils.isFileExists(FILE_LED_LEFT) && FileUtils.isFileExists(FILE_LED_RIGHT);
        }

        /**
         * @return Button brightness value that currently in use
         */
        @Override
        public Integer getCurrentValue() {
            // Read the value that currently in use, not the one from sp.
            // User might modify this value though some kernel manager.
            String currentVal = FileUtils.getFileValue(FILE_LED_LEFT, null);
            return currentVal == null ? BACKLIGHT_MAX_BRIGHTNESS : Integer.valueOf(currentVal);
        }

        @Override
        public boolean applyValue(Integer newValue) {
            String newStrVal = newValue.toString();
            return FileUtils.writeValue(FILE_LED_LEFT, newStrVal) &&
                    FileUtils.writeValue(FILE_LED_RIGHT, newStrVal);
        }

        @Override
        public void applySharedPreferences(Integer newValue, SharedPreferences sp) {
            sp.edit().putInt(KEY_BTN_BRIGHTNESS, newValue).apply();
        }

        @Override
        public boolean restore(SharedPreferences sp) {
            if(!isSupported()) return false;

            int storedValue = sp.getInt(KEY_BTN_BRIGHTNESS, BACKLIGHT_MAX_BRIGHTNESS);
            return applyValue(storedValue);
        }
    }

    public static KernelFeatureImp FEATURE = new KernelFeatureImp();

    public BacklightPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_seek_bar);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mValueText = (TextView) holder.findViewById(R.id.current_value);

        // Todo: W: I/O operation on main thread
        int brValue = FEATURE.getCurrentValue();
        SeekBar mSeekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekBar.setMax(BACKLIGHT_MAX_BRIGHTNESS);
        mSeekBar.setProgress(brValue);
        mSeekBar.setOnSeekBarChangeListener(this);
        updateText(brValue);
    }


    private void updateText(int progress) {
        mValueText.setText(Math.round(progress / PROGRESS_OFFSET) + "%");
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        FEATURE.applyValue(progress);
        updateText(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        FEATURE.applySharedPreferences(seekBar.getProgress(), getSharedPreferences());
    }
}