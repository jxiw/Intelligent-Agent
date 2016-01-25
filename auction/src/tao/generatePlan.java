package tao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import logist.task.Task;
import logist.topology.Topology.City;

public class generatePlan {
	TaskPlan bestPlan = new TaskPlan(); 
	double prob1 = 0.4, prob2 = 0.6;
	int max_round = 10000;
	int totalTaskNumber = 50;
	
	static Map<Integer, Task> map = new HashMap<Integer, Task>();
	static Map<Integer, City> tasks_map = new HashMap<Integer, City>();
	List<modifiedVehicle> vehicles;

	public generatePlan(List<modifiedVehicle> vehicles){
		this.vehicles = vehicles;
	}
	
	//=======================randomly assign parameters for vehicles===================
    public void RandomShuffle() {
        Random random = new Random(System.currentTimeMillis());
        List<modifiedVehicle> newVehicles = new ArrayList<modifiedVehicle>();
        for (modifiedVehicle v: this.vehicles){
            City newHome = v.homeCity.randomNeighbor(random);
            for (int i = 0; i < 3; i++){
                newHome = newHome.randomNeighbor(random);
            }
            int newCapacity = (int) Math.ceil(v.capacity * (0.8 + 0.4 * random.nextDouble()));
            newVehicles.add(new modifiedVehicle(v.id, newHome, newCapacity, v.costPerKm));
        }
        this.vehicles = newVehicles;
    }
	
	//========================main function to generate the best plan===================
	public TaskPlan SLS(TaskPlan updatedStates){
		bestPlan = updatedStates;

		TaskPlan A_old = new TaskPlan();
		TaskPlan A = updatedStates;
		A.cost = calculateCost(A);
		
		if(A == null) return null;
		
		for(int round = 0; round < max_round; round++){
			A_old = A;
			A = ChooseNeighbors(A_old);
			
			if(A.cost < bestPlan.cost)
				bestPlan = A;
		}
		return bestPlan;
	}
	
	
	// =======================calculate cost for each plan=======================
	public double calculateCost(TaskPlan plan){
		double cost = 0;
		List<List<Integer>> vehicle_list = plan.vehicleTasks;
		
		// for each vehicle list, sum up their costs
		for(int vehicle_index = 0; vehicle_index < vehicle_list.size(); vehicle_index++){
			modifiedVehicle vehicle = vehicles.get(vehicle_index);
			List<Integer> list = vehicle_list.get(vehicle_index);
			
			if(list.size() == 0) continue;

			// get the distance of vehicle location to first place
			cost += (vehicle.homeCity.distanceTo(tasks_map.get(list.get(0)))) * vehicle.costPerKm;
			
			// sum the distances from last city to current city
			for(int i  = 1; i < list.size(); i++){
				City last = tasks_map.get(list.get(i-1));
				cost += (last.distanceTo(tasks_map.get(list.get(i)))) * vehicle.costPerKm;
			}
		}
		return cost;
	}
	

	//========================== return the next plan according to prob=======================
	public TaskPlan LocalChoice(TaskPlan A_old, Queue<TaskPlan> N){
        TaskPlan A = N.poll();        
        
        double rand = Math.random();
		Random rd = new Random();

		if(N.size() <= 0 || rand > prob2)
			return A_old;
		if(rand < prob1)
			return A;
		else
			return (TaskPlan) N.toArray()[rd.nextInt(N.size())];
	}
	
    //======================Comparator anonymous class implementation=====================
    public Comparator<TaskPlan> costComparator = new Comparator<TaskPlan>(){
         
        @Override
        public int compare(TaskPlan c1, TaskPlan c2) {
            return (int) (c1.cost - c2.cost);
        }
    };
    
    
    //=============================Choose neighbors function============================

