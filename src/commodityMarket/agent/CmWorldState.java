package commodityMarket.agent;

import java.util.List;

import commodityMarket.domain.CommodityAssets;
import ddejonge.nb3.NB3Action;
import ddejonge.nb3.NB3WorldState;


public class CmWorldState extends NB3WorldState{

	//FIELDS
	CommodityAssets commodityAssets;
	
	
	//CONSTRUCTOR
	CmWorldState(CommodityAssets commodityAssignment){
		super(commodityAssignment.NUM_AGENTS);
		this.commodityAssets = commodityAssignment;
	}
	
	
	//METHODS OVERRIDDEN FROM NB3WorldState
	
	@Override
	public NB3WorldState copy() {
		return new CmWorldState(this.commodityAssets.copy());
	}
	
	@Override
	public boolean isLegal(List<? extends NB3Action> actions) {
		//check if the bid is valid
		
		//Create a temporary copy of the current CommodityAssets object.
		CommodityAssets tempCopy = this.commodityAssets.copy();
		
		for(NB3Action action : actions){
			CmAction cmAction = (CmAction)action;
			
			//Let the action act on the assets.
			tempCopy.exchange(cmAction.transaction);
			
		}
		
		//Now check that no agent has a negative amount of any commodity.
		for(int agentID=0; agentID<commodityAssets.NUM_AGENTS; agentID++){
			for(int commodity=0; commodity<commodityAssets.NUM_COMMODITIES; commodity++){
				if(tempCopy.getAssets(agentID, commodity) < 0){
					return false;
				}
			}
		}
		
		return true; 
	}

	
	/**
	 * Let the given actions act on this world state.
	 * That is: execute the commodity transactions so that the assets of the agents
	 *  are changed.
	 */
	@Override
	public void update(List<? extends NB3Action> actions) {
		
		for(NB3Action action : actions){
			commodityAssets.exchange(((CmAction)action).transaction);
		}
	}

	
	
	public String toString(){
		return this.commodityAssets.toString();
	}






	
	
}
