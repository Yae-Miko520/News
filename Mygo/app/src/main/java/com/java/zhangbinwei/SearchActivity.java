package com.java.zhangbinwei;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private EditText etKeywords, etStartDate, etEndDate;
    private Spinner spinnerCategory;
    private Button btnSearch;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etKeywords = findViewById(R.id.et_keywords);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSearch = findViewById(R.id.btn_search);


        // 初始化分类下拉菜单
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // 日期选择器
        setupDatePicker(etStartDate);
        setupDatePicker(etEndDate);

        // 搜索按钮点击事件
        btnSearch.setOnClickListener(v -> performSearch());

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

    private void setupDatePicker(EditText editText) {
        editText.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(
                    SearchActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        editText.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });
    }

    private void performSearch() {
        String keywords = etKeywords.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();

        // 如果分类是"全部"，则传递空字符串
        if ("全部".equals(category)) {
            category = "";
        }

        // 启动搜索结果Activity
        com.java.zhangbinwei.SearchResultActivity.start(this, keywords, category, startDate, endDate);
    }
}