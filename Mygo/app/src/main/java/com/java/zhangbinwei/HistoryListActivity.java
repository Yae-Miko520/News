package com.java.zhangbinwei;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;

import com.google.gson.Gson;
import com.java.zhangbinwei.adpter.NewsListAdapter;
import com.java.zhangbinwei.db.HistoryDbHelper;
import com.java.zhangbinwei.entity.HistoryInfo;

import java.util.ArrayList;
import java.util.List;

public class HistoryListActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerView;

    private NewsListAdapter mNewsListAdapter;

    private List<NewsInfo.DataDTO> mDataDTOList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history_list);
        //初始化控件
        recyclerView = findViewById(R.id.history_list);
        //初始化适配器
        mNewsListAdapter = new NewsListAdapter(this);
        //设置适配器
        recyclerView.setAdapter(mNewsListAdapter);

        //获取数据
        List<HistoryInfo> historyInfoList = HistoryDbHelper.getInstance((HistoryListActivity.this)).queryHistoryListData(null);

        Gson gson = new Gson();
        for (HistoryInfo historyInfo : historyInfoList) {
            mDataDTOList.add(gson.fromJson(historyInfo.getNew_json(), NewsInfo.DataDTO.class));
        }


        //设置数据
        mNewsListAdapter.setListData(mDataDTOList);

        //设置点击事件
        mNewsListAdapter.setmOnItemClickListener(new NewsListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NewsInfo.DataDTO dataDTO,int position) {
                //跳转到详情页
                Intent intent = new Intent(HistoryListActivity.this, NewsDetailsActivity.class);
                //传递对象的时候，该类一定要实现Serializable接口
                intent.putExtra("dataDTO", dataDTO);
                startActivity(intent);
            }
        });

        //返回
        findViewById(R.id.toolbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 使用系统返回行为，支持动画
                onBackPressed();
            }
        });
    }
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }
}