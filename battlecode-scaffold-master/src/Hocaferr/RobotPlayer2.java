package Hocaferr;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer2{

	// Variáveis relacionadas a Destino ou direção
	static Direction movingDirection = Direction.NORTH_EAST;	
	static int[] possibleDirections = new int[]{0,1,-1,2,-2,3,-3,4};
	static MapLocation archonLocation;
	static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>(); // localizacoes passadas
	static int[] tryDirections = {0,-1,1,-2,2}; // revisar este como necessário?
	
	static RobotController rc;
	static int id = 0; // para facilitar os outros robos a pegar o lider - colocar em -1
	static int patient = 30; //paciencia para ficar no mesmo lugar até o máximo de 30	
	static Random rnd;
	static RobotType[] buildList = new RobotType[]{RobotType.GUARD,RobotType.TURRET};
	
	public static void run(RobotController rcIn) throws GameActionException{
		rc = rcIn;
		archonLocation = rc.getLocation();

		if(rc.getTeam()==Team.B)
			movingDirection = Direction.SOUTH_WEST;

		while(true){
			try {
				if(rc.getType()==RobotType.ARCHON){
					archonCode();
				}  //else if(rc.getType()==RobotType.TURRET){
				//	turretCode();
				//}else if(rc.getType()==RobotType.TTM){
				//	ttmCode();
				//}else if(rc.getType()==RobotType.GUARD){
				//	guardCode();
				//}
			}catch(Exception e){
				e.printStackTrace();
			}

			Clock.yield();
			//				signalling();
			//				repeat();
			// o comando de movimentar pode gerar uma exceção e ela deve ser tratada, 
			// neste caso apenas coloca na lista da stack de console
		}
	}	

	
	//ARCHON CODE
	private static void archonCode() throws GameActionException {


		// Comunicacao
		if(rc.getRoundNum()==0){
			Signal[] incomingMessages = rc.emptySignalQueue();
			//rc.setIndicatorString(0,""+incomingMessages.length+"messages received");
			id = incomingMessages.length;
			rc.broadcastMessageSignal(0, 0, 100);
			return;
		}else{
			if(id==0){
				sendinstructions(); // diz qual o valor da variável movingDirection
			}else{
				followinstructions(); // pega o valor da variável movingDirection
			}
		}

		if(rc.isCoreReady()){
			// Construir
			rc.setIndicatorString(0, "   teste");
			Direction randomDir = randomDirection();
			RobotType toBuild = buildList[rnd.nextInt(buildList.length)];

			if(rc.getTeamParts()>100){
				if(rc.canBuild(randomDir, toBuild)){
					rc.build(randomDir,toBuild);
					rc.setIndicatorString(0,"building messages");

				}else {
					//Reparar	
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if(weakestOne!=null){
						rc.repair(weakestOne);
						rc.setIndicatorString(0,"reparing messages");

					}
				}
			}
			
			if(rc.canMove(movingDirection)){
				//Mover
				fowardish(movingDirection);
				rc.setIndicatorString(0, movingDirection + "   moving messages");
			}
		}

	}
	
	//TURRET CODE
	private static void turretCode() throws GameActionException {
		
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(visibleEnemyArray,incomingSignals);
		
		if(enemyArray.length>0){
			if(rc.isWeaponReady()){
				//look for adjacent enemies to attack
				for(MapLocation oneEnemy:enemyArray){
					if(rc.canAttackLocation(oneEnemy)){
						rc.setIndicatorString(0,"trying to attack");
						rc.attackLocation(oneEnemy);
						break;
					}
				}
			}
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0];
				Direction toEnemy = rc.getLocation().directionTo(goal);
				rc.pack();
			}
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = randomDirection();
					rc.pack();
				}
			}
		}
	}
	
	// TTM CODE
	private static void ttmCode() throws GameActionException {
		RobotInfo[] visibleEnemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		Signal[] incomingSignals = rc.emptySignalQueue();
		MapLocation[] enemyArray = combineThings(visibleEnemyArray,incomingSignals);
		
		if(enemyArray.length>0){
			rc.unpack();
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0];
				Direction toEnemy = rc.getLocation().directionTo(goal);
				tryToMove(toEnemy);
			}
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = randomDirection();
					tryToMove(away);
				}else{//maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000,rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if(weakestOne!=null){//found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						tryToMove(towardFriend);
					}
				}
			}
		}
	}
	
	
	
	
	

	//GUARD CODE
	private static void guardCode() throws GameActionException {
		RobotInfo[] enemyArray = rc.senseHostileRobots(rc.getLocation(), 1000000);
		
		if(enemyArray.length>0){
			if(rc.isWeaponReady()){
				//look for adjacent enemies to attack
				for(RobotInfo oneEnemy:enemyArray){
					if(rc.canAttackLocation(oneEnemy.location)){
						rc.setIndicatorString(0,"trying to attack");
						rc.attackLocation(oneEnemy.location);
						break;
					}
				}
			}
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0].location;
				Direction toEnemy = rc.getLocation().directionTo(goal);
				tryToMove(toEnemy);
			}
		}else{//there are no enemies nearby
			//check to see if we are in the way of friends
			//we are obstructing them
			if(rc.isCoreReady()){
				RobotInfo[] nearbyFriends = rc.senseNearbyRobots(2, rc.getTeam());
				if(nearbyFriends.length>3){
					Direction away = randomDirection();
					tryToMove(away);
				}else{//maybe a friend is in need!
					RobotInfo[] alliesToHelp = rc.senseNearbyRobots(1000000,rc.getTeam());
					MapLocation weakestOne = findWeakest(alliesToHelp);
					if(weakestOne!=null){//found a friend most in need
						Direction towardFriend = rc.getLocation().directionTo(weakestOne);
						tryToMove(towardFriend);
					}
				}
			}
		}
	}
	
	
	
	
	
	// revisar necessidade
	private static void repeat() throws GameActionException{
		RobotInfo[] zombieEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,Team.ZOMBIE);
		RobotInfo[] normalEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,rc.getTeam().opponent());
		RobotInfo[] opponentEnemies = joinRobotInfo(zombieEnemies, normalEnemies);
		
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
				fowardish(movingDirection);
			}

		}


	}
	
	
	
	
	
	// Funções de sustentação
	//
	//
	
	
	// Função para determinar a destino
	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
	private static MapLocation findWeakest(RobotInfo[] listOfRobots){
		double weakestSoFar = 0;
		MapLocation weakestLocation = null;
		for(RobotInfo r:listOfRobots){
			double weakness = r.maxHealth-r.health;
			if(weakness>weakestSoFar){
				weakestLocation = r.location;
				weakestSoFar=weakness;
			}
		}
		return weakestLocation;
	}

	private static MapLocation[] combineThings(RobotInfo[] visibleEnemyArray, Signal[] incomingSignals) {
		ArrayList<MapLocation> attackableEnemyArray = new ArrayList<MapLocation>();
		for(RobotInfo r:visibleEnemyArray){
			attackableEnemyArray.add(r.location);
		}
		for(Signal s:incomingSignals){
			if(s.getTeam()==rc.getTeam().opponent()){
				MapLocation enemySignalLocation = s.getLocation();
				int distanceToSignalingEnemy = rc.getLocation().distanceSquaredTo(enemySignalLocation);
				if(distanceToSignalingEnemy<=rc.getType().attackRadiusSquared){
					attackableEnemyArray.add(enemySignalLocation);
				}
			}
		}
		MapLocation[] finishedArray = new MapLocation[attackableEnemyArray.size()];
		for(int i=0;i<attackableEnemyArray.size();i++){
			finishedArray[i]=attackableEnemyArray.get(i);
		}
		return finishedArray;
	}
	
	public static void tryToMove(Direction forward) throws GameActionException{
		if(rc.isCoreReady()){
			for(int deltaD:tryDirections){
				Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
				if(rc.canMove(maybeForward)){
					rc.move(maybeForward);
					return;
				}
			}
			if(rc.getType().canClearRubble()){
				//failed to move, look to clear rubble
				MapLocation ahead = rc.getLocation().add(forward);
				if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
					rc.clearRubble(forward);
				}
			}
		}
	}
	
	private static void sendinstructions() throws GameActionException {
		MapLocation aheadLocation = rc.getLocation().add(movingDirection.dx*4, movingDirection.dy*4);
		if(!rc.onTheMap(aheadLocation)){
			movingDirection = randomDirection();
			return;
		}
		rc.broadcastMessageSignal(rc.getTeam().ordinal(), movingDirection.ordinal(), 10000); //broadcast range to 10000
		movingDirection = rc.getLocation().directionTo(aheadLocation);		
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
	
	private static void fowardish(Direction ahead) throws GameActionException {
		
		int id = RobotPlayer2.id;
		int waitTurns = id==0?6:1;
		//if(rc.getRoundNum()%waitTurns==0){
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
		//}
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