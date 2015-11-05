package org.swerverobotics.library.internal;

import android.graphics.Color;
import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.util.TypeConversion;
import org.swerverobotics.library.interfaces.II2cDeviceClient;

import java.util.Map;

/**
 * This class implements a driver for either a HiTechnic color sensor or a
 * Modern Robotics color sensor
 */
public class ColorSensorOnI2cDeviceClient extends ColorSensor implements IOpModeStateTransitionEvents
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    // See http://www.hitechnic.com/cgi-bin/commerce.cgi?preadd=action&key=NCO1038

    public static final int ADDRESS_I2C_HITECHNIC = 2;
    public static final int ADDRESS_I2C_MODERN    = 60;

    public static final int ADDRESS_COMMAND_HITECHNIC = 65;
    public static final int ADDRESS_COMMAND_MODERN    = 3;

    public static final int OFFSET_COMMAND = 4;
    public static final int OFFSET_COLOR_NUMBER = 5;
    public static final int OFFSET_RED_READING = 6;
    public static final int OFFSET_GREEN_READING = 7;
    public static final int OFFSET_BLUE_READING = 8;
    public static final int OFFSET_ALPHA_VALUE = 9;
    public static final int COMMAND_PASSIVE_LED = 1;
    public static final int COMMAND_ACTIVE_LED = 0;

    final I2cDeviceClient i2cDeviceClient;
    final FLAVOR          flavor;
          boolean         ledIsEnabled;

    OpMode                              context;
    ColorSensor                         target;
    String                              targetName;
    I2cController                       controller;
    int                                 targetPort;
    I2cController.I2cPortReadyCallback  targetCallback;
    boolean                             isArmed;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public enum FLAVOR { HITECHNIC, MODERNROBOTICS };

    public ColorSensorOnI2cDeviceClient(OpMode context, I2cDeviceClient i2cDeviceClient, FLAVOR flavor, ColorSensor target,
                                        I2cController controller, int targetPort, I2cController.I2cPortReadyCallback callback)
        {
        this.context        = context;
        this.i2cDeviceClient = i2cDeviceClient;
        this.isArmed        = false;
        this.target         = target;
        this.targetName     = findTargetName();
        this.controller     = controller;
        this.targetPort     = targetPort;
        this.targetCallback = callback;
        this.flavor         = flavor;

        this.ledIsEnabled = false;

        this.i2cDeviceClient.setReadWindow(new II2cDeviceClient.ReadWindow(
                this.getIbOffsetBase() + OFFSET_COMMAND,
                OFFSET_ALPHA_VALUE - OFFSET_COMMAND + 1,
                II2cDeviceClient.READ_MODE.REPEAT));

        RobotStateTransitionNotifier.register(context, this);
        }

    public static ColorSensor create(OpMode context, ColorSensor target, FLAVOR flavor)
        {
        I2cController controller;
        int           port;
        int           i2cAddr8Bit;
        I2cController.I2cPortReadyCallback callback;

        if (flavor == FLAVOR.HITECHNIC)
            {
            LegacyModule legacyModule = MemberUtil.legacyModuleOfHiTechnicColorSensor(target);
            controller  = legacyModule;
            port        = MemberUtil.portOfHiTechnicColorSensor(target);
            i2cAddr8Bit = ADDRESS_I2C_HITECHNIC;
            callback    = MemberUtil.callbacksOfLegacyModule(legacyModule)[port];
            }
        else
            {
            DeviceInterfaceModule deviceInterfaceModule = MemberUtil.deviceModuleOfModernColorSensor(target);
            controller  = deviceInterfaceModule;
            port        = MemberUtil.portOfModernColorSensor(target);
            i2cAddr8Bit = target.getI2cAddress();
            callback    = MemberUtil.callbacksOfDeviceInterfaceModule(deviceInterfaceModule)[port];
            }

        II2cDevice i2cDevice                = new I2cDeviceOnI2cDeviceController(controller, port);
        I2cDeviceClient i2cDeviceClient     = new I2cDeviceClient(context, i2cDevice, i2cAddr8Bit, false);
        ColorSensorOnI2cDeviceClient result = new ColorSensorOnI2cDeviceClient(context, i2cDeviceClient, flavor, target, controller, port, callback);
        result.arm();
        return result;
        }

    private String findTargetName()
        {
        if (this.context != null)
            {
            for (Map.Entry<String,ColorSensor> pair : this.context.hardwareMap.colorSensor.entrySet())
                {
                if (pair.getValue() == this.target)
                    return pair.getKey();
                }
            }
        return null;
        }

    private void arm()
        {
        if (!this.isArmed)
            {
            this.controller.deregisterForPortReadyCallback(this.targetPort);
            if (this.targetName != null) this.context.hardwareMap.colorSensor.put(this.targetName, this);
            this.i2cDeviceClient.arm();
            this.isArmed = true;
            }
        }

    private void disarm()
        {
        if (this.isArmed)
            {
            this.isArmed = false;
            this.i2cDeviceClient.disarm();
            if (this.targetName != null) this.context.hardwareMap.colorSensor.put(this.targetName, this.target);
            this.controller.registerForI2cPortReadyCallback(this.targetCallback, this.targetPort);
            }
        }

    //----------------------------------------------------------------------------------------------
    // IOpModeStateTransitionEvents
    //----------------------------------------------------------------------------------------------

    @Override synchronized public boolean onUserOpModeStop()
        {
        if (this.isArmed)
            {
            this.disarm();
            }
        return true;    // unregister us
        }

    @Override synchronized public boolean onRobotShutdown()
        {
        // We actually shouldn't be here by now, having received a onUserOpModeStop()
        // after which we should have been unregistered. But we close down anyway.
        this.close();
        return true;    // unregister us
        }

    //----------------------------------------------------------------------------------------------
    // HardwareDevice
    //----------------------------------------------------------------------------------------------

    @Override public void close()
        {
        this.i2cDeviceClient.close();
        }

    @Override public int getVersion()
        {
        return this.flavor == FLAVOR.HITECHNIC ? 2 : 1;
        }

    @Override public String getConnectionInfo()
        {
        return null;
        }

    @Override public String getDeviceName()
        {
        return this.flavor == FLAVOR.HITECHNIC
                ? "Swerve NXT Color Sensor"
                : "Swerve Modern Robotics I2C Color Sensor";
        }

    //----------------------------------------------------------------------------------------------
    // ColorSensor
    //----------------------------------------------------------------------------------------------

    int getIbOffsetBase()
        {
        return this.flavor == FLAVOR.HITECHNIC
                ? (ADDRESS_COMMAND_HITECHNIC - OFFSET_COMMAND)
                : (ADDRESS_COMMAND_MODERN - OFFSET_COMMAND);
        }

    int read(int dib)
        {
        byte b = this.i2cDeviceClient.read8(getIbOffsetBase() + dib);
        return TypeConversion.unsignedByteToInt(b);
        }

    @Override public synchronized int red()
        {
        return this.read(OFFSET_RED_READING);
        }

    @Override public synchronized int green()
        {
        return this.read(OFFSET_GREEN_READING);
        }

    @Override public synchronized int blue()
        {
        return this.read(OFFSET_BLUE_READING);
        }

    @Override public synchronized int alpha()
        {
        return this.read(OFFSET_ALPHA_VALUE);
        }

    @Override public synchronized int argb()
        {
        return Color.argb(this.alpha(), this.red(), this.green(), this.blue());
        }

    @Override public synchronized void enableLed(boolean enable)
        {
        if (this.ledIsEnabled != enable)
            {
            this.ledIsEnabled = enable;
            this.i2cDeviceClient.write8(getIbOffsetBase() + OFFSET_COMMAND, enable ? COMMAND_ACTIVE_LED : COMMAND_PASSIVE_LED);
            }
        }

    @Override public synchronized int getI2cAddress()
        {
        return this.i2cDeviceClient.getI2cAddr();
        }

    @Override public synchronized void setI2cAddress(int i2cAddr8Bit)
        {
        this.i2cDeviceClient.setI2cAddr(i2cAddr8Bit);
        }
    }
