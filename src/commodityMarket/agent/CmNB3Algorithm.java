package commodityMarket.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import commodityMarket.domain.*;
import ddejonge.nb3.NB3Action;
import ddejonge.nb3.NB3AgentSet;
import ddejonge.nb3.NB3Algorithm;
import ddejonge.nb3.NB3Message;
import ddejonge.nb3.NB3Node;
import ddejonge.nb3.NB3Proposal;
import ddejonge.nb3.NB3WorldState;
import ddejonge.nb3.NB3Message.MsgType;
import ddejonge.negoServer.Message;
import ddejonge.unstructuredNegotiationProtocol.Proposal;
import ddejonge.unstructuredNegotiationProtocol.ProposalImpl;



public class CmNB3Algorithm extends NB3Algorithm{
	
	//STATIC FIELDS
	final static int ROOT = 0;
	final static int SUPPLIER = 1;
	final static int COMMODITY = 2;
	final static int QUANTITY = 3;
	final static int CONSUMER = 4;
	
	final static int NUMBER_OF_NODE_TYPES = 4; //we have defined 4 node types (apart from the 'root' type).
	
	//FIELDS
	
	/**
	 * The agent that runs this algorithm.
	 */
	Negotiator theAgent;
	
	//CONSTRUCTOR
	CmNB3Algorithm(Negotiator agent){
		super(NUMBER_OF_NODE_TYPES); 	//Make sure the parent class knows how many node types we have defined.
		this.theAgent = agent;			
	}

	
	
	//GETTERS AND SETTERS
	@Override
	protected int getChildNodeType(int typeOfNodeToSplit){
		
		//When the NB3 algorithm is about to add children to a tree node
		// it calls this method to know which type it should assign to the new
		// nodes.
		
		switch (typeOfNodeToSplit) {
		case ROOT:
			return SUPPLIER;
		case SUPPLIER:
			return COMMODITY;
		case COMMODITY:
			return QUANTITY;
		case QUANTITY:
			return CONSUMER;
		case CONSUMER:
			return SUPPLIER;
		default:
			throw new IllegalArgumentException("ExampleNB3Algorithm.getChildNodeType() Error! unknown node type: " + typeOfNodeToSplit);
		}
	}

	
	@Override
	protected List<Object> getSplitLabels(NB3Node nodeToSplit) {
		
		//this happens if there is no more node to split in the queue.
		if(nodeToSplit == null){
			return null;
		}
		
		List<Object> childLabels = new ArrayList<>();
				

		CmWorldState worldState = (CmWorldState)this.getCurrentState();
		int numAgents = worldState.commodityAssets.NUM_AGENTS;
		int numCommodities = worldState.commodityAssets.NUM_COMMODITIES;
		
		int supplierID;
		int commodityID;
		
		
		int childNodeType = getChildNodeType(nodeToSplit.getType());
		
		switch (childNodeType) {
		case SUPPLIER: //add SUPPLIER nodes
			
			for(supplierID=0; supplierID<numAgents; supplierID++){
				
				//verify that we can add it.
				if( ! canBeAdded(nodeToSplit, supplierID)){				
					continue;
				}
				
				childLabels.add(supplierID);
			}
			
			break;
		case COMMODITY: //add COMMODITY nodes
			
			supplierID = (int) nodeToSplit.getLabel();
			
			for(commodityID=0; commodityID<numCommodities; commodityID++){
				
				
				//verify that we can add it.
				if( ! canBeAdded(nodeToSplit, commodityID)){				
					continue;
				}
				
				//You can't supply this commodity if you don't have any units of this commodity. 
				if(worldState.commodityAssets.getAssets(supplierID, commodityID) == 0){
					continue;
				}
				
				//if yes, then add it.
				childLabels.add(commodityID);
				
			}
			
			break;
		case QUANTITY: //add QUANTITY nodes
			
			//get the current distribution of assets.
			CommodityAssets assets = worldState.commodityAssets;
			
			//get the id of the supplier given in the grand parent node
			supplierID = (int)nodeToSplit.getParent().getLabel();
			
			//get the id of the commodity given in the parent node.
			commodityID = (int)nodeToSplit.getLabel();
			
			//now find out how many units of this commodity the supplier owns.
			int theSupplierOwns = assets.getAssets(supplierID, commodityID); 
			
			int maxValue = 10;
			if(theSupplierOwns < maxValue){
				maxValue = theSupplierOwns;
			}
			
			//create a label for each possible quantity
			for(int quantity=1; quantity<=maxValue; quantity++){
				
				Integer label = new Integer(quantity);
				
				//verify that we can add it.
				if( ! canBeAdded(nodeToSplit, label)){				
					continue;
				}
				
				childLabels.add(label);
			}
			
			break;
		case CONSUMER:  //add CONSUMER nodes
			
			supplierID = (int)nodeToSplit.getParent().getParent().getLabel();
			commodityID = (int)nodeToSplit.getParent().getLabel();
			
			for(int consumerID=0; consumerID<numAgents; consumerID++){
				
				//the consumer cannot be the same as the supplier.
				if(consumerID == supplierID){
					continue; 
				}
				
				//It doesn't make sense to supply anything to someone who doesn't need that commodity.
				if(theAgent.preferenceProfile.getRequirements(consumerID, commodityID) == 0){
					continue;
				}
				
				//verify that we can add it.
				if( ! canBeAdded(nodeToSplit, consumerID)){				
					continue;
				}
				
				childLabels.add(consumerID);
			}
			
			break;
		default:
			throw new IllegalArgumentException("ExampleNB3Algorithm.getSplitLabels() Error! unknown node type: " + nodeToSplit.getType());
		}
		
		return childLabels;
	}

	
	public NB3AgentSet getParticipatingAgents(ArrayList<NB3Node> branch) {
		
		NB3AgentSet participatingAgents = new NB3AgentSet(); 
		for(NB3Node node : branch){
			
			if(node.getType() == SUPPLIER || node.getType() == CONSUMER){
				int agentID = (int)node.getLabel();
				participatingAgents.add(agentID);
			}
		}
		
		return participatingAgents;
	}
	
