package org.softeg.slartus.forpda.download;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.cookie.Cookie;
import org.softeg.slartus.forpda.Client;
import org.softeg.slartus.forpda.HttpHelper;
import org.softeg.slartus.forpda.classes.AlertDialogBuilder;
import org.softeg.slartus.forpda.classes.AppProgressDialog;
import org.softeg.slartus.forpda.classes.DownloadTask;
import org.softeg.slartus.forpda.common.Log;
import org.softeg.slartus.forpda.db.DownloadsTable;
import org.softeg.slartus.forpdacommon.ExtPreferences;
import org.softeg.slartus.forpdacommon.FileUtils;
import org.softeg.slartus.forpdacommon.NotReportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User: slinkin
 * Date: 16.07.12
 * Time: 9:58
 */
public class DownloadsService extends IntentService {

    public static Integer notification_text_color = null;
    public static float notification_text_size = -1;
    private final String COLOR_SEARCH_RECURSE_TIP = "SOME_SAMPLE_TEXT";


    private boolean recurseGroup(ViewGroup gp) {
        final int count = gp.getChildCount();
        for (int i = 0; i < count; ++i) {
            if (gp.getChildAt(i) instanceof TextView) {
                final TextView text = (TextView) gp.getChildAt(i);
                final String szText = text.getText().toString();
                if (COLOR_SEARCH_RECURSE_TIP.equals(szText)) {
                    notification_text_color = text.getTextColors().getDefaultColor();
                    notification_text_size = text.getTextSize();
                    DisplayMetrics metrics = new DisplayMetrics();
                    WindowManager systemWM = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    systemWM.getDefaultDisplay().getMetrics(metrics);
                    notification_text_size /= metrics.scaledDensity;
                    return true;
                }
            } else if (gp.getChildAt(i) instanceof ViewGroup)
                return recurseGroup((ViewGroup) gp.getChildAt(i));
        }
        return false;
    }

    private void extractColors() {
        if (notification_text_color != null)
            return;

        try {
            Notification ntf = new Notification();
            //noinspection deprecation
            ntf.setLatestEventInfo(this, COLOR_SEARCH_RECURSE_TIP, "Utest", null);
            LinearLayout group = new LinearLayout(this);
            ViewGroup event = (ViewGroup) ntf.contentView.apply(this, group);
            recurseGroup(event);
            group.removeAllViews();
        } catch (Exception e) {
            // notification_text_color = android.R.color.black;
        }
    }

    public static final String DOWNLOAD_FILE_ID_KEY = "DownloadFileIdKey";

    public static final String DOWNLOAD_FILE_TEMP_NAME_KEY = "DownloadFileTempNameKey";

    public static final int UPDATE_PROGRESS = 8344;

    public DownloadsService() {
        super("DownloadsService");

    }

    public static String getDownloadDir(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("downloads.path", getDefaultDownloadPath());
    }

