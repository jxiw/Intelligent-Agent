package template;

import logist.simulation.Vehicle;
import logist.topology.Topology.City;

public class MyVehicle {
	
	private Vehicle vehicle;
	private int capacity;
	private City initCity;
	private double costPerKm;
		
	public MyVehicle(Vehicle vehicle,City initCity, int capacity, double costPerKm){
		this.vehicle = vehicle;
		this.initCity = initCity;
		this.capacity = capacity;
		this.costPerKm = costPerKm;
	}
	
	public MyVehicle(Vehicle vehicle){
		this.vehicle = vehicle;
		this.initCity = vehicle.homeCity();
		this.capacity = vehicle.capacity();
		this.costPerKm = vehicle.costPerKm();
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public City getInitCity() {
		return initCity;
	}

	public void setInitCity(City initCity) {
		this.initCity = initCity;
	}

	public double getCostPerKm() {
		return costPerKm;
	}

	public void setCostPerKm(double costPerKm) {
		this.costPerKm = costPerKm;
	}
	
}
