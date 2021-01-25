

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Game extends Agent {

	private static final long serialVersionUID = 1L;
	
	// States
	private final String PLAYING = "Playing";
	private final String MEETING = "Meeting";
	private final String EMERGENCY = "Emergency";
	private static final String OVER = "Over"; 
	
	// Colors
	public final String RESET = "\u001B[0m";
	public final String BLACK = "\033[0;30m";  
	public final String RED = "\033[0;91m";    
    public final String GREEN = "\033[0;32m";   
    public final String LIME = "\033[0;92m";  
    public final String YELLOW = "\033[0;33m"; 
    public final String BLUE = "\033[0;94m";   
    public final String MAGENTA = "\033[0;95m";
    public final String CYAN = "\033[0;96m";   
    public final String WHITE = "\033[0;97m";  
    public final String PURPLE = "\033[0;35m";  
	
	private int numOfImposters;
	private int numOfCrewmates;
	private final int LINES = 14;
	private final int COLUMNS = 31;
	private TypeOfPosition[] map;
	private final Blackboard bb = Blackboard.getInstance();
		
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
			int numPlayers = Integer.parseInt((String) args[0]);
			int numImposters = Integer.parseInt((String) args[1]);
			
			if(numPlayers > 10 || numPlayers < 4) {
				System.out.println("Number of players should be between 4 and 10.");
				return;
			}
			
			if(numImposters > 3 || numImposters < 1) {
				System.out.println("Number of imposters should be between 1 and 3.");
				return;
			}
			
			if(numPlayers < 5 && numImposters > 1) {
				numOfCrewmates = numPlayers - numImposters;
				numOfImposters = 1;
				
			} else if (numPlayers < 8 && numImposters > 2) {
				numOfCrewmates = numPlayers - numImposters;
				numOfImposters = 2;
				
			} else {
				numOfCrewmates = numPlayers - numImposters;
				numOfImposters = numImposters;
			}

		} else {
			System.out.println("Parameters should be in the format: game:Game(numOfPlayers, numOfImposters)");
			return;
		}
		
		createMap();
		createAgents();
		createBehaviour();
		
	}	
	
	private void createBehaviour() {
		// FSM BEHAVIOUR
		FSMBehaviour game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};
		
		TickerBehaviour prints = new TickerBehaviour(this,1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {
				printMap();	
			}
		};

		// Registers the states of the Ant
		game.registerFirstState(new Playing(), PLAYING);
		game.registerState(new Meeting(), MEETING);
		game.registerState(new Emergency(),EMERGENCY);
		game.registerLastState(new Over(), OVER);

		// Registers the transitions	
		// PLAYING
		game.registerTransition(PLAYING, PLAYING, 0);
		game.registerTransition(PLAYING, MEETING, 1);	
		game.registerTransition(PLAYING, EMERGENCY, 2);
		game.registerTransition(PLAYING, OVER, 3);	

		// MEETING
		game.registerTransition(MEETING, PLAYING, 0);
		game.registerTransition(MEETING, OVER, 1);
		
		// EMERGENCY
		game.registerTransition(EMERGENCY, EMERGENCY, 0);
		game.registerTransition(EMERGENCY, PLAYING, 1);
		game.registerTransition(EMERGENCY, OVER, 2);
		
		ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
        addBehaviour(tbf.wrap(game));
        addBehaviour(tbf.wrap(prints));
	
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
		
		bb.setMap(map);
	}

	public void printMap() {
		int j = 0;
		int i = 0;
		int index = 0;
		boolean isAgent;
		
		Map<String, Position> maps = bb.getPositions();
		Set<String> keys = maps.keySet();
		Map<Integer, String> agentsPositions = new HashMap<>();
		List<String> imposters = bb.getImposters();
		

		for(String key : keys) {
			Position pos = maps.get(key);
			int ind = pos.getX() + pos.getY() * COLUMNS;
			agentsPositions.put(ind, key);
		}
				
		for(int x = 0; x < LINES * COLUMNS; x++, j++) {
			isAgent = false;

			if(j == COLUMNS) {
				i++;
				j = 0;
				System.out.println();
			}
			
			index = j + i * COLUMNS;
			String symbol = "";			
			
			if(agentsPositions.containsKey(index)) {
				String key = agentsPositions.get(index);
				isAgent = true;
				symbol = " ";
												
				if(imposters.contains(key)) symbol = "I";
				else symbol = "C";
					
				if(key.equals(Colors.RED.toString())) {
					System.out.print(RED + symbol + RESET);
										
				} else if(key.equals(Colors.BLUE.toString())) {
					System.out.print(BLUE + symbol + RESET);
				
				} else if(key.equals(Colors.YELLOW.toString())) {
					System.out.print(YELLOW + symbol + RESET);
					
				} else if(key.equals(Colors.GREEN.toString())) {
					System.out.print(GREEN + symbol + RESET);
				
				} else if(key.equals(Colors.LIME.toString())) {
					System.out.print(LIME + symbol + RESET);
					
				} else if(key.equals(Colors.PURPLE.toString())) {
					System.out.print(PURPLE + symbol +RESET);
					
				} else if(key.equals(Colors.BLACK.toString())) {
					System.out.print(BLACK + symbol + RESET);
					
				} else if(key.equals(Colors.WHITE.toString())) {
					System.out.print(WHITE + symbol + RESET);
					
				} else if(key.equals(Colors.CYAN.toString())) {
					System.out.print(CYAN + symbol + RESET);
					
				} else if(key.equals(Colors.MAGENTA.toString())) {
					System.out.print(MAGENTA + symbol + RESET);
				}
			}
			
			if(map[index] == TypeOfPosition.VENT && !isAgent){
				System.out.print("#");
				
			} else if((map[index] == TypeOfPosition.SHIELDS || map[index] == TypeOfPosition.FILLGAS 
					|| map[index] == TypeOfPosition.CARDSWIPE || map[index] == TypeOfPosition.ASTEROIDS
					|| map[index] == TypeOfPosition.DOWNLOAD || map[index] == TypeOfPosition.UPLOAD
					|| map[index] == TypeOfPosition.TRASH || map[index] == TypeOfPosition.WIRES 
					|| map[index] == TypeOfPosition.MEDBAY) && !isAgent){
				System.out.print("T");
			} else if(map[index] == TypeOfPosition.WALL && !isAgent) {
				System.out.print("|");
			} else if(map[index] == TypeOfPosition.O2 && !isAgent){
				System.out.print("O");
			} else if(map[index] == TypeOfPosition.REACTOR && !isAgent) {
				System.out.print("R");
			} else if(map[index] == TypeOfPosition.LIGHTS && !isAgent) {
				System.out.print("L");
			} else if(!isAgent){
				System.out.print(" ");
			}				
		}
		
		System.out.println();
	}
	

	private void createAgents() {		
		Colors[] types = Colors.values();
		TypeOfPosition[] tasks = TypeOfPosition.values();
		int type = 0;
		
		AgentContainer c = getContainerController();
		Random random = new Random();
		
		for(int i = 0; i < numOfCrewmates; i++, type++) {			
			try {
				Colors typeOfPosition = types[type];				
				Object args[] = new Object[3];
				args[0] = tasks[random.nextInt(14 - 7 + 1) + 7];
				args[1] = tasks[random.nextInt(14 - 7 + 1) + 7];
				args[2] = tasks[random.nextInt(14 - 7 + 1) + 7];
				
				AgentController crew = c.createNewAgent(typeOfPosition.toString(), "Crewmate", args);
				crew.start();
				bb.setPosition(typeOfPosition.toString(), 11, 5);
			} catch (StaleProxyException e) {
				System.out.println("Error while creating a Crewmate.");
			}
		}
		
		for(int i = 0; i < numOfImposters; i++, type++) {
			try {
				Colors typeOfPosition = types[type];
				AgentController imp = c.createNewAgent(typeOfPosition.toString(), "Imposter", null);
				imp.start();
				bb.setPosition(typeOfPosition.toString(), 11, 4);
				bb.setImposters(typeOfPosition.toString());
			} catch (StaleProxyException e) {
				System.out.println("Error while creating an Imposter.");
			}
		}		
		
		//printMap();
	}

	public class Playing extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			while(true) {
				ACLMessage msg = myAgent.receive();

				if(msg != null) {
					String[] message = msg.getContent().split(" ");

					if(message[0].equals("Body") ) { // Body YELLOW
						System.out.println(msg.getSender() + " found " + message[1] + "'s body.");	
						endValue = 1;
						break;
						
					} else if(message[0].equals("Meeting")) { // Meeting
						System.out.println(msg.getSender() + " called a meeting.");	
						endValue = 1;
						break;
					}		
				}
			}		
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
	
	public class Emergency extends OneShotBehaviour {
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

		@Override
		public void action() {
			
		}	
	}

}
