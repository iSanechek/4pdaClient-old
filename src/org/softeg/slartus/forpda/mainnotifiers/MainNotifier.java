package org.softeg.slartus.forpda.mainnotifiers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.softeg.slartus.forpda.MyApp;
import org.softeg.slartus.forpda.common.Log;
import org.softeg.slartus.forpdacommon.DateExtensions;
import org.softeg.slartus.forpdacommon.ExtPreferences;

import java.util.Date;
import java.util.GregorianCalendar;

/*
 * Created by slartus on 03.06.2014.
 */
public abstract class MainNotifier {
    protected String name;
    protected int period;

    protected static String getAppVersion(Context context) {
        try {
            String packageName = context.getPackageName();
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_META_DATA);

            return pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e1) {
            Log.e(context, e1);
        }
        return "";
    }

    public MainNotifier(String name, int period) {
        this.name = name;
        this.period = period;

    }

    protected boolean isTime() {
        GregorianCalendar lastShowpromoCalendar = new GregorianCalendar();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.getContext());
        Date lastCheckDate=ExtPreferences.getDateTime(prefs, "notifier." + name, null);
        if(lastCheckDate==null){
            saveTime();
            return true;
        }

        lastShowpromoCalendar.setTime(lastCheckDate);

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        int days = DateExtensions.getDaysBetween(calendar.getTime(), lastShowpromoCalendar.getTime());
        return days >= period;
    }

    protected void saveTime() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.getContext());
        SharedPreferences.Editor editor = prefs.edit();
        ExtPreferences.putDateTime(editor, "notifier." + name, new Date());
        editor.commit();
    }
}
