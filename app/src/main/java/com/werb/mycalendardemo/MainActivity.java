package com.werb.mycalendardemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionButton;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionHelper;
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionLayout;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RFACLabelItem;
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RapidFloatingActionContentLabelList;
import com.werb.mycalendardemo.alarmremind.SendAlarmBroadcast;
import com.werb.mycalendardemo.customview.calendar.CalendarView;
import com.werb.mycalendardemo.database.AlarmDBSupport;
import com.werb.mycalendardemo.models.BaseCalendarEvent;
import com.werb.mycalendardemo.models.CalendarEvent;
import com.werb.mycalendardemo.pager.HomePager;
import com.werb.mycalendardemo.utils.BusProvider;
import com.werb.mycalendardemo.utils.CalendarManager;
import com.werb.mycalendardemo.utils.Events;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements RapidFloatingActionContentLabelList.OnRapidFloatingActionContentLabelListListener {

    //侧边栏布局
    @Bind(R.id.main_draw_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.left_draw)
    NavigationView mNavigationView;
    //悬浮按钮
    @Bind(R.id.fab_layout)
    RapidFloatingActionLayout fab_layout;
    @Bind(R.id.fab_button_group)
    RapidFloatingActionButton fab_button_group;
    private RapidFloatingActionHelper rfabHelper;
    //日历View
    @Bind(R.id.calendar_view)
    public CalendarView calendar_view;
    //CalendarView自定义控件的属性
    private int mCalendarHeaderColor, mCalendarDayTextColor, mCalendarPastDayTextColor, mCalendarCurrentDayColor;
    //月份
    @Bind(R.id.title_day)
    TextView title_day;
    //日程 ListView
    @Bind(R.id.main_frame)
    FrameLayout main_frame;

    //left_menu点击打开关闭侧边栏
    @OnClick(R.id.left_menu)
    void openLeftDrawe() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    //旋转箭头
    @Bind(R.id.title_arrow)
    ImageView title_arrow;
    //打开关闭CalendarView
    private boolean isOpen = true;//日历视图是否显示

    @OnClick(R.id.calendar_open_close)
    void openAndCloseCalendarView() {
        if (!isOpen) {
            calendar_view.setVisibility(View.VISIBLE);
            arrowRotateAnim(isOpen);
            isOpen = true;
        } else {
            calendar_view.setVisibility(View.GONE);
            arrowRotateAnim(isOpen);
            isOpen = false;
        }
    }

    //回到今天
    @OnClick(R.id.go_today)
    void goToday() {
        homePager.agenda_view.getAgendaListView().scrollToCurrentDate(CalendarManager.getInstance().getToday());
        calendar_view.scrollToDate(CalendarManager.getInstance().getToday(), CalendarManager.getInstance().getWeeks());
    }

    private HomePager homePager;
    private AlarmDBSupport support;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        initCalendarInfo();
        initLayoutView();
        setMonthTitle();
        initFab();

        //第一次进入初始化数据
        support = new AlarmDBSupport(getApplicationContext());
        List<CalendarEvent> eventList = new ArrayList<>();
        List<AlarmBean> alllist = support.getAll();
        mockList(eventList, alllist);
        System.out.println("---onCreate---" + eventList.size());
        homePager.initData(eventList);

        SendAlarmBroadcast.startAlarmService(this);
    }

    /**
     * 初始化布局
     */
    private void initLayoutView() {
        //初始化 CalendarView
        mCalendarHeaderColor = R.color.calendar_header_day_background;
        mCalendarDayTextColor = R.color.calendar_text_day;
        mCalendarCurrentDayColor = R.color.calendar_text_current_day;
        mCalendarPastDayTextColor = R.color.calendar_text_past_day;

        calendar_view.findViewById(R.id.cal_day_names).setBackgroundColor(getResources().getColor(mCalendarHeaderColor));

        // CalendarView 初始化,完成布局的初始化，数据的绑定,日期的显示
        calendar_view.init(CalendarManager.getInstance(this), getResources().getColor(mCalendarDayTextColor), getResources().getColor(mCalendarCurrentDayColor), getResources().getColor(mCalendarPastDayTextColor));

        //填充 HomePager
        homePager = new HomePager(MainActivity.this);
        main_frame.addView(homePager.mRootView);

    }

    /**
     * title_arrow 旋转动画
     */
    private void arrowRotateAnim(Boolean isOpen) {
        RotateAnimation anim;
        if (isOpen) {
            anim = new RotateAnimation(0, 180f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        } else {
            anim = new RotateAnimation(180f, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        }
        anim.setDuration(150);
        anim.setFillAfter(true);
        title_arrow.startAnimation(anim);
    }

    /**
     * 设置月份
     */
    private void setMonthTitle() {
        BusProvider.getInstance().toObserverable().subscribe(event -> {
            if (event instanceof Events.CalendarMonthEvent) {
                Events.CalendarMonthEvent monthEvent = (Events.CalendarMonthEvent) event;
                title_day.setText(monthEvent.getMonth());
            }
        });
    }

    /**
     * 初始化 Fab
     */
    private void initFab() {
        RapidFloatingActionContentLabelList rfaContent = new RapidFloatingActionContentLabelList(this);
        rfaContent.setOnRapidFloatingActionContentLabelListListener(this);
        List<RFACLabelItem> items = new ArrayList<>();
        items.add(new RFACLabelItem<Integer>()
                        .setLabel("活动")
                        .setResId(R.drawable.ic_access_alarms_white_24dp)
                        .setIconNormalColor(0xffd84315)
                        .setIconPressedColor(0xffbf360c)
                        .setLabelSizeSp(14)
                        .setWrapper(0)
        );
        rfaContent
                .setItems(items)
                .setIconShadowRadius(5)
                .setIconShadowColor(0xff888888)
                .setIconShadowDy(5)
        ;
        rfabHelper = new RapidFloatingActionHelper(
                this,
                fab_layout,
                fab_button_group,
                rfaContent
        ).build();

    }

    @Override
    public void onRFACItemLabelClick(int i, RFACLabelItem rfacLabelItem) {
        Toast.makeText(this, "Click Label !", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRFACItemIconClick(int i, RFACLabelItem rfacLabelItem) {
        if (i == 0) {
            Intent intent = new Intent(MainActivity.this, AddScheduleActivity.class);
            intent.putExtra("type","mainToAdd");
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        System.out.println("---onRestart");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        support.deactivate();
    }



    /**
     * 初始化日历信息
     */
    private void initCalendarInfo() {
        // 设置日历显示的时间，最大为当前时间+1年，最小为当前时间-2月
        Calendar minDate = Calendar.getInstance();
        Calendar maxDate = Calendar.getInstance();

        minDate.add(Calendar.MONTH, -5);
        minDate.set(Calendar.DAY_OF_MONTH, 1);
        maxDate.add(Calendar.YEAR, 1);
        //根据你传入的开始结束值，构建生成Calendar数据（各种Item，JavaBean）
        CalendarManager.getInstance(this).buildCal(minDate, maxDate, Locale.getDefault());
    }

    /**
     * 构建数据集合
     * @param eventList list显示的集合
     * @param beanList 从数据库中读到的全部数据集合
     */
    private void mockList(List<CalendarEvent> eventList, List<AlarmBean> beanList) {

        for (AlarmBean bean : beanList) {
            Calendar startTime1 = Calendar.getInstance();
            startTime1.set(bean.getYear(), bean.getMonth(), bean.getDay());
            Calendar endTime1 = Calendar.getInstance();
            endTime1.set(bean.getYear(), bean.getMonth(), bean.getDay());

            boolean isAllday;
            if (bean.getIsAllday() == 1) {
                isAllday = true;
            } else {
                isAllday = false;
            }

            int colorId = 0;
            switch (bean.getAlarmColor()) {
                case "默认颜色":
                    colorId = R.color.moren;
                    break;
                case "罗勒绿":
                    colorId = R.color.luolelv;
                    break;
                case "耀眼黄":
                    colorId = R.color.yaoyanhuang;
                    break;
                case "番茄红":
                    colorId = R.color.fanqiehong;
                    break;
                case "低调灰":
                    colorId = R.color.didiaohui;
                    break;
                case "橘子红":
                    colorId = R.color.juzihong;
                    break;
                case "深空蓝":
                    colorId = R.color.shenkonglan;
                    break;
            }

            Calendar startCalendar = Calendar.getInstance();
            startCalendar.set(Calendar.HOUR_OF_DAY, bean.getStartTimeHour());
            startCalendar.set(Calendar.MINUTE, bean.getStartTimeMinute());
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.set(Calendar.HOUR_OF_DAY, bean.getEndTimeHour());
            endCalendar.set(Calendar.MINUTE, bean.getEndTimeMinute());

            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
            String startTime = df.format(startCalendar.getTime());
            String endtTime = df.format(endCalendar.getTime());
            String startAndEndTime = startTime + "-" + endtTime;

            BaseCalendarEvent event1 = new BaseCalendarEvent(bean.getId(),bean.getTitle(), bean.getDescription(), bean.getLocal(),
                    ContextCompat.getColor(this, colorId), startTime1, endTime1, isAllday, startAndEndTime);
            System.out.println("---" + event1.toString());
            eventList.add(event1);
        }

    }
}
