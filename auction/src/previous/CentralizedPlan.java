package previous;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.simulation.Vehicle;
import logist.task.Task;
import previous.Action.Type;

public class CentralizedPlan implements Cloneable{
	
	private HashMap<Vehicle,LinkedList<Action>> vehicleActions = new HashMap<Vehicle, LinkedList<Action>>();
	
	public HashMap<Vehicle, LinkedList<Action>> getVehicleActions() {
		return vehicleActions;
	}

	public void setVehicleActions(HashMap<Vehicle, LinkedList<Action>> vehicleActions) {
		this.vehicleActions = vehicleActions;
	}
	
	public void removeTask(Task t){
        for (Vehicle v: vehicleActions.keySet()){
        	removeActionFormVehicle(v,new Action(Type.PICKUP, t));
        	removeActionFormVehicle(v,new Action(Type.DELIVERY, t));
        }
    }
	
	public void removeActionFormVehicle(Vehicle v1,Action action){
		LinkedList<Action> actionList = vehicleActions.get(v1);
        actionList.remove(action);
        vehicleActions.put(v1, actionList);
    }
	
	public List<CentralizedPlan> insertTask(Task task){
		
		List<CentralizedPlan> planList = new ArrayList<CentralizedPlan>();
		for(Map.Entry< Vehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			LinkedList<Action> actionList = entry.getValue();
			Action pickupAction = new Action(Type.PICKUP,task);
			Action deliverAction = new Action(Type.DELIVERY, task);
			Vehicle vehicle = entry.getKey();
			for(int pos1=0;pos1<=actionList.size();pos1++){				
				for(int pos2=pos1+1;pos2<=actionList.size()+1;pos2++){
					CentralizedPlan copyPlan = null;
					try {
						copyPlan = (CentralizedPlan) this.clone();
					} catch (CloneNotSupportedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					LinkedList<Action> copyActionList = (LinkedList<Action>) actionList.clone();
					copyActionList.add(pos1, pickupAction);
					copyActionList.add(pos2, deliverAction);
					copyPlan.vehicleActions.put(vehicle,copyActionList);
					if(!copyPlan.violateConstraint()){
						planList.add(copyPlan);
					}
				}
			}	
		}
		return planList;
	}
	
	public double cost() {
		double cost = 0;
		for (Map.Entry<Vehicle, LinkedList<Action>> entry : vehicleActions
				.entrySet()) {
			Vehicle vehicle = entry.getKey();
			LinkedList<Action> actionList = entry.getValue();
			if (actionList.size() > 0) {
				Task startTask = actionList.get(0).getCurrentTask();
				double des = vehicle.homeCity()
						.distanceTo(startTask.pickupCity);

				for (int i = 0; i < actionList.size() - 1; i++) {
					Action preAction = actionList.get(i);
					Action postAction = actionList.get(i + 1);

					if (preAction.type == Type.PICKUP
							&& postAction.type == Type.PICKUP) {
						des += preAction.currentTask.pickupCity
								.distanceTo(postAction.currentTask.pickupCity);
					} else if (preAction.type == Type.DELIVERY
							&& postAction.type == Type.PICKUP) {
						des += preAction.currentTask.deliveryCity
								.distanceTo(postAction.currentTask.pickupCity);
					} else if (preAction.type == Type.PICKUP
							&& postAction.type == Type.DELIVERY) {
						des += preAction.currentTask.pickupCity
								.distanceTo(postAction.currentTask.deliveryCity);
					} else {
						des += preAction.currentTask.deliveryCity
								.distanceTo(postAction.currentTask.deliveryCity);
					}

				}
				cost += des * vehicle.costPerKm();
			}
		}

		return cost;
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
        
        o.vehicleActions = new HashMap<Vehicle, LinkedList<Action>>();
        for (Iterator<Vehicle> keyIt = vehicleActions.keySet().iterator(); keyIt.hasNext();) {    
        	Vehicle key = keyIt.next();    
            o.vehicleActions.put(key, (LinkedList<Action>) vehicleActions.get(key).clone());    
        }
   
        return o;    
	}
	 
	@Override
	public String toString() {
		String output="";
		for (Map.Entry<Vehicle, LinkedList<Action>> entry : vehicleActions.entrySet()){
			output += entry.getKey().name()+" "+entry.getValue()+"\n"; 
		}
		return "CentralizedPlan:VehicleActions=" + output + "";
	}
	
	public boolean violateConstraint() {
		// check capacity
		boolean isViolate = false;
		for (Map.Entry<Vehicle, LinkedList<Action>> entry : vehicleActions
				.entrySet()) {
			Vehicle vehicle = entry.getKey();
			int capacity = vehicle.capacity();
			LinkedList<Action> actionList = entry.getValue();
			//HashMap<Task, Integer> map = new HashMap<Task, Integer>();
			if (actionList.size() > 0) {
				int tmpWeight = 0;
				for (int i = 0; i < actionList.size(); i++) {
//					Task task = actionList.get(i).currentTask;
//					if (map.containsKey(task)) {
//						int value = map.get(task);
//						value++;
//						if (value > 2) {
//							isViolate = true;
//							break;
//						}
//						map.put(task, value);
//					} else {
//						map.put(task, 0);
//					}
					if (actionList.get(i).type == Type.PICKUP) {
						tmpWeight += actionList.get(i).currentTask.weight;
					} else {
						tmpWeight -= actionList.get(i).currentTask.weight;
					}

					if (tmpWeight > capacity) {
						isViolate = true;
						break;
					}
				}
			}

			if (isViolate) {
				break;
			}
		}
		return isViolate;
	}
	
	public int getTaskNum(){
		int taskNum=0;
		for(Map.Entry< Vehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			taskNum += entry.getValue().size();
		}
		return taskNum;
	}
	
	public void printPlan(){
		for(Map.Entry< Vehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			System.out.println(entry.getKey().name() +" "+ entry.getValue());
		}
	}
	
}
