package com.java.zhangbinwei.adpter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.java.zhangbinwei.NewsInfo;
import com.java.zhangbinwei.R;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

public class NewsListAdapter extends RecyclerView.Adapter<NewsListAdapter.MyHolder> {

    // 添加已读新闻集合
    private Set<String> readNewsSet = new HashSet<>();
    // 添加变量保存最后点击位置
    private int lastClickedPosition = -1;

    // 添加页脚类型常量
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;
    private List<NewsInfo.DataDTO> mDataDTOList = new ArrayList<>();
    private Context mContext;
    private List<View> mFooterViews = new ArrayList<>();

    public List<NewsInfo.DataDTO> getDataList() {
        return mDataDTOList;
    }
    public void setItemRead(int position, String newsId) {
        if (!readNewsSet.contains(newsId)) {
            readNewsSet.add(newsId);
            notifyItemChanged(position);
        }
    }

    // 添加获取位置的方法
    public int getLastClickedPosition() {
        return lastClickedPosition;
    }
    // 设置已读新闻的方法
    public void setReadNews(Set<String> readNewsIds) {
        // 创建新的已读集合
        Set<String> newReadNewsSet = new HashSet<>(readNewsIds);
        // 找出需要更新的位置
        List<Integer> changedPositions = new ArrayList<>();
        // 只更新状态变化的项
        for (int i = 0; i < mDataDTOList.size(); i++) {
            String newsId = mDataDTOList.get(i).getNewsID();
            boolean wasRead = this.readNewsSet.contains(newsId);
            boolean nowRead = newReadNewsSet.contains(newsId);

            if (wasRead != nowRead) {
                changedPositions.add(i);
            }
        }
        this.readNewsSet = new HashSet<>(readNewsIds);
        // 只刷新变化的项
        for (int position : changedPositions) {
            notifyItemChanged(position);
        }
    }

    // 添加页脚视图
    public void addFooterView(View footerView) {
        //mFooterViews.clear(); // 确保只有一个页脚
        //mFooterViews.add(footerView);
        //notifyDataSetChanged();
    }
    // 重写getItemViewType方法
    @Override
    public int getItemViewType(int position) {
        /*if (position == mDataDTOList.size() && !mFooterViews.isEmpty()) {
            return TYPE_FOOTER;
        }*/
        return TYPE_ITEM;
    }



    public void setListData(List<NewsInfo.DataDTO> listData) {
        mDataDTOList.clear();
        if (listData != null) {
            mDataDTOList.addAll(listData);
        }
        notifyDataSetChanged();
    }

    public NewsListAdapter(Context mContext) {
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        /*if (viewType == TYPE_FOOTER) {
            return new MyHolder(mFooterViews.get(0));
        }*/
        //加载布局文件
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_list_item, parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        if(position < mDataDTOList.size()){
            //绑定数据
            NewsInfo.DataDTO dataDTO = mDataDTOList.get(position);

            // 检查是否为已读新闻
            boolean isRead = readNewsSet.contains(dataDTO.getNewsID());

            // 设置标题颜色
            int titleColor = isRead ?
                    ContextCompat.getColor(mContext, R.color.gray) :
                    ContextCompat.getColor(mContext, R.color.black);

            holder.title.setTextColor(titleColor);

            holder.title.setText(dataDTO.getTitle());
            // 设置来源和时间（单行显示）
            holder.organizations.setText(dataDTO.getPublisher());
            holder.publishTime.setText((dataDTO.getPublishTime()));
            //加载图片
            Glide.get(mContext).clearMemory();
            String imageUrl = dataDTO.getImage();
            // 1. 处理包含多个URL的情况（用逗号分隔）
            if (imageUrl.contains(",")) {
                // 取第一个URL并去除方括号
                imageUrl = imageUrl.split(",")[0].replace("[", "").replace("]", "").trim();
            }

            // 2. 处理包含?url=的参数
            if (imageUrl.contains("?url=")) {
                try {
                    // 拆分并解码URL
                    String encodedUrl = imageUrl.split("\\?url=")[1].split("&")[0];
                    imageUrl = URLDecoder.decode(encodedUrl, "UTF-8"); // 关键解码步骤
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            if (!TextUtils.isEmpty(imageUrl)) {
                holder.image.setVisibility(View.VISIBLE);
                // 添加监听器来处理加载错误
                Glide.with(mContext)
                        .load(imageUrl)
                        .error(R.mipmap.ic_launcher)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                // 图片加载失败，隐藏图片视图
                                holder.image.setVisibility(View.GONE);
                                return false; // 返回false，表示继续执行默认的错误处理（显示错误占位图）
                            }

                            @Override
                            public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }


                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                // 图片加载成功，确保图片视图可见
                                holder.image.setVisibility(View.VISIBLE);
                                return false; // 返回false，表示继续执行默认的加载处理
                            }
                        })
                        .into(holder.image);
            } else {
                // 确保完全移除图片视图
                holder.image.setVisibility(View.GONE);
                holder.image.setLayoutParams(new LinearLayoutCompat.LayoutParams(0, 0));
            }

            //点击事件
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 记录点击位置
                    lastClickedPosition = position;
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(dataDTO, position);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDataDTOList.size() ;//+ (mFooterViews.isEmpty() ? 0 : 1);
    }

    // 添加追加数据的方法
    public void appendListData(List<NewsInfo.DataDTO> listData) {
        if (listData != null && !listData.isEmpty()) {
            int startPosition = mDataDTOList.size();
            mDataDTOList.addAll(listData);
            notifyItemRangeInserted(startPosition, listData.size());
        }
    }

    // 页脚ViewHolder
    static class FooterViewHolder extends RecyclerView.ViewHolder {
        FooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView organizations;
        TextView publishTime;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            organizations = itemView.findViewById(R.id.organizations);
            publishTime = itemView.findViewById(R.id.publishTime);
        }
    }



    private OnItemClickListener mOnItemClickListener;


    public void setmOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(NewsInfo.DataDTO dataDTO, int position);
    }

}
