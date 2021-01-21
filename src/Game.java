
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
		// create agents
		//createAgents();
		
		/*

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

		addBehaviour(game);*/
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
					
			if(index == 96 || index == 196 || index == 204 || index == 251 || index == 256 || index == 258 || index == 272 || index == 313 || index == 328) {
				map[index] = TypeOfPosition.VENT;
				
			} else if(index == 35 || index == 36 || (index >= 45 && index <= 49) || (index >= 52 && index <= 55) || (index >= 67 && index <= 84) 
					|| index == 86 || index == 97 || index == 98 || index == 104 || index == 107 || index == 108 || index == 111 || index == 112
					|| (index >= 114 && index <= 117) || index == 129 || (index >= 134 && index <= 136) || index == 138 || index == 139
					|| index == 142 || index == 143 || index == 147 || index == 156 || index == 158 || index == 160 || index == 162 
					|| index == 163 || (index >= 165 && index <= 167) || (index >= 169 && index <= 174) || index == 176 
					|| (index >= 178 && index <= 180) || (index >= 182 && index <= 184) || (index >= 187 &&  index <= 189) || index == 191 
					|| index == 193 || index == 194 || index == 197
					|| (index >= 200 && index <= 203) || index == 205 || (index >= 207 && index <= 211) || index == 213 || index == 214
					|| (index >= 218 && index <= 225) || index == 232 || index == 263
					|| index == 233 || (index >= 242 && index <= 246) || index == 249 || index == 250 || index == 253 || index == 255 
					|| index == 260 || (index >= 264 && index <= 270) || index == 273 || (index >= 275 && index <= 277) || index == 284 
					|| index == 291 || (index >= 293 && index <= 295) || (index >= 297 && index <= 299) || index == 301 || index == 303 
					|| index == 304 || (index >= 314 && index <= 316) || index == 321 || index == 322 || index == 324 || index == 326 
					|| (index >= 329 && index <= 332) || index == 334 || index == 335 || (index >= 346 && index <= 349)
					|| index == 351 || index == 355 || index == 357 || index == 365 || index == 366 || (index >= 376 && index <= 378)
					|| (index >= 380 && index <= 386) || (index >= 388 && index <= 396)) {
				map[index] = TypeOfPosition.NORMAL;
				
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
				map[index] = TypeOfPosition.WALL;

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
					|| map[index] == TypeOfPosition.TRASH || map[index] == TypeOfPosition.WIRES || map[index] == TypeOfPosition.MEDBAY){
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
				System.out.print(" ");
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
