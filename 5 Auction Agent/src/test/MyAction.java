package test;

import logist.topology.Topology.City;

public class MyAction {
	private enum actionType{
		PICKUP,
		DELIVER
	}
	
	private actionType aType;
	private City city;
	private int weight;
	
	public int id;
	
	public MyAction(boolean isPickup, City city, int weight, int id){
		this.aType = isPickup ? actionType.PICKUP : actionType.DELIVER;
		this.city = city;
		this.weight = weight;
		this.id = id;
	}
	
	public City getCity(){
		return city;
	}

	public int getWeight(){
		if(aType == actionType.PICKUP)
			return weight;
		else return -weight;
	}
	
	public boolean isPickup(){
		return (aType == actionType.PICKUP);
	}
	
	public boolean isDeliver(){
		return (aType == actionType.DELIVER);
	}
}
