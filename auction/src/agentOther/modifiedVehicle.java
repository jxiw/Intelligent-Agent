package agentOther;

import logist.simulation.Vehicle;
import logist.topology.Topology.City;

class modifiedVehicle {
	int id ;
	int capacity;
	City homeCity;
	int costPerKm;
	
	public modifiedVehicle(){
	}
	
	public modifiedVehicle(Vehicle vehicle){
		this.id = vehicle.id();
		this.capacity = vehicle.capacity();
		this.homeCity = vehicle.homeCity();
		this.costPerKm = vehicle.costPerKm();
	}
	
	public modifiedVehicle(int id, City newHome, int newCapacity, int newCostPerKm){
		this.id = id;
		this.capacity = newCapacity;
		this.homeCity = newHome;
		this.costPerKm = newCostPerKm;
	}
}
