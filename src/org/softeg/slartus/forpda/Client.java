package org.softeg.slartus.forpda;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.softeg.slartus.forpda.classes.AlertDialogBuilder;
import org.softeg.slartus.forpda.classes.DownloadTask;
import org.softeg.slartus.forpda.classes.DownloadTasks;
import org.softeg.slartus.forpda.classes.ExtTopic;
import org.softeg.slartus.forpda.classes.Forum;
import org.softeg.slartus.forpda.classes.TopicBodyBuilder;
import org.softeg.slartus.forpda.classes.WebViewExternals;
import org.softeg.slartus.forpda.classes.common.Functions;
import org.softeg.slartus.forpda.common.Log;
import org.softeg.slartus.forpda.db.ForumsTable;
import org.softeg.slartus.forpda.download.DownloadReceiver;
import org.softeg.slartus.forpda.download.DownloadsService;
import org.softeg.slartus.forpda.prefs.HtmlPreferences;
import org.softeg.slartus.forpdaapi.IHttpClient;
import org.softeg.slartus.forpdaapi.LoginResult;
import org.softeg.slartus.forpdaapi.NewsApi;
import org.softeg.slartus.forpdaapi.OnProgressChangedListener;
import org.softeg.slartus.forpdaapi.ProfileApi;
import org.softeg.slartus.forpdaapi.ProgressState;
import org.softeg.slartus.forpdaapi.Reputations;
import org.softeg.slartus.forpdaapi.TopicApi;
import org.softeg.slartus.forpdaapi.TopicReadingUsers;
import org.softeg.slartus.forpdaapi.UserProfile;
import org.softeg.slartus.forpdaapi.Users;
import org.softeg.slartus.forpdacommon.NotReportException;
import org.softeg.slartus.forpdacommon.PatternExtensions;
import org.softeg.slartus.forpdacommon.SimpleCookie;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Admin
 * Date: 16.09.11
 * Time: 18:40
 * To change this template use File | Settings | File Templates.
 */
public class Client implements IHttpClient {

    public static final String SITE = "4pda.ru";

    public String UserId = "0";
    private String m_User = MyApp.getContext().getString(R.string.guest);

    private String m_K = "";

    private Client() {

    }

    public String getAuthKey() {
        return m_K;
    }


    public static final Client INSTANCE = new Client();


    public URI getRedirectUri() {
        return HttpHelper.getRedirectUri();
    }


    public void deletePost(String forumId, String themeId, String postId, CharSequence authKey) throws IOException {
        org.softeg.slartus.forpdaapi.Post.delete(this, forumId, themeId, postId, authKey);
    }

    public String getEditPostPlus(String forumId, String themeId, String postId, String authKey,
                                  Map<String, String> outParams) throws Throwable {
        if (TextUtils.isEmpty(forumId) || TextUtils.isEmpty(authKey)) {
            ExtTopic topic = new ExtTopic(themeId, "");
            setThemeForumAndAuthKey(topic);

            forumId = topic.getForumId();
            authKey = topic.getAuthKey();

            outParams.put("forumId", forumId);
            outParams.put("authKey", authKey);
        }

        String res = org.softeg.slartus.forpdaapi.Post.getEditPage(this, forumId, themeId, postId, authKey);

        String error = org.softeg.slartus.forpdaapi.Post.checkEditPage(res);

        if (!TextUtils.isEmpty(error))
            throw new NotReportException(error);

        return res;

    }

    public void editPost(String forumId, String themeId, String authKey, String postId, Boolean enablesig,
                         Boolean enableEmo, String post, String addedFileList, String post_edit_reason) throws IOException {
        org.softeg.slartus.forpdaapi.Post.applyEdit(this, forumId, themeId, authKey, postId, enablesig,
                enableEmo, post, addedFileList, post_edit_reason);
    }

    public String attachFilePost(String forumId, String themeId, String authKey, String attachPostKey, String postId, Boolean enablesig, Boolean enableEmo,
                                 String post, String filePath, String addedFileList, ProgressState progress
            , String post_edit_reason) throws Exception {
        return org.softeg.slartus.forpdaapi.Post.attachFile(this, forumId, themeId, authKey, attachPostKey, postId, enablesig, enableEmo,
                post, filePath, addedFileList, progress, post_edit_reason);
    }

    public String deleteAttachFilePost(String forumId, String themeId, String authKey, String postId,
                                       Boolean enablesig, Boolean enableemo,
                                       String post, String attachToDeleteId, String fileList
            , String post_edit_reason) throws Exception {
        return org.softeg.slartus.forpdaapi.Post.deleteAttachedFile(this, forumId, themeId, authKey, postId,
                enablesig, enableemo,
                post, attachToDeleteId, fileList, post_edit_reason);
    }

