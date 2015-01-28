#pragma config(Hubs,  S1, HTMotor,  HTMotor,  HTMotor,  HTServo)
#pragma config(Sensor, S1,     ,               sensorI2CMuxController)
#pragma config(Motor,  mtr_S1_C1_1,     motorD,        tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C1_2,     motorE,        tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C2_1,     motorF,        tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C2_2,     motorG,        tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C3_1,     motorH,        tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C3_2,     motorI,        tmotorTetrix, openLoop)
#pragma config(Servo,  srvo_S1_C4_1,    servo1,               tServoNone)
#pragma config(Servo,  srvo_S1_C4_2,    servo2,               tServoNone)
#pragma config(Servo,  srvo_S1_C4_3,    servo3,               tServoNone)
#pragma config(Servo,  srvo_S1_C4_4,    servo4,               tServoNone)
#pragma config(Servo,  srvo_S1_C4_5,    servo5,               tServoNone)
#pragma config(Servo,  srvo_S1_C4_6,    servo6,               tServoNone)
//*!!Code automatically generated by 'ROBOTC' configuration wizard               !!*//

#include "JoystickDriver.c"
#include "Motors.h"
#include "Servos.h"
#include "Menu.c"

bool WaitForStartBool = false;

void MoveLeft(int Power)
{
	motor[motorD] = Power;
	//motor[LeftDriveMotor] = Power;
	nxtDisplayString(1, "LeftPower: %i", Power);
}

void Shoot()
{
	//Motors_SetSpeed(S1, 2, 2, 100);
	motor[motorG] = 100;
}

void StopShoot()
{
	motor[motorG] = 0;
}

void RaiseServos()
{
	servo[servo1] = 240;
	// Servos_SetPosition(S1, 4, 1, 240);
}

void PickupBlocks(int Power)
{
	//Motors_SetSpeed(S1, 3, 1, Power);
	motor[motorH] = Power;
	nxtDisplayString(3, "PickupPower: %i", Power);
}

void MoveArm(int Power)
{
	//Motors_SetSpeed(S1, 2, 1, Power);
  motor[motorF] = -Power;
	nxtDisplayString(4, "ArmPower: %i", Power);
}

void LowerServos()
{
	servo[servo1] = 70;
	//Servos_SetPosition(S1, 4, 1, 70);
}

void MoveRight(int Power)
{
	//Motors_SetSpeed(S1, 1, 2, -Power);
  motor[motorE] = -Power;
	nxtDisplayString(2, "RightPower: %i", -Power);
}

void WaitForStartMenu()
{
	string t = "Wait for Start";
	string b = "Yes";
	string c = "No";
	int MenuReturn = Menu(t, b, c);

	if(MenuReturn == 0)
	{
		WaitForStartBool = true;
	}
	else if(MenuReturn == 1)
	{
		WaitForStartBool = false;
	}
}

void MainProgram()
{
	//raise the servos
	RaiseServos();

	int StartEncoder = nMotorEncoder[motorE];

	//go backward
	MoveLeft(-25);
	MoveRight(-30);

	sleep(500);

	MoveArm(60);

	sleep(2100);

	servo[servo2] = 240;

	MoveArm(0);

	while(nMotorEncoder[motorE] < StartEncoder + 1440 * 4.0)
	{
		writeDebugStreamLine("%i", nMotorEncoder[motorE]);
	}

	//stop
	MoveLeft(0);
	MoveRight(0);

	//FIRST TRY

	//grab the tube
	LowerServos();
	sleep(200);

	//SECOND TRY

	//turn right
	MoveRight(-40);
	MoveLeft(40);

	sleep(600);

	RaiseServos();
	sleep(200);

	//move back
	MoveRight(-40);
	MoveLeft(-40);

	sleep(400);

	//stop
	MoveRight(0);
	MoveLeft(0);

	//lower servos
	LowerServos();
	sleep(200);

	//THIRD TRY

	//turn right
	MoveRight(-40);
	MoveLeft(40);

	sleep(600);

	//raise servos
	RaiseServos();

	//move back
	MoveRight(-40);
	MoveLeft(-40);

	sleep(400);

	//stop
	MoveRight(0);
	MoveLeft(0);

	//lower servos
	LowerServos();
	sleep(500);

	//TRY TO SCORE NOW
	//go Straight
	MoveLeft(40);
	MoveRight(40);

	sleep(500);

	MoveLeft(0);
	MoveRight(0);

	//go Straight
	MoveLeft(-80);
	MoveRight(-80);

	sleep(200);

	MoveRight(0);
	MoveLeft(0);

	PickupBlocks(100);

	Shoot();

	sleep(5000);

	StopShoot();
}

task main()
{
	//turn off joystick debug
	bDisplayDiagnostics = false;

	//lets the used pick the program
	WaitForStartMenu();

	eraseDisplay();

	//waits for start it bool is set
	if(WaitForStartBool)
	{
		waitForStart();
	}

	eraseDisplay();

	nxtDisplayString(3, "Running...");

	MainProgram();
}
