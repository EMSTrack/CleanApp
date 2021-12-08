package org.emstrack.ambulance.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.emstrack.ambulance.R;

/**
 * mostly from https://codeburst.io/android-swipe-menu-with-recyclerview-8f28a235ff28
 * https://github.com/FanFataL/swipe-controller-demo
 */
public class SwipeController extends ItemTouchHelper.Callback {

    private final int swipeFlags;
    private boolean swipeBack = false;
    private ButtonsState buttonShowedState = ButtonsState.GONE;
    private RectF buttonInstance;
    private RecyclerView.ViewHolder currentItemViewHolder;
    private SwipeControllerActions buttonsActions = null;
    private String rightButtonText;
    private String leftButtonText;
    private int leftButtonColor;
    private int rightButtonColor;
    private float leftButtonWidth;
    private float rightButtonWidth;
    private float buttonCorners;

    enum ButtonsState {
        GONE,
        LEFT_VISIBLE,
        RIGHT_VISIBLE
    }

    public SwipeController(Context context,
                           SwipeControllerActions buttonsActions,
                           int swipeFlags) {
        this.buttonsActions = buttonsActions;
        this.swipeFlags = swipeFlags;
        leftButtonText = context.getString(R.string.EDIT);
        leftButtonColor = Color.BLUE;
        leftButtonWidth = 200;
        rightButtonText = context.getString(R.string.DELETE);
        rightButtonColor = Color.RED;
        rightButtonWidth = 200;
        buttonCorners = 16;
    }

    public SwipeController(Context context, SwipeControllerActions buttonsActions) {
        this(context, buttonsActions, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
    }

    public void setLeftButtonText(String leftButtonText) {
        this.leftButtonText = leftButtonText;
    }

    public void setRightButtonText(String rightButtonText) {
        this.rightButtonText = rightButtonText;
    }

    public void setLeftButtonWidth(float leftButtonWidth) {
        this.leftButtonWidth = leftButtonWidth;
    }

    public void setLeftButtonColor(int leftButtonColor) {
        this.leftButtonColor = leftButtonColor;
    }

    public void setRightButtonColor(int rightButtonColor) {
        this.rightButtonColor = rightButtonColor;
    }

    public void setRightButtonWidth(float rightButtonWidth) {
        this.rightButtonWidth = rightButtonWidth;
    }

    public void setButtonCorners(float buttonCorners) {
        this.buttonCorners = buttonCorners;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = buttonShowedState != ButtonsState.GONE;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (buttonShowedState != ButtonsState.GONE) {
                if (buttonShowedState == ButtonsState.LEFT_VISIBLE) dX = Math.max(dX, leftButtonWidth);
                if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) dX = Math.min(dX, -rightButtonWidth);
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            else {
                setTouchListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }

        if (buttonShowedState == ButtonsState.GONE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
        currentItemViewHolder = viewHolder;
    }

    private void setTouchListener(final Canvas c,
                                  final RecyclerView recyclerView,
                                  final RecyclerView.ViewHolder viewHolder,
                                  final float dX,
                                  final float dY,
                                  final int actionState,
                                  final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
            if (swipeBack) {
                if (dX < -rightButtonWidth) buttonShowedState = ButtonsState.RIGHT_VISIBLE;
                else if (dX > leftButtonWidth) buttonShowedState  = ButtonsState.LEFT_VISIBLE;

                if (buttonShowedState != ButtonsState.GONE) {
                    setTouchDownListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    setItemsClickable(recyclerView, false);
                }
            }
            return false;
        });
    }

    private void setTouchDownListener(final Canvas c,
                                      final RecyclerView recyclerView,
                                      final RecyclerView.ViewHolder viewHolder,
                                      final float dX,
                                      final float dY,
                                      final int actionState,
                                      final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                setTouchUpListener(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            return false;
        });
    }

    private void setTouchUpListener(final Canvas c,
                                    final RecyclerView recyclerView,
                                    final RecyclerView.ViewHolder viewHolder,
                                    final float dX,
                                    final float dY,
                                    final int actionState,
                                    final boolean isCurrentlyActive) {
        recyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                SwipeController.super.onChildDraw(c, recyclerView, viewHolder, 0F, dY, actionState, isCurrentlyActive);
                recyclerView.setOnTouchListener((v1, event1) -> false);
                setItemsClickable(recyclerView, true);
                swipeBack = false;

                if (buttonsActions != null && buttonInstance != null && buttonInstance.contains(event.getX(), event.getY())) {
                    if (buttonShowedState == ButtonsState.LEFT_VISIBLE) {
                        buttonsActions.onLeftClicked(viewHolder.getAdapterPosition());
                    }
                    else if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
                        buttonsActions.onRightClicked(viewHolder.getAdapterPosition());
                    }
                }
                buttonShowedState = ButtonsState.GONE;
                currentItemViewHolder = null;
            }
            return false;
        });
    }

    private void setItemsClickable(RecyclerView recyclerView, boolean isClickable) {
        for (int i = 0; i < recyclerView.getChildCount(); ++i) {
            recyclerView.getChildAt(i).setClickable(isClickable);
        }
    }

    private void drawButtons(Canvas c, RecyclerView.ViewHolder viewHolder) {
        float leftButtonWidthWithoutPadding = leftButtonWidth - 20;
        float rightButtonWidthWithoutPadding = rightButtonWidth - 20;

        View itemView = viewHolder.itemView;
        Paint p = new Paint();

        RectF leftButton = new RectF(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + leftButtonWidthWithoutPadding, itemView.getBottom());
        p.setColor(leftButtonColor);
        c.drawRoundRect(leftButton, buttonCorners, buttonCorners, p);
        drawText(leftButtonText, c, leftButton, p);

        RectF rightButton = new RectF(itemView.getRight() - rightButtonWidthWithoutPadding, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        p.setColor(rightButtonColor);
        c.drawRoundRect(rightButton, buttonCorners, buttonCorners, p);
        drawText(rightButtonText, c, rightButton, p);

        buttonInstance = null;
        if (buttonShowedState == ButtonsState.LEFT_VISIBLE) {
            buttonInstance = leftButton;
        }
        else if (buttonShowedState == ButtonsState.RIGHT_VISIBLE) {
            buttonInstance = rightButton;
        }
    }

    private void drawText(String text, Canvas c, RectF button, Paint p) {
        float textSize = 32;
        p.setColor(Color.WHITE);
        p.setAntiAlias(true);
        p.setTextSize(textSize);

        float textWidth = p.measureText(text);
        c.drawText(text, button.centerX()-(textWidth/2), button.centerY()+(textSize/2), p);
    }

    public void onDraw(Canvas c) {
        if (currentItemViewHolder != null) {
            drawButtons(c, currentItemViewHolder);
        }
    }

}
