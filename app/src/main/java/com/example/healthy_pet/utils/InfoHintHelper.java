package com.example.healthy_pet.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * Tiện ích dùng chung cho nút tròn "!" đặt sát bên phải tiêu đề in đậm
 * và hiển thị thẻ chú thích nổi (PopupWindow) neo bên trên nút "!".
 * - Đo kích thước theo chế độ EXACTLY và truyền chiều rộng/chiều cao tường minh vào PopupWindow.
 * - Không làm xê dịch hay ảnh hưởng bố cục các thành phần UI khác.
 * - Đồng bộ màu sắc theo vai trò (Emerald cho User, Indigo cho Admin) và Light/Dark Mode.
 */
public final class InfoHintHelper {

    private static WeakReference<PopupWindow> activePopupRef;
    private static WeakReference<View> lastDismissedAnchorRef;
    private static long lastDismissUptimeMs = 0L;

    private InfoHintHelper() {
    }

    public static void attach(@Nullable View triggerView,
                              @Nullable CharSequence hintMessage,
                              boolean isAdminRole) {
        attach(triggerView, hintMessage, isAdminRole, false);
    }

    public static void attach(@Nullable View triggerView,
                              @Nullable final CharSequence hintMessage,
                              final boolean isAdminRole,
                              final boolean onSolidBanner) {
        if (triggerView == null) {
            return;
        }
        styleTriggerBadge(triggerView, isAdminRole, onSolidBanner);

        if (hintMessage == null || hintMessage.toString().trim().isEmpty()) {
            triggerView.setVisibility(View.GONE);
            return;
        }

        triggerView.setVisibility(View.VISIBLE);
        triggerView.setContentDescription("Xem chú thích tính năng");

        triggerView.setOnClickListener(v -> {
            PopupWindow current = activePopupRef != null ? activePopupRef.get() : null;
            if (current != null && current.isShowing()) {
                current.dismiss();
                return;
            }
            View lastAnchor = lastDismissedAnchorRef != null ? lastDismissedAnchorRef.get() : null;
            if (lastAnchor == v && (SystemClock.uptimeMillis() - lastDismissUptimeMs) < 220L) {
                return;
            }
            showFloatingHintPopup(v, hintMessage.toString().trim(), isAdminRole);
        });

        triggerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                PopupWindow current = activePopupRef != null ? activePopupRef.get() : null;
                if (current != null && current.isShowing()) {
                    try {
                        current.dismiss();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    /**
     * Trang trí nút dấu chấm than (!) hình tròn nhỏ gọn đồng bộ màu vai trò & Light/Dark Mode.
     */
    private static void styleTriggerBadge(@NonNull View view, boolean isAdmin, boolean onSolidBanner) {
        Context context = view.getContext();
        boolean isNight = isNightMode(context);

        int bgColor;
        int strokeColor;
        int textColor;

        if (onSolidBanner) {
            bgColor = Color.parseColor("#2EFFFFFF");
            strokeColor = Color.parseColor("#D9FFFFFF");
            textColor = Color.WHITE;
        } else if (isAdmin) {
            bgColor = Color.parseColor(isNight ? "#1E2642" : "#EEF2FF");
            strokeColor = Color.parseColor(isNight ? "#818CF8" : "#6366F1");
            textColor = Color.parseColor(isNight ? "#A5B4FC" : "#4338CA");
        } else {
            bgColor = Color.parseColor(isNight ? "#13382F" : "#ECFDF5");
            strokeColor = Color.parseColor(isNight ? "#34D399" : "#10B981");
            textColor = Color.parseColor(isNight ? "#6EE7B7" : "#047857");
        }

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(bgColor);
        circle.setStroke(Math.max(1, dp(context, 1.3f)), strokeColor);
        view.setBackground(circle);

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            tv.setText("!");
            tv.setTextColor(textColor);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setIncludeFontPadding(false);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        }
    }

    /**
     * Hiển thị thẻ chú thích nổi ưu tiên neo phía trên nút "!" (nổi neo bên trên),
     * tự động đảo xuống ngay bên dưới nếu nút "!" quá sát mép trên màn hình để không bị cắt khuất.
     */
    private static void showFloatingHintPopup(@NonNull View anchor,
                                              @NonNull String message,
                                              boolean isAdmin) {
        Context context = anchor.getContext();
        if (context == null) return;

        boolean isNight = isNightMode(context);

        int surfaceColor = Color.parseColor(isNight ? "#1E293B" : "#FFFFFF");
        int borderColor = isAdmin
                ? Color.parseColor(isNight ? "#80818CF8" : "#596366F1")
                : Color.parseColor(isNight ? "#8034D399" : "#5910B981");
        int accentBarColor = isAdmin
                ? Color.parseColor(isNight ? "#818CF8" : "#6366F1")
                : Color.parseColor(isNight ? "#34D399" : "#10B981");
        int headerLabelColor = isAdmin
                ? Color.parseColor(isNight ? "#A5B4FC" : "#4F46E5")
                : Color.parseColor(isNight ? "#6EE7B7" : "#059669");
        int bodyTextColor = Color.parseColor(isNight ? "#F1F5F9" : "#1E293B");
        int closeIconColor = Color.parseColor(isNight ? "#94A3B8" : "#64748B");

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;

        int outerPadH = dp(context, 6f);
        int outerPadV = dp(context, 3f);
        int arrowW = dp(context, 14f);
        int arrowH = dp(context, 6f);

        int padLeft = dp(context, 18f);
        int padRight = dp(context, 12f);
        int padTop = dp(context, 8f);
        int padBottom = dp(context, 9f);

        // Khối thẻ chính vẽ sẵn thanh nhấn màu dọc bên trái (không dùng MATCH_PARENT child)
        CardBubbleLayout cardBox = new CardBubbleLayout(context, accentBarColor);
        cardBox.setOrientation(LinearLayout.VERTICAL);
        cardBox.setPadding(padLeft, padTop, padRight, padBottom);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(context, 14f));
        cardBg.setColor(surfaceColor);
        cardBg.setStroke(Math.max(1, dp(context, 1.2f)), borderColor);
        cardBox.setBackground(cardBg);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardBox.setElevation(dp(context, 5f));
        }

        // Hàng tiêu đề nhỏ gọn (FrameLayout không dùng weight=1)
        FrameLayout headerRow = new FrameLayout(context);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvTagLabel = new TextView(context);
        tvTagLabel.setText("💡 Chú thích");
        tvTagLabel.setTextColor(headerLabelColor);
        tvTagLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvTagLabel.setTypeface(Typeface.DEFAULT_BOLD);
        headerRow.addView(tvTagLabel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL));

