package template;

import java.util.Comparator;

import logist.task.Task;
import logist.topology.Topology.City;

public class StateCompartor implements Comparator<State> {

	private Task[] tasksArray;
	private double costPerKm;

	public StateCompartor(Task[] tasksArray, double costPerKm) {
		this.tasksArray = tasksArray;
		this.costPerKm = costPerKm;
	}

	@Override
	public int compare(State o1, State o2) {
		// TODO Auto-generated method stub

		// heuristic
		String sign1 = o1.getTaskSign();
		String sign2 = o2.getTaskSign();

		double leastRoute1 = 0;
		double leastRoute2 = 0;

		for (int i = 0; i < sign1.length(); i++) {

			if (sign1.charAt(i) == '0') {
				City pickUpCity = o1.getTask().pickupCity;
				double routeLength = o1.getLocation().distanceTo(pickUpCity) + tasksArray[i].pathLength();
				if (leastRoute1 < routeLength) {
					leastRoute1 = routeLength;
				}
			} else if (sign1.charAt(i) == '1') {
				City deliveryCity = o1.getTask().deliveryCity;
				double routeLength = o1.getLocation().distanceTo(deliveryCity);
				if (leastRoute1 < routeLength) {
					leastRoute1 = routeLength;
				}
			}

			if (sign2.charAt(i) == '0') {
				City pickUpCity = o2.getTask().pickupCity;
				double routeLength = o2.getLocation().distanceTo(pickUpCity) + tasksArray[i].pathLength();
				if (leastRoute2 < routeLength) {
					leastRoute2 = routeLength;
				}
			} else if (sign2.charAt(i) == '1') {
				City deliveryCity = o2.getTask().deliveryCity;
				double routeLength = o2.getLocation().distanceTo(deliveryCity);
				if (leastRoute2 < routeLength) {
					leastRoute2 = routeLength;
				}
			}
		}
		return Double.compare(o1.getCost() + leastRoute1 * costPerKm, o2.getCost() + leastRoute2 * costPerKm);
	}

}
