package com.java.zhangbinwei.adpter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.java.zhangbinwei.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<String> categories;
    private final boolean isSelectedList;
    private final OnItemActionListener listener;
    private ItemTouchHelper touchHelper;

    public interface OnItemActionListener {
        void onItemMove(int fromPosition, int toPosition);
        void onItemDropped();
        void onItemAdded(String category);
        void onItemRemoved(String category);
    }

    public CategoryAdapter(List<String> categories, boolean isSelectedList, OnItemActionListener listener) {
        this.categories = categories;
        this.isSelectedList = isSelectedList;
        this.listener = listener;
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        holder.tvCategory.setText(category);
        // 设置标签以便拖动识别
        holder.itemView.setTag(category);

        // 设置删除按钮可见性（仅在已选分类列表显示）
        holder.ivDelete.setVisibility(isSelectedList && !category.equals("全部") ? View.VISIBLE : View.GONE);


        // 修改点击事件处理
        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            String currentCategory = categories.get(currentPosition);
            if (isSelectedList) {
                listener.onItemRemoved(currentCategory);
            } else {
                listener.onItemAdded(currentCategory);
            }
        });

        // 修改删除按钮事件处理
        holder.ivDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            String currentCategory = categories.get(currentPosition);
            listener.onItemRemoved(currentCategory);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(categories, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(categories, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        // 添加这行确保数据正确刷新
        notifyItemRangeChanged(Math.min(fromPosition, toPosition), Math.abs(fromPosition - toPosition) + 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        ImageView ivDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}