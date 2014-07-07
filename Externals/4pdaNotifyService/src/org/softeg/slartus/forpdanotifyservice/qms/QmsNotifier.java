package org.softeg.slartus.forpdanotifyservice.qms;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.softeg.slartus.forpdaapi.QmsUser;
import org.softeg.slartus.forpdaapi.QmsUserThemes;
import org.softeg.slartus.forpdaapi.QmsUsers;
import org.softeg.slartus.forpdaapi.Qms_2_0;
import org.softeg.slartus.forpdacommon.ExtDateFormat;
import org.softeg.slartus.forpdacommon.ExtPreferences;
import org.softeg.slartus.forpdacommon.NotificationBridge;
import org.softeg.slartus.forpdanotifyservice.Client;
import org.softeg.slartus.forpdanotifyservice.MainService;
import org.softeg.slartus.forpdanotifyservice.NotifierBase;
import org.softeg.slartus.forpdanotifyservice.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: slinkin
 * Date: 31.05.13
 * Time: 8:20
 * To change this template use File | Settings | File Templates.
 */
public class QmsNotifier extends NotifierBase {
    private static final String LOG_TAG = "QmsMainService.QmsNotifier";
    public static final String NEW_ACTION = "org.softeg.slartus.forpdanotifyservice.newqms";
    public static final String TIME_OUT_KEY = "qms.service.timeout";
    public static final String LAST_DATETIME_KEY = "qms.service.lastdatetime";
    public static final String UNREAD_MESSAGE_USERS_COUNT_KEY = "UnreadMessageUsersCount";
    public static final String HAS_UNREAD_MESSAGE_KEY = "HasUnreadMessage";

    public QmsNotifier(Context context) {
        super(context);
    }


    public static void restartTask(final Context context, Intent intent) {
        MainService.readCookiesPath(context, intent);
        QmsNotifier qmsNotifier = new QmsNotifier(context);
        qmsNotifier.readSettings(context, intent);
        qmsNotifier.restartTask(context);
    }

    public static Boolean isUse(Context context) {
        return ExtPreferences.getBoolean(context, "qms.service.use", false);
    }

    public void checkUpdates() {
        try {
            Log.i(LOG_TAG, "checkQms.start");
            if (!isUse(getContext()))
                return;

            String cookiesPath = loadCookiesPath();

            // Log.d(LOG_TAG, "CookiesPath " + cookiesPath);

            if (cookiesPath == null)
                return;


            Client client = new Client(cookiesPath);
            QmsUsers qmsUsers = Qms_2_0.getQmsSubscribers(client);
            // Log.d(LOG_TAG, "QmsUsers.size=" + qmsUsers.size());

            Intent intent = new Intent(NEW_ACTION);
            intent.putExtra(UNREAD_MESSAGE_USERS_COUNT_KEY, qmsUsers.unreadMessageUsersCount());
            Boolean hasUnread = false;
            if (qmsUsers.size() > 0 && checkUser(client, qmsUsers, qmsUsers.get(0))) {
                hasUnread = true;
                intent.putExtra(HAS_UNREAD_MESSAGE_KEY, true);
            } else {
                intent.putExtra(HAS_UNREAD_MESSAGE_KEY, false);
            }
            sendNotify(getContext(), qmsUsers, hasUnread);
            getContext().sendBroadcast(intent);


            Log.i(LOG_TAG, "checkQms.end");

        } catch (Throwable throwable) {
            Log.e(LOG_TAG, throwable.toString());
        }
    }

    private SimpleDateFormat m_DateFormat = new SimpleDateFormat("dd MMM");
    private SimpleDateFormat m_TodayTimeFormat = new SimpleDateFormat("Сегодня HH:mm");
    private SimpleDateFormat m_TimeFormat = new SimpleDateFormat("HH:mm");


    private boolean checkUser(Client client, QmsUsers qmsUsers, QmsUser user) throws Throwable {
        // Log.d(LOG_TAG, "checkUser=" + user.getMid());

        if (TextUtils.isEmpty(user.getNewMessagesCount()))
            return false;

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());

        GregorianCalendar lastDateTime = loadLastDate(LAST_DATETIME_KEY);


        calendar.set(GregorianCalendar.HOUR, 0);
        calendar.set(GregorianCalendar.MINUTE, 0);
        calendar.set(GregorianCalendar.SECOND, 0);
        calendar.set(GregorianCalendar.MILLISECOND, 0);


        qmsUsers.clear();
        QmsUserThemes qmsUserThemes = Qms_2_0.getQmsUserThemes(client, user.getMid(), qmsUsers, false);

        if (qmsUsers.size() > 0 && !qmsUsers.get(0).getMid().equals(user.getMid())) {
            return checkUser(client, qmsUsers, qmsUsers.get(0));
        }

        if (qmsUserThemes.size() == 0 || qmsUserThemes.getHasNewCount() == 0)
            return false;

        if (qmsUserThemes.getHasNewCount() == 1) {

            qmsUsers.get(0).LastThemeId = qmsUserThemes.getFirstHasNew().Id;
        }

        String strDate = qmsUserThemes.get(0).Date;


