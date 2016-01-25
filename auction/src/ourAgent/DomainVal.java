package ourAgent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
import ourAgent.Action.Type;

public class DomainVal {
	public List<MyVehicle> vehicles;
	public List<Action> actions;
	
	public void initMyVehiclesAndActionsList(List<Vehicle> allMyVehicles) {
		this.vehicles = new LinkedList<MyVehicle>();
        for (Vehicle v: allMyVehicles){
            vehicles.add(new MyVehicle(v, v.homeCity(), v.capacity()));
        }
		this.actions = new ArrayList<Action>(60);
	}

    public void initShuffleMyVehiclesAndActionsList(List<Vehicle> allMyVehicles) {
        this.vehicles = new LinkedList<MyVehicle>();
        Random random = new Random(System.currentTimeMillis());
        for (Vehicle v: allMyVehicles){
            Topology.City newHome = v.homeCity().randomNeighbor(random);
            for (int i = 0; i < 3; i++){
                newHome = newHome.randomNeighbor(random);
            }
            // new capacity from 0.8 to 1.2 of the original
            int newCapacity = (int) Math.ceil(v.capacity() * (0.8 + 0.4 * random.nextDouble()));
            vehicles.add(new MyVehicle(v, newHome, newCapacity));
        }
        this.actions = new ArrayList<Action>(60);
    }
	
	public void addTask(Task t){
		actions.add(new Action(Type.PICKUP, t));
		actions.add(new Action(Type.DELIVERY, t));
	}
	
	public void removeTask(Task t){
        for (Iterator<Action> it = actions.iterator(); it.hasNext(); ){
            if (it.next().task.id == t.id){
                it.remove();
            }
        }
	}
	
	public void clearTasks(){
		actions.clear();
	}
	
	public void initTasks(TaskSet ts){
		for(Task t : ts){
			actions.add(new Action(Type.PICKUP, t));
			actions.add(new Action(Type.DELIVERY, t));
		}
	}
}
