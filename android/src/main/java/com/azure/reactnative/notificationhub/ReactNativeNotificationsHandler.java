package com.azure.reactnative.notificationhub;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

public final class ReactNativeNotificationsHandler {
    public static final String TAG = "ReactNativeNotification";

    private static final long DEFAULT_VIBRATION = 300L;

    /**
     * Used for both "notification" and "data" payload types in order to notify a running ReactJS app.
     *
     * Example:
     *  {"data":{"message":"Notification Hub test notification"}} // data
     *  {"notification":{"body":"Notification Hub test notification"}} // notification
     */
    public static void sendBroadcast(final Context context, final Intent intent, final long delay) {
        ReactNativeUtil.runInWorkerThread(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().sleep(delay);
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                    localBroadcastManager.sendBroadcast(intent);
                } catch (Exception e) {
                }
            }
        });
    }

    /**
     * Used for both "notification" and "data" payload types in order to notify a running ReactJS app.
     *
     * Example:
     *  {"data":{"message":"Notification Hub test notification"}} // data
     *  {"notification":{"body":"Notification Hub test notification"}} // notification
     */
    public static void sendBroadcast(final Context context, final Bundle bundle, final long delay) {
        ReactNativeUtil.runInWorkerThread(new Runnable() {
            public void run() {
                try {
                    Thread.currentThread().sleep(delay);
                    Intent intent = ReactNativeUtil.createBroadcastIntent(TAG, bundle);
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                    localBroadcastManager.sendBroadcast(intent);
                } catch (Exception e) {
                }
            }
        });
    }

    /**
     * Used for "data" payload type in order to create a notification and announce it using
     * notification service.
     *
     * Example: {"data":{"message":"Notification Hub test notification"}}
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    public static void sendNotification(final Context context,
                                        final Bundle bundle,
                                        final String notificationChannelID) {
        ReactNativeUtil.runInWorkerThread(new Runnable() {
            public void run() {
                try {
                    Class intentClass = ReactNativeUtil.getMainActivityClass(context);
                    if (intentClass == null) {
                        Log.e(TAG, ERROR_NO_ACTIVITY_CLASS);
                        return;
                    }

                    String message = bundle.getString(KEY_REMOTE_NOTIFICATION_MESSAGE);
                    if (message == null) {
                        message = bundle.getString(KEY_REMOTE_NOTIFICATION_BODY);
                    }

                    if (message == null) {
                        Log.e(TAG, ERROR_NO_MESSAGE);
                        return;
                    }

                    Resources res = context.getResources();
                    String packageName = context.getPackageName();

                    String largeIcon = bundle.getString(KEY_REMOTE_NOTIFICATION_LARGE_ICON);
                    int largeIconResId = ReactNativeUtil.getLargeIcon(bundle, largeIcon, res, packageName);
                    Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

                    int smallIconResId = ReactNativeUtil.getSmallIcon(bundle, res, packageName);

                    String title = bundle.getString(KEY_REMOTE_NOTIFICATION_TITLE);
                    if (title == null) {
                        ApplicationInfo appInfo = context.getApplicationInfo();
                        title = context.getPackageManager().getApplicationLabel(appInfo).toString();
                    }

                    int priority = ReactNativeUtil.getNotificationCompatPriority(
                            bundle.getString(KEY_REMOTE_NOTIFICATION_PRIORITY));
                    NotificationCompat.Builder notificationBuilder = ReactNativeUtil.initNotificationCompatBuilder(
                            context,
                            notificationChannelID,
                            title,
                            bundle.getString(KEY_REMOTE_NOTIFICATION_TICKER),
                            NotificationCompat.VISIBILITY_PRIVATE,
                            priority,
                            bundle.getBoolean(KEY_REMOTE_NOTIFICATION_AUTO_CANCEL, true));


                    NotificationCompat.Builder summaryNotificationBuilder = null;

                    String group = bundle.getString(KEY_REMOTE_NOTIFICATION_GROUP);

                    if (group != null) {
                        notificationBuilder.setGroup(group);

                        summaryNotificationBuilder = ReactNativeUtil.initNotificationCompatBuilder(
                                context,
                                notificationChannelID,
                                title,
                                bundle.getString(KEY_REMOTE_NOTIFICATION_TICKER),
                                NotificationCompat.VISIBILITY_PRIVATE,
                                priority,
                                bundle.getBoolean(KEY_REMOTE_NOTIFICATION_AUTO_CANCEL, true));


                        summaryNotificationBuilder
                                .setSmallIcon(smallIconResId)
                                .setLargeIcon(largeIconBitmap)
                                .setStyle(ReactNativeUtil.getInboxStyle(title))
                                .setGroup(group)
                                .setGroupSummary(true);
                    }

                    notificationBuilder.setContentText(message);

                    String subText = bundle.getString(KEY_REMOTE_NOTIFICATION_SUB_TEXT);
                    if (subText != null) {
                        notificationBuilder.setSubText(subText);
                    }

                    String numberString = bundle.getString(KEY_REMOTE_NOTIFICATION_NUMBER);
                    if (numberString != null) {
                        notificationBuilder.setNumber(Integer.parseInt(numberString));
                    }

                    notificationBuilder.setSmallIcon(smallIconResId);

                    if (bundle.getString(KEY_REMOTE_NOTIFICATION_AVATAR_URL) == null) {
                        if (largeIconResId != 0 && (
                                largeIcon != null ||
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {
                            notificationBuilder.setLargeIcon(largeIconBitmap);
                        }
                    } else {
                        Bitmap avatar = ReactNativeUtil.fetchImage(
                                bundle.getString(KEY_REMOTE_NOTIFICATION_AVATAR_URL));
                        if (avatar != null) {
                            notificationBuilder.setLargeIcon(avatar);
                        }
                    }

                    String bigText = bundle.getString(KEY_REMOTE_NOTIFICATION_BIG_TEXT);
                    if (bigText == null) {
                        bigText = message;
                    }
                    notificationBuilder.setStyle(ReactNativeUtil.getBigTextStyle(bigText));

                    String imageUrl = bundle.getString(KEY_REMOTE_NOTIFICATION_IMAGE_URL);

                    if (imageUrl != null && ReactNativeUtil.isConnectedToWiFi(context)) {
                        Bitmap bitmap = ReactNativeUtil.fetchImage(imageUrl);
                        if (bitmap != null) {
                            notificationBuilder
                                    .setLargeIcon(bitmap)
                                    .setStyle(new NotificationCompat.BigPictureStyle()
                                            .bigPicture(bitmap)
                                            .bigLargeIcon(null));
                        }
                    }

                    // Create notification intent
                    Intent intent = ReactNativeUtil.createNotificationIntent(context, bundle, intentClass);

                    if (!bundle.containsKey(KEY_REMOTE_NOTIFICATION_PLAY_SOUND) || bundle.getBoolean(KEY_REMOTE_NOTIFICATION_PLAY_SOUND)) {
                        Uri soundUri = ReactNativeUtil.getSoundUri(context, bundle);
                        notificationBuilder.setSound(soundUri);
                    }

                    if (bundle.containsKey(KEY_REMOTE_NOTIFICATION_ONGOING)) {
                        notificationBuilder.setOngoing(bundle.getBoolean(KEY_REMOTE_NOTIFICATION_ONGOING));
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        notificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL);

                        String color = bundle.getString(KEY_REMOTE_NOTIFICATION_COLOR);
                        if (color != null) {
                            notificationBuilder.setColor(Color.parseColor(color));
                        }
                    }

                    int notificationID = bundle.getString(KEY_REMOTE_NOTIFICATION_ID).hashCode();

                    final PendingIntent pendingIntent;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        pendingIntent = PendingIntent.getActivity(context, notificationID, intent,
                                PendingIntent.FLAG_IMMUTABLE);
                    } else {
                        pendingIntent = PendingIntent.getActivity(context, notificationID, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                    }

                    notificationBuilder.setContentIntent(pendingIntent);

                    if (!bundle.containsKey(KEY_REMOTE_NOTIFICATION_VIBRATE) || bundle.getBoolean(KEY_REMOTE_NOTIFICATION_VIBRATE)) {
                        long vibration = bundle.containsKey(KEY_REMOTE_NOTIFICATION_VIBRATION) ?
                                (long) bundle.getDouble(KEY_REMOTE_NOTIFICATION_VIBRATION) : DEFAULT_VIBRATION;
                        if (vibration == 0)
                            vibration = DEFAULT_VIBRATION;
                        notificationBuilder.setVibrate(new long[]{0, vibration});
                    }

                    // Process notification's actions
                    ReactNativeUtil.processNotificationActions(context, bundle, notificationBuilder, notificationID);

                    notificationBuilder.setPriority(2);

                    Notification notification = notificationBuilder.build();
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                            Context.NOTIFICATION_SERVICE);
                    if (bundle.containsKey(KEY_REMOTE_NOTIFICATION_TAG)) {
                        String tag = bundle.getString(KEY_REMOTE_NOTIFICATION_TAG);
                        notificationManager.notify(tag, notificationID, notification);
                    } else {
                        notificationManager.notify(notificationID, notification);
                    }

                    if (summaryNotificationBuilder != null) {
                        Notification summaryNotification = summaryNotificationBuilder.build();
                        notificationManager.notify(group.hashCode(), summaryNotification);
                    }

                } catch (Exception e) {
                    Log.e(TAG, ERROR_SEND_PUSH_NOTIFICATION, e);
                }
            }
        });
    }

    private ReactNativeNotificationsHandler() {
    }
}
