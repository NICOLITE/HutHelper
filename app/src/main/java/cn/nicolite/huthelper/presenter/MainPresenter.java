package cn.nicolite.huthelper.presenter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.nicolite.huthelper.BuildConfig;
import cn.nicolite.huthelper.base.presenter.BasePresenter;
import cn.nicolite.huthelper.db.dao.ConfigureDao;
import cn.nicolite.huthelper.db.dao.MenuDao;
import cn.nicolite.huthelper.db.dao.TimeAxisDao;
import cn.nicolite.huthelper.model.Constants;
import cn.nicolite.huthelper.model.bean.Configure;
import cn.nicolite.huthelper.model.bean.HttpResult;
import cn.nicolite.huthelper.model.bean.Menu;
import cn.nicolite.huthelper.model.bean.TimeAxis;
import cn.nicolite.huthelper.model.bean.Update;
import cn.nicolite.huthelper.model.bean.User;
import cn.nicolite.huthelper.model.bean.Weather;
import cn.nicolite.huthelper.network.api.APIUtils;
import cn.nicolite.huthelper.network.exception.ExceptionEngine;
import cn.nicolite.huthelper.utils.ListUtils;
import cn.nicolite.huthelper.utils.LogUtils;
import cn.nicolite.huthelper.view.activity.MainActivity;
import cn.nicolite.huthelper.view.activity.WebViewActivity;
import cn.nicolite.huthelper.view.iview.IMainView;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * MainPresenter
 * Created by nicolite on 17-10-22.
 */

public class MainPresenter extends BasePresenter<IMainView, MainActivity> {

    public MainPresenter(IMainView view, MainActivity activity) {
        super(view, activity);
    }