	@Override
	public List<Object> actions2Labels(List<? extends NB3Action> actions) {
		
		List<CmAction> cmActions = (List<CmAction>)actions;
		
		//make sure that the actions are in the right order so that they can be added to the tree in the right order.
		//it is not strictly necessary, but if not, it could mean we are adding a deal to the tree that is already represented in a different branch.
		Collections.sort(cmActions);
		
		//create a list to put the labels in and return.
		List<Object> labels = new ArrayList<>(cmActions.size() * NUMBER_OF_NODE_TYPES);
		
		for(CmAction action : cmActions){
			
			//Important! these labels should be added in exactly this order,
			//because this is also the order in which the tree search adds
			//labels to the tree,
			labels.add(action.transaction.getSupplier());
			labels.add(action.transaction.getCommodity());
			labels.add(action.transaction.getQuantity());
			labels.add(action.transaction.getConsumer());
			
		}
		
		return labels;
	}

	@Override
	public List<? extends NB3Action> branch2actions(List<NB3Node> branch) {
		
		List<CmAction> actions = new ArrayList<CmAction>();
		
		int supplier = -1;
		int commodity = -1;
		int quantity = -1;
		int consumer = -1;
		
		
		for(NB3Node node : branch){
			
			if(node.getType() == SUPPLIER){
				
				supplier = (int)node.getLabel();
				
			}else if(node.getType() == COMMODITY){
				
				commodity = (int)node.getLabel();
				
			}else if(node.getType() == QUANTITY){
				
				quantity = (int)node.getLabel();
			
			}else if(node.getType() == CONSUMER){
				
				consumer = (int)node.getLabel();
				
			}
			
			
			//Every 4th node in the branch represents a new action. Therefore, after every 4th node we collect the
			//values of the previous 4 nodes and put them together into an action.
			if(node.getType() == NUMBER_OF_NODE_TYPES){
				
				if(supplier == -1 || commodity == -1 || consumer == -1 || quantity == -1){
					throw new IllegalArgumentException("branch2actions() Error! the components of the CommodityTransactions have not been set correctly.");
				}
				
				///now that we have all labels, we can create a transaction object
				CommodityTransaction transaction = new CommodityTransaction(supplier, commodity, quantity, consumer);
				
				//wrap the transaction object into a CmAction.
				CmAction action = new CmAction(transaction, getAgentNames());
				
				//store the action in the list.
				actions.add(action);
				
				//reset these values.
				supplier = -1;
				commodity = -1;
				quantity = -1;
				consumer = -1;
				
			}
			
			
		}
		
		return actions;
	}

	

	
	
	
	//METHODS TO CALCULATE THE NODE BOUNDS.
	@Override
	public float calculateUpperBound(int agentID, List<NB3Node> branch, NB3WorldState ws) {
		
		//We can calculate the upper bound of any agent by assuming that all other agents will give everything they
		// own to the given agent.
		
		//However, if the given agent already has supplied a certain commodity, then it doesn't make sense to also consume it in the same deal.
		//Therefore, we can ignore such transactions to refine the upper bound.
		
		
		//Make a copy of the current state and get the commodities currently owned by the agent.
		CommodityAssets maximalAssets = ((CmWorldState)ws).commodityAssets.copy();
		
		// Let all other agents give their assets to the current agent, except when the current agent has already acted
		// as a supplier for a certain commodity in the branch.
		for(int otherAgentID = 0; otherAgentID<maximalAssets.NUM_AGENTS; otherAgentID++){
			
			if(otherAgentID == agentID){
				continue;
			}
			
			for(int commodity=0; commodity<maximalAssets.NUM_COMMODITIES; commodity++){
				
				if( ! hasSupplied(agentID, commodity, branch) ){ //check if the given agent appears as a supplier of this commodity in the given branch.
					
					int quantity = maximalAssets.getAssets(agentID, commodity) + maximalAssets.getAssets(otherAgentID, commodity);
					
					maximalAssets.setAssets(agentID, commodity, quantity);
				}
			}
		}

		//calculate the utility value of the given agent when it has received all assets of all other agents
		//and return this value as the upper bound.
		int val = this.theAgent.preferenceProfile.calculateValue(agentID, maximalAssets);
		
		return val;
		
	}
	
	
	@Override
	public float calculateIntermediateValue(int agentID, List<NB3Node> branch, NB3WorldState ws){
		
		//convert the branch into a list of actions
		List<CmAction> actions = (List<CmAction>)this.branch2actions(branch);
		
		//crate a copy of the current world state and let those actions modify this copy.
		CmWorldState tempState = (CmWorldState)this.getCurrentState().copy();
		tempState.update(actions);
		
		//If the last node of the branch is a QUANTITY node, then we can refine 
		//the calculation, because we can subtract a number
		//of units of a certain commodity from its assets. (however we do not know to which consumer this will be given)
		if(branch.size() > 0){
			NB3Node lastNodeInBranch = branch.get(branch.size() - 1);
			if(lastNodeInBranch.getType() == QUANTITY){
				
				//get the supplier.
				int supplierID = (int)lastNodeInBranch.getParent().getParent().getLabel();
				
				//if the supplier is not the same as the agent for which we are calculating the intermediate value, then this will not have any effect.
				if(supplierID == agentID){
					
					int quantity = (int)lastNodeInBranch.getLabel();
					int commodityID = (int)lastNodeInBranch.getParent().getLabel();
					
					int oldStockSize = tempState.commodityAssets.getAssets(supplierID, commodityID);
					int newStockSize = oldStockSize - quantity;
					tempState.commodityAssets.setAssets(supplierID, commodityID, newStockSize);
				}
				
			}
		}
		
		//Now use the assets of the copied world state to calculate the utility value of the agent.
		int value = theAgent.preferenceProfile.calculateValue(agentID, tempState.commodityAssets);
		
		
		return (float)value;
		
	}
	
