package com.example.exphero1;


import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import orbotix.robot.base.*;
import orbotix.robot.sensor.DeviceSensorsData;
import orbotix.robot.sensor.LocatorData;
import orbotix.robot.widgets.CalibrationImageButtonView;
import orbotix.sphero.CollisionListener;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.LocatorListener;
import orbotix.sphero.SensorFlag;
import orbotix.sphero.SensorListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.connection.SpheroConnectionView;
import orbotix.sphero.PersistentOptionFlags;

public class MainActivity extends Activity {

    // Settings
    private int CollisionSpeed = 15;
    private int CalibrationDistance = 15;
    private int HarassMinSpeed = 8;
    private int HarassWantedSpeed = 8;
    private float HarassChangeSpeed = 0.25f;
    private float HarassMaxAccel = 50;
	private static final String TAG = "Exphero v1.1";
	
	private SpheroConnectionView mSpheroConnectionView;
	private Sphero mRobot = null;
	private RelativeLayout ConnectionOverlay;
	private boolean justErase = false, connected = false;
    private TextView StatusBar, ConnectionText, LogText, FastText;

    private DrawingView Drawing;
    private CircleView Circle;
    private ImageView ImgSphero;

    private CalibrationImageButtonView mCalibrationImageButtonView;
    private CalibrationView mCalibrationView;

    protected float heading=0;
    private float nextAngle = 0;

    private int oldX=0,oldY=0,firstX=0,firstY=0;
    private boolean FirstLocation = false, Calibrating = false, MeasureCollision = false, HadCollision = false, Calibrated = false;
    private float Speed = 0, Direction = 0, Deviation = 0;
    private double Angle;

    // Store accelerations
    private float AccelX,AccelY,AccelZ,Accel;

