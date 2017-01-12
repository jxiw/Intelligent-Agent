package AuctionAgent;

import logist.task.Task;

public class Action {

	public static enum Type {
		PICKUP, DELIVERY
	}

	Type type;
	Task currentTask;

	public Task getCurrentTask() {
		return currentTask;
	}

	public void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
	}

	public Action(Type type, Task currentTask) {
		super();
		this.type = type;
		this.currentTask = currentTask;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentTask == null) ? 0 : currentTask.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Action other = (Action) obj;
		if (currentTask == null) {
			if (other.currentTask != null)
				return false;
		} else if (!currentTask.equals(other.currentTask))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "type=" + type + ", currentTask=" + currentTask;
	}

}
