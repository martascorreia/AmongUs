import java.util.List;
import java.util.Random;


public class DistanceUtils {

	public static int manDistance(Position p1, Position p2) {
		return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
	}

	public static Position closestTask(Position pos, List<Position> tasks) {
		Position result = tasks.get(0);
		
		for(Position p : tasks) {
			if(manDistance(pos, p) < manDistance(pos, result))
				result = p;
		}
		
		return result;
	}

	public static Position nextMove(Position my, Position goal) {
		int myX = my.getX();
		int myY = my.getY();
		int goalX = goal.getX();
		int goalY = goal.getY();
		
		if(myX > goalX) {
			return new Position(myX-1,myY);
		}else if(myX < goalX) {
			return new Position(myX+1,myY);
		}else if(myY > goalY) {
			return new Position(myX-1,myY);
		}else {
			return new Position(myX-1,myY);
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
}

