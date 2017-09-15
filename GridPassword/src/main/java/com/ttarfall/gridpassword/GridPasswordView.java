package com.ttarfall.gridpassword;

/**
 * Created by ttarfall on 2017/9/15.
 */

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.InputFilter;
import android.text.TextPaint;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.ttarfall.gridpassword.R;

/**
 * @author ttarfall
 * @date 2017-08-30 17:43
 */
public class GridPasswordView extends TextView {

    private static final String TAG = "GridPasswordView";
    private final int DEFAULT_PASSWORD_LENGTH = 6;//默认密码长度
    private static final String DEFAULT_TRANSFORMATION = "●";

    private int mPasswordLength;//密码长度

    private int mLineWidth;//线宽度
    private int mLineHeight;//线高度
    private int mLineColor;//线颜色
    private boolean mXLineHeightFull;
    private Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ColorStateList mTextColor;
    private TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private boolean mCursorVisiable = true;
    private int mCursorWidth;
    private int mCursorHeight;
    private int mCursorDrawableRes;
    private Drawable mCursorDrawable;
    private BitmapDrawable mCursorBitmapDrawable;
    private Rect mCursorRect;
    private long mShowCursor;
    private Blink mBlink;
    private static final int BLINK = 500;

    private InputMethodManager mInputMethodManager;
    private float mOffsetLeft;//水平方向偏移量
    private float mOffsetTop;//垂直方向偏移
    private float mXLineHeight;//水平方向线高度
    private float mTotalLineWidth;//总共长度
    private float mYLineHeight;//垂直方向长度

    public GridPasswordView(Context context) {
        this(context, null);
    }

