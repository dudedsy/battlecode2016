package focused16;

import battlecode.common.*;

public class BotTurret extends Globals {
	public static void loop() {
//		Debug.init("memory");
		while (true) {
			try {
				Globals.update();
				processSignals();
				if (rc.getType() == RobotType.TURRET) {
				    turnTurret();
				} else {
					turnTTM();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static MapLocation attackTarget = null;
	private static MapLocationHashSet destroyedZombieDens = new MapLocationHashSet();
	private static boolean isAttackingZombieDen = false;

	private static int lastKnownArchonId = -1;
	private static MapLocation lastKnownArchonLocation = null;
	private static int lastKnownArchonLocationRound = -999999;
	
	private static final int PACK_DELAY = 20;
	private static int packCountdown = PACK_DELAY;
	
	private static boolean inHealingState = false;
	
	private static void turnTurret() throws GameActionException {
		manageHealingState();

		if (!rc.isWeaponReady() && !rc.isCoreReady()) return;
		Radar.removeDistantEnemyTurrets(9 * RobotType.SCOUT.sensorRadiusSquared);
		
		RobotInfo[] attackableEnemies = rc.senseHostileRobots(here, myAttackRadiusSquared);
		if (rc.isWeaponReady()) {
			if (tryToAttackAnEnemy(attackableEnemies)) {
				packCountdown = PACK_DELAY;
				return;
			}
			if (attackableEnemies.length == 0 && rc.isCoreReady()) {
				--packCountdown;
				if (packCountdown == 0) {
					rc.pack();
					return;
				}
			}		
		}
	}
	
	private static void turnTTM() throws GameActionException {
		manageHealingState();
		
		if (!rc.isCoreReady()) return;
		Radar.removeDistantEnemyTurrets(9 * RobotType.SCOUT.sensorRadiusSquared);
		
		RobotInfo[] attackableEnemies = rc.senseHostileRobots(here, RobotType.TURRET.attackRadiusSquared);
		if (attackableEnemies.length > 0) {
			rc.unpack();
			packCountdown = PACK_DELAY;
			return;
		}
		for (int i = 0; i < Radar.numCachedEnemies; ++i) {
			FastRobotInfo hostile = Radar.enemyCache[i];
			if (here.distanceSquaredTo(hostile.location) <= RobotType.TURRET.attackRadiusSquared) {
				rc.unpack();
				packCountdown = PACK_DELAY;
				return;
			}
		}
		
		if (inHealingState) {
			if (tryToHealAtArchon()) {
				return;
			}
		}
		
		lookForAttackTarget();
	}
	
	private static void manageHealingState() {
		if (rc.getHealth() <= myType.maxHealth / 3) {
			inHealingState = true;
		}
		if (rc.getHealth() == myType.maxHealth) {
			inHealingState = false;
		}
	}

	private static double enemyScore(RobotType type, double health) {
		switch(type) {
		case ARCHON:
			return 0.0001;
		case ZOMBIEDEN:
			return 0.00001;
			
		case SCOUT:
			return 0.25 * RobotType.TURRET.attackPower / (health * RobotType.TURRET.attackDelay);
		case TTM:
			return 0.5 * RobotType.TURRET.attackPower / (health * RobotType.TURRET.attackDelay);
		case TURRET:
			return type.attackPower / (health * type.attackDelay);

		default:
			return type.attackPower / (health * type.attackDelay);
		}
	}
	
	public static boolean tryToAttackAnEnemy(RobotInfo[] attackableEnemies) throws GameActionException {
		MapLocation bestTarget = null;
		double maxScore = -99;
		boolean weAreAttacked = false;
		for (RobotInfo hostile : attackableEnemies) {
			if (!rc.canAttackLocation(hostile.location)) continue;
			boolean hostileIsAttackingUs = hostile.location.distanceSquaredTo(here) <= hostile.type.attackRadiusSquared;
			if (weAreAttacked) {
				if (!hostileIsAttackingUs) {
					continue;
				}
			} else {
				if (hostileIsAttackingUs) {
					weAreAttacked = true;
					maxScore = -99;
				}
			}
			
			double score = enemyScore(hostile.type, hostile.health);
			if (score > maxScore) {
				maxScore = score;
				bestTarget = hostile.location;				
			}
		}
		RobotType typeAttackedWithRadar = null;
		for (int i = 0; i < Radar.numCachedEnemies; ++i) {
			FastRobotInfo hostile = Radar.enemyCache[i];
			if (here.distanceSquaredTo(hostile.location) <= mySensorRadiusSquared) continue;
			if (!rc.canAttackLocation(hostile.location)) continue;
			
			boolean hostileIsAttackingUs = hostile.location.distanceSquaredTo(here) <= hostile.type.attackRadiusSquared;
			if (weAreAttacked) {
				if (!hostileIsAttackingUs) {
					continue;
				}
			} else {
				if (hostileIsAttackingUs) {
					weAreAttacked = true;
					maxScore = -99;
				}
			}
			
			double score = enemyScore(hostile.type, hostile.type.maxHealth);
			if (score > maxScore) {
				maxScore = score;
				bestTarget = hostile.location;	
				typeAttackedWithRadar = hostile.type;
			}			
		}
		/*if (bestTarget == null) {
			FastTurretInfo closestEnemyTurret = Radar.findClosestEnemyTurret();
//			Debug.indicate("memory", 0, "closestEnemyTurret = " + (closestEnemyTurret == null ? null : closestEnemyTurret.location));
//			if (closestEnemyTurret != null) Debug.indicateDot("memory", closestEnemyTurret.location, 0, 0, 255);
			if (closestEnemyTurret != null && rc.canAttackLocation(closestEnemyTurret.location)) {
				bestTarget = closestEnemyTurret.location;
				//System.out.println("we are " + here + ", attacking Radar.closestEnemyTurret() = " + closestEnemyTurret.location);
//				Debug.indicateDot("memory", closestEnemyTurret.location, 255, 0, 0);
		    }
		}*/
		if (bestTarget != null) {
			rc.attackLocation(bestTarget);
			/*if (typeAttackedWithRadar == RobotType.TURRET) {
				lastTurretAttackedWithRadar = bestTarget;
				lastTurretAttackedWithRadarRound = rc.getRoundNum();
			}*/
			return true;
		}
		return false;
	}
	

	private static void addAttackTarget(MapLocation targetNew, boolean isZombieDen) {
		if (isZombieDen && destroyedZombieDens.contains(targetNew)) {
			return;
		}
		if (attackTarget == null) {
			attackTarget = targetNew;
			isAttackingZombieDen = isZombieDen;
		} else if (!isAttackingZombieDen && isZombieDen) {
			isAttackingZombieDen = true;
			attackTarget = targetNew;
		} else if (isAttackingZombieDen && !isZombieDen) {
			return;
		} else {
			isAttackingZombieDen = isZombieDen;
			attackTarget = targetNew;
		}
	}

	private static void processSignals() {
		Radar.clearEnemyCache();
		
		Signal[] signals = rc.emptySignalQueue();
		for (Signal sig : signals) {
			if (sig.getTeam() != us) continue;

			int[] data = sig.getMessage();
			if (data != null) {
				switch(data[0] & Messages.CHANNEL_MASK) {
				case Messages.CHANNEL_ATTACK_TARGET:
					MapLocation suggestedTarget = Messages.parseAttackTarget(data);
					addAttackTarget(suggestedTarget, false);
					break;
					
				case Messages.CHANNEL_DEN_ATTACK_COMMAND:
					MapLocation denTarget = Messages.parseDenAttackCommand(data);
					addAttackTarget(denTarget, true);
					break;
					
				case Messages.CHANNEL_ZOMBIE_DEN:
					MapLocation denLoc = Messages.parseZombieDenLocation(data);
					if (Messages.parseZombieDenWasDestroyed(data)) {
						destroyedZombieDens.add(denLoc);
						if (denLoc.equals(attackTarget) && isAttackingZombieDen) {
							isAttackingZombieDen = false;
							attackTarget = null;
						}
					}
					break;
					
				case Messages.CHANNEL_RADAR:
					Messages.addRadarDataToEnemyCache(data, sig.getLocation(), myAttackRadiusSquared);
					//MapLocation closest = Messages.getClosestRadarHit(data, sig.getLocation());
					//if (attackTarget == null
					//		|| here.distanceSquaredTo(closest) < here.distanceSquaredTo(attackTarget)) {
					//	attackTarget = closest;
					//}
					break;
					
				case Messages.CHANNEL_ARCHON_LOCATION:
					MapLocation archonLoc = Messages.parseArchonLocation(data);
//					Debug.indicate("heal", 2, "got archonLoc = " + archonLoc);
					if (lastKnownArchonLocation == null 
							|| (lastKnownArchonLocationRound < rc.getRoundNum() - 50)
							|| here.distanceSquaredTo(lastKnownArchonLocation) > here.distanceSquaredTo(archonLoc)) {
						lastKnownArchonLocation = archonLoc;
						lastKnownArchonLocationRound = rc.getRoundNum();
//						Debug.indicateAppend("heal", 2, "; new best");
					}
					break;
					
				case Messages.CHANNEL_ENEMY_TURRET_WARNING:
					Messages.processEnemyTurretWarning(data);
					break;
					
				default:
				}
			}
		}
	}
	
	private static boolean tryGoToCenterOfMass() throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(mySensorRadiusSquared, us);
		if (allies.length == 0) return false;
		int avgX = 0;
		int avgY = 0;
		int N = 0;
		for (RobotInfo ally : allies) {
			if (ally.type == RobotType.TURRET || ally.type == RobotType.TTM
					|| ally.type == RobotType.SCOUT) continue;
			avgX += ally.location.x;
			avgY += ally.location.y;
			++N;
		}
		if (N == 0) return false;
		avgX /= N;
		avgY /= N;
		Nav.goToBug(new MapLocation(avgX, avgY));
		return true;
	}


	private static void locateNearestArchon() throws GameActionException {
		// first look for our favorite archon
		if (rc.canSenseRobot(lastKnownArchonId)) {
			RobotInfo archon = rc.senseRobot(lastKnownArchonId);
			lastKnownArchonLocation = archon.location;
			lastKnownArchonLocationRound = rc.getRoundNum();
			return;
		}
		
		// else look for any nearby archon
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(mySensorRadiusSquared, us);
		for (RobotInfo ally : nearbyAllies) {
			if (ally.type == RobotType.ARCHON) {
				lastKnownArchonLocation = ally.location;
				lastKnownArchonLocationRound = rc.getRoundNum();
				lastKnownArchonId = ally.ID;
				return;
			}
		}
		
		// else hope that we have gotten an archon location broadcast
	}
	

	private static boolean tryToHealAtArchon() throws GameActionException {
		if (!rc.isCoreReady()) return false;
		
		locateNearestArchon();
		
		if (lastKnownArchonLocation == null) {
			return false;
		}
		
		Nav.goToBug(lastKnownArchonLocation);
		return true;
	}

	private static void lookForAttackTarget() throws GameActionException {
		if (!rc.isCoreReady()) return;
		
		if (attackTarget != null) {
			if (rc.canSenseLocation(attackTarget)) {
				RobotInfo targetInfo = rc.senseRobotAtLocation(attackTarget);
				if (targetInfo == null || targetInfo.team == us) {
					if (isAttackingZombieDen) {
						destroyedZombieDens.add(attackTarget);
					}
					attackTarget = null;
					isAttackingZombieDen = false;
				} else if (targetInfo.type != RobotType.ZOMBIEDEN) {
					isAttackingZombieDen = false;
				}
			}
		}
		
		if (attackTarget != null) {
			Nav.goToBug(attackTarget);
		} else {
			// no attack target
			tryToHealAtArchon();
		}
	}
}
