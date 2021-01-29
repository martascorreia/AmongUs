import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ThreadedBehaviourFactory;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class Reactor extends Agent{
	
	//REACTOR
	private static final long serialVersionUID = 1L;
	
	private int timer = 0;
	private boolean sabotage = false;
	private Blackboard bb = Blackboard.getInstance();
	
	// Behaviours
	ThreadedBehaviourFactory tbf;
	TickerBehaviour reactorTime;
	CyclicBehaviour interaction;
	
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
	
		reactorTime = new TickerBehaviour(this,1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {
				if(sabotage) {
					timer--;
					
					if(timer == 0) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("GameOver");
						List<String> players =  bb.getAllPlayers();
						
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						
						msg.addReceiver(new AID("OXYGEN",AID.ISLOCALNAME));
						msg.addReceiver(new AID("LIGHTS",AID.ISLOCALNAME));
						msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
						
						send(msg);
					}
				}
			}
		};
		
		interaction = new CyclicBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage rec = null;
				rec= receive();
				if(rec != null){
					String message = rec.getContent();
					
					if(message.equals("ReactorSabotage")) {
						System.out.println("-----------------------REACTOR SABOTAGE-----------------------");
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("ReactorProblem");
						List<String> players = bb.getAllAlivePlayers();
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
						if(!bb.isMeeting()) {
							send(msg);
							timer = 40;
							sabotage = true;
						} else {
							sabotage = false;
						}
									
					} else if(message.equals("ReactorFix")) {
						System.out.println("-----------------------REACTOR FIXED-----------------------");

						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("ReactorFixed");
						List<String> players = bb.getAllAlivePlayers();
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
						if(!bb.isMeeting())
							send(msg);
						sabotage = false;
						bb.setEmergencyCalling(false);

					} else if(message.contentEquals("GameOver")) {
						stopBehaviours();
					}
				}	
			}			
		};
		
		tbf = new ThreadedBehaviourFactory();
		addBehaviour(tbf.wrap(reactorTime));
		addBehaviour(tbf.wrap(interaction));
		
	}
	
	private void stopBehaviours() {
		tbf.getThread(reactorTime).interrupt();
		tbf.getThread(interaction).interrupt();
		tbf.interrupt();
	}
}
