package com.example.ignc.helpers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class AndroidHelper {
    private final Context context;
    private final ContentResolver contentResolver;
    private BrightnessObserver brightnessObserver;
    private OnBrightnessChangeListener brightnessChangeListener;

    public interface OnBrightnessChangeListener {
        void onBrightnessChanged(int brightness);
    }

    public AndroidHelper(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    public void startMonitoringBrightness(OnBrightnessChangeListener listener) {
        this.brightnessChangeListener = listener;

        if (brightnessObserver == null) {
            brightnessObserver = new BrightnessObserver(new Handler(Looper.getMainLooper()));
            contentResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                    false,
                    brightnessObserver
            );
            notifyCurrentBrightness();
        }
    }
    public void stopMonitoringBrightness() {
        if (brightnessObserver != null) {
            contentResolver.unregisterContentObserver(brightnessObserver);
            brightnessObserver = null;
            brightnessChangeListener = null;
        }
    }

    private void notifyCurrentBrightness() {
        try {
            int brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS);
            if (brightnessChangeListener != null) {
                brightnessChangeListener.onBrightnessChanged(brightness);
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e("AndroidHelper", "Erro ao obter brilho da tela", e);
            if (brightnessChangeListener != null) {
                brightnessChangeListener.onBrightnessChanged(-1); // Valor inválido em caso de erro
            }
        }
    }

    private class BrightnessObserver extends ContentObserver {
        public BrightnessObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            notifyCurrentBrightness();
        }
    }
}