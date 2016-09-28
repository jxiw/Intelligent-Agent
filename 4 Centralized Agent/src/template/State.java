package template;

import logist.task.Task;

public class State {

	private boolean isPickup;
	private Task currentTask;
	public boolean isPickup() {
		return isPickup;
	}
	public void setPickup(boolean isPickup) {
		this.isPickup = isPickup;
	}
	public Task getCurrentTask() {
		return currentTask;
	}
	public void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
	}
	public State(boolean isPickup, Task currentTask) {
		super();
		this.isPickup = isPickup;
		this.currentTask = currentTask;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((currentTask == null) ? 0 : currentTask.hashCode());
		result = prime * result + (isPickup ? 1231 : 1237);
		return result;
	}
	@Override
	public String toString() {
		return "State [isPickup=" + isPickup + ", currentTask=" + currentTask
				+ "]";
	}
}
