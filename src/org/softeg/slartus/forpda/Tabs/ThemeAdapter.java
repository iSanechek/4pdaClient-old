package org.softeg.slartus.forpda.Tabs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.softeg.slartus.forpda.Client;
import org.softeg.slartus.forpda.QuickStartActivity;
import org.softeg.slartus.forpda.R;
import org.softeg.slartus.forpda.classes.AlertDialogBuilder;
import org.softeg.slartus.forpda.classes.ExtTopic;
import org.softeg.slartus.forpda.classes.common.ExtUrl;
import org.softeg.slartus.forpda.common.HelpTask;
import org.softeg.slartus.forpda.common.Log;
import org.softeg.slartus.forpdaapi.Topic;
import org.softeg.slartus.forpdaapi.TopicApi;
import org.softeg.slartus.forpdacommon.ExtPreferences;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Admin
 * Date: 21.09.11
 * Time: 0:03
 * To change this template use File | Settings | File Templates.
 */
public class ThemeAdapter extends ArrayAdapter<ExtTopic> {
    private LayoutInflater m_Inflater;

    private int m_ThemeTitleSize = 13;
    private int m_TopTextSize = 10;
    private int m_BottomTextSize = 11;
    private int m_FlagTextSize = 12;
    private CharSequence tabId;
    private CharSequence template;


