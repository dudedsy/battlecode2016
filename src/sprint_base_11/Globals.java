package sprint_base_11;

import battlecode.common.*;

public class Globals {
	public static RobotController rc;
	public static MapLocation here;
	public static Team us;
	public static Team them;
	public static int myID;
	public static RobotType myType;
	public static int myAttackRadiusSquared;
	public static int mySensorRadiusSquared;
	
	public static void init(RobotController theRC) {
		rc = theRC;
		us = rc.getTeam();
		them = us.opponent();
		myID = rc.getID();
		myType = rc.getType();
		myAttackRadiusSquared = myType.attackRadiusSquared;
		mySensorRadiusSquared = myType.sensorRadiusSquared;
		here = rc.getLocation();
	}
	
	public static void update() {
		here = rc.getLocation();
	}
}
