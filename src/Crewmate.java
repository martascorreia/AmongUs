import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Crewmate extends Agent {

	private static final long serialVersionUID = 1L;
	private final Blackboard bb = Blackboard.getInstance();

	// States
	private final String PLAYING = "Playing";
	private final String MEETING = "Meeting";
	private final String EMERGENCY = "Emergency";
	private final String DOINGTASK = "DoingTask";
	private static final String OVER = "Over"; 

	private Map<String, Position> tasks;
	private Map<String, Boolean> states; 
	private int doingTaskCounter = 3;

	private String lastDead;
	private String deadPlace;
	//private List<String> info;
	private Map<Date,String> info;

	//I saw Green at Oxygen // 5 palavras 
	//I saw Green with Orange at Oxygen // 6 palavrs
	//Im on Oxygen and i saw Green now // 8 palavras
	//Green did a task  //4 

	// Behaviours
	Interaction interaction;
	FSMBehaviour game;
	ThreadedBehaviourFactory tbf;

	protected void setup(){
		states = new HashMap<>();
		states.put("playing", true);
		states.put("meeting", false);
		states.put("reactor", false);
		states.put("lights", false);
		states.put("oxygen", false);
		states.put("over", false);
		states.put("dead", false);
		states.put("task", false);
		info = new HashMap<>();
		tasks = new HashMap<>();

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

		// TASKS
		Object[] args= getArguments();
		for(Object p : args) {
			tasks.put(p.toString(), bb.getTaskPosition(p.toString()));
		}

		// FSM
		game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};

		// Registers the states of the Agent
		game.registerFirstState(new Playing(), PLAYING);
		game.registerState(new DoingTask(),DOINGTASK);
		game.registerState(new Meeting(), MEETING);
		game.registerState(new Emergency(),EMERGENCY);
		game.registerLastState(new Over(), OVER);

		// Registers the transitions	
		// PLAYING
		game.registerTransition(PLAYING, PLAYING, 0);
		game.registerTransition(PLAYING, MEETING, 1);	
		game.registerTransition(PLAYING, EMERGENCY, 2);
		game.registerTransition(PLAYING, DOINGTASK,3);
		game.registerTransition(PLAYING, OVER, 4);	

		// DOING TASK
		game.registerTransition(DOINGTASK, DOINGTASK,0);
		game.registerTransition(DOINGTASK, PLAYING, 1);
		game.registerTransition(DOINGTASK, MEETING,2);
		game.registerTransition(DOINGTASK, EMERGENCY,3);
		game.registerTransition(DOINGTASK, OVER,4);

		// MEETING
		game.registerTransition(MEETING, MEETING, 0);
		game.registerTransition(MEETING, PLAYING, 1);
		game.registerTransition(MEETING, OVER, 2);

		// EMERGENCY
		game.registerTransition(EMERGENCY, EMERGENCY, 0);
		game.registerTransition(EMERGENCY, PLAYING, 1);
		game.registerTransition(EMERGENCY, MEETING, 2);
		game.registerTransition(EMERGENCY, OVER, 3);


		tbf = new ThreadedBehaviourFactory();
		addBehaviour(tbf.wrap(game));
		interaction = new Interaction();
		addBehaviour(tbf.wrap(interaction));
	}


	public class Interaction extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage rec = null;
			if(!states.get("meeting")) {
				rec = receive();

				if(rec != null) {				
					String msg = rec.getContent();

					// Reactor
					if(msg.equals("ReactorProblem")) {
						states.replace("reactor", true);
						states.replace("task", false);
						states.replace("playing", false);
					}else if(msg.equals("ReactorFixed")) {
						states.replace("playing", true);
						states.replace("reactor", false);

						// Lights
					}else if(msg.equals("LightsProblem")) {
						states.replace("lights", true);
						states.replace("task", false);
						states.replace("playing", false);									
					}else if(msg.equals("LightsFixed")) {
						states.replace("playing", true);
						states.replace("lights", false);

						// Oxygen
					}else if(msg.equals("OxygenProblem")) {
						states.replace("oxygen", true);
						states.replace("task", false);
						states.replace("playing", false);	
					}else if(msg.equals("OxygenFixed")) {
						states.replace("playing", true);
						states.replace("oxygen", false);

						// Game Over
					}else if(msg.equals("GameOver")) {		
						states.replace("over", true);
						states.replace("playing", false);
						states.replace("task", false);
						states.replace("meeting", false);
						states.replace("reactor", false);
						states.replace("lights", false);
						states.replace("oxygen", false);

						// Meeting
					}else if(msg.equals("Meeting")) {					
						states.replace("meeting", true);
						states.replace("playing", false);
						states.replace("task", false);
						states.replace("reactor", false);
						states.replace("lights", false);
						states.replace("oxygen", false);
					}else if(msg.equals("EndMeeting")) {
						states.replace("playing", true);
						states.replace("meeting", false);

						// Dead
					}else if(msg.equals("YouAreDead")) {
						states.replace("dead", true);
						synchronized (bb) {
							Position pos = bb.getAlivePlayers().get(getLocalName());
							bb.setPlayerAsCorpse(getLocalName(), pos.getX(), pos.getY());
							bb.setPlayerAsDead(getLocalName(), pos.getX(), pos.getY());
						}
					}
				}
			}
		}

	}

	public class Playing extends OneShotBehaviour {

		private static final long serialVersionUID = 1L;
		private int endValue;

		public void action() {     

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			if(states.get("playing")) {
				
				// INFO
				Map<String,Position> vision = DistanceUtils.getPlayersNear(getLocalName(),bb.getCrewmateVision(),bb.getAlivePlayers());
				if(!vision.isEmpty()) {
					String[] players = vision.keySet().toArray(new String[vision.keySet().size()]);
					for(String player : players) {
						Position value = vision.get(player);
						String task = bb.getLocal(value);
						String information = "I was with " + player + " near " + task ;
						if(bb.isTask(value)) 
							information += " and " + player + " was on that task";
						
						info.put(Calendar.getInstance().getTime(), information);
					}
				}

				// MOVEMENT
				Position myPosition = bb.getPlayerPosition(getLocalName());
				String dead = DistanceUtils.reportCorpse(getLocalName());
				if(dead != null && !states.get("dead")) {
					String local = bb.getLocal(bb.getCorpsesPlayers().get(dead));
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("Body " + dead + " " + local);
					msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
					send(msg);
					
				}else if(!tasks.isEmpty()) {		
					String closestTask = DistanceUtils.closestTask(myPosition, tasks);	
					if(DistanceUtils.manDistance(myPosition, tasks.get(closestTask)) == 0) {
						doingTaskCounter = 3;
						endValue = 3;
						states.replace("task", true);
						states.replace("playing", false);

					}else {
						endValue = 0;
						myPosition = DistanceUtils.nextMove(myPosition, tasks.get(closestTask));
					}

				}else {
					endValue = 0;
					myPosition = DistanceUtils.randomMove(myPosition);
				}
				synchronized (bb) {
					bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
				}
			}else if(states.get("meeting")) {
				endValue = 1;

			}else if(states.get("over")) {
				endValue = 4;

			}else if(states.get("task")){
				endValue = 3;

			}else {
				endValue = 2;
			}

		}	

		public int onEnd() {
			return endValue;
		}
	}

	public class DoingTask extends OneShotBehaviour{

		private static final long serialVersionUID = 1L;
		private int endValue;

		public void action() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			if(states.get("task")) {
				doingTaskCounter -= 1;

				if(doingTaskCounter == 0) {
					states.replace("task", false);
					states.replace("playing", true);					
					String task = DistanceUtils.closestTask(bb.getPlayerPosition(getLocalName()), tasks);
					tasks.remove(task);
					synchronized(bb) {
						bb.incrementTaskDone();
					}
					endValue = 1;
				} else {
					endValue = 0;
				}

			}else if(states.get("meeting")) {
				endValue = 2;

			}else if(states.get("over")) {
				endValue = 4;

			}else {
				endValue = 3;
			}
		}

		public int onEnd() {
			return endValue;
		}		
	}


	public class Meeting extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			ACLMessage rec = null;
			if(states.get("meeting")) {
			
				rec = receive();

				if(rec != null) {				
					String msg = rec.getContent();

					
					// THIS IS THE SAME AS ABOVE BUT MEETING IS TRUE
					// Reactor
					if(msg.equals("ReactorProblem")) {
						
					}else if(msg.equals("ReactorFixed")) {
						// Lights
					}else if(msg.equals("LightsProblem")) {									
					}else if(msg.equals("LightsFixed")) {
						// Oxygen
					}else if(msg.equals("OxygenProblem")) {
					}else if(msg.equals("OxygenFixed")) {

						// Game Over
					}else if(msg.equals("GameOver")) {		
						states.replace("over", true);
						states.replace("playing", false);
						states.replace("task", false);
						states.replace("meeting", false);
						states.replace("reactor", false);
						states.replace("lights", false);
						states.replace("oxygen", false);

						// Meeting					
					}else if(msg.equals("EndMeeting")) {
						states.replace("playing", true);
						states.replace("meeting", false);

						// Dead
					}else if(msg.equals("YouAreDead")) {
						states.replace("dead", true);
						synchronized (bb) {
							Position pos = bb.getAlivePlayers().get(getLocalName());
							bb.setPlayerAsCorpse(getLocalName(), pos.getX(), pos.getY());
							bb.setPlayerAsDead(getLocalName(), pos.getX(), pos.getY());
						}
					}else {
						//MENSAGENS DA REUNIAO
						String[] message = msg.split(" ");
						System.out.println(msg);

						if(message.length == 6 && message[1].equals("found")){
							lastDead = message[2];
							deadPlace= message[5];
							
							System.out.println(lastDead + " " + deadPlace);
						}
					}
				}
				
				endValue = 0;
			} else if(states.get("playing")) {		
				endValue = 1;
				
			} else {
				endValue = 2;
			}		
		}	

		public int onEnd() {
			return endValue;
		}
	}

	public class Emergency extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;


		public void action() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			String dead = DistanceUtils.reportCorpse(getLocalName());
			if(states.get("dead")) {
				endValue = 1;

				Position myPosition = bb.getPlayerPosition(getLocalName());
				synchronized(bb){
					bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
				}
			}else if (dead != null) {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				String local = bb.getLocal(bb.getCorpsesPlayers().get(dead));
				msg.setContent("Body " + dead+ " " + local);
				msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
				send(msg);
			} else if(states.get("reactor")) {
				Position myPosition = bb.getPlayerPosition(getLocalName());
				Position emergency = bb.getEmergencyPosition("REACTOR");

				if(DistanceUtils.manDistance(myPosition, emergency) == 0) {
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("ReactorFix");
					msg.addReceiver(new AID("REACTOR", AID.ISLOCALNAME));
					send(msg);

				}else {
					endValue = 0;
					myPosition = DistanceUtils.nextMove(myPosition, emergency);
				}
				synchronized(bb) {
					bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
				}
				endValue = 0;
			} else if(states.get("lights")) {
				Position myPosition = bb.getPlayerPosition(getLocalName());
				Position emergency = bb.getEmergencyPosition("LIGHTS");

				if(DistanceUtils.manDistance(myPosition, emergency) == 0) {
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("LightsFix");
					msg.addReceiver(new AID("LIGHTS", AID.ISLOCALNAME));
					send(msg);

				}else {
					endValue = 0;
					myPosition = DistanceUtils.nextMove(myPosition, emergency);
				}
				synchronized(bb) {
					bb.setPlayerPosition(getLocalName(),myPosition.getX(), myPosition.getY());
				}
				endValue = 0;
			} else if(states.get("oxygen")){
				Position myPosition = bb.getPlayerPosition(getLocalName());
				Position emergency = bb.getEmergencyPosition("O2");

				if(DistanceUtils.manDistance(myPosition, emergency) == 0) {
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setContent("O2Fix");
					msg.addReceiver(new AID("O2", AID.ISLOCALNAME));
					send(msg);

				}else {
					endValue = 0;
					myPosition = DistanceUtils.nextMove(myPosition, emergency);
				}
				synchronized(bb) {
					bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
				}
				endValue = 0;		

			}else if(states.get("over")) {
				endValue = 3;

			}else if(states.get("playing")){
				endValue = 1;

			}else {
				endValue = 2;
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
			tbf.getThread(interaction).interrupt();
			tbf.getThread(game).interrupt();
			tbf.interrupt();
		}	 
	}	
}
