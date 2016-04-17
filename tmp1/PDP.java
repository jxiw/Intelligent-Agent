package tmp1;

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
	
	public PDP(List<Vehicle> vehicles, TaskSet tasks) {
		super();
		this.vehicles = vehicles;
		this.tasks = tasks;
	}

	public CentralizedPlan SLSAlgorithm(){
		CentralizedPlan plan = SelectInitialSolution();
		int iterationNum = 10000;
		
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
		int selectVehicleNum = random.nextInt();
		Vehicle selectVehicle = vehicles.get(selectVehicleNum);
		
		for(Vehicle currentVehicle: vehicles){
			if(currentVehicle != selectVehicle){
				State exchangeState = oldPlan.getNextState().get(selectVehicle).get(0);
				if(exchangeState != null && exchangeState.getCurrentTask().weight <= currentVehicle.capacity()){
					planSet.addAll(changingVehicle(oldPlan,selectVehicle,currentVehicle));
				}
			}
		}
		
		//get number of nextState
		LinkedList<State> nextState = oldPlan.getNextState().get(selectVehicle);
		if(nextState != null){
			
			// Swap tIdx1Task and tIdx2Task
			int length = nextState.size();
			int accumulateValueIdx1 = 0;
			if(length > 2){
				for(int tIdx1=0;tIdx1<length;tIdx1++){
					
					int accumulateValueIdx2 = accumulateValueIdx1;
					for(int tIdx2=tIdx1+1;tIdx2<=length;tIdx2++){
						
						accumulateValueIdx2 += nextState.get(tIdx2-1).getCurrentTask().weight;	
						if(!testDeliverBeforePickup(nextState, tIdx1, tIdx2)){
							
							if(nextState.get(tIdx2).isPickup() && accumulateValueIdx1+nextState.get(tIdx2).getCurrentTask().weight > selectVehicle.capacity()){
								continue;
							}
							
							if(nextState.get(tIdx1).isPickup() && accumulateValueIdx2+nextState.get(tIdx1).getCurrentTask().weight > selectVehicle.capacity()){
								continue;
							}
							
							planSet.add(changingTaskOrder(oldPlan,selectVehicle,tIdx1,tIdx2));
						}
					}
					
					Task tIdx1Task = nextState.get(tIdx1).getCurrentTask();
					if(nextState.get(tIdx1).isPickup()){
						accumulateValueIdx1 += tIdx1Task.weight;
					}else{
						accumulateValueIdx1 -= tIdx1Task.weight;
					}
					
				}
			}	
		}
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
		LinkedList<State> nextStateListV1 = newPlan.getNextState().get(v1);//get vehicle1 nextStateList
		nextStateListV1.remove(0);//remove firstState contain corresponding delivery State
	
		//remove v1 nextState
		newPlan.removeStateFromListByIntial(v1,startStateV1);
		
		//add v2 nextState
		//State startStateV2 = newPlan.getNextState().get(v2).get(0);//get vehicle2 startValue 
		LinkedList<State> stateListV2 = newPlan.getNextState().get(v2);//get vehicle2 nextStateList
		
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
	
	private CentralizedPlan changingTaskOrder(CentralizedPlan oldPlan,Vehicle v1,int tIdx1,int tIdx2){
		CentralizedPlan newPlan=null;
		try {
			newPlan = (CentralizedPlan) oldPlan.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LinkedList<State> nextState = newPlan.getNextState().get(v1);
		State swapState1 = nextState.get(tIdx1);
		State swapState2 = nextState.get(tIdx2);
		nextState.remove(tIdx2);
		nextState.remove(tIdx1);
		
		nextState.add(tIdx1, swapState2);
		nextState.add(tIdx2, swapState1);
		
		return newPlan;
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
		if(num < 45){
			returnPlan = minCostPlan;
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
			if(nextState != null){
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
			if(nextState != null){
				int tmpWeight = 0;
				for(int i=0;i<nextState.size();i++){
					if(nextState.get(i).isPickup()){
						tmpWeight += nextState.get(i).getCurrentTask().weight;
					}else{
						tmpWeight -= nextState.get(i).getCurrentTask().weight;
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
	
}
