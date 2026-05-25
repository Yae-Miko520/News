package com.java.zhangbinwei;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar; // 添加这行
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.java.zhangbinwei.databinding.ActivityMainBinding;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //private String[] titles = {"娱乐","军事","教育","文化","健康","财经","体育","汽车","科技","社会"};

    private DrawerLayout drawerLayout; // 声明DrawerLayout
    private ActionBarDrawerToggle drawerToggle; // 声明DrawerToggle
    private NavigationView nav_view;
    private List<TitleInfo> titles = new ArrayList<TitleInfo>();
    private TabLayout tab_layout;
    private ViewPager2 viewPager;
    // 添加当前选中分类的索引
    private int currentTabPosition = 0;
    // 添加分类管理返回时的刷新逻辑
    private static final int REQUEST_CODE_MANAGE_CATEGORIES = 100;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MANAGE_CATEGORIES && resultCode == RESULT_OK) {
            refreshCategories();
        }
    }
    private void refreshCategories() {
        // 重新加载分类
        loadCategories();

        // 保存当前选中的位置
        int currentPosition = viewPager.getCurrentItem();

        // 重新设置适配器
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                String category = titles.get(position).getTitle();
                return TabNewsFragment.newInstance(category);
            }

            @Override
            public int getItemCount() {
                return titles.size();
            }
        });

        // 重新设置 TabLayout
        new TabLayoutMediator(tab_layout, viewPager,
                (tab, position) -> tab.setText(titles.get(position).getTitle())
        ).attach();

        // 恢复之前的位置
        viewPager.setCurrentItem(currentPosition, false);
    }

    // 当从分类管理返回时刷新数据
    @Override
    protected void onResume() {
        super.onResume();
        // 确保显示正确的分类
        if (viewPager != null) {
            viewPager.setCurrentItem(currentTabPosition, false);
        }
        /*//重新加载分类
        loadCategories();
        // 重新设置适配器
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                String category = titles.get(position).getTitle();
                return TabNewsFragment.newInstance(category);
            }

            @Override
            public int getItemCount() {
                return titles.size();
            }
        });


        // 重新设置TabLayout
        new TabLayoutMediator(tab_layout, viewPager,
                (tab, position) -> tab.setText(titles.get(position).getTitle())
        ).attach();
        // 确保在onResume时显示正确的分类
        viewPager.setCurrentItem(currentTabPosition, false);*/
    }


    private void loadCategories() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String json = prefs.getString("categories", null);

        // 清除现有分类
        titles.clear();
        // 首先添加"全部"分类
        titles.add(new TitleInfo("全部"));


        if (json != null) {
            Type type = new TypeToken<Set<String>>() {}.getType();
            Set<String> savedCategories = new Gson().fromJson(json, type);
            if (savedCategories != null) {
                for (String name : savedCategories) {
                    titles.add(new TitleInfo(name));
                }
            }
        } else {
            // 只添加一次默认分类
            String[] defaultCategories = {"娱乐", "军事", "教育", "文化", "健康", "财经", "体育", "汽车", "科技", "社会"};
            for (String category : defaultCategories) {
                titles.add(new TitleInfo(category));
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 1. 初始化DrawerLayout和Toolbar
        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar); // 获取Toolbar

        // 2. 设置Toolbar为ActionBar
        setSupportActionBar(toolbar);

        // 3. 创建ActionBarDrawerToggle并关联DrawerLayout和Toolbar
        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar, // 关联Toolbar
                R.string.navigation_drawer_open, // 打开描述 (需在strings.xml定义)
                R.string.navigation_drawer_close // 关闭描述 (需在strings.xml定义)
        );

        // 4. 添加DrawerLayout监听器
        drawerLayout.addDrawerListener(drawerToggle);

        // 5. 同步Toggle状态
        drawerToggle.syncState();
        // 6. 设置Toolbar导航图标点击事件 (可选，Toggle已自动处理)
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(nav_view)) {
                drawerLayout.closeDrawer(nav_view);
            } else {
                drawerLayout.openDrawer(nav_view);
            }
        });

        //初始化数据
        loadCategories();
        //初始化控件
        tab_layout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.viewPager);
        nav_view = findViewById(R.id.nav_view);
        // 添加这行代码：设置离屏页面限制，防止Fragment被销毁
        viewPager.setOffscreenPageLimit(10); // 设置足够大的值，确保所有标签页都保留在内存中
        Log.d("MainActivity", "onCreate: " + titles.size());

        //搜索点击事件
        View searchContainer = findViewById(R.id.search_container);
        searchContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到搜索界面
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });



        //nav_view设置点击事件
        nav_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_history) {
                    //跳转到历史记录
                    Intent intent = new Intent(MainActivity.this, HistoryListActivity.class);
                    startActivity(intent);
                }else if (item.getItemId() == R.id.nav_manage_categories) {
                    // 跳转到分类管理
                    Intent intent = new Intent(MainActivity.this, CategoryManageActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_MANAGE_CATEGORIES);
                }else if (item.getItemId() == R.id.nav_favorites) {  // 处理收藏列表点击
                    Intent intent = new Intent(MainActivity.this, FavoriteListActivity.class);
                    startActivity(intent);
                }
                return true;
            }
        });



        //viewPager2设置适配器
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                String categories = titles.get(position).getTitle();
                Log.d("MainActivity", "createFragment: " + categories);
                TabNewsFragment tabNewsFragment = TabNewsFragment.newInstance(categories);
                return tabNewsFragment;
            }

            @Override
            // This method returns the number of items in the list
            public int getItemCount() {
                // Return the size of the titles list
                return titles.size();
            }
        });

        //tab_layout点击事件
        tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //设置viewPager选中当前页
                viewPager.setCurrentItem(tab.getPosition(),false);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        //tab_layout设置viewPager2
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tab_layout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(titles.get(position).getTitle());
            }
        });
        tabLayoutMediator.attach();

        // 设置页面切换监听
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentTabPosition = position; // 保存当前选中的分类
            }
        });

        // 恢复之前的状态（如果有）
        if (savedInstanceState != null) {
            currentTabPosition = savedInstanceState.getInt("currentTabPosition", 0);
            viewPager.setCurrentItem(currentTabPosition, false);
        }
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentTabPosition", currentTabPosition);
    }

}
