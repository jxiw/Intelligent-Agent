package AuctionAgent;

//the list of imports
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import AuctionAgent.Action.Type;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AgentTeamWL implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;

	private PDPlan myPlan;
	private PDPlan oppPlan;

	private double myCost;
	private double myNewCost;
	private double oppCost;
	private double oppNewCost;

	private List<MyVehicle> myVehicles;
	private List<MyVehicle> oppVehicles;

	private List<City> allCities;
	private List<City> myVehicleCities;

	private double initialBidRatio = 0.5;
	private double initialNum = 4;

	private double oppRatio = 0.85;
	private double myMarginBidRatio = 0.8;

	final static double oppRatioUpper = 0.9;
	final static double oppRatioLower = 0.8;

	final static double myRatioUpper = 0.85;
	final static double myRatioLower = 0.75;

	private double bidOppMin = Double.MAX_VALUE;
	private int round = 0;
	private long allowedTime = 40000L;

	private double bidAboutPositionMin = 0.9;
	private double bidAboutPositionMax = 1.1;
	double[][] propobality;
	// private double ratio=0.85;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;

		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		allowedTime = ls.get(LogistSettings.TimeoutKey.PLAN);

		List<Vehicle> vehicles = agent.vehicles();
		myVehicles = new ArrayList<MyVehicle>(vehicles.size());
		oppVehicles = new ArrayList<MyVehicle>(vehicles.size());

		allCities = topology.cities();
		myVehicleCities = new ArrayList<Topology.City>();

		for (Vehicle vehicle : vehicles) {
			MyVehicle myVehicle = new MyVehicle(vehicle);
			myVehicles.add(myVehicle);
			myVehicleCities.add(vehicle.homeCity());
		}

		for (Vehicle vehicle : vehicles) {
			Random random = new Random();
			City randomCity;
			do {
				int randomNum = random.nextInt(allCities.size());
				randomCity = allCities.get(randomNum);
			} while (myVehicleCities.contains(randomCity));

			MyVehicle oppVehicle = new MyVehicle(null, randomCity, vehicle.capacity(), vehicle.costPerKm());
			oppVehicles.add(oppVehicle);
		}

		this.myPlan = new PDPlan(myVehicles);
		this.oppPlan = new PDPlan(oppVehicles);

		propobality = new double[topology.size()][topology.size()];
		initPro();
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		double myBid = bids[agent.id()];
		double oppBid = bids[1 - agent.id()];
		if (oppBid < bidOppMin) {
			bidOppMin = oppBid;
		}

		System.out.println(round);
		if (winner == agent.id()) {
			myCost = myNewCost;
			myPlan.updatePlan();

			myMarginBidRatio = Math.min(myRatioLower, myMarginBidRatio + 0.01);
			oppRatio = Math.min(oppRatioLower, oppRatio + 0.01);

		} else {
			oppCost = oppNewCost;
			oppPlan.updatePlan();

			myMarginBidRatio = Math.max(myRatioUpper, myMarginBidRatio - 0.01);
			oppRatio = Math.max(oppRatioUpper, oppRatio - 0.01);
		}

		if (round == 1) {
			City predictCity = null;
			double costDiff = Double.MAX_VALUE;
			for (City city : allCities) {
				if (!myVehicleCities.contains(city)) {
					double diff = Math.abs((city.distanceTo(previous.pickupCity)
							+ previous.pickupCity.distanceTo(previous.deliveryCity)) * oppVehicles.get(0).getCostPerKm()
							- oppBid);
					if (diff < costDiff) {
						costDiff = diff;
						predictCity = city;
					}
				}
			}
			oppVehicles.get(0).setInitCity(predictCity);
			System.out.println("City: " + predictCity);
		}

		System.out.println(myBid + " VS " + oppBid);
	}

	@Override
	public Long askPrice(Task task) {

		if (myPlan.getBiggestVehicle().getCapacity() < task.weight)
			return null;

		myNewCost = myPlan.solveWithNewTask(task).cost();
		oppNewCost = oppPlan.solveWithNewTask(task).cost();

		double myMarginalCost = myNewCost - myCost;
		double oppMarginalCost = oppNewCost - oppCost;

		System.out.println("predict cost:" + oppMarginalCost);

		double mybid = oppMarginalCost * oppRatio;

		if (mybid < myMarginBidRatio * myMarginalCost) {
			mybid = myMarginBidRatio * myMarginalCost;
		}

		// if(mybid > 1.3*myMarginalCost){
		// mybid = 1.1*myMarginalCost;
		// }

		if (round > 0 && mybid < bidOppMin) {
			mybid = Math.max(bidOppMin - 1, 0);
		}

		if (round < initialNum) {
			mybid = initialBidRatio * mybid;
		}

		// mybid = mybid *
		// propobality[task.pickupCity.id][task.deliveryCity.id];
		round++;

		return (long) Math.floor(mybid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		System.out.println(tasks.size());

		// CentralizedPlan bestPlan = myPlan.getBestPlan();
		// bestPlan.removeTask(tasks);
		// System.out.println(bestPlan.getTaskNum());
		PDPlan pdplan = new PDPlan(myVehicles);
		pdplan.solveWithTaskSet(tasks);

		List<Plan> plans = new ArrayList<Plan>();
		PDP pdpAlg = new PDP(myVehicles, tasks);
		pdpAlg.SLSAlgorithmWithInitPlan(allowedTime, pdplan.getBestPlan());
		// pdpAlg.SLSAlgorithm(allowedTime);
		CentralizedPlan selectedPlan = pdplan.getBestPlan().cost() < pdpAlg.getBestPlan().cost() ? pdplan.getBestPlan()
				: pdpAlg.getBestPlan();

		System.out.println(myPlan.getBestPlan().cost() + "VS" + selectedPlan.cost());

		selectedPlan.printPlan();
		for (MyVehicle vehicle : myVehicles) {
			plans.add(makePlan(vehicle, selectedPlan.getVehicleActions().get(vehicle)));
		}

		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}

	private Plan makePlan(MyVehicle vehicle, LinkedList<Action> linkedList) {

		City currentCity = vehicle.getInitCity();
		Plan plan = new Plan(currentCity);

		for (Action action : linkedList) {
			if (action.type == Type.PICKUP) {
				City nextCity = action.currentTask.pickupCity;
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
				}
				currentCity = nextCity;
				plan.appendPickup(action.currentTask);
			} else {
				City nextCity = action.currentTask.deliveryCity;
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
				}
				currentCity = nextCity;
				plan.appendDelivery(action.currentTask);
			}
		}

		return plan;
	}

	private void initPro() {

		int max = 0;
		int min = Integer.MAX_VALUE;

		int[][] cityEdge = new int[topology.size()][topology.size()];
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				cityEdge[city1.id][city2.id] = city1.neighbors().size() * city2.neighbors().size();
				if (cityEdge[city1.id][city2.id] > max) {
					max = cityEdge[city1.id][city2.id];
				}
				if (cityEdge[city1.id][city2.id] < min) {
					min = cityEdge[city1.id][city2.id];
				}
			}
		}

		double ratio = (bidAboutPositionMax - bidAboutPositionMin) / (max - min);
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				propobality[city1.id][city2.id] = (cityEdge[city1.id][city2.id] - min) * ratio + bidAboutPositionMin;
				System.out.println(propobality[city1.id][city2.id]);
			}
		}
	}

	private Plan makePlan(MyVehicle vehicle, LinkedList<Action> linkedList, TaskSet tasks) {
		City currentCity = vehicle.getInitCity();
		Plan plan = new Plan(currentCity);
		HashMap<Integer, Task> taskMap = new HashMap<Integer, Task>();
		for (Task task : tasks) {
			taskMap.put(task.id, task);
		}

		for (Action action : linkedList) {
			System.out.println(action.currentTask.id);

			if (action.type == Type.PICKUP) {
				City nextCity = action.currentTask.pickupCity;
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
				}
				currentCity = nextCity;

				Task selectedTask = taskMap.get(action.currentTask.id);
				plan.appendPickup(selectedTask);

			} else {
				City nextCity = action.currentTask.deliveryCity;
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
				}
				currentCity = nextCity;

				Task selectedTask = taskMap.get(action.currentTask.id);
				plan.appendDelivery(selectedTask);
			}
		}
		return plan;
	}
}
