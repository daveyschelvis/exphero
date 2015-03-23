package com.example.exphero1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CircleView extends View
{
	private Path drawPath;
	private Paint drawPaint, drawPaint2, canvasPaint;

	private int paintColor = 0xFF58ED3E;
    private int paintColor2 = 0xFFFF0000;

    private Canvas drawCanvas;

    private Bitmap canvasBitmap;

    private int W = 600;
    private int H = 600;
    private float BeginX = W/2;
    private float BeginY = H/2;

    private float Length1 = 200;
    private float Length2 = 150;


    public CircleView(Context context, AttributeSet attrs){
	    super(context, attrs);
	    Log.e("Drawing.status", "Starting");
	    setupDrawing();
	}
	private void setupDrawing(){
		drawPath = new Path();
		
		drawPaint = new Paint();
		drawPaint.setColor(paintColor);
		drawPaint.setAntiAlias(false);
		drawPaint.setStrokeWidth(15);
		drawPaint.setStyle(Paint.Style.STROKE);
		drawPaint.setStrokeJoin(Paint.Join.ROUND);
		drawPaint.setStrokeCap(Paint.Cap.ROUND);
		
		drawPaint2 = new Paint();
		drawPaint2.setColor(paintColor2);
		drawPaint2.setAntiAlias(false);
		drawPaint2.setStrokeWidth(5);
		drawPaint2.setStyle(Paint.Style.STROKE);
		drawPaint2.setStrokeJoin(Paint.Join.BEVEL);
		drawPaint2.setStrokeCap(Paint.Cap.BUTT);
	
		canvasPaint = new Paint(Paint.DITHER_FLAG);
		
	    Log.e("Drawing.status", "Setup");
	}
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	//view given size
		super.onSizeChanged(w, h, oldw, oldh);
		
		canvasBitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
		drawCanvas = new Canvas(canvasBitmap);
		
	    Log.e("Circle.status", "Size changed");
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
	//draw view
	    Log.e("Circle.status", "onDraw called");
		canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
		canvas.drawPath(drawPath, drawPaint);
	}
    public void Reset(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }
	public void DrawLine(float Angle1, float Angle2, Boolean DrawAngle2) {
        Reset();
        float EndX = (float) (BeginX+Math.sin(Angle1)*Length1);
        float EndY = (float) (BeginY-Math.cos(Angle1)*Length1);
	    Log.e("Circle.status", "Line: "+EndX+", "+EndY);
	    drawPath.moveTo(BeginX, BeginY);
	    drawPath.lineTo(EndX, EndY);
	    drawCanvas.drawPath(drawPath, drawPaint);
	    drawPath.reset();
        if(DrawAngle2) {
            float EndX2 = (float) (BeginX+Math.sin(Angle2)*Length2);
            float EndY2 = (float) (BeginY-Math.cos(Angle2)*Length2);
            Log.e("Circle.status", "Line2: "+EndX2+", "+EndY2);
            drawPath.moveTo(BeginX, BeginY);
            drawPath.lineTo(EndX2, EndY2);
            drawCanvas.drawPath(drawPath, drawPaint2);
            drawPath.reset();
        }
        invalidate();
	}
}

