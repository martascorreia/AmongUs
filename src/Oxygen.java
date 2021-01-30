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

/**
 * Oxygen class
 * Represents the oxygen sabotage in the Among Us game, making the crewmates go fix it
 * @author Francisco Cavaco (51105), Marta Correia (51022) and Miguel Tavares (51966)
 *
 */
public class Oxygen extends Agent{
	private static final long serialVersionUID = 1L;
	private int timer = 40;
	private boolean sabotage = false;
	private Blackboard bb = Blackboard.getInstance();

	// Behaviors
	ThreadedBehaviourFactory tbf;
	TickerBehaviour oxygenTime;
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

		// behavior for the timer before it explodes
		oxygenTime = new TickerBehaviour(this,1000) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onTick() {

				if(bb.isMeeting()) {
					sabotage =false;
				}else {
					if(sabotage) {
						timer--;

						if(timer == 0) {
							ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
							System.out.println("Oxygen Exploded");
							msg.setContent("GameOver");
							List<String> players =  bb.getAllPlayers();

							for(String player : players) {
								msg.addReceiver(new AID(player,AID.ISLOCALNAME));
							}

							msg.addReceiver(new AID("O2",AID.ISLOCALNAME));
							msg.addReceiver(new AID("LIGHTS",AID.ISLOCALNAME));

							send(msg);
						}
					}
				}
			}
		};

		// behavior for the interaction between oxygen and the players
		interaction = new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage rec = null;
				rec= receive();
				if(rec != null){
					String message = rec.getContent();
					if(message.equals("OxygenSabotage")) {
						System.out.println("-----------------------OXYGEN SABOTAGE-----------------------");

						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("OxygenProblem");
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

					}else if(message.equals("OxygenFix")) {
						System.out.println("-----------------------OXYGEN FIXED-----------------------");

						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("OxygenFixed");
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
		addBehaviour(tbf.wrap(oxygenTime));
		addBehaviour(tbf.wrap(interaction));
	}

	private void stopBehaviours() {
		tbf.getThread(oxygenTime).interrupt();
		tbf.getThread(interaction).interrupt();
		tbf.interrupt();

	}
}
