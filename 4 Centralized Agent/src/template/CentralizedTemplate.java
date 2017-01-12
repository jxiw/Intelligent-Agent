package template;

//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config\\settings_default.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();

		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		// Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

		PDP pdpAlg = new PDP(vehicles, tasks);
		pdpAlg.SLSAlgorithm();
		CentralizedPlan centralizedPlan = pdpAlg.getBestPlan();
		pdpAlg.printPlan(centralizedPlan);
		System.out.println("cost " + pdpAlg.caculatePlanCost(centralizedPlan));

		List<Plan> plans = new ArrayList<Plan>();
		for (Vehicle vehicle : vehicles) {
			if (centralizedPlan.getNextState().get(vehicle) != null) {
				plans.add(makePlan(vehicle, centralizedPlan.getNextState().get(vehicle)));
			}
		}

		while (plans.size() < vehicles.size()) {
			plans.add(Plan.EMPTY);
		}

		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");

		return plans;
	}

	private Plan makePlan(Vehicle vehicle, LinkedList<State> linkedList) {
		// TODO Auto-generated method stub
		City currentCity = vehicle.homeCity();
		Plan plan = new Plan(currentCity);

		for (State state : linkedList) {
			if (state.isPickup()) {
				City nextCity = state.getCurrentTask().pickupCity;
				System.out.println(currentCity + " " + nextCity);
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
				}
				currentCity = nextCity;
				plan.appendPickup(state.getCurrentTask());
			} else {
				City nextCity = state.getCurrentTask().deliveryCity;
				System.out.println(currentCity + " " + nextCity);
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
				}
				currentCity = nextCity;
				plan.appendDelivery(state.getCurrentTask());
			}
		}

		return plan;
	}

	// private List<Plan> makePlan(CentralizedPlan plan){

	// ArrayList<Plan> planList = new ArrayList<Plan>();
	// HashMap<Vehicle, LinkedList<State>> vehicleToState = plan.getNextState();
	// for(Map.Entry< Vehicle, LinkedList<State> > entry :
	// vehicleToState.entrySet()){
	// Vehicle vehicle = entry.getKey();
	// System.out.println("Vehicle "+vehicle);
	// Plan tmpPlan = new Plan(vehicle.homeCity());
	// LinkedList<State> nextState = entry.getValue();

	// if(nextState != null && nextState.size() > 0){

	// City currentCity = vehicle.homeCity();
	// for(State state:nextState){
	// if(state.isPickup()){
	// City nextCity = state.getCurrentTask().pickupCity;
	// System.out.println(currentCity + " "+ nextCity);
	// for (City city : currentCity.pathTo(nextCity)) {
	// tmpPlan.appendMove(city);
	// }
	// currentCity = nextCity;
	// tmpPlan.appendPickup(state.getCurrentTask());

	// }else{

	// City nextCity = state.getCurrentTask().deliveryCity;
	// System.out.println(currentCity + " "+ nextCity);
	// for (City city : currentCity.pathTo(nextCity)) {
	// tmpPlan.appendMove(city);
	// }
	// currentCity = nextCity;
	// tmpPlan.appendDelivery(state.getCurrentTask());
	// }
	// }
	// planList.add(tmpPlan);
	// }

	// }
	// return planList;
	// }

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity)) {
				plan.appendMove(city);
			}

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path()) {
				plan.appendMove(city);
			}

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
}