    public Boolean changeReputation(String postId, String userId, String type, String message, Map<String, String> outParams) throws IOException {

        return org.softeg.slartus.forpdaapi.User.changeReputation(this, postId, userId, type, message, outParams);

    }

    public String claim(String themeId, String postId, String message) throws IOException {
        String error = org.softeg.slartus.forpdaapi.Post.claim(this, themeId, postId, message);
        if (!TextUtils.isEmpty(error))
            return error;
        return "Жалоба отправлена";
    }

    public String themeSubscribe(ExtTopic topic, String emailtype) throws IOException, URISyntaxException, ParseException {
        return themeSubscribe(topic.getId(), emailtype);

    }

    public String themeSubscribe(String topicId, String emailtype) throws IOException, URISyntaxException, ParseException {
        String error = TopicApi.changeFavorite(this, topicId, emailtype);
        if (!TextUtils.isEmpty(error))
            return error;
        return "Подписка оформлена";
    }

    public void clearCookies() throws IOException {
        HttpHelper httpHelper = new HttpHelper();
        try {
            httpHelper.clearCookies();
        } finally {
            httpHelper.close();
        }
    }

    public Boolean hasLoginCookies() {
        Boolean session = false;
        Boolean pass_hash = false;
        Boolean member = false;
        HttpHelper httpHelper = null;
        try {
            try {
                httpHelper = new HttpHelper();

            } catch (IOException e) {
                Log.e(null, e);
                return false;
            }
            List<Cookie> cookies = httpHelper.getCookies();
            for (Cookie cookie : cookies) {
                if (!session && cookie.getName().equals("session_id"))
                    session = true;
                else if (!pass_hash && cookie.getName().equals("pass_hash"))
                    pass_hash = true;
                else if (!member && cookie.getName().equals("member_id"))
                    member = true;
            }
        } finally {
            if (httpHelper != null)
                httpHelper.close();
        }
        return session && pass_hash && member;
    }


    public String performGetWithCheckLogin(String url, OnProgressChangedListener beforeGetPage, OnProgressChangedListener afterGetPage) throws IOException {
        if (beforeGetPage != null)
            beforeGetPage.onProgressChanged("Получение данных...");
        String body = performGet(url);
        if (beforeGetPage != null)
            afterGetPage.onProgressChanged("Получение данных...");

        Matcher headerMatcher = PatternExtensions.compile("<body>([\\s\\S]*?)globalmess").matcher(body);
        if (headerMatcher.find()) {
            checkLogin(headerMatcher.group(1));
            checkMails(headerMatcher.group(1));
        } else {
            checkLogin(body);
            checkMails(body);
        }
        return body;
    }

    /**
     * Загрузка тестовой страницы, проверка логина, проверка писем
     */
    public void loadTestPage() throws IOException {

        String body = performGet("http://4pda.ru/forum/index.php?showforum=200");
        checkLogin(body);
        checkMails(body);
    }

    public String performGetFullVersion(String s) throws IOException {

        HttpHelper httpHelper = new HttpHelper(HttpHelper.FULL_USER_AGENT);
        //HttpHelper httpHelper = new HttpHelper();
        String res = null;
        try {
            // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
            res = httpHelper.performGet(s);
        } finally {
            httpHelper.close();

        }
        if (TextUtils.isEmpty(res))
            throw new NotReportException("Сервер вернул пустую страницу");
        // m_HttpHelper.close();
        return res;
    }

    public String performGet(String s) throws IOException {
        return performGet(s, true);
    }

    public String performGet(String s, Boolean checkEmptyResult) throws IOException {

        HttpHelper httpHelper = new HttpHelper();
        String res = null;
        try {
            // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
            res = httpHelper.performGet(s);
        } finally {
            httpHelper.close();

        }
        if (checkEmptyResult && TextUtils.isEmpty(res))
            throw new NotReportException("Сервер вернул пустую страницу");
        // m_HttpHelper.close();
        return res;
    }

    public String performPost(String s, Map<String, String> additionalHeaders) throws IOException {
        HttpHelper httpHelper = new HttpHelper();
        String res = null;
        try {
            // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
            res = httpHelper.performPost(s, additionalHeaders);
            //  m_HttpHelper.close();
        } finally {
            httpHelper.close();
        }
        return res;
    }


    public String uploadFile(String url, String filePath, Map<String, String> additionalHeaders
            , ProgressState progress) throws Exception {
        HttpHelper httpHelper = new HttpHelper();
        String res = null;
        try {
            // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
            res = httpHelper.uploadFile(url, filePath, additionalHeaders, progress);
            //  m_HttpHelper.close();
        } finally {
            httpHelper.close();
        }
        return res;
    }

