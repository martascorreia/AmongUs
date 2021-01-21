
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Game extends Agent {

	private static final long serialVersionUID = 1L;
	
	// States
	private static final String PLAYING = "Playing";
	private static final String MEETING = "Meeting";
	private static final String OVER = "Over"; 
	private int numOfImposters;
	private int numOfCrewmates;
	private static final int LINES = 14;
	private static final int COLUMNS = 31;
	private TypeOfPosition[] map;
	private List<Agent> crewmates;
	private List<Agent> imposters;
		
	protected void setup(){		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setName(getLocalName() + "among-us");
		sd.setType("among-us");
		dfd.addServices(sd);

		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			System.out.println("Exception while registering the service!");
			return;
		}		

		// To get number of crewmates and imposters
		Object[] args = getArguments();
		if (args != null && args.length == 2) {
			numOfCrewmates = Integer.parseInt((String) args[0]);
			numOfImposters = Integer.parseInt((String) args[1]);

		} else {
			System.out.println("Parameters should be in the format: game:Game(numOfCrewmates, numOfImposters)");
			return;
		}

		createMap();
		printMap();
		createAgents();
		
		// FSM BEHAVIOUR
		FSMBehaviour game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};

		// Registers the states of the Ant
		game.registerFirstState(new Playing(), PLAYING);
		game.registerState(new Meeting(), MEETING);
		game.registerLastState(new Over(), OVER);

		// Registers the transitions	
		// PLAYING
		game.registerTransition(PLAYING, PLAYING, 0);
		game.registerTransition(PLAYING, MEETING, 1);	
		game.registerTransition(PLAYING, OVER, 2);	

		// MEETING
		game.registerTransition(MEETING, PLAYING, 0);
		game.registerTransition(MEETING, OVER, 1);

		addBehaviour(game);
	}	
	
	private void createMap() {
		this.map = new TypeOfPosition[LINES * COLUMNS];
		
		int j = 0;
		int i = 0;
		Integer index = 0;
		
		for(int x = 0; x < LINES * COLUMNS; x++, j++) {
			if(j == COLUMNS) {
				i++;
				j = 0;
			}
						
			index = j + i * COLUMNS;
					
			if(index == 78 || index == 96 || index == 196 || index == 204 || index == 251 || index == 256 || index == 272 || index == 313 || index == 328) {
				map[index] = TypeOfPosition.VENT;
				
			} else if(i == 0 || i == 13 || j == 0 || j == 30) {
				map[index] = TypeOfPosition.WALL;
				
			} else if(index == 157) {
				map[index] = TypeOfPosition.REACTOR;

			} else if(index == 320) {
				map[index] = TypeOfPosition.LIGHTS;
			
			} else if(index == 177) {
				map[index] = TypeOfPosition.O2;
				
			} else if(index == 34) {
				map[index] = TypeOfPosition.FILLGAS;
				
			} else if(index == 375) {
				map[index] = TypeOfPosition.FILLGAS;
			
			} else if(index == 259) {
				map[index] = TypeOfPosition.WIRES;
				
			} else if(index == 198) {
				map[index] = TypeOfPosition.MEDBAY;
				
			} else if(index == 387) {
				map[index] = TypeOfPosition.TRASH;
			
			} else if(index == 50) {
				map[index] = TypeOfPosition.DOWNLOAD;
			
			} else if(index == 85) {
				map[index] = TypeOfPosition.ASTEROIDS;
			
			} else if(index == 267) {
				map[index] = TypeOfPosition.UPLOAD;
				
			} else if(index == 300) {
				map[index] = TypeOfPosition.CARDSWIPE;
				
			} else if(index == 397) {
				map[index] = TypeOfPosition.SHIELDS;
				
			} else if(index == 215) {
				map[index] = TypeOfPosition.WIRES;
				
			} else {
				map[index] = TypeOfPosition.NORMAL;
			}	
		}
	}

	public void printMap() {
		int j = 0;
		int i = 0;
		int index = 0;
		
		for(int x = 0; x < LINES * COLUMNS; x++, j++) {
			if(j == COLUMNS) {
				i++;
				j = 0;
				System.out.println();
			}
			
			index = j + i * COLUMNS;
			
			if(map[index] == TypeOfPosition.VENT){
				System.out.print("#");
				
			} else if(map[index] == TypeOfPosition.SHIELDS || map[index] == TypeOfPosition.FILLGAS 
					|| map[index] == TypeOfPosition.CARDSWIPE || map[index] == TypeOfPosition.ASTEROIDS
					|| map[index] == TypeOfPosition.DOWNLOAD || map[index] == TypeOfPosition.UPLOAD
					|| map[index] == TypeOfPosition.TRASH || map[index] == TypeOfPosition.WIRES 
					|| map[index] == TypeOfPosition.MEDBAY){
				System.out.print("T");
			} else if(map[index] == TypeOfPosition.WALL) {
				System.out.print("|");
			} else if(map[index] == TypeOfPosition.O2){
				System.out.print("O");
			} else if(map[index] == TypeOfPosition.REACTOR) {
				System.out.print("R");
			} else if(map[index] == TypeOfPosition.LIGHTS) {
				System.out.print("L");
			} else {
				System.out.print("_");
			}				
		}
	}

	/**
	 * 
	 */
	private void createAgents() {
		crewmates = new ArrayList<>();
		imposters = new ArrayList<>();
		
		for(int i = 0; i < numOfCrewmates; i++) {
			Crewmate crew = new Crewmate();
			crewmates.add(crew);
		}
		
		for(int i = 0; i < numOfImposters; i++) {
			Imposter imp = new Imposter();
			imposters.add(imp);
		}	
	}

	/**
	 * 
	 * @author Marta Correia
	 *
	 */
	public class Playing extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			
		}	

		public int onEnd() {
			return endValue;
		}
	}
	
	public class Meeting extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			
		}	

		public int onEnd() {
			return endValue;
		}
	}
	
	public class Over extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			
		}	
	}

}
