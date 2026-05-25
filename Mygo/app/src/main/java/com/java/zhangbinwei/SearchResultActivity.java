package com.java.zhangbinwei;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class SearchResultActivity extends AppCompatActivity {

    private static final String EXTRA_KEYWORDS = "keywords";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_START_DATE = "start_date";
    private static final String EXTRA_END_DATE = "end_date";

    public static void start(Context context, String keywords, String category,
                             String startDate, String endDate) {
        Intent intent = new Intent(context, SearchResultActivity.class);
        intent.putExtra(EXTRA_KEYWORDS, keywords);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_START_DATE, startDate);
        intent.putExtra(EXTRA_END_DATE, endDate);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        // 获取搜索参数
        Intent intent = getIntent();
        String keywords = intent.getStringExtra(EXTRA_KEYWORDS);
        String category = intent.getStringExtra(EXTRA_CATEGORY);
        String startDate = intent.getStringExtra(EXTRA_START_DATE);
        String endDate = intent.getStringExtra(EXTRA_END_DATE);

        // 创建并添加Fragment
        Fragment fragment = TabNewsFragment.newSearchInstance(
                keywords, category, startDate, endDate);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

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