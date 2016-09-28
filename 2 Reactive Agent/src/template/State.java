package template;

import logist.topology.Topology.City;

//This class defines our states representation. 
public class State {
	/*State class has three property:
	  1. Whether there is a task;
	  2. Current city of the agent and, if a task exists, the task departure city;
	  3. Task destination city;
	  If property 1 is "false", property 2 and 3 are NULL.*/
	private boolean isTask;
	private City taskFrom;
	private City taskTo;
	
	public boolean isTask() {
		return isTask;
	}
	public void setTask(boolean isTask) {
		this.isTask = isTask;
	}
	public City getTaskFrom() {
		return taskFrom;
	}
	public void setTaskFrom(City taskFrom) {
		this.taskFrom = taskFrom;
	}
	public City getTaskTo() {
		return taskTo;
	}
	public void setTaskTo(City taskTo) {
		this.taskTo = taskTo;
	}
	public State(boolean isTask, City taskFrom, City taskTo) {
		super();
		this.isTask = isTask;
		this.taskFrom = taskFrom;
		this.taskTo = taskTo;
	}
	
	//We use HashMap to store agent states, so we override hashCode and equals methods for each state.
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isTask ? 1231 : 1237);
		result = prime * result
				+ ((taskFrom == null) ? 0 : taskFrom.hashCode());
		result = prime * result + ((taskTo == null) ? 0 : taskTo.hashCode());
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
		if (isTask != other.isTask)
			return false;
		if (taskFrom == null){
			if(other.taskFrom != null){
				return false;
			}
		}
		else if (!taskFrom.equals(other.taskFrom))
			return false;
		if (taskTo == null){
			if(other.taskTo != null){
				return false;
			}
		}
		else if (!taskTo.equals(other.taskTo))
			return false;
		return true;
	}
		
}
