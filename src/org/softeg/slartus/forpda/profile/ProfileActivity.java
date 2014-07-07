package org.softeg.slartus.forpda.profile;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.softeg.slartus.forpda.BaseFragmentActivity;
import org.softeg.slartus.forpda.Client;
import org.softeg.slartus.forpda.IntentActivity;
import org.softeg.slartus.forpda.R;
import org.softeg.slartus.forpda.ReputationActivity;
import org.softeg.slartus.forpda.classes.AlertDialogBuilder;
import org.softeg.slartus.forpda.qms_2_0.QmsNewThreadActivity;
import org.softeg.slartus.forpda.search.SearchActivity;
import org.softeg.slartus.forpdaapi.UserProfile;

import java.util.ArrayList;

/**
 * User: slinkin
 * Date: 21.11.11
 * Time: 14:44
 */
public class ProfileActivity extends BaseFragmentActivity {
    private String m_UserId = "";
    private UserProfile m_UserProfile;
    private MenuFragment mFragment1;
    private TabHost mTabHost;
    private ViewPager viewPager;
    private TabsAdapter tabsAdapter;
    public static final String USER_ID_KEY = "UserIdKey";
    public static final String USER_NAME_KEY = "UserNameKey";
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.profile_activity);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getSupportActionBar().setSubtitle(getString(R.string.profile));
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();

        viewPager = (ViewPager) findViewById(R.id.pager);
        tabsAdapter = new TabsAdapter(ProfileActivity.this, mTabHost, viewPager);

        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();

            if (extras != null) {
                m_UserId = extras.getString(USER_ID_KEY);
                if (extras.containsKey(USER_NAME_KEY))
                    setUserName(extras.getString(USER_NAME_KEY));
            }
        }

        Bundle bundle = new Bundle();
        bundle.putString(USER_ID_KEY, m_UserId);
        tabsAdapter.addTab(mTabHost.newTabSpec("main").setIndicator(createTabView(this, getString(R.string.Profile))), ProfileMainView.class, bundle);
        tabsAdapter.addTab(mTabHost.newTabSpec("views").setIndicator(createTabView(this, getString(R.string.Views))), ProfileViewsFragment.class, bundle);
        tabsAdapter.addTab(mTabHost.newTabSpec("comments").setIndicator(createTabView(this, getString(R.string.Comments))), ProfileCommentsFragment.class, bundle);
        tabsAdapter.addTab(mTabHost.newTabSpec("friends").setIndicator(createTabView(this, getString(R.string.Friends))), ProfileFriendsFragment.class, bundle);

        createActionMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return true;
    }

    public Handler getHandler() {
        return mHandler;
    }

    @Override
    protected void onPause() {
        super.onPause();
        getIntent().putExtra("CurrentTab", mTabHost.getCurrentTab());

        mTabHost.setCurrentTab(-1);
    }

    @Override
    protected void onResume() {
        super.onPause();
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras.containsKey("CurrentTab"))
                mTabHost.setCurrentTab(getIntent().getIntExtra("CurrentTab", 0));
        }

    }

    public static void startActivity(Context context, String userId) {
        startActivity(context, userId, null);
    }

    public static void startActivity(Context context, String userId, String userName) {
        Intent intent = new Intent(context, ProfileWebViewActivity.class);

        intent.putExtra(ProfileActivity.USER_ID_KEY, userId);
        intent.putExtra(ProfileActivity.USER_NAME_KEY, userName);

        context.startActivity(intent);
    }

    private View createTabView(final Context context, final String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        //view.seton
        assert view != null;
        TextView tv = (TextView) view.findViewById(R.id.tabsText);

        tv.setText(text);
        return view;
    }

    private void createActionMenu() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mFragment1 = (MenuFragment) fm.findFragmentByTag("f1");
        if (mFragment1 == null) {
            mFragment1 = new MenuFragment();
            ft.add(mFragment1, "f1");
        }
        ft.commit();
    }

    public String getUserId() {
        return m_UserId;
    }

    public String getUserNick() {
        if (m_UserProfile != null)
        return m_UserProfile.login;
return null;
    }

    private void setUserName(String userName) {
        if (TextUtils.isEmpty(userName))
            return;
        if (m_UserProfile != null)
            m_UserProfile.login = userName;
        setTitle(userName);

    }

    public void setUserProfile(UserProfile value) {
        m_UserProfile = value;
        if (m_UserProfile != null)
            setUserName(m_UserProfile.login);
    }

    public UserProfile getUserProfile() {
        return m_UserProfile;
    }

    public static final class MenuFragment extends SherlockFragment {
        public MenuFragment() {

        }

        private ProfileActivity getProfileActivity() {

            return (ProfileActivity) getActivity();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            com.actionbarsherlock.view.MenuItem item;

            if (Client.getInstance().getLogined() && !getProfileActivity().getUserId().equals(Client.getInstance().UserId)) {


                item = menu.add(getString(R.string.MessagesQms)).setIcon(R.drawable.ic_menu_send);
                item.setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem menuItem) {
                        QmsNewThreadActivity.showUserNewThread(getActivity(), getProfileActivity().getUserId(), getProfileActivity().getUserNick());

                        return true;
                    }
                });
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            }


            item = menu.add(getString(R.string.Reputation)).setIcon(R.drawable.ic_menu_view);
            item.setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem menuItem) {
                    new AlertDialogBuilder(getActivity())
                            .setTitle("Репутация")
                            .setAdapterSingleChoiceItems(new CharSequence[]{"+1", "Посмотреть", "-1", "Действия с репутацией"}, -1, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    switch (i) {
                                        case 0:
                                            ReputationActivity.plusRep(getActivity(), getProfileActivity().getHandler(), getProfileActivity().getUserId(), getProfileActivity().getUserNick());
                                            break;
                                        case 1:
                                            ReputationActivity.showRep(getActivity(), getProfileActivity().getUserId());
                                            break;
                                        case 2:
                                            ReputationActivity.minusRep(getActivity(), getProfileActivity().getHandler(), getProfileActivity().getUserId(), getProfileActivity().getUserNick());
                                            break;
                                        case 3:
                                            ReputationActivity.showSelfRep(getActivity(), getProfileActivity().getUserId());
                                            break;
                                    }
                                    dialogInterface.dismiss();
                                }
                            })
                            .create().show();

                    return true;
                }
            });
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            item = menu.add(getString(R.string.FindUserTopics)).setIcon(R.drawable.ic_menu_view);
            item.setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem menuItem) {
                    SearchActivity.findUserTopicsActivity(getActivity(), getProfileActivity().getUserNick());
                    return true;
                }
            });
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            item = menu.add(getString(R.string.FindUserPosts)).setIcon(R.drawable.ic_menu_view);
            item.setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem menuItem) {
                    SearchActivity.findUserPostsActivity(getActivity(), getProfileActivity().getUserNick());
                    return true;
                }
            });
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

            item = menu.add(getString(R.string.OpenInBrowser)).setIcon(R.drawable.ic_menu_view);
            item.setOnMenuItemClickListener(new com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem menuItem) {
                    IntentActivity.showInDefaultBrowser(getActivity(), "http://4pda.ru/forum/index.php?showuser=" + getProfileActivity().getUserId());
                    return true;
                }
            });
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
    }

    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final ProfileActivity mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;
            private Fragment fragment;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }

            public void setFragment(Fragment fragment) {
                this.fragment = fragment;
            }

            public Fragment getFragment() {
                return fragment;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }


            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(ProfileActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }


        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            Boolean active = position == mViewPager.getCurrentItem();

            if (info.getFragment() == null) {
                ProfileFragment fragment = (ProfileFragment) Fragment.instantiate(mContext, info.clss.getName(), info.args);

                fragment.setOnProfileChangedListener(new ProfileFragment.OnProfileChanged() {
                    public void onProfileChanged(UserProfile userProfile) {
                        mContext.setUserProfile(userProfile);
                    }
                });

                info.setFragment(fragment);
            }
            ((ProfileFragment) info.getFragment()).setUserProfile(mContext.getUserProfile());
            ((ProfileFragment) info.getFragment()).Active = active;

            return info.getFragment();
        }


        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
            ((ProfileMainView) getItem(mViewPager.getCurrentItem())).startLoad();
        }


        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }


        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }


        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void finishUpdate(android.view.ViewGroup container) {
            super.finishUpdate(container);
        }
    }
}
