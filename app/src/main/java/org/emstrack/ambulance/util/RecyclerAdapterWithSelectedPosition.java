package org.emstrack.ambulance.util;

import android.os.Build;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.util.ViewHolderWithSelectedPosition.OnClick;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Abstract RecyclerAdapterWithSelectedPosition
 * @author Mauricio de Oliveira
 * @since 1/22/2022
 */

@SuppressWarnings("unchecked")
public abstract class RecyclerAdapterWithSelectedPosition<T, S extends ViewHolderWithSelectedPosition<T>>
        extends RecyclerView.Adapter<S> {

    public interface Compare<T> {
        boolean compare(T listEntry, T entry);
    }

    public class ItemTouch extends ItemTouchHelper.SimpleCallback {

        // https://yfujiki.medium.com/drag-and-reorder-recyclerview-items-in-a-user-friendly-manner-1282335141e9
        int dragFrom = -1;
        int dragTo = -1;

        ItemTouch(int dragDirs, int swipeDirs) {
            super(dragDirs, swipeDirs);
        }

        public ItemTouch() {
            this(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public void onMoved(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, int fromPos, @NonNull RecyclerView.ViewHolder target, int toPos, int x, int y) {
            super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            RecyclerAdapterWithSelectedPosition<T, S> adapterWithSelectedPosition = (RecyclerAdapterWithSelectedPosition<T, S>) recyclerView.getAdapter();
            if (adapterWithSelectedPosition != null) {
                dragFrom = viewHolder.getLayoutPosition();
                dragTo = target.getLayoutPosition();
                return adapterWithSelectedPosition.moveItem(dragFrom, dragTo);
            }
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (viewHolder != null && actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder.itemView.setAlpha(0.5f);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            viewHolder.itemView.setAlpha(1f);

            RecyclerAdapterWithSelectedPosition<T, S> adapterWithSelectedPosition = (RecyclerAdapterWithSelectedPosition<T, S>) recyclerView.getAdapter();
            if (adapterWithSelectedPosition != null) {
                adapterWithSelectedPosition.onItemMoved(dragFrom, dragTo);
            }
        }

    }

    private static final String TAG = RecyclerAdapterWithSelectedPosition.class.getSimpleName();
    private final List<T> list;
    private final OnClick<T> onClick;
    private final Compare<T> compare;
    private int selectedPosition;
    private boolean selectOnClick = true;

    public RecyclerAdapterWithSelectedPosition(List<T> list, OnClick<T> onClick, Compare<T> compare) {
        this.onClick = onClick;
        this.list = list;
        selectedPosition = RecyclerView.NO_POSITION;
        this.compare = compare;
    }

    public RecyclerAdapterWithSelectedPosition(List<T> list, OnClick<T> onClick) {
        this(list, onClick, (listEntry, entry) -> listEntry == entry);
    }

    @CallSuper
    @Override
    public void onBindViewHolder(@NonNull S holder, int position) {
        T item = list.get(position);
        if (item != null) {
            if (selectOnClick) {
                holder.set(item, (entry) -> {
                    // update selected item
                    setSelectedPosition(holder.getLayoutPosition());

                    if (onClick != null) {
                        // perform click
                        onClick.onClick(entry);
                    }
                });
            } else {
                holder.set(item, onClick);
            }
            // Log.d(TAG, String.format("> position = %d, selectedPosition = %d", position, selectedPosition));
            holder.setSelected(position == selectedPosition);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setSelectOnClick(boolean selectOnClick) {
        this.selectOnClick = selectOnClick;
    }

    public boolean isSelectOnClick() {
        return selectOnClick;
    }

    public int getPosition(T entry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return IntStream.range(0, list.size())
                    .filter(i -> compare.compare(list.get(i), entry))
                    .findFirst()
                    .orElse(-1);
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (compare.compare(list.get(i), entry)) {
                    return i;
                }
            }
            return -1;
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int position) {
        if (selectedPosition != position) {
            notifyItemChanged(selectedPosition);
            selectedPosition = position;
            notifyItemChanged(selectedPosition);
        }
    }

    @CallSuper
    public boolean moveItem(int from, int to) {
        // swap items in list
        Collections.swap(list, from, to);
        // notify of move
        notifyItemMoved(from, to);
        return true;
    }

    public void onItemMoved(int from, int to) {

    }

}
