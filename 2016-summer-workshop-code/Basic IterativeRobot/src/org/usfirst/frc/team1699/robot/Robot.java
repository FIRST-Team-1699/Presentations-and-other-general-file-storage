package org.usfirst.frc.team1699.robot;

import edu.wpi.first.wpilibj.Jaguar;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.IterativeRobot;

public class Robot extends IterativeRobot 
{
    
	// Joysticks
	Joystick joy1;
	Joystick joy2;
	
	// Motors
	Jaguar left1;
	Jaguar left2;
	Jaguar right1;
	Jaguar right2;
	
	// RobotDrive
	
	RobotDrive drive;
	
    public void robotInit() 
    {
    	joy1 = new Joystick(0);
    	joy2 = new Joystick(1);
    	
    	left1 = new Jaguar(0);
    	left2 = new Jaguar(1);
    	right1 = new Jaguar(2);
    	right2 = new Jaguar(3);
    	
    	drive = new RobotDrive(left1, left2, right1, right2);
    }
    
    public void teleopPeriodic() 
    {
    	drive.tankDrive(joy1.getRawAxis(0), joy2.getRawAxis(0));
    }
    
}
