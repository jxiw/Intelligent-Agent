package previous;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import previous.Action.Type;

public class PDP {

	private List<Vehicle> vehicles;
	private TaskSet tasks;
	private static double prop=0.35;
	private CentralizedPlan bestPlan;
	private double minCost = Integer.MAX_VALUE;
	
	static int allowedTime = 300000;
	
	public PDP(List<Vehicle> vehicles, TaskSet tasks) {
		super();
		this.vehicles = vehicles;
		this.tasks = tasks;
	}

	public CentralizedPlan SLSAlgorithm(){
		
		long startTime = System.currentTimeMillis();
		
		CentralizedPlan plan = SelectInitialSolution();
		int iterationNum = 3000;
		
		for(int i=0;i<iterationNum;i++){
			
			// to prevent time out, after deadline the best solution will be found
            if (System.currentTimeMillis() - startTime > allowedTime) {
                return bestPlan;
            }
			CentralizedPlan oldPlan = plan;
			ArrayList<CentralizedPlan> planSet = ChooseNeighbours(oldPlan);
			plan = localChoice(oldPlan,planSet);	
		}
		return plan;
	} 
	
	public ArrayList<CentralizedPlan> ChooseNeighbours(CentralizedPlan oldPlan){
		ArrayList<CentralizedPlan> planSet = new ArrayList<CentralizedPlan>();
		
		Random random = new Random();
		int selectVehicleNum = random.nextInt(vehicles.size());
		Vehicle selectVehicle = vehicles.get(selectVehicleNum);
		
		for (Vehicle exchangeVehicle : vehicles) {
			for (Vehicle currentVehicle : vehicles) {
				LinkedList<Action> vehicleActions = oldPlan.getVehicleActions()
						.get(exchangeVehicle);
				if (currentVehicle != exchangeVehicle
						&& vehicleActions.size() > 0) {
					for (int i = 0; i < oldPlan.getVehicleActions()
							.get(exchangeVehicle).size(); i++) {
						Action exchangeAction = oldPlan.getVehicleActions()
								.get(exchangeVehicle).get(0);
						if (exchangeAction.currentTask.weight <= currentVehicle
								.capacity()) {
							List<CentralizedPlan> planList = changingVehicle(
									oldPlan, exchangeVehicle, currentVehicle);
							for (CentralizedPlan plan : planList) {
								if (!plan.violateConstraint()) {
									planSet.add(plan);
								}
							}
						}
					}
				}
			}
		}
		

		LinkedList<Action> vehicleAction = oldPlan.getVehicleActions().get(selectVehicle);

		//Swap tIdxTask
		int length = vehicleAction.size();
		if(length > 2){
			for(int tIdx=0;tIdx<length;tIdx++){
				if(vehicleAction.get(tIdx).type == Type.PICKUP){
					List<CentralizedPlan> planList= changingTaskOrder(oldPlan,selectVehicle,tIdx);
					for(CentralizedPlan plan:planList){
						if(!plan.violateConstraint()){
							planSet.add(plan);
						}
					}
				}
			}
		}
		return planSet;
	}
	
