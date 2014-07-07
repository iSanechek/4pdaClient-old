package org.softeg.slartus.forpda.profile;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;

import org.softeg.slartus.forpda.Client;
import org.softeg.slartus.forpda.HttpHelper;
import org.softeg.slartus.forpda.R;
import org.softeg.slartus.forpda.common.Log;
import org.softeg.slartus.forpdaapi.UserProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * User: slinkin
 * Date: 27.09.12
 * Time: 7:57
 */
public class ProfileMainView extends ProfileFragment
        implements LoaderManager.LoaderCallbacks<UserProfile> {

    private ImageView mImageAvatar;
    private ProgressBar mSpinnerAvatar;
    private ProgressBar pgsMain;

    private Drawable mDrawableAvatar;
    private String mUrlAvatar;

    private static String[] groups = new String[]{"Основное", "О себе", "Личная информация", "Интересы", "Другая информация", "Статистика",
            "Контактная информация"};
    // коллекция для групп
    ArrayList<Map<String, String>> groupData;

    // коллекция для элементов одной группы
    ArrayList<Map<String, String>> childDataItem;

    // общая коллекция для коллекций элементов
    ArrayList<ArrayList<Map<String, String>>> childData;

    // список аттрибутов группы или элемента
    Map<String, String> m;

    ExpandableListView elvMain;
    boolean mDualPane;

    public static String getGroupName(int groupPosition) {
        if (groupPosition == -1)
            return "Аватар";
        return groups[groupPosition];
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        View detailsFrame = getActivity().findViewById(R.id.details);
//        mDualPane = detailsFrame != null
//                && detailsFrame.getVisibility() == View.VISIBLE;

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        if (Active)
            getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist.  The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed.  Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }

        View v = inflater.inflate(R.layout.profile_main, null);

        pgsMain = (ProgressBar) v.findViewById(R.id.pgsMain);


        View imageView = inflater.inflate(R.layout.image_view, null);
        mSpinnerAvatar = (ProgressBar) imageView.findViewById(R.id.pgsAvatar);
        mImageAvatar = (ImageView) imageView.findViewById(R.id.imgAvatar);
        mImageAvatar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Object url = view.getTag();
                if (url == null || TextUtils.isEmpty(url.toString())) return;
//                ImageViewActivity.showImageUrl(getActivity(), url.toString());
                ProfileMainFullViewFragmentActivity.showActivity(getActivity(), -1, -1, "<img src=\"" + url + "\" border=\"0\">");

            }
        });

        elvMain = (ExpandableListView) v.findViewById(R.id.elvMain);
        elvMain.addHeaderView(imageView);

        elvMain.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                ProfileMainFullViewFragmentActivity.showActivity(getActivity(), groupPosition, childPosition, getUserProfile().getValue(groupPosition, childPosition));

                return true;
            }
        });

        return v;
    }

    @Override
    public void startLoad() {
        if (Active && getActivity() != null)
            if (getLoaderManager().getLoader(0) == null)
                getLoaderManager().initLoader(0, null, this);

        Active = true;
    }

    public String getUserId() {
        return getArguments().getString(ProfileActivity.USER_ID_KEY);
    }

    protected void fillData(UserProfile userProfile) {
        // setTitle(userProfile.login+": Просмотр профиля");
        //setImageDrawablePhoto(userProfile.personalPhoto);
        setImageDrawableAvatar(userProfile.avatar);
        // заполняем коллекцию групп из массива с названиями групп
        groupData = new ArrayList<Map<String, String>>();
        for (String group : groups) {
            // заполняем список аттрибутов для каждой группы
            m = new HashMap<String, String>();
            m.put("groupName", group); // имя компании
            groupData.add(m);
        }

        // список аттрибутов групп для чтения
        String groupFrom[] = new String[]{"groupName"};
        // список ID view-элементов, в которые будет помещены аттрибуты групп
        int groupTo[] = new int[]{android.R.id.text1};


        // создаем коллекцию для коллекций элементов
        childData = new ArrayList<ArrayList<Map<String, String>>>();

        fillGroups(userProfile);

        // список аттрибутов элементов для чтения
        String childFrom[] = new String[]{"itemName"};
        // список ID view-элементов, в которые будет помещены аттрибуты элементов
        int childTo[] = new int[]{android.R.id.text1};

        SimpleExpandableListAdapter adapter = new SimpleExpandableListAdapter(
                getActivity(),
                groupData,
                R.layout.simple_expandable_list_item_1,
                groupFrom,
                groupTo,
                childData,
                R.layout.simple_list_item_1,
                childFrom,
                childTo);
        elvMain.setAdapter(adapter);
        elvMain.expandGroup(0);

    }

    private void fillGroups(UserProfile userProfile) {

        //0
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getMain())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);

        //1
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getAboutGroup())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);
        //2
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getPrivateInfo())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);
        //3
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getInterests())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);
        //4
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getOtherInfo())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);
        //5
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getStatistic())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);
        //6
        childDataItem = new ArrayList<Map<String, String>>();
        for (String item : UserProfile.getGroupSimpleData(userProfile.getContactInfo())) {
            m = new HashMap<String, String>();
            m.put("itemName", item);
            childDataItem.add(m);
        }
        childData.add(childDataItem);
    }

    private static final int COMPLETE = 0;
    private static final int FAILED = 1;

    private void setImageDrawableAvatar(final String imageUrl) {
        mImageAvatar.setTag(imageUrl);
        if (TextUtils.isEmpty(imageUrl)) {
            mImageAvatar.setVisibility(View.VISIBLE);
            mSpinnerAvatar.setVisibility(View.GONE);
            return;
        }
        mDrawableAvatar = null;
        mSpinnerAvatar.setVisibility(View.VISIBLE);
        mImageAvatar.setVisibility(View.GONE);
        mUrlAvatar = imageUrl;
        new Thread() {
            public void run() {
                HttpHelper httpHelper = null;
                try {
                    httpHelper = new HttpHelper();
                    mDrawableAvatar = Drawable.createFromStream(httpHelper.getImageStream(imageUrl), "name");

                    imageLoadedHandlerAvatar.sendEmptyMessage(COMPLETE);

                } catch (OutOfMemoryError e) {
                    Bundle data = new Bundle();
                    data.putSerializable("exception", e);
                    data.putString("message", "Нехватка памяти: " + mUrlAvatar);
                    Message message = new Message();
                    message.what = FAILED;
                    message.setData(data);
                    imageLoadedHandlerAvatar.sendMessage(message);
                } catch (Exception e) {
                    Bundle data = new Bundle();
                    data.putSerializable("exception", e);
                    data.putString("message", "Ошибка загрузки изображения по адресу: " + mUrlAvatar);
                    Message message = new Message();
                    message.what = FAILED;
                    message.setData(data);
                    imageLoadedHandlerAvatar.sendMessage(message);

                } finally {
                    if (httpHelper != null)
                        httpHelper.close();
                }
            }
        }.start();
    }

    /**
     * Callback that is received once the image has been downloaded
     */
    private final Handler imageLoadedHandlerAvatar = new Handler(new Handler.Callback() {

        public boolean handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case COMPLETE:

                        mImageAvatar.setImageDrawable(mDrawableAvatar);

                        mImageAvatar.setVisibility(View.VISIBLE);
                        mSpinnerAvatar.setVisibility(View.GONE);
                        break;
                    case FAILED:
                        mSpinnerAvatar.setVisibility(View.GONE);
                        Bundle data = msg.getData();
                        Log.e(getActivity(), data.getString("message"), (Throwable) data.getSerializable("exception"));
                    default:
                        // Could change image here to a 'failed' image
                        // otherwise will just keep on spinning
                        break;
                }
            } catch (Exception ex) {
                Log.e(getActivity(), "Ошибка загрузки изображения по адресу: " + mUrlAvatar, ex);
            }

            return true;
        }
    });


    public Loader<UserProfile> onCreateLoader(int i, Bundle bundle) {
        LoadTask loadTask = new LoadTask(getActivity(), getUserProfile());

        loadTask.UserId = getUserId();
        return loadTask;
    }

    public void onLoadFinished(Loader<UserProfile> userProfileLoader, UserProfile userProfile) {
        if (((LoadTask) userProfileLoader).Ex != null) {
            Log.e(getActivity(), ((LoadTask) userProfileLoader).Ex);
            setState(false);
            return;
        }
        setUserProfile(userProfile);
        doOnProfileChanged(userProfile);
        fillData(userProfile);
        setState(false);
    }

    public void onLoaderReset(Loader<UserProfile> userProfileLoader) {
        setState(false);
    }

    protected void setState(Boolean load) {
        pgsMain.setVisibility(load ? View.VISIBLE : View.GONE);
        elvMain.setVisibility(load ? View.GONE : View.VISIBLE);
    }

    public static class LoadTask extends AsyncTaskLoader<UserProfile> {

        protected UserProfile m_UserProfile;

        public LoadTask(Context context, UserProfile userProfile) {
            super(context);
            m_UserProfile = userProfile;

        }

        public String UserId;
        public Throwable Ex;

        @Override
        public UserProfile loadInBackground() {
            Ex = null;
            try {
                return loadData(m_UserProfile, UserId);
            } catch (Throwable e) {
                Ex = e;
                return null;
            }
        }

        protected UserProfile loadData(UserProfile userProfile, String userId) throws Throwable {
            return Client.getInstance().loadUserProfile(userProfile, userId);
        }

        /**
         * Called when there is new data to deliver to the client.  The
         * super class will take care of delivering it; the implementation
         * here just adds a little more logic.
         */
        @Override
        public void deliverResult(UserProfile userProfile) {
            if (isReset()) {
                // An async query came in while the loader is stopped.  We
                // don't need the result.
                if (userProfile != null) {
                    onReleaseResources(userProfile);
                }
            }
            UserProfile oldApps = userProfile;
            m_UserProfile = userProfile;

            if (isStarted()) {
                // If the Loader is currently started, we can immediately
                // deliver its results.
                super.deliverResult(userProfile);
            }

            // At this point we can release the resources associated with
            // 'oldApps' if needed; now that the new result is delivered we
            // know that it is no longer in use.
            if (oldApps != null) {
                onReleaseResources(oldApps);
            }
        }

        protected Boolean needLoad() {
            return m_UserProfile == null || TextUtils.isEmpty(m_UserProfile.registration);
        }

        /**
         * Handles a request to start the Loader.
         */
        @Override
        protected void onStartLoading() {
            if (needLoad() || takeContentChanged()) {
                // If the data has changed since the last time it was loaded
                // or is not currently available, start a load.
                forceLoad();
                return;
            }
            if (m_UserProfile != null) {
                // If we currently have a result available, deliver it
                // immediately.
                deliverResult(m_UserProfile);
            }


        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override
        public void onCanceled(UserProfile apps) {
            super.onCanceled(apps);

            // At this point we can release the resources associated with 'apps'
            // if needed.
            onReleaseResources(apps);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override
        protected void onReset() {
            super.onReset();

            // Ensure the loader is stopped
            onStopLoading();

            // At this point we can release the resources associated with 'apps'
            // if needed.
            if (m_UserProfile != null) {
                onReleaseResources(m_UserProfile);
                m_UserProfile = null;
            }


        }

        /**
         * Helper function to take care of releasing resources associated
         * with an actively loaded data set.
         */
        protected void onReleaseResources(UserProfile apps) {
            // For a simple List<> there is nothing to do.  For something
            // like a Cursor, we would close it here.
        }
    }

//    private static class ProfileMainAdapter extends SimpleExpandableListAdapter{
//        private LayoutInflater m_Inflater;
//        static class ViewHolder {
//            AdvWebView webView;
//
//        }
//
//        public ProfileMainAdapter(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo, List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
//            super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
//            m_Inflater = LayoutInflater.from(context);
//        }
//
//        public android.view.View getChildView(int groupPosition, int childPosition, boolean isLastChild, android.view.View convertView, android.view.ViewGroup parent){
//            final ViewHolder holder;
//            if (convertView == null) {
//
//                convertView = m_Inflater.inflate(R.layout.profile_main_item, parent, false);
//                holder = new ViewHolder();
//                holder.webView=(AdvWebView)convertView.findViewById(R.id.wvBody);
//
//                convertView.setTag(holder);
//
//            } else {
//                holder = (ViewHolder) convertView.getTag();
//            }
//
//            Object item=getChild(groupPosition,childPosition);
//            String body=item==null?"":item.toString();
//            holder.webView.loadDataWithBaseURL("\"file:///android_asset/\"", body, "text/html", "UTF-8", null);
//            return convertView;
//        }
//    }

}
