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

public class Lights extends Agent {
	private int timer = 40;
	private boolean sabotagem = false;
	private Blackboard bb = Blackboard.getInstance();
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
		ThreadedBehaviourFactory tbf = new ThreadedBehaviourFactory();
		TickerBehaviour lightsTime = new TickerBehaviour(this,1000) {

			@Override
			protected void onTick() {
				if(sabotagem) {
					timer-=1;
				}
			}
		};
		
		CyclicBehaviour interaction = new CyclicBehaviour() {

			@Override
			public void action() {
				ACLMessage rec = null;
				rec= receive();
				if(rec != null){
					if(rec.getContent().equals("LightsSabotage")) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("LightsProblem");
						List<String> players = bb.getInstance().getAllPlayers();
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						send(msg);
					    //addBehaviour(tbf.wrap(reactorTime)); TODO TESTAR ISTO DPS
						timer = 40;
						sabotagem = true;
					}else if(rec.getContent().equals("LightsFix")) {
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("LightsFixed");
						List<String> players = bb.getInstance().getAllPlayers();
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						send(msg);
						sabotagem = false;
						bb.setEmergencyCalling(false);
						
					}
				}	
			}
		};
		addBehaviour(tbf.wrap(lightsTime));
		addBehaviour(tbf.wrap(interaction));
	}

}
