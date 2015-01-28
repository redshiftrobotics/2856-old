#pragma config(Hubs,  S1, HTMotor,  HTMotor,  HTMotor,  HTServo)
#pragma config(Sensor, S1,     ,               sensorI2CMuxController)
#pragma config(Motor,  mtr_S1_C1_1,     LeftDriveMotor, tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C1_2,     RightDriveMotor, tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C2_1,     Arm,           tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C2_2,     Shooter,       tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C3_1,     BlockCollector, tmotorTetrix, openLoop)
#pragma config(Motor,  mtr_S1_C3_2,     motorI,        tmotorTetrix, openLoop)
#pragma config(Servo,  srvo_S1_C4_1,    Left,                 tServoStandard)
#pragma config(Servo,  srvo_S1_C4_2,    Right,                tServoStandard)
//*!!Code automatically generated by 'ROBOTC' configuration wizard               !!*//

#include "Menu.c"
#include "JoystickDriver.c"

string Distance = "";
bool WaitForStartBool = false;

void DistanceMenu()
{
	string t = "Distance?";
	string a = "Small";
	string b = "Medium";
	string c = "Large";
	int MenuReturn = Menu(t, a, b, c);

	if(MenuReturn == 0)
	{
		Distance = "Small";
	}
	else if(MenuReturn == 1)
	{
		Distance = "Medium";
	}
	else
	{
		Distance = "Large":
	}
}

void MoveLeft(int Power)
{
	motor[LeftDriveMotor] = Power;
}

void MoveRight(int Power)
{
	motor[RightDriveMotor] = -Power;
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

task main()
{
	//turn off joystick debug
	bDisplayDiagnostics = false;

	//lets the used pick the program
	WaitForStartMenu();

	DistanceMenu();

	eraseDisplay();

	nxtDisplayString(1, "Wait: %i", WaitForStartBool);
	nxtDisplayString(2, "Distance: %s", Distance);

	//waits for start it bool is set
	if(WaitForStartBool)
	{
		waitForStart();
	}

	eraseDisplay();

	nxtDisplayString(3, "Running...");

	MoveLeft(80);
	MoveRight(80);
	sleep(1700);

	MoveLeft(0);
	MoveRight(0);
	sleep(500);

	MoveLeft(-50);
	MoveRight(50);
	sleep(800);

	MoveLeft(80);
	MoveRight(80);

	if(Distance == "Small")
	{
		sleep(1000);
	}
	else if(Distance == "Medium")
	{
		sleep(1500);
	}
	else if(Distance == "Large")
	{
		sleep(2000);
	}

	MoveLeft(0);
	MoveRight(0);
}