    public GridPasswordView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridPasswordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        initAttrs(context, attrs, defStyleAttr);
        setTextIsSelectable(true);
        setFilters(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setCursorVisible(isCursorVisible());
        } else {
            setCursorVisible(true);
        }
        mTextPaint.setTextSize(getTextSize());
        updateCursorDrawable(ContextCompat.getDrawable(context, mCursorDrawableRes));
    }

    private void initAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridPasswordView, defStyleAttr, 0);
        mPasswordLength = a.getInteger(R.styleable.GridPasswordView_gpPasswordLength, DEFAULT_PASSWORD_LENGTH);
        mLineWidth = a.getDimensionPixelSize(R.styleable.GridPasswordView_gpLineWidth, dp2px(1));
        mLineHeight = a.getDimensionPixelSize(R.styleable.GridPasswordView_gpLineHeight, dp2px(40));
        mLineColor = a.getColor(R.styleable.GridPasswordView_gpLineColor, Color.GRAY);
        mCursorWidth = a.getDimensionPixelSize(R.styleable.GridPasswordView_gpCursorWidth, 0);
        mCursorHeight = a.getDimensionPixelSize(R.styleable.GridPasswordView_gpCursorHeight, 0);
        mCursorDrawableRes = a.getResourceId(R.styleable.GridPasswordView_gpCursorDrawable, 0);
        mXLineHeightFull = a.getBoolean(R.styleable.GridPasswordView_gpXLineHeightFull, false);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width;
        int height;
        if (mLineHeight > 0) {
            if (heightMode == MeasureSpec.AT_MOST) {
                height = mLineHeight + mLineWidth * 2
                        + getPaddingTop()
                        + getPaddingBottom();
                height = Math.min(height, heightSize);
            } else {
                height = heightSize;
            }
            if (widthMode == MeasureSpec.AT_MOST) {
                width = mLineHeight * mPasswordLength + mLineWidth * (mPasswordLength + 1)
                        + getPaddingLeft()
                        + getPaddingRight();
                width = Math.min(width, widthSize);
            } else {
                width = widthSize;
            }
        } else {
            width = widthSize;
            int lineHeight = (int) ((width - getPaddingLeft()
                    - getPaddingRight()
                    - mLineWidth * (mPasswordLength + 1)) * 1.0f / 6);
            if (heightMode == MeasureSpec.AT_MOST) {
                height = lineHeight + mLineWidth * 2
                        + getPaddingTop()
                        + getPaddingBottom();
            } else {
                height = heightSize;
            }
        }
        calLineLayout(width, height, mLineWidth);
        setMeasuredDimension(width, height);
    }

    private void calLineLayout(int width, int height, int lineWidth) {
        mOffsetLeft = getPaddingLeft();
        mOffsetTop = getPaddingRight();
        //绘制密码框
        if (mLineHeight > 0) {
            mYLineHeight = Math.min(mLineHeight, height - lineWidth * 2);
            //绘制横线
            mTotalLineWidth = mYLineHeight * mPasswordLength + lineWidth * (mPasswordLength + 1);
            int contentWidth = width - getPaddingLeft() - getPaddingRight();
            if (mTotalLineWidth > contentWidth || mXLineHeightFull) {
                mTotalLineWidth = contentWidth;
                mXLineHeight = (contentWidth - lineWidth * (mPasswordLength + 1)) * 1.0f / mPasswordLength;
            } else {
                mXLineHeight = mYLineHeight;
            }
            mOffsetLeft = (contentWidth - mTotalLineWidth) * 1.0f / 2 + mOffsetLeft;
            final float offset = height - mYLineHeight - lineWidth * 2;
            if (offset > 0) {
                mOffsetTop = offset * 1.0f / 2;
            } else {
                mOffsetTop = 0f;
            }
        } else {
            final int rightPadding = getPaddingRight();
            mTotalLineWidth = width - mOffsetLeft - rightPadding;
            mYLineHeight = (mTotalLineWidth - lineWidth * (mPasswordLength + 1)) / mPasswordLength;
            mXLineHeight = mYLineHeight;
        }
    }

    private boolean isPasswordInputType(int inputType) {
        final int variation =
                inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
        return variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD)
                || variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD)
                || variation
                == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int height = getHeight();
        final int lineWidth = mLineWidth;
        final float lineWidth2 = lineWidth * 1.0f / 2;
        final float offsetLeft = mOffsetLeft;
        final float offsetTop = mOffsetTop;
        final float xLineHeight = mXLineHeight;
        final float totalLineWidth = mTotalLineWidth;
        final float yLineHeight = mYLineHeight;
        final int passwordLength = mPasswordLength;


        canvas.save();
        final Paint linePaint = mLinePaint;
        linePaint.setColor(mLineColor);
        linePaint.setStrokeWidth(lineWidth);
        //绘制横线
        for (int i = 0; i < 2; i++) {
            canvas.drawLine(offsetLeft, lineWidth2 + (yLineHeight + lineWidth) * i + offsetTop,
                    totalLineWidth + offsetLeft, lineWidth2 + (yLineHeight + lineWidth) * i + offsetTop, linePaint);
        }
        //绘制竖线
        for (int i = 0; i < passwordLength + 1; i++) {
            canvas.drawLine(lineWidth2 + (xLineHeight + lineWidth) * i + offsetLeft, offsetTop,
                    lineWidth2 + (xLineHeight + lineWidth) * i + offsetLeft, yLineHeight + lineWidth * 2 + offsetTop, linePaint);
        }
        canvas.restore();

        //绘制密码
        final Paint textPaint = mTextPaint;
        textPaint.setColor(mTextColor.getDefaultColor());
        final float xLineHeight2 = xLineHeight * 1.0f / 2;
        CharSequence pwd = getText();
        if (pwd != null && pwd.length() > 0) {
            final Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float textHeight = fontMetrics.bottom - fontMetrics.top;
            final float baseline = height / 2 - lineWidth2 - textHeight / 2 - fontMetrics.top;
            final float textWidth;
            boolean pwdFlag = !isPasswordInputType(getInputType());
            if (pwdFlag) {
                textWidth = textPaint.measureText(DEFAULT_TRANSFORMATION);
            } else {
                textWidth = textPaint.measureText(pwd.subSequence(0, 1).toString());
            }
            canvas.save();
            for (int i = 0; i < pwd.length(); i++) {
                if (pwdFlag) {
                    canvas.drawText(pwd, i, i + 1,
                            offsetLeft + lineWidth + xLineHeight2 + (xLineHeight + lineWidth) * i - textWidth / 2, baseline, textPaint);
                } else {
                    canvas.drawText(DEFAULT_TRANSFORMATION, 0, 1,
                            offsetLeft + lineWidth + xLineHeight2 + (xLineHeight + lineWidth) * i - textWidth / 2, baseline, textPaint);
                }
            }
            canvas.restore();
        }
        if (shouldBlink()) {
            final float cursorWidth2 = getCursorWidth() * 1.0f / 2;
            final float cursorHeight2 = getCursorHeight() * 1.0f / 2;
            int position = 0;
            if (pwd != null) {
                position = pwd.length();
            }
            if (position < passwordLength) {
                canvas.save();
                mCursorRect = new Rect();
                mCursorRect.left = (int) (offsetLeft + lineWidth + xLineHeight2 - cursorWidth2 + position * (xLineHeight + lineWidth));
                mCursorRect.top = (int) (height / 2 - cursorHeight2);
                mCursorRect.right = (int) (offsetLeft + lineWidth + xLineHeight2 + cursorWidth2 + position * (xLineHeight + lineWidth));
                mCursorRect.bottom = (int) (height / 2 + cursorHeight2);
                mCursorBitmapDrawable.setBounds(mCursorRect);
                mCursorBitmapDrawable.draw(canvas);
                canvas.restore();
            }
        }

    }

    public int getCursorHeight() {
        int height = mCursorHeight;
        if (height < 1) {
            if (mCursorBitmapDrawable != null)
                height = mCursorBitmapDrawable.getIntrinsicHeight();
        }
        return height;
    }

    public int getCursorWidth() {
        int width = mCursorWidth;
        if (width < 1) {
            if (mCursorBitmapDrawable != null)
                width = mCursorBitmapDrawable.getIntrinsicWidth();
        }
        return width;
    }

    public void setLineColor(@ColorInt int lineColor) {
        if (lineColor != mLineColor) {
            this.mLineColor = lineColor;
            invalidate();
        }
    }

    public void setLineWidth(int lineWidth) {
        if (lineWidth != mLineWidth) {
            this.mLineWidth = lineWidth;
            requestLayout();
            invalidate();
        }
    }

    @Override
    public boolean getFreezesText() {
        return true;
    }

    @Override
    protected boolean getDefaultEditable() {
        return true;
    }

    @Override
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }

    @Override
    public int length() {
        return super.length();
    }

    @Override
    public void setLines(int lines) {
        if (lines > 1) {
            lines = 1;
        }
        super.setLines(lines);
    }

    @Override
    public void setFilters(InputFilter[] filters) {
        if (mPasswordLength > 0) {
            filters = new InputFilter[]{new InputFilter.LengthFilter(mPasswordLength)};
        } else {
            filters = new InputFilter[0];
        }
        super.setFilters(filters);
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        color = Color.TRANSPARENT;
        super.setTextColor(color);
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        mTextColor = colors;
        colors = ColorStateList.valueOf(Color.TRANSPARENT);
        super.setTextColor(colors);
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
    }

    @Override
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;
        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();
        float s = TypedValue.applyDimension(
                unit, size, r.getDisplayMetrics());
        if (s != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(s);
        }
        super.setTextSize(unit, size);
    }

    @Override
    public TextPaint getPaint() {
        return mTextPaint;
    }

    @Override
    public void setCursorVisible(boolean visible) {
        super.setCursorVisible(false);//设置系统的cursor为false
        if (mCursorVisiable != visible) {
            mCursorVisiable = visible;
            invalidate();
        }
    }

    public void setCursorDrawableRes(@DrawableRes int resId) {
        if (resId != mCursorDrawableRes) {
            this.mCursorDrawableRes = resId;
            setCursorDrawable(ContextCompat.getDrawable(getContext(), resId));
        }
    }

    public void setCursorDrawable(@Nullable Drawable d) {
        if (mCursorDrawable != d) {
            updateCursorDrawable(d);
        }
    }

    private void updateCursorDrawable(Drawable d) {
        mCursorDrawable = d;
        if (d != null) {
            int width = d.getIntrinsicWidth();
            int height = d.getIntrinsicHeight();
            width = width > 0 ? width : mCursorWidth;
            height = height > 0 ? height : mCursorHeight;
            // 取 drawable 的颜色格式
            Bitmap.Config config = d.getOpacity() != PixelFormat.OPAQUE ?
                    Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
            // 建立对应 bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, config);
            // 建立对应 bitmap 的画布
            Canvas canvas = new Canvas(bitmap);
            //把 drawable 内容画到画布中
            d.setBounds(0, 0, width, height);
            d.draw(canvas);
            if (mCursorWidth > 0 && mCursorHeight > 0) {
                Matrix matrix = new Matrix();
                matrix.postScale(width, mCursorWidth, height, mCursorHeight);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                mCursorBitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            } else {
                mCursorBitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            }
        } else {
            mCursorBitmapDrawable = null;
        }
    }

    private boolean mInvalidateCursorVisible = true;

    private void invalidateCursorPath() {
        if (mCursorBitmapDrawable != null) {
            mCursorBitmapDrawable.setAlpha(mInvalidateCursorVisible ? 255 : 0);
            if (mCursorRect != null) {
                invalidate(mCursorRect.left, mCursorRect.top, mCursorRect.right, mCursorRect.bottom);
            } else {
                invalidate();
            }
        }
    }

    private int dp2px(int dp) {
        float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return null;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                showSoftInput();
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resumeBlink();
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);
        switch (screenState) {
            case View.SCREEN_STATE_ON:
                resumeBlink();
                break;
            case View.SCREEN_STATE_OFF:
                suspendBlink();
                break;
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        mShowCursor = SystemClock.uptimeMillis();
        if (focused) {
            makeBlink();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            if (mBlink != null) {
                mBlink.uncancel();
                makeBlink();
            }
        } else {
            if (mBlink != null) {
                mBlink.cancel();
            }
        }
    }

    private void makeBlink() {
        if (shouldBlink()) {
            mShowCursor = SystemClock.uptimeMillis();
            if (mBlink == null) mBlink = new Blink();
            removeCallbacks(mBlink);
            postDelayed(mBlink, BLINK);
        } else {
            if (mBlink != null) removeCallbacks(mBlink);
        }
    }

    private void suspendBlink() {
        if (mBlink != null) {
            mBlink.cancel();
        }
    }

    private void resumeBlink() {
        if (mBlink != null) {
            mBlink.uncancel();
            makeBlink();
        }
    }

    private class Blink implements Runnable {

        private boolean mCancelled;

        @Override
        public void run() {
            if (mCancelled) {
                return;
            }
            removeCallbacks(this);
            if (shouldBlink()) {
                if (getLayout() != null) {
                    mInvalidateCursorVisible = !mInvalidateCursorVisible;
                    invalidateCursorPath();
                }
                postDelayed(this, BLINK);
            }
        }

        void cancel() {
            if (!mCancelled) {
                removeCallbacks(this);
                mCancelled = true;
            }
        }

        void uncancel() {
            mCancelled = false;
        }
    }

    /**
     * @return True when the TextView isFocused and has a valid zero-length selection (cursor).
     */
    private boolean shouldBlink() {
        if (!mCursorVisiable ||
                !isFocused() ||
                mCursorBitmapDrawable == null) return false;
        return true;
    }

    public void showSoftInput() {
        try {
            InputMethodManager imm = getInputMethodManager();
            requestFocus();
            imm.showSoftInput(this, 0);
        } catch (Exception e) {
            Log.d(TAG, "showSoftInput: " + e);
        }
    }

    public InputMethodManager getInputMethodManager() {
        if (mInputMethodManager == null) {
            mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        return mInputMethodManager;
    }
}