    public TaskPlan ChooseNeighbors(TaskPlan A_old){
        Queue<TaskPlan> N = new PriorityQueue<TaskPlan>(10000, costComparator);

        // pick a vehicle with non-empty tasks
        Random rand = new Random();
        int index = rand.nextInt(vehicles.size());
        while(A_old.vehicleTasks.get(index).size() == 0)
        	index = rand.nextInt(vehicles.size());
        
        // change vehicles
		Random rd = new Random();
        int t = A_old.vehicleTasks.get(index).get(rd.nextInt(A_old.vehicleTasks.get(index).size()));
        if(t >= totalTaskNumber)
        	t = t - totalTaskNumber;
		
        for(modifiedVehicle vehicle: vehicles){
        	if(vehicle.id == index) continue;
        	if(map.get(t).weight <= vehicle.capacity){
        		TaskPlan A = ChangeVehicle(A_old, index, vehicle.id, t);
        		N.add(A);
        	}
        }
        
        // change order
        int length = A_old.vehicleTasks.get(index).size();
        if(length >= 2){
        	for(int id1 = 0; id1 <= length-2; id1++){
        		for(int id2 = id1+1; id2 <= length-1; id2++){
        			if(id1 == id2) continue;
        			TaskPlan A = ChangeOrder(A_old, index, id1, id2);
        			if(A != null) {
        				N.add(A);
        			}
        		}
        	}
        }
        
		return LocalChoice(A_old, N); 
	}
	
	//=================================Change vehicles===================================
	public TaskPlan ChangeVehicle(TaskPlan A_old, int vid1, int vid2, int t){
		TaskPlan A = new TaskPlan();
    	for(int i = 0; i < A_old.vehicleTasks.size(); i++)
    		A.vehicleTasks.add(new ArrayList<Integer>(A_old.vehicleTasks.get(i)));

    	int pickup = t;
		int delivery = pickup + totalTaskNumber;
    	
		A.vehicleTasks.get(vid2).add(pickup);
		A.vehicleTasks.get(vid2).add(delivery);
		A.vehicleTasks.get(vid1).remove(A.vehicleTasks.get(vid1).indexOf(pickup));
		A.vehicleTasks.get(vid1).remove(A.vehicleTasks.get(vid1).indexOf(delivery));

		A.cost = calculateCost(A);
		return A;
	}
	
	//=================================Change tasks===================================
	public TaskPlan ChangeOrder(TaskPlan A_old, int index, int id1, int id2){
		TaskPlan A = new TaskPlan();
    	for(int i = 0; i < A_old.vehicleTasks.size(); i++)
    		A.vehicleTasks.add(new ArrayList<Integer>(A_old.vehicleTasks.get(i)));		
		
    	List<Integer> list = A.vehicleTasks.get(index);
		
    	// swap
    	int tmp = list.get(id1);
    	list.set(id1, list.get(id2));
    	list.set(id2, tmp);
    	
    	// test constraints
    	Set<Integer> set = new HashSet<Integer>();
    	int weight = 0;
    	
    	for(int i = 0; i <= Math.max(id1, id2); i++){
    		if(list.get(i) < totalTaskNumber){
    			if(set.contains(list.get(i) + totalTaskNumber)) return null;
    			if((weight + map.get(list.get(i)).weight) > vehicles.get(index).capacity) return null;
    			
    			set.add(list.get(i));
    			weight += map.get(list.get(i)).weight;
    		}
    		else{
    			if(! set.contains(list.get(i) - totalTaskNumber)) return null;
    			set.remove(list.get(i) - totalTaskNumber);
    			weight -= map.get(list.get(i) - totalTaskNumber).weight;
    		}
    	}
		A.cost = calculateCost(A);
		return A;
	}
	
	//======================print the tasks taken by each vehicle in the plan=====================
	public void printPlan(TaskPlan plan){
		System.out.println("Plan cost = " + plan.cost);
		for(int vehicle_index = 0; vehicle_index < plan.vehicleTasks.size(); vehicle_index++){
			modifiedVehicle vehicle = vehicles.get(vehicle_index);
			List<Integer> list = plan.vehicleTasks.get(vehicle_index);
			
			System.out.print("Vehicle " + vehicle_index + " has tasks: ");
			for(int i  = 0; i < list.size(); i++)
				System.out.print(list.get(i) + ", ");
			System.out.println();
		}
	}
}