import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Blackboard {

	private Map<String, Position> alivePlayers;
	private Map<String, Position> deadPlayers;
	private Map<String, Position> corpses;
	private Map<String, Position> tasks;
	private Map<String, Position> emergencies;
	private List<String> imposters;
	
	private int numOfImposters;
	private int numOfCrewmates;
	private int numOfPlayers;
	
	private boolean isMeeting = false;
	
	private static final int LINES = 14;
	private static final int COLUMNS = 31;
	public int CREWMATE_VISION = 2;
	public int IMPOSTER_VISION = 4;
	public int NUMBER_TASK;
	
	public int tasksDone = 0;
	private boolean emergencyCalling;
	private int imposterKillDistance = 1;

	private TypeOfPosition[] map;
	
	private static Blackboard blackboard;
	
	private Blackboard() {
		this.alivePlayers = new HashMap<>();
		this.deadPlayers = new HashMap<>();
		this.corpses = new HashMap<>();
		this.tasks = new HashMap<>();
		this.emergencies = new HashMap<>();
		this.imposters = new ArrayList<>();
		this.emergencyCalling = false;
	}
	
	public static Blackboard getInstance() {
		if(blackboard == null) blackboard = new Blackboard();
		return blackboard;
	}
	
	// PLAYERS
	public Position getPlayerPosition(String key) {
		if(deadPlayers.containsKey(key)) return deadPlayers.get(key);
		return alivePlayers.get(key);
	}
	
	public void setPlayerPosition(String key, int x, int y) {
		if(deadPlayers.containsKey(key)) {
			if(deadPlayers.containsKey(key)) deadPlayers.replace(key , new Position(x, y));
			else deadPlayers.put(key, new Position(x, y));
		} else {
			if(alivePlayers.containsKey(key)) alivePlayers.replace(key , new Position(x, y));
			else alivePlayers.put(key, new Position(x, y));
		}		
	}
	
	public Map<String, Position> getAlivePlayers(){
		Map<String, Position> newMap = new HashMap<>();
		synchronized(alivePlayers) {
			newMap.putAll(alivePlayers);
		}
		return newMap;
	}

	public List<String> getAllAlivePlayers() {
		List<String> result = new ArrayList<String>();
		for(String agent: alivePlayers.keySet()) {
			result.add(agent);
		}
		
		return result;
	}
	
	// MAP	
	public TypeOfPosition[] getMap() {
		return map;
	}

	public void setMap(TypeOfPosition[] map) {
		this.map = map;		
	}
	
	// TASKS
	public Position getTaskPosition(String key) {
		return tasks.get(key);
	}
	
	public void setTaskPosition(String key, Position value) {
		tasks.put(key, value);
	}
	
	// EMERGENCIES
	public Position getEmergencyPosition(String key) {
		return emergencies.get(key);
	}
	
	public void setEmergencyPosition(String key, Position value) {
		emergencies.put(key, value);
	}
	
	// VARIABLES
	public void setNum(int numOfPlayers, int numOfImposters) {
		this.numOfPlayers = numOfPlayers;
		this.numOfCrewmates = numOfPlayers - numOfImposters;
		this.numOfImposters = numOfImposters;
		this.NUMBER_TASK = numOfCrewmates * 3;
	}
	
	public int getNumOfPlayers() {
		return numOfPlayers;
	}
	
	public int getNumOfCrewmates() {
		return numOfPlayers;
	}
	
	public int getNumOfImposters() {
		return numOfPlayers;
	}
	
	public int getCollums() {
		return COLUMNS;
	}
	
	public int getLines() {
		return LINES;
	}
	
	// IMPOSTER
	public void setImposters(String color) {
		imposters.add(color);
	}
	
	public List<String> getImposters() {
		return imposters;
	}
	
	// EMERGENCIES
	public void setEmergencyCalling(boolean value) {
		this.emergencyCalling = value;
	}
	
	public boolean getEmergencyCalling() {
		return this.emergencyCalling;
	}
	
	// DISTANCES
	public int getImposterVision() {
		return IMPOSTER_VISION;
	}
	
	public int getCrewmateVision() {
		return CREWMATE_VISION;
	}
	
	public int getDistanceKill() {
		return imposterKillDistance;
	}
	
	public void setCrewmateVision(int vision) {
		CREWMATE_VISION = vision;
	}
	
	// DEAD
	public Map<String, Position> getDeadPlayers() {
		return deadPlayers;
	}
	
	public void setPlayerAsDead(String key, int x, int y) {
		alivePlayers.remove(key);
		deadPlayers.put(key, new Position (x, y));
	}
	
	// CORPSES
	public Map<String, Position> getCorpsesPlayers() {
		return corpses;
	}
	
	public void setPlayerAsCorpse(String key, int x, int y) {
		corpses.put(key, new Position (x, y));
	}
	
	public void resetCorpses() {
		corpses.clear();
	}
	
	public void incrementTaskDone() {
		this.tasksDone++;
	}
	
	public int getTasksDone() {
		return tasksDone;
	}
	
	public List<String> getAllPlayers(){
		List<String> allPlayers = new ArrayList<>();
	
		for(String name : this.alivePlayers.keySet()) {
			allPlayers.add(name);
		}
		for(String name : this.deadPlayers.keySet()) {
			allPlayers.add(name);
		}
		
		return allPlayers;
	}
	
	public String getLocal(Position p) {
		TypeOfPosition closest = TypeOfPosition.MEETING;
		Position closestP = new Position(10,4);
		int x = 0, y = 0;
		for(int i = 0; i < map.length; x++,i++) {
			if(x == COLUMNS) {
               y++;
               x = 0;
            }
			if(map[i] != TypeOfPosition.WALL && map[i] != TypeOfPosition.NORMAL) {
				Position testP =  new Position(x,y);
				if(DistanceUtils.manDistance(p,testP) < DistanceUtils.manDistance(p,closestP)){
					closestP = testP;
					closest = map[i];
				}
			}
		}
		return closest.toString();
	}
	
	public Position getPosition(String name) {
		TypeOfPosition position = TypeOfPosition.valueOf(name);
		int x = 0, y = 0;
		for(int i = 0; i < map.length; x++,i++) {
			if(x == COLUMNS) {
               y++;
               x = 0;
            }
			
			if(map[i].equals(position)) {
				return new Position(x, y);
			}
		}
		return null;
	}


	public boolean isTask(Position p) {
		Iterator<String> iter = tasks.keySet().iterator();
		while(iter.hasNext()) {
			String key = iter.next();
			Position value = tasks.get(key);
			if(value.getX() == p.getX() && value.getY() == p.getY())
				return true;
		}
		return false;
	}
	
	public void setMeeting() {
		if(isMeeting) isMeeting = false;
		else isMeeting = true;
	}
	
	public boolean isMeeting() {
		return isMeeting;
	}
	
	
}
