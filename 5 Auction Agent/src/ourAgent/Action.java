package ourAgent;

import logist.task.Task;
import logist.topology.Topology.City;

public class Action {
	
	public static enum Type{
        PICKUP,
        DELIVERY
    }
	
	Type type;
	Task task;

	public Action(Type type, Task task){
		this.type = type;
		this.task = task;
	}
	
	public City getCity(){
		if(type == Type.PICKUP) return task.pickupCity;
		else return task.deliveryCity;
	}

    public Task getTask() {
        return task;
    }

    public Action oppositeAction(){
        return new Action(type == Type.DELIVERY ? Type.PICKUP : Type.DELIVERY, task);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Action action = (Action) o;

        if (!task.equals(action.task)) return false;
        if (type != action.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type == Type.DELIVERY ? 1 : 0;
        result = 31 * result + task.hashCode();
        return result;
    }
}