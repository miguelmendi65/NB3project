package commodityMarket.agent;


import commodityMarket.domain.CommodityTransaction;
import ddejonge.nb3.NB3Action;
import ddejonge.nb3.NB3AgentSet;



public class CmAction extends NB3Action implements Comparable<CmAction>{

	
	CommodityTransaction transaction;
	
	public CmAction(CommodityTransaction transaction, String[] agentNames){
		this.transaction = transaction;
	}
	
		
	@Override
	protected NB3AgentSet determineParticipatingAgents() {
		
		NB3AgentSet pa = new NB3AgentSet();
		pa.add(transaction.getConsumer());
		pa.add(transaction.getSupplier());
		
		return pa;

	}

	//Whenever we receive an incoming proposal we make sure that the actions of that proposal are added to the tree in the correct order.
	//This is not strictly necessary, but if not, it could mean we are adding a deal to the tree that is already represented in a different branch.
	// This compareTo function is used to put the actions of an incoming proposal in the right order, and must therefore be consistent with the
	// method getSplitLabels()
	@Override
	public int compareTo(CmAction action) {
		
		CommodityTransaction t1 = this.transaction;
		CommodityTransaction t2 = action.transaction;
		
		//First compare the suppliers.
		int diff = t1.getSupplier() - t2.getSupplier();
		if(diff != 0){
			return diff;
		}
		
		//If suppliers are equal, then compare the commodities
		diff = t1.getCommodity() - t2.getCommodity();
		if(diff != 0){
			return diff;
		}
		
		//if consumers are also equal, compare the quantities.
		diff = t1.getQuantity() - t2.getQuantity();
		if(diff != 0){
			return diff;
		}
		
		//if commodities are equal as well, compare the consumers.
		diff = t1.getConsumer() - t2.getConsumer();
		if(diff != 0){
			return diff;
		}
		
		//if everything is the same, return 0.
		return 0;
	}
	

	public String toString(){
		return this.transaction.toString();
	}

}
