package com.java.zhangbinwei;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.java.zhangbinwei.adpter.CategoryAdapter;
import com.java.zhangbinwei.helper.CrossListTouchCallback;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryManageActivity extends AppCompatActivity
        implements CategoryAdapter.OnItemActionListener {

    private RecyclerView recyclerSelected;
    private RecyclerView recyclerAvailable;
    private Button btnSaveCategories;

    private CategoryAdapter selectedAdapter;
    private CategoryAdapter availableAdapter;

    private final List<String> fixedCategories = Arrays.asList(
            "娱乐", "军事", "教育", "文化", "健康", "财经", "体育", "汽车", "科技", "社会"
    );

    private List<String> selectedCategories = new ArrayList<>();
    private List<String> availableCategories = new ArrayList<>();

    private ItemTouchHelper selectedTouchHelper;
    private ItemTouchHelper availableTouchHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_manage);

        recyclerSelected = findViewById(R.id.recycler_selected);
        recyclerAvailable = findViewById(R.id.recycler_available);
        btnSaveCategories = findViewById(R.id.btn_save_categories);

        // 设置布局管理器
        recyclerSelected.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerAvailable.setLayoutManager(new GridLayoutManager(this, 4));

        // 加载用户之前选择的分类
        loadSelectedCategories();

        // 初始化适配器
        selectedAdapter = new CategoryAdapter(selectedCategories, true, this);
        availableAdapter = new CategoryAdapter(availableCategories, false, this);

        recyclerSelected.setAdapter(selectedAdapter);
        recyclerAvailable.setAdapter(availableAdapter);

        // 设置拖动回调
        // 设置拖动回调 - 双向支持
        CrossListTouchCallback selectedCallback = new CrossListTouchCallback(
                this,
                recyclerSelected,
                recyclerAvailable
        );

        CrossListTouchCallback availableCallback = new CrossListTouchCallback(
                this,
                recyclerAvailable,
                recyclerSelected
        );
        selectedTouchHelper = new ItemTouchHelper(selectedCallback);
        availableTouchHelper = new ItemTouchHelper(availableCallback);

        selectedTouchHelper.attachToRecyclerView(recyclerSelected);
        availableTouchHelper.attachToRecyclerView(recyclerAvailable);

        // 保存按钮点击事件
        btnSaveCategories.setOnClickListener(v -> {
            saveSelectedCategories();
            setResult(RESULT_OK);
            finish();
        });
    }

    private void loadSelectedCategories() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String json = prefs.getString("categories", null);

        // 添加"全部"分类作为第一个固定项
        //selectedCategories.add("全部");

        if (json != null) {
            Type type = new TypeToken<Set<String>>() {}.getType();
            Set<String> savedCategories = new Gson().fromJson(json, type);
            if (savedCategories != null) {
                selectedCategories.addAll(savedCategories);
            }
        } else {
            // 默认添加所有分类
            selectedCategories.addAll(fixedCategories);
        }

        // 构建可用分类列表（排除已选的）
        availableCategories.addAll(fixedCategories);
        availableCategories.removeAll(selectedCategories);
    }

    private void saveSelectedCategories() {
        // 排除"全部"分类
        List<String> toSave = new ArrayList<>(selectedCategories);
        //toSave.remove("全部");

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(new HashSet<>(toSave));
        editor.putString("categories", json);
        editor.apply();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        selectedAdapter.moveItem(fromPosition, toPosition);
    }

    @Override
    public void onItemDropped() {
        // 拖动结束后的处理（如需要）
    }

    @Override
    public void onItemAdded(String category) {
        // 从可用列表移除
        int position = availableCategories.indexOf(category);
        if (position != -1) {
            availableCategories.remove(position);
            availableAdapter.notifyItemRemoved(position);

            // 添加到已选列表
            selectedCategories.add(category);
            selectedAdapter.notifyItemInserted(selectedCategories.size() - 1);
        }
    }

    @Override
    public void onItemRemoved(String category) {
        // 从已选列表移除
        int position = selectedCategories.indexOf(category);
        if (position != -1) {
            selectedCategories.remove(position);
            selectedAdapter.notifyItemRemoved(position);

            // 添加到可用列表
            availableCategories.add(category);
            availableAdapter.notifyItemInserted(availableCategories.size() - 1);
        }
    }
}