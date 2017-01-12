package template;

import logist.task.Task;
import logist.topology.Topology.City;

public class State {

	private State parentSate;
	private Task task;
	private City location;
	private double cost;
	private String taskSign;
	private int accumulateWeight;
	private boolean isPickup;

	// private int taskSign;
	// private List<State> nextStates;

	// public State(City location, double cost, int taskSign, List<State>
	// nextStates) {
	// super();
	// this.location = location;
	// this.cost = cost;
	// this.taskSign = taskSign;
	// this.nextStates = nextStates;
	// }

	public City getLocation() {
		return location;
	}

	public State(State parentSate, Task task, City location, double cost, String taskSign, int accumulateWeight,
			boolean isPickup) {
		super();
		this.parentSate = parentSate;
		this.task = task;
		this.location = location;
		this.cost = cost;
		this.taskSign = taskSign;
		this.accumulateWeight = accumulateWeight;
		this.isPickup = isPickup;
	}

	public void setLocation(City location) {
		this.location = location;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public String getTaskSign() {
		return taskSign;
	}

	public void setTaskSign(String taskSign) {
		this.taskSign = taskSign;
	}

	public State getParentSate() {
		return parentSate;
	}

	public void setParentSate(State parentSate) {
		this.parentSate = parentSate;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public int getAccumulateWeight() {
		return accumulateWeight;
	}

	public void setAccumulateWeight(int accumulateWeight) {
		this.accumulateWeight = accumulateWeight;
	}

	public boolean isPickup() {
		return isPickup;
	}

	public void setPickup(boolean isPickup) {
		this.isPickup = isPickup;
	}

	@Override
	public String toString() {
		return taskSign + " " + cost + "";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + accumulateWeight;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (isPickup ? 1231 : 1237);
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((taskSign == null) ? 0 : taskSign.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (accumulateWeight != other.accumulateWeight)
			return false;
		if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
			return false;
		if (isPickup != other.isPickup)
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
		if (taskSign == null) {
			if (other.taskSign != null)
				return false;
		} else if (!taskSign.equals(other.taskSign))
			return false;
		return true;
	}
}
