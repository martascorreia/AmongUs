import Game.Emergency;
import Game.Meeting;
import Game.Over;
import Game.Playing;
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
	private static final String OVER = "Over"; 

	private boolean statePlaying;
	private boolean stateMeeting;
	private boolean stateEmergencyReactor;
	private boolean stateEmergencyLights;
	private boolean stateEmergencyOxygen;
	private boolean stateOver;
	private boolean stateDead;

	protected void setup(){

		statePlaying = true;
		stateMeeting = false;
		stateEmergencyReactor = false;
		stateEmergencyLights = false;
		stateEmergencyOxygen = false;
		stateOver = false;
		stateDead=false;
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

		FSMBehaviour game = new FSMBehaviour(this) {
			private static final long serialVersionUID = 1L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};
		// Registers the states of the Agent
		game.registerFirstState(new Playing(this,1000), PLAYING);
		game.registerState(new Meeting(), MEETING);
		game.registerState(new Emergency(this,1000),EMERGENCY);
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
					statePlaying = false;
					stateEmergencyReactor = true;					
				}else if(msg.equals("ReactorFixed")) {
					statePlaying = true;
					stateEmergencyReactor = false;
				}else if(msg.equals("LightsProblem")) {
					statePlaying = false;
					stateEmergencyLights = true;					
				}else if(msg.equals("LightsFixed")) {
					statePlaying = true;
					stateEmergencyLights = false;
				}else if(msg.equals("OxygenProblem")) {
					statePlaying = false;
					stateEmergencyOxygen = true;					
				}else if(msg.equals("OxygenFixed")) {
					statePlaying = true;
					stateEmergencyOxygen = false;
				}else if(msg.equals("GameOver")) {
					stateOver = true;
					statePlaying = false;
					stateMeeting = false;
					stateEmergencyReactor = false;
					stateEmergencyLights = false;
					stateEmergencyOxygen = false;
				}else if(msg.equals("StartMeeting")) {
					stateMeeting=true;
					statePlaying = false;
					stateEmergencyReactor = false;
					stateEmergencyLights = false;
					stateEmergencyOxygen = false;
				}else if(msg.equals("EndMeeting")) {
					statePlaying = true;
					stateMeeting= false;
				}else if(msg.equals("YouAreDead")) {
					stateDead= true;
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
				endValue = 0;
			}else if(stateMeeting) {
				endValue = 1;
			}else if(stateOver) {
				endValue = 3;
			}else {
				endValue = 2;
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


}
