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
	private boolean sabotagem = false;
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
	
		TickerBehaviour reactorTime = new TickerBehaviour(this,1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {
				if(sabotagem) {
					timer--;
					
					if(timer == 0) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("GameOver");
						List<String> players = Blackboard.getInstance().getAllPlayers();
						
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						
						msg.addReceiver(new AID("OXYGEN",AID.ISLOCALNAME));
						msg.addReceiver(new AID("LIGHTS",AID.ISLOCALNAME));
						
						send(msg);
					}
				}
			}
		};
		
		CyclicBehaviour interaction = new CyclicBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage rec = null;
				rec= receive();
				if(rec != null){
					String message = rec.getContent();
					
					if(message.equals("ReactorSabotage")) {
						System.out.println("REACTOR SABOTAGE");
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("ReactorProblem");
						List<String> players = Blackboard.getInstance().getAllPlayers();
						
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						
						send(msg);
					    //addBehaviour(tbf.wrap(reactorTime)); TODO TESTAR ISTO DPS
						timer = 40;
						sabotagem = true;
						
					}else if(message.equals("ReactorFix")) {
						System.out.println("REACTOR FIXED");

						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("ReactorFixed");
						List<String> players = Blackboard.getInstance().getAllPlayers();
						
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						
						send(msg);
						sabotagem = false;
					} else if(message.contentEquals("GameOver")) {
						// stop behaviour
					}
				}	
			}
		};
		
		ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
		addBehaviour(tbf.wrap(reactorTime));
		addBehaviour(tbf.wrap(interaction));
	}
}
