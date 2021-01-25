import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jade.core.Agent;

public class Blackboard {

	private Map<String, Position> positions;
	private List<String> imposters;
	private int numOfImposters;
	private int numOfCrewmates;
	private int numOfPlayers;
	private static final int LINES = 14;
	private static final int COLUMNS = 31;		

	private TypeOfPosition[] map;
	
	private static Blackboard blackboard;
	
	private Blackboard() {
		this.positions = new HashMap<>();
		this.imposters = new ArrayList<>();
	}
	
	public static Blackboard getInstance() {
		if(blackboard == null) blackboard = new Blackboard();
		return blackboard;
	}
	
	public Position getPosition(String key) {
		return positions.get(key);
	}
	
	public Position setPosition(String key, int x, int y) {
		if(positions.containsKey(key)) positions.remove(key);
		return positions.put(key, new Position(x, y));
	}
	
	public Map<String, Position> getPositions(){
		return positions;
	}
	
	public TypeOfPosition[] getMap() {
		return map;
	}

	public void setMap(TypeOfPosition[] map) {
		this.map = map;		
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
		for(String agent: positions.keySet()) {
			result.add(agent);
		}
		
		return result;
	}
	
	public int getCollums() {
		return this.COLUMNS;
	}
	
	public int getLines() {
		return this.LINES;
	}
	
}