	@Override
	public float calculateLowerBound(int agentID, List<NB3Node> branch, NB3WorldState ws) {
		

		if(branch.size() < NUMBER_OF_NODE_TYPES){
			return 0f;
		}
		
		
		NB3Node lastNode = branch.get(branch.size()-1);

		//the supplier of a new transaction can only have higher ID than (or equal to) the supplier of the last one 
		//in the branch.
		//Therefore, if agentID is lower then the supplierID of last transaction, that agent can no longer act as a supplier,
		// so its lower bound must equal its intermediate value. 
		
		
		//get the last supplier node
		NB3Node lastSupplierNode = lastNode;
		while(lastSupplierNode.getType() != SUPPLIER){
			lastSupplierNode = lastSupplierNode.getParent();
		}
		
		//get the id of the last supplier.
		int lastSupplierID = (int)lastSupplierNode.getLabel();
		
		if(agentID < lastSupplierID){
			return calculateIntermediateValue(agentID, branch, ws);
		}
		
		
		
		
		
		//If agent A has received a certain commodity, then it doesn't make sense to give it away again in the same deal.
		// Therefore, the lower bound can be calculated by removing all commodities the agent has not received.
		
		
		//Make a copy of the current state and get the commodities currently owned by the agent.
		CommodityAssets assets = ((CmWorldState)ws).commodityAssets.copy();
		int[] agentAssets = assets.getAssets(agentID);
		
		// set all its commodities to zero, except those for which the given agent has acted as a consumer in the branch.
		for(int commodity=0; commodity<assets.NUM_COMMODITIES; commodity++){
			if( ! hasConsumed(agentID, commodity, branch)){
				agentAssets[commodity] = 0;
			}
		}
		
		return this.theAgent.preferenceProfile.calculateValue(agentID, agentAssets);

	}
	
	
	
