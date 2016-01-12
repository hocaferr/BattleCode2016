package Hocaferr2;

import java.util.ArrayList;

import battlecode.common.*;

public class FuncLib{

	static int[] possibleDirections = new int[]{0,1,-1,2,-2,3,-3,4};
	static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>(); // localizacoes passadas
	static int patient = 30; //paciencia para ficar no mesmo lugar até o máximo de 30	

	
	public static void fowardish(Direction ahead) throws GameActionException {
		RobotController rc = RobotPlayer.rc; // mos que usar em todas as funcs esta declaracao para buscar o valor da variavel
		int id = RobotPlayer.id;
		int waitTurns = id==0?6:1;
		if(rc.getRoundNum()%waitTurns==0){
			for(int i:possibleDirections){
				Direction candidateDirection = Direction.values()[(ahead.ordinal()+i+8)%8];
				MapLocation candidateLocation = rc.getLocation().add(candidateDirection);
				if(patient>0){
					if(rc.canMove(candidateDirection)&&!pastLocations.contains(candidateLocation)){
						pastLocations.add(rc.getLocation());
						if(pastLocations.size()>5) // numero de posições no arrraylist de localizacões passadas
							pastLocations.remove(0);
						rc.move(candidateDirection);
						patient= Math.min(patient +1,  30);
						return;
					}
				}else{
					if(rc.canMove(candidateDirection)){
						rc.move(candidateDirection);
						patient= Math.min(patient +1,  30);
						return;
					}else{ // dig - cavar ou tirar o obstaculo?
						if(rc.senseRubble(candidateLocation)>GameConstants.RUBBLE_SLOW_THRESH){
							rc.clearRubble(candidateDirection);
							return;
						}
					}
				}
			}
			patient = patient - 5;
		}
	}

	public static RobotInfo[] joinRobotInfo(RobotInfo[] zombieEnemies, RobotInfo[] normalEnemies) {
		RobotInfo[] opponentEnemies = new RobotInfo[zombieEnemies.length+normalEnemies.length];
		int index = 0;
		for ( RobotInfo i:zombieEnemies){
			opponentEnemies[index]=i;
			index++;
			
		}
		
		
		return opponentEnemies;
	}
}