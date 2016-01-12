package Hocaferr2;

import java.util.ArrayList;

import battlecode.common.*;

public class RobotPlayer{

	static Direction movingDirection=Direction.NORTH_EAST;	
	static RobotController rc;
	static MapLocation archonLocation;
	static int id = -1; // para facilitar os outros robos a pegar o lider - colocar em -1
	
	public static void run(RobotController rcIn) throws GameActionException{
		rc=rcIn;
		archonLocation = rc.getLocation();

		if(rc.getTeam()==Team.B)
			movingDirection=Direction.SOUTH_WEST;
		
		while(true){
			try {
				signalling();
				repeat();
				Clock.yield();
			} catch (GameActionException e) {
				// TODO Auto-generated catch block
				// o comando de movimentar pode gerar uma exceção e ela deve ser tratada, 
				// neste caso apenas coloca na lista da stack de console
				e.printStackTrace();
			}
		}
	}	

	private static void signalling() throws GameActionException{
		if(rc.getType()==RobotType.ARCHON){
			if(rc.getRoundNum()==0){
				Signal[] incomingMessages = rc.emptySignalQueue();
				rc.setIndicatorString(0,""+incomingMessages.length+"messages received");
				id = incomingMessages.length;
				rc.broadcastMessageSignal(0, 0, 100);
			}else{
				if(id==0){
					sendinstructions();
				}else{
					followinstructions();
				}
			}
		}else{
			followinstructions();
		}
	}

	private static void sendinstructions() throws GameActionException {
		MapLocation aheadLocation = rc.getLocation().add(movingDirection.dx*4, movingDirection.dy*4);
		if(!rc.onTheMap(aheadLocation)||rc.getRoundNum()%200==199){
			movingDirection = randomDirection();
		}
		rc.broadcastMessageSignal(rc.getTeam().ordinal(), movingDirection.ordinal(), 10000); //broadcazst range to 10000
		
	}
	
	private static Direction randomDirection() {
				return Direction.values()[(int)(Math.random()*8)];
	}

	private static void followinstructions() {
		Signal[] incomingMessages = rc.emptySignalQueue();
		if(incomingMessages.length==0)
			return;
		Signal currentMessage = null;
		for(int messageIndex=0;messageIndex<incomingMessages.length;messageIndex++){
			currentMessage = incomingMessages[messageIndex];
			if(rc.getTeam().ordinal()==currentMessage.getMessage()[0]){
				break;
			}
		}
		if(currentMessage==null)
			return;
		archonLocation = currentMessage.getLocation();
		Direction archonDirection = Direction.values()[currentMessage.getMessage()[1]];
		MapLocation goalLocation = archonLocation.add(archonDirection.dx*5, archonDirection.dy*5);
		movingDirection = rc.getLocation().directionTo(goalLocation);
	}
	
	public static void repeat() throws GameActionException{
		RobotInfo[] zombieEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,Team.ZOMBIE);
		RobotInfo[] normalEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,rc.getTeam().opponent());
		RobotInfo[] opponentEnemies = FuncLib.joinRobotInfo(zombieEnemies, normalEnemies);
		
		int distToPack = rc.getLocation().distanceSquaredTo(archonLocation);
		
		if(opponentEnemies.length>0&&rc.getType().canAttack()&&distToPack<36){
			if(rc.isWeaponReady()){
				rc.attackLocation(opponentEnemies[0].location);
			}

		}else {

			if(rc.isCoreReady()){
				if(id>0&&rc.canBuild(movingDirection,  RobotType.VIPER)){
					rc.build(movingDirection,  RobotType.VIPER);
					return;
				}
				FuncLib.fowardish(movingDirection);
			}

		}


	}

	
}