        Map<String, Date> additionalHeaders = new HashMap<String, Date>();
        if ("Вчера".equals(strDate)) {
            calendar.add(GregorianCalendar.DAY_OF_YEAR, -1);

            // если самое начало вчера позднее последнего нового, значит, новое
            if (calendar.after(lastDateTime)) {
                setLastDateTime(calendar);
                return true;
            }
            calendar.set(GregorianCalendar.HOUR, 23);
            calendar.set(GregorianCalendar.MINUTE, 59);
            calendar.set(GregorianCalendar.SECOND, 59);

            if (lastDateTime == null) {
                setLastDateTime(calendar);
                return true;
            }

            // если самый конец вчера раньше последнего нового, значит, надо проверить время этого вчерашнего
            if (calendar.before(lastDateTime)) {
                setLastDateTime(calendar);
                return true;// считаем, что новое
                //return checkByTime(client, m_Mails.get(0), qmsUserThemes.get(0), calendar);// тут проверяем по времени
            }

        } else if (ExtDateFormat.tryParse(m_TimeFormat, strDate, additionalHeaders)||ExtDateFormat.tryParse(m_TodayTimeFormat, strDate, additionalHeaders)) {
            Date lastDate = additionalHeaders.get("date");
            // Log.d(LOG_TAG, "qms_last_time" + m_DateTimeFormat.format(lastDate.getTime()));
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(lastDate);
            calendar.set(GregorianCalendar.HOUR, c.get(GregorianCalendar.HOUR));
            calendar.set(GregorianCalendar.MINUTE, c.get(GregorianCalendar.MINUTE));
            calendar.set(GregorianCalendar.SECOND, c.get(0));

            if (lastDateTime == null) {
                setLastDateTime(calendar);
                return true;
            }

            if (calendar.after(lastDateTime)) {
                // Log.d(LOG_TAG, "LastDateTime=" + m_DateTimeFormat.format(lastDateTime.getTime()));
                setLastDateTime(calendar);
                return true;
            }
        } else if (ExtDateFormat.tryParse(m_DateFormat, strDate, additionalHeaders)) {
            Date lastDate = additionalHeaders.get("date");
            GregorianCalendar c = new GregorianCalendar();
            c.setTime(lastDate);

            calendar.set(GregorianCalendar.MONTH, c.get(GregorianCalendar.MONTH));
            calendar.set(GregorianCalendar.DAY_OF_YEAR, c.get(GregorianCalendar.DAY_OF_YEAR));

            if (lastDateTime == null) {
                setLastDateTime(calendar);
                return true;
            }

            // если самое начало даты позднее последнего нового, значит, новое
            if (calendar.after(lastDateTime)) {
                setLastDateTime(calendar);
                return true;
            }

            // если самый конец даты раньше последнего нового, значит, надо проверить время этого вчерашнего
            if (calendar.before(lastDateTime)) {
                setLastDateTime(calendar);
                return true;// считаем, что новое
                // return checkByTime(client, m_Mails.get(0), qmsUserThemes.get(0), calendar);// тут проверяем по времени
            }
        }

        return false;
    }

    private void setLastDateTime(GregorianCalendar calendar) {
        saveLastDate(calendar, LAST_DATETIME_KEY);
    }

    @Override
    public void readSettings(Context context, Intent intent) {
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey(QmsNotifier.TIME_OUT_KEY)) {
            float timeOut = intent.getExtras().getFloat(QmsNotifier.TIME_OUT_KEY, 5);
            // Log.d(LOG_TAG, "timeOutExtras " + timeOut);
            saveTimeOut(context, timeOut, TIME_OUT_KEY);
        }
    }

    public static int REQUEST_CODE_START = 839264710;

    private static PendingIntent getAlarmPendingIntent(Context context) {
        Intent downloader = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context,
                REQUEST_CODE_START, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
    }


    private static void restartTaskStatic(Context context) {
        float timeOut = loadTimeOut(context, TIME_OUT_KEY);
        Log.i(LOG_TAG, "checkQms.TimeOut: " + timeOut);

        PendingIntent pendingIntent = getAlarmPendingIntent(context);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), (long) (timeOut * 60000), pendingIntent);
    }


    @Override
    public void restartTask(Context context) {
        restartTaskStatic(context);
    }

    public static void cancelAlarm(Context context) {
        QmsNotifier qmsNotifier = new QmsNotifier(context);
        qmsNotifier.cancel(context);
    }

    @Override
    public void cancel(Context context) {
        try {
            AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarm.cancel(PendingIntent.getBroadcast(context, REQUEST_CODE_START, new Intent(context, AlarmReceiver.class), 0));
        } catch (Throwable ex) {
            Log.e(LOG_TAG, ex.toString());
        }
    }

    private static final int MY_NOTIFICATION_ID = 1;

    private static void sendNotify(Context context, QmsUsers mails, boolean hasUnreadMessage) {
        Log.i(LOG_TAG, "qms sendNotify");
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String url = "http://4pda.ru/forum/index.php?act=qms";
        if (mails.unreadMessageUsersCount() == 1)// если новые сообщения только с одним пользователем
            url += "&mid=" + mails.get(0).getMid();
        if (mails.get(0).LastThemeId != null)// если новые сообщения только по одной теме
            url += "&t=" + mails.get(0).LastThemeId;
        Intent marketIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url));


        if (hasUnreadMessage) {
            Log.i(LOG_TAG, "notify!");
            String message = "Новые сообщения (" + mails.unreadMessageUsersCount() + ")";
            if (mails.unreadMessageUsersCount() == 1)
                message = "Новые сообщения от " + mails.get(0).getNick() + "(" + mails.unreadMessageUsersCount() + ")";

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, marketIntent, 0);
            Notification noti = NotificationBridge.createBridge(
                    context,
                    R.drawable.icon,
                    "Имеются непрочитанные сообщения",
                    System.currentTimeMillis())
                    .setContentTitle(message)
                    .setContentText("Новые сообщения")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .createNotification();

            notificationManager.notify(MY_NOTIFICATION_ID, noti);
        } else if (mails.unreadMessageUsersCount() == 0) {
            notificationManager.cancel(MY_NOTIFICATION_ID);
        }
    }


}
