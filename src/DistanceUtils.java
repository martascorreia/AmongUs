import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DistanceUtils {
	
	private final static Blackboard bb = Blackboard.getInstance();

	public static int manDistance(Position p1, Position p2) {
		return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
	}

	public static String closestTask(Position pos, Map<String, Position> tasks) {
		String[] keys = tasks.keySet().toArray(new String[tasks.keySet().size()]);
		String result = keys[0];

		for(String key : keys) {
			if(manDistance(pos, tasks.get(key)) < manDistance(pos, tasks.get(result)))
				result = key;
		}
		
		return result;
	}

	public static Position nextMove(Position my, Position goal) {
		int myX = my.getX();
		int myY = my.getY();
		int goalX = goal.getX();
		int goalY = goal.getY();
		
		if(myX > goalX) {
			return new Position(myX - 1,myY);
			
		}else if(myX < goalX) {
			return new Position(myX + 1,myY);
			
		}else if(myY > goalY) {
			return new Position(myX, myY - 1);
			
		}else {
			return new Position(myX,myY + 1);
		}

	}
	
	public static Position randomMove(Position my) {
		Blackboard bb = Blackboard.getInstance();
		TypeOfPosition[] map = bb.getMap();

		int myX = my.getX();
		int myY = my.getY();
		
		Random gen = new Random();
		int rnd = gen.nextInt(4);
		
		Position newP;		
		if(rnd == 0) {
			newP= new Position(myX+1,myY);
		}else if(rnd == 1) {
			newP= new Position(myX-1,myY);
		}else if(rnd == 2) {
			newP= new Position(myX,myY+1);
		}else {
			newP= new Position(myX,myY-1);
		}
		
		return map[newP.getX() + newP.getY()*bb.getCollums()] == TypeOfPosition.WALL ? randomMove(my): newP;
	}
	
	public static Map<String, Position> getPlayersNearImp(String name, double d, Map<String, Position> players) {
		Position myPosition = bb.getPlayerPosition(name);				
		String[] keys = players.keySet().toArray(new String[players.keySet().size()]);

		Map<String, Position> playersNear = new HashMap<>();
		for(String key : keys) {
			Position value = players.get(key);
			if(DistanceUtils.manDistance(myPosition, value) <= d && !key.equals(name) && !bb.getImposters().contains(key)) {
				playersNear.put(key, value);
			}
		}			
		
		return playersNear;
	}
}

