package commodityMarket.agent;


import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import commodityMarket.domain.*;
import ddejonge.nb3.NB3Message;
import ddejonge.nb3.NB3WorldState;
import ddejonge.negoServer.Message;
import ddejonge.negoServer.NegotiationClient;
import ddejonge.unstructuredNegotiationProtocol.ProposalImpl;

public class Negotiator extends Thread{


	public static final int SERVER_PORT = 1234;
	
	//FIELDS
	
	/**The name used to connect to the server.*/
	String myName;
	
	/**To connect with the negotiation server.*/
	NegotiationClient negoClient;
	
	//Information about the instance of the Commodity Market.
	CommodityAssets intitialAssets;
	CommodityUtilityProfile preferenceProfile;
	String[] agentNames;
	long deadline;
	
	
	//THE NB3 ALGORITHM
	CmNB3Algorithm nb3Algorithm;
	
	
	
	public static void main(String[] args) {
		
		if(args.length == 0){
			System.err.println("Error! You must specify a name for the agent in the command line arguments.");
		
		}else{
			
			String name = args[0];
			
			try {
				InetAddress serverAddress = InetAddress.getLocalHost();
				Negotiator negotiator = new Negotiator(name, serverAddress, SERVER_PORT);
				negotiator.negotiate();
				
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}

		}
		
	}
	
	
	
	//CONSTRUCTOR
	public Negotiator(String name, InetAddress serverAddress, int serverPort){
		this.myName = name;
		this.negoClient = new NegotiationClient(serverAddress, serverPort);
		this.nb3Algorithm = new CmNB3Algorithm(this);
	}
	
	
	@Override
	public void run() {
		
		//This method allows us to run the negotiate() method in a separate thread.
		
		negotiate();
	}
	
	/**
	 * Call this method to make sure that everything the nb3 algorithm does is logged.
	 * @param folderPath
	 */
	public void enableLoggers(String folderPath){
		this.nb3Algorithm.enableLoggers(folderPath);
	}
	
	public void negotiate(){
		
		
		//register with the server.
		this.negoClient.connect(this.myName);
		
		//TODO: handle the case that something went wrong with the connection.
		
		
		
		//Wait till we the connection with the server is established and all other agents are also connected.
		//Once all agents are connected the method waitTillReady() will return the information (received from the Notary)
		// about the instance of the commodity market.
		System.out.println("CmNegotiator.negotiate() " + this.myName + " waiting for start message.");
		Serializable[] domainInfo = (Serializable[])this.negoClient.waitTillReady();
		System.out.println("CmNegotiator.negotiate() " + this.myName + " Has received start message.");
		
		
		//extract the information from the domain info sent to us by the Notary.
		for (int i = 0; i < domainInfo.length; i++) {
			
			if(domainInfo[i] instanceof CommodityAssets){
				this.intitialAssets = (CommodityAssets)domainInfo[i];
			}else if(domainInfo[i] instanceof CommodityUtilityProfile){
				this.preferenceProfile = (CommodityUtilityProfile)domainInfo[i];
			}else if(domainInfo[i] instanceof Long){
				this.deadline = (long)domainInfo[i];
			}else if(domainInfo[i] instanceof String[]){
				
				// Each agent is identified by a number as well as by a name.
				// this number is simply the index of the name in this array.
				this.agentNames = (String[]) domainInfo[i];
			}
		}
		
		long currentTime = System.currentTimeMillis();
		long time_last_decision = -1;
		
		//set the duration of the interval between consecutive accept/propose decisions.
		// e.g. if this is set to 100 ms. then the agent will expand the search
		// tree for 100 ms. before it will again determine if it is time to make a new proposal
		// or to accept a standing offer. After making this decision it will again spend
		// 100 ms. expanding the tree, etcetera.
		// Here, we set it to 1% of the total length of the negotiation session. 
		long deliberation_time = (deadline - currentTime) / 100;
		
		
		//Create the initial world state for the NB3 algorithm
		NB3WorldState nb3InitialState = new CmWorldState(this.intitialAssets);
		
		//Initialize the algorithm
		nb3Algorithm.initialize(myName, agentNames, nb3InitialState, deadline);

		
		
		//***MAIN LOOP	
		
		//loop until the deadline is reached, or a deal has been confirmed.
		while(currentTime < this.deadline){
			
			//Expand the search tree (picks one node and adds children to it).
			nb3Algorithm.expand();
			
			//See if we have received any message from any of the other negotiators.
			// e.g. a new proposal or an acceptance of a proposal made earlier.
			if(negoClient.hasMessage()){
				
				
				//if yes, remove it from the message queue.
				Message msg = negoClient.removeMessageFromQueue();
				
				//convert it so that the NB3 algorithm can handle it.
				NB3Message nb3msg = nb3Algorithm.convertMessage(msg);
				
				//let the NB3 algorithm handle the message.
				nb3Algorithm.handleIncomingMessages(nb3msg);
			}
			
			
			//Check if enough time has passed since our last 'accept-or-propose decision' and if yes make another accept-or-propose decision.
			if(currentTime - time_last_decision > deliberation_time){	
				time_last_decision = currentTime;
				nb3Algorithm.acceptOrPropose();
			}
			
			currentTime = System.currentTimeMillis();
		}
		
		
		//make sure the search- and negotiation- logs are written to hard disk. 
		this.nb3Algorithm.writeLogFiles();
	}
	
	
	
	public void sendMessage(List<String> receivers, String performative, ProposalImpl content){
		
		try {
			this.negoClient.sendMessage(receivers, performative, content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	

	
}
