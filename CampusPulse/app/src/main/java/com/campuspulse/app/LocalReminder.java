package com.campuspulse.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

/** 保存提醒选择，并在活动开始前通过系统闹钟发送本地通知。 */
public class LocalReminder {
    private static final String CHANNEL_ID = "campuspulse_activity_reminders";
    private static final String PREFS = "campuspulse_reminders";
    private static final String IDS = "activity_ids";

    public static int selectedMinutes(Context context, long activityId) {
        return preferences(context).getInt("minutes_" + activityId, 60);
    }

    public static boolean hasReminder(Context context, long activityId) {
        return preferences(context).getStringSet(IDS, new HashSet<>()).contains(String.valueOf(activityId));
    }

    public static boolean schedule(Context context, long activityId, String title, String startTime, int minutes) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            format.setLenient(false);
            format.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            Date start = format.parse(startTime);
            if (start == null) return false;
            long when = start.getTime() - minutes * 60_000L;
            if (when <= System.currentTimeMillis()) return false;
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarms == null) return false;
            PendingIntent pending = pendingIntent(context, activityId, title, minutes);
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pending);
            }
            SharedPreferences prefs = preferences(context);
            Set<String> ids = new HashSet<>(prefs.getStringSet(IDS, new HashSet<>()));
            ids.add(String.valueOf(activityId));
            prefs.edit().putStringSet(IDS, ids).putInt("minutes_" + activityId, minutes)
                    .putString("title_" + activityId, title).putString("start_" + activityId, startTime).apply();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void cancel(Context context, long activityId) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pending = PendingIntent.getBroadcast(context, (int) activityId,
                new Intent(context, Receiver.class), PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pending != null) {
            if (alarms != null) alarms.cancel(pending);
            pending.cancel();
        }
        SharedPreferences prefs = preferences(context);
        Set<String> ids = new HashSet<>(prefs.getStringSet(IDS, new HashSet<>()));
        ids.remove(String.valueOf(activityId));
        prefs.edit().putStringSet(IDS, ids).remove("minutes_" + activityId)
                .remove("title_" + activityId).remove("start_" + activityId).apply();
    }

    public static void cancelAll(Context context) {
        Set<String> ids = new HashSet<>(preferences(context).getStringSet(IDS, new HashSet<>()));
        for (String id : ids) cancel(context, Long.parseLong(id));
    }

    public static void restoreAll(Context context) {
        Set<String> ids = new HashSet<>(preferences(context).getStringSet(IDS, new HashSet<>()));
        for (String id : ids) {
            long activityId = Long.parseLong(id);
            SharedPreferences prefs = preferences(context);
            schedule(context, activityId, prefs.getString("title_" + id, "校园活动"),
                    prefs.getString("start_" + id, ""), prefs.getInt("minutes_" + id, 60));
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static PendingIntent pendingIntent(Context context, long activityId, String title, int minutes) {
        Intent intent = new Intent(context, Receiver.class);
        intent.putExtra("activityId", activityId);
        intent.putExtra("title", title);
        intent.putExtra("minutes", minutes);
        return PendingIntent.getBroadcast(context, (int) activityId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static class Receiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
            long activityId = intent.getLongExtra("activityId", 0);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "活动提醒",
                        NotificationManager.IMPORTANCE_DEFAULT));
            }
            Intent detail = new Intent(context, ActivityDetailActivity.class);
            detail.putExtra("activityId", activityId);
            PendingIntent open = PendingIntent.getActivity(context, (int) activityId, detail,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
            Notification notification = builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("活动即将开始")
                    .setContentText(intent.getStringExtra("title") + " · 点击查看详情")
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build();
            manager.notify((int) activityId, notification);
        }
    }

    public static String reminderLabel(int minutes) {
        if (minutes == 1440) return "1 天";
        if (minutes == 60) return "1 小时";
        return minutes + " 分钟";
    }

    public static class BootReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            restoreAll(context);
        }
    }
}
