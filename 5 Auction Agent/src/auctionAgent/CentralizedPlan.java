package AuctionAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.task.Task;
import logist.task.TaskSet;
import AuctionAgent.Action.Type;

public class CentralizedPlan implements Cloneable{
	
	private HashMap<MyVehicle,LinkedList<Action>> vehicleActions = new HashMap<MyVehicle, LinkedList<Action>>();
	
	public HashMap<MyVehicle, LinkedList<Action>> getVehicleActions() {
		return vehicleActions;
	}

	public void setVehicleActions(HashMap<MyVehicle, LinkedList<Action>> vehicleActions) {
		this.vehicleActions = vehicleActions;
	}
	
	public void removeTask(Task t){
        for (MyVehicle v: vehicleActions.keySet()){
        	removeActionFormMyVehicle(v,new Action(Type.PICKUP, t));
        	removeActionFormMyVehicle(v,new Action(Type.DELIVERY, t));
        }
    }
	
	public void removeActionFormMyVehicle(MyVehicle v1,Action action){
		LinkedList<Action> actionList = vehicleActions.get(v1);
        actionList.remove(action);
        vehicleActions.put(v1, actionList);
    }
	
	public List<CentralizedPlan> insertTask(Task task){
		
		List<CentralizedPlan> planList = new ArrayList<CentralizedPlan>();
		for(Map.Entry< MyVehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			LinkedList<Action> actionList = entry.getValue();
			//Create Action about this Task
			Action pickupAction = new Action(Type.PICKUP,task);
			Action deliverAction = new Action(Type.DELIVERY, task);
			MyVehicle MyVehicle = entry.getKey();
			
			//Insert Action
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
					copyPlan.vehicleActions.put(MyVehicle,copyActionList);
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
		for (Map.Entry<MyVehicle, LinkedList<Action>> entry : vehicleActions
				.entrySet()) {
			MyVehicle MyVehicle = entry.getKey();
			LinkedList<Action> actionList = entry.getValue();
			if (actionList.size() > 0) {
				Task startTask = actionList.get(0).getCurrentTask();
				double des = MyVehicle.getInitCity()
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
				cost += des * MyVehicle.getCostPerKm();
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
        
        o.vehicleActions = new HashMap<MyVehicle, LinkedList<Action>>();
        for (Iterator<MyVehicle> keyIt = vehicleActions.keySet().iterator(); keyIt.hasNext();) {    
        	MyVehicle key = keyIt.next();    
            o.vehicleActions.put(key, (LinkedList<Action>) vehicleActions.get(key).clone());    
        }
   
        return o;    
	}
	 
	@Override
	public String toString() {
		String output="";
		for (Map.Entry<MyVehicle, LinkedList<Action>> entry : vehicleActions.entrySet()){
			output += entry.getKey().getVehicle().name()+" "+entry.getValue()+"\n"; 
		}
		return "CentralizedPlan:MyVehicleActions=" + output + "";
	}
	
	public boolean violateConstraint() {
		// check capacity
		boolean isViolate = false;
		for (Map.Entry<MyVehicle, LinkedList<Action>> entry : vehicleActions
				.entrySet()) {
			MyVehicle MyVehicle = entry.getKey();
			int capacity = MyVehicle.getCapacity();
			LinkedList<Action> actionList = entry.getValue();
			if (actionList.size() > 0) {
				int tmpWeight = 0;
				for (int i = 0; i < actionList.size(); i++) {
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
		for(Map.Entry< MyVehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			taskNum += entry.getValue().size();
		}
		return taskNum;
	}
	
	public void printPlan(){
		for(Map.Entry< MyVehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			System.out.println(entry.getKey().getVehicle().name() +" "+ entry.getValue());
		}
	}
	
	public void removeTask(TaskSet tasks){
		HashMap<Integer,Task> taskMap = new HashMap<Integer, Task>();
		for(Task task:tasks){
			taskMap.put(task.id, task);
		}
		
		for(Map.Entry< MyVehicle, LinkedList<Action> > entry : vehicleActions.entrySet()){
			LinkedList<Action> actionList= entry.getValue();
			for(int i=actionList.size()-1;i>0;i--){
				if(!taskMap.containsKey(actionList.get(i).currentTask.id)){
					actionList.remove(i);
				}
			}
		}
	}
	
}