	public List<CentralizedPlan> changingVehicle(CentralizedPlan oldPlan,Vehicle v1,Vehicle v2){
		//change firstTask on v1 to v2
		
		//Copy new newPlan From oldPlan
		CentralizedPlan newPlan = null;
		try {
			newPlan = (CentralizedPlan) oldPlan.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Task startTask = newPlan.getVehicleActions().get(v1).get(0).currentTask;//get vehicle1 startValue
		LinkedList<Action> actionList = newPlan.getVehicleActions().get(v1);//get vehicle1 nextStateList
		newPlan.removeTask(startTask);
		
		Action pickupAction = new Action(Type.PICKUP, startTask);
		Action deliverAction = new Action(Type.DELIVERY, startTask);
		
		LinkedList<Action> stateListV2 = newPlan.getVehicleActions().get(v2);//get vehicle2 nextStateList
		stateListV2.addFirst(pickupAction);//set firstState before nextStateList
		
		//handle corresponding delivery state
		List<CentralizedPlan> planSet = new ArrayList<CentralizedPlan>();
		for(int i=1;i<=stateListV2.size();i++){
			
			LinkedList<Action> stateListCopy = (LinkedList<Action>) stateListV2.clone();
			stateListCopy.add(i, deliverAction);
			
			CentralizedPlan copyPlan;
			try {
				
				copyPlan = (CentralizedPlan) newPlan.clone();
				copyPlan.getVehicleActions().put(v2,stateListCopy);
				planSet.add(copyPlan);
				
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		return planSet;		
		
	}
	
	private List<CentralizedPlan> changingTaskOrder(CentralizedPlan oldPlan,Vehicle v1,int tIdx){
		
		List<CentralizedPlan> planSet = new ArrayList<CentralizedPlan>();
		
		CentralizedPlan newPlan=null;
		try {
			newPlan = (CentralizedPlan) oldPlan.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LinkedList<Action> actionList = newPlan.getVehicleActions().get(v1);
		Task task = actionList.get(tIdx).currentTask;
		newPlan.removeTask(task);

		Action pickupAction = new Action(Type.PICKUP, task);
		Action deliveryAction = new Action(Type.DELIVERY, task);
		
		for(int insert1=0;insert1<=actionList.size();insert1++){
			
			for(int insert2=insert1+1;insert2<=actionList.size()+1;insert2++){
				
				CentralizedPlan copyPlan;
				try {
					
					copyPlan = (CentralizedPlan) newPlan.clone();
					LinkedList<Action> copyList = (LinkedList<Action>) actionList.clone();
					copyList.add(insert1, pickupAction);
					copyList.add(insert2, deliveryAction);
					copyPlan.getVehicleActions().put(v1,copyList);
					planSet.add(copyPlan);
					
				} catch (CloneNotSupportedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return planSet;
	}
	
	private CentralizedPlan localChoice(CentralizedPlan oldPlan,ArrayList<CentralizedPlan> planSet) {
		
		CentralizedPlan returnPlan = oldPlan;
		CentralizedPlan minCostPlan = null;
		double minCost = Integer.MAX_VALUE;
		for(CentralizedPlan plan:planSet){
			double tmpCost = plan.cost();
			if(tmpCost<minCost){
				minCostPlan = plan;
				minCost = tmpCost;
			}
		}
		
		Random random = new Random();
		int num = random.nextInt(100);
		if(num < prop*100){
			returnPlan = minCostPlan;
			if(minCost < this.minCost){
				this.bestPlan = returnPlan;
				this.minCost = minCost;
			}
		}else if(num < 2*prop*100){
			returnPlan = oldPlan;
		}else{
			returnPlan = planSet.get(random.nextInt(planSet.size()));
		}
		return returnPlan;
	}

	public CentralizedPlan SelectInitialSolution(){

		int minCapacity = Integer.MIN_VALUE;
		Vehicle selectedVehicle = null;
		for(Vehicle vehicle:vehicles){
			if(vehicle.capacity() > minCapacity){
				minCapacity = vehicle.capacity();
				selectedVehicle = vehicle;
			}
		}
		
		HashMap<Vehicle,LinkedList<Action>> stateMap = new HashMap<Vehicle, LinkedList<Action>>();
		LinkedList<Action> actionList = new LinkedList<Action>();
		
		for(Task task:tasks){
			Action pickupState = new Action(Type.PICKUP,task);
			Action deliverState = new Action(Type.DELIVERY,task);
			actionList.addLast(pickupState);
			actionList.addLast(deliverState);
		}
	
		System.out.println(vehicles.size());
		
		for(Vehicle vehicle:vehicles){
			stateMap.put(vehicle, new LinkedList<Action>());
		}
		
		stateMap.put(selectedVehicle,actionList);
		CentralizedPlan initialPlan = new CentralizedPlan();
		initialPlan.setVehicleActions(stateMap);
		return initialPlan;
	}
	
	public CentralizedPlan getBestPlan(){
		return bestPlan;
	}
	
}
