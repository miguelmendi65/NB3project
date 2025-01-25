package commodityMarket.domain;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import ddejonge.negoServer.CommunicationProtocol;
import ddejonge.negoServer.Message;
import ddejonge.unstructuredNegotiationProtocol.Notary;
import ddejonge.unstructuredNegotiationProtocol.Proposal;
import ddejonge.unstructuredNegotiationProtocol.UnstructuredNegotiationProtocol;

public class CommodityMarketNotary extends Notary{
	
	
	//Domain info
	CommodityAssets commodityAssets;
	CommodityUtilityProfile utilityProfile;
	
	//IMPORTANT NOTE:
	// when the agents make proposals they need to specify a transaction using agent IDs rather than names.
	// this means that the agents must know the mapping between IDs and names used by the Notary.
	// In principle we could use the registeredNames list for this, but I think it is safer to do it explicitly.
	String[] id2Name;
	
	//inherited fields:
	//  long deadline;
	//  protected int numAgents;
	//  List<String> registeredNames
	
	

	public CommodityMarketNotary(int negotiationLength, CommodityAssets commodityAssets, CommodityUtilityProfile utilityProfile) {
		super(negotiationLength, commodityAssets.NUM_AGENTS);
		
		this.commodityAssets = commodityAssets;
		this.utilityProfile = utilityProfile;
		
		/*
		//also store these two objects in the domain info field of the Notary class.
		//This field contains the information that will be sent to the agents when the negotiations start.
		Serializable[] domInfo = new Serializable[2];
		domInfo[0] = this.commodityAssets;
		domInfo[1] = this.utilityProfile;
		
		this.domainInfo = domInfo;
		*/
		
	}

	
	
	@Override
	public String getName() {
		return UnstructuredNegotiationProtocol.NOTARY;
	}

	@Override 
	public boolean processAgentEntering(Message msg, List<Message> messagesToForward){
		
		boolean accessGranted;
		
		if(registeredNames.size() < numAgents){
			accessGranted = true;
		}else{
			accessGranted = false;
		}
		
		if(accessGranted){
			registeredNames.add(msg.getSender());
		
			messagesToForward.clear(); //to be sure that it is empty.
			
			
			//if the required amount of agents has registered and there is some domain info to share, then send this information to the agents.
			if(registeredNames.size() == numAgents){
				
				id2Name = new String[numAgents];
				
				for(int i=0; i<numAgents; i++){
					id2Name[i] = registeredNames.get(i);
				}
				
				for(int i=0; i<numAgents; i++){
					
					//Make a copy of the preference profile, but with the preferences of all other agents slightly modified in a random way, 
					//   to make sure that the agents don't have perfect information about the other agents' preferences.
					CommodityUtilityProfile adaptedPreferenceProfile = utilityProfile.modify(i);
				
					this.deadline = System.currentTimeMillis() + negotiationLength;
					
					Serializable[] domInfo = new Serializable[4];
					domInfo[0] = this.commodityAssets;
					domInfo[1] = adaptedPreferenceProfile;
					domInfo[2] = this.deadline;
					domInfo[3] = this.id2Name;
					messagesToForward.add(new Message(CommunicationProtocol.PROTOCOL_CONVERSATION_ID, registeredNames, CommunicationProtocol.START, domInfo));
					
				
				}
				
			
				System.out.println("CommodityMarketNotary.processAgentEntering() Five agents have registered. Let's start!");
				System.out.println();
				
				printAssetsAndUtilityProfiles();
			
			}
		}
		
		return accessGranted;
		
	}
	
	
	String getAgentNameById(int id){
		return id2Name[id];
	}
	
	int getAgentIdByName(String name){
		for (int i = 0; i < id2Name.length; i++) {
			if(id2Name[i].equals(name)){
				return i;
			}
		}
		
		throw new RuntimeException("CommodityMarketNotary.getAgentIdByName() Error! unknown name: " + name + Arrays.toString(id2Name));
	}
	

