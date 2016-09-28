package auctionAgent;

import java.util.LinkedList;
import java.util.List;

import logist.task.Task;
import logist.task.TaskSet;

public class PDPlan {

	private MyVehicle biggestMyVehicle;
	//private static double prop=1;
	private CentralizedPlan bestPlan;
	private CentralizedPlan searchPlan;
	private CentralizedPlan newPlan;
	
	public PDPlan(List<MyVehicle> MyVehicles){
		int capacity = Integer.MIN_VALUE;
		this.searchPlan = new CentralizedPlan();
		for(MyVehicle MyVehicle:MyVehicles){
			LinkedList<Action> actionList = new LinkedList<Action>();
			searchPlan.getVehicleActions().put(MyVehicle, actionList);
			if(capacity < MyVehicle.getCapacity()){
				capacity = MyVehicle.getCapacity();
				biggestMyVehicle = MyVehicle;
			}
		}
	}
	
	public MyVehicle getBiggestVehicle() {
		return biggestMyVehicle;
	}

	public CentralizedPlan solveWithNewTask(Task task){
		try {
			
			newPlan = (CentralizedPlan) searchPlan.clone();
			List<CentralizedPlan> planSet = newPlan.insertTask(task);
			newPlan = localChoice(searchPlan,planSet);
		
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newPlan;
	}

	public void solveWithTaskSet(TaskSet tasks){
		for(Task task:tasks){
			List<CentralizedPlan> planSet = searchPlan.insertTask(task);
			searchPlan = localChoice(searchPlan,planSet);
		}
	}
	
	public void updatePlan(){
		searchPlan = newPlan;
	}
	
	public CentralizedPlan getBestPlan() {
		return bestPlan;
	}
	
	public CentralizedPlan getSearchPlan() {
		return searchPlan;
	}

	private CentralizedPlan localChoice(CentralizedPlan oldPlan,
			List<CentralizedPlan> planSet) {

		CentralizedPlan minCostPlan = oldPlan;
		
		double minCost = Integer.MAX_VALUE;
		for (CentralizedPlan plan : planSet) {
			double tmpCost = plan.cost();
			if (tmpCost < minCost) {
				minCostPlan = plan;
				minCost = tmpCost;
			}
		}
		
		this.bestPlan = minCostPlan;
		return minCostPlan;
	}
	
}
