package ourAgent;

import logist.simulation.Vehicle;
import logist.topology.Topology;

/**
 * Simple vehicle implementation to allow change of the home city and capacity
 */
public class MyVehicle{
    public Vehicle vehicle;
    public Topology.City homeCity;
    public int capacity;

    public MyVehicle(Vehicle vehicle, Topology.City homeCity, int capacity){
        this.vehicle = vehicle;
        this.homeCity = homeCity;
        this.capacity = capacity;
    }

    public Topology.City homeCity(){
        return homeCity;
    }

    public int capacity(){
        return capacity;
    }

    public int costPerKm(){
        return vehicle.costPerKm();
    }

    public int id(){
        return vehicle.id();
    }
}