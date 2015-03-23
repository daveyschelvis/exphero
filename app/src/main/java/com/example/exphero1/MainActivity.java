package com.example.exphero1;


import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import orbotix.robot.base.*;
import orbotix.robot.sensor.LocatorData;
import orbotix.robot.widgets.CalibrationImageButtonView;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.ConfigurationControl;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.LocatorListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.sphero.PersistentOptionFlags;

public class MainActivity extends Activity {
    // Settings

    private int CollisionSpeed = 20;
    private int CalibrationDistance = 20;
	private static final String TAG = "Exphero v1.0";
	
	private SpheroConnectionView mSpheroConnectionView;
	private Sphero mRobot = null;
	private RelativeLayout ConnectionOverlay;
	private boolean justErase = false, connected = false;
    private TextView StatusBar, ConnectionText, LogText, FastText;
    
    private DrawingView Drawing;

    private CalibrationImageButtonView mCalibrationImageButtonView;
    private CalibrationView mCalibrationView;
    private float nextAngle = 0;
    private int oldX=0,oldY=0,firstX=0,firstY=0;
    private boolean FirstLocation = false, Calibrating = false, HasCollisions = false, HadCollision = false, Calibrated = false;
    private double Speed = 0;
    private double Angle;

    private LocatorListener mLocatorListener = new LocatorListener() {
        @Override
        public void onLocatorChanged(LocatorData locatorData) {
            int tmpX = Math.round(locatorData.getPositionX());
            int tmpY = Math.round(locatorData.getPositionY());
            if(locatorData != null && locatorData.getPositionX()!=0) {
                if(!FirstLocation) {
                    FirstLocation = true;
                    firstX = tmpX;
                    firstY = tmpY;
                    addMessageToStatus("First Location: "+firstX+" "+firstY);
                }
                if(Calibrated) {
                    Speed = Math.sqrt(Math.pow(locatorData.getVelocityX(),2f)+Math.pow(locatorData.getVelocityY(),2f));
                    if(Speed>CollisionSpeed) {
                        HasCollisions = true;
                    }
                    FastLog(HasCollisions+" V: "+Math.round(Speed));
                    HandlePosition(tmpX-firstX, tmpY-firstY, HadCollision);
                    HadCollision = false;
                } else {
                    float Distance = (float) Math.sqrt(Math.pow(firstX-tmpX,2f)+Math.pow(firstY-tmpY,2f));
                    FastLog("S: "+Distance);
                    if(Distance>=CalibrationDistance) {
                        Angle = -Math.atan2(tmpX-firstX,tmpY-firstY);
                        addMessageToStatus("Calibrated: "+Angle*180/Math.PI);
                        addMessageToStatus("Dx: "+(tmpX-firstX));
                        addMessageToStatus("Dy: "+(tmpY-firstY));
                        Calibrated = true;
                    }
                }
            }
        }
    };
    private final CollisionListener mCollisionListener = new CollisionListener() {
        @Override
        public void collisionDetected(CollisionDetectedAsyncData collisionData) {
            if(HasCollisions) {
                HasCollisions = false;
                HadCollision = true;
                mRobot.stop();
                addMessageToStatus("Collision");
                addMessageToStatus("Power X: " + collisionData.getImpactPower().x);
                addMessageToStatus("Power Y: " + collisionData.getImpactPower().y);
                HandlePosition(oldX, oldY, true);
            }
        }
    };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);

        mCalibrationImageButtonView = (CalibrationImageButtonView) findViewById(R.id.calibration_image_button);
        mCalibrationImageButtonView.setCalibrationView(mCalibrationView);

        mCalibrationImageButtonView.setRadius(100);
        mCalibrationImageButtonView.setOrientation(CalibrationView.CalibrationCircleLocation.ABOVE);
		
		StatusBar = (TextView) findViewById(R.id.TitleBar);
        FastText = (TextView) findViewById(R.id.FastText);
		LogText = (TextView) findViewById(R.id.LogText);
		LogText.setMovementMethod(new ScrollingMovementMethod());
		ConnectionText = (TextView) findViewById(R.id.ConnectionLoading); 
		ConnectionOverlay = (RelativeLayout) findViewById(R.id.ConnectionOverlay);
		Drawing = (DrawingView) findViewById(R.id.Drawing);
		
		mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
		mSpheroConnectionView.setSingleSpheroMode(true);

		ConnectStatus("Searching Sphero...");
		mSpheroConnectionView.addDiscoveryListener(new DiscoveryListener() {

			@Override
			public void discoveryComplete(List<Sphero> paramList) {
			}

			@Override
			public void onBluetoothDisabled() {
				ConnectStatus("Bluetooth disabled");
			}

			@Override
			public void onFound(List<Sphero> paramList) {
				Sphero s=paramList.get(0);
	            RobotProvider.getDefaultProvider().connect(s);
				ConnectStatus("Connecting to "+s.getName());
			}

            private Robot robot = null;
            public void setRobot(Robot robot){
                this.robot = robot;
            }

            public void onConnected(Robot robot) {

                mRobot = (Sphero) robot;
                setRobot(mRobot);
                mCalibrationView.setRobot(mRobot);
                addMessageToStatus("executed onConnect in discovery");

            }


        });
		RobotProvider.getDefaultProvider().addConnectionListener(new ConnectionListener() {
            private Robot robot = null;
            public void setRobot(Robot robot){
                this.robot = robot;
            }
			@Override
			public void onConnected(Robot robot) {
				connected = true;
				ConnectStatus("Connected");
				mRobot = (Sphero) robot;
                setRobot(mRobot);
                mRobot.getConfiguration().setPersistentFlag(PersistentOptionFlags.EnableMotionTimeout, false);
                mRobot.setColor(0, 255, 0);
                mRobot.getSensorControl().addLocatorListener(mLocatorListener);
                mRobot.getCollisionControl().addCollisionListener(mCollisionListener);
                mRobot.getCollisionControl().startDetection(45, 45, 100, 100, 100);
                mRobot.getSensorControl().setRate(5);
                mCalibrationView.setRobot(mRobot);
				ConnectionOverlay(false);
			}

			@Override
			public void onConnectionFailed(Robot robot) {
				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						ConnectionOverlay(true);
						connected = false;
						ConnectStatus("Connection failed");
					}
				});
			}

			@Override
			public void onDisconnected(Robot robot) {
				MainActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						ConnectionOverlay(true);
						connected = false;
						ConnectStatus("Disconnected");
						mSpheroConnectionView.startDiscovery();
					}
				});
			}
		
		});
	}
		
	@Override
    protected void onResume() {
        super.onResume();
		ConnectionOverlay(true);
		ConnectStatus("Searching Sphero...");
        mSpheroConnectionView.startDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
		ConnectionOverlay(true);
		if(connected) {
			ConnectStatus("Connection closed");
            mRobot.getSensorControl().removeLocatorListener(mLocatorListener);
            mRobot.getCollisionControl().removeCollisionListener(mCollisionListener);
			RobotProvider.getDefaultProvider().disconnectControlledRobots();
	        RobotProvider.getDefaultProvider().removeDiscoveryListeners();
			connected = false;
		}
    }
	
	protected void ConnectionOverlay(boolean Show) {
		if(!Show) {
			ConnectionOverlay.setVisibility(View.GONE);
		} else {
			ConnectionOverlay.setVisibility(View.VISIBLE);
		}
	}
	public void SetRoll() {
        float heading=0;
        if(false) {
            heading = (float) Math.random() * 360;
        } else {
            heading = nextAngle;
            nextAngle += 90;
            nextAngle = nextAngle%360;
        }
        Log.e("Exphero.roll", "Set angle: "+heading);
        addMessageToStatus("Set angle: " + heading);
		mRobot.setColor(0, 0, 255);
		mRobot.drive(heading, .3f);
	}
	private void HandlePosition(int X, int Y, boolean Collision) {
        oldX = (int) Math.round(X);
        oldY = (int) Math.round(Y);
		float newX = PosCorrection((float) (-X*Math.cos(Angle)-Y*Math.sin(Angle)), true);
		float newY = PosCorrection((float) (-X*Math.sin(Angle)+Y*Math.cos(Angle)), false);
        double Distance = Math.sqrt(newX*newX+newY*newY);
		Drawing.DrawLine(newX, newY, Collision);
	}
	private float PosCorrection(float D, boolean isX) {
		float newD = 0;
        if(false) {
            newD = (Math.abs(D)+43*(Math.abs(D)-200)/1000)*(D/Math.abs(D));
        } else {
            newD = -D;
        }
		return newD;
	}
    private void addMessageToStatus(String msg) {
        Log.e("Exphero.status", msg);
        LogText.append(msg + "\n");
        final int scrollAmount = LogText.getLayout().getLineTop(LogText.getLineCount()) - LogText.getHeight();
        if (scrollAmount > 0)
            LogText.scrollTo(0, scrollAmount);
        else
            LogText.scrollTo(0, 0);
    }
    private void ChangeStatus(String msg) {
        Log.d(TAG, "Status:" + msg);
        StatusBar.setText(msg);
    }
    private void FastLog(String msg) {
        FastText.setText(msg);
    }
    private void ConnectStatus(String msg) {
		Log.d(TAG, "Conntent Status:" + msg);
        ConnectionText.setText(msg + "\n");
    }
    public void StartButton(View view) {
        addMessageToStatus("Start Sphero");
        SetRoll();
    }
    public void StopButton(View view) {
        addMessageToStatus("Stop Sphero");
        mRobot.stop();
        HasCollisions = false;
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        if(mCalibrationView.isCalibrating()) {
            mRobot.setColor(0, 0, 0);
            Calibrating = true;
            HasCollisions = true;
        } else if(Calibrating) {
            Calibrating = false;
            Calibrated = false;
            FirstLocation = false;
            Drawing.Reset();
            nextAngle = 0;
        }
        return super.dispatchTouchEvent(event);
    }
}