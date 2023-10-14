package com.azure.reactnative.notificationhub;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static com.azure.reactnative.notificationhub.ReactNativeConstants.*;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class ReactNativeFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ReactNativeFMS";

    /**
     * Maximum allowed notifications in the notification tray
     */
    private static final Integer NOTIFICATION_VISIBLE_LIMIT = 20;

    /**
     * Identifier for ranker-group notifications
     */
    private static final String NOTIFICATION_RANKER_GROUP = "ranker_group";

    /**
     * Comparator for sorting notifications by their post time in ascending order.
     */
    private final Comparator<StatusBarNotification> NOTIFICATION_COMPARATOR_BY_POST_TIME =
            (o1, o2) -> (int) (o1.getPostTime() - o2.getPostTime());

    private static String notificationChannelID;

    public static void createNotificationChannel(Context context) {
        if (notificationChannelID == null) {
            ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();
            ReactNativeNotificationChannelBuilder builder = ReactNativeNotificationChannelBuilder.Factory.create();

            if (notificationHubUtil.hasChannelName(context)) {
                builder.setName(notificationHubUtil.getChannelName(context));
            }

            if (notificationHubUtil.hasChannelDescription(context)) {
                builder.setDescription(notificationHubUtil.getChannelDescription(context));
            }

            if (notificationHubUtil.hasChannelImportance(context)) {
                builder.setImportance(notificationHubUtil.getChannelImportance(context));
            }

            if (notificationHubUtil.hasChannelShowBadge(context)) {
                builder.setShowBadge(notificationHubUtil.getChannelShowBadge(context));
            }

            if (notificationHubUtil.hasChannelEnableLights(context)) {
                builder.enableLights(notificationHubUtil.getChannelEnableLights(context));
            }

            if (notificationHubUtil.hasChannelEnableVibration(context)) {
                builder.enableVibration(notificationHubUtil.getChannelEnableVibration(context));
            }

            notificationChannelID = NOTIFICATION_CHANNEL_ID;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = builder.build(context);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    notificationChannelID = channel.getId();
                }
            }
        }
    }

    public static void deleteNotificationChannel(Context context) {
        if (notificationChannelID != null) {
            final String channelToDeleteID = notificationChannelID;
            notificationChannelID = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                notificationManager.deleteNotificationChannel(channelToDeleteID);
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Intent intent = ReactNativeNotificationHubUtil.IntentFactory.createIntent(this, ReactNativeRegistrationIntentService.class);
        ReactNativeRegistrationIntentService.enqueueWork(this, intent);
    }

    /**
     * Gets called when a notification is received
     * on the foreground and background.
     */
    @Override
    public void handleIntent(Intent intent) {

        try {
            if (intent.getExtras() != null) {
                String className = ReactNativeFirebaseMessagingService.class.getSimpleName();
                RemoteMessage.Builder builder = new RemoteMessage.Builder(className);

                for (String key : intent.getExtras().keySet()) {
                    builder.addData(key, intent.getExtras().get(key).toString());
                }

                onMessageReceived(builder.build());
            }
            else {
                super.handleIntent(intent);
            }
        }
        catch (Exception e) {
            super.handleIntent(intent);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        ReactNativeNotificationHubUtil notificationHubUtil = ReactNativeNotificationHubUtil.getInstance();

        if (notificationChannelID == null) {
            createNotificationChannel(this);
        }

        Bundle bundle = remoteMessage.toIntent().getExtras();

        // Retrieve notification body from google notification payload
        if (bundle.get(KEY_REMOTE_GCM_NOTIFICATION_BODY) != null) {
            bundle.putString(KEY_REMOTE_NOTIFICATION_BODY, bundle.getString(KEY_REMOTE_GCM_NOTIFICATION_BODY));
        }

        // Retrieve notification title from google notification payload
        if (bundle.get(KEY_REMOTE_GCM_NOTIFICATION_TITLE) != null) {
            bundle.putString(KEY_REMOTE_NOTIFICATION_TITLE, bundle.getString(KEY_REMOTE_GCM_NOTIFICATION_TITLE));
        }

        // Try to cancel the oldest visible notification to ensure
        // that the app can continue displaying new notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cancelOldestVisibleNotification();
        }

        if (bundle != null && notificationHubUtil.getAppIsForeground()) {
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_FOREGROUND, true);
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_USER_INTERACTION, false);
            bundle.putBoolean(KEY_REMOTE_NOTIFICATION_COLDSTART, false);
        } else {
            ReactNativeNotificationsHandler.sendNotification(this, bundle, notificationChannelID);
        }

        ReactNativeNotificationsHandler.sendBroadcast(this, bundle, 0);
    }

    /**
     * Cancels the oldest notification visible in the notification tray
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void cancelOldestVisibleNotification() {

        // Initialize notification manager
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        // Get an array of currently visible notifications
        StatusBarNotification[] statusBarNotifications = notificationManager
                .getActiveNotifications();

        if (statusBarNotifications != null) {

            // Convert to an ArrayList for easier manipulation
            ArrayList<StatusBarNotification> notifications =
                    new ArrayList<>(Arrays.asList(statusBarNotifications));

            // Exclude ranker-group notifications (if supported)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notifications.removeIf(
                        notification -> Objects.equals(notification.getTag(), NOTIFICATION_RANKER_GROUP));
            }

            // Check if the number of visible notifications exceeds the limit
            if (notifications.size() >= NOTIFICATION_VISIBLE_LIMIT) {

                // Sort notifications by post time in ascending order (if supported)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notifications.sort(NOTIFICATION_COMPARATOR_BY_POST_TIME);
                }

                // Cancel the oldest notification
                StatusBarNotification notificationCanceled = notifications.get(0);
                notificationManager.cancel(notificationCanceled.getId());
            }
        }
    }
}
