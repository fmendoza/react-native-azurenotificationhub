package com.azure.reactnative.notificationhub;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.RESOURCE_DEF_TYPE_RAW;
import static com.azure.reactnative.notificationhub.ReactNativeConstants.RESOURCE_NAME_NOTIFICATION_SOUND;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class ReactNativeNotificationChannelBuilder {

    private String mID = ReactNativeConstants.NOTIFICATION_CHANNEL_ID;
    private CharSequence mName = "rn-push-notification-channel-name";
    private int mImportance = NotificationManager.IMPORTANCE_DEFAULT;
    private boolean mShowBadge = true;
    private boolean mEnableLights = true;
    private boolean mEnableVibration = true;
    private String mDesc = null;

    public static class Factory {
        public static ReactNativeNotificationChannelBuilder create() {
            return new ReactNativeNotificationChannelBuilder();
        }
    }

    private ReactNativeNotificationChannelBuilder() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public NotificationChannel build(Context context) {

        @SuppressLint("WrongConstant")
        NotificationChannel channel = new NotificationChannel(mID, mName, mImportance);
        channel.setShowBadge(mShowBadge);
        channel.enableLights(mEnableLights);
        channel.enableVibration(mEnableVibration);

        // Set custom sound
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        int resId = context
                .getResources()
                .getIdentifier(
                        RESOURCE_NAME_NOTIFICATION_SOUND,
                        RESOURCE_DEF_TYPE_RAW,
                        context.getPackageName()
                );

        Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + resId);

        channel.setSound(soundUri, audioAttributes);

        if (mDesc != null) {
            channel.setDescription(mDesc);
        }

        return channel;
    }

    public ReactNativeNotificationChannelBuilder setId(String id) {
        this.mID = id;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setName(CharSequence name) {
        this.mName = name;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setImportance(int importance) {
        this.mImportance = importance;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setDescription(String desc) {
        this.mDesc = desc;
        return this;
    }

    public ReactNativeNotificationChannelBuilder setShowBadge(boolean showBadge) {
        this.mShowBadge = showBadge;
        return this;
    }

    public ReactNativeNotificationChannelBuilder enableLights(boolean enableLights) {
        this.mEnableLights = enableLights;
        return this;
    }

    public ReactNativeNotificationChannelBuilder enableVibration(boolean enableVibration) {
        this.mEnableVibration = enableVibration;
        return this;
    }
}
