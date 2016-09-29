package TestAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.task.Task;
import logist.topology.Topology.City;
import logist.plan.Plan;

public class MyVehicle {
	private int capacity;
	private City initCity;
	private double costPerKm;
	
	public ArrayList<MyAction> actions;
	private HashMap<Integer, Task> tasks;
	
	public MyVehicle(City initCity, int capacity, double costPerKm){
		this.initCity = initCity;
		this.capacity = capacity;
		this.costPerKm = costPerKm;
		
		actions = new ArrayList<MyAction>();
		tasks = new HashMap<Integer, Task>();
	}
	
	public double marginalCost(List<MyTask> mytasks){
		double cost0 = cost();
		
		int[] i_pickup = new int[mytasks.size()];
		int[] i_deliver = new int[mytasks.size()];
		
		int i = 0;
		for(MyTask mytask : mytasks){
			MyAction pickup = new MyAction(true, mytask.pickupCity, mytask.weight,mytask.id);
			i_pickup[i] = addAction(pickup, 0);
		
			MyAction deliver = new MyAction(false, mytask.deliveryCity, mytask.weight, mytask.id);
			i_deliver[i] = addAction(deliver, i_pickup[i] + 1);
			i++;
		}
		
		double cost = cost();
		
		for(i = mytasks.size() - 1; i >= 0; i--){
			actions.remove(i_deliver[i]);
			actions.remove(i_pickup[i]);
		}
		
		return cost - cost0;
	}
	
	public double marginalCost(Task task){
		double cost0 = cost();
		
		MyAction pickup = new MyAction(true, task.pickupCity, task.weight, task.id);
		int i_pickup = addAction(pickup, 0);
		
		MyAction deliver = new MyAction(false, task.deliveryCity, task.weight, task.id);
		int i_deliver = addAction(deliver, i_pickup + 1);
		
		double cost = cost();
		
		actions.remove(i_deliver);
		actions.remove(i_pickup);
		
		return cost - cost0;
	}
	
	public void addTask(Task task){
		MyAction pickup = new MyAction(true, task.pickupCity, task.weight, task.id);
		int i_pickup = addAction(pickup, 0);
		
		MyAction deliver = new MyAction(false, task.deliveryCity, task.weight, task.id);
		addAction(deliver, i_pickup + 1);
		
		tasks.put(task.id, task);
	}
	
	private int addAction(MyAction action, int i_min0){
		int i_min = i_min0;
		double c_min = Double.MAX_VALUE;
		for(int i = i_min0; i <= actions.size(); i++){
			actions.add(i, action);
			if(isConsistent() && cost() < c_min){
				i_min = i;
				c_min = cost();
			}
			actions.remove(i);
		}
		actions.add(i_min, action);
		
		return i_min;
	}
	
	private boolean isConsistent(){
		int c = 0;
		for(MyAction a: actions){
			c += a.getWeight();
			if(c > capacity) return false;
		}
		return true;
	}
	
	private double cost(){
		double cost = 0;
		City c = initCity;
		for(MyAction a: actions){
			cost += c.distanceTo(a.getCity()) * costPerKm;
			c = a.getCity();
		}
		return cost;
	}
	
	public Plan getPlan(){
		City current = initCity;
		Plan plan = new Plan(current);
		
		for(MyAction a: actions){
			for (City city : current.pathTo(a.getCity()))
				plan.appendMove(city);
			
			if(a.isPickup()) plan.appendPickup(tasks.get(a.id));
			else plan.appendDelivery(tasks.get(a.id));
			
			current = a.getCity();
		}
		
		return plan;
	}
}
