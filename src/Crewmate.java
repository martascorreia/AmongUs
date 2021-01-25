
import java.util.List;

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

public class Crewmate extends Agent {

	private static final long serialVersionUID = 1L;
	private final Blackboard bb = Blackboard.getInstance();

	// States
	private final String PLAYING = "Playing";
	private final String MEETING = "Meeting";
	private final String EMERGENCY = "Emergency";
	private final String DOINGTASK = "DoingTask";
	private static final String OVER = "Over"; 

	private List<Position> myTasks;
	private boolean statePlaying;
	private boolean stateMeeting;
	private boolean stateEmergencyReactor;
	private boolean stateEmergencyLights;
	private boolean stateEmergencyOxygen;
	private boolean stateOver;
	private boolean stateDead;
	private boolean stateTask;
	private int doingTaskCounter = 3;
	
	// I Saw collor at x,y with collor
	protected void setup(){

		statePlaying = true;
		stateMeeting = false;
		stateEmergencyReactor = false;
		stateEmergencyLights = false;
		stateEmergencyOxygen = false;
		stateOver = false;
		stateDead=false;
		stateTask=false;
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
		
		Object[] args= getArguments();
		for(Object p : args) {
			myTasks.add(bb.getPosition((String) p));
		}
		FSMBehaviour game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};
		// Registers the states of the Agent
		game.registerFirstState(new Playing(this,1000), PLAYING);
		game.registerState(new DoingTask(this,1000),DOINGTASK);
		game.registerState(new Meeting(), MEETING);
		game.registerState(new Emergency(this,1000),EMERGENCY);
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
		game.registerTransition(DOINGTASK, PLAYING,1);
		game.registerTransition(DOINGTASK, MEETING,2);
		game.registerTransition(DOINGTASK, EMERGENCY,3);
		game.registerTransition(DOINGTASK, OVER,4);
		// MEETING
		game.registerTransition(MEETING, PLAYING, 0);
		game.registerTransition(MEETING, OVER, 1);

		// EMERGENCY
		game.registerTransition(EMERGENCY, EMERGENCY, 0);
		game.registerTransition(EMERGENCY, PLAYING, 1);
		game.registerTransition(EMERGENCY, MEETING, 2);
		game.registerTransition(EMERGENCY, OVER, 3);


		ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
		addBehaviour(tbf.wrap(game));
		addBehaviour(tbf.wrap(new Interaction()));
	}


	public class Interaction extends CyclicBehaviour{

		@Override
		public void action() {
			ACLMessage rec = null;
			rec=receive();
			if(rec!=null) {
				
				String msg = rec.getContent();
				if(msg.equals("ReactorProblem")) {
					stateEmergencyReactor = true;	
					stateTask=false;
					statePlaying = false;
				}else if(msg.equals("ReactorFixed")) {
					statePlaying = true;
					stateEmergencyReactor = false;
				}else if(msg.equals("LightsProblem")) {
					stateEmergencyLights = true;	
					stateTask=false;
					statePlaying = false;
									
				}else if(msg.equals("LightsFixed")) {
					statePlaying = true;
					stateEmergencyLights = false;
				}else if(msg.equals("OxygenProblem")) {
					stateEmergencyOxygen = true;	
					stateTask=false;
					statePlaying = false;		
				}else if(msg.equals("OxygenFixed")) {
					statePlaying = true;
					stateEmergencyOxygen = false;
				}else if(msg.equals("GameOver")) {
					stateOver = true;
					stateTask=false;
					statePlaying = false;
					stateMeeting = false;
					stateEmergencyReactor = false;
					stateEmergencyLights = false;
					stateEmergencyOxygen = false;
				}else if(msg.equals("StartMeeting")) {
					stateMeeting=true;
					statePlaying = false;
					stateTask=false;
					stateEmergencyReactor = false;
					stateEmergencyLights = false;
					stateEmergencyOxygen = false;
				}else if(msg.equals("EndMeeting")) {
					statePlaying = true;
					stateMeeting= false;
				}else if(msg.equals("YouAreDead")) {
					stateDead=true;
				}
			}
		}

	}
	public class Playing extends TickerBehaviour {

		private static final long serialVersionUID = 1L;
		private int endValue;

		public Playing(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onTick() {
			if(statePlaying) {
				Position myPosition = bb.getPosition(getLocalName());
				
				//Getting info
				
				
				//MOVEMENT
				if(!myTasks.isEmpty()) {
					Position closestTask = DistanceUtils.closestTask(myPosition, myTasks);
					if(DistanceUtils.manDistance(myPosition, closestTask) == 0) {
						doingTaskCounter=3;
						endValue=3;
					}else {
						myPosition = DistanceUtils.closestMove(myPosition, closestTask);
					}
				}else {
					myPosition = DistanceUtils.randomMove(myPosition);
				}
				bb.setPosition(getLocalName(), myPosition.getX(), myPosition.getY());
				endValue = 0;
			}else if(stateMeeting) {
				endValue = 1;
			}else if(stateOver) {
				endValue = 4;
			}else if(stateTask){
				endValue = 3;
			}else {
				endValue = 2;
			}
		}	

		public int onEnd() {
			return endValue;
		}
	}
	
	public class DoingTask extends TickerBehaviour{
		
		private static final long serialVersionUID = 1L;
		private int endValue;
		
		public DoingTask(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onTick() {
			
			if(stateTask) {
				doingTaskCounter -= 1;
				if(doingTaskCounter == 0) {
					stateTask=false;
					statePlaying=true;
					Position doingTask = DistanceUtils.closestTask(bb.getPosition(getLocalName()),myTasks);
					myTasks.remove(myTasks.indexOf(doingTask));
					endValue=1;
				}else {
					endValue = 0;
				}
				
			}else if(stateMeeting) {
				endValue = 2;
			}else if(stateOver) {
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
			if(!stateDead) {
				
			}
		}	

		public int onEnd() {
			return endValue;
		}
	}

	public class Emergency extends TickerBehaviour {
		private static final long serialVersionUID = 1L;
		private int endValue;

		public Emergency(Agent a, long period) {
			super(a, period);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onTick() {
			if(stateDead) {
				endValue = 1;
			}else if(stateEmergencyReactor) {
				endValue = 0;
			}else if(stateEmergencyLights) {
				endValue = 0;
			}else if(stateEmergencyOxygen){
				endValue = 0;
			}else if(stateOver) {
				endValue = 3;
			}else if(statePlaying){
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
			System.out.println("Agent " +getLocalName()+ "stopped");
		}	 
	}


	
	// METODOS AUXILIARES
	
}
