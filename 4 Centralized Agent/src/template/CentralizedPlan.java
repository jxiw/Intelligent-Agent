package template;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import logist.simulation.Vehicle;
import logist.task.Task;

public class CentralizedPlan implements Cloneable {

	private HashMap<Vehicle, LinkedList<State>> nextState;

	public HashMap<Vehicle, LinkedList<State>> getNextState() {
		return nextState;
	}

	public void setNextState(HashMap<Vehicle, LinkedList<State>> nextState) {
		this.nextState = nextState;
	}

	public void removeCorrespondingDeliverState(Vehicle v1, State state) {
		Task task = state.getCurrentTask();
		LinkedList<State> stateList = nextState.get(v1);
		for (int i = 0; i < stateList.size(); i++) {
			State currentTask = stateList.get(i);
			if (currentTask.getCurrentTask() == task) {
				stateList.remove(i);
			}
		}
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		CentralizedPlan o = null;
		try {
			o = (CentralizedPlan) super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println(e.toString());
		}

		o.nextState = new HashMap<Vehicle, LinkedList<State>>();
		for (Iterator<Vehicle> keyIt = nextState.keySet().iterator(); keyIt.hasNext();) {
			Vehicle key = keyIt.next();
			o.nextState.put(key, (LinkedList<State>) nextState.get(key).clone());
		}

		return o;
	}

	@Override
	public String toString() {
		return "CentralizedPlan [nextState=" + nextState + "]";
	}
}
