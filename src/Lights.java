import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

/**
 * Lights class
 * Represents the lights sabotage in the Among Us game, making the crewmates lose vision distance
 * @author Francisco Cavaco (51105), Marta Correia (51022) and Miguel Tavares (51966)
 *
 */
public class Lights extends Agent {
	private static final long serialVersionUID = 1L;
	private Blackboard bb = Blackboard.getInstance();
	
	// Behaviors
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
				
		// behavior for the interaction between lights and the players
		interaction = new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage rec = null;
				rec = receive();
				if(rec != null){
					String message = rec.getContent();
					if(message.equals("LightsSabotage")) {
						System.out.println("-----------------------LIGHTS SABOTAGE-----------------------");

						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("LightsProblem");
						List<String> players = bb.getAllAlivePlayers();
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
						if(!bb.isMeeting())
							send(msg);
										
					}else if(message.equals("LightsFix")) {
						System.out.println("-----------------------LIGTHS FIXED-----------------------");

						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setContent("LightsFixed");
						List<String> players = bb.getAllAlivePlayers();
						for(String player : players) {
							msg.addReceiver(new AID(player,AID.ISLOCALNAME));
						}
						msg.addReceiver(new AID("Game",AID.ISLOCALNAME));
						if(!bb.isMeeting())
							send(msg);
						bb.setEmergencyCalling(false);
						bb.setCrewmateVision(bb.CREWMATE_VISION);
						
					} else if(message.contentEquals("GameOver")) {
						stopBehaviours();
					}
					
				}	
			}
		};
		
		addBehaviour(interaction);
	}

	private void stopBehaviours() {
		removeBehaviour(interaction);
	}
}
