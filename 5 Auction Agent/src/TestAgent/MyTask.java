package TestAgent;

import logist.task.Task;
import logist.topology.Topology.City;

public class MyTask {
	public City pickupCity;
	public City deliveryCity;
	public int id;
	public int weight;
	
	public MyTask(Task task){
		this.pickupCity = task.pickupCity;
		this.deliveryCity = task.deliveryCity;
		this.weight = task.weight;
		this.id = task.id;
	}
	
	public MyTask(City pc, City dc, int weight, int id){
		this.pickupCity = pc;
		this.deliveryCity = dc;
		this.weight = weight;
		this.id = id;
	}
}