	@Override
	protected boolean verifyValidity(Proposal proposal) {
		
		//This method is called whenever all participating agents have accepted the proposal.
		
		
		//extract the list of commodity transactions from the incoming Proposal object.
		List<CommodityTransaction> commodityTransactions = (List<CommodityTransaction>)proposal.getProposedDeal();
		
		//Create a temporary copy of the current CommodityAssets object.
		CommodityAssets tempCopy = this.commodityAssets.copy();
		
		//Let the proposed transaction act on the assets.
		for(CommodityTransaction transaction : commodityTransactions){
			tempCopy.exchange(transaction);
		}

		//Now check that no agent has a negative amount of any commodity.
		for(int agentID=0; agentID<commodityAssets.NUM_AGENTS; agentID++){
			for(int commodity=0; commodity<commodityAssets.NUM_COMMODITIES; commodity++){
				if(tempCopy.getAssets(agentID, commodity) < 0){
					return false;
				}
			}
		}
		
		//if everything is okay, then the copy will become the new CommodityAssets object.
		this.commodityAssets = tempCopy;
		
		this.logger.logln("CommodityMarketNotary.verifyValidity() DEAL CONFIRMED: " + getProposalString(proposal), true);
		
		return true;
	}
	
	
	String getProposalString(Proposal proposal){
		
		String s = "Agents: " + proposal.getParticipants() + System.lineSeparator();
		List<CommodityTransaction> transactions = (List<CommodityTransaction>)proposal.getProposedDeal();
		for(CommodityTransaction transaction : transactions){
			s += getAgentNameById(transaction.supplier) + " supplies " + transaction.quantity + " units of " + commodityAssets.commodityNames[transaction.commodity] + " to " + getAgentNameById(transaction.consumer) + System.lineSeparator();
		}
		return s;
	}
	
	
	void printAssetsAndUtilityProfiles(){
		
		this.logger.logln("REQUIREMENTS", true);
		
		String[][] stringTable1 = new String[commodityAssets.NUM_AGENTS+1][commodityAssets.NUM_COMMODITIES + 1];
		
		for(int col=1; col<=commodityAssets.NUM_COMMODITIES; col++){
			stringTable1[0][col] = "" + commodityAssets.commodityNames[col-1];
		}
		
		for(int row=1; row<=commodityAssets.NUM_AGENTS; row++){
			
			stringTable1[row][0] = getAgentNameById(row-1) + ": ";
			for(int col=1; col<=commodityAssets.NUM_COMMODITIES; col++){
				stringTable1[row][col] = "" + utilityProfile.requirements[row-1][col-1];
			}
		}
		
		this.logger.logln(table2string(stringTable1, 10), true);
		this.logger.logln(true);
		
		this.logger.logln("ASSETS", true);
		String[][] stringTable2 = new String[commodityAssets.NUM_AGENTS][commodityAssets.NUM_COMMODITIES + 1];
		for(int i=0; i<commodityAssets.NUM_AGENTS; i++){
			
			stringTable2[i][0] = getAgentNameById(i) + ": ";
			for(int j=0; j<commodityAssets.NUM_COMMODITIES; j++){
				stringTable2[i][j+1] = "" + commodityAssets.getAssets(i,j);
			}
		}
		
		this.logger.logln(table2string(stringTable2, 10), true);
		
		
	}


	static String table2string(String[][] table, int columnWidth){
		
		int numRows = table.length;
		int numCols = table[0].length;
		
		
		String string = "";
		
		for(int row = 0; row<numRows; row++){
			
			if(row > 0){
				string += System.lineSeparator();
			}
			
			String line = "";
			
			for(int col = 0; col<numCols; col++){
				
				String entry = table[row][col];
				if(entry == null){
					entry = "";
				}
				while(entry.length() < columnWidth){
					entry = " " + entry;
				}
				
				line = line + entry;
			}
			
			string += line;
			
		}
		
		return string;
	}
	
	

	void printResults(int[] intitialUtilities){
		
		
		this.logger.logln("UTILITIES:", true);
		
		//Let's print the initial and final utilities of each agent.
		String[][] stringTable = new String[commodityAssets.NUM_AGENTS+1][3];
		stringTable[0][1] = "BEFORE";
		stringTable[0][2] = "AFTER";
		for(int row=1; row<=commodityAssets.NUM_AGENTS; row++){
			
			int finalVal = utilityProfile.calculateValue(row-1, commodityAssets);
			
			stringTable[row][0] = registeredNames.get(row-1) + ": ";
			stringTable[row][1] = "" + intitialUtilities[row-1];
			stringTable[row][2] = "" + finalVal;
		}
		
		this.logger.logln(table2string(stringTable, 10), true);
	}
	

	
}
