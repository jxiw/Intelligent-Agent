package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplateRand implements ReactiveBehavior {

	private Random random;
	private double pPickup;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			City destCity = currentCity.randomNeighbor(random);
			action = new Move(destCity);
			System.out.println(vehicle.name() + " has no available task. It just moves from " + vehicle.getCurrentCity()
					+ " to " + destCity + ".");
		} else {
			action = new Pickup(availableTask);
			System.out.println(vehicle.name() + " picks up a task from " + availableTask.pickupCity + " to "
					+ availableTask.deliveryCity + ".");
		}
		return action;
	}
}
