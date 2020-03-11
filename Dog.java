import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import lejos.hardware.BrickFinder;
import lejos.hardware.Button;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.BaseRegulatedMotor;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.subsumption.Arbitrator;
import lejos.robotics.subsumption.Behavior;

public class Dog {

    private Socket socket;
    private DataInputStream in;
    private final EV3 ev3 = (EV3) BrickFinder.getLocal();
    private final TextLCD lcd = ev3.getTextLCD();
    private final Keys keys = ev3.getKeys();
    String btIPPrefix = "10.0.1.";
    int ipDomain = 0;
    int ipEnd = 0;
    final static int MODE_ROW = 0;
    final static int IP_ROW = 2;
    final static int STATUS_ROW = 4;
    final static int VALUE_POS = 10;
	
	final static float WHEEL_DIAMETER = 56;
	final static float AXLE_LENGTH = 190;
	final static float ANGULAR_SPEED = 100;
	final static float SPEED = 50;
	
	String getIP() {
        return btIPPrefix + ipEnd;
    }
	
	void drawRow(String string, int row) {
        lcd.clear(row);
        lcd.drawString(string, 0, row);        
    }
        
    void setMode(boolean isBluetooth) {
        drawRow("Bluetooth", MODE_ROW);                
        drawRow(getIP(), IP_ROW);
    }
    
    void init () {
        drawRow("IP:", IP_ROW-1);
        drawRow("Status:", STATUS_ROW-1);
        for(;;) {
            drawRow("Setting IP", STATUS_ROW);
            int but = Button.waitForAnyPress();
            if( (but & Button.ID_ESCAPE) != 0 ) {
                System.exit(0);
            }
            if( (but & Button.ID_ENTER) != 0 ) {
                if( connect() ) {                
                    try {
                        run();
                    } catch (IOException e) {
                        drawRow("Disconnected",STATUS_ROW);
                        drawRow("E:" + e.getMessage(), STATUS_ROW+1);
                    }
                }
            } else if( (but & Button.ID_UP) != 0 ) {
                ipEnd = Math.min(ipEnd+1,99 + ipDomain); 
            } else if( (but & Button.ID_DOWN) != 0 ) {
                ipEnd = Math.max(ipEnd-1,ipDomain); 
            }
        }
    }
    
    private void run() {
		//IDK
	}

	boolean connect () {
        try {
            drawRow("Connecting...", STATUS_ROW);
            socket = new Socket(getIP(), 1234);
            in = new DataInputStream(socket.getInputStream());
            drawRow("Connected", STATUS_ROW);
            return true;
        } catch (IOException e) {
            drawRow("E:" + e.getMessage(), STATUS_ROW);
            keys.waitForAnyPress();
        }        
        return false;
    }
	boolean disconnect () {
        try {
            drawRow("Disconnecting", STATUS_ROW);
            socket.close();
            drawRow("Disconnected", STATUS_ROW);
            return true;
        } catch (IOException e) {
            drawRow("E:" + e.getMessage(), STATUS_ROW);
            keys.waitForAnyPress();
        }        
        return false;
    }
	
	public static void main(String[] args) {
		
		EV3UltrasonicSensor us = new EV3UltrasonicSensor(SensorPort.S1);
		SampleProvider distance = (SampleProvider) us.getDistanceMode();
		
//		EV3ColorSensor cs = new EV3ColorSensor(SensorPort.S2);
//		SensorMode colour = cs.getAmbientMode(); 
//		
//		NXTSoundSensor ss = new NXTSoundSensor(SensorPort.S3);
//		SampleProvider sound = ss.getDBAMode();
		
		BaseRegulatedMotor mLeft = new EV3LargeRegulatedMotor(MotorPort.A);
		Wheel wLeft = WheeledChassis.modelWheel(mLeft, WHEEL_DIAMETER).offset(-AXLE_LENGTH / 2);
		BaseRegulatedMotor mRight = new EV3LargeRegulatedMotor(MotorPort.B);
		Wheel wRight = WheeledChassis.modelWheel(mRight, WHEEL_DIAMETER).offset(AXLE_LENGTH / 2);
		Chassis chassis = new WheeledChassis(new Wheel[] { wRight, wLeft}, WheeledChassis.TYPE_DIFFERENTIAL);
		MovePilot pilot = new MovePilot(chassis);
		pilot.setLinearSpeed(SPEED);
		
		Arbitrator ab = new Arbitrator(new Behavior[] {new Explorer(distance, pilot), new Follower(distance), new Fetcher(mLeft, mRight)});
		
		Button.ENTER.waitForPressAndRelease();
		
		ab.go();
		
		us.close();
//		cs.close();
//		ss.close();
	}
}

