package com.example.exphero1;

import android.view.View;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;

public class DrawingView extends View
{
	private Path drawPath;
	private Paint drawPaint, drawPaint2, canvasPaint;

	private int paintColor = 0xFF58ED3E;
    private int paintColor2 = 0xFFd60000;

    private Canvas drawCanvas;

    private Bitmap canvasBitmap;
    
    private int W = 1200;
    private int H = 800;
    private float BeginX = W/2;
    private float BeginY = H/2;
	
	
    public DrawingView(Context context, AttributeSet attrs){
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
		
	    Log.e("Drawing.status", "Size changed");
		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
	//draw view
	    Log.e("Drawing.status", "onDraw called");
		canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
		canvas.drawPath(drawPath, drawPaint);
	}
	
    public void Origin(float X, float Y) {
	    Log.e("Drawing.status", "Origin: "+X+", "+Y);
        BeginX = X;
        BeginY = Y;
    }
    public void Reset(){
        drawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
        invalidate();
    }
	public void DrawLine(float EndX, float EndY, boolean Collision) {
		EndX = W/2+EndX;
		EndY = H/2+EndY;
	    Log.e("Drawing.status", "Line: "+BeginX+", "+BeginY+", "+EndX+", "+EndY+", "+Collision);
        double Distance = Math.sqrt((EndX-BeginX)*(EndX-BeginX)+(EndY-BeginY)*(EndY-BeginY));
	    drawPath.moveTo(BeginX, BeginY);
	    drawPath.lineTo(EndX, EndY);
	    drawCanvas.drawPath(drawPath, drawPaint);
	    drawPath.reset();
        if(Collision) {
            drawPath.moveTo(Math.round(EndX + ((EndX - BeginX) * 7.5 / Distance)), (Math.round(EndY + ((EndY - BeginY) * 7.5 / Distance))));
            drawPath.lineTo(Math.round(EndX + ((EndX - BeginX) * 10 / Distance)), (Math.round(EndY + ((EndY - BeginY) * 10 / Distance))));
            drawCanvas.drawPath(drawPath, drawPaint2);
            drawPath.reset();
        }
        BeginX = EndX;
        BeginY = EndY;
        invalidate();
	}
}

