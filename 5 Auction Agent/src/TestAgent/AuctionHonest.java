package test;

import java.util.List;

import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

public class AuctionHonest implements AuctionBehavior {
	
	private Agent agent;
	private MyPlan myplan;
	
	private final long DELTA_BID = 10;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.agent = agent;
		myplan = new MyPlan(agent.vehicles());
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			myplan.addTask(previous);
		}
	}
	
	@Override
	public Long askPrice(Task task) {
		long cost = (long) myplan.margCostEstim(task);
		return cost + DELTA_BID;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return myplan.getPlans();
	}
}
