import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Blackboard {

	private Map<String, Position> players;
	private Map<String, Position> tasks;
	private Map<String, Position> emergencies;
	private List<String> imposters;
	private int numOfImposters;
	private int numOfCrewmates;
	private int numOfPlayers;
	private static final int LINES = 14;
	private static final int COLUMNS = 31;
	private boolean emergencyCalling;

	private TypeOfPosition[] map;
	
	private static Blackboard blackboard;
	
	private Blackboard() {
		this.players = new HashMap<>();
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
		return players.get(key);
	}
	
	public void setPlayerPosition(String key, int x, int y) {
		if(players.containsKey(key)) players.replace(key , new Position(x, y));
		else players.put(key, new Position(x, y));
	}
	
	public Map<String, Position> getPlayersPositions(){
		Map<String, Position> newMap = new HashMap<>();
		newMap.putAll(players);
		return newMap;
	}

	public List<String> getAllPlayers() {
		List<String> result = new ArrayList<String>();
		for(String agent: players.keySet()) {
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
	
	public void setEmergencyCalling(boolean value) {
		this.emergencyCalling = value;
	}
	
	public boolean getEmergencyCalling() {
		return this.emergencyCalling;
	}
}
