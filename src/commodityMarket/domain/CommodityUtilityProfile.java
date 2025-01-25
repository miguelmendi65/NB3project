package commodityMarket.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;



public class CommodityUtilityProfile implements Serializable{
	
	//STATIC FIELDS
	static Random random = new Random();
	
	//FIELDS
	int numAgents;
	int numCommodities;
	
	//stores for each commodity how much units are needed to generate 1 output utility value.
	int[][] requirements;
	
	
	//CONSTRUCTORS
	private CommodityUtilityProfile(int numAgents, int numCommodities){
		
		this.numAgents = numAgents;
		this.numCommodities = numCommodities;
		
		requirements = new int[numAgents][numCommodities];
	}
	
	
	
	
	//METHODS
	public int calculateValue(int agentID, CommodityAssets assets){
		return calculateValue(requirements[agentID], assets.getAssets(agentID));
	}
	
	public int calculateValue(int agentID, int[] stock){
		return calculateValue(requirements[agentID], stock);
	}
	
	
	public int calculateValue(int[] requirements, int[] stock){
		
		int[] stock_copy = Arrays.copyOf(stock, stock.length);
		
		int value = 0;
		
		while(true){
			
			stock_copy = subtract(stock_copy, requirements);
		
			if(hasNegativeValue(stock_copy)){
				break;
			}
			
			value++;
			
		}
		
		return value;
		
	}

	//returns the vector x - y
	private int[] subtract(int[] x, int[] y){
		
		for(int i=0; i<x.length; i++){
			
			x[i] -= y[i];
			
		}
		
		return x;
	}
	
	private boolean hasNegativeValue(int[] x){
		
		for(int i=0; i<x.length; i++){
			
			if(x[i] < 0){
				return true;
			}
			
		}
		
		return false;
	}

	public static CommodityUtilityProfile getRandomProfile(int numAgents, int numCommodities){
		
		CommodityUtilityProfile profile = new CommodityUtilityProfile(numAgents, numCommodities);
		
		boolean ok = false;
		
		while(!ok){
		
			for(int i=0; i<numAgents; i++){
				
				//pick two random commodities:
				int randomCommodity1 = -1;
				int randomCommodity2 = -1;
				while(randomCommodity1 == randomCommodity2){
					randomCommodity1 = random.nextInt(numCommodities);
					randomCommodity2 = random.nextInt(numCommodities);
				}
	
				//assign positive value to those two commodities.
				profile.requirements[i][randomCommodity1] = 1 + random.nextInt(10);
				profile.requirements[i][randomCommodity2] = 1 + random.nextInt(10);
				
			}
			
			//For each commodity there must be at least one agent that doesn't require this commodity.
			ok = checkIfOkay(profile);

		
		}
		return profile;
	}
	
	//checks that for every commodity there is at least one agent that doesn't require that commodity.
	private static boolean checkIfOkay(CommodityUtilityProfile profile){
		
		for(int j=0; j<profile.numCommodities; j++){
			
			boolean thisCommodityIsOkay = false;
			
			for(int i=0; i<profile.numAgents; i++){
				
				if(profile.requirements[i][j] == 0){
					thisCommodityIsOkay = true;
				}
				
			}
			
			if( ! thisCommodityIsOkay){
				return false;
			}
			
		}
		
		return true;
	}
	
	
	public int getRequirements(int agentID, int commodity){
		return this.requirements[agentID][commodity];
	}
	
	/**
	 * Returns a copy of the preference profile, but with the preferences of all other agents slightly modified in a random way. 
	 * 
	 * @param agentID The id of the agent for which the preferences should not be modified.
	 * @return
	 */
	public CommodityUtilityProfile modify(int agentID) {
		
		CommodityUtilityProfile adapted = new CommodityUtilityProfile(this.numAgents, this.numCommodities);
		
		for(int i=0; i<numAgents; i++){
			
			for(int j=0; j<numCommodities; j++){
				
				
				adapted.requirements[i][j] = this.requirements[i][j];
				
				if(i != agentID && this.requirements[i][j] != 0){
					adapted.requirements[i][j]--;
					adapted.requirements[i][j] += random.nextInt(3);
				}
				
			}
			
		}
		
		return adapted;
	}


	//GETTERS AND SETTERS
}
