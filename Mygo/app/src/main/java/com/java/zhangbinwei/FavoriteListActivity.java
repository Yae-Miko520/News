package com.java.zhangbinwei;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.java.zhangbinwei.adpter.NewsListAdapter;
import com.java.zhangbinwei.db.FavoriteDbHelper;
import com.java.zhangbinwei.entity.FavoriteInfo;

import java.util.ArrayList;
import java.util.List;

public class FavoriteListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NewsListAdapter mNewsListAdapter;
    private List<NewsInfo.DataDTO> mDataDTOList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_list);

        recyclerView = findViewById(R.id.favorite_list);
        mNewsListAdapter = new NewsListAdapter(this);
        recyclerView.setAdapter(mNewsListAdapter);

        loadFavorites();

        // 返回按钮
        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<FavoriteInfo> favoriteInfoList =
                    FavoriteDbHelper.getInstance(this).queryFavoriteListData(null);

            Gson gson = new Gson();
            for (FavoriteInfo favoriteInfo : favoriteInfoList) {
                mDataDTOList.add(gson.fromJson(favoriteInfo.getNew_json(), NewsInfo.DataDTO.class));
            }

            runOnUiThread(() -> {
                mNewsListAdapter.setListData(mDataDTOList);
                setupClickListener();
            });
        }).start();
    }

    private void setupClickListener() {
        mNewsListAdapter.setmOnItemClickListener((dataDTO, position) -> {
            Intent intent = new Intent(FavoriteListActivity.this, NewsDetailsActivity.class);
            intent.putExtra("dataDTO", dataDTO);
            // 添加位置信息以便更新
            intent.putExtra("position", position);
            // 使用 startActivityForResult 启动详情页
            startActivityForResult(intent, 1);
        });
    }
    // 添加 onActivityResult 方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // 获取被移除的新闻位置
            int removedPosition = data.getIntExtra("removed_position", -1);
            if (removedPosition != -1 && removedPosition < mDataDTOList.size()) {
                // 从列表中移除该项
                mDataDTOList.remove(removedPosition);
                // 通知适配器更新
                mNewsListAdapter.notifyItemRemoved(removedPosition);
            }
        }
        // 在 FavoriteListActivity.java 的 onActivityResult 中：
        String removedId = data.getStringExtra("removed_id");
        if (removedId != null) {
            for (int i = 0; i < mDataDTOList.size(); i++) {
                if (mDataDTOList.get(i).getNewsID().equals(removedId)) {
                    mDataDTOList.remove(i);
                    mNewsListAdapter.notifyItemRemoved(i);
                    break;
                }
            }
        }
    }
}