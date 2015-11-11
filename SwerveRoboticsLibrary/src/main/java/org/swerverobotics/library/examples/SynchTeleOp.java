package org.swerverobotics.library.examples;

import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.util.*;
import org.swerverobotics.library.*;
import org.swerverobotics.library.interfaces.*;

/**
 * An example of a synchronous opmode that implements a simple drive-a-bot. 
 */
@TeleOp(name="TeleOp")
@Disabled
public class SynchTeleOp extends SynchronousOpMode
{
	//motors declarations
	DcMotor leftDrive = null;
	DcMotor rightDrive = null;
	DcMotor backBrace = null;
	DcMotor arm = null;
	DcMotor blockCollector = null;
	DcMotor backWheel = null;

	float BackTargetEncoder = 0;
	float ArmTargetEncoder = 0;

	@Override
	protected void main() throws InterruptedException
	{
		//initialize motors
		this.leftDrive = this.hardwareMap.dcMotor.get("left_drive");
		this.rightDrive = this.hardwareMap.dcMotor.get("right_drive");
		this.backBrace = this.hardwareMap.dcMotor.get("back_brace");
		this.arm = this.hardwareMap.dcMotor.get("arm");
		this.backWheel = this.hardwareMap.dcMotor.get("back_wheel");
		this.blockCollector = this.hardwareMap.dcMotor.get("block_collector");

		//run these with encoders
		backBrace.setChannelMode(DcMotorController.RunMode.RUN_USING_ENCODERS);
		arm.setChannelMode(DcMotorController.RunMode.RUN_USING_ENCODERS);

		//reverse the left motor
		this.rightDrive.setDirection(DcMotor.Direction.REVERSE);

		//set initial encoders
		BackTargetEncoder = backBrace.getCurrentPosition();
		ArmTargetEncoder = arm.getCurrentPosition();

		// Wait until we've been given the ok to go
		this.waitForStart();

		// Enter a loop processing all the input we receive
		while (this.opModeIsActive())
		{
			updateGamepads();

			this.DriveControl(this.gamepad1);
			this.BackBraceControl(this.gamepad1);

			// Emit telemetry with the freshest possible values
			this.telemetry.update();

			// Let the rest of the system run until there's a stimulus from the robot controller runtime.
			this.idle();
		}
	}

	void BackBraceControl(Gamepad pad)
	{
		if (Math.abs(pad.right_trigger) > .1)
		{
			BackTargetEncoder += pad.left_trigger * 50;
		}
		else if (Math.abs(pad.left_trigger) > .1)
		{
			BackTargetEncoder -= pad.left_trigger * 50;
		}

		//the difference between current and target position
		float BackDifference = ((float)BackTargetEncoder - (float)backBrace.getCurrentPosition()) / 500f;

		//bound the speed of the back brace
		if (BackDifference > 1) {
			BackDifference = 1;
		}
		if (BackDifference < -1) {
			BackDifference = -1;
		}

		//set the back brace power
		backBrace.setPower(BackDifference);
	}

	void ArmControl(Gamepad pad)
	{
		if (Math.abs(pad.right_stick_y) > .1) {
			BackTargetEncoder += pad.right_stick_y * 50;
		}

		float BackDifference = ((float)BackTargetEncoder - (float)backBrace.getCurrentPosition()) / 500f;

		if (BackDifference > 1) {
			BackDifference = 1;
		}
		if (BackDifference < -1) {
			BackDifference = -1;
		}

		backBrace.setPower(BackDifference);
	}

	void DriveControl(Gamepad pad) throws InterruptedException {
		// Remember that the gamepad sticks range from -1 to +1, and that the motor
		// power levels range over the same amount
		float ctlPower = pad.left_stick_y;
		float ctlSteering = pad.left_stick_x;


		ctlPower = Range.clip(ctlPower, ctlSteering - 1, ctlSteering + 1);
		ctlPower = Range.clip(ctlPower, -ctlSteering - 1, -ctlSteering + 1);

		// Figure out how much power to send to each motor. Be sure
		// not to ask for too much, or the motor will throw an exception.
		float powerLeft = Range.clip(ctlPower - ctlSteering, -1f, 1f);
		float powerRight = Range.clip(ctlPower + ctlSteering, -1f, 1f);

		// Tell the motors
		this.leftDrive.setPower(powerLeft / 4);
		this.rightDrive.setPower(powerRight / 4);
	}
}
