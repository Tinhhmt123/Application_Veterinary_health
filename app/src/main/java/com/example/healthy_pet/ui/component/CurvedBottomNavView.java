package com.example.healthy_pet.ui.component;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Thanh điều hướng dưới dạng sóng cong + vòng tròn nổi (Curved Bottom Navigation):
 * - Trạng thái mặc định: Chỉ hiện icon nét viền đơn sắc, ẩn hoàn toàn chữ và màu.
 * - Khi chọn mục: Đường cong lõm & vòng tròn nổi trượt mượt sang mục đó,
 *   nhấc icon lên cao, phóng to nhẹ, hiện màu chủ đạo (Emerald cho User / Indigo cho Admin)
 *   và hiện chữ tên mục nhỏ gọn bên dưới.
 */
public class CurvedBottomNavView extends View {

    public static final class NavItem {
        @DrawableRes
        public final int iconRes;
        public final String title;

        public NavItem(@DrawableRes int iconRes, @NonNull String title) {
            this.iconRes = iconRes;
            this.title = title;
        }
    }

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    private final List<NavItem> items = new ArrayList<>();
    private final List<Drawable> cachedDrawables = new ArrayList<>();
    private float[] itemProgress = new float[0];

    private boolean isAdminTheme = false;
    private int selectedIndex = 0;
    private float cradleCenterRatio = 0.1f; // Vị trí tâm hõm cong theo tỉ lệ chiều ngang (0..1)
    private int touchDownSlot = -1;

    private ValueAnimator transitionAnimator;
    private OnTabSelectedListener listener;

    private final Path barFillPath = new Path();
    private final Path barTopContourPath = new Path();
    private final Paint barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barContourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cradleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circleShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circleTintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circleStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CurvedBottomNavView(Context context) {
        super(context);
        init();
    }

