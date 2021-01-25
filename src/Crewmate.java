import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class Crewmate extends Agent {

	private static final long serialVersionUID = 1L;
	private final Blackboard bb = Blackboard.getInstance();
	
	// States
	private final String PLAYING = "Playing";
	private final String MEETING = "Meeting";
	private final String EMERGENCY = "Emergency";
	private static final String OVER = "Over"; 

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
		
	}

}
