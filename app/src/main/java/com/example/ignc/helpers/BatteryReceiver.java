package com.example.ignc.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryReceiver extends BroadcastReceiver {
    private OnBatteryLevelChanged listener;
    public BatteryReceiver(OnBatteryLevelChanged listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPercentage = (level * 100) / (float) scale;

        if (listener != null) {
            listener.onBatteryLevelChanged(batteryPercentage);
        }
    }

    public interface OnBatteryLevelChanged {
        void onBatteryLevelChanged(float percentage);
    }
}