    public ThemeAdapter(Context context, CharSequence tabId, CharSequence template,
                        int textViewResourceId, ArrayList<ExtTopic> objects) {
        super(context, textViewResourceId, objects);
        this.tabId = tabId;
        this.template = template;

        m_Inflater = LayoutInflater.from(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        m_ThemeTitleSize = ExtPreferences.parseInt(prefs,
                "interface.themeslist.title.font.size", 13);
        m_TopTextSize = (int) Math.floor(10.0 / 13 * m_ThemeTitleSize);
        m_BottomTextSize = (int) Math.floor(11.0 / 13 * m_ThemeTitleSize);
        m_FlagTextSize = (int) Math.floor(12.0 / 13 * m_ThemeTitleSize);

    }

    private String params;

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    private Boolean m_ShowForumTitle = false;

    public void showForumTitle(Boolean isShow) {
        m_ShowForumTitle = isShow;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {


            convertView = m_Inflater.inflate(R.layout.theme_item, parent, false);


            holder = new ViewHolder();
            holder.txtIsNew = (ImageView) convertView
                    .findViewById(R.id.txtIsNew);
            holder.usericon = convertView.findViewById(R.id.usericon);
            //holder.txtIsNew.setTextSize(m_FlagTextSize);

            holder.txtAuthor = (TextView) convertView
                    .findViewById(R.id.txtAuthor);
            holder.txtAuthor.setTextSize(m_TopTextSize);

            holder.txtLastMessageDate = (TextView) convertView
                    .findViewById(R.id.txtLastMessageDate);
            holder.txtLastMessageDate.setTextSize(m_TopTextSize);

            holder.txtTitle = (TextView) convertView
                    .findViewById(R.id.txtTitle);
            holder.txtTitle.setTextSize(m_ThemeTitleSize);

            holder.txtDescription = (TextView) convertView
                    .findViewById(R.id.txtDescription);
            holder.txtDescription.setTextSize(m_BottomTextSize);
//            holder.txtPostsCount=(TextView) convertView
//                    .findViewById(R.id.txtPostsCount);

            if (m_ShowForumTitle) {
                holder.txtForumTitle = (TextView) convertView
                        .findViewById(R.id.txtForumTitle);
                holder.txtForumTitle.setVisibility(View.VISIBLE);
                holder.txtForumTitle.setTextSize(m_BottomTextSize);
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ExtTopic topic = this.getItem(position);

        holder.txtAuthor.setText(topic.getLastMessageAuthor());
        holder.txtLastMessageDate.setText(topic.getLastMessageDateStr());
        holder.txtTitle.setText(topic.getTitle());
        holder.txtDescription.setText(topic.getDescription());
        //holder.txtPostsCount.setText(topic.getPostsCount());
        if (m_ShowForumTitle && !TextUtils.isEmpty(topic.getForumTitle())) {
            holder.txtForumTitle.setText("@" + topic.getForumTitle());
        }

        if (topic.getIsNew()) {
            holder.txtIsNew.setImageResource(R.drawable.new_flag);
        } else if (topic.getIsOld()) {
            holder.txtIsNew.setImageResource(R.drawable.old_flag);
        } else {
            holder.txtIsNew.setImageBitmap(null);
        }


        return convertView;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo,
                                    Boolean addFavorites, Handler handler) {
        try {

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            if (info.id == -1) return;
            final ExtTopic topic = getItem((int) info.id);
            if (TextUtils.isEmpty(topic.getId())) return;

            menu.add(getContext().getString(R.string.navigate_getfirstpost)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem menuItem) {
                    showSaveNavigateActionDialog(topic, Topic.NAVIGATE_VIEW_FIRST_POST, "");
                    return true;
                }
            });
            menu.add(getContext().getString(R.string.navigate_getlastpost)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem menuItem) {

                    showSaveNavigateActionDialog(topic, Topic.NAVIGATE_VIEW_LAST_POST, "view=getlastpost");
                    return true;
                }
            });
            menu.add(getContext().getString(R.string.navigate_getnewpost)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem menuItem) {

                    showSaveNavigateActionDialog(topic, Topic.NAVIGATE_VIEW_NEW_POST, "view=getnewpost");
                    return true;
                }
            });

            ExtUrl.addUrlSubMenu(handler, getContext(), menu, topic.getShowBrowserUrl(params), topic.getId(),
                    topic.getTitle());
            addOptionsMenu(getContext(), handler, menu, topic, addFavorites, null);
        } catch (Exception ex) {
            Log.e(this.getContext(), ex);
        }
    }

    private void showTopicActivity(ExtTopic topic, String params) {
        topic.showActivity(getContext(), params);
        topic.setIsNew(false);
        notifyDataSetChanged();
    }

    private void showSaveNavigateActionDialog(final ExtTopic topic, final CharSequence selectedAction, final String params) {
        showSaveNavigateActionDialog(getContext(), getTabId(), getTemplate(), selectedAction,
                new Runnable() {
                    @Override
                    public void run() {
                        showTopicActivity(topic, params);
                    }
                });
    }

    public static void showSaveNavigateActionDialog(Context context, final CharSequence tabId, final CharSequence template,
                                                    final CharSequence selectedAction,
                                                    final Runnable showTopicAction) {
        final String navigateAction = ThemesTab.getTopicNavigateAction(tabId, template);
        if (navigateAction == null || !selectedAction.equals(navigateAction)) {
            new AlertDialogBuilder(context)
                    .setTitle("Действие по умолчанию")
                    .setMessage("Назначить по умолчанию?")
                    .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            ThemesTab.saveOpenThemeParams(tabId, template, selectedAction);
                            showTopicAction.run();

                        }
                    })
                    .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            showTopicAction.run();
                        }
                    })
                    .create().show();
        } else {
            showTopicAction.run();
        }

    }


    public  SubMenu addOptionsMenu(final Context context, final Handler mHandler, Menu menu, final ExtTopic topic,
                                         Boolean addFavorites, final String shareItUrl) {
        SubMenu optionsMenu = menu.addSubMenu("Опции..").setIcon(R.drawable.ic_menu_more);

        configureOptionsMenu(context, mHandler, optionsMenu, topic, addFavorites, shareItUrl);
        return optionsMenu;
    }

    public  void configureOptionsMenu(final Context context, final Handler mHandler, SubMenu optionsMenu, final ExtTopic topic,
                                            Boolean addFavorites, final String shareItUrl) {
        optionsMenu.clear();

        if (Client.getInstance().getLogined()) {

            if (addFavorites) {
                Boolean isFavotitesList = Tabs.TAB_FAVORITES.equals(getTemplate());
                optionsMenu.add(context.getString(R.string.AddToFavorites)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        topic.startSubscribe(context, mHandler);

                        return true;
                    }
                });

                optionsMenu.add(context.getString(R.string.DeleteFromFavorites)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        final HelpTask helpTask = new HelpTask(context, context.getString(R.string.DeletingFromFavorites));
                        helpTask.setOnPostMethod(new HelpTask.OnMethodListener() {
                            public Object onMethod(Object param) {
                                if (helpTask.Success)
                                    Toast.makeText(context, (String) param, Toast.LENGTH_SHORT).show();
                                else
                                    Log.e(context, helpTask.ex);
                                return null;
                            }
                        });
                        helpTask.execute(new HelpTask.OnMethodListener() {
                            public Object onMethod(Object param) throws IOException, ParseException, URISyntaxException {
                                return topic.removeFromFavorites();  //To change body of implemented methods use File | Settings | File Templates.
                            }
                        }
                        );

                        return true;
                    }
                });

                 if(isFavotitesList) {
                     optionsMenu.add("\"Закрепить\" в избранном").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                         public boolean onMenuItemClick(MenuItem menuItem) {

                             final HelpTask helpTask = new HelpTask(context, "\"Закрепить\" в избранном");
                             helpTask.setOnPostMethod(new HelpTask.OnMethodListener() {
                                 public Object onMethod(Object param) {
                                     if (helpTask.Success)
                                         Toast.makeText(context, (String) param, Toast.LENGTH_SHORT).show();
                                     else
                                         Log.e(context, helpTask.ex);
                                     return null;
                                 }
                             });
                             helpTask.execute(new HelpTask.OnMethodListener() {
                                                  public Object onMethod(Object param) throws IOException, ParseException, URISyntaxException {
                                                      return TopicApi.pinFavorite(Client.getInstance(), topic.getId().toString(), TopicApi.TRACK_TYPE_PIN);
                                                  }
                                              }
                             );

                             return true;
                         }
                     });

                     optionsMenu.add("\"Открепить\" в избранном").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                         public boolean onMenuItemClick(MenuItem menuItem) {

                             final HelpTask helpTask = new HelpTask(context, "\"Открепить\" в избранном");
                             helpTask.setOnPostMethod(new HelpTask.OnMethodListener() {
                                 public Object onMethod(Object param) {
                                     if (helpTask.Success)
                                         Toast.makeText(context, (String) param, Toast.LENGTH_SHORT).show();
                                     else
                                         Log.e(context, helpTask.ex);
                                     return null;
                                 }
                             });
                             helpTask.execute(new HelpTask.OnMethodListener() {
                                                  public Object onMethod(Object param) throws IOException, ParseException, URISyntaxException {
                                                      return TopicApi.pinFavorite(Client.getInstance(), topic.getId().toString(), TopicApi.TRACK_TYPE_UNPIN);
                                                  }
                                              }
                             );

                             return true;
                         }
                     });
                 }
                optionsMenu.add(context.getString(R.string.OpenTopicForum)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        Intent intent = new Intent(context, QuickStartActivity.class);

                        intent.putExtra("template", Tabs.TAB_FORUMS);
                        intent.putExtra(ForumTreeTab.KEY_FORUM_ID, topic.getForumId());

                        intent.putExtra(ForumTreeTab.KEY_TOPIC_ID, topic.getId());
                        context.startActivity(intent);

                        return true;
                    }
                });
            }


        }
        optionsMenu.add(context.getString(R.string.NotesByTopic)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {

                Intent intent = new Intent(context, QuickStartActivity.class);
                intent.putExtra("template", NotesTab.TEMPLATE);
                intent.putExtra(NotesTab.TOPIC_ID_KEY, topic.getId());
                context.startActivity(intent);
                return true;
            }
        });
        optionsMenu.add(context.getString(R.string.Share)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {

                try {
                    Intent sendMailIntent = new Intent(Intent.ACTION_SEND);
                    sendMailIntent.putExtra(Intent.EXTRA_SUBJECT, topic.getTitle());
                    sendMailIntent.putExtra(Intent.EXTRA_TEXT, TextUtils.isEmpty(shareItUrl) ? ("http://4pda.ru/forum/index.php?showtopic=" + topic.getId()) : shareItUrl);
                    sendMailIntent.setType("text/plain");

                    context.startActivity(Intent.createChooser(sendMailIntent, context.getString(R.string.Share)));
                } catch (Exception ex) {
                    return false;
                }
                return true;
            }
        });
        //return optionsMenu;
    }

    public CharSequence getTabId() {
        return tabId;
    }

    public CharSequence getTemplate() {
        return template;
    }


    public class ViewHolder {
        View usericon;
        ImageView txtIsNew;
        TextView txtAuthor;
        TextView txtLastMessageDate;
        TextView txtTitle;
        TextView txtDescription;
        TextView txtForumTitle;

    }

}
