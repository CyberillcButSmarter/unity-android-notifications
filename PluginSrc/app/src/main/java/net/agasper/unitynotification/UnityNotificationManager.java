package net.agasper.unitynotification;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class UnityNotificationManager extends BroadcastReceiver
{
    private static Set<String> channels = new HashSet<>();

    private static final String EXTRA_TICKER = "ticker";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_ID = "id";
    private static final String EXTRA_COLOR = "color";
    private static final String EXTRA_SOUND = "sound";
    private static final String EXTRA_SOUND_NAME = "soundName";
    private static final String EXTRA_VIBRATE = "vibrate";
    private static final String EXTRA_LIGHTS = "lights";
    private static final String EXTRA_LARGE_ICON = "l_icon";
    private static final String EXTRA_SMALL_ICON = "s_icon";
    private static final String EXTRA_BUNDLE = "bundle";
    private static final String EXTRA_CHANNEL = "channel";
    private static final String EXTRA_ACTIONS_BUNDLE = "actionsBundle";
    private static final String EXTRA_ACTIONS = "actions";

    public static void CreateChannel(String identifier, String name, String description, int importance, String soundName, int enableLights, int lightColor, int enableVibration, long[] vibrationPattern, String bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;

        channels.add(identifier);

        NotificationManager nm = (NotificationManager) UnityPlayer.currentActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(identifier, name, importance);
        channel.setDescription(description);
        if (soundName != null) {
            Resources res = UnityPlayer.currentActivity.getResources();
            int id = res.getIdentifier(soundName, "raw", UnityPlayer.currentActivity.getPackageName());
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            if (id != 0) {
                channel.setSound(Uri.parse("android.resource://" + bundle + "/" + id), audioAttributes);
            }
        }
        channel.enableLights(enableLights == 1);
        channel.setLightColor(lightColor);
        channel.enableVibration(enableVibration == 1);
        if (vibrationPattern == null)
            vibrationPattern = new long[] { 1000L, 1000L };
        channel.setVibrationPattern(vibrationPattern);
        nm.createNotificationChannel(channel);
    }

    @TargetApi(24)
    private static void createChannelIfNeeded(String identifier, String name, String soundName, boolean enableLights, boolean enableVibration, String bundle) {
        if (channels.contains(identifier))
            return;
        channels.add(identifier);

        CreateChannel(identifier, name, identifier + " notifications", NotificationManager.IMPORTANCE_DEFAULT, soundName, enableLights ? 1 : 0, Color.GREEN, enableVibration ? 1 : 0, null, bundle);
    }

    public static void SetNotification(int id, long delayMs, String title, String message, String ticker, int sound, String soundName, int vibrate,
                                       int lights, String largeIconResource, String smallIconResource, int bgColor, String bundle, String channel,
                                       ArrayList<NotificationAction> actions)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channel == null)
                channel = "default";
            createChannelIfNeeded(channel, title, soundName, lights == 1, vibrate == 1, bundle);
        }

        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity == null)
            return;

        AlarmManager am = (AlarmManager)currentActivity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(currentActivity, UnityNotificationManager.class);
        intent.putExtra(EXTRA_TICKER, ticker);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_COLOR, bgColor);
        intent.putExtra(EXTRA_SOUND, sound == 1);
        intent.putExtra(EXTRA_SOUND_NAME, soundName);
        intent.putExtra(EXTRA_VIBRATE, vibrate == 1);
        intent.putExtra(EXTRA_LIGHTS, lights == 1);
        intent.putExtra(EXTRA_LARGE_ICON, largeIconResource);
        intent.putExtra(EXTRA_SMALL_ICON, smallIconResource);
        intent.putExtra(EXTRA_BUNDLE, bundle);
        intent.putExtra(EXTRA_CHANNEL, channel);
        Bundle b = new Bundle();
        b.putParcelableArrayList(EXTRA_ACTIONS, actions);
        intent.putExtra(EXTRA_ACTIONS_BUNDLE, b);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;

        long triggerAt = System.currentTimeMillis() + delayMs;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(currentActivity, id, intent, pendingFlags);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        else
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    }

    public static void SetRepeatingNotification(int id, long delayMs, String title, String message, String ticker, long rep, int sound, String soundName, int vibrate, int lights,
                                                String largeIconResource, String smallIconResource, int bgColor, String bundle, String channel, ArrayList<NotificationAction> actions)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (channel == null)
                channel = "default";
            createChannelIfNeeded(channel, title, soundName, lights == 1, vibrate == 1, bundle);
        }

        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity == null)
            return;

        AlarmManager am = (AlarmManager)currentActivity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(currentActivity, UnityNotificationManager.class);
        intent.putExtra(EXTRA_TICKER, ticker);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_COLOR, bgColor);
        intent.putExtra(EXTRA_SOUND, sound == 1);
        intent.putExtra(EXTRA_SOUND_NAME, soundName);
        intent.putExtra(EXTRA_VIBRATE, vibrate == 1);
        intent.putExtra(EXTRA_LIGHTS, lights == 1);
        intent.putExtra(EXTRA_LARGE_ICON, largeIconResource);
        intent.putExtra(EXTRA_SMALL_ICON, smallIconResource);
        intent.putExtra(EXTRA_BUNDLE, bundle);
        intent.putExtra(EXTRA_CHANNEL, channel);
        Bundle b = new Bundle();
        b.putParcelableArrayList(EXTRA_ACTIONS, actions);
        intent.putExtra(EXTRA_ACTIONS_BUNDLE, b);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;

        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMs, rep, PendingIntent.getBroadcast(currentActivity, id, intent, pendingFlags));
    }

    public void onReceive(Context context, Intent intent)
    {
        // Android 13+ requires runtime POST_NOTIFICATIONS permission.
        // If the user denied it, NotificationManager.notify() would effectively no-op.
        // We simply return to avoid extra work.
        if (Build.VERSION.SDK_INT >= 33) {
            if (context.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null)
            return;

        String ticker = intent.getStringExtra(EXTRA_TICKER);
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String s_icon = intent.getStringExtra(EXTRA_SMALL_ICON);
        String l_icon = intent.getStringExtra(EXTRA_LARGE_ICON);
        int color = intent.getIntExtra(EXTRA_COLOR, 0);
        String bundle = intent.getStringExtra(EXTRA_BUNDLE);
        Boolean sound = intent.getBooleanExtra(EXTRA_SOUND, false);
        String soundName = intent.getStringExtra(EXTRA_SOUND_NAME);
        Boolean vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, false);
        Boolean lights = intent.getBooleanExtra(EXTRA_LIGHTS, false);
        int id = intent.getIntExtra(EXTRA_ID, 0);
        String channel = intent.getStringExtra(EXTRA_CHANNEL);
        Bundle b = intent.getBundleExtra(EXTRA_ACTIONS_BUNDLE);
        ArrayList<NotificationAction> actions = null;
        if (b != null && b.containsKey(EXTRA_ACTIONS)) {
            actions = b.getParcelableArrayList(EXTRA_ACTIONS);
        }

        Resources res = context.getResources();

        Intent notificationIntent = null;
        if (bundle != null) {
            notificationIntent = context.getPackageManager().getLaunchIntentForPackage(bundle);
        }
        if (notificationIntent == null) {
            // Fallback: Unity's default activity.
            notificationIntent = new Intent(context, UnityPlayerActivity.class);
        }
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, notificationIntent, pendingFlags);

        if (channel == null)
            channel = "default";

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(context, channel);
        } else {
            builder = new NotificationCompat.Builder(context);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        builder.setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(message);

        builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOnlyAlertOnce(true)
                .setGroup(channel)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());

        if (message != null && (message.length() > 40 || message.contains("\n"))) {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(color);

        if (ticker != null && ticker.length() > 0)
            builder.setTicker(ticker);

        if (s_icon != null && s_icon.length() > 0)
            builder.setSmallIcon(res.getIdentifier(s_icon, "drawable", context.getPackageName()));

        if (l_icon != null && l_icon.length() > 0)
            builder.setLargeIcon(BitmapFactory.decodeResource(res, res.getIdentifier(l_icon, "drawable", context.getPackageName())));

        if (sound) {
            if (soundName != null) {
                int identifier = res.getIdentifier(soundName, "raw", context.getPackageName());
                if (identifier != 0) {
                    builder.setSound(Uri.parse("android.resource://" + bundle + "/" + identifier));
                } else {
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                }
            } else
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }

        if (vibrate)
            builder.setVibrate(new long[] { 0L, 250L, 250L, 250L });

        if (lights)
            builder.setLights(Color.GREEN, 3000, 3000);

        if (actions != null) {
            for (int i = 0; i < actions.size(); i++) {
                NotificationAction action = actions.get(i);
                int icon = 0;
                if (action.getIcon() != null && action.getIcon().length() > 0)
                    icon = res.getIdentifier(action.getIcon(), "drawable", context.getPackageName());
                builder.addAction(icon, action.getTitle(), buildActionIntent(action, id, i, context));
            }
        }

        Notification notification = builder.build();
        notificationManager.notify(id, notification);
    }

    private static PendingIntent buildActionIntent(NotificationAction action, int notificationId, int actionIndex, Context context) {
        Intent intent = new Intent(context, UnityNotificationActionHandler.class);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("actionIndex", actionIndex);
        intent.putExtra("gameObject", action.getGameObject());
        intent.putExtra("handlerMethod", action.getHandlerMethod());
        intent.putExtra("actionId", action.getIdentifier());
        intent.putExtra("foreground", action.isForeground());

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;

        int requestCode = (notificationId & 0x00FFFFFF) | ((actionIndex & 0xFF) << 24);
        return PendingIntent.getBroadcast(context, requestCode, intent, pendingFlags);
    }

    public static void CancelPendingNotification(int id)
    {
        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity == null)
            return;

        AlarmManager am = (AlarmManager)currentActivity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(currentActivity, UnityNotificationManager.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(currentActivity, id, intent, pendingFlags);
        am.cancel(pendingIntent);
    }

    public static void ClearShowingNotifications()
    {
        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity == null)
            return;

        NotificationManager nm = (NotificationManager)currentActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancelAll();
    }
}
