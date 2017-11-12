package cn.nicolite.huthelper.presenter;

import android.text.TextUtils;

import java.util.List;

import cn.nicolite.huthelper.base.presenter.BasePresenter;
import cn.nicolite.huthelper.model.bean.Configure;
import cn.nicolite.huthelper.model.bean.GoodsResult;
import cn.nicolite.huthelper.model.bean.LostAndFound;
import cn.nicolite.huthelper.model.bean.User;
import cn.nicolite.huthelper.network.api.APIUtils;
import cn.nicolite.huthelper.network.exception.ExceptionEngine;
import cn.nicolite.huthelper.utils.ListUtils;
import cn.nicolite.huthelper.view.fragment.LostAndFoundFragment;
import cn.nicolite.huthelper.view.iview.ILostAndFoundView;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by nicolite on 17-11-12.
 */

public class LostAndFoundPresenter extends BasePresenter<ILostAndFoundView, LostAndFoundFragment> {
    public LostAndFoundPresenter(ILostAndFoundView view, LostAndFoundFragment activity) {
        super(view, activity);
    }


    public void showLostAndFoundList(int type, boolean isManual) {
        switch (type) {
            case LostAndFoundFragment.ALL:
                loadMoreAll(1, isManual, false);
                break;
            case LostAndFoundFragment.FOUND:
                loadMoreFound(1, isManual, false);
                break;
            case LostAndFoundFragment.LOST:
                loadMoreLost(1, isManual, false);
                break;
        }
    }

    public void loadMore(int page, int type) {
        switch (type) {
            case LostAndFoundFragment.ALL:
                loadMoreAll(page, true, true);
                break;
            case LostAndFoundFragment.FOUND:
                loadMoreFound(page, true, true);
                break;
            case LostAndFoundFragment.LOST:
                loadMoreLost(page, true, true);
                break;
        }
    }

    public void loadMoreAll(int page, boolean isManual, boolean isLoadMore) {
        loadLostAndFoundList(page, 0, isManual, isLoadMore);
    }

    public void loadMoreFound(int page, boolean isManual, boolean isLoadMore) {
        loadLostAndFoundList(page, 1, isManual, isLoadMore);
    }

    public void loadMoreLost(int page, boolean isManual, boolean isLoadMore) {
        loadLostAndFoundList(page, 2, isManual, isLoadMore);
    }

    public void loadLostAndFoundList(final int page, int type, final boolean isManual, final boolean isLoadMore) {
        APIUtils
                .getLostAndFoundAPI()
                .getLostAndFoundList(page, type)
                .compose(getActivity().<GoodsResult<List<LostAndFound>>>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GoodsResult<List<LostAndFound>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (getView() != null && !isManual) {
                            getView().showLoading();
                        }
                    }

                    @Override
                    public void onNext(GoodsResult<List<LostAndFound>> listGoodsResult) {
                        if (getView() != null) {
                            getView().closeLoading();
                            if (listGoodsResult.getCode() == 200) {
                                if (isLoadMore) {
                                    if (page <= listGoodsResult.getPageination()) {
                                        getView().showLoadMoreList(listGoodsResult.getData());
                                    } else {
                                        getView().noMoreData();
                                    }
                                } else {
                                    getView().showLostAndFoundList(listGoodsResult.getData());
                                    if (ListUtils.isEmpty(listGoodsResult.getData())) {
                                        getView().showMessage("暂时没有相关内容！");
                                    }
                                }
                            } else {
                                getView().showMessage("获取数据失败，" + listGoodsResult.getCode());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (getView() != null) {
                            getView().closeLoading();
                            getView().loadMoreFailure();
                            getView().showMessage(ExceptionEngine.handleException(e).getMsg());
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    public void searchLostAndFound(String searchText, final int page, final boolean isLoadMore) {
        if (TextUtils.isEmpty(searchText)) {
            searchText = "";
        }

        if (TextUtils.isEmpty(userId)) {
            getView().showMessage("获取用户信息失败！");
            return;
        }
        List<Configure> configureList = getConfigureList();

        if (ListUtils.isEmpty(configureList)) {
            getView().showMessage("获取用户信息失败！");
            return;
        }

        Configure configure = configureList.get(0);
        User user = configure.getUser();

        APIUtils
                .getLostAndFoundAPI()
                .searchLostAndFound(user.getStudentKH(), configure.getAppRememberCode(), page, searchText)
                .compose(getActivity().<GoodsResult<List<LostAndFound>>>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GoodsResult<List<LostAndFound>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (getView() != null) {
                            getView().showLoading();
                        }
                    }

                    @Override
                    public void onNext(GoodsResult<List<LostAndFound>> listGoodsResult) {
                        if (getView() != null) {
                            getView().closeLoading();
                            if (listGoodsResult.getCode() == 200) {
                                if (isLoadMore) {
                                    if (page <= listGoodsResult.getPageination()) {
                                        getView().showLoadMoreList(listGoodsResult.getData());
                                    } else {
                                        getView().noMoreData();
                                    }
                                } else {
                                    getView().showLostAndFoundList(listGoodsResult.getData());
                                    if (ListUtils.isEmpty(listGoodsResult.getData())) {
                                        getView().showMessage("暂时没有相关内容！");
                                    }
                                }
                            } else {
                                getView().showMessage("获取数据失败，" + listGoodsResult.getCode());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
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

    public void showLostAndFoundByUserId(final int page, String userId, final boolean isLoadMore) {
        if (TextUtils.isEmpty(userId)) {
            getView().showMessage("获取用户信息失败！");
            return;
        }
        List<Configure> configureList = getConfigureList();

        if (ListUtils.isEmpty(configureList)) {
            getView().showMessage("获取用户信息失败！");
            return;
        }

        Configure configure = configureList.get(0);
        User user = configure.getUser();

        APIUtils
                .getLostAndFoundAPI()
                .getLostAndFoundListByUserId(user.getStudentKH(), configure.getAppRememberCode(), page, userId)
                .compose(getActivity().<GoodsResult<List<LostAndFound>>>bindToLifecycle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GoodsResult<List<LostAndFound>>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        if (getView() != null) {
                            getView().showLoading();
                        }
                    }

                    @Override
                    public void onNext(GoodsResult<List<LostAndFound>> listGoodsResult) {
                        if (getView() != null) {
                            getView().closeLoading();
                            if (listGoodsResult.getCode() == 200) {
                                if (isLoadMore) {
                                    if (page <= listGoodsResult.getPageination()) {
                                        getView().showLoadMoreList(listGoodsResult.getData());
                                    } else {
                                        getView().noMoreData();
                                    }
                                } else {
                                    getView().showLostAndFoundList(listGoodsResult.getData());
                                    if (ListUtils.isEmpty(listGoodsResult.getData())) {
                                        getView().showMessage("暂时没有相关内容！");
                                    }
                                }
                            } else {
                                getView().showMessage("获取数据失败，" + listGoodsResult.getCode());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
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
}