    // Display found corners:
    private float lastFastAngle = 0;
    private boolean LastCorner = true;

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
                    Speed = (float) Math.sqrt(Math.pow(locatorData.getVelocityX(),2f)+Math.pow(locatorData.getVelocityY(),2f));
                    Direction = (float) ((Math.atan2(locatorData.getVelocityX(), locatorData.getVelocityY()) - Angle) * 180 / Math.PI);
                    Deviation = (float) Speed>=HarassMinSpeed?(Direction-heading):45;
                    if(Speed>CollisionSpeed&&!HadCollision) {
                        MeasureCollision = true;
                    } else if(HadCollision) {
                        Circle.DrawLine(heading, Speed > HarassMinSpeed ? Direction : 0, Speed > HarassMinSpeed);
                        handleHarass(tmpX - firstX, tmpY - firstY);
                        setHarass(Speed);
                    } else {
                        FastLog(MeasureCollision + " V: " + Math.round(Speed));
                        Circle.DrawLine(heading, Speed > HarassMinSpeed ? Direction : 0, Speed > HarassMinSpeed);
                        handleRoll(tmpX - firstX, tmpY - firstY, false);
                    }
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
    private SensorListener mSensorListener = new SensorListener() {
        @Override
        public void sensorUpdated(DeviceSensorsData ballData) {
            AccelX = (float) ballData.getAccelerometerData().getFilteredAcceleration().x;
            AccelY = (float) ballData.getAccelerometerData().getFilteredAcceleration().y;
            AccelZ = (float)ballData.getAccelerometerData().getFilteredAcceleration().z;
            Accel = (float) Math.sqrt(Math.pow(AccelX,2)+Math.pow(AccelY,2));
        }

    };
    private final CollisionListener mCollisionListener = new CollisionListener() {
        @Override
        public void collisionDetected(CollisionDetectedAsyncData collisionData) {
            if(MeasureCollision) {
                lastFastAngle = heading;
                setToHarass();
                addMessageToStatus("Collision");
                addMessageToStatus("Power X: " + collisionData.getImpactPower().x);
                addMessageToStatus("Power Y: " + collisionData.getImpactPower().y);
                handleRoll(oldX, oldY, true);
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
        Circle = (CircleView) findViewById(R.id.Circle);
        ImgSphero = (ImageView) findViewById(R.id.CircleSphero);

		mSpheroConnectionView = (SpheroConnectionView) findViewById(R.id.sphero_connection_view);
		mSpheroConnectionView.setSingleSpheroMode(true);

		ConnectStatus("Searching Sphero...");
		mSpheroConnectionView.addDiscoveryListener(new DiscoveryListener() {

			@Override
			public void discoveryComplete(List<Sphero> paramList) {
                ConnectStatus("Searching Sphero...");
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
                mRobot.getSensorControl().addSensorListener(mSensorListener, SensorFlag.ACCELEROMETER_NORMALIZED);
                mRobot.getCollisionControl().addCollisionListener(mCollisionListener);
                mRobot.getCollisionControl().startDetection(45, 45, 100, 100, 100);
                mRobot.getSensorControl().setRate(30);
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
            ForceStop();
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
    protected void setToRoll() {
        ImgSphero.setImageResource(R.drawable.icon_sphero);
        MeasureCollision = false;
        HadCollision = false;
    }
    private void handleRoll(int X, int Y, boolean Collision) {
        oldX = (int) Math.round(X);
        oldY = (int) Math.round(Y);
        float newX = PosCorrection((float) (-X*Math.cos(Angle)-Y*Math.sin(Angle)), true);
        float newY = PosCorrection((float) (-X*Math.sin(Angle)+Y*Math.cos(Angle)), false);
        Drawing.DrawLine(newX, newY, Collision);
    }
	public void setRoll() {
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
    protected void setToHarass() {
        ImgSphero.setImageResource(R.drawable.icon_sphero_red);
        MeasureCollision = false;
        HadCollision = true;
    }
    private void handleHarass(int X, int Y) {
        oldX = (int) Math.round(X);
        oldY = (int) Math.round(Y);
        float newX = PosCorrection((float) (-X*Math.cos(Angle)-Y*Math.sin(Angle)), true);
        float newY = PosCorrection((float) (-X*Math.sin(Angle)+Y*Math.cos(Angle)), false);
        Drawing.DrawLine(newX, newY, false);
    }
    public void setHarass(float v) {
        float correction = 0;
        if(false) {
            if (v < HarassWantedSpeed - 7) {
                correction = 4 * HarassChangeSpeed;
                if (Math.abs(lastFastAngle - (heading + correction)) >= 45) {
                    if (!LastCorner) {
                        LastCorner = true;
                        addMessageToStatus("Corner found");
                    }
                } else {
                    LastCorner = false;
                }
            } else if (v > HarassWantedSpeed + 5) {
                correction = -15 * HarassChangeSpeed;
                lastFastAngle = (heading + correction);
                LastCorner = false;
            } else {
                correction = (float) -(Math.pow(v - HarassWantedSpeed, 3) / 8000 * Math.pow(v, 2)) * HarassChangeSpeed;
                lastFastAngle = (heading + correction);
                LastCorner = false;
            }
        } else {
            correction = -HarassChangeSpeed*(v-HarassWantedSpeed)/2;
        }
        if(HarassMaxAccel<Accel) {
            correction = correction + (Accel-HarassMaxAccel)*10;
        }
        heading = (heading+correction);
        FastLog(Accel + " X: " + AccelX + " Y: " + AccelY + " Z: " + AccelZ);
        addMessageToStatus(Accel + " X: " + AccelX + " Y: " + AccelY + " Z: " + AccelZ);
//        addMessageToStatus(v+", "+correction + " => "+heading);
        mRobot.setColor(255, 0, 0);
        mRobot.drive(heading, 0.2f);
    }

    public void ForceStop() {
        mRobot.stop();
        MeasureCollision = false;
        HadCollision = false;
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
        setToRoll();
        setRoll();
    }
    public void StopButton(View view) {
        addMessageToStatus("Stop Sphero");
        ForceStop();
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        if(mCalibrationView.isCalibrating()) {
            mRobot.setColor(0, 0, 0);
            Calibrating = true;
            setToRoll();
        } else if(Calibrating) {
            MeasureCollision = false;
            setToRoll();
            Calibrating = false;
            Calibrated = false;
            FirstLocation = false;
            Drawing.Reset();
            nextAngle = 0;
        }
        return super.dispatchTouchEvent(event);
    }
}