package com.fra.frigoplanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class RectView extends View {
    private Rect rectToDraw;
    private Paint paint;

    public RectView(Context context) {
        super(context);
        init();
    }

    public RectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f); // Thickness
    }

    // Update the rect
    public void setRect(Rect rect) {
        this.rectToDraw = rect;
        invalidate(); // Force redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rectToDraw != null) {
            canvas.drawRect(rectToDraw, paint);
        }
    }
}

