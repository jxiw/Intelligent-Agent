package TestAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

public class AuctionRiskSeeking implements AuctionBehavior {

	private Topology topology;
	private Agent agent;
	private Random random;

	private MyPlan myplan;
	private MyPlan opponentplan;

	private int numBids;
	private int numTakenTasks;
	private int totalWeight;

	private final long MIN_BID = 10;
	private final long DELTA_BID = 10;
	private final long RISK_SEEK_NUM = 10;
	private final double ETA = 0.1;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.topology = topology;
		this.agent = agent;
		this.numTakenTasks = 0;
		this.numBids = 0;
		this.totalWeight = 1;

		this.random = new Random();

		myplan = new MyPlan(agent.vehicles());
		opponentplan = new MyPlan(agent.vehicles());
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			myplan.addTask(previous);
			numTakenTasks++;
		} else {
			opponentplan.addTask(previous);
		}
		numBids++;
		this.totalWeight += previous.weight;
	}

	@Override
	public Long askPrice(Task task) {
		long cost = (long) (myplan.margCostEstim(task) * (1 - Math.pow(Math.E, -this.numBids * this.ETA))
				+ myplan.margAvgCostEstim(generateRandTasks()) * Math.pow(Math.E, -this.numBids * this.ETA));
		long opponentcost = (long) (opponentplan.margCostEstim(task) * (1 - Math.pow(Math.E, -this.numBids * this.ETA))
				+ opponentplan.margAvgCostEstim(generateRandTasks()) * Math.pow(Math.E, -this.numBids * this.ETA));

		System.out.println("real cost:" + cost);
		return Math.max(Math.max(cost + DELTA_BID, opponentcost), MIN_BID);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		return myplan.getPlans();
	}

	private List<MyTask> generateRandTasks() {
		List<MyTask> mytasks = new ArrayList<MyTask>();
		for (int i = 0; i < RISK_SEEK_NUM; i++) {
			int pc = random.nextInt(topology.cities().size());
			int dc = random.nextInt(topology.cities().size());
			int weight = this.totalWeight / (this.numBids + 1);
			int id = numTakenTasks + i + 1;
			mytasks.add(new MyTask(topology.cities().get(pc), topology.cities().get(dc), weight, id));
		}
		return mytasks;
	}

}