    public void showWeather() {
        APIUtils
                .getWeatherAPI()
                .getWeather()
                .compose(getActivity().<Weather>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Weather>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (getView() != null) {
                            getView().showLoading();
                        }
                    }

                    @Override
                    public void onNext(@NonNull Weather weather) {
                        if (getView() != null) {
                            getView().closeLoading();
                            getView().showWeather(weather.getData().getCity(), weather.getData().getWendu(),
                                    weather.getData().getForecast().get(0).getType());

                            String userId = getLoginUser();

                            if (TextUtils.isEmpty(userId)) {
                                getView().showMessage("获取当前登录用户失败，请重新登录！");
                                return;
                            }

                            final ConfigureDao configureDao = getDaoSession().getConfigureDao();
                            List<Configure> list = configureDao.queryBuilder().where(ConfigureDao.Properties.UserId.eq(userId)).list();
                            if (ListUtils.isEmpty(list)){
                                getView().showMessage("获取用户信息失败！");
                                return;
                            }

                            Configure configure = list.get(0);
                            configure.setCity(weather.getData().getCity());
                            configure.setTmp(weather.getData().getWendu());
                            configure.setContent(weather.getData().getForecast().get(0).getType());
                            configureDao.update(configure);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (getView() != null) {
                            getView().closeLoading();
                            getView().showMessage(ExceptionEngine.handleException(e).getMsg());
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void showTimeAxis() {
        APIUtils
                .getDateLineAPI()
                .getTimeAxis()
                .compose(getActivity().<List<TimeAxis>>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<TimeAxis>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        if (getView() != null) {
                            getView().showLoading();
                        }
                    }

                    @Override
                    public void onNext(@NonNull List<TimeAxis> timeAxisList) {
                        if (getView() != null) {
                            getView().closeLoading();
                            if (!ListUtils.isEmpty(timeAxisList)) {
                                getView().showTimeAxis(timeAxisList);
                                TimeAxisDao timeAxisDao = getDaoSession().getTimeAxisDao();
                                timeAxisDao.deleteAll();
                                for (TimeAxis timeAxis : timeAxisList) {
                                    timeAxisDao.insert(timeAxis);
                                }

                            } else {
                                TimeAxisDao timeAxisDao = getDaoSession().getTimeAxisDao();
                                List<TimeAxis> list = timeAxisDao.queryBuilder().list();
                                if (!ListUtils.isEmpty(list)){
                                    getView().showTimeAxis(list);
                                }
                                getView().showMessage("未获取到时间轴数据！");
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (getView() != null) {
                            getView().closeLoading();

                            TimeAxisDao timeAxisDao = getDaoSession().getTimeAxisDao();
                            List<TimeAxis> list = timeAxisDao.queryBuilder().list();
                            if (!ListUtils.isEmpty(list)){
                                getView().showTimeAxis(list);
                            }

                            getView().showMessage(ExceptionEngine.handleException(e).getMsg());
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void showNotification() {

    }

    public void showSyllabus() {

    }

    public void showMenu() {

        MenuDao menuDao = getDaoSession().getMenuDao();

        if (menuDao.count() == 0 || menuDao.count() > 15) {
            List<Menu> menuItems = new ArrayList<>();
            Menu item = new Menu((long)1, 0, 0, WebViewActivity.TYPE_LIBRARY, "图书馆", "cn.nicolite.huthelper.view.activity.WebViewActivity", true);
            menuItems.add(item);
            item = new Menu((long)2, 1, 1, 0, "课程表", "cn.nicolite.huthelper.view.activity.CourseTableActivity", true);
            menuItems.add(item);
            item = new Menu((long)3, 2, 2, 0, "考试查询", "cn.nicolite.huthelper.view.activity.ExamActivity", true);
            menuItems.add(item);
            item = new Menu((long)4, 3, 3, 0, "成绩查询", "cn.nicolite.huthelper.view.activity.NewGradeActivity", true);
            menuItems.add(item);
            item = new Menu((long)5, 4, 4, WebViewActivity.TYPE_HOMEWORK, "网上作业", "cn.nicolite.huthelper.view.activity.WebViewActivity", true);
            menuItems.add(item);
            item = new Menu((long)6, 5, 5, 0, "二手市场", "cn.nicolite.huthelper.view.activity.MarketActivity", true);
            menuItems.add(item);
            item = new Menu((long)7, 6, 6, 0, "校园说说", "cn.nicolite.huthelper.view.activity.SayActivity", true);
            menuItems.add(item);
            item = new Menu((long)8, 7, 7, 0, "电费查询", "cn.nicolite.huthelper.view.activity.ElectricActivity", true);
            menuItems.add(item);
            item = new Menu((long)9, 8, 8, 0, "校招薪水", "cn.nicolite.huthelper.view.activity.OfferActivity", false);
            menuItems.add(item);
            item = new Menu((long)10, 9, 9, 0, "实验课表", "cn.nicolite.huthelper.view.activity.ExpLessonActivity", true);
            menuItems.add(item);
            item = new Menu((long)11, 10, 10, 0, "校历", "cn.nicolite.huthelper.view.activity.CalendarActivity", false);
            menuItems.add(item);
            item = new Menu((long)12, 11, 11, 0, "失物招领", "cn.nicolite.huthelper.view.activity.LoseListActivity", true);
            menuItems.add(item);
            item = new Menu((long)13, 12, 12, 0, "宣讲会", "cn.nicolite.huthelper.view.activity.CareerTalkActivity", true);
            menuItems.add(item);
            item = new Menu((long)14, 13, 13, 0, "全部", "cn.nicolite.huthelper.view.activity.AllActivity", true);
            menuItems.add(item);
            item = new Menu((long)15, 14, 14, 0, "视频专栏", "cn.nicolite.huthelper.view.activity.VideoListActivity", false);
            menuItems.add(item);
            item = new Menu((long)16, 15, 15, 0, "新生攻略", "cn.nicolite.huthelper.view.activity.FreshmanHelpActivity", false);
            menuItems.add(item);

            for (Menu menu : menuItems) {
                menu.setUserId(getLoginUser());
                List<Menu> list = menuDao.queryBuilder().where(MenuDao.Properties.Id.eq(menu.getId())).list();
               if (!ListUtils.isEmpty(list)) {
                   menuDao.update(menu);
                   continue;
               }
                menuDao.insert(menu);
            }
        }

        List<Menu> menuList = menuDao.queryBuilder()
                .where(MenuDao.Properties.UserId.eq(getLoginUser()), MenuDao.Properties.IsMain.eq(true))
                .orderAsc(MenuDao.Properties.Index)
                .list();

        if (getView() != null) {
            getView().showMenu(menuList);
        }

    }

    //必须调用此接口以记录该用户是否使用工大助手
    public void checkUpdate(String num) {
        try {
            int versionCode = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
            APIUtils
                    .getUpdateAPI()
                    .checkUpdate(num, versionCode)
                    .compose(getActivity().<HttpResult<Update>>bindToLifecycle())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<HttpResult<Update>>() {
                        @Override
                        public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {

                        }

                        @Override
                        public void onNext(@io.reactivex.annotations.NonNull HttpResult<Update> updateHttpResult) {

                        }

                        @Override
                        public void onError(@io.reactivex.annotations.NonNull Throwable e) {

                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void connectRongIM() {
        String userId = getLoginUser();
        if (TextUtils.isEmpty(userId)) {
            getView().showMessage("获取当前登录用户失败，请重新登录！");
            return;
        }

        List<Configure> configureList = getConfigureList();

        final Configure configure;
        if (ListUtils.isEmpty(configureList)) {
            getView().showMessage("获取Token失败，请重新登录！");
            return;
        }
        configure = configureList.get(0);
        final User user = configure.getUser();

        RongIM.connect(configure.getToken(), new RongIMClient.ConnectCallback() {
            @Override
            public void onTokenIncorrect() {
                getView().showMessage("Token不正确，请重新登录！");
            }

            @Override
            public void onSuccess(String s) {
                if (user == null) {
                    getView().showMessage("未获取到用户信息，请重新登录！");
                    return;
                }
                RongIM.getInstance()
                        .setCurrentUserInfo(new UserInfo(user.getUser_id(), user.getTrueName(),
                                Uri.parse(Constants.PICTURE_URL + user.getHead_pic_thumb())));
                RongIM.getInstance().setMessageAttachedUserInfo(true);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                getView().showMessage("连接即时聊天服务器出错！");
            }
        });
    }

    public void initPush(String studentKH) {
        XGPushManager.registerPush(getActivity().getApplicationContext(), studentKH, new XGIOperateCallback() {
            @Override
            public void onSuccess(Object o, int i) {
                LogUtils.d(TAG, "注册成功，设备token为：" + o);
            }

            @Override
            public void onFail(Object o, int i, String s) {
                LogUtils.d(TAG, "注册失败，错误码：" + i + "，错误信息：" + s);
                getView().showMessage("注册推送服务失败：" + s);
            }
        });
        XGPushConfig.enableDebug(getActivity().getApplicationContext(), BuildConfig.LOG_DEBUG);
    }

    public void initNotice() {

    }

    public void initUser() {
        String userId = getLoginUser();
        if (TextUtils.isEmpty(userId)) {
            getView().showMessage("获取当前登录用户失败，请重新登录！");
            return;
        }
        List<Configure> configureList = getConfigureList();
        if (ListUtils.isEmpty(configureList)){
            getView().showMessage("获取用户信息失败！");
            return;
        }

        User user = configureList.get(0).getUser();

         if (user != null){
             getView().showUser(user);
         }
    }

    public void share() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
        intent.putExtra(Intent.EXTRA_TEXT, "工大助手下载连接：https://www.coolapk.com/apk/cn.nicolite.huthelper");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(Intent.createChooser(intent, "分享"));
    }

    public void startChat() {
        Map<String, Boolean> supportedConversation = new HashMap<>();
        supportedConversation.put(Conversation.ConversationType.PRIVATE.getName(), false);
        RongIM.getInstance().startConversationList(getActivity(), supportedConversation);
    }

    public void checkPermission() {
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
        };
        AndPermission
                .with(getActivity())
                .requestCode(200)
                .permission(permissions)
                .callback(new PermissionListener() {
                    @Override
                    public void onSucceed(int requestCode, @android.support.annotation.NonNull List<String> grantPermissions) {

                    }

                    @Override
                    public void onFailed(int requestCode, @android.support.annotation.NonNull List<String> deniedPermissions) {
                        getView().showMessage("获取权限失败，请授予文件读写和读取手机状态权限！");
                    }
                })
                .start();
    }
}