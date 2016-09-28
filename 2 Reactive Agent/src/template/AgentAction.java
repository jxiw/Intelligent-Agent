package template;

import logist.topology.Topology.City;
/*This class defines how the agent conducts an action. It has three properties:
 * 1. Departure city of the action;
 * 2. Destination city of the action;
 * 3. Whether the agent picks up a task in this action.
 * An agent is always on the move, and property 3 can be "true" or "false".
 */
public class AgentAction {

	private City departure;
	private City destination;
	private boolean isPickup;
	
	public City getDeparture() {
		return departure;
	}
	public void setDeparture(City departure) {
		this.departure = departure;
	}
	public City getDestination() {
		return destination;
	}
	public void setDestination(City destination) {
		this.destination = destination;
	}
	public AgentAction(City departure, City destination, boolean isPackup) {
		super();
		this.departure = departure;
		this.destination = destination;
		this.isPickup = isPackup;
	}
	
	public boolean isPickup() {
		return isPickup;
	}
	public void setPickup(boolean isPickup) {
		this.isPickup = isPickup;
	}
	
	//We use HashMap to store agent actions, so we override hashCode and equals methods for each action.
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((departure == null) ? 0 : departure.hashCode());
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result + (isPickup ? 1231 : 1237);
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
		AgentAction other = (AgentAction) obj;
		if (departure == null) {
			if (other.departure != null)
				return false;
		} else if (!departure.equals(other.departure))
			return false;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (isPickup != other.isPickup)
			return false;
		return true;
	}
	
}
