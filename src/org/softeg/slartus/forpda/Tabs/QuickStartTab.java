package org.softeg.slartus.forpda.Tabs;

import android.content.Context;
import android.content.pm.PackageInfo;

import org.softeg.slartus.forpdaapi.OnProgressChangedListener;

import java.io.IOException;
import java.util.List;

/**
 * User: slinkin
 * Date: 29.11.11
 * Time: 16:01
 */
public class QuickStartTab extends ThemesTab {
    List<PackageInfo> m_Applications;

    public static final String TEMPLATE = Tabs.TAB_QUICK_START;
    public static final String TITLE = "Быстрый доступ";
    private String template;

    public QuickStartTab(Context context, String tabTag, ITabParent tabParent, String template) {
        super(context, tabTag, tabParent);
        this.template = template;
    }

    @Override
    public void refresh() {
        super.refresh();
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }


    @Override
    public void getThemes(OnProgressChangedListener progressChangedListener) throws IOException {

    }

}