    @Override
    public CookieStore getCookieStore() throws IOException {
        HttpHelper httpHelper = new HttpHelper();

        try {
            return httpHelper.getCookieStore();
        } finally {
            httpHelper.close();
        }
    }

    public String performPost(String s, Map<String, String> additionalHeaders, String encoding) throws IOException {
        HttpHelper httpHelper = new HttpHelper();
        String res = null;
        try {
            // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
            res = httpHelper.performPost(s, additionalHeaders, encoding);
            //  m_HttpHelper.close();
        } finally {
            httpHelper.close();
        }
        return res;
    }

    public UserProfile loadUserProfile(UserProfile userProfile, String userId) throws Throwable {
        return UserProfile.loadProfile(this, userProfile, userId);
    }

    public UserProfile loadUserProfileFriends(UserProfile userProfile, String userId) throws Throwable {
        return UserProfile.loadProfileFriends(this, userProfile, userId, m_K);
    }

    public UserProfile loadUserProfileComments(UserProfile userProfile, String userId) throws Throwable {
        return UserProfile.loadUserProfileComments(this, userProfile, userId, m_K);
    }

    public List<Cookie> getCookies() throws IOException {
        HttpHelper httpHelper = new HttpHelper();
        try {
            return httpHelper.getCookies();
        } finally {
            httpHelper.close();
        }

    }

    public Users getTopicWritersUsers(String topicId) throws IOException {

        return org.softeg.slartus.forpdaapi.Topic.getWriters(this, topicId);

    }

    public static Client getInstance() {
        return INSTANCE;  //To change body of created methods use File | Settings | File Templates.
    }

    public Boolean likeNews(String postId) throws IOException {
        return NewsApi.like(this, postId);
    }

    public interface OnUserChangedListener {
        void onUserChanged(String user, Boolean success);
    }

    public void doOnUserChangedListener(String user, Boolean success) {
        for (OnUserChangedListener listener : m_OnUserChangeListeners) {
            listener.onUserChanged(user, success);
        }

    }

    private List<OnUserChangedListener> m_OnUserChangeListeners = new ArrayList<>();

    public void addOnUserChangedListener(OnUserChangedListener p) {
        m_OnUserChangeListeners.add(p);
    }

    public interface OnMailListener {
        void onMail(int count);
    }

    public void doOnMailListener() {
        for (OnMailListener listener : m_OnMailListeners) {
            listener.onMail(0);
        }

    }

    private List<OnMailListener> m_OnMailListeners = new ArrayList<>();

    public void addOnMailListener(OnMailListener p) {
        m_OnMailListeners.add(p);
    }


    public interface OnProgressPositionChangedListener {
        void onProgressChanged(Context context, DownloadTask downloadTask, Exception ex);
    }

    public void doOnOnProgressChanged(OnProgressChangedListener listener, String state) {
        if (listener != null) {
            listener.onProgressChanged(state);
        }
    }