        TextView tvClose = new TextView(context);
        tvClose.setText("✕");
        tvClose.setTextColor(closeIconColor);
        tvClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        tvClose.setTypeface(Typeface.DEFAULT_BOLD);
        tvClose.setPadding(dp(context, 6f), 0, dp(context, 2f), 0);
        headerRow.addView(tvClose, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL));

        TextView tvBody = new TextView(context);
        tvBody.setText(message);
        tvBody.setTextColor(bodyTextColor);
        tvBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.8f);
        tvBody.setLineSpacing(dp(context, 2f), 1.0f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = dp(context, 3f);
        tvBody.setLayoutParams(bodyLp);

        cardBox.addView(headerRow);
        cardBox.addView(tvBody);

        // Tính chiều rộng chuẩn tường minh cho thẻ chú thích (216dp -> tối đa 288dp)
        int maxCardW = Math.min(screenW - dp(context, 32f), dp(context, 288f));
        int minCardW = Math.min(maxCardW, dp(context, 216f));
        int naturalTextW = Math.round(tvBody.getPaint().measureText(message)) + padLeft + padRight + dp(context, 6f);
        int cardW = Math.max(minCardW, Math.min(maxCardW, naturalTextW));
        int popupW = cardW + (outerPadH * 2);

        LinearLayout rootWrapper = new LinearLayout(context);
        rootWrapper.setOrientation(LinearLayout.VERTICAL);
        rootWrapper.setBackgroundColor(Color.TRANSPARENT);
        rootWrapper.setClipChildren(false);
        rootWrapper.setClipToPadding(false);
        rootWrapper.setPadding(outerPadH, outerPadV, outerPadH, outerPadV);

        PointerTriangleView topArrow = new PointerTriangleView(context, surfaceColor, borderColor, true);
        LinearLayout.LayoutParams topArrowLp = new LinearLayout.LayoutParams(arrowW, arrowH);
        topArrow.setLayoutParams(topArrowLp);
        topArrow.setVisibility(View.GONE);

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                cardW, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardBox.setLayoutParams(cardLp);

        PointerTriangleView bottomArrow = new PointerTriangleView(context, surfaceColor, borderColor, false);
        LinearLayout.LayoutParams bottomArrowLp = new LinearLayout.LayoutParams(arrowW, arrowH);
        bottomArrow.setLayoutParams(bottomArrowLp);
        bottomArrow.setVisibility(View.VISIBLE);

        rootWrapper.addView(topArrow);
        rootWrapper.addView(cardBox);
        rootWrapper.addView(bottomArrow);

        // Đo kích thước theo chế độ EXACTLY để không bao giờ bị bóp ngang hoặc kéo giãn dọc
        int exactWidthSpec = View.MeasureSpec.makeMeasureSpec(popupW, View.MeasureSpec.EXACTLY);
        int unspecifiedHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        rootWrapper.measure(exactWidthSpec, unspecifiedHeightSpec);

        int popupH = rootWrapper.getMeasuredHeight();

        int[] anchorLoc = new int[2];
        anchor.getLocationOnScreen(anchorLoc);
        int anchorCenterX = anchorLoc[0] + anchor.getWidth() / 2;
        int anchorTopY = anchorLoc[1];
        int anchorBottomY = anchorLoc[1] + anchor.getHeight();

        int safeTopBound = getStatusBarHeight(context) + dp(context, 4f);
        int preferredAboveY = anchorTopY - popupH + dp(context, 1f);
        boolean showAbove = preferredAboveY >= safeTopBound;

        if (!showAbove) {
            topArrow.setVisibility(View.VISIBLE);
            bottomArrow.setVisibility(View.GONE);
            rootWrapper.measure(exactWidthSpec, unspecifiedHeightSpec);
            popupH = rootWrapper.getMeasuredHeight();
        }

        int screenMargin = dp(context, 12f);
        int popupX = anchorCenterX - (int) (popupW * 0.65f);
        if (popupX < screenMargin) {
            popupX = screenMargin;
        }
        if (popupX + popupW > screenW - screenMargin) {
            popupX = Math.max(screenMargin, screenW - screenMargin - popupW);
        }

        // Căn mũi nhọn của bong bóng trỏ thẳng vào tâm nút "!"
        int arrowLeftInsideRoot = anchorCenterX - popupX - outerPadH - (arrowW / 2);
        int minArrowLeft = dp(context, 16f);
        int maxArrowLeft = Math.max(minArrowLeft, cardW - arrowW - dp(context, 16f));
        if (arrowLeftInsideRoot < minArrowLeft) arrowLeftInsideRoot = minArrowLeft;
        if (arrowLeftInsideRoot > maxArrowLeft) arrowLeftInsideRoot = maxArrowLeft;

        if (showAbove) {
            bottomArrowLp.leftMargin = arrowLeftInsideRoot;
            bottomArrow.setLayoutParams(bottomArrowLp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bottomArrow.setElevation(dp(context, 5.2f));
            }
        } else {
            topArrowLp.leftMargin = arrowLeftInsideRoot;
            topArrow.setLayoutParams(topArrowLp);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                topArrow.setElevation(dp(context, 5.2f));
            }
        }

        int popupY = showAbove ? preferredAboveY : (anchorBottomY - dp(context, 1f));

        // Truyền cả popupW và popupH tường minh để WindowManager dựng đúng khung nhỏ gọn
        final PopupWindow popupWindow = new PopupWindow(rootWrapper, popupW, popupH, true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        popupWindow.setOnDismissListener(() -> {
            lastDismissedAnchorRef = new WeakReference<>(anchor);
            lastDismissUptimeMs = SystemClock.uptimeMillis();
        });

        View.OnClickListener closeClick = v -> popupWindow.dismiss();
        tvClose.setOnClickListener(closeClick);
        cardBox.setOnClickListener(closeClick);

        activePopupRef = new WeakReference<>(popupWindow);
        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY);
    }

    private static boolean isNightMode(@NonNull Context context) {
        int mode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int getStatusBarHeight(@NonNull Context context) {
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            return context.getResources().getDimensionPixelSize(resId);
        }
        return dp(context, 26f);
    }

    private static int dp(@NonNull Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Khung thẻ bo góc tự vẽ thanh nhấn màu dọc bên trái bằng Canvas,
     * tránh dùng View con MATCH_PARENT bên trong PopupWindow.
     */
    private static final class CardBubbleLayout extends LinearLayout {
        private final Paint stripPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF stripRect = new RectF();
        private final float stripLeft;
        private final float stripWidth;
        private final float stripRadius;
        private final float stripVerticalInset;

        CardBubbleLayout(Context context, int stripColor) {
            super(context);
            stripPaint.setStyle(Paint.Style.FILL);
            stripPaint.setColor(stripColor);
            float density = context.getResources().getDisplayMetrics().density;
            stripLeft = 9.5f * density;
            stripWidth = Math.max(2f, 3.5f * density);
            stripRadius = 2f * density;
            stripVerticalInset = 9.5f * density;
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float top = stripVerticalInset;
            float bottom = Math.max(top + stripWidth, getHeight() - stripVerticalInset);
            stripRect.set(stripLeft, top, stripLeft + stripWidth, bottom);
            canvas.drawRoundRect(stripRect, stripRadius, stripRadius, stripPaint);
        }
    }

    /**
     * Mũi nhọn tam giác nhỏ nối liền thân thẻ chú thích với nút tròn "!".
     */
    private static final class PointerTriangleView extends View {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path fillPath = new Path();
        private final Path borderPath = new Path();
        private final boolean pointUp;

        PointerTriangleView(Context context, int fillColor, int strokeColor, boolean pointUp) {
            super(context);
            this.pointUp = pointUp;
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setColor(fillColor);

            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(Math.max(1f, context.getResources().getDisplayMetrics().density * 1.2f));
            borderPaint.setColor(strokeColor);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            fillPath.reset();
            borderPath.reset();

            if (pointUp) {
                fillPath.moveTo(0f, h);
                fillPath.lineTo(w / 2f, 0f);
                fillPath.lineTo(w, h);
                fillPath.close();

                borderPath.moveTo(0f, h);
                borderPath.lineTo(w / 2f, 0f);
                borderPath.lineTo(w, h);
            } else {
                fillPath.moveTo(0f, 0f);
                fillPath.lineTo(w / 2f, h);
                fillPath.lineTo(w, 0f);
                fillPath.close();

                borderPath.moveTo(0f, 0f);
                borderPath.lineTo(w / 2f, h);
                borderPath.lineTo(w, 0f);
            }

            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(borderPath, borderPaint);
        }
    }
}
