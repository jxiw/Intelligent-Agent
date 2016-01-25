package test;

import java.util.ArrayList;
import java.util.List;

import logist.task.Task;
import logist.plan.Plan;
import logist.simulation.Vehicle;

public class MyPlan {
	
	private ArrayList<MyVehicle> vehicles;
	
	public MyPlan(List<Vehicle> vehicles){
		this.vehicles = new ArrayList<MyVehicle>();
		for(int i = 0; i < vehicles.size(); i++){
			Vehicle v = vehicles.get(i);
			this.vehicles.add(i, new MyVehicle(v.homeCity(), v.capacity(), v.costPerKm()));
		}
	}
	
	public int addTask(Task task){
		double c_min = vehicles.get(0).marginalCost(task);
		int i_min = 0, i = 0;
		for(MyVehicle v : vehicles) {
			double mc = v.marginalCost(task);
			if(mc < c_min) {c_min = mc; i_min = i;}
			i++;
		}
		vehicles.get(i_min).addTask(task);
		return i_min;
	}
	
	public double margCostEstim(Task task){
		double c_min = vehicles.get(0).marginalCost(task);
		for(MyVehicle v : vehicles) {
			double mc = v.marginalCost(task);
			if(mc < c_min) c_min = mc;
		}
		
		return c_min;
	}
	
	public double margAvgCostEstim(List<MyTask> mytasks){
		double c_min = vehicles.get(0).marginalCost(mytasks);
		for(MyVehicle v : vehicles) {
			double mc = v.marginalCost(mytasks);
			if(mc < c_min) c_min = mc;
		}
		
		if(mytasks.size()/vehicles.size() > 0){
			double mmc = 0;
			for(int i = 0; i < vehicles.size(); i++) {
				mmc += vehicles.get(i).marginalCost(mytasks.subList(
						i*mytasks.size()/vehicles.size(), (i+1)*mytasks.size()/vehicles.size()));
			}
			if(mmc < c_min) c_min = mmc;
		}
		
		return c_min/mytasks.size();
	}
	
	public List<Plan> getPlans(){
		List<Plan> plans = new ArrayList<Plan>();
		
		for (MyVehicle v : vehicles)
			plans.add(v.getPlan());

		return plans;
	}
}
