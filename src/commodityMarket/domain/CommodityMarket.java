package commodityMarket.domain;
import java.io.File;
import java.net.UnknownHostException;
import java.util.List;

import commodityMarket.agent.Negotiator;
import ddejonge.negoServer.Logger;
import ddejonge.negoServer.NegotiationServer;


public class CommodityMarket extends Thread{

	public static final int SERVER_PORT = 1234;
	
	String logFolderPath = "log" + File.separator + Logger.getDateString() + File.separator; 
	
	//set the duration of the negotiations to 30 seconds.
	int negotiationLength = 30*1000;
	
	CommodityAssets assets = null;
	CommodityUtilityProfile utilityProfile = null;
	
	/**
	 * @param args
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws UnknownHostException {
		
		CommodityMarket market = new CommodityMarket();
		market.startNewSession();
	}
	
	public CommodityMarket(){
		
	}
	
	public CommodityMarket(String logFolderPath, int negotiationLength){
		this.logFolderPath = logFolderPath;
		this.negotiationLength = negotiationLength;
	}
	
	
	
	@Override
	public void run() {
		startNewSession();
	}
	
	public void setAssets(CommodityAssets assets){
		this.assets = assets;
	}
	
	public void setUtilityProfile(CommodityUtilityProfile utilityProfile){
		this.utilityProfile = utilityProfile;
	}
	
	private void startNewSession(){
		
		//1. Create an instance of some negotiation domain.
		// The world state represents how many units each negotiator has of each commodity.
		// This information is available to everyone.
		if(assets == null){
			assets = CommodityAssets.getRandomInstance();
		}
		//2. Create a utility profile and pass it to each of the agents
		// This represents how much each agent needs of each commodity.
		// The object created here, is the true preference of each agent. However, the agents will only receive 
		//   a modified version of this object, in which the utility functions of the other agents are slightly adjusted. 
		//   this is to represent the fact that agents do not have perfect information about each others' preferences.
		if(utilityProfile == null){
			utilityProfile = CommodityUtilityProfile.getRandomProfile(assets.NUM_AGENTS, assets.NUM_COMMODITIES);
		}
		/*
		printAssetsAndUtilityProfiles(assets, utilityProfile);
		*/

		
		//This is to print the initial utilities of each agent.
		int[] intitialUtilities = new int[assets.NUM_AGENTS];
		for(int i=0; i<assets.NUM_AGENTS; i++){
			intitialUtilities[i] = utilityProfile.calculateValue(i, assets);
		}
		
		
		//3 Set up a negotiation server.
		CommodityMarketNotary notary = new CommodityMarketNotary(negotiationLength, assets, utilityProfile);
		NegotiationServer negoServer = new NegotiationServer(notary);
		negoServer.setPortNumber(SERVER_PORT);
		negoServer.enableLogging(logFolderPath, "server.log");
		negoServer.startServerInNewThread();
		
	
		System.out.println();
		System.out.println("Negotiation Sever started!");
		System.out.println();
		
		long deadline = notary.getDeadline();
		
		long lastPrintedTime = 1000;
		long currentTime = System.currentTimeMillis();
		
		//wait till deadline passes.
		while(currentTime < deadline || deadline == 0){ //if deadline == 0 it means negotiations haven't started yet.
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			
			deadline = notary.getDeadline();
			currentTime = System.currentTimeMillis();
			
			long secondsToGo = (deadline - System.currentTimeMillis()) / 1000;
			if(secondsToGo > 0 && secondsToGo < lastPrintedTime){
				lastPrintedTime = secondsToGo;
				System.out.println(secondsToGo + " seconds to go till deadline.");
			}
			
			
		}
		
		notary.printResults(intitialUtilities);
		
		negoServer.stopServer();


		
		System.out.println("Negotiations have finished!");
		

	}
	
	
	
	

	

	

	

	

}
