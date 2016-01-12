package Hocaferr1;

import java.util.ArrayList;

import battlecode.common.*;

public class RobotPlayer{

	static Direction movingDirection=Direction.NORTH_EAST;	
	static RobotController rc;
	static int[] possibleDirections = new int[]{0,1,-1,2,-2,3,-3,4};
	static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>(); // localizacoes passadas
	static boolean patient = true; //paciencia para ficar no mesmo lugar
	
	public static void run(RobotController rcIn){
		rc=rcIn;

		if(rc.getTeam()==Team.B)
			movingDirection=Direction.SOUTH_WEST;

		while(true){
			try {

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

	public static void repeat() throws GameActionException{
		RobotInfo[] zombieEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,Team.ZOMBIE);
		RobotInfo[] normalEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,rc.getTeam().opponent());
		RobotInfo[] opponentEnimies = joinRobotInfo(zombieEnemies, normalEnemies);
		
		if(zombieEnemies.length>0&&rc.getType().canAttack()){
			if(rc.isWeaponReady()){
				rc.attackLocation(zombieEnemies[0].location);
			}

		}else {

			if(rc.isCoreReady()){
				fowardish(movingDirection);
			}

		}


	}

	private static void fowardish(Direction ahead) throws GameActionException {
		for(int i:possibleDirections){
			Direction candidateDirection = Direction.values()[(ahead.ordinal()+i+8)%8];
			MapLocation candidateLocation = rc.getLocation().add(candidateDirection);
			if(patient){
				if(rc.canMove(candidateDirection)&&!pastLocations.contains(candidateLocation)){
					pastLocations.add(rc.getLocation());
					if(pastLocations.size()>20) // numero de posições no arrraylist de localizacões passadas
						pastLocations.remove(0);
					rc.move(candidateDirection);
					return;
				}
			}else{
				if(rc.canMove(candidateDirection)){
					rc.move(candidateDirection);
					return;
				}else{ // dig - cavar ou tirar o obstaculo?
					if(rc.senseRubble(candidateLocation)>GameConstants.RUBBLE_SLOW_THRESH){
						rc.clearRubble(candidateDirection);
						return;
					}
				}
			}
		}
		patient = false;
	}

	private static RobotInfo[] joinRobotInfo(RobotInfo[] zombieEnemies, RobotInfo[] normalEnemies) {
		RobotInfo[] opponentEnemies = new RobotInfo[zombieEnemies.length+normalEnemies.length];
		int index = 0;
		for ( RobotInfo i:zombieEnemies){
			opponentEnemies[index]=i;
			index++;
			
		}
		
		
		return opponentEnemies;
	}
}