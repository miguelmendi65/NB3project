package commodityMarket.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;

import commodityMarket.domain.CommodityAssets;
import commodityMarket.domain.CommodityMarket;
import commodityMarket.domain.CommodityUtilityProfile;
import ddejonge.negoServer.Logger;

public class RunMarketAndFiveAgents {
	
	/**
	 * Starts 5 negotiators, each in a new thread.
	 * @param args
	 */
	public static void main(String[] args) {
		
		String[] names = {"Alice", "Bob", "Charles", "David", "Eve"};
		
		//If this method is called on 23 December 2015 at exactly 15:50 
		// then the dateString will be:  2015_12_23__15-30-00
		String dateString = Logger.getDateString();
		
		//Set the path where all the log files will be stored.
		String logfolderPath = "log/" + dateString;
		
		//Set the duration of the negotiations to 30 seconds.
		int negotiationLength = 30*1000;
		
		
		//Run an instance of the Commodity Market
		CommodityMarket market = new CommodityMarket(logfolderPath, negotiationLength);
		CommodityAssets assets = CommodityAssets.getRandomInstance();
		market.setAssets(assets);
		CommodityUtilityProfile utilityProfile = CommodityUtilityProfile.getRandomProfile(assets.NUM_AGENTS, assets.NUM_COMMODITIES);
		market.setUtilityProfile(utilityProfile);
		market.start();
		
		
		try {
			
			InetAddress serverAddress = InetAddress.getLocalHost();
			
			for(int i=0; i<5; i++){
				
				System.out.println("RunFiveAgents.main() starting agent " + names[i]);
				
				Negotiator agent = new Negotiator(names[i], serverAddress, 1234);
				agent.enableLoggers(logfolderPath);
				agent.start();
			}
			
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
		

		
		
	}

}
