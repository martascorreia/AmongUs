import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
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

	private int numOfImposters;
	private int numOfCrewmates;
	private final int LINES = 14;
	private final int COLUMNS = 31;
	private TypeOfPosition[] map;
	private final Blackboard bb = Blackboard.getInstance();
	private Map<String, String> colors;
	private boolean onMeeting;

	// Behaviours
	CyclicBehaviour tasks;
	TickerBehaviour prints;
	FSMBehaviour game;
	ThreadedBehaviourFactory tbf;

	protected void setup(){

		colors = new HashMap<>();
		colors.put("RED", "\033[0;91m");
		colors.put("BLACK", "\033[0;30m");
		colors.put("GREEN", "\033[0;32m");
		colors.put("LIME", "\033[0;92m");
		colors.put("YELLOW", "\033[0;33m");
		colors.put("BLUE", "\033[0;94m");
		colors.put("MAGENTA", "\033[0;95m");
		colors.put("CYAN", "\033[0;96m");
		colors.put("WHITE", "\033[0;97m");
		colors.put("PURPLE", "\033[0;35m");
		onMeeting = false;
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

			/*if(numPlayers > 10 || numPlayers < 4) {
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
			}*/

			numOfCrewmates = numPlayers - numImposters;
			numOfImposters = numImposters;
			bb.setNum(numPlayers, numOfImposters);

		} else {
			System.out.println("Parameters should be in the format: game:Game(numOfPlayers, numOfImposters)");
			return;
		}

		createMap();
		createAgents();
		behaviours();

	}	

	private void behaviours() {
		tasks = new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				int alivePlayers;
				synchronized(bb) {
					alivePlayers= bb.getAlivePlayers().size();
				}
				if(bb.NUMBER_TASK == bb.getTasksDone() || alivePlayers == numOfImposters) {

					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("GameOver");
					List<String> players =  bb.getAllPlayers();

					for(String player : players) {
						msg.addReceiver(new AID(player,AID.ISLOCALNAME));
					}		
					
					msg.addReceiver(new AID("REACTOR",AID.ISLOCALNAME));
					msg.addReceiver(new AID("LIGHTS",AID.ISLOCALNAME));
					msg.addReceiver(new AID("OXYGEN",AID.ISLOCALNAME));
					msg.addReceiver(new AID("Game",AID.ISLOCALNAME));

					send(msg);

					if(bb.NUMBER_TASK == bb.getTasksDone())
						System.out.println("Victory for crewmates");
					else 
						System.out.println("Victory for imposters");
				}			
			}

		};

		prints = new TickerBehaviour(this,1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {
				if(!onMeeting)
					printMap();	
			}
		};

		// FSM BEHAVIOUR
		game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};

		// Registers the states of the Game
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
		game.registerTransition(EMERGENCY, MEETING, 2);
		game.registerTransition(EMERGENCY, OVER, 3);

		tbf = new ThreadedBehaviourFactory();
		addBehaviour(tbf.wrap(game));
		addBehaviour(tbf.wrap(prints));	
		addBehaviour(tbf.wrap(tasks));	

	}

	private void createMap() {
		this.map = new TypeOfPosition[LINES * COLUMNS];

		int x = 0;
		int y = 0;
		Integer index = 0;

		for(int i = 0; i < LINES * COLUMNS; i++, x++) {
			if(x == COLUMNS) {
				y++;
				x = 0;
			}

			index = x + y * COLUMNS;

			if(index == 78 || index == 96 || index == 196 || index == 204 || index == 251 || index == 256 || index == 272 || index == 313 || index == 328) {
				map[index] = TypeOfPosition.VENT;

			} else if(y == 0 || y == 13 || x == 0 || x == 30) {
				map[index] = TypeOfPosition.WALL;

			} else if(index == 134) {
				map[index] = TypeOfPosition.MEETING;

			} else if(index == 157) {
				map[index] = TypeOfPosition.REACTOR;
				bb.setEmergencyPosition(TypeOfPosition.REACTOR.toString(), new Position(x, y));

			} else if(index == 320) {
				map[index] = TypeOfPosition.LIGHTS;
				bb.setEmergencyPosition(TypeOfPosition.LIGHTS.toString(), new Position(x, y));

			} else if(index == 177) {
				map[index] = TypeOfPosition.O2;
				bb.setEmergencyPosition(TypeOfPosition.O2.toString(), new Position(x, y));

			} else if(index == 34) {
				map[index] = TypeOfPosition.FILLGAS;
				bb.setTaskPosition(TypeOfPosition.FILLGAS.toString(), new Position(x, y));
			
			} else if(index == 344) {
				map[index] = TypeOfPosition.FILLGAS2;
				bb.setTaskPosition(TypeOfPosition.FILLGAS2.toString(), new Position(x, y));

			} else if(index == 375) {
				map[index] = TypeOfPosition.LEAVES;
				bb.setTaskPosition(TypeOfPosition.LEAVES.toString(), new Position(x, y));

			} else if(index == 259) {
				map[index] = TypeOfPosition.WIRES1;
				bb.setTaskPosition(TypeOfPosition.WIRES1.toString(), new Position(x, y));

			} else if(index == 198) {
				map[index] = TypeOfPosition.MEDBAY;
				bb.setTaskPosition(TypeOfPosition.MEDBAY.toString(), new Position(x, y));

			} else if(index == 387) {
				map[index] = TypeOfPosition.TRASH;
				bb.setTaskPosition(TypeOfPosition.TRASH.toString(), new Position(x, y));

			} else if(index == 50) {
				map[index] = TypeOfPosition.DOWNLOAD;
				bb.setTaskPosition(TypeOfPosition.DOWNLOAD.toString(), new Position(x, y));

			} else if(index == 85) {
				map[index] = TypeOfPosition.ASTEROIDS;
				bb.setTaskPosition(TypeOfPosition.ASTEROIDS.toString(), new Position(x, y));

			} else if(index == 267) {
				map[index] = TypeOfPosition.UPLOAD;
				bb.setTaskPosition(TypeOfPosition.UPLOAD.toString(), new Position(x, y));

			} else if(index == 300) {
				map[index] = TypeOfPosition.CARDSWIPE;
				bb.setTaskPosition(TypeOfPosition.CARDSWIPE.toString(), new Position(x, y));

			} else if(index == 397) {
				map[index] = TypeOfPosition.SHIELDS;
				bb.setTaskPosition(TypeOfPosition.SHIELDS.toString(), new Position(x, y));

			} else if(index == 215) {
				map[index] = TypeOfPosition.WIRES2;
				bb.setTaskPosition(TypeOfPosition.WIRES2.toString(), new Position(x, y));
			
			} else if(index == 70) {
				map[index] = TypeOfPosition.ONETOTEN;
				bb.setTaskPosition(TypeOfPosition.ONETOTEN.toString(), new Position(x, y));

			} else if(index == 218) {
				map[index] = TypeOfPosition.SIMONSAYS;
				bb.setTaskPosition(TypeOfPosition.SIMONSAYS.toString(), new Position(x, y));
					
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
		
		int tasksDone = bb.getTasksDone();
		System.out.print(" ");
		for(int k = 0; k < numOfCrewmates * 3 ; k++) {
			System.out.print("--");
		}
		System.out.println();
		System.out.print("|");
		int l = tasksDone;
		for(int k = 0; k < numOfCrewmates * 3; k++, l--) {
			if(l <= 0) System.out.print("  ");
			else System.out.print("||");
		}
		System.out.println("|");
		
		System.out.print(" ");
		for(int k = 0; k < numOfCrewmates * 3; k++) {
			System.out.print("--");
		}
		
		System.out.println();

		Map<String, Position> alivePlayers = bb.getAlivePlayers();
		Map<String, Position> deadPlayers = bb.getDeadPlayers();
		Map<String, Position> corpses = bb.getCorpsesPlayers();
		List<String> imposters = bb.getImposters();

		Set<String> keys = alivePlayers.keySet();
		Map<Integer, String> agentsPositions = new HashMap<>();
		for(String key : keys) {
			Position pos = alivePlayers.get(key);
			int ind = pos.getX() + pos.getY() * COLUMNS;
			agentsPositions.put(ind, key);
		}

		Set<String> keysD = deadPlayers.keySet();
		Map<Integer, String> deadPositions = new HashMap<>();
		for(String key : keysD) {
			Position pos = deadPlayers.get(key);
			int ind = pos.getX() + pos.getY() * COLUMNS;
			deadPositions.put(ind, key);
		}

		Set<String> keysC = corpses.keySet();
		Map<Integer, String> corpsesPositions = new HashMap<>();
		for(String key : keysC) {
			Position pos = corpses.get(key);
			int ind = pos.getX() + pos.getY() * COLUMNS;
			corpsesPositions.put(ind, key);
		}

		for(int x = 0; x < LINES * COLUMNS; x++, j++) {
			if(j == COLUMNS) {
				i++;
				j = 0;
				System.out.println();
			}

			index = j + i * COLUMNS;
			String symbol = "";			

			if(agentsPositions.containsKey(index)) {
				String key = agentsPositions.get(index);
				symbol = " ";

				if(imposters.contains(key)) symbol = "I";
				else symbol = "C";

				System.out.print(colors.get(key) + symbol + RESET);

			} else if(corpsesPositions.containsKey(index)) {
				String key = corpsesPositions.get(index);
				symbol = "B";

				System.out.print(colors.get(key) + symbol + RESET);

			} else if(deadPositions.containsKey(index)) {
				String key = deadPositions.get(index);
				symbol = "G";

				System.out.print(colors.get(key) + symbol + RESET);

			} else if(map[index] == TypeOfPosition.VENT){
				System.out.print("#");

			} else if((map[index] == TypeOfPosition.SHIELDS || map[index] == TypeOfPosition.FILLGAS 
					|| map[index] == TypeOfPosition.CARDSWIPE || map[index] == TypeOfPosition.ASTEROIDS
					|| map[index] == TypeOfPosition.DOWNLOAD || map[index] == TypeOfPosition.UPLOAD
					|| map[index] == TypeOfPosition.TRASH || map[index] == TypeOfPosition.WIRES1 
					|| map[index] == TypeOfPosition.WIRES2 || map[index] == TypeOfPosition.MEDBAY
					|| map[index] == TypeOfPosition.FILLGAS2 ||  map[index] == TypeOfPosition.ONETOTEN
					|| map[index] == TypeOfPosition.SIMONSAYS)){
				System.out.print("T");
			}else if(map[index] == TypeOfPosition.MEETING) {
				System.out.print("o");
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

		System.out.println();
		System.out.println();
	}

	private void createAgents() {		
		Colors[] colors = Colors.values();
		TypeOfPosition[] tasks = TypeOfPosition.values();
		int indexColor = 0;

		AgentContainer c = getContainerController();
		Random random = new Random();

		// CREW
		for(int i = 0; i < numOfCrewmates; i++, indexColor++) {			
			try {
				Object args[] = new Object[3];
				args[0] = tasks[random.nextInt(17 - 7 + 1) + 7];
				args[1] = tasks[random.nextInt(17 - 7 + 1) + 7];
				args[2] = tasks[random.nextInt(17 - 7 + 1) + 7];

				Colors name = colors[indexColor];				
				bb.setPlayerPosition(name.toString(), 11, 5);
				AgentController crew = c.createNewAgent(name.toString(), "Crewmate", args);
				crew.start();
			} catch (StaleProxyException e) {
				System.out.println("Error while creating a Crewmate.");
			}
		}

		// IMPOSTERS
		for(int i = 0; i < numOfImposters; i++, indexColor++) {
			try {
				Object args[] = new Object[3];
				args[0] = tasks[random.nextInt(14 - 7 + 1) + 7];
				args[1] = tasks[random.nextInt(14 - 7 + 1) + 7];
				args[2] = tasks[random.nextInt(14 - 7 + 1) + 7];

				Colors name = colors[indexColor];
				bb.setPlayerPosition(name.toString(), 11, 4);
				bb.setImposters(name.toString());
				AgentController imp = c.createNewAgent(name.toString(), "Imposter", args);
				imp.start();
			} catch (StaleProxyException e) {
				System.out.println("Error while creating an Imposter.");
			}
		}		

		// EMERGENCIES
		try {
			AgentController lights = c.createNewAgent("LIGHTS", "Lights", null);
			lights.start();			
			AgentController reactor = c.createNewAgent("REACTOR", "Reactor", null);
			reactor.start();
			AgentController oxygen = c.createNewAgent("OXYGEN", "Oxygen", null);
			oxygen.start();
		} catch (StaleProxyException e) {
			System.out.println("Error while creating agent emergencies.");
		}

	}

	public class Playing extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			ACLMessage msg = myAgent.receive();

			if(msg != null) {
				String[] message = msg.getContent().split(" ");

				if(message[0].equals("Body") ) { // Body YELLOW
					//System.out.println(msg.getSender().getLocalName()+ " found " + message[1] + "'s body.");	
					
					ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
					sendMsg.setContent("Meeting");
					List<String> players = bb.getAllPlayers();

					ACLMessage foundBodyMessage =  new ACLMessage(ACLMessage.INFORM);
					foundBodyMessage.setContent(msg.getSender().getLocalName() + " found " + message[1] + " body at " + message[2]);
					foundBodyMessage.addReceiver(new AID(getLocalName(),AID.ISLOCALNAME));
					
					for(String player : players) {
						sendMsg.addReceiver(new AID(player,AID.ISLOCALNAME));
						foundBodyMessage.addReceiver(new AID(player,AID.ISLOCALNAME));
					}				
					
					send(sendMsg);
					send(foundBodyMessage);
					
					endValue = 1;
					onMeeting = true;
					System.out.println("-----------------------MEETING-----------------------");

				} else if(message[0].equals("Meeting")) { // Meeting
					System.out.println(msg.getSender().getLocalName() + " called a meeting.");	

					ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
					sendMsg.setContent("Meeting");
					List<String> players = bb.getAllPlayers();
					for(String player : players) {
						sendMsg.addReceiver(new AID(player,AID.ISLOCALNAME));
					}						
					send(sendMsg);
					endValue = 1;
					onMeeting = true;
					System.out.println("-----------------------MEETING-----------------------");

				} else if(message[0].equals("ReactorSabotage") || message[0].equals("LightsSabotage") || message[0].equals("OxygenSabotage")){
					endValue = 2;
					
				} else if(message[0].equals("GameOver")){
					endValue = 3;
					
				}else {

					endValue = 0;
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
			ACLMessage msg = myAgent.receive();
			
			if(msg != null) {
				String message = msg.getContent();
				System.out.println(msg.getSender().getLocalName() + ": " + message);
			}
			
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
			ACLMessage msg = myAgent.receive();

			if(msg != null) {
				String[] message = msg.getContent().split(" ");

				if(message[0].equals("Body") ) { // Body YELLOW
					System.out.println(msg.getSender() + " found " + message[1] + "'s body.");
					ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
					sendMsg.setContent("Meeting");
					List<String> players = bb.getAllPlayers();
					for(String player : players) {
						sendMsg.addReceiver(new AID(player,AID.ISLOCALNAME));
					}						
					send(sendMsg);
					onMeeting = true;
					endValue = 2;
				}else if(message[0].equals("ReactorFixed") || message[0].equals("LightsFixed") || message[0].equals("OxygenFixed")) {
					endValue = 1;
				}else if(message[0].equals("GameOver") ) { // Body YELLOW
					System.out.println("Game over: time to fix emergency run out.");	
					endValue = 3;						

				} else {
					endValue = 0;
				}
			}
		}	

		public int onEnd() {
			return endValue;
		}
	}

	public class Over extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			tbf.getThread(tasks).interrupt();
			tbf.getThread(prints).interrupt();
			tbf.interrupt();
			printMap();
			myAgent.doDelete();
		}	
	}

}