    public CurvedBottomNavView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CurvedBottomNavView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);
        setBackgroundColor(Color.TRANSPARENT);

        barFillPaint.setStyle(Paint.Style.FILL);

        barContourPaint.setStyle(Paint.Style.STROKE);
        barContourPaint.setStrokeCap(Paint.Cap.ROUND);
        barContourPaint.setStrokeJoin(Paint.Join.ROUND);

        barShadowPaint.setStyle(Paint.Style.STROKE);
        barShadowPaint.setStrokeCap(Paint.Cap.ROUND);
        barShadowPaint.setStrokeJoin(Paint.Join.ROUND);

        cradleGlowPaint.setStyle(Paint.Style.FILL);
        circleShadowPaint.setStyle(Paint.Style.FILL);
        circleFillPaint.setStyle(Paint.Style.FILL);
        circleTintPaint.setStyle(Paint.Style.FILL);
        circleStrokePaint.setStyle(Paint.Style.STROKE);

        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextSize(sp(10.8f));
    }

    public void setupTabs(@NonNull List<NavItem> navItems, boolean adminTheme, int initialIndex) {
        items.clear();
        items.addAll(navItems);

        cachedDrawables.clear();
        for (NavItem item : items) {
            Drawable d = AppCompatResources.getDrawable(getContext(), item.iconRes);
            if (d != null) {
                d = DrawableCompat.wrap(d.mutate());
            }
            cachedDrawables.add(d);
        }

        this.isAdminTheme = adminTheme;
        int count = items.size();
        itemProgress = new float[count];

        int safeIndex = count > 0 ? Math.max(0, Math.min(initialIndex, count - 1)) : 0;
        this.selectedIndex = safeIndex;
        for (int i = 0; i < count; i++) {
            itemProgress[i] = (i == safeIndex) ? 1f : 0f;
        }
        cradleCenterRatio = getSlotCenterRatio(safeIndex);
        invalidate();
    }

    public void setOnTabSelectedListener(@Nullable OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int newIndex, boolean animate) {
        int count = items.size();
        if (count == 0) return;
        final int targetIndex = Math.max(0, Math.min(newIndex, count - 1));

        if (transitionAnimator != null && transitionAnimator.isRunning()) {
            transitionAnimator.cancel();
        }

        if (!animate || getWidth() == 0) {
            selectedIndex = targetIndex;
            for (int i = 0; i < count; i++) {
                itemProgress[i] = (i == targetIndex) ? 1f : 0f;
            }
            cradleCenterRatio = getSlotCenterRatio(targetIndex);
            invalidate();
            return;
        }

        final float startRatio = cradleCenterRatio;
        final float endRatio = getSlotCenterRatio(targetIndex);
        final float[] startItemProg = new float[count];
        System.arraycopy(itemProgress, 0, startItemProg, 0, count);

        selectedIndex = targetIndex;

        transitionAnimator = ValueAnimator.ofFloat(0f, 1f);
        transitionAnimator.setDuration(280L);
        transitionAnimator.setInterpolator(new DecelerateInterpolator(1.7f));
        transitionAnimator.addUpdateListener(anim -> {
            float fraction = (float) anim.getAnimatedValue();
            cradleCenterRatio = startRatio + (endRatio - startRatio) * fraction;
            for (int i = 0; i < count; i++) {
                float goal = (i == targetIndex) ? 1f : 0f;
                itemProgress[i] = startItemProg[i] + (goal - startItemProg[i]) * fraction;
            }
            invalidate();
        });
        transitionAnimator.start();
    }

    private float getSlotCenterRatio(int index) {
        int count = items.size();
        if (count <= 0) return 0.5f;
        return (index + 0.5f) / count;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (transitionAnimator != null && transitionAnimator.isRunning()) {
            transitionAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = items.size();
        int w = getWidth();
        int h = getHeight();
        if (count == 0 || w <= 0 || h <= 0) return;

        boolean isNight = isNightMode();

        // Bảng màu đồng bộ Light Mode & Dark Mode theo vai trò User (Emerald) / Admin (Indigo)
        int barSurfaceColor = Color.parseColor(isNight ? "#1E293B" : "#FFFFFF");
        int circleSurfaceColor = Color.parseColor(isNight ? "#243247" : "#FFFFFF");
        int inactiveStrokeColor = Color.parseColor(isNight ? "#94A3B8" : "#2D3748");

        int activeAccentColor;
        int contourColor;
        int circleRingColor;
        int innerTintColor;
        int cradleAuraCenterColor;

        if (isAdminTheme) {
            activeAccentColor = Color.parseColor(isNight ? "#818CF8" : "#4F46E5");
            contourColor = Color.parseColor(isNight ? "#4D818CF8" : "#2E6366F1");
            circleRingColor = Color.parseColor(isNight ? "#8C818CF8" : "#596366F1");
            innerTintColor = Color.parseColor(isNight ? "#1F818CF8" : "#126366F1");
            cradleAuraCenterColor = Color.parseColor(isNight ? "#336366F1" : "#1C6366F1");
        } else {
            activeAccentColor = Color.parseColor(isNight ? "#34D399" : "#059669");
            contourColor = Color.parseColor(isNight ? "#4D34D399" : "#2E10B981");
            circleRingColor = Color.parseColor(isNight ? "#8C34D399" : "#5910B981");
            innerTintColor = Color.parseColor(isNight ? "#1F34D399" : "#1210B981");
            cradleAuraCenterColor = Color.parseColor(isNight ? "#3310B981" : "#1C10B981");
        }

        float slotW = (float) w / count;
        float barTop = dp(16f);
        float circleRadius = dp(21f);
        float circleCenterY = dp(24f);
        float cradleGap = dp(6.5f);
        float dipBottomY = circleCenterY + circleRadius + cradleGap; // ~51.5dp
        float cradleHalfW = Math.min(dp(41f), slotW * 0.58f);

        float cradleX = cradleCenterRatio * w;
        float leftShoulderX = cradleX - cradleHalfW;
        float rightShoulderX = cradleX + cradleHalfW;

        // 1. Vầng màu dịu nhẹ trong hõm sóng để đường cong nổi bật rõ ràng trên mọi nền trang
        float auraRadius = cradleHalfW * 0.95f;
        cradleGlowPaint.setShader(new RadialGradient(
                cradleX,
                circleCenterY + dp(6f),
                Math.max(1f, auraRadius),
                new int[]{cradleAuraCenterColor, Color.TRANSPARENT},
                new float[]{0.2f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cradleX, circleCenterY + dp(6f), auraRadius, cradleGlowPaint);

        // 2. Tạo đường cong S-Bezier mượt mà cho thân thanh menu dưới
        float startEdgeX = Math.min(0f, leftShoulderX - dp(4f));
        float endEdgeX = Math.max(w, rightShoulderX + dp(4f));

        barTopContourPath.reset();
        barTopContourPath.moveTo(startEdgeX, barTop);
        barTopContourPath.lineTo(leftShoulderX, barTop);
        barTopContourPath.cubicTo(
                leftShoulderX + cradleHalfW * 0.50f, barTop,
                cradleX - cradleHalfW * 0.46f, dipBottomY,
                cradleX, dipBottomY
        );
        barTopContourPath.cubicTo(
                cradleX + cradleHalfW * 0.46f, dipBottomY,
                rightShoulderX - cradleHalfW * 0.50f, barTop,
                rightShoulderX, barTop
        );
        barTopContourPath.lineTo(endEdgeX, barTop);

        barFillPath.reset();
        barFillPath.addPath(barTopContourPath);
        barFillPath.lineTo(endEdgeX, h);
        barFillPath.lineTo(startEdgeX, h);
        barFillPath.close();

        // Đổ bóng mềm dọc theo viền sóng của thanh menu (Hardware-accelerated)
        barShadowPaint.setColor(Color.parseColor(isNight ? "#40000000" : "#12000000"));
        barShadowPaint.setStrokeWidth(dp(5f));
        canvas.save();
        canvas.translate(0f, -dp(1.2f));
        canvas.drawPath(barTopContourPath, barShadowPaint);
        canvas.restore();

        // Nền chính của thanh menu dưới
        barFillPaint.setColor(barSurfaceColor);
        canvas.drawPath(barFillPath, barFillPaint);

        // Viền nhấn mảnh dọc đường cong sóng
        barContourPaint.setColor(contourColor);
        barContourPaint.setStrokeWidth(dp(1.2f));
        canvas.drawPath(barTopContourPath, barContourPaint);

        // 3. Vẽ Vòng tròn nổi (Floating Circle) tại vị trí mục đang chọn
        float shadowRadius = circleRadius + dp(5.5f);
        int shadowCore = Color.parseColor(isNight ? "#59000000" : "#24000000");
        int shadowMid = Color.parseColor(isNight ? "#24000000" : "#0E000000");
        circleShadowPaint.setShader(new RadialGradient(
                cradleX,
                circleCenterY + dp(2.2f),
                shadowRadius,
                new int[]{shadowCore, shadowMid, Color.TRANSPARENT},
                new float[]{0.55f, 0.80f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(cradleX, circleCenterY + dp(2.2f), shadowRadius, circleShadowPaint);

        circleFillPaint.setColor(circleSurfaceColor);
        canvas.drawCircle(cradleX, circleCenterY, circleRadius, circleFillPaint);

        circleTintPaint.setColor(innerTintColor);
        canvas.drawCircle(cradleX, circleCenterY, circleRadius - dp(2.5f), circleTintPaint);

        circleStrokePaint.setColor(circleRingColor);
        circleStrokePaint.setStrokeWidth(dp(1.5f));
        canvas.drawCircle(cradleX, circleCenterY, circleRadius - dp(0.75f), circleStrokePaint);

        // 4. Vẽ từng Icon và Nhãn chữ (chỉ hiện chữ & màu khi mục được chọn)
        float flatIconCenterY = barTop + (h - barTop) * 0.50f;
        float baseIconSize = dp(22f);
        float selectedScaleBoost = 0.16f; // Phóng to ~16% khi chọn

        for (int i = 0; i < count; i++) {
            Drawable icon = cachedDrawables.get(i);
            if (icon == null) continue;

            float prog = itemProgress[i]; // 0f = mặc định (chỉ viền, ẩn chữ), 1f = đang chọn
            float slotCenterX = (i + 0.5f) * slotW;

            // Nội suy mượt từ vị trí phẳng lên tâm vòng tròn nổi
            float iconCenterX = slotCenterX + (cradleX - slotCenterX) * prog * 0.15f;
            float iconCenterY = flatIconCenterY + (circleCenterY - flatIconCenterY) * prog;
            float scale = 1.0f + (selectedScaleBoost * prog);
            int halfSize = Math.round((baseIconSize * scale) * 0.5f);

            int iconColor = blendColor(inactiveStrokeColor, activeAccentColor, prog);
            DrawableCompat.setTint(icon, iconColor);
            icon.setBounds(
                    Math.round(iconCenterX) - halfSize,
                    Math.round(iconCenterY) - halfSize,
                    Math.round(iconCenterX) + halfSize,
                    Math.round(iconCenterY) + halfSize
            );
            icon.draw(canvas);

            // Chữ tên mục: Ẩn hoàn toàn ở mặc định (prog = 0), hiện mượt bên dưới vòng tròn khi chọn
            if (prog > 0.04f) {
                String title = items.get(i).title;
                if (title != null && !title.isEmpty()) {
                    labelPaint.setColor(activeAccentColor);
                    labelPaint.setAlpha(Math.min(255, Math.round(255f * prog)));

                    float textHalfW = labelPaint.measureText(title) * 0.5f;
                    float edgePad = dp(6f);
                    float clampedTextX = Math.max(edgePad + textHalfW, Math.min(w - edgePad - textHalfW, slotCenterX));
                    float baseTextY = h - dp(8.5f) + (1f - prog) * dp(4f);
                    canvas.drawText(title, clampedTextX, baseTextY, labelPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int count = items.size();
        if (count == 0 || !isEnabled()) {
            return super.onTouchEvent(event);
        }

        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            if (y < 0 || y > getHeight()) return false;
            touchDownSlot = Math.max(0, Math.min((int) (x / ((float) getWidth() / count)), count - 1));
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            int upSlot = Math.max(0, Math.min((int) (x / ((float) getWidth() / count)), count - 1));
            if (upSlot == touchDownSlot && x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight()) {
                playSoundEffect(SoundEffectConstants.CLICK);
                performClick();
                setSelectedIndex(upSlot, true);
                if (listener != null) {
                    listener.onTabSelected(upSlot);
                }
            }
            touchDownSlot = -1;
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            touchDownSlot = -1;
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int blendColor(int fromColor, int toColor, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        float inv = 1f - clamped;
        int a = Math.round(Color.alpha(fromColor) * inv + Color.alpha(toColor) * clamped);
        int r = Math.round(Color.red(fromColor) * inv + Color.red(toColor) * clamped);
        int g = Math.round(Color.green(fromColor) * inv + Color.green(toColor) * clamped);
        int b = Math.round(Color.blue(fromColor) * inv + Color.blue(toColor) * clamped);
        return Color.argb(a, r, g, b);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }
}