	//COMMUNICATION LAYER.
	@Override
	protected NB3Message convertMessage(Object domainMessage){
		
		Message domainMsg = (Message)domainMessage;
		
		//Translate the message types defined by the domain to message types recognized the NB3 algorithm.
		MsgType msgType;
		if(domainMsg.getPerformative().equals("PROPOSE")){
			msgType = MsgType.PROPOSE;
		}else if(domainMsg.getPerformative().equalsIgnoreCase("ACCEPT")){
			msgType = MsgType.ACCEPT;
		}else if(domainMsg.getPerformative().equals("CONFIRM")){
			msgType = MsgType.CONFIRM;
		}else if(domainMsg.getPerformative().equals("REJECT")){
			msgType = MsgType.REJECT;
		}else if(domainMsg.getPerformative().equals("ILLEGAL")){
			msgType = MsgType.ILLEGAL;
		}else{
			throw new IllegalArgumentException("ExampleAgent.convertMessage() Error! unknown performative: " + domainMsg.getPerformative());
		}

		//Extract the Proposal object from the incoming message
		Proposal domainProposal = (Proposal)domainMsg.getContent();
		
		//Extract the list of proposed transactions from the proposal.
		List<CommodityTransaction> transActions = (List<CommodityTransaction>)domainProposal.getProposedDeal();
		
		//Convert the transactions into NB3 type actions.
		ArrayList<CmAction> actions = new ArrayList<>();
		for(CommodityTransaction transaction : transActions){
			actions.add(new CmAction(transaction, theAgent.agentNames));
		}
		
		NB3Message nb3Message = new NB3Message(domainMsg.getSender(), domainMsg.getReceivers(), msgType, actions, domainProposal.getId());
		
		return nb3Message;
	}
	
	@Override
	protected void sendMessage(NB3Proposal nb3Proposal, MsgType messageType) {

		
		//get the names of all agents negotiating. 
		String[] agentNames = this.getAgentNames();
		
		//set the list of receivers equal to the set of agents participating in the deal (except myself).
		List<String> participatingAgentNames = new ArrayList<>();
		List<String> receiverNames = new ArrayList<>();
		
		NB3AgentSet partAgents = nb3Proposal.getParticipatingAgents();
		for(int agentID : partAgents){
			
			participatingAgentNames.add(agentNames[agentID]);
			if( ! agentNames[agentID].equals(this.theAgent.myName)){
				receiverNames.add(agentNames[agentID]);
			}
		}
		
		//Translate the message types recognized the NB3 algorithm to the message types defined by the domain.
		String msgPerformative = null;
		if(messageType == MsgType.ACCEPT){
			msgPerformative = "ACCEPT";
		}else if(messageType == MsgType.PROPOSE){
			msgPerformative = "PROPOSE";
		}else{
			throw new IllegalArgumentException("CmNB3Algorithm.sendMessage() Error! unkown message type: " + messageType);
		}
		
		//Translate the NB3Action objects of the proposal into Action objects defined by the domain.
		ArrayList<CommodityTransaction> transactions = new ArrayList<>();
		for(NB3Action action : nb3Proposal.getActions()){
			transactions.add(((CmAction)action).transaction);
		}
		
		String proposalID = nb3Proposal.getID();
		
		ProposalImpl domainProposal = new ProposalImpl(proposalID, participatingAgentNames, transactions);
		
		//Now we can send the message.
		this.theAgent.sendMessage(receiverNames, msgPerformative, domainProposal);
		
	}


	
	
	
	
	
	
