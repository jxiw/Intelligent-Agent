package heuristic;

import java.util.Comparator;

import logist.task.Task;
import template.State;

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

		double heurstic1 = 0;
		double heurstic2 = 0;

		for (int i = 0; i < sign1.length(); i++) {

			if (sign1.charAt(i) == '0') {
				heurstic1 += tasksArray[i].pathLength();
			}

			if (sign2.charAt(i) == '0') {
				heurstic2 += tasksArray[i].pathLength();
			}
		}
		return Double.compare(o1.getCost() + heurstic1 * costPerKm, o2.getCost() + heurstic2 * costPerKm);
	}

}
