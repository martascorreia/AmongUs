import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private Map<String, Long> info;
	private Map<String, Integer> suspicion;

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
		suspicion = new HashMap<>();

		synchronized(bb) {
			Map<String, Position> players = bb.getAlivePlayers();
			Iterator<String> iter = players.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				suspicion.put(key, 0);
			}
			suspicion.remove(getLocalName());
		}	
		

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
			if(!states.get("meeting")) {
				ACLMessage rec = receive();

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
				if(!states.get("dead")) {
					Map<String,Position> vision = DistanceUtils.getPlayersNear(getLocalName(),bb.getCrewmateVision(),bb.getAlivePlayers());
					if(vision.isEmpty()) {
						Position value = bb.getPlayerPosition(getLocalName());
						String task = bb.getLocal(value);
						info.put("I was alone near " + task, System.currentTimeMillis());
						
					} else {
						String[] players = vision.keySet().toArray(new String[vision.keySet().size()]);								
							for(String player : players) {
								Position value = vision.get(player);
								String task = bb.getLocal(value);
								String information = "I was with " + player + " near " + task ;
								if(bb.isTask(value)) 
									information += " and " + player + " was on that task";
	
								info.put(information, System.currentTimeMillis());
							}
					
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


	//I saw Green at Oxygen // 5 palavras 
	//I saw Green with Orange at Oxygen // 6 palavrs TODO
	//Im on Oxygen and i saw Green now // 8 palavras

	public class Meeting extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		@Override
		public void action() {
			if(states.get("meeting")) {
				ACLMessage rec = receive();
				
				endValue = 0;

				if(rec != null) {				
					String msg = rec.getContent();					
					// Reactor
					if(msg.equals("ReactorProblem")) {
						ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
						sendMsg.setContent("ReactorFixed");
						sendMsg.addReceiver(new AID("REACTOR", AID.ISLOCALNAME));
						send(sendMsg);

					}else if(msg.equals("LightsProblem")) {	
						ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
						sendMsg.setContent("LightsFixed");
						sendMsg.addReceiver(new AID("LIGHTS", AID.ISLOCALNAME));
						send(sendMsg);

					}else if(msg.equals("OxygenProblem")) {
						ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
						sendMsg.setContent("OxygenFixed");
						sendMsg.addReceiver(new AID("OXYGEN", AID.ISLOCALNAME));
						send(sendMsg);

					}else if(msg.equals("ReactorFixed") || msg.equals("LightsFixed") || msg.equals("OxygenFixed")) {
						// do nothing

					}else if(msg.equals("GameOver")) {		
						states.replace("over", true);
						states.replace("meeting", false);
						endValue = 2;

					} else if(msg.equals("EndMeeting")) {
						states.replace("playing", true);
						states.replace("meeting", false);
						endValue = 1;

					}else if(msg.equals("YouAreDead")) {
						states.replace("dead", true);
						synchronized (bb) {
							Position pos = bb.getAlivePlayers().get(getLocalName());
							bb.setPlayerAsCorpse(getLocalName(), pos.getX(), pos.getY());
							bb.setPlayerAsDead(getLocalName(), pos.getX(), pos.getY());
						}
						states.replace("playing", false);
						states.replace("meeting", true);
						
					} else if(states.get("dead")) {

					} else {
						// MEETING
						String[] message = msg.split(" ");
						String reporter = message[0];
						String lastDead = message[2];
						String deadPlace= message[5];
						Map<String, Position> dead = bb.getDeadPlayers();
						Iterator<String> iter = dead.keySet().iterator();
						while(iter.hasNext()) 
							suspicion.remove(iter.next());
						
						// MY INFO
						StringBuilder sb = new StringBuilder();

						// RECENT MESSAGES
						Iterator<String> keyIter2 = info.keySet().iterator();
						String mostRecent = "";
						if(keyIter2.hasNext()) {
							mostRecent = keyIter2.next();
							while (keyIter2.hasNext()) {
								String key = keyIter2.next();
								if(info.get(key) <= info.get(mostRecent)) {
									mostRecent = key;
								}
							}
							
							String[] split = mostRecent.split(" ");
							sb.append("I am with " + split[3] + " near " + split[5]);
							sb.append(" \n ");
							info.remove(mostRecent);
						}
						
						//TIME + "I was with player near task" //6
						//TIME + "I was with player near task and player was on that task"; // 12
						// I was alone near task //5

						// Remove messages not on the scene of the crime
						List<String> myInfo = new ArrayList<String>();
						Iterator<String> keyIter3 = info.keySet().iterator();
						while (keyIter3.hasNext()) {
							String key = keyIter3.next();
							String[] split = key.split(" "); 
							String local;
							if(split.length == 5)
								local = split[4];
							else local = split[5];
							if(local.equals(deadPlace)) {
								myInfo.add(key);
								sb.append(key);
								sb.append(" \n ");
							}
						}

						// another list with the people that were with dead players					
						
						// SEND INFO
						ACLMessage sendMsg = new ACLMessage(ACLMessage.INFORM);
						if(sb.length() == 0)
							sendMsg.setContent("I have no info");
						else 
							sendMsg.setContent(sb.toString());
						List<String> players = bb.getAllPlayers();
						for(String player : players) {
							sendMsg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}			
						sendMsg.removeReceiver(new AID(getLocalName(), AID.ISLOCALNAME));
						sendMsg.addReceiver(new AID("Game", AID.ISLOCALNAME));
						send(sendMsg);

						// RECEIVE INFO
						Map<String, String> othersInfo = new HashMap<>();
						List<String> currentPosition = new ArrayList<String>();
						for(int i = 0; i < bb.getAlivePlayers().size() - 1; i++) {
							ACLMessage msg2 = myAgent.receive();
							if(msg2 != null) {
								String rcvMsg = msg2.getContent();
								String[] informations = rcvMsg.split(" \n ");
								currentPosition.add(informations[0]);
								for(int j = 1; j < informations.length; j++) {
									othersInfo.put(msg2.getSender().getLocalName(), informations[j]);
								}
							} else i--;
						}	
						
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
						}
						
						// ANALYSE AND MAKE DECISION
						String[] split = mostRecent.split(" ");
						if(split.length > 0) {
							if(!split[2].contentEquals("alone")) {
								String otherPlayer = split[3];
								String local = split[5];
								boolean playerDoingTask = false;
								playerDoingTask = (split.length > 6);
	
								if(bb.getAlivePlayers().containsKey(otherPlayer)){
									if(local.equals(deadPlace) )
									suspicion.replace(otherPlayer, suspicion.get(otherPlayer) + 10);
	
									if(playerDoingTask)
									suspicion.replace(otherPlayer, suspicion.get(otherPlayer) - 5);
								}
							}
						}


						for(String info : myInfo) {
							split = info.split(" ");
							if(!split[2].contentEquals("alone")) {
								String otherPlayer = split[3];
								String local = split[5];
								boolean playerDoingTask = false;
								playerDoingTask = (split.length > 6);
								
								if(bb.getAlivePlayers().containsKey(otherPlayer) && !getLocalName().contentEquals(otherPlayer) ){
									//if(local.equals(deadPlace))
										suspicion.replace(otherPlayer, suspicion.get(otherPlayer) + 10);
	
									if(playerDoingTask) 
										suspicion.replace(otherPlayer, suspicion.get(otherPlayer) - 5);
								}
							}
						}

						Iterator<String> iter2 = othersInfo.keySet().iterator();
						while(iter2.hasNext()) {
							String player = iter2.next();
							String info = othersInfo.get(player);
							split = info.split(" ");

							if(!split[2].contentEquals("alone")) {
								String otherPlayer = split[3];
								String local = split[5];
								boolean playerDoingTask = false;
								playerDoingTask = (split.length > 6);

								if(!bb.getAlivePlayers().containsKey(otherPlayer)) {
									//if(local.equals(deadPlace))
										suspicion.replace(player, suspicion.get(player) + 20);	
									
									//else 
										//suspicion.replace(player, suspicion.get(player) + 15);
									
								} else {
									if(playerDoingTask && !getLocalName().contentEquals(otherPlayer))
										suspicion.replace(otherPlayer, suspicion.get(otherPlayer) - 5);

									//if(local.equals(deadPlace)) {
									//suspicion.replace(otherPlayer, suspicion.get(otherPlayer) + 10);	
									suspicion.replace(player, suspicion.get(player) + 10);		
									//}
								}

							} else {
								String local = split[4];
								//if(local.equals(deadPlace))
									suspicion.replace(player, suspicion.get(player) + 15);

								if(DistanceUtils.manDistance(bb.getPosition(local), bb.getPosition(deadPlace)) < 10)
									suspicion.replace(player, suspicion.get(player) + 10);				
							}
							
							if(player.equals(reporter))
								suspicion.replace(player, 0);
						}

						// VOTE
						Iterator<String> iter3 = suspicion.keySet().iterator();
						String mostSus = iter3.next();
						while(iter3.hasNext()) {
							String sus = iter3.next();
							int level = suspicion.get(sus);
							if(level > suspicion.get(mostSus)) {
								mostSus = sus;
							}							
						}
						
						// add suspicion after votes
						
						ACLMessage voting = new ACLMessage(ACLMessage.INFORM);
						voting.setContent(mostSus);
						voting.addReceiver(new AID("Game", AID.ISLOCALNAME));
						send(voting);
						info.clear();
						endValue = 0;
					}
				}

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
				msg.setContent("Body " + dead + " " + local);
				msg.addReceiver(new AID("Game", AID.ISLOCALNAME));
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