    public static String getDefaultDownloadPath() {
        return Environment.getExternalStorageDirectory() + "/download/4pda/".replace("/", File.separator);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int notificationId = intent.getExtras().getInt(DOWNLOAD_FILE_ID_KEY);
        ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra("receiver");
        String tempFilePath = intent.getStringExtra(DOWNLOAD_FILE_TEMP_NAME_KEY);
        downloadFile(receiver, notificationId, tempFilePath);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT < 9)
            extractColors();
    }

    public static void download(final Context context1, final String url) {
        download(context1, url, null, -1);
    }

    public static void download(final Context context1, final String url, final String tempFilePath, final int notificationId) {
        try {
            int downloadManager = ExtPreferences
                    .parseInt(PreferenceManager.getDefaultSharedPreferences(context1.getApplicationContext()),
                            "file.downloaderManagers",
                            0);
            switch (downloadManager) {
                case 0:// клиент
                    clientDownload(context1, url, tempFilePath, notificationId);
                    break;
                case 1: // системный
                    systemDownload(context1, url);
                    break;
                case 2:
                    new GetTempUrlTask(context1).execute(url);
                    break;
            }

        } catch (Throwable ex) {
            Log.e(context1, ex);
        }

    }


    private static void systemDownload(Context context1, String url) throws IOException {
        if (Build.VERSION.SDK_INT < 9) {
            new AlertDialogBuilder(context1)
                    .setTitle("Ошибка")
                    .setMessage("Скачивание при помощи Системного менеджера возможно только для устройств с android не ниже версии 2.3!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create()
                    .show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 9) {
            DownloadManager dm = (DownloadManager) context1.getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            List<Cookie> cookies = Client.getInstance().getCookies();
            StringBuilder sb = new StringBuilder();
            for (Cookie cookie : cookies) {
                sb.append(cookie.getName() + "=" + cookie.getValue() + ";");

            }
            request.addRequestHeader("Cookie", sb.toString());

            dm.enqueue(request);

            Intent downloadIntent = new Intent();
            downloadIntent.setAction(DownloadManager.ACTION_VIEW_DOWNLOADS);

            context1.startActivity(downloadIntent);
        }

    }

    private static void clientDownload(final Context context1, final String url, String tempFilePath, final int notificationId) throws UnsupportedEncodingException {
        final String fileName = FileUtils.getFileNameFromUrl(url);

        if (TextUtils.isEmpty(tempFilePath)) {
            final String filePath = FileUtils.combine(DownloadsService.getDownloadDir(context1), FileUtils.getFileNameFromUrl(url) + "_download");
            final File file = new File(filePath);
            if (file.exists()) {
                new AlertDialogBuilder(context1)
                        .setTitle("Внимание!")
                        .setMessage("Имеется недокачанный файл с таким же названием.\nДокачать?")
                        .setPositiveButton("Докачать", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                startDownload(context1, url, filePath, notificationId, fileName);
                            }
                        })
                        .setNegativeButton("Перекачать", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                file.delete();
                                startDownload(context1, url, null, notificationId, fileName);
                            }
                        }).create().show();
                return;
            }
        }

        startDownload(context1, url, tempFilePath, notificationId, fileName);
    }

    private static void startDownload(Context context1, String url, String tempFilePath, int notificationId, String fileName) {
        try {
            Toast.makeText(context1, "Загрузка начата", Toast.LENGTH_SHORT).show();
            if (notificationId == -1)
                notificationId = DownloadsTable.getNextId();

            DownloadReceiver.showProgressNotification(context1, notificationId, fileName, 0, url);

            Client.getInstance().downloadFile(context1, url, notificationId, tempFilePath);
        } catch (Exception ex) {
            Log.e(context1, ex);
        }


    }


    public void downloadFile(ResultReceiver receiver, int notificationId, String tempFilePath) {

        DownloadTask downloadTask = null;
        try {

            String dirPath = getDownloadDir(getApplicationContext());
            downloadTask = Client.getInstance().getDownloadTasks().getById(notificationId);

            if (downloadTask.getState() == DownloadTask.STATE_CANCELED) {
                return;
            }

            String url = downloadTask.getUrl();

            url = FileUtils.getDirPath(url) + "/" + URLEncoder.encode(FileUtils.getFileNameFromUrl(url));
            HttpHelper httpHelper = new HttpHelper();

            try {

                String fileName = TextUtils.isEmpty(tempFilePath) ? FileUtils.getFileNameFromUrl(url) : FileUtils.getFileNameFromUrl(tempFilePath.replace("_download", ""));
                String saveDir = dirPath;

                String filePath = TextUtils.isEmpty(tempFilePath) ? FileUtils.getUniqueFilePath(saveDir, fileName) : FileUtils.combine(saveDir, fileName);
                downloadTask.setOutputFile(filePath);
                String downloadingFilePath = filePath + "_download";
                downloadTask.setDownloadingFilePath(downloadingFilePath);


                FileUtils.mkDirs(downloadingFilePath);
                // new File(downloadingFilePath).createNewFile();

                long total = TextUtils.isEmpty(tempFilePath) ? 0 : DownloadTask.getRange(tempFilePath);


                url = FileUtils.getDirPath(url) + "/" + URLEncoder.encode(FileUtils.getFileNameFromUrl(url));
                HttpEntity entity = httpHelper.getDownloadResponse(url, total);

                long fileLength = entity.getContentLength() + total;
                downloadTask.updateInfo(fileLength);
                downloadTask.setProgressState(total, fileLength);
                sendDownloadProgressState(receiver, notificationId);


                int count;
                int percent = 0;
                int prevPercent = 0;

                Date lastUpdateTime = new Date();
                Boolean first = true;

                InputStream in = entity.getContent();
                FileOutputStream output = new FileOutputStream(downloadingFilePath, true);

                byte data[] = new byte[1024];
                try {
                    while ((count = in.read(data)) != -1) {
                        if (downloadTask.getState() == DownloadTask.STATE_CANCELED) {
                            sendDownloadProgressState(receiver, notificationId);
                            return;
                        }

                        output.write(data, 0, count);
                        total += count;

                        percent = (int) ((float) total / fileLength * 100);

                        long diffInMs = new Date().getTime() - lastUpdateTime.getTime();
                        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);

                        if ((percent != prevPercent && diffInSec > 1) || first) {
                            lastUpdateTime = new Date();
                            downloadTask.setProgressState(total, fileLength);
                            sendDownloadProgressState(receiver, notificationId);
                            first = false;
                        }
                        prevPercent = percent;
                    }
                    downloadTask.setProgressState(fileLength, fileLength);
                    sendDownloadProgressState(receiver, notificationId);
                } finally {
                    output.flush();
                    output.close();
                    in.close();
                }
                File downloadingFile = new File(downloadingFilePath);
                File downloadedFile = new File(filePath);
                if (!downloadingFile.renameTo(downloadedFile)) {
                    throw new NotReportException("Не могу переименовать файл: " + downloadingFilePath + " в " + filePath);
                }
                downloadTask.setState(downloadTask.STATE_SUCCESSFULL);
                sendDownloadProgressState(receiver, notificationId);
                DownloadsTable.endRow(downloadTask);
            } finally {
                httpHelper.close();
            }
        } catch (Exception ex) {
            if (downloadTask != null) {
                downloadTask.setEx(ex);
                downloadTask.setState(downloadTask.STATE_ERROR);
                DownloadsTable.endRow(downloadTask);
                sendDownloadProgressState(receiver, notificationId);
            }

            Log.e(null, ex);
        }


    }

    public static void sendDownloadProgressState(ResultReceiver receiver, int downloadTaskId) {
        Bundle resultData = new Bundle();
        resultData.putInt("downloadTaskId", downloadTaskId);
        receiver.send(UPDATE_PROGRESS, resultData);
    }

    private static class GetTempUrlTask extends AsyncTask<String, Void, Uri> {


        private final ProgressDialog dialog;
        public String m_ChatBody;

        private Context m_Context;

        public GetTempUrlTask(Context context) {
            m_Context = context;
            dialog = new AppProgressDialog(context);
        }

        @Override
        protected Uri doInBackground(String... params) {
            HttpHelper httpHelper = null;

            try {
                httpHelper = new HttpHelper();
                String url = params[0];

                httpHelper.getDownloadResponse(url, 0);
                URI redirectUri = HttpHelper.getRedirectUri();
                Uri uri = Uri.parse(url);
                if (redirectUri != null)
                    uri = Uri.parse(redirectUri.toString());


                return uri;
            } catch (Throwable e) {
                ex = e;
                return null;
            } finally {
                if (httpHelper != null)
                    httpHelper.close();
            }
        }

        // can use UI thread here
        protected void onPreExecute() {
            this.dialog.setMessage("Запрос ссылки...");
            this.dialog.show();
        }

        private Throwable ex;

        // can use UI thread here
        protected void onPostExecute(final Uri uri) {
            if (this.dialog.isShowing()) {
                this.dialog.dismiss();
            }

            if (uri != null) {
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, uri);
                m_Context.startActivity(marketIntent);
            } else {
                if (ex != null)
                    Log.e(m_Context, ex);
                else
                    Toast.makeText(m_Context, "Неизвестная ошибка",
                            Toast.LENGTH_SHORT).show();
            }
        }
    }

}
