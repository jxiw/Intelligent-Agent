package template;

import logist.topology.Topology.City;

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
