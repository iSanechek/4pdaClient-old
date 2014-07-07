package org.softeg.slartus.forpda.styles;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import org.softeg.slartus.forpda.IntentActivity;
import org.softeg.slartus.forpda.MyApp;
import org.softeg.slartus.forpda.R;
import org.softeg.slartus.forpda.classes.BrowserViewsFragmentActivity;
import org.softeg.slartus.forpda.classes.WebViewExternals;
import org.softeg.slartus.forpda.classes.common.Functions;
import org.softeg.slartus.forpda.classes.common.StringUtils;
import org.softeg.slartus.forpda.common.Log;
import org.softeg.slartus.forpda.prefs.HtmlPreferences;
import org.softeg.slartus.forpda.topicview.ThemeActivity;

/**
 * Created by slinkin on 17.06.13.
 */
public class StyleInfoActivity extends BrowserViewsFragmentActivity {
    private static final String STYLE_PATH_KEY = "StylePath";
    private WebView webView;
    private Handler mHandler = new Handler();

    @Override
    public String Prefix() {
        return "theme";
    }

    @Override
    public WebView getWebView() {
        return webView;
    }

    @Override
    public ImageButton getFullScreenButton() {
        return null;
    }

    @Override
    public void nextPage() {

    }

    @Override
    public void prevPage() {

    }

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        setContentView(R.layout.activity_style_info);
        webView = (WebView) findViewById(R.id.wvBody);
        registerForContextMenu(webView);
        setWebViewSettings();

        webView.addJavascriptInterface(this, "HTMLOUT");
        webView.setWebViewClient(new MyWebViewClient());
    }

    public static void showStyleInfo(Context activity, String stylePath) {
        Intent intent = new Intent(activity, StyleInfoActivity.class);
        intent.putExtra(STYLE_PATH_KEY, stylePath);

        activity.startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            String stylePath = intent.getStringExtra(STYLE_PATH_KEY);

            showStyle(stylePath);
        }
    }

    private void showStyle(String stylePath) {
        stylePath = MyApp.getInstance().getThemeCssFileName(stylePath);
        String xmlPath = stylePath.replace(".css", ".xml");
        CssStyle cssStyle = CssStyle.parseStyle(this, xmlPath);
        if (!cssStyle.ExistsInfo) {
            Toast.makeText(this, "Стиль не содержит описания", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        StringBuilder sb = new StringBuilder();

        sb.append("<html xml:lang=\"en\" lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        sb.append("<head>\n");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n");
        sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"file://" + stylePath + "\" />\n");
        sb.append("<script type=\"text/javascript\" src=\"file:///android_asset/theme.js\"></script>\n");
        sb.append("<script type=\"text/javascript\" src=\"file:///android_asset/blockeditor.js\"></script>\n");

        sb.append("</head>\n");
        sb.append("<body><div class=\"post_body\">\n");

        addValue(sb, "Название", cssStyle.Title);
        addValue(sb, "Версия", cssStyle.Version);
        addValue(sb, "Автор", cssStyle.Author);
        addValue(sb, "Комментарий", cssStyle.Comment);

        if (cssStyle.ScreenShots.size() > 0) {
            sb.append("<b>Скриншоты:</b><br/>\n");
            for (CssStyleScreenShot cssStyleScreenShot : cssStyle.ScreenShots) {
                sb.append("<a attach_id=\"0\" s=0 id=\"id\" href=\"" + cssStyleScreenShot.FullView
                        + "\" rel=\"lytebox[0]\" title=\"Скриншот\" target=\"_blank\"><img src=\""
                        + cssStyleScreenShot.Preview + "\"/></a>&nbsp;");
            }
            sb.append("<br/>\n");
        }
        sb.append("</div></body>\n");
        sb.append("</html>");

        String res = sb.toString();
        HtmlPreferences htmlPreferences = new HtmlPreferences();
        htmlPreferences.load(this);
        if (htmlPreferences.isSpoilerByButton()) {

            res = HtmlPreferences.modifySpoiler(res);
        }
        if (htmlPreferences.isUseLocalEmoticons()) {
            res = HtmlPreferences.modifyStyleImagesBody(res);
        }

        if (!WebViewExternals.isLoadImages("theme"))
            res = HtmlPreferences.modifyAttachedImagesBody(Functions.isWebviewAllowJavascriptInterface(this), res);

        webView.loadDataWithBaseURL("http://4pda.ru/forum/", res, "text/html", "UTF-8", null);
    }

    private void addValue(StringBuilder sb, String title, String value) {
        if (!TextUtils.isEmpty(value))
            sb.append(String.format("<b>%s:</b>&nbsp;%s<br/>\n", title, value));
    }

    @JavascriptInterface
    public void showImgPreview(final String title, final String previewUrl, final String fullUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ThemeActivity.showImgPreview(StyleInfoActivity.this, title, previewUrl, fullUrl);
            }
        });
    }

    @JavascriptInterface
    public void copyToClipboard(final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringUtils.copyToClipboard(StyleInfoActivity.this, value);
                    Toast.makeText(StyleInfoActivity.this, String.format("Текст '%s' скопирован в буфер обмена", value), Toast.LENGTH_SHORT).show();
                } catch (Throwable ex) {
                    Log.e(StyleInfoActivity.this, ex);
                }
            }
        });
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            IntentActivity.tryShowUrl(StyleInfoActivity.this, mHandler, url, true, false);

            return true;
        }
    }
}
