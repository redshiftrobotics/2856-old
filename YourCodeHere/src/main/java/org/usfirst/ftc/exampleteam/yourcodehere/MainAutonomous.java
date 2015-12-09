package org.usfirst.ftc.exampleteam.yourcodehere;

import android.util.Log;

import com.qualcomm.robotcore.hardware.*;
import org.swerverobotics.library.*;
import org.swerverobotics.library.interfaces.*;
import com.qualcomm.ftcrobotcontroller.CustomSettingsActivity;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * A skeletal example of a do-nothing first OpMode. Go ahead and change this code
 * to suit your needs, or create sibling OpModes adjacent to this one in the same
 * Java package.
 */
@TeleOp(name="Main Autonomous")
public class MainAutonomous extends SynchronousOpMode {

	public IMU Robot;

	@Override
	public void main() throws InterruptedException {
		DcMotor LeftMotor = hardwareMap.dcMotor.get("left_drive");
		DcMotor RightMotor = hardwareMap.dcMotor.get("right_drive");
		DcMotor BackBrace = hardwareMap.dcMotor.get("back_brace");
		DcMotor BackWheel = hardwareMap.dcMotor.get("back_wheel");
		Servo climberServo = this.hardwareMap.servo.get("climber_control");
		Servo leftDebris = this.hardwareMap.servo.get("left_debris");
		Servo rightDebris = this.hardwareMap.servo.get("right_debris");

		LeftMotor.setDirection(DcMotor.Direction.REVERSE);

		Robot = new IMU(LeftMotor, RightMotor, hardwareMap, telemetry, this);

		//set positions of servos
		rightDebris.setPosition(1);
		leftDebris.setPosition(1);

		//zero for continuos servo
		climberServo.setPosition(0.55);


		waitForStart();

		Thread.sleep(CustomSettingsActivity.startDelay * 1000);


//		if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.BLUE) {
//
//
//		} else if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.RED){
//
//		}
//
//		if (CustomSettingsActivity.rampCloseness == CustomSettingsActivity.rampCloseness.NEAR) {
//
//		} else if (CustomSettingsActivity.rampCloseness == CustomSettingsActivity.rampCloseness.FAR){
//
//		}




		double InitialRotation = Robot.Rotation();
		double BackBraceInitial = BackBrace.getCurrentPosition();

		Robot.Straight(1.5f);
		Robot.Stop();

		//back brace to correct height
		while (Math.abs(BackBraceInitial - BackBrace.getCurrentPosition()) < 1440 * 4)
		{
			//need to do this whenever not using rotation libraries
			Robot.UpdateAngles();

			BackBrace.setPower(1);
		}
		BackBrace.setPower(0);


		//(40, left tread) for blue

		//!!!!!!! If no option is selected the robot will default to running on the blue alliance

		if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.BLUE) {
			Robot.Turn(37, "Left");
		} else if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.RED){
			Robot.Turn(-45);
		} else {
			//OPTION NOT SELECTED
			Robot.Turn(38, "Left");
//
// 		while (Math.abs(BackBraceInitial - BackBrace.getCurrentPosition()) > 200)
//			{
//				//need to do this whenever not using rotation libraries
//				Robot.UpdateAngles();
//
//				BackBrace.setPower(-1);
//			}
		}


		Robot.Straight(7f);

		Robot.Stop();

		//back brace to correct height
		while (Math.abs(BackBraceInitial - BackBrace.getCurrentPosition()) > 200)
		{
			//need to do this whenever not using rotation libraries
			Robot.UpdateAngles();

			BackBrace.setPower(-1);
		}

		telemetry.log.add("Backbrace Encoder Dif: " + Math.abs(BackBraceInitial - BackBrace.getCurrentPosition()));

		//current rotation minus initial rotation
		//there was a bug where the robot would turn 360 degrees and add 360, so take the mod
		double AdditionalTurnDegrees = (((Robot.Rotation() - InitialRotation) + 45));

		if(AdditionalTurnDegrees < -200)
		{
			AdditionalTurnDegrees += 360;
		}
		else if(AdditionalTurnDegrees > 200)
		{
			AdditionalTurnDegrees -= 360;
		}

		telemetry.log.add(AdditionalTurnDegrees + " additional degrees to turn.");

		BackBrace.setPower(0);

		//((135 - (float) AdditionalTurnDegrees) * -1, "Right") for blue
		if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.BLUE) {
			Robot.Turn(155 + (float) AdditionalTurnDegrees, "Right");
		} else if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.RED){
			Robot.Turn(135 - (float) AdditionalTurnDegrees, "Left");
		} else {
			//OPTION NOT SELECTED
			Robot.Turn(140 + (float) AdditionalTurnDegrees, "Right");
		}


		if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.BLUE) {
			Robot.Straight(-1.7f);
		} else if (CustomSettingsActivity.fieldSide == CustomSettingsActivity.FieldSide.RED){
			Robot.Straight(-1.5f);
		} else {
			//OPTION NOT SELECTED picking blue
			Robot.Straight(-1.7f);
		}



		Robot.Stop();

		climberServo.setPosition(0);
		Thread.sleep(3000);
		climberServo.setPosition(0.55);


//		Trigger.takeImage();
//		Thread.sleep(1000);
//
//		if(Trigger.determineSides() == "left") {
//
//		} else if (Trigger.determineSides() == "right") {
//			//ButtonServo.setPosition(180);
//		} else {
//			Robot.Straight(-1);
//		}


	}



	public void RunIdle() throws InterruptedException
	{
		idle();
	}
}
