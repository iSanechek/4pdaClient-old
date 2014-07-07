package org.softeg.slartus.forpda.download;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.softeg.slartus.forpda.Client;
import org.softeg.slartus.forpda.QuickStartActivity;
import org.softeg.slartus.forpda.R;
import org.softeg.slartus.forpda.Tabs.DownloadsTab;
import org.softeg.slartus.forpda.classes.DownloadTask;
import org.softeg.slartus.forpdacommon.FileUtils;
import org.softeg.slartus.forpdacommon.NotificationBridge;

import java.io.UnsupportedEncodingException;


/**
 * User: slinkin
 * Date: 30.07.12
 * Time: 10:31
 */
public class DownloadReceiver extends ResultReceiver {
    private Handler m_Handler;
    private Context m_Context;

    public DownloadReceiver(Handler handler, Context context) {
        super(handler);
        m_Handler = handler;
        m_Context = context;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        if (resultCode != DownloadsService.UPDATE_PROGRESS) return;
        int notificationId = resultData.getInt("downloadTaskId");

        final DownloadTask downloadTask = Client.getInstance().getDownloadTasks().getById(notificationId);

        final Context context = m_Context;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        switch (downloadTask.getState()) {
            case DownloadTask.STATE_ERROR:
            case DownloadTask.STATE_CANCELED: {
                Intent intent = new Intent(context, QuickStartActivity.class);
                intent.putExtra("template", DownloadsTab.TEMPLATE);

                Notification notification = null;
                try {
                    notification = NotificationBridge.createBridge(
                            context,
                            R.drawable.icon,
                            context.getString(R.string.DownloadAborted),
                            System.currentTimeMillis())
                            .setContentTitle(downloadTask.getFileName())
                            .setContentText(DownloadTask.getStateMessage(downloadTask.getState(), downloadTask.getEx()))
                            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                            .setAutoCancel(true)
                            .createNotification();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                mNotificationManager.notify(downloadTask.getUrl(), notificationId, notification);
                return;
            }
            case DownloadTask.STATE_SUCCESSFULL: {
                Intent intent = getRunFileIntent(downloadTask.getOutputFile());

                Notification notification = null;
                try {
                    notification = NotificationBridge.createBridge(
                            context,
                            R.drawable.icon,
                            context.getString(R.string.DownloadComplete),
                            System.currentTimeMillis())
                            .setContentTitle(downloadTask.getFileName())
                            .setContentText(DownloadTask.getStateMessage(downloadTask.getState(), downloadTask.getEx()))
                            .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                            .setAutoCancel(true)
                            .createNotification();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                mNotificationManager.notify(downloadTask.getUrl(), notificationId, notification);

                m_Handler.post(new Runnable() {
                    public void run() {
                        try {
                            Toast.makeText(context, downloadTask.getFileName() + "\n" + context.getString(R.string.DownloadComplete), Toast.LENGTH_SHORT).show();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                });
                return;
            }
            default: {

                try {
                    showProgressNotification(context, notificationId, downloadTask.getFileName(), downloadTask.getPercents(), downloadTask.getUrl());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void showProgressNotification(Context context, int notificationId, String title, int percents, String tag) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(context, QuickStartActivity.class);
        intent.putExtra("template", DownloadsTab.TEMPLATE);
        String contentText = percents + "%";
        NotificationBridge notificationBridge = NotificationBridge.createBridge(
                context,
                R.drawable.icon,
                context.getString(R.string.DownloadFile),
                System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(contentText)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                .setAutoCancel(true);


        Notification notification = null;
        if (Build.VERSION.SDK_INT < 14) {
            final RemoteViews notification_view = new RemoteViews(context.getPackageName(), R.layout.download_task_notification);

            notification_view.setTextViewText(R.id.txtFileName, title);
            notification_view.setTextViewText(R.id.txtContent, contentText);
            if (Build.VERSION.SDK_INT < 9 && DownloadsService.notification_text_color != null) {
                notification_view.setTextColor(R.id.txtFileName, DownloadsService.notification_text_color);
                notification_view.setFloat(R.id.txtFileName, "setTextSize", DownloadsService.notification_text_size);

                notification_view.setTextColor(R.id.txtContent, DownloadsService.notification_text_color);
                //notification_view.setFloat(R.id.txtContent, "setTextSize", DownloadsService.notification_text_size);
            }


            notification_view.setProgressBar(R.id.progress, 100, percents, false);
            notification = notificationBridge.createNotification();
            notification.contentView = notification_view;
        } else {
            notificationBridge.setProgress(100, percents, false);
            notification = notificationBridge.createNotification();
        }

        mNotificationManager.notify(tag, notificationId, notification);
    }


    private Intent getRunFileIntent(String filePath) {
        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        String mimeType = myMime.getMimeTypeFromExtension(FileUtils.fileExt(filePath).substring(1));
        newIntent.setDataAndType(Uri.parse("file://" + filePath), mimeType);
        newIntent.setFlags(newIntent.FLAG_ACTIVITY_NEW_TASK);
        return newIntent;
    }
}