    public void showLoginForm(Context mContext, OnUserChangedListener onUserChangedListener) {
        try {
            // if (m_LoginDialog == null)
            AlertDialog m_LoginDialog;
            {
                final Context context = mContext;
                final OnUserChangedListener monUserChangedListener = onUserChangedListener;
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.login_activity, null);

                final EditText username_edit = (EditText) layout.findViewById(R.id.username_edit);
                final EditText password_edit = (EditText) layout.findViewById(R.id.password_edit);
                final CheckBox privacy_checkbox = (CheckBox) layout.findViewById(R.id.privacy_checkbox);
                final CheckBox remember_checkbox = (CheckBox) layout.findViewById(R.id.remember_checkbox);
                final CheckBox autologin_checkbox = (CheckBox) layout.findViewById(R.id.autologin_checkbox);


                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                username_edit.setText(preferences.getString("Login", ""));
                privacy_checkbox.setChecked(preferences.getBoolean("LoginPrivacy", false));
                remember_checkbox.setChecked(preferences.getBoolean("LoginRemember", true));
                autologin_checkbox.setChecked(preferences.getBoolean("AutoLogin", true));


                m_LoginDialog = new AlertDialogBuilder(context)
                        .setTitle("Вход")
                        .setView(layout)
                        .setPositiveButton("Вход", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                LoginTask loginTask = new LoginTask(context);
                                loginTask.setOnUserChangedListener(monUserChangedListener);
                                loginTask.execute(username_edit.getText().toString(), password_edit.getText().toString(),
                                        Boolean.toString(privacy_checkbox.isChecked()),
                                        Boolean.toString(remember_checkbox.isChecked()), Boolean.toString(autologin_checkbox.isChecked())
                                );
                            }
                        })
                        .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (monUserChangedListener != null)
                                    monUserChangedListener.onUserChanged(m_User, false);
                            }
                        })
                        .create();
            }

            m_LoginDialog.show();
        } catch (Exception ex) {
            Log.e(mContext, ex);
        }
    }

    public String getUser() {
        return m_User;
    }

    private Boolean m_Logined = false;

    public Boolean getLogined() {
        return m_Logined;
    }

    private String m_LoginFailedReason;

    public String getLoginFailedReason() {
        return m_LoginFailedReason;
    }

    public String reply(String forumId, String themeId, String authKey, String post,
                        Boolean enablesig, Boolean enableemo, Boolean quick, String addedFileList) throws IOException {
        return reply(forumId, themeId, authKey, null, post,
                enablesig, enableemo, quick, addedFileList);
    }

    public String reply(String forumId, String themeId, String authKey, String attachPostKey, String post,
                        Boolean enablesig, Boolean enableemo, Boolean quick, String addedFileList) throws IOException {
        return org.softeg.slartus.forpdaapi.Post.reply(this, forumId, themeId, authKey, attachPostKey, post,
                enablesig, enableemo, addedFileList, quick);
    }


    public Boolean login(String login, String password, Boolean privacy) throws Exception {

        HttpHelper httpHelper = new HttpHelper();
        try {
            httpHelper.clearCookies();
            httpHelper.writeExternalCookies();


            final HttpHelper finalHttpHelper = httpHelper;
            LoginResult loginResult = ProfileApi.login(new IHttpClient() {


                public String performGetWithCheckLogin(String s, OnProgressChangedListener beforeGetPage, OnProgressChangedListener afterGetPage) throws IOException {
                    return null;
                }

                public String performGet(String s, Boolean b) throws IOException {
                    return null;
                }


                public String performGet(String s) throws IOException {
                    return null;
                }

                @Override
                public String performGetFullVersion(String s) throws IOException {
                    return null;
                }

                public String performPost(String s, Map<String, String> additionalHeaders) throws IOException {

                    String res = null;
                    try {
                        // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
                        res = finalHttpHelper.performPost(s, additionalHeaders);
                        checkLogin(res);
                        finalHttpHelper.writeExternalCookies();
                    } catch (Exception ignored) {

                    } finally {
                        finalHttpHelper.close();
                    }
                    return res;
                }

                public String performPost(String s, Map<String, String> additionalHeaders, String encoding) throws IOException {

                    String res = null;
                    try {
                        // s="http://4pda.ru/2009/12/28/18506/#comment-363525";
                        res = finalHttpHelper.performPost(s, additionalHeaders, encoding);
                        checkLogin(res);
                        finalHttpHelper.writeExternalCookies();
                    } catch (Exception ignored) {

                    } finally {
                        finalHttpHelper.close();
                    }
                    return res;
                }

                @Override
                public String uploadFile(String url, String filePath, Map<String, String> additionalHeaders, ProgressState progress) throws Exception {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public CookieStore getCookieStore() throws IOException {
                    try {
                        return finalHttpHelper.getCookieStore();
                    } finally {
                        finalHttpHelper.close();
                    }
                }

            }, login, password, privacy);
            m_Logined = loginResult.isSuccess();
            m_LoginFailedReason = m_Logined ? null : loginResult.getLoginError().toString();


            // m_SessionId = outParams.get("SessionId");
            m_User = loginResult.getUserLogin().toString();
            m_K = loginResult.getK().toString();

            httpHelper.getCookieStore().addCookie(new SimpleCookie("4pda.UserId", loginResult.getUserId().toString()));
            httpHelper.getCookieStore().addCookie(new SimpleCookie("4pda.User", m_User));
            httpHelper.getCookieStore().addCookie(new SimpleCookie("4pda.K", m_K));

            httpHelper.writeExternalCookies();

        } finally {
            httpHelper.close();
        }

        return m_Logined;
    }

    private final Pattern checkLoginPattern = PatternExtensions.compile("<a href=\"(http://4pda.ru)?/forum/index.php\\?showuser=(\\d+)\">(.*?)</a></b> \\( <a href=\"(http://4pda.ru)?/forum/index.php\\?act=Login&amp;CODE=03&amp;k=([a-z0-9]{32})\">Выход</a>");

    public void checkLoginByCookies() {
        try {
            HttpHelper httpHelper = null;
            try {
                httpHelper = new HttpHelper();

                if (checkLogin(httpHelper.getCookieStore())) {
                    m_Logined = true;
                }
            } finally {

                if (httpHelper != null)
                    httpHelper.finalize();
            }
        } catch (Throwable ignored) {

        } finally {
            doOnUserChangedListener(m_User, m_Logined);
        }

    }

    public Boolean checkLogin(CookieStore cookies) {
        if (cookies == null)
            return false;
        m_User = "";
        m_K = "";

        Cookie memberIdCookie = null;
        for (Cookie cookie : cookies.getCookies()) {
            if ("4pda.UserId".equals(cookie.getName())) {
                UserId = cookie.getValue();
            } else if ("4pda.User".equals(cookie.getName())) {
                m_User = cookie.getValue();
            } else if ("4pda.K".equals(cookie.getName())) {
                m_K = cookie.getValue();
            } else if ("member_id".equals(cookie.getName())) {
                memberIdCookie = cookie;
            }
        }

        return !TextUtils.isEmpty(m_User) && !TextUtils.isEmpty(UserId) && !TextUtils.isEmpty(m_K) && memberIdCookie != null && UserId.equals(memberIdCookie.getValue());

    }

    public void checkLogin(String pageBody) {

        try {
            HttpHelper httpHelper = null;
            try {
                httpHelper = new HttpHelper();

                if (checkLogin(httpHelper.getCookieStore())) {
                    m_Logined = true;
                    return;
                }
                if (!TextUtils.isEmpty(m_User) && !TextUtils.isEmpty(UserId)
                        && !TextUtils.isEmpty(m_K)) {

                    List<Cookie> cookies = httpHelper.getLastCookies();
                    for (Cookie cookie : cookies) {
                        if ("member_id".equals(cookie.getName())) {
                            if (UserId.equals(cookie.getValue())) {
                                m_Logined = true;
                                return;
                            }
                            break;
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (httpHelper != null)
                    httpHelper.close();
            }


            Matcher m = checkLoginPattern.matcher(pageBody);
            if (m.find()) {
                UserId = m.group(2);
                m_User = m.group(3);
                m_K = m.group(5);
                m_Logined = true;
            } else {
                m_Logined = false;
                m_User = "гость";
                m_K = "";
                UserId = "";
            }

        } finally {
            doOnUserChangedListener(m_User, m_Logined);
        }


    }

    private int m_Qms_2_0Count = 0;

    public int getQms_2_0_Count() {
        return m_Qms_2_0Count;
    }

    public void setQms_2_0_Count(int count) {
        m_Qms_2_0Count = count;
    }

    final Pattern qms_2_0_Pattern = PatternExtensions.compile("id=\"events-count\">Сообщений:\\s+(\\d+)</a>");

    public void checkMails(String pageBody) {
        m_Qms_2_0Count = 0;
        Matcher m = qms_2_0_Pattern.matcher(pageBody);
        if (m.find()) {
            m_Qms_2_0Count = Integer.parseInt(m.group(1));
        }


        doOnMailListener();
    }

    public Boolean logout() throws Throwable {
        String res = ProfileApi.logout(this, m_K);
        HttpHelper httpHelper = new HttpHelper();
        try {
            httpHelper.clearCookies();
            httpHelper.writeExternalCookies();
        } finally {
            httpHelper.close();
        }

        checkLogin(res);
        if (m_Logined)
            m_LoginFailedReason = "Неудачный выход";

        return !m_Logined;
    }

    public Forum loadForums() throws Exception {
        //   org.softeg.slartus.forpdaapi.Forum forum = org.softeg.slartus.forpdaapi.Forums.loadForums(this);

        return ForumsTable.loadForumsTree();


    }

    public String loadPageAndCheckLogin(String url, OnProgressChangedListener progressChangedListener) throws IOException {

        doOnOnProgressChanged(progressChangedListener, "Получение данных...");
        String body = performGet(url);
        doOnOnProgressChanged(progressChangedListener, "Обработка данных...");

        Matcher headerMatcher = PatternExtensions.compile("<body>([\\s\\S]*?)globalmess").matcher(body);
        if (headerMatcher.find()) {
            checkLogin(headerMatcher.group(1));
            checkMails(headerMatcher.group(1));
        } else {
            checkLogin(body);
            checkMails(body);
        }
        return body;
    }

    public void loadUserReputation(Reputations res, Boolean self, OnProgressChangedListener progressChangedListener) throws IOException {
        org.softeg.slartus.forpdaapi.User.loadReputation(this, res.userId, self, res, progressChangedListener, progressChangedListener);

    }


    public TopicBodyBuilder parseTopic(String topicPageBody,
                                       Context context, String themeUrl, Boolean spoilFirstPost) throws IOException {

        checkLogin(topicPageBody);

        Pattern pattern = PatternExtensions.compile("showtopic=(\\d+)(&(.*))?");
        Matcher m = pattern.matcher(themeUrl);
        String topicId = null;
        String urlParams = null;
        if (m.find()) {

            topicId = m.group(1);

            urlParams = m.group(3);
        }


        return loadTopic(context, topicId, topicPageBody, spoilFirstPost, m_Logined,
                urlParams);
    }


    public static ExtTopic createTopic(String id, String page) {

        final Pattern navStripPattern = PatternExtensions.compile("<div id=\"navstrip\">(.*?)</div>");
        final Pattern userPattern =
                PatternExtensions.compile("/forum/index.php\\?act=Login&amp;CODE=03&amp;k=([a-z0-9]{32})\">Выход</a>");
        final Pattern titlePattern = PatternExtensions.compile("<title>(.*?) - 4PDA</title>");
        final Pattern descriptionPattern = PatternExtensions.compile("<div class=\"topic_title_post\">(.*?)<");
        final Pattern moderatorTitlePattern = PatternExtensions.compile("onclick=\"return setpidchecks\\(this.checked\\);\".*?>&nbsp;(.*?)<");
        final Pattern pagesCountPattern = PatternExtensions.compile("var pages = parseInt\\((\\d+)\\);");
        final Pattern lastPageStartPattern = PatternExtensions.compile("(http://4pda.ru)?/forum/index.php\\?showtopic=\\d+&amp;st=(\\d+)");
        final Pattern currentPagePattern = PatternExtensions.compile("<span class=\"pagecurrent\">(\\d+)</span>");

        String title = "";
        Matcher m = titlePattern.matcher(page);
        if (m.find()) {
            title = m.group(1);
        }


        ExtTopic topic = new ExtTopic(id, title);

        m = descriptionPattern.matcher(page);
        if (m.find()) {
            topic.setDescription(m.group(1).replace(title + ", ", ""));
        } else {
            m = moderatorTitlePattern.matcher(page);
            if (m.find())
                topic.setDescription(m.group(1).replace(title + ", ", ""));
        }
        m = navStripPattern.matcher(page);
        if (m.find()) {
            final Pattern forumPatter = PatternExtensions.compile("<a href=\"(http://4pda.ru)?/forum/index.php\\?.*?showforum=(\\d+).*?\">(.*?)</a>");
            Matcher forumMatcher = forumPatter.matcher(m.group(1));
            while (forumMatcher.find()) {
                topic.setForumId(forumMatcher.group(2));
                topic.setForumTitle(forumMatcher.group(3));
            }
        }

        m = userPattern.matcher(page);
        if (m.find()) {
            topic.setAuthKey(m.group(1));
        }

        m = pagesCountPattern.matcher(page);
        if (m.find()) {
            topic.setPagesCount(m.group(1));
        }

        m = lastPageStartPattern.matcher(page);

        while (m.find()) {
            topic.setLastPageStartCount(m.group(2));
        }

        m = currentPagePattern.matcher(page);
        if (m.find()) {
            topic.setCurrentPage(m.group(1));
        } else
            topic.setCurrentPage("1");
        return topic;

    }

    private TopicBodyBuilder loadTopic(Context context,
                                       String id, String topicBody, Boolean spoilFirstPost,
                                       Boolean logined, String urlParams) throws IOException {


        Matcher mainMatcher = PatternExtensions.compile("^([\\s\\S]*?)<!--Begin Msg Number \\d+-->([\\s\\S]*?)<!-- TABLE([\\s\\S]*)").matcher(topicBody);

        if (!mainMatcher.find()) {
            final Pattern errorPattern = PatternExtensions.compile("<div class=\"errorwrap\">([\\s\\S]*?)</div>");
            Matcher errorMatcher = errorPattern.matcher(topicBody);
            if (errorMatcher.find()) {
                final Pattern errorReasonPattern = PatternExtensions.compile("<p>(.*?)</p>");
                Matcher errorReasonMatcher = errorReasonPattern.matcher(errorMatcher.group(1));
                if (errorReasonMatcher.find()) {
                    throw new NotReportException(errorReasonMatcher.group(1));
                }
            }


            if (TextUtils.isEmpty(topicBody))
                throw new NotReportException("Сервер вернул пустую страницу");
            if (topicBody.startsWith("<h1>"))
                throw new NotReportException("Ответ сайта 4pda: " + Html.fromHtml(topicBody).toString());
            throw new IOException("Ошибка разбора страницы id=" + id);
        }


        topicBody = null;
        Boolean isWebviewAllowJavascriptInterface = Functions.isWebviewAllowJavascriptInterface(context);

//        Boolean isFullVersion = mainMatcher.group(3).contains("<a href=\"/wp-content/plugins/ngx.php?mb=1\"><b>Мобильная версия</b></a>");
//        if (isFullVersion) {
//
//            return loadFullVersionTopic(context, id, mainMatcher, spoilFirstPost, logined, urlParams, enableSig,
//                    enableEmo, postBody, hidePostForm, isWebviewAllowJavascriptInterface);
//        }


        ExtTopic topic = createTopic(id, mainMatcher.group(1));

        String body = mainMatcher.group(2);


        mainMatcher = null;
        TopicBodyBuilder topicBodyBuilder = new TopicBodyBuilder(context, logined, topic, urlParams,
                isWebviewAllowJavascriptInterface);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean browserStyle = prefs.getBoolean("theme.BrowserStyle", false);
        topicBodyBuilder.beginTopic();

        if (browserStyle) {
            body = body
                    .replace("onclick=\"return confirm('Подтвердите удаление');\"", "")
                    .replace("href=\"#\"", "");
            if (!WebViewExternals.isLoadImages("theme"))
                body = HtmlPreferences.modifyAttachedImagesBody(Functions.isWebviewAllowJavascriptInterface(context), body);
            topicBodyBuilder.addBody(body);
            topicBodyBuilder.endTopic();
        } else {

            Matcher matcher = Pattern.compile("<a name=\"entry(\\d+)\"></a><div class=\"post_header_container\">([\\s\\S]*?)(?:<!--Begin Msg Number|<!-- TABLE)",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(body + "<!-- TABLE");


            final Pattern postHeaderPattern = PatternExtensions.compile("<span class=\"post_date\">(.*?)&nbsp;.*?#(\\d+).*");
            final Pattern nickPattern = PatternExtensions.compile("title=\"Вставить ник\" onclick=\"return insertText\\('[^']*\\[B\\](.*?),\\[/B\\] '\\)\"[^>]*>");
            Pattern userInfoPattern = PatternExtensions.compile("<span class=\"post_user_info\">(?:<strong>.*?</strong><br />)?Группа:(.*?)<font color=\"(.*?)\">[\\s\\S]*?mid=(\\d+)");

            final Pattern reputationPattern = PatternExtensions.compile("title=\"Просмотреть репутацию\">(.*?)</a>");
            final Pattern actionsPattern = PatternExtensions.compile(".*Жалоба.*");
            final Pattern bodyPattern = PatternExtensions.compile("<div class=\"post_body(?:\\s|\\w)*?\">([\\s\\S]*)</div>");

            body = null;
            String today = Functions.getToday();
            String yesterday = Functions.getYesterToday();
            org.softeg.slartus.forpda.classes.Post post = null;
            Boolean spoil = spoilFirstPost;
            Boolean first = true;
            while (matcher.find()) {
                if (post != null) {
                    topicBodyBuilder.addPost(post, spoil, first);
                    spoil = false;
                    first = false;
                }
                String postId = matcher.group(1);

                String str = matcher.group(2);
                Matcher m = postHeaderPattern.matcher(str);
                if (m.find()) {
                    post = new org.softeg.slartus.forpda.classes.Post(postId, Functions.getForumDateTime(Functions.parseForumDateTime(m.group(1), today, yesterday)), m.group(2));

                } else
                    continue;

                m = nickPattern.matcher(str);
                if (m.find()) {
                    String author = m.group(1);
                    if (author == null)
                        author = "unknown";
                    post.setAuthor(author);
                }

                m = userInfoPattern.matcher(str);
                if (m.find()) {
                    post.setUserGroup(Html.fromHtml(m.group(1)).toString().trim());
                    post.setUserState(m.group(2));
                    post.setUserId(m.group(3));
                }

                m = reputationPattern.matcher(str);
                if (m.find()) {
                    post.setUserReputation(m.group(1));
                    post.setCanPlusRep(str.contains("Поднять репутацию"));
                    post.setCanMinusRep(str.contains("Опустить репутацию"));
                }

                m = actionsPattern.matcher(str);
                if (m.find()) {
                    post.setCanEdit(str.contains("Ред."));
                    post.setCanDelete(str.contains("Удал."));
                }

                m = bodyPattern.matcher(str);
                if (m.find()) {
                    String postBody = m.group(1);
                    if (postBody != null)
                        postBody = postBody.trim();

                    post.setBody("<div class=\"post_body emoticons\">" + postBody + "</div>");
                }

                //topic.addMessage(post);
            }

            if (post != null) {
                topicBodyBuilder.addPost(post, spoil, first);
                spoil = false;
            }

            topicBodyBuilder.endTopic();

        }

        return topicBodyBuilder;
    }


//    private void parseTopicByJsoup(String body, TopicBodyBuilder topicBodyBuilder, Boolean spoilFirstPost) {
//        Document doc = Jsoup.parse(body);
//        Elements elements = doc.getElementsByAttributeValue("class", "post_header_container");
//
//        String today = Functions.getToday();
//        String yesterday = Functions.getYesterToday();
//        final Pattern dataPattern = PatternExtensions.compile("(.*?)&nbsp;.*?p=(\\d+).*?#(\\d+).*");
//        final Pattern userInfoPattern = PatternExtensions.compile("Группа:(.*?)<font color=\"(.*?)\">[\\s\\S]*?mid=(\\d+)[\\s\\S]*?act=rep.*?>(.*?)</a>");
//        Boolean spoil = spoilFirstPost;
//        Boolean first = true;
//        org.softeg.slartus.forpda.classes.Post post=null;
//        for (Element element : elements) {
//            if (post != null) {
//                spoil = false;
//                first = false;
//            }
//            Elements els = element.getElementsByAttributeValue("class", "post_header");
//            String value = els.get(0).html();
//            Matcher m = dataPattern.matcher(value);
//
//            if (!m.find())
//                continue;
//
//
//            post = new org.softeg.slartus.forpda.classes.Post(m.group(2), Functions.getForumDateTime(Functions.parseForumDateTime(m.group(1), today, yesterday)), m.group(3));
//
//            els = element.getElementsByAttributeValue("class", "post_nick");
//            post.setAuthor(els.get(0).child(0).html());
//
//            els = element.getElementsByAttributeValue("class", "post_user_info");
//
//            String str = els.get(0).html();
//            m = userInfoPattern.matcher(str);
//            if (m.find()) {
//                post.setUserGroup(Html.fromHtml(m.group(1)).toString().trim());
//                post.setUserState(m.group(2));
//                post.setUserId(m.group(3));
//                post.setUserReputation(m.group(4));
//            }
//
//            post.setCanPlusRep(str.contains("Поднять репутацию"));
//            post.setCanMinusRep(str.contains("Опустить репутацию"));
//
//            els = element.getElementsByAttributeValue("class", "post_action");
//            str = els.get(0).html();
//            post.setCanEdit(str.contains("Ред."));
//            post.setCanDelete(str.contains("Удал."));
//
//            while (true) {
//                Element bodyElement = element.nextElementSibling();
//                if ("post_body".equals(bodyElement.className())) {
//                    post.setBody("<div class=\"post_body\">" + bodyElement.html() + "</div>");
//                    break;
//                }
//            }
//            topicBodyBuilder.addPost(post, spoil, first);
//        }
//        topicBodyBuilder.endTopic();
//    }


    public void markAllForumAsRead() throws Throwable {
        org.softeg.slartus.forpdaapi.Forums.markAllAsRead(this);
    }

    public String getThemeForumId(CharSequence themeId) throws IOException {

        String res = performGet("http://4pda.ru/forum/lofiversion/index.php?t" + themeId + ".html");

        return parseForumId(res);

    }

    private String parseForumId(String pageBody) {
        Pattern pattern = Pattern.compile("<div class='ipbnav'>.*<a href='http://4pda.ru/forum/lofiversion/index.php\\?f(\\d+).html'>.*?</a></div>",
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(pageBody);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }

    private void setThemeForumAndAuthKey(ExtTopic topic) throws IOException {

        String res = performGet("http://4pda.ru/forum/lofiversion/index.php?t" + topic.getId() + ".html");

        if (TextUtils.isEmpty(topic.getForumId())) {
            String forumId = parseForumId(res);
            if (forumId != null) {
                topic.setForumId(forumId);
            }
        }

        if (TextUtils.isEmpty(topic.getAuthKey())) {
            Pattern pattern = Pattern.compile("name=\"auth_key\" value=\"(.*)\"",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(res);
            if (m.find()) {
                topic.setAuthKey(m.group(1));
            }
        }

    }


    public TopicReadingUsers getTopicReadingUsers(String topicId) throws IOException {
        return org.softeg.slartus.forpdaapi.Topic.getReadingUsers(this, topicId);
    }

    public String removeFromFavorites(ExtTopic topic) throws IOException, ParseException, URISyntaxException {
        return removeFromFavorites(topic.getId());
    }

    public static String removeFromFavorites(CharSequence topicId) throws IOException, ParseException, URISyntaxException {
        return TopicApi.deleteFromFavorites(Client.getInstance(), topicId.toString());
    }


    public DownloadTask downloadFile(Context context, String url, int notificationId, String tempFilePath) {
        DownloadTask downloadTask = m_DownloadTasks.add(url, notificationId, null);

        Intent intent = new Intent(context, DownloadsService.class);
        intent.putExtra(DownloadsService.DOWNLOAD_FILE_ID_KEY, notificationId);
        intent.putExtra(DownloadsService.DOWNLOAD_FILE_TEMP_NAME_KEY, tempFilePath);
        intent.putExtra("receiver", new DownloadReceiver(new Handler(), context));
        context.startService(intent);

        return downloadTask;
    }

    private DownloadTasks m_DownloadTasks = new DownloadTasks();

    public DownloadTasks getDownloadTasks() {
        return m_DownloadTasks;
    }


}