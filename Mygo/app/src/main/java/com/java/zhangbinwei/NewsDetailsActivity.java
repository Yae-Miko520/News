package com.java.zhangbinwei;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.gson.Gson;
import com.java.zhangbinwei.adpter.ImageSliderAdapter;
import com.java.zhangbinwei.db.FavoriteDbHelper;
import com.java.zhangbinwei.db.HistoryDbHelper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewsDetailsActivity extends AppCompatActivity {

    private NewsInfo.DataDTO dataDTO;
    private Toolbar toolbar;
    private TextView tvTitle;
    private TextView tvSource;
    private TextView tvPublishTime;
    private TextView tvSummaryLabel;
    private TextView tvContentLabel;
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutDots;
    private StyledPlayerView playerView;
    private WebView wvContent;
    private SimpleExoPlayer player;
    private ImageSliderAdapter imageSliderAdapter;
    private List<String> imageUrls = new ArrayList<>();

    // 添加收藏状态变量
    private boolean isFavorite = false;
    // 添加位置变量
    private int favoritePosition = -1;

    private ImageView favoriteIcon;

    // 添加成员变量
    private TextView tvSummary;
    private static final String PREFS_NAME = "NewsSummaryPrefs";
    private static final String GLM_API_KEY = "5369af6b3f3b4068b9295d9ae76d2bf7.UEsPhakpj6xyxW5K";
    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    private Handler summaryHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 1 && msg.obj != null) {
                String summary = (String) msg.obj;
                tvSummaryLabel.setVisibility(View.VISIBLE);
                tvSummary.setText(summary);
                tvSummary.setVisibility(View.VISIBLE);

                // 保存到本地
                saveSummaryToCache(dataDTO.getNewsID(), summary);
            }
        }
    };


    private String filterContent(String content) {
        // 1. 使用Jsoup进行HTML解析和清理
        Document doc = Jsoup.parse(content);

        // 2. 移除特定类名/ID的元素（常见广告、分享、声明等）
        doc.select("div.share-box, div.related-news, div.ep-source, div.statement, div.disclaimer, div.ad, div.recommend").remove();


        // 4. 移除脚本和样式
        doc.select("script, style").remove();

        // 5. 移除空元素
        doc.select("p:empty, div:empty, span:empty").remove();

        // 7. 返回过滤后的HTML
        return doc.html();
    }

    private String removeSpecificTextPatterns(String content) {
        // 1. 移除开头的分享信息
        content = content.replaceAll("用微信扫码二维码 分享至好友和朋友圈", "");

        // 2. 移除结尾的版权声明
        content = content.replaceAll("特别声明：以上内容为自媒体平台网易号用户上传并发布，本平台仅提供信息存储服务。", "");
        content = content.replaceAll("返回搜狐，查看更多 责任编辑：", "");
        content = content.replaceAll("Notice: The content above is uploaded and posted by a user of NetEase Hao, which is a social media platform and only provides information storage services.", "");

        // 3. 移除日期行（如"爱顺的球 2025-07-08 16:21:06"等）
        content = content.replaceAll("\\w+\\s+\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "");

        // 4. 移除多余的空行
        content = content.replaceAll("(\\n\\s*){2,}", "\n");

        return content;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_details);

        // 初始化控件
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("新闻详情");
        }
        // 初始化收藏图标
        favoriteIcon = findViewById(R.id.favorite_icon);
        favoriteIcon.setOnClickListener(v -> toggleFavorite());

        tvTitle = findViewById(R.id.tvTitle);
        tvSource = findViewById(R.id.tvSource);
        tvPublishTime = findViewById(R.id.tvPublishTime);
        tvSummary = findViewById(R.id.tvSummary);
        tvSummaryLabel = findViewById(R.id.tvSummaryLabel);
        tvContentLabel = findViewById(R.id.tvContentLabel);


        viewPagerImages = findViewById(R.id.viewPagerImages);
        layoutDots = findViewById(R.id.layoutDots);
        playerView = findViewById(R.id.playerView);
        wvContent = findViewById(R.id.wvContent);

        // 配置WebView
        wvContent.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 确保内容可见
                wvContent.setVisibility(View.VISIBLE);
            }

            @SuppressLint("NewApi")
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                // 处理加载错误
                wvContent.loadData("<h3>内容加载失败</h3>", "text/html", "UTF-8");
            }
        });
        WebSettings webSettings = wvContent.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);

        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 获取传递的数据
        dataDTO = (NewsInfo.DataDTO) getIntent().getSerializableExtra("dataDTO");

        // 设置数据
        if (dataDTO != null) {
            tvTitle.setText(dataDTO.getTitle());
            tvSource.setText(dataDTO.getPublisher());
            tvPublishTime.setText(dataDTO.getPublishTime());

            // 处理图片
            setupImageSlider(dataDTO.getImage());

            // 处理视频内容
            if (dataDTO.getVideo() != null && !dataDTO.getVideo().isEmpty()) {
                playerView.setVisibility(View.VISIBLE);
                initializePlayer(dataDTO.getVideo());
            } else {
                playerView.setVisibility(View.GONE);
            }

            // 处理新闻内容
            if (dataDTO.getContent() != null && !dataDTO.getContent().isEmpty()) {
                tvContentLabel.setVisibility(View.VISIBLE);
                wvContent.setVisibility(View.VISIBLE);

                String filteredContent = filterContent(dataDTO.getContent());
                // 应用文本模式过滤
                filteredContent = removeSpecificTextPatterns(filteredContent);
                String htmlContent = "<html><head>" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                        "<style>" +
                        "body { font-size: 16px; line-height: 1.6; color: #333; } " +
                        "img { display: block; max-width: 100%; height: auto; margin: 10px auto; } " +
                        "p { margin: 15px 0; }" +
                        ".section-label { font-weight: bold; margin: 15px 0 5px; color: #333; }" + // 添加标签样式
                        "</style></head>" +
                        "<body>" +
                        filteredContent  +
                        "</body></html>";
                wvContent.loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                );
                //wvContent.loadData(htmlContent, "text/html; charset=UTF-8", "UTF-8");
            } else {
                wvContent.setVisibility(View.GONE);
            }

            // 尝试显示摘要
            showSummaryIfAvailable();

            // 添加历史记录 - 确保在UI线程执行
            runOnUiThread(() -> {
                String dataDTOJson = new Gson().toJson(dataDTO);
                HistoryDbHelper.getInstance(this).addHistory(null, dataDTO.getNewsID(), dataDTOJson);
            });
        }
        // 检查收藏状态
        checkFavoriteStatus();

        // 修改工具栏返回按钮点击事件
        toolbar.setNavigationOnClickListener(v -> {
            // 同样设置返回结果
            Intent resultIntent = new Intent();
            resultIntent.putExtra("read_news_id", dataDTO.getNewsID());
            setResult(RESULT_OK, resultIntent);
            onBackPressed();
        });
        // 获取传递的位置信息
        favoritePosition = getIntent().getIntExtra("position", -1);
    }

    private void checkFavoriteStatus() {
        new Thread(() -> {
            isFavorite = FavoriteDbHelper.getInstance(this).isFavorite(dataDTO.getNewsID());
            runOnUiThread(() -> updateFavoriteIcon());
        }).start();
    }

    private void toggleFavorite() {
        new Thread(() -> {
            String dataDTOJson = new Gson().toJson(dataDTO);

            if (isFavorite) {
                FavoriteDbHelper.getInstance(this).deleteFavorite(dataDTO.getNewsID());
                // 设置结果，通知收藏列表移除该项
                Intent resultIntent = new Intent();
                resultIntent.putExtra("removed_position", favoritePosition);
                setResult(RESULT_OK, resultIntent);
                resultIntent.putExtra("removed_id", dataDTO.getNewsID());
            } else {
                FavoriteDbHelper.getInstance(this).addFavorite(null, dataDTO.getNewsID(), dataDTOJson);
            }

            isFavorite = !isFavorite;
            runOnUiThread(() -> {
                updateFavoriteIcon();
                Toast.makeText(NewsDetailsActivity.this,
                        isFavorite ? "已收藏" : "已取消收藏",
                        Toast.LENGTH_SHORT).show();
            });
        }).start();

    }

    private void updateFavoriteIcon() {
        favoriteIcon.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled :
                R.drawable.ic_favorite_border);
    }


    // 添加新方法
    private void showSummaryIfAvailable() {
        String cachedSummary = getCachedSummary(dataDTO.getNewsID());
        if (cachedSummary != null) {
            tvSummaryLabel.setVisibility(View.VISIBLE);
            tvSummary.setText(cachedSummary);
            tvSummary.setVisibility(View.VISIBLE);
        } else {
            fetchSummaryFromAPI();
        }
    }

    private String getCachedSummary(String newsId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(newsId, null);
    }

    private void saveSummaryToCache(String newsId, String summary) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(newsId, summary);
        editor.apply();
    }

    private void fetchSummaryFromAPI() {
        runOnUiThread(() -> {
            tvSummary.setText("生成摘要中...");
            tvSummary.setVisibility(View.VISIBLE);
        });
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // 构建请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "glm-4");

                JSONArray messages = new JSONArray();
                JSONObject message = new JSONObject();
                message.put("role", "user");
                message.put("content", "请为以下新闻生成一段简洁摘要（不超过100字）：\n标题：" +
                        dataDTO.getTitle() + "\n内容：" + dataDTO.getContent());
                messages.put(message);

                requestBody.put("messages", messages);

                Request request = new Request.Builder()
                        .url(API_URL)
                        .addHeader("Authorization", "Bearer " + GLM_API_KEY)
                        .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject messageObj = firstChoice.getJSONObject("message");
                        String summary = messageObj.getString("content");

                        // 发送到主线程更新UI
                        Message msg = summaryHandler.obtainMessage(1, summary);
                        summaryHandler.sendMessage(msg);
                    }
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvSummary.setText("摘要生成失败");
                    tvSummary.setOnClickListener(v -> fetchSummaryFromAPI());
                });
                Log.e("SummaryAPI", "Error fetching summary", e);
            }
        }).start();
    }

    // 设置图片轮播
    private void setupImageSlider(String imageUrlsStr) {
        if (TextUtils.isEmpty(imageUrlsStr)) {
            viewPagerImages.setVisibility(View.GONE);
            layoutDots.setVisibility(View.GONE);
            return;
        }

        // 清空现有数据
        imageUrls.clear();
        // 使用 Set 去重
        Set<String> uniqueUrls = new LinkedHashSet<>();

        // 分割并处理每个图片URL
        String[] urls = imageUrlsStr.split(",");
        for (String url : urls) {
            String processedUrl = url.replace("[", "").replace("]", "").trim();

            // 跳过空URL
            if (TextUtils.isEmpty(processedUrl)) {
                continue;
            }
            // 处理URL编码
            if (processedUrl.contains("?url=")) {
                try {
                    String encodedUrl = processedUrl.split("\\?url=")[1].split("&")[0];
                    processedUrl = URLDecoder.decode(encodedUrl, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    continue; // 跳过处理失败的URL
                }
            }
            // 再次检查URL是否有效
            if (!TextUtils.isEmpty(processedUrl)) {
                // 添加到Set中去重
                uniqueUrls.add(processedUrl);
            }
        }
        // 添加去重后的URL
        imageUrls.addAll(uniqueUrls);

        if (imageUrls.isEmpty()) {
            viewPagerImages.setVisibility(View.GONE);
            layoutDots.setVisibility(View.GONE);
            return;
        }

        viewPagerImages.setVisibility(View.VISIBLE);


        // 设置适配器
        imageSliderAdapter = new ImageSliderAdapter(this, imageUrls);
        viewPagerImages.setAdapter(imageSliderAdapter);


        // 设置图片点击监听
        imageSliderAdapter.setOnImageClickListener(position -> {
            showFullScreenImage(imageUrls.get(position));
        });

        // 设置指示器（只有多张图片时才显示）
        if (imageUrls.size() > 1) {
            layoutDots.setVisibility(View.VISIBLE);
            setupIndicators(imageUrls.size());

            // 监听页面变化更新指示器
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    updateIndicators(position);
                }
            });
        }else{
            layoutDots.setVisibility(View.GONE);
        }
    }

    // 设置指示器
    private void setupIndicators(int count) {
        layoutDots.removeAllViews();

        if (count <= 1) {
            layoutDots.setVisibility(View.GONE);
            return;
        }

        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(i == 0 ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            layoutDots.addView(dot, params);
        }
    }

    // 更新指示器
    private void updateIndicators(int position) {
        int childCount = layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView dot = (ImageView) layoutDots.getChildAt(i);
            dot.setImageResource(i == position ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        }
    }

    // 显示全屏图片
    private void showFullScreenImage(String imageUrl) {
        if (TextUtils.isEmpty(imageUrl)) {
            return; // 如果URL为空，不显示全屏图片
        }
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(this)
                .load(imageUrl)
                .into(imageView);

        dialog.setContentView(imageView);
        dialog.show();

        // 点击图片关闭
        imageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void initializePlayer(String videoUrl) {
        player = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保返回时不触发刷新
        setResult(RESULT_CANCELED);
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (player != null && player.isPlaying()) {
            player.setPlayWhenReady(false);
        }
        // 设置返回结果并传递已读新闻ID
        Intent resultIntent = new Intent();
        resultIntent.putExtra("read_news_id", dataDTO.getNewsID());
        setResult(RESULT_OK, resultIntent);
        super.onBackPressed();
    }
}