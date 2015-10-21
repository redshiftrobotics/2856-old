package org.usfirst.ftc.exampleteam.yourcodehere;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.qualcomm.ftcrobotcontroller.FtcRobotControllerActivity;
import com.qualcomm.ftcrobotcontroller.CustomSettingsActivity;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorController;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;
import org.swerverobotics.library.*;
import org.swerverobotics.library.interfaces.*;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class IMU
{
    // Our sensors, motors, and other devices go here, along with other long term state
    IBNO055IMU imu;
    IBNO055IMU.Parameters parameters = new IBNO055IMU.Parameters();

    //heading data
    float Heading;
    float PreviousHeading;
    int Rotations = 0;
    boolean FirstUpdate = true;
    ElapsedTime ProgramTime;

    //time data
    float PreviousTime = 0;
    float CurrentTime = 0;
    float UpdateTime = 0;

    //PID data
    float ComputedRotation;
    float PreviousComputedRotation;
    float Target = 20;
    float TargetRateOfChange = 10;
    ArrayList HistoricData = new ArrayList();
    ArrayList DerivativeData = new ArrayList();
    float D;
    float I;
    float P;
    float DConstant;
    float IConstant;
    float PConstant;
    //can be "Straight" or "Turn"
    String Motion = "Turn";
	//can be 'Right' or 'Left'
	String StationaryWheel = "Right";
    //from 0 to 1
    float Power = .6f;

    //motor setup
    DcMotorController DriveController;
    DcMotor LeftMotor;
    DcMotor RightMotor;
    HardwareMap hardwareMap;
    TelemetryDashboardAndLog telemetry;
	Test MainOpMode;
	// Button btn;

//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		Log.d("test", "test");
//		super.onCreate(savedInstanceState);
//		Log.d("test", "test2");
//		setContentView(R.layout.activity_ftc_controller);
//		btn = (Button)findViewById(R.id.Send);
//		btn.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//
//				//runThread();
//			}
//		});
//	}

    public IMU(HardwareMap map, TelemetryDashboardAndLog tel, Test OpMode)
    {
        //set the hardware map
        hardwareMap = map;
        telemetry = tel;
		MainOpMode = OpMode;

        // setup the IMU
        parameters.angleunit = IBNO055IMU.ANGLEUNIT.DEGREES;
        parameters.accelunit = IBNO055IMU.ACCELUNIT.METERS_PERSEC_PERSEC;
        parameters.loggingEnabled = true;
        parameters.loggingTag = "BNO055";

        // the I2C device is names IMU
        imu = ClassFactory.createAdaFruitBNO055IMU(hardwareMap.i2cDevice.get("imu"), parameters);

		//telemetry.addData("11", imu.);

        //setup telemetry
        telemetry.setUpdateIntervalMs(200);

        //setup the program timer
        ProgramTime = new ElapsedTime();

        //sets the current time
        CurrentTime = (float) ProgramTime.time() * 1000;
    }

	public void ResetValues()
	{
		HistoricData.clear();
		DerivativeData.clear();
	}


	public void Stop()
	{
		LeftMotor.setPower(0);
		RightMotor.setPower(0);
	}

    //this is the update loop
    public void Forward(float Rotations) throws InterruptedException
    {
		//remove the historic data values
		ResetValues();

        Motion = "Straight";

        //start position
        long StartPosition = RightMotor.getCurrentPosition();

        //this is the first update
        FirstUpdate = true;

        //update the angles
        UpdateAngles();

        //set the target to the current position
        Target = ComputedRotation;



        while(Math.abs(StartPosition - RightMotor.getCurrentPosition()) < Rotations * 1400)
		{
            //this is the update loop
            UpdateTime();
            UpdateAngles();
            PreformCalculations();
			//telemetry.addData("7", Math.abs(StartPosition - RightMotor.getCurrentPosition()));

			//update telemetry
            telemetry.update();

			//idle using the reference to the instance of the main opmode
			MainOpMode.idle();
        }
    }

	private float ValueStandardDeviation()
	{
		//compute the mean
		float Mean = 0;
		for (int i = 0; i < DerivativeData.size(); i++)
		{
			Mean += (float) DerivativeData.get(i);
		}
		Mean /= DerivativeData.size();

		float Summation = 0;
		for (int i = 0; i < DerivativeData.size(); i++)
		{
			Summation += Math.pow((float) DerivativeData.get(i) - Mean, 2);
		}

		Summation /= DerivativeData.size();

		Summation = (float) Math.sqrt((double)Summation);

		return Summation;
	}

	//this is the update loop
	public void Turn(float Degrees) throws InterruptedException
	{
		//remove the historic data values
		ResetValues();

		Motion = "Turn";

		//this is the first update
		FirstUpdate = true;

		//update the angles, if this is uncommented it gets the values from the UI
		UpdateAngles();

		//set the target to the current position
		Target = ComputedRotation + Degrees;

		//degrees that something has to be off
		float Error = 2;

		//while the distance from the target is greater than 1
		while (ValueStandardDeviation() > .05f || Math.abs(ComputedRotation - Target) > Error)
		{
			//get the standard deviation
			ValueStandardDeviation();

			//this is the update loop
			UpdateTime();
			UpdateAngles();
			PreformCalculations();
			//telemetry.addData("7", Math.abs(StartPosition - RightMotor.getCurrentPosition()));

			//update telemetry
			telemetry.update();

			//idle using the reference to the instance of the main opmode
			MainOpMode.idle();
		}
	}

	public void UpdateConstants()
	{
		PConstant = CustomSettingsActivity.P;
		IConstant = CustomSettingsActivity.I;
		DConstant = CustomSettingsActivity.D;
	}


    void PreformCalculations()
    {
        //add the rotation to the historic data
        HistoricData.add(ComputedRotation - Target);

        DerivativeData.add(ComputedRotation);

        //make sure the list never gets longer than a certain length
        if(HistoricData.size() > 500)
        {
            HistoricData.remove(0);
        }

        //make sure the list never gets longer than a certain length
        if(DerivativeData.size() > 5)
        {
            DerivativeData.remove(0);
        }

        //find the average of the historic data list for the I value
        float IntegralAverage = 0;
        for(int i = 0; i < HistoricData.size(); i++)
        {
            IntegralAverage += (float) HistoricData.get(i);
        }
        IntegralAverage /= HistoricData.size();


        //gets the derivative
        float DerivativeAverage = 0;
        for (int i = 0; i < DerivativeData.size(); i++) {
            DerivativeAverage += (float) DerivativeData.get(i);
        }

        DerivativeAverage /= DerivativeData.size();

        // compute all of the values
        I = (IntegralAverage);
        P = ComputedRotation - Target;

		//update the constants here from the selection menu
		UpdateConstants();

        //constants
        if(Motion == "Straight")
        {
            D = (ComputedRotation - DerivativeAverage) / ((UpdateTime / 1000) * (1 + (DerivativeData.size() / 2)));

            IConstant = 11f;
            PConstant = 1.3f;
            DConstant = 2;
        }
        else if (Motion == "Turn")
        {
            //compute the d with the rate of change
            D = (ComputedRotation - DerivativeAverage) / ((UpdateTime / 1000) * (1 + (DerivativeData.size() / 2))) - TargetRateOfChange;

            //DConstant = 0f;
			//IConstant = .1f;
			//PConstant = 2.8f;
        }

        float Direction = I * IConstant + P * PConstant + D * DConstant;

        //logs data
        telemetry.addData("00", "P: " + P);
        telemetry.addData("01", "I: " + I);
        telemetry.addData("02", "D: " + D);

        telemetry.addData("03", "PConstant: " + PConstant);
        telemetry.addData("04", "IConstant: " + IConstant);
        telemetry.addData("05", "DConstant: " + DConstant);

        //constrain the direction so that abs(Direction) < 50
        if(Direction > 50)
        {
            Direction = 50;
        }
        else if (Direction < -50)
        {
            Direction = -50;
        }

        if(Motion == "Straight")
        {
            float Multiplier = Power / 2;
            LeftMotor.setPower(Multiplier + (Direction / 200));
            RightMotor.setPower(Multiplier - (Direction / 200));
        }
        else if (Motion == "Turn")
        {
            float Multiplier = Power * 4;

			if(StationaryWheel == "Right")
			{
				RightMotor.setPower(0);
				LeftMotor.setPower((Direction / 200) * Multiplier);
			}
			else if(StationaryWheel == "Left") {
				LeftMotor.setPower(0);
				RightMotor.setPower(-(Direction / 200) * Multiplier);
			}
        }
    }

    void UpdateTime()
    {
        PreviousTime = CurrentTime;
        CurrentTime = (float) ProgramTime.time() * 1000;

        //set the update time
        UpdateTime = CurrentTime - PreviousTime;
    }

    void UpdateAngles() {
        //sets the previous heading
        PreviousHeading = Heading;

        //sets the current heading
        EulerAngles Angle = imu.getAngularOrientation();
        Heading = (float) Angle.heading;

        //do rotation computation here
        if(!FirstUpdate) {
            //if it switches from large to small
            if (PreviousHeading > 300 && Heading < 60) {
                //increase the rotations by one
                Rotations++;
            }

            if (PreviousHeading < 60 && Heading > 300) {
                Rotations--;
            }
        }

        //set the previous to to the current
        PreviousComputedRotation = ComputedRotation;

        //set the current rotation
        ComputedRotation = Heading + (Rotations * 360);

        if(FirstUpdate)
        {
            FirstUpdate = false;
        }
    }
}
