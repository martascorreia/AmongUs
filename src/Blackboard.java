import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jade.core.Agent;

public class Blackboard {

	private Map<String, Position> players;
	private Map<String, Position> tasks;
	private List<String> imposters;
	private int numOfImposters;
	private int numOfCrewmates;
	private int numOfPlayers;
	private static final int LINES = 14;
	private static final int COLUMNS = 31;		

	private TypeOfPosition[] map;
	
	private static Blackboard blackboard;
	
	private Blackboard() {
		this.players = new HashMap<>();
		this.tasks = new HashMap<>();
		this.imposters = new ArrayList<>();
	}
	
	public static Blackboard getInstance() {
		if(blackboard == null) blackboard = new Blackboard();
		return blackboard;
	}
	
	public Position getPlayerPosition(String key) {
		return players.get(key);
	}
	
	public Position setPlayerPosition(String key, int x, int y) {
		if(players.containsKey(key)) players.remove(key);
		return players.put(key, new Position(x, y));

	}
	
	public Map<String, Position> getPlayersPositions(){
		Map<String, Position> newMap = new HashMap<>();
		newMap.putAll(players);
		return newMap;
	}
	
	public TypeOfPosition[] getMap() {
		return map;
	}

	public void setMap(TypeOfPosition[] map) {
		this.map = map;		
	}
	
	public Position getTaskPosition(String key) {
		return tasks.get(key);
	}
	
	public void setTaskPosition(String key, Position value) {
		tasks.put(key, value);
	}
	
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
	
	public void setImposters(String color) {
		imposters.add(color);
	}
	
	public List<String> getImposters() {
		return imposters;
	}
	
	public List<String> getAllPlayers() {
		List<String> result = new ArrayList<String>();
		for(String agent: players.keySet()) {
			result.add(agent);
		}
		
		return result;
	}
	
	public int getCollums() {
		return COLUMNS;
	}
	
	public int getLines() {
		return LINES;
	}
	
}
