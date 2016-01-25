package ourAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.Measures;
import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;
import ourAgent.Action.Type;

public class A {
    Map<MyVehicle, List<Action>> vehicleActions;
    DomainVal domainVal;
	
	// Empty default constructor
	public A(DomainVal domainVal){
        this.domainVal = domainVal;
        vehicleActions = new HashMap<MyVehicle, List<Action>>(domainVal.vehicles.size());
        for (MyVehicle v:domainVal.vehicles){
            List<Action> actionList = new ArrayList<Action>();
            vehicleActions.put(v, actionList);
        }
	}
	
	// Copy constructor
	public A(A a){
        // use the sam domain
        this.domainVal = a.domainVal;

        vehicleActions = new HashMap<MyVehicle, List<Action>>(domainVal.vehicles.size());
        for (MyVehicle v:domainVal.vehicles){
            List<Action> actionList = new ArrayList<Action>();
            actionList.addAll(a.vehicleActions.get(v));
            vehicleActions.put(v, actionList);
        }
	}

    public void appendActionForMyVehicle(Action a, MyVehicle v){
        List<Action> actionList = vehicleActions.get(v);
        actionList.add(a);
        vehicleActions.put(v, actionList);
    }

    public boolean appendTaskToBiggestMyVehicle(Task t){
        MyVehicle biggestMyVehicle = domainVal.vehicles.get(0);
        for (MyVehicle v:domainVal.vehicles){
            if (biggestMyVehicle.capacity() < v.capacity()){
                biggestMyVehicle = v;
            }
        }
        if (biggestMyVehicle.capacity() < t.weight){
            return false;
        }
        else{
            appendActionForMyVehicle(new Action(Type.PICKUP, t), biggestMyVehicle);
            appendActionForMyVehicle(new Action(Type.DELIVERY, t), biggestMyVehicle);
            return true;
        }
    }

    public void removeTask(Task t){
        for (MyVehicle v: vehicleActions.keySet()){
            removeActionForMyVehicle(new Action(Type.PICKUP, t), v);
            removeActionForMyVehicle(new Action(Type.DELIVERY, t), v);
        }
    }

    public void replaceActionForMyVehicle(Action a, MyVehicle v, int index){
        List<Action> actionList = vehicleActions.get(v);
        actionList.set(index, a);
        vehicleActions.put(v, actionList);
    }

    public void removeActionForMyVehicle(Action a, MyVehicle v){
        List<Action> actionList = vehicleActions.get(v);
        actionList.remove(a);
        vehicleActions.put(v, actionList);
    }
	
	public double cost(){
		double totalCost = 0;
		for(MyVehicle v : vehicleActions.keySet()){
            City c = v.homeCity();
            long vehicleDistanceSum = 0;
			for (Action action : vehicleActions.get(v)){
				vehicleDistanceSum += c.distanceUnitsTo(action.getCity());
                c = action.getCity();
            }
			double vehicleCost = Measures.unitsToKM(vehicleDistanceSum * v.costPerKm());
			totalCost += vehicleCost;
		}
		
		return totalCost;
	}
	
	public Plan getPlanForMyVehicle(int index){
		MyVehicle v = domainVal.vehicles.get(index);
		City currentCity = v.homeCity();
		Plan plan = new Plan(currentCity);
		
		for(Action a: vehicleActions.get(v)){
			City destination = a.getCity();
			
			for(City c : currentCity.pathTo(destination)){
				plan.appendMove(c);
			}
			
			if(a.type == Type.PICKUP) plan.appendPickup(a.task);
			else plan.appendDelivery(a.task);
			
			currentCity = destination;
		}
		
		return plan;
	}
}