package com.java.zhangbinwei;


import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.java.zhangbinwei.adpter.NewsListAdapter;
import com.java.zhangbinwei.db.HistoryDbHelper;
import com.java.zhangbinwei.entity.HistoryInfo;
//import com.java.zhangbinwei.adpter.NewsListAdapter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
//import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TabNewsFragment extends Fragment {

    //private String url = "https://api2.newsminer.net/svc/news/queryNewsList?size=15&startDate=2024-06-20&endDate=2024-08-30&words=拜登&categories=";
    //添加下拉刷新相关成员变量
    private TextView tvRefreshHint;
    // 添加变量控制是否自动刷新
    private boolean shouldAutoRefresh = true;
    // 添加变量控制首次加载
    private boolean isFirstLoad = true;



    // 添加底部提示变量
    private TextView tvLoadMoreHint;
    // 添加变量保存滚动位置
    // 在 TabNewsFragment 中添加
    private int scrollOffset = 0;
    private int lastScrollPosition = -1;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private ProgressBar progressBar; // 用于加载更多的进度条

    // 添加保存滚动位置的变量
    private Parcelable recyclerViewState;
    private LinearLayoutManager layoutManager;


    //添加搜索相关的成员变量
    private String size="";
    private String keywords="";
    private String category="";
    private String startDate="";
    private String endDate="";

    // 添加统一的日志标签
    private static final String TAG = "TabNewsFragment";

    private View rootView;
    private RecyclerView recyclerView;

    private NewsListAdapter mNewsListAdapter;

    private static final String ARG_PARAM = "title";
    private String title;

    public void saveScrollState(LinearLayoutManager layoutManager) {
        this.lastScrollPosition = layoutManager.findFirstVisibleItemPosition();
        View firstView = layoutManager.findViewByPosition(lastScrollPosition);
        if (firstView != null) {
            this.scrollOffset = firstView.getTop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        shouldAutoRefresh = false; // 返回时禁止自动刷新
        // 更新已读状态（不触发全局刷新）
        updateReadStatus();
        // 检查是否需要刷新数据
        if (getArguments() != null && getArguments().getBoolean("needsRefresh", false)) {
            getArguments().remove("needsRefresh");
            refreshData();
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 确保只加载一次数据
        if (savedInstanceState == null) {
            // 只在首次创建视图时加载数据
            getHttpData(currentPage);
        } else {
            // 恢复状态后更新已读状态
            updateReadStatus();
        }
    }

    private void updateReadStatus() {
        // 直接在主线程更新，避免异步延迟
        if (getActivity() != null) {
            Set<String> readNewsIds = getReadNewsIds();
            if (mNewsListAdapter != null) {
                mNewsListAdapter.setReadNews(readNewsIds);
            }
        }
    }
    private void restoreScrollPosition() {
        if (lastScrollPosition >= 0 && lastScrollPosition < mNewsListAdapter.getItemCount()) {
            // 延迟执行确保布局完成
            new Handler().postDelayed(() -> {
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(lastScrollPosition, scrollOffset);
                }
            }, 50);
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            // 检查 Fragment 是否附加到 Activity
            if (!isAdded() || getActivity() == null) {
                return;
            }
            swipeRefreshLayout.setRefreshing(false);
            isLoading = false;

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            //super.handleMessage(msg);
            if (msg.what == 100) {
                // 获取已读新闻ID
                ArrayList<String> readNewsList = msg.getData().getStringArrayList("readNewsIds");
                Set<String> readNewsIds = new HashSet<>(readNewsList);

                // 设置已读新闻
                mNewsListAdapter.setReadNews(readNewsIds);
                String data = (String) msg.obj;
                Log.d(TAG, "API Response: " + data); // 添加响应日志

                try {
                    NewsInfo newsInfo = new Gson().fromJson(data, NewsInfo.class);
                    if (newsInfo != null && newsInfo.getData() != null && !newsInfo.getData().isEmpty()) {
                        //逻辑处理
                        // 判断是否有更多数据（根据返回的数据量）
                        hasMore = newsInfo.getData().size() >= 20; // 假设每页20条
                        if (currentPage == 1) {
                            // 下拉刷新：替换整个列表
                            mNewsListAdapter.setListData(newsInfo.getData());
                            Toast.makeText(getActivity(), "已刷新最新新闻", Toast.LENGTH_SHORT).show();
                            // 滚动到顶部
                            if(shouldAutoRefresh) {
                                recyclerView.scrollToPosition(0);
                            }
                            else {
                                // 恢复滚动位置
                                restoreScrollPosition();
                            }
                        } else {
                            // 上拉加载更多：追加到列表
                            mNewsListAdapter.appendListData(newsInfo.getData());
                            Toast.makeText(getActivity(), "已加载更多新闻", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (currentPage > 1) {
                            currentPage--; // 恢复页码
                            hasMore = false;
                            Toast.makeText(getActivity(), "没有更多新闻了>_<", Toast.LENGTH_SHORT).show();
                        }else {
                            // 处理第一页无数据的情况
                            mNewsListAdapter.setListData(new ArrayList<>());
                            Toast.makeText(getActivity(), "没有找到相关新闻>_<", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "JSON解析错误: " + e.getMessage());
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(getActivity(), "数据解析错误", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            // 恢复滚动位置
            /*if (recyclerViewState != null && layoutManager != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
                recyclerViewState = null; // 重置状态
            }*/
        }
    };

    public TabNewsFragment() {
        Log.d("-------------------", "TabNewsFragment constructor called");
    }


    public static TabNewsFragment newInstance(String param) {
        TabNewsFragment fragment = new TabNewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM, param);
        fragment.setArguments(args);
        return fragment;
    }

    // 添加新的静态方法用于搜索
    public static TabNewsFragment newSearchInstance(String keywords, String category,
                                                    String startDate, String endDate) {
        TabNewsFragment fragment = new TabNewsFragment();
        Bundle args = new Bundle();
        args.putString("keywords", keywords);
        args.putString("category", category);
        args.putString("startDate", startDate);
        args.putString("endDate", endDate);
        args.putBoolean("isSearch", true); // 标记为搜索模式
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.getBoolean("isSearch", false)) {
            // 搜索模式：使用传入的参数
            keywords = args.getString("keywords", "");
            category = args.getString("category", "");
            startDate = args.getString("startDate", "");
            endDate = args.getString("endDate", "");
        } else {
            // 原有模式：从Tab获取分类
            if (getArguments() != null) {
                category = getArguments().getString(ARG_PARAM);
            }
        }
    }

    // 添加辅助方法判断是否搜索模式
    private boolean isSearchMode() {
        Bundle args = getArguments();
        return args != null && args.getBoolean("isSearch", false);
    }


    // 保存滚动位置
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存 RecyclerView 的滚动位置
        if (layoutManager != null) {
            recyclerViewState = layoutManager.onSaveInstanceState();
            outState.putParcelable("recyclerViewState", recyclerViewState);
        }
        // 保存是否首次加载状态
        outState.putBoolean("isFirstLoad", isFirstLoad);
    }
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            // 恢复滚动位置
            recyclerViewState = savedInstanceState.getParcelable("recyclerViewState");
            if (recyclerViewState != null && layoutManager != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
            }
            // 恢复首次加载状态
            isFirstLoad = savedInstanceState.getBoolean("isFirstLoad", true);
        }
    }
    // 刷新数据
    private void refreshData() {
        // 用户主动刷新时设置标志
        shouldAutoRefresh = true;
        // 保存当前位置
        if (layoutManager != null) {
            recyclerViewState = layoutManager.onSaveInstanceState();
        }
        currentPage=1;
        hasMore = true;
        // 重置时间范围以获取最新新闻
        if (!isSearchMode()) {
            // 非搜索模式下，重置为当前日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            endDate = sdf.format(new Date());
        }
        getHttpData(currentPage);
    }

    // 加载更多数据
    private void loadMoreData() {
        currentPage++;
        getHttpData(currentPage);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("-------------", "onCreateView called");
        rootView = inflater.inflate(R.layout.fragment_tab_news, container, false);
        // 初始化下拉刷新控件
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        // 设置下拉刷新颜色
        swipeRefreshLayout.setColorSchemeResources(
                R.color.purple_200,
                R.color.purple_500,
                R.color.purple_700);

        //初始化RecyclerView控件
        recyclerView = rootView.findViewById(R.id.recyclerView);
        //初始化适配器
        mNewsListAdapter = new NewsListAdapter(getActivity());
        //设置适配器
        recyclerView.setAdapter(mNewsListAdapter);
        progressBar = rootView.findViewById(R.id.progressBar); // 直接查找布局中的进度条
        // 添加加载更多进度条
        //View footer = LayoutInflater.from(getContext()).inflate(R.layout.loading_footer, recyclerView, false);
        //progressBar = footer.findViewById(R.id.progressBar);
        //progressBar.setVisibility(View.GONE); // 初始隐藏
        //mNewsListAdapter.addFooterView(footer);

        // 初始化提示文本
        tvRefreshHint = rootView.findViewById(R.id.tvRefreshHint);
        // 初始化底部提示
        tvLoadMoreHint = rootView.findViewById(R.id.tvLoadMoreHint);
        recyclerView.addItemDecoration(new DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
        ));
        // 初始化布局管理器
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);

        // 恢复滚动位置（如果有）
        if (savedInstanceState != null) {
            recyclerViewState = savedInstanceState.getParcelable("recyclerViewState");
            if (recyclerViewState != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
            }
        }

        // 设置滚动监听器用于加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();

                // 当滚动到顶部时显示提示
                if (firstVisibleItemPosition == 0) {
                    // 检查第一个item是否完全可见
                    View firstItem = recyclerView.getChildAt(0);
                    if (firstItem != null) {
                        boolean isAtTop = layoutManager.getDecoratedTop(firstItem) >= recyclerView.getPaddingTop();
                        tvRefreshHint.setVisibility(isAtTop ? View.VISIBLE : View.GONE);
                    } else {
                        tvRefreshHint.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvRefreshHint.setVisibility(View.GONE);
                }

                // 显示/隐藏底部提示
                if (lastVisibleItemPosition >= totalItemCount - 2) {
                    tvLoadMoreHint.setVisibility(View.VISIBLE);
                } else {
                    tvLoadMoreHint.setVisibility(View.GONE);
                }

                // 判断是否需要加载更多
                if (!isLoading && hasMore) {
                    // 滚动到底部时加载更多
                    if (lastVisibleItemPosition >= totalItemCount - 1){
                        loadMoreData();
                    }
                }
            }
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });



        // 添加 SwipeRefreshLayout 的刷新位置限制
        swipeRefreshLayout.setProgressViewOffset(true, 0, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics()));
        // 刷新数据
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG,"DiaoYong!!");
        super.onActivityCreated(savedInstanceState);

        //初始化适配器
        //mNewsListAdapter = new NewsListAdapter(getActivity());

        //设置适配器
        //recyclerView.setAdapter(mNewsListAdapter);

        //点击事件
        mNewsListAdapter.setmOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NewsInfo.DataDTO dataDTO , int position) {
                // 保存点击位置
                View firstVisibleItem = recyclerView.getChildAt(0);
                scrollOffset = firstVisibleItem != null ? firstVisibleItem.getTop() : 0;
                lastScrollPosition = position;

                //跳转到详情页
                Intent intent = new Intent(getActivity(), NewsDetailsActivity.class);
                //传递对象的时候，该类一定要实现Serializable接口
                intent.putExtra("dataDTO", dataDTO);
                // 使用 startActivityForResult 以便接收返回结果
                startActivityForResult(intent, 1);
            }
        });
        // 只在首次加载数据
        if (isFirstLoad) {
            getHttpData(currentPage);
            isFirstLoad = false;
        }
    }

    private void getHttpData(int page) {
        if (isLoading) return;
        isLoading = true;
        if (page > 1 && progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // 获取当前日期作为默认结束日期
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // 构建动态URL
        StringBuilder urlBuilder = new StringBuilder("https://api2.newsminer.net/svc/news/queryNewsList?");
        if (!TextUtils.isEmpty(size)) {
            urlBuilder.append("size=").append(size);
        }else{
            urlBuilder.append("size=").append("20");
        }

        // 添加时间范围参数
        if (!TextUtils.isEmpty(startDate)) {
            urlBuilder.append("&startDate=").append(startDate);
        }else{
            urlBuilder.append("&startDate=").append("1970-01-01");
        }
        if (!TextUtils.isEmpty(endDate)) {
            urlBuilder.append("&endDate=").append(endDate);
        }else{
            urlBuilder.append("&endDate=").append(currentDate);
        }

        // 添加关键词参数
        if (!TextUtils.isEmpty(keywords)) {
            try {
                // 对关键词进行URL编码
                String encodedKeywords = URLEncoder.encode(keywords, "UTF-8");
                urlBuilder.append("&words=").append(encodedKeywords);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                urlBuilder.append("&words=").append(keywords);
            }
        }else{
            urlBuilder.append("&words=").append("");
        }

        // 添加分类参数
        if (!TextUtils.isEmpty(category) && !"全部".equals(category)) {
            urlBuilder.append("&categories=").append(category);
        }else{
            urlBuilder.append("&categories=").append("");
        }


        urlBuilder.append("&page=").append(page);
        String finalUrl = urlBuilder.toString();

        Log.d(TAG, "Request URL: " + finalUrl); // 添加URL日志


        //创建OkHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        //创建Request对象
        Request request = new Request.Builder()
                .url(finalUrl)
                .get()
                .build();
        //通过OkHttpClient对象和Request对象创建Call对象
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "onFailure: " + e.toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                String data = response.body().string();
                // 获取已读新闻ID
                Set<String> readNewsIds = getReadNewsIds();
                //Log.d(TAG, "onResponse: " + data);
                Message message = new Message();
                message.what = 100;
                message.obj = data;
                // 传递已读新闻ID
                Bundle bundle = new Bundle();
                // 将 Set 转换为 ArrayList
                bundle.putStringArrayList("readNewsIds", new ArrayList<>(readNewsIds));
                message.setData(bundle);
                //发送消息
                mHandler.sendMessage(message);

            }
        });
    }
    // 获取已读新闻ID集合
    private Set<String> getReadNewsIds() {
        // 从数据库获取已读新闻ID
        Set<String> readNewsIds = new HashSet<>();
        if (getActivity() != null) {
            List<HistoryInfo> historyList = HistoryDbHelper.getInstance(getActivity()).queryHistoryListData(null);
            for (HistoryInfo history : historyList) {
                readNewsIds.add(history.getUniquekey());
            }
        }
        return readNewsIds;
    }
    // 添加 onActivityResult 方法
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {
            // 获取已读新闻ID
            String readNewsId = data.getStringExtra("read_news_id");
            if (readNewsId != null && mNewsListAdapter != null) {
                // 查找并更新对应新闻项
                List<NewsInfo.DataDTO> dataList = mNewsListAdapter.getDataList();
                for (int i = 0; i < dataList.size(); i++) {
                    if (dataList.get(i).getNewsID().equals(readNewsId)) {
                        // 更新适配器的已读集合
                        mNewsListAdapter.setItemRead(i, readNewsId);
                        break;
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            shouldAutoRefresh = false;
        }
    }
}