	//SOME HELPER FUNCTIONS



	/**
	 * Returns true if we can safely add a new child node to nodeToSplit with the given label.
	 * Returns false if adding the label could cause a certain deal to be represented more than once in the tree,
	 *  or if it results in a deal that doesn't make sense (i.e. the same agent acting both as a receiver and as a supplier for 
	 *  the same commodity)
	 * 
	 * @param nodeToSplit
	 * @param labelToAdd
	 * @return
	 */
	private boolean canBeAdded(NB3Node nodeToSplit, Object labelToAdd){
		
		return checkDoubleRole(nodeToSplit, labelToAdd) && checkHash(nodeToSplit, labelToAdd);
		
	}
	
	
	/**
	 * Returns true if we can safely add a new child node to nodeToSplit with the given label.
	 * Returns false if adding the label results in a deal that doesn't make sense 
	 *  (because some agent is acting both as a receiver and as a supplier for the same commodity)
	 * 
	 * @param nodeToSplit
	 * @param labelToAdd
	 * @return
	 */
	boolean checkDoubleRole(NB3Node nodeToSplit, Object labelToAdd){
		
		int typeOfLabelToAdd = getNextTypeOfNodeToSplit(nodeToSplit.getType());

		List<NB3Node> branch = nodeToSplit.getBranch();
		
		//If an agent receives any quantity of any commodity, we should not generate a transaction in which that same
		// agent supplies any quantity of the same commodity to any other agent.
		// After all, it doesn't make sense to make a deal in which you receive something and then give it away again.
		
		if(typeOfLabelToAdd == COMMODITY ){
			
			int currentCommodity = (int)labelToAdd;
			int currentSupplier = (int)nodeToSplit.getLabel();
			
			if(hasConsumed(currentSupplier, currentCommodity, branch)){
				return false;
			}
			
		}
		
		// Similarly, if you have supplied any commodity, you should not also receive it in the same deal.
		if(typeOfLabelToAdd == CONSUMER){
			
			int currentConsumer = (int)labelToAdd;
			int currentCommodity = (int)nodeToSplit.getParent().getLabel();
			
			if(hasSupplied(currentConsumer, currentCommodity, branch)){
				return false;
			}

		}
		
		return true;
	}
	
	/**
	 * Returns true if the given agent has acted as a consumer for the given commodity in the given branch.<br/>
	 * Returns false otherwise.
	 * 
	 * @param agentName
	 * @param currentCommodity
	 * @param branch
	 * @return
	 */
	boolean hasConsumed(int agentID, int currentCommodity, List<NB3Node> branch){
		
		for(int i=0; i<branch.size(); i+=NUMBER_OF_NODE_TYPES){ 
			
			int consumer = -1;
			int commodity = -1;
			
			for(int j=0; j<NUMBER_OF_NODE_TYPES; j++){
				
				if(i+j >= branch.size()){
					break;
				}
				
				NB3Node node = branch.get(i+j);

				if(node.getType() == CONSUMER){
					consumer = (int) node.getLabel();
				}else if(node.getType() == COMMODITY){
					commodity = (int) node.getLabel();
				}
				
			}
			
			//we see that the current consumer we want to add to the branch has already acted as a supplier for the same commodity.
			if(consumer == agentID && commodity == currentCommodity){
				return true;
			}

		}
		
		return false;
	}
	
	/**
	 * Returns true if the given agent has acted as a supplier for the given commodity in the given branch.<br/>
	 * Returns false otherwise.
	 * 
	 * @param agentName
	 * @param currentCommodity
	 * @param branch
	 * @return
	 */
	boolean hasSupplied(int agentID, int currentCommodity, List<NB3Node> branch){
		
		for(int i=0; i<branch.size(); i+= NUMBER_OF_NODE_TYPES){
			
			int supplier = -1;
			int commodity = -1;
			
			for(int j=0; j<NUMBER_OF_NODE_TYPES; j++){
			
				if(i+j >= branch.size()){
					break;
				}
				
				NB3Node node = branch.get(i+j);

				if(node.getType() == SUPPLIER){
					supplier = (int) node.getLabel();
				}else if(node.getType() == COMMODITY){
					commodity = (int) node.getLabel();
				}
			}
			
			//we see that the current consumer we want to add to the branch has already acted as a supplier for the same commodity.
			if(supplier == agentID && commodity == currentCommodity){
				return true;
			}
		}
		
		return false;
		
	}
	
	
	//these fields are only used by the method checkHash()
	List<Object> previousLabels = new ArrayList<Object>(2*NUMBER_OF_NODE_TYPES);
	NB3Node lastNode = null;
	
