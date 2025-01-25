package commodityMarket.domain;

import java.io.Serializable;
import java.util.Random;


public class CommodityAssets implements Serializable{

	
	//FIELDS
	public final String[] commodityNames = {"GOLD", "OIL", "IRON", "GRAIN", "WOOD"};
	
	public final int NUM_COMMODITIES = commodityNames.length;
	public final int NUM_AGENTS = 5;
	
	public static final int MAX_VALUE = 300; //the highest number of units that any agent can initially have.
	public static final int MAX_TRANSACTION_VALUE = 99;
	
	
	
	/**
	 * Stores for each agent how many items of each commodity it has. 
	 */
	private int[][] stock = new int[NUM_AGENTS][NUM_COMMODITIES];
	
	//CONSTRUCTORS
	public CommodityAssets() {
		
	}

	public static CommodityAssets getRandomInstance(){
		
		CommodityAssets assignment = new CommodityAssets();
		
		Random random = new Random();
		
		for(int i=0; i<assignment.NUM_AGENTS; i++){
			for(int j=0; j<assignment.NUM_COMMODITIES; j++){
				assignment.setAssets(i, j, random.nextInt(MAX_VALUE+1));
			}
		}
		
		return assignment;
	}
	
	
	
	public void setAssets(int agentID, int good, int quantity){
		
		if(stock == null && NUM_AGENTS != -1 ){
			stock = new int[NUM_AGENTS][NUM_COMMODITIES];
		}
		
		if(stock != null){			
			stock[agentID][good] = quantity;			
		}
	}
	
	public int[] getAssets(int agentID){
		return stock[agentID].clone();
	}
	
	public int getAssets(int agentID, int commodity){
		return stock[agentID][commodity];
	}
	
	public void exchange(CommodityTransaction transactoin){
		this.exchange(transactoin.supplier, transactoin.commodity, transactoin.quantity, transactoin.consumer);
	}
	
	void exchange(int supplier, int commodity, int quantity, int consumer){
		
		if(quantity > MAX_TRANSACTION_VALUE){
			throw new IllegalArgumentException("CommodityAssets.exchange() Error! Too many units of some commodity are being exchanged in one transaction: " + quantity + ", where maximum is " + MAX_TRANSACTION_VALUE);
		}
		
		this.stock[supplier][commodity] -= quantity;
		this.stock[consumer][commodity] += quantity;
	}
	
	public CommodityAssets copy(){
		
		CommodityAssets copy = new CommodityAssets();
		
		
		for(int i=0; i<NUM_AGENTS;i++){
			for(int j=0; j<NUM_COMMODITIES;j++){
				copy.stock[i][j] = this.stock[i][j];
			}
		}
		
		return copy;
		
	}
	
	public String toString(){
		String string = "";
		for(int ag=0; ag<NUM_AGENTS; ag++){
			string += "agent " + ag + " has: ";
			for(int comm=0; comm<NUM_COMMODITIES; comm++){
				
				if(comm > 0){
					string += " , ";
				}
				
				string += this.stock[ag][comm];

			}
			string += "\r\n";
		}
		
		return string;
	}
}
