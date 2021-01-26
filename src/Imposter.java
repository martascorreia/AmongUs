import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class Imposter extends Agent {

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
	private int killCooldownCounter = 40;
	private int sabotageCooldownCounter = 10;
		
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
		
		// behaviours 
		// ticker behaviour - kill cooldown
		// ticker behaviour - emergency cooldown
		// fsm behaviour - playing
		
		// TASKS
		Object[] args= getArguments();
		for(Object p : args) {
			tasks.put(p.toString(), bb.getTaskPosition(p.toString()));
		}
		
		TickerBehaviour killCooldown = new TickerBehaviour(this, 1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {
				if(killCooldownCounter != 0)
					killCooldownCounter--;
			}
			
		};
		
		TickerBehaviour sabotageCooldown = new TickerBehaviour(this, 1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {
				if(sabotageCooldownCounter != 0)
					sabotageCooldownCounter--;
			}
			
		};
		
		// FSM
		FSMBehaviour game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};
		
		// Registers the states of the Agent
		game.registerFirstState(new Playing(), PLAYING);
		game.registerState(new DoingTask(), DOINGTASK);
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


		ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
		addBehaviour(tbf.wrap(game));
		addBehaviour(tbf.wrap(killCooldown));
		addBehaviour(tbf.wrap(sabotageCooldown));
		addBehaviour(tbf.wrap(new Interaction()));
	}
	
	public class Interaction extends CyclicBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage rec = null;
			rec = receive();
			
			if(rec != null) {				
				String msg = rec.getContent();
				
				// REACTOR
				if(msg.equals("ReactorProblem")) {
					states.replace("reactor", true);
					states.replace("task", false);
					states.replace("playing", false);
				}else if(msg.equals("ReactorFixed")) {
					states.replace("playing", true);
					states.replace("reactor", false);
					
				// LIGHTS
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
				}else if(msg.equals("StartMeeting")) {					
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
				Position myPosition = bb.getPlayerPosition(getLocalName());				
				
				Map<String, Position> imposterVision = DistanceUtils.getPlayersNearImp(getLocalName(), bb.getImposterVision(), bb.getAlivePlayersPositions());
				Map<String, Position> crewmateVision = DistanceUtils.getPlayersNearImp(getLocalName(), bb.getCrewmateVision(), imposterVision);
				Map<String, Position> killable = DistanceUtils.getPlayersNearImp(getLocalName(), bb.getDistanceKill(), imposterVision);
				
				if(killable.size() == 1 && killCooldownCounter == 0) {
					String name = killable.keySet().toArray(new String[killable.keySet().size()])[0];
					Map<String, Position> crewmatesSee = DistanceUtils.getPlayersNearImp(name, bb.getCrewmateVision(), imposterVision);
					
					if(crewmatesSee.isEmpty()) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("YouAreDead");
						msg.addReceiver(new AID(name,AID.ISLOCALNAME));
						send(msg);
						myPosition = killable.get(name);
						
						// CALL EMERGENCY
						if(!bb.getEmergencyCalling()){
							if(myPosition.getX() + myPosition.getY() * bb.getCollums() > 15) {
								callReactor();
							} else {
								callOxygen();
							}
						}
					}
					
				} else if(killable.size() > 1 && callLigths() && killCooldownCounter == 0) {
					bb.setCrewmateVision(1);
					
					// could also verify every single killable
					String name = killable.keySet().toArray(new String[killable.keySet().size()])[0];
					Map<String, Position> crewmatesSee = DistanceUtils.getPlayersNearImp(name, bb.getCrewmateVision(), imposterVision);
					
					if(crewmatesSee.isEmpty()) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("YouAreDead");
						msg.addReceiver(new AID(name,AID.ISLOCALNAME));
						send(msg);
						myPosition = killable.get(name);
					}
					
				}	else if(!imposterVision.isEmpty() && crewmateVision.isEmpty() && killCooldownCounter == 0) {
					// could also verify every single killable
					String name = imposterVision.keySet().toArray(new String[killable.keySet().size()])[0];					
					myPosition = DistanceUtils.nextMove(myPosition, bb.getPlayerPosition(name));
					
				}
				// MOVEMENT
				else if(!tasks.isEmpty()) {					
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
				
				bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
				
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

		

		private boolean callReactor() {
			if(bb.getEmergencyCalling()) return false;
			bb.setEmergencyCalling(true);
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("ReactorSabotage");				
			msg.addReceiver(new AID("REACTOR",AID.ISLOCALNAME));				
			send(msg);			
			return true;
		}
		
		private boolean callLigths() {
			if(bb.getEmergencyCalling()) return false;
			bb.setEmergencyCalling(true);
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("LigthsSabotage");				
			msg.addReceiver(new AID("LIGHTS",AID.ISLOCALNAME));				
			send(msg);			
			return true;
		}
		
		private boolean callOxygen() {		
			if(bb.getEmergencyCalling()) return false;
			bb.setEmergencyCalling(true);
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setContent("OxygenSabotage");				
			msg.addReceiver(new AID("OXYGEN",AID.ISLOCALNAME));				
			send(msg);
			return true;
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
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			 
			if(states.get("dead")) {
				endValue = 1;
				
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
				
				bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
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
				
				bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
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
				
				bb.setPlayerPosition(getLocalName(), myPosition.getX(), myPosition.getY());
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

	public class Emergency extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;


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
			System.out.println("Agent "+ getLocalName()+ "stopped");
		}	 
	}	

}
