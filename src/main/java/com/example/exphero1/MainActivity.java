package com.example.exphero1;


import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;

import orbotix.robot.base.OrbBasicControl;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.widgets.CalibrationImageButtonView;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.Sphero;
import orbotix.view.calibration.CalibrationView;
import orbotix.view.connection.SpheroConnectionView;


public class MainActivity extends Activity {
	private static final String TAG = "Exphero v1.0";
	
	private SpheroConnectionView mSpheroConnectionView;
	private Sphero mRobot;
	private RelativeLayout ConnectionOverlay;
	protected boolean OrbActive = false;
	
    private TextView StatusBar, ConnectionText;

    /** One-Touch Calibration Button */
    private CalibrationImageButtonView mCalibrationImageButtonView;

    /** Calibration View widget */
    private CalibrationView mCalibrationView;
    
    private DrawingView Drawing;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);



        mCalibrationView = (CalibrationView) findViewById(R.id.calibration_view);

        // Initialize calibrate button view where the calibration circle shows above button
        // This is the default behavior
        mCalibrationImageButtonView = (CalibrationImageButtonView) findViewById(R.id.calibration_image_button);
        mCalibrationImageButtonView.setCalibrationView(mCalibrationView);
        // You can also change the size and location of the calibration views (or you can set it in XML)
        mCalibrationImageButtonView.setRadius(100);
        mCalibrationImageButtonView.setOrientation(CalibrationView.CalibrationCircleLocation.ABOVE);
		
		StatusBar = (TextView) findViewById(R.id.TitleBar);
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
                // Set Robot
                mRobot = (Sphero) robot; // safe to cast for now
                //Set connected Robot to the Controllers
                setRobot(mRobot);

                // Make sure you let the calibration view knows the robot it should control
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
                addMessageToStatus("Connected");
				mRobot = (Sphero) robot;
                setRobot(mRobot);
                mCalibrationView.setRobot(mRobot);
		    	LoadOrb("exphero");
				ChangeStatus("Ready!");
				ConnectionOverlay(false);
				mRobot.getOrbBasicControl().addEventListener(new OrbBasicControl.EventListener() {
					
                    @Override
                    public void onEraseCompleted(boolean success) {
                        String successStr = (success) ? "Success" : "Failure";
                        Log.e("Exphero.orb", "Program Erased: "+successStr);
                        if(success) {
                            Log.e("Exphero.orb", "Load program");
                            mRobot.getOrbBasicControl().loadProgram();
                        }
                    }

                    @Override
                    public void onLoadProgramComplete(boolean success) {
                        String successStr = (success) ? "Success" : "Failure";
                        Log.e("Exphero.orb", "Program Loaded: "+successStr);
                        if(success) {
                            Log.e("Exphero.orb", "Start program");
                            mRobot.getOrbBasicControl().executeProgram();
                            Log.e("Exphero.orb", "Program executed");
                    		SetRoll();
                        }
                    }

                    @Override
                    public void onPrintMessage(String message) {
                    	String[] parts = message.split(",");
                    	if(parts[0].equals("Position")) {
                    		mRobot.setColor(255, 0, 0);
                            addMessageToStatus("Location: " + parts[1] + ", " + parts[2]);
                            parts[1] = parts[1].replaceAll("\\D+","");
                            parts[2] = parts[2].replaceAll("\\D+","");
                            HandlePosition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    	} else if(parts[0].equals("Accelone")) {
                                addMessageToStatus("Accelone: " + parts[1]);
                    	} else {
                            addMessageToStatus("Message: " + message);
                    	}
                    }

                    @Override
                    public void onErrorMessage(String message) {
                        Log.e("Exphero.orb", "Error: "+message);
                        addMessageToStatus("ERROR: " + message);
                    }

                    @Override
                    public void onErrorByteArray(byte[] bytes) {
                        //To change body of implemented methods use File | Settings | File Templates.
                    }
                });
			}

			@Override
			public void onConnectionFailed(Robot robot) {
//				ConnectionOverlay(true);
				ConnectStatus("Connection failed");
			}

			@Override
			public void onDisconnected(Robot robot) {
				//ConnectionOverlay(true);
				OrbActive = false;
//				ConnectStatus("Disconnected");
				mSpheroConnectionView.startDiscovery();
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
		ConnectStatus("Connection closed");
		RobotProvider.getDefaultProvider().disconnectControlledRobots();
        RobotProvider.getDefaultProvider().removeDiscoveryListeners();
    }
	
	protected void ConnectionOverlay(boolean Show) {
		if(!Show) {
			ConnectionOverlay.setVisibility(View.GONE);
		} else {
			ConnectionOverlay.setVisibility(View.VISIBLE);
		}
	}
	protected void LoadOrb(String FileName) {
		Field[] fields = R.raw.class.getFields();
		boolean Found = false;
		int fileInt = 0;
        for (Field f : fields)
            try {
            	if(f.getName().equals(FileName)) {
            		Found = true;
            		fileInt = f.getInt(null);
                    Log.e("Exphero.orb", "Field selected " + f.getName() + ">" + f.getInt(null));
            	}
            } catch (IllegalArgumentException e) {
                Log.e("Exphero.orb", "IllegalArgument " + e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                Log.e("Exphero.orb", "IllegalAccess " + e.getLocalizedMessage());
            }
        if(!Found) {
            Log.e("Exphero.orb", "File not found");
        } else {
            Log.e("Exphero.orb", "File found");
        	try {
                Resources res = getResources();
                InputStream in_s = res.openRawResource(fileInt);
                byte[] program = new byte[in_s.available()];
                in_s.read(program);
                mRobot.getOrbBasicControl().setProgram(program);

            } catch (Exception e) {
                Log.e("Exphero.orb", "Error Decoding Resource");
            }
        }
	}
	public void SetRoll() {
		float heading = (float) Math.random()*360;    	
        Log.e("Exphero.roll", "Set angle: "+heading);
        addMessageToStatus("Set angle: " + heading);
		mRobot.setColor(0, 255, 0);
		mRobot.drive(heading, 0f);
	}
	private void HandlePosition(int X, int Y) {
		float newX = PosCorrection(X, true);
		float newY = PosCorrection(Y, false);
		double Distance = Math.sqrt(newX*newX+newY*newY);
        addMessageToStatus("Corrected location: " + newX + ", " + newY);
        addMessageToStatus("Distance: " + Distance);
		Drawing.DrawLine(newX, newY, true);
		
	}
	private float PosCorrection(int D, boolean isX) {
		float newD = (Math.abs(D)+43*(Math.abs(D)-200)/1000)*(D/Math.abs(D));
		return newD;
	}
    private void addMessageToStatus(String msg) {
        Log.e("Exphero.status", msg);
    }
    private void ChangeStatus(String msg) {
		Log.d(TAG, "Status:" + msg);
        StatusBar.setText(msg + "\n");
    }
    private void ConnectStatus(String msg) {
		Log.d(TAG, "Conntent Status:" + msg);
        ConnectionText.setText(msg + "\n");
    }
    public void StartButton(View view) {
    	addMessageToStatus("Start Orb");
    	if(!OrbActive) {
//    		OrbActive = true;
            Log.e("Exphero.orb", "Abort program");
            mRobot.getOrbBasicControl().abortProgram();
            Log.e("Exphero.orb", "Erase storage");
            mRobot.getOrbBasicControl().eraseStorage();
    	} else {
    		SetRoll();
    	}
    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        mCalibrationView.interpretMotionEvent(event);
        return super.dispatchTouchEvent(event);
    }
}