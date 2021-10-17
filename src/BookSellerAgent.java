import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.DFService;
public class BookSellerAgent extends Agent {
    // The catalogue of books for sale (maps the title of a book to its price)
    private Hashtable catalogue;
    // Put agent initializations here
    protected void setup() {
        catalogue = new Hashtable();
        Object[] args = getArguments();
        String books = "";
        if (args != null && args.length > 0) {
            System.out.println("Hello! Seller-agent "+getAID().getName()+" is ready.");
            System.out.println("Books:");
            args = ((String)args[0]).split(" ");
            for (int i = 0; i < args.length / 2; i++) {
                catalogue.put((String) args[2 * i], Integer.valueOf((String) args[2 * i + 1]));
                books += args[2 * i] + ": " + args[2 * i + 1] + " uah,  ";
            }

            System.out.println(books+"\n");
            books = "";
        } else {
            System.out.println("This seller " + getAID().getName() + "doesn't have books." );
            doDelete();
            return;
        }

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName("JADE-book-trading");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.format("Seller-agent "+ getAID().getName()+ " have been added to DF\n");

        addBehaviour(new OfferRequestsServer());
        addBehaviour(new PurchaseOrdersServer());
    }
    // Put agent clean-up operations here
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.format("Seller-agent "+ getAID().getName()+ " have been removed from DF\n");
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //
        System.out.println("Seller-agent "+getAID().getName()+" terminating.");
    }
    /**
     This is invoked by the GUI when the user adds a new book for sale
     */
    public void updateCatalogue(final String title, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(title, new Integer(price));
            }
        } );
    }
    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                // Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.get(title);
                if (price != null) {
                    // The requested book is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(price.intValue()));
                }
                else {
                    // The requested book is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else{
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
            if (msg != null) {
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.get(title);
                if (price != null) {
                    // The requested book is available for sale. Reply with the confirmation
                    reply.setPerformative(ACLMessage.INFORM);
                    catalogue.remove(title);
                } else {
                    // The requested book is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }}


