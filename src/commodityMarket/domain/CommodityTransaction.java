package commodityMarket.domain;

import java.io.Serializable;

public class CommodityTransaction implements Serializable{
	
	//STATIC FIELDS

	//FIELDS
	int supplier;
	int commodity;
	int quantity;
	int consumer;
	
	
	//CONSTRUCTORS
	public CommodityTransaction(int supplier, int commodity, int quantity, int consumer){
		this.supplier = supplier;
		this.commodity = commodity;
		this.quantity = quantity;
		this.consumer = consumer;
	}
	
	//METHODS

	//GETTERS AND SETTERS
	public int getSupplier(){
		return this.supplier;
	}
	
	public int getCommodity(){
		return this.commodity;
	}
	
	public int getQuantity(){
		return this.quantity;
	}
	
	public int getConsumer(){
		return this.consumer;
	}
	

	
	
	public String toString(){
		
		String s = "agent " + supplier + " gives " + quantity + " units of " + commodity + " to agent " + consumer; 
		return s;
		
	}
}
