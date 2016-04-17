package tmp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class PDP {

	private ArrayList<Vehicle> vehicles;
	private TaskSet taskSet;

	public CentralizedPlan SLSAlgorithm(){
		CentralizedPlan plan = SelectInitialSolution();
		int iterationNum = 10000;
		
		for(int i=0;i<iterationNum;i++){
			
			CentralizedPlan oldPlan = plan;
			ArrayList<CentralizedPlan> planSet = ChooseNeighbours(oldPlan);
			
			plan = localChoice(planSet);
			
			
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
		State startState = oldPlan.getVehicleToStartState().get(selectVehicle);
		LinkedList<State> nextList = oldPlan.getNextState().get(startState);
		int length = oldPlan.getNextState().get(startState).size() + 1;
		double accumulateValue = 0;
		if(length > 2){
			for(int tIdx1=1;tIdx1<length;tIdx1++){
				if(){
					
				}
				for(int tIdx2=tIdx1+1;tIdx2<=length;tIdx2++){
					if(nextList.get(tIdx1).isPickup() == true){
						
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
		
		State startStateV1 = newPlan.getVehicleToStartState().get(v1);//get vehicle1 startValue
		LinkedList<State> nextStateListV1 = newPlan.getNextState().get(startStateV1);//get vehicle2 nextStateList
		State nextStateV1 = nextStateListV1.get(0);//get vehicle2 thirdState
		nextStateListV1.remove(0);//remove firstState contain corresponding delivery State
	
		//remove v1 nextState
		newPlan.removeStateFromListByIntial(startStateV1);
		newPlan.getVehicleToStartState().put(v1, nextStateV1);//put secondState as First
		newPlan.getNextState().put(nextStateV1,nextStateListV1);//put nextState
		
		//add v2 nextState
		State startStateV2 = newPlan.getVehicleToStartState().get(v2);//get vehicle2 startValue 
		LinkedList<State> stateListV2 = newPlan.getNextState().get(startStateV2);//get vehicle2 nextStateList
		
		stateListV2.addFirst(startStateV2);//set firstState before nextStateList
		newPlan.getVehicleToStartState().put(v2,startStateV1);//put vehicle1 startState as vehicle2 startState
		newPlan.getNextState().put(startStateV1, stateListV2);//change nextStateList point startStateV1;
		
		State deliveryState = new State(false,startStateV1.getCurrentTask());
		
		//handle corresponding delivery state
		List<CentralizedPlan> planSet = new ArrayList<CentralizedPlan>();
		for(int i=0;i<=stateListV2.size();i++){
			LinkedList<State> stateListCopy = (LinkedList<State>) stateListV2.clone();
			stateListCopy.add(i, deliveryState);
			
			CentralizedPlan copyPlan;
			try {
				
				copyPlan = (CentralizedPlan) newPlan.clone();
				copyPlan.getNextState().put(startStateV1,stateListCopy);
				planSet.add(copyPlan);
				
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		return planSet;		
		
	}
	
	private List<CentralizedPlan> changingTaskOrder(CentralizedPlan oldPlan,Vehicle v1,int tIdx1,int tIdx2){
		CentralizedPlan newPlan=null;
		try {
			newPlan = (CentralizedPlan) oldPlan.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		State startState = newPlan.getVehicleToStartState().get(v1);
		
	}
	
	private CentralizedPlan localChoice(ArrayList<CentralizedPlan> planSet) {
		
		return null;
	}

	public CentralizedPlan SelectInitialSolution(){
	
		return null;
	}
	
}
