package tmp;

import java.util.HashMap;
import java.util.LinkedList;

import logist.simulation.Vehicle;
import logist.task.Task;
import template.State;

public class CentralizedPlan implements Cloneable{
	
	private HashMap<Vehicle,State> vehicleToStartState;
	private HashMap<State,LinkedList<State>> nextState;
	private HashMap<State,Vehicle> taskToVehicle;
	//private HashMap<State,Integer> time;
	
	public HashMap<Vehicle, State> getVehicleToStartState() {
		return vehicleToStartState;
	}
	public void setVehicleToStartState(HashMap<Vehicle, State> vehicleToStartState) {
		this.vehicleToStartState = vehicleToStartState;
	}
	public HashMap<State, LinkedList<State>> getNextState() {
		return nextState;
	}
	public void setNextState(HashMap<State, LinkedList<State>> nextState) {
		this.nextState = nextState;
	}
	public HashMap<State, Vehicle> getTaskToVehicle() {
		return taskToVehicle;
	}
	public void setTaskToVehicle(HashMap<State, Vehicle> taskToVehicle) {
		this.taskToVehicle = taskToVehicle;
	}
	
//	public HashMap<State, Integer> getTime() {
//		return time;
//	}
//	public void setTime(HashMap<State, Integer> time) {
//		this.time = time;
//	}
	
	public void removeStateFromListByIntial(State state){
		Task task = state.getCurrentTask();
		LinkedList<State> stateList = nextState.get(state);
		for(int i=0;i<stateList.size();i++){
			State currentTask = stateList.get(i);
			if(currentTask.getCurrentTask() == task){
				stateList.remove(i);
			}
		}
	}

	 @Override
	protected Object clone() throws CloneNotSupportedException {
		CentralizedPlan o=null;    
        try    
         {    
             o=(CentralizedPlan)super.clone();    
         }    
        catch(CloneNotSupportedException e)    
         {    
             System.out.println(e.toString());    
         }    
         o.vehicleToStartState= (HashMap<Vehicle, State>) vehicleToStartState.clone();
         o.taskToVehicle = (HashMap<State, Vehicle>) taskToVehicle.clone();
         o.vehicleToStartState = (HashMap<Vehicle, State>) vehicleToStartState.clone();
         return o;    
	 } 
	
}