	/**
	 * Returns true if we can safely add a new child node to nodeToSplit with the given label.
	 * Returns false if adding the label could cause a certain deal to be represented more than once in the tree.
	 * 
	 * @param nodeToSplit
	 * @param labelToAdd
	 * @return
	 */
	boolean checkHash(NB3Node nodeToSplit, Object labelToAdd){
		
		//If nodeToSplit has less then 4 ancestors, return true immediately.
		//otherwise, get the label of the nodeToSplit and its n-1 ancestors, where n is at least 4 and at most 7.
		//The first 4 of these labels represent a single action, the last labels represent the new action that is under construction.
		//The action under construction must have a higher hash then the previous one.
		
		//1. Get the list containing the labels of n ancestors.
		previousLabels.clear();
		
		NB3Node node = nodeToSplit;
		while(true){
			
			if(node.getType() == ROOT){
				break;
			}
			
			previousLabels.add(node.getLabel());
			node = node.getParent();
			
			if(node.getType() == NUMBER_OF_NODE_TYPES && previousLabels.size() >= NUMBER_OF_NODE_TYPES ){
				break;
			}
			
		}
		Collections.reverse(previousLabels);
		
		
		if(previousLabels.size() < NUMBER_OF_NODE_TYPES){
			return true;
		}
		
		//Consistency check.
		if(previousLabels.size() >= 2 * NUMBER_OF_NODE_TYPES){
			throw new IllegalArgumentException("ExampleNB3Algorithm.calculateCheckSum() Error! too many labels: " + previousLabels.size());
		}
		
		previousLabels.add(labelToAdd);
		
		//determine the hash of the previous action.
		int hash1 = calculateHash(previousLabels.subList(0, NUMBER_OF_NODE_TYPES));
		
		//determine the hash of the action under construction.
		int hash2 = calculateHash(previousLabels.subList(NUMBER_OF_NODE_TYPES, previousLabels.size()));
		
		return hash2 >= hash1;
		
	}
	
	
	/**
	 * The given list of labels should not be more than NUM_LABEL_TYPES long.
	 * It represents a (partially defined) action.
	 * This method then returns a hash value for that action. This allows us to ensure that the actions in a branch will always be ordered from low
	 * hash to high hash, and hence preventing that the same deal can appear more than once with the actions in different orders.
	 * 
	 * @param labels
	 * @return
	 */
	int calculateHash(List<Object> labels){
		
		if(labels.size() > NUMBER_OF_NODE_TYPES){
			throw new IllegalArgumentException("ExampleNB3Algorithm.calculateCheckSum() Error! too many labels: " + labels.size());
		}
		
		
		//This code is to make sure the user has not changed the value of MAX_TRANSACTION_VALUE.
		//Currently dead code, but becomes live if someone sets the value of MAX_TRANSACTION_VALUE too high.
		if(CommodityAssets.MAX_TRANSACTION_VALUE > 99){
			throw new IllegalArgumentException("ExampleNB3Algorithm.calculateHash() Error! this method cannot handle transactions with more than 99 units being exchanged. Please reduce CommodityAssets.MAX_TRANSACTION_VALUE");
		}
		
		
		// A transaction is essentially just 4 numbers (supplierID, commodityID, quantity, and consumerID)
		// Therefore, the hash is simply calculated as those 4 numbers put next to each other. 
		
		int hash = 0;
		for(int i=0; i<NUMBER_OF_NODE_TYPES; i++){
			
			int number;
			
			if(i >= labels.size()){
				
				number = 99; 
				//if there are less than 4 labels the list represents only a partially defined action. For the last labels we simply choose the highest possible value,
				// because we are interested in the highest possible hash from any action that could result from this partially defined action. 
	
			}else{
				
				number = (int)labels.get(i);
			}
			
			
			hash *= 100;
			hash += number;
			
		}
		
		return hash;
	}


}
