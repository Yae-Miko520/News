
package com.java.zhangbinwei.helper;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.java.zhangbinwei.R;
import com.java.zhangbinwei.adpter.CategoryAdapter;

public class CrossListTouchCallback extends ItemTouchHelper.Callback {

    private final CategoryAdapter.OnItemActionListener listener;
    private RecyclerView sourceRecyclerView;
    private RecyclerView targetRecyclerView;

    public CrossListTouchCallback(CategoryAdapter.OnItemActionListener listener,
                                  RecyclerView sourceRecyclerView,
                                  RecyclerView targetRecyclerView) {
        this.listener = listener;
        this.sourceRecyclerView = sourceRecyclerView;
        this.targetRecyclerView = targetRecyclerView;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        // 同一列表内的移动
        if (recyclerView == target.itemView.getParent()) {
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            listener.onItemMove(fromPosition, toPosition);
            return true;
        }
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // 不需要滑动删除
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // 设置拖动时的视图效果
            viewHolder.itemView.setAlpha(0.7f);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(1.0f);

        // 检查是否拖放到另一个列表
        View targetView = findViewAt(targetRecyclerView, viewHolder.itemView);
        if (targetView != null) {
            String category = (String) viewHolder.itemView.getTag();
            if (sourceRecyclerView.getId() == R.id.recycler_selected) {
                listener.onItemRemoved(category);
            } else {
                listener.onItemAdded(category);
            }
        }
        listener.onItemDropped();
    }

    private View findViewAt(RecyclerView recyclerView, View draggedView) {
        int[] draggedLocation = new int[2];
        draggedView.getLocationOnScreen(draggedLocation);

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            int[] childLocation = new int[2];
            child.getLocationOnScreen(childLocation);

            // 检查拖动的视图是否在目标视图上方
            if (draggedLocation[0] + draggedView.getWidth() / 2 >= childLocation[0] &&
                    draggedLocation[0] + draggedView.getWidth() / 2 <= childLocation[0] + child.getWidth() &&
                    draggedLocation[1] + draggedView.getHeight() / 2 >= childLocation[1] &&
                    draggedLocation[1] + draggedView.getHeight() / 2 <= childLocation[1] + child.getHeight()) {
                return child;
            }
        }
        return null;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true; // 启用长按拖动
    }
}