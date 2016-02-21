package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class PDP {

	private List<Vehicle> vehicles;
	private TaskSet tasks;
	private static double prop=0.35;
	private CentralizedPlan bestPlan;
	private int minCost = Integer.MAX_VALUE;
	
	public PDP(List<Vehicle> vehicles, TaskSet tasks) {
		super();
		this.vehicles = vehicles;
		this.tasks = tasks;
	}

	public CentralizedPlan SLSAlgorithm(){
		CentralizedPlan plan = SelectInitialSolution();
		int iterationNum = 3000;
		
		for(int i=0;i<iterationNum;i++){
			
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
		
		for(Vehicle exchangeVehicle: vehicles){
			if(oldPlan.getNextState() != null){
				for(Vehicle currentVehicle: vehicles){
					LinkedList<State> nextState = oldPlan.getNextState().get(exchangeVehicle);
					if(currentVehicle != exchangeVehicle && nextState != null && nextState.size() > 0){
						for(int i=0;i<oldPlan.getNextState().get(exchangeVehicle).size();i++){					
							State exchangeState = oldPlan.getNextState().get(exchangeVehicle).get(0);
							if(exchangeState != null && exchangeState.getCurrentTask().weight <= currentVehicle.capacity()){
								List<CentralizedPlan> planList= changingVehicle(oldPlan,exchangeVehicle,currentVehicle);
								for(CentralizedPlan plan:planList){
									if(!violateConstraint(plan)){
										//System.out.println("change Vehicle");
										//printPlan(plan);
										planSet.add(plan);
									}
								}
							}
						}
					}
				}
			}
		}
		
		//get number of nextState
		//for(Vehicle selectVehicle: vehicles){
			LinkedList<State> nextState = oldPlan.getNextState().get(selectVehicle);
			if(nextState != null){
				
				//Swap tIdxTask
				int length = nextState.size();
				if(length > 2){
					for(int tIdx=0;tIdx<length;tIdx++){
						if(nextState.get(tIdx).isPickup()){
							List<CentralizedPlan> planList= changingTaskOrder(oldPlan,selectVehicle,tIdx);
							for(CentralizedPlan plan:planList){
								if(!violateConstraint(plan)){
									//System.out.println("change order");
									//printPlan(plan);
									planSet.add(plan);
								}
							}
						}
					}
				}	
			}
		//}
		return planSet;
	}
	
	//If deliver before picking up,return true, otherwise return false  
	public boolean testDeliverBeforePickup(LinkedList<State> nextState,int i,int j){
		boolean ret = false;
		State swapTask1 = nextState.get(i);
		State swapTask2 = nextState.get(j);
		for(int m=i+1;m<j;m++){
			if(swapTask1.isPickup() && nextState.get(m).isPickup() == false){
				ret = true;
				break;
			}
			if(!swapTask2.isPickup() && nextState.get(m).isPickup() == true){
				ret = true;
				break;
			}
		}
		return ret;
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
		
		State startStateV1 = newPlan.getNextState().get(v1).get(0);//get vehicle1 startValue
		LinkedList<State> nextState1 = newPlan.getNextState().get(v1);//get vehicle1 nextStateList
		//slove  bug
		for(int i=nextState1.size()-1;i>=0;i--){
			if(nextState1.get(i).getCurrentTask() == startStateV1.getCurrentTask()){
				nextState1.remove(i);
			}
		}
		//remove v1 nextState
		//newPlan.removeCorrespondingDeliverState(v1,startStateV1);
		//nextStateList.remove(0);//remove firstState contain corresponding delivery State
		
		//add v2 nextState
		//State startStateV2 = newPlan.getNextState().get(v2).get(0);//get vehicle2 startValue 
		LinkedList<State> stateListV2 = newPlan.getNextState().get(v2);//get vehicle2 nextStateList
		
		if(stateListV2 == null){
			stateListV2 = new LinkedList<State>();
		}
		
		stateListV2.addFirst(startStateV1);//set firstState before nextStateList
		
		State deliveryState = new State(false,startStateV1.getCurrentTask());
		
		//handle corresponding delivery state
		List<CentralizedPlan> planSet = new ArrayList<CentralizedPlan>();
		for(int i=1;i<=stateListV2.size();i++){
			LinkedList<State> stateListCopy = (LinkedList<State>) stateListV2.clone();
			stateListCopy.add(i, deliveryState);
			
			CentralizedPlan copyPlan;
			try {
				
				copyPlan = (CentralizedPlan) newPlan.clone();
				copyPlan.getNextState().put(v2,stateListCopy);
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
		
		LinkedList<State> nextState = newPlan.getNextState().get(v1);
		State delState = nextState.get(tIdx);
		//System.out.println(delState);
		
		
		for(int i=nextState.size()-1;i>=0;i--){
			if(nextState.get(i).getCurrentTask() == delState.getCurrentTask()){
				nextState.remove(i);
			}
		}
		
		State deliveryState = new State(false,delState.getCurrentTask());
		//System.out.println(deliveryState);
		
		for(int insert1=0;insert1<=nextState.size();insert1++){
			
			for(int insert2=insert1+1;insert2<=nextState.size()+1;insert2++){
				
				CentralizedPlan copyPlan;
				try {
					
					copyPlan = (CentralizedPlan) newPlan.clone();
					LinkedList<State> copyList = (LinkedList<State>) nextState.clone();
					copyList.add(insert1, delState);
					copyList.add(insert2,deliveryState);
					copyPlan.getNextState().put(v1,copyList);
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
		int minCost = Integer.MAX_VALUE;
		for(CentralizedPlan plan:planSet){
			int tmpCost = caculatePlanCost(plan);
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
		
		HashMap<Vehicle,LinkedList<State>> stateMap = new HashMap<Vehicle, LinkedList<State>>();
		LinkedList<State> nextState = new LinkedList<State>();
		
		for(Task task:tasks){
			State pickupState = new State(true,task);
			State deliverState = new State(false, task);
			nextState.addLast(pickupState);
			nextState.addLast(deliverState);
		}
	
		System.out.println(vehicles.size());
		
//		for(Vehicle vehicle:vehicles){
//			stateMap.put(vehicle, new LinkedList<State>());
//		}
		
		stateMap.put(selectedVehicle,nextState);
		CentralizedPlan initialPlan = new CentralizedPlan();
		initialPlan.setNextState(stateMap);
		return initialPlan;
	}
	
	public int caculatePlanCost(CentralizedPlan plan) {
		int cost=0;
		HashMap<Vehicle, LinkedList<State>> vehicleToState = plan.getNextState();
		for(Map.Entry< Vehicle, LinkedList<State> > entry : vehicleToState.entrySet()){
			Vehicle v1 = entry.getKey();
			LinkedList<State> nextState = entry.getValue();
			if(nextState != null && nextState.size() > 0){
				Task startTask = nextState.get(0).getCurrentTask();
				cost += v1.homeCity().distanceTo(startTask.pickupCity);
				for(int i=0;i<nextState.size()-1;i++){
					State preState = nextState.get(i);
					State postState = nextState.get(i+1);
					if(preState.isPickup() == true && postState.isPickup() == true){
						cost += preState.getCurrentTask().pickupCity.distanceTo(postState.getCurrentTask().pickupCity) * v1.costPerKm();
					}else if(preState.isPickup() == false && postState.isPickup() == true){
						cost += preState.getCurrentTask().deliveryCity.distanceTo(postState.getCurrentTask().pickupCity) * v1.costPerKm();
					}else if(preState.isPickup() == true && postState.isPickup() == false){
						cost += preState.getCurrentTask().pickupCity.distanceTo(postState.getCurrentTask().deliveryCity) * v1.costPerKm();
					}else{
						cost += preState.getCurrentTask().deliveryCity.distanceTo(postState.getCurrentTask().deliveryCity) * v1.costPerKm();
					}
					
				}
			}
		}
		
		return cost;
	}
	
	public boolean violateConstraint(CentralizedPlan plan){
		//check capacity
		boolean isViolate = false;
		HashMap<Vehicle, LinkedList<State>> planMap = plan.getNextState();
		for(Map.Entry< Vehicle, LinkedList<State> > entry : planMap.entrySet()){
			Vehicle v1 = entry.getKey();
			int capacity = v1.capacity();
			LinkedList<State> nextState = entry.getValue();
			HashMap<Task,Integer> map = new HashMap<Task, Integer>();
			if(nextState != null){
				int tmpWeight = 0;
				for(int i=0;i<nextState.size();i++){
					
					Task task = nextState.get(i).getCurrentTask();
					
					if(map.containsKey(task)){
						int value = map.get(task);
						value++;
						if(value > 2){
							isViolate = true;
							break;
						}
						map.put(task, value);
					}
					else{
						map.put(task, 0);
					}
					
					if(nextState.get(i).isPickup()){
						tmpWeight += nextState.get(i).getCurrentTask().weight;
						for(int j=0;j<i;j++){
							if(nextState.get(j).getCurrentTask() == task){
								isViolate =  true;
								break;
							}
						}
					}
					else{
						tmpWeight -= nextState.get(i).getCurrentTask().weight;
						for(int j=i+1;j<nextState.size();j++){
							if(nextState.get(j).getCurrentTask() == task){
								isViolate =  true;
								break;
							}
						}
					}
					
					if(tmpWeight > capacity){
						isViolate =  true;
						break;
					}
					
				}
			}
			
			if(isViolate){
				break;
			}
		}
		return isViolate;
	}
	
	public void printPlan(CentralizedPlan plan){
		HashMap<Vehicle, LinkedList<State>> vehicleToState = plan.getNextState();
		for(Map.Entry< Vehicle, LinkedList<State> > entry : vehicleToState.entrySet()){
			System.out.println(entry.getKey().name() +" "+ entry.getValue());
		}
	}
	
	public CentralizedPlan getBestPlan(){
		return bestPlan;
	}
	
}
