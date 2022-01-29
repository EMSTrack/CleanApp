package org.emstrack.ambulance.util;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class DragHelper implements View.OnTouchListener {

    private static final String TAG = DragHelper.class.getSimpleName();
    private static final float SNAP_FACTOR = .7f;

    private final View companionView;
    private float dX, dY;
    private float dXc, dYc;
    private int lastAction;
    private boolean verticalConstraint, horizontalConstraint, snap;
    private float maxVertical, minVertical, maxHorizontal, minHorizontal;
    private int animateTime;
    private float x0, y0;

    public DragHelper() {
        this(null);
    }

    public DragHelper(View companionView) {
        this.companionView = companionView;
        verticalConstraint = false;
        horizontalConstraint = false;
        snap = false;
        maxVertical = -1;
        maxHorizontal = -1;
        minVertical = 1;
        minHorizontal = 1;
        animateTime = -1;
    }

    public void setMaxHorizontal(float maxHorizontal) {
        this.maxHorizontal = maxHorizontal;
    }

    public float getMaxHorizontal() {
        return maxHorizontal;
    }

    public void setMaxVertical(float maxVertical) {
        this.maxVertical = maxVertical;
    }

    public float getMaxVertical() {
        return maxVertical;
    }

    public void setMinVertical(float minVertical) {
        this.minVertical = minVertical;
    }

    public float getMinVertical() {
        return minVertical;
    }

    public void setMinHorizontal(float minHorizontal) {
        this.minHorizontal = minHorizontal;
    }

    public float getMinHorizontal() {
        return minHorizontal;
    }

    public void setAnimateTime(int animateTime) {
        this.animateTime = animateTime;
    }

    public int getAnimateTime() {
        return animateTime;
    }

    public void setVerticalConstraint(boolean verticalConstraint) {
        this.verticalConstraint = verticalConstraint;
    }

    public boolean isVerticalConstraint() {
        return verticalConstraint;
    }

    public void setHorizontalConstraint(boolean horizontalConstraint) {
        this.horizontalConstraint = horizontalConstraint;
    }

    public boolean isHorizontalConstraint() {
        return horizontalConstraint;
    }

    public void setSnap(boolean snap) {
        this.snap = snap;
    }

    public boolean isSnap() {
        return snap;
    }

    public void setDown(float offset) {
        setVerticalConstraint(true);
        setMinVertical(-offset);
        setMaxVertical(0);
        setSnap(true);
    }

    public void setUp(float offset) {
        setVerticalConstraint(true);
        setMinVertical(0);
        setMaxVertical(offset);
        setSnap(true);
    }

    public boolean isDown() {
        return minVertical < 0 && maxVertical == 0;
    }

    public boolean isUp() {
        return maxVertical > 0 && minVertical == 0;
    }

    public void toggleSnap(View view) {
        toggleSnap(view, animateTime);
    }

    public void toggleSnap(View view, int animateTime) {
        if (!snap) {
            return;
        }

        float x = view.getX();
        float y = view.getY();

        if (!verticalConstraint) {
            if (maxHorizontal > 0 && minHorizontal == 0) {
                // snap up
                x += maxHorizontal;
                minHorizontal = -maxHorizontal;
                maxHorizontal = 0;
            } else if (minHorizontal < 0 && maxHorizontal == 0) {
                // snap up
                x += minHorizontal;
                maxHorizontal = -minHorizontal;
                minHorizontal = 0;
            }
        }

        if (!horizontalConstraint) {
            if (maxVertical >= 0 && minVertical == 0) {
                // snap up
                y += maxVertical;
                minVertical = -maxVertical;
                maxVertical = 0;
            } else if (minVertical < 0 && maxVertical == 0) {
                // snap up
                y += minVertical;
                maxVertical = -minVertical;
                minVertical = 0;
            }
        }

        view.animate()
                .x(x)
                .y(y)
                .setDuration(animateTime)
                .start();

    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                x0 = view.getX();
                dX = x0 - event.getRawX();
                y0 = view.getY();
                dY = y0 - event.getRawY();
                lastAction = MotionEvent.ACTION_DOWN;
                Log.d(TAG, String.format("x0 = %f, y0 = %f, dx = %f, dy = %f", x0, y0, dX, dY));
                Log.d(TAG, String.format("minHorizontal = %f, maxHorizontal = %f", minHorizontal, maxHorizontal));
                Log.d(TAG, String.format("minVertical = %f, maxVertical = %f", minVertical, maxVertical));
                if (companionView != null) {
                    dXc = companionView.getX() - event.getRawX();
                    dYc = companionView.getY() - event.getRawY();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!verticalConstraint) {
                    float x = event.getRawX() + dX;
                    Log.d(TAG, String.format("x = %f, x - x0 = %f", x, x - x0));
                    if ((maxHorizontal < 0 || x <= x0 + maxHorizontal) && (minHorizontal > 0 || x >= x0 + minHorizontal)) {
                        view.setX(x);
                        if (companionView != null) {
                            companionView.setX(event.getRawX() + dXc);
                        }
                    }
                }
                if (!horizontalConstraint) {
                    float y = event.getRawY() + dY;
                    Log.d(TAG, String.format("y = %f, y - y0 = %f", y, y - y0));
                    if ((maxVertical < 0 || y <= y0 + maxVertical) && (minVertical > 0 || y >= y0 + minVertical)) {
                        view.setY(y);
                        if (companionView != null) {
                            companionView.setY(event.getRawY() + dYc);
                        }
                    }
                }
                lastAction = MotionEvent.ACTION_MOVE;
                break;

            case MotionEvent.ACTION_UP:
                if (lastAction == MotionEvent.ACTION_DOWN) {
                    view.performClick();
                } else if (snap) {
                    float x = event.getRawX() + dX;
                    if (!verticalConstraint) {
                        if (maxHorizontal >= 0 && x >= x0) {
                            if (x < x0 + SNAP_FACTOR * maxHorizontal) {
                                x = x0;
                            } else {
                                // snapped to max
                                x = x0 + maxHorizontal;
                                if (minHorizontal > 0) {
                                    minHorizontal = -maxHorizontal;
                                } else {
                                    minHorizontal -= maxHorizontal;
                                }
                                maxHorizontal = 0;
                            }
                        } else if (minHorizontal <= 0 && x <= x0) {
                            if (x > x0 + SNAP_FACTOR * minHorizontal) {
                                x = x0;
                            } else {
                                // snapped to min
                                x = x0 + minHorizontal;
                                if (maxHorizontal < 0) {
                                    maxHorizontal = -minHorizontal;
                                } else {
                                    maxHorizontal -= minHorizontal;
                                }
                                minHorizontal = 0;
                            }
                        }
                    } else {
                        x = x0;
                    }
                    float y = event.getRawY() + dY;
                    if (!horizontalConstraint) {
                        if (maxVertical >= 0 && y >= y0) {
                            if (y < y0 + SNAP_FACTOR * maxVertical) {
                                y = y0;
                            } else {
                                // snapped to max
                                y = y0 + maxVertical;
                                if (minVertical > 0) {
                                    minVertical = -maxVertical;
                                } else {
                                    minVertical -= maxVertical;
                                }
                                maxVertical = 0;
                            }
                        } else if (minVertical <= 0 && y <= y0) {
                            if (y > y0 + SNAP_FACTOR * minVertical) {
                                y = y0;
                            } else {
                                // snapped to min
                                y = y0 + minVertical;
                                if (maxVertical < 0) {
                                    maxVertical = -minVertical;
                                } else {
                                    maxVertical -= minVertical;
                                }
                                minVertical = 0;
                            }
                        }
                    } else {
                        y = y0;
                    }
                    Log.d(TAG, String.format("x = %f, y = %f", x, y));
                    Log.d(TAG, String.format("minHorizontal = %f, maxHorizontal = %f", minHorizontal, maxHorizontal));
                    Log.d(TAG, String.format("minVertical = %f, maxVertical = %f", minVertical, maxVertical));
                    if (animateTime >= 0) {
                        view.animate()
                                .x(x)
                                .y(y)
                                .setDuration(animateTime)
                                .start();
                    }
                }
                break;

            default:
                return false;
        }
        return true;
    }

}
