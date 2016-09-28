package template;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.task.Task;

public class PDPlan {

	private MyVehicle biggestMyVehicle;
	private static double prop=1;
	private CentralizedPlan bestPlan;
	private CentralizedPlan searchPlan;
	private CentralizedPlan lastestPlan;
	private double minCost = Integer.MAX_VALUE;
	
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
		lastestPlan = searchPlan;
		List<CentralizedPlan> planSet = searchPlan.insertTask(task);
		searchPlan = localChoice(searchPlan,planSet);
		return searchPlan;
	}

	public void useLastestPlan(){
		searchPlan = lastestPlan;
	}
	
	public CentralizedPlan getBestPlan() {
		return bestPlan;
	}
	
	public CentralizedPlan getSearchPlan() {
		return searchPlan;
	}

	private CentralizedPlan localChoice(CentralizedPlan oldPlan,
			List<CentralizedPlan> planSet) {

		CentralizedPlan returnPlan = oldPlan;
		CentralizedPlan minCostPlan = null;
		
		double minCost = Integer.MAX_VALUE;
		for (CentralizedPlan plan : planSet) {
			double tmpCost = plan.cost();
			if (tmpCost < minCost) {
				minCostPlan = plan;
				minCost = tmpCost;
			}
		}
		
		this.bestPlan = minCostPlan;
		Random random = new Random();
		int num = random.nextInt(100);
		if (num < prop * 100) {
			returnPlan = minCostPlan;
			if (minCost < this.minCost) {
				this.minCost = minCost;
			}
		} else {
			returnPlan = planSet.get(random.nextInt(planSet.size()));
		}
		return returnPlan;
	}
	
}
