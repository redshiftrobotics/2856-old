package org.usfirst.ftc.exampleteam.yourcodehere;

import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.util.*;
import org.swerverobotics.library.*;
import org.swerverobotics.library.interfaces.*;

/**
 * An example of a synchronous opmode that implements a simple drive-a-bot. 
 */
@TeleOp(name="2856 TeleOp")
public class Teleop extends SynchronousOpMode
{
	//motors declarations
	DcMotor leftDrive = null;
	DcMotor rightDrive = null;
	DcMotor backBrace = null;
	//DcMotor arm = null;
	//DcMotor blockCollector = null;
	DcMotor backWheel = null;
	Servo leftClimberServo = null;
	Servo rightClimberServo = null;
	Servo hooker = null;
	Servo climberControl = null;
	Servo leftDebris = null;
	Servo rightDebris = null;

	boolean debounce = false;

	boolean up = true;

	float BackTargetEncoder = 0;
	float ArmStartEncoder = 0;

	@Override
	protected void main() throws InterruptedException
	{
		//initialize motors
		this.leftDrive = this.hardwareMap.dcMotor.get("left_drive");
		this.rightDrive = this.hardwareMap.dcMotor.get("right_drive");
		this.backBrace = this.hardwareMap.dcMotor.get("back_brace");
		//this.arm = this.hardwareMap.dcMotor.get("arm");
		//this.blockCollector = this.hardwareMap.dcMotor.get("block_collector");
		this.backWheel = this.hardwareMap.dcMotor.get("back_wheel");
		this.leftClimberServo = this.hardwareMap.servo.get("left_climber");
		this.rightClimberServo = this.hardwareMap.servo.get("right_climber");
		//this.hooker = this.hardwareMap.servo.get("hooker");
		this.climberControl = this.hardwareMap.servo.get("climber_control");
		this.leftDebris = this.hardwareMap.servo.get("left_debris");
		this.rightDebris = this.hardwareMap.servo.get("right_debris");

		//run these with encoders
		backBrace.setChannelMode(DcMotorController.RunMode.RUN_USING_ENCODERS);
		//arm.setChannelMode(DcMotorController.RunMode.RUN_USING_ENCODERS);

		backWheel.setDirection(DcMotor.Direction.REVERSE);
		this.rightDrive.setDirection(DcMotor.Direction.REVERSE);

		//this.arm.setDirection(DcMotor.Direction.REVERSE);

		//set initial encoders
		BackTargetEncoder = backBrace.getCurrentPosition();
		//ArmStartEncoder = arm.getCurrentPosition();

		//
		// Wait until we've been given the ok to go
		this.waitForStart();

		// Enter a loop processing all the input we receive
		while (this.opModeIsActive())
		{
			updateGamepads();

			this.DriveControl(this.gamepad1);
			this.BackBraceControl(this.gamepad1);
			this.ClimberDeploymentControl(this.gamepad1); //hit climbers on ramp
			this.ClimberControl(this.gamepad2); //climberDumper mechanism
			//this.ArmControl(this.gamepad2);
			//this.CollectorControl(this.gamepad2);
			this.HookControl(this.gamepad1);
			this.DebrisControl(this.gamepad1);

			// Emit telemetry with the freshest possible values
			this.telemetry.update();

			// Let the rest of the system run until there's a stimulus from the robot controller runtime.
			this.idle();
		}
	}

//	void ArmControl(Gamepad pad)
//	{
//		float Encoder = arm.getCurrentPosition();
//
//		if(pad.left_stick_y > .1 && ArmStartEncoder > Encoder)
//		{
//			arm.setPower(pad.left_stick_y);
//		}
//		else if(pad.left_stick_y < -.1 && ArmStartEncoder < Encoder + 3 * 1440)
//		{
//			arm.setPower(pad.left_stick_y);
//		}
//		else
//		{
//			arm.setPower(0);
//		}
//	}

	void DebrisControl(Gamepad pad) {

		if(debounce != pad.x && pad.x)
		{
			up = !up;
		}

		debounce = pad.x;

		if(up) {
			this.rightDebris.setPosition(0);
			this.leftDebris.setPosition(1);
		} else {
			this.rightDebris.setPosition(0.65);
			this.leftDebris.setPosition(0.7);
		}

	}

	void ClimberControl(Gamepad pad) {
		if(pad.a)
		{
			this.climberControl.setPosition(1);
		} else {
			this.climberControl.setPosition(0);
		}
	}

	void ClimberDeploymentControl(Gamepad pad)
	{
		if(pad.left_bumper)
		{
			this.leftClimberServo.setPosition(.6);
		}
		else
		{
			this.leftClimberServo.setPosition(0);
		}

		if(pad.right_bumper)
		{
			this.rightClimberServo.setPosition(.35);
		}
		else
		{
			this.rightClimberServo.setPosition(1);
		}
	}


	void HookControl(Gamepad pad)
	{

		if(pad.a)
		{
			this.hooker.setPosition(.1);
		}

		if(pad.b) {
			this.hooker.setPosition(.8);
		}

	}


//	void CollectorControl(Gamepad pad)
//	{
//		if(Math.abs(pad.right_stick_y) > .1)
//		{
//			blockCollector.setPower(pad.right_stick_y);
//		}
//		else
//		{
//			blockCollector.setPower(0);
//		}
//	}

	void BackBraceControl(Gamepad pad)
	{

		float threshold = .4f;
		if (Math.abs(pad.left_trigger) > threshold || Math.abs(pad.right_trigger) > threshold) {
			if (Math.abs(pad.right_trigger) > threshold) {
				BackTargetEncoder += pad.right_trigger * 50;
			} else if (Math.abs(pad.left_trigger) > threshold) {
				BackTargetEncoder -= pad.left_trigger * 50;
			}
		}

		//the difference between current and target position
		float BackDifference = ((float) BackTargetEncoder - (float) backBrace.getCurrentPosition()) / 500f;

		//bound the speed of the back brace
		if (BackDifference > 1) {
			BackDifference = 1;
		}
		if (BackDifference < -1) {
			BackDifference = -1;
		}

		telemetry.addData("32", "current position: " + backBrace.getCurrentPosition());
		telemetry.addData("12", "back target: " + BackTargetEncoder);
		telemetry.addData("25", BackDifference);

		//set the back brace power
		backBrace.setPower(BackDifference);
	}

	void DriveControl(Gamepad pad) throws InterruptedException {
		// Remember that the gamepad sticks range from -1 to +1, and that the motor
		// power levels range over the same amount
		float leftPower = pad.right_stick_y;
		float rightPower = pad.left_stick_y;

		float backWheelPower = -(leftPower + rightPower) / 2f;


		// drive the motors
		this.leftDrive.setPower(leftPower / 2);
		this.rightDrive.setPower(rightPower / 2);
		this.backWheel.setPower(backWheelPower);
	}
}
