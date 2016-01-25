package tao;

//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

@SuppressWarnings("unused")
public class AuctionTemplate implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private int VirtualOpponents = 3;
	private int totalTaskNumber = 50;
	
	private generatePlan[] AllPlans = new generatePlan[VirtualOpponents + 1];
	private TaskPlan[] initialStates = new TaskPlan[VirtualOpponents + 1];
	private TaskPlan[] updatedStates = new TaskPlan[VirtualOpponents + 1];
	private Company[] companies = new Company[2];
	private Map<Integer, Task> map = new HashMap<Integer, Task>();
  private List<Double> estimationRatio = new ArrayList<Double>();

	private double alpha = 0.8;
	private double alphaMax = 1;
	private double alphaMin = 0.45;
	private double alphaUpStep = 0.1;
	private double alphaDownStep = 0.05;
	private double ratio = 1;
	private double ratioMax = 2.5;
	private double ratioMin = 0.7;
	private double beta = 0.85;
	private double oppoTotalBids = 0;
	private int round = 0;
	private RegressionModel model;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		this.model = new RegressionModel();
		
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		
		// initialize vehicle list for my agent
		List<modifiedVehicle> vehicles = new ArrayList<modifiedVehicle>();
		for(int i = 0; i < agent.vehicles().size(); i++){
			vehicles.add(new modifiedVehicle(agent.vehicles().get(i)));
		}
		
		// initialize generatePlan class
		for(int i = 0; i < VirtualOpponents + 1; i++){
			if(i == 0)
				AllPlans[i] = new generatePlan(vehicles);
			else
				AllPlans[i] = new generatePlan(getVehicles());
			
			// initialize plans
			initialStates[i] = new TaskPlan();
			updatedStates[i] = new TaskPlan();

			int size = AllPlans[i].vehicles.size();
			List<Integer> tmp = new ArrayList<Integer>();
			for(int k = 0; k < size; k++){
				initialStates[i].vehicleTasks.add(tmp);
				updatedStates[i].vehicleTasks.add(tmp);
			}
		}
		
		for(int i = 0; i < 2; i++)
			companies[i] = new Company();
	}

	List<modifiedVehicle> getVehicles(){
		List<modifiedVehicle> res = new ArrayList<modifiedVehicle>();
		int size = randInt((int)Math.round(agent.vehicles().size() * 0.5), (int)Math.round(agent.vehicles().size() * 1.5));
		
		for(int i = 0; i < size; i++){
			modifiedVehicle vehicle = new modifiedVehicle();
	        Random rand = new Random();
			double tmp = agent.vehicles().get(rand.nextInt(agent.vehicles().size())).capacity();
			double low = tmp * 0.8;
			double high = tmp * 1.2;
			vehicle.id = i;
			vehicle.capacity = randInt((int)low, (int)high);
			vehicle.homeCity = topology.cities().get(rand.nextInt(topology.cities().size()));
			vehicle.costPerKm = agent.vehicles().get(rand.nextInt(agent.vehicles().size())).costPerKm();
			res.add(vehicle);
		}
		return res;
	}
	
	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {

/*		
		// update ratio according to time closeness
		double tmp = 0;
		double sum = 0;
      for (int i = 0; i < estimationRatio.size(); i++) {
      	tmp +=   (i + 1) * estimationRatio.get(i);
      	sum += (i+1);
      }
      
      ratio = 0.5 * (tmp/sum) + 0.5 * ratio;
*/
		model.compute(estimationRatio, 0, estimationRatio.size()-1);
		ratio = model.evaluateAt(estimationRatio.size());
      ratio = Math.min(ratioMax, ratio);
      ratio = Math.max(ratioMin, ratio);
      
		
      // update alpha
		if (winner == agent.id()) {
			companies[0].last_bid = companies[0].current_bid;
			companies[0].last_estimate = companies[0].current_estimate;
			
			initialStates[0] = CopyState(updatedStates[0]);
			alpha = Math.min(alphaMax, alpha + alphaUpStep);
		}
		else{
			oppoTotalBids += bids[1 - agent.id()];
			companies[1].last_bid = bids[1 - agent.id()];
			companies[1].last_estimate = companies[1].current_estimate;
			
			for(int i = 1; i < 4; i++)
				initialStates[i] = CopyState(updatedStates[i]);
			alpha = Math.max(alphaMin, alpha - alphaDownStep);
		}
		
		for(int i = 1; i <4; i++)
			AllPlans[i].RandomShuffle();
	}
	
	@Override
	public Long askPrice(Task task) {
		map.put(task.id, task);
		generatePlan.tasks_map.put(task.id, task.pickupCity);
		generatePlan.tasks_map.put(task.id + totalTaskNumber, task.deliveryCity);
		generatePlan.map = map;
		
		if (vehicle.capacity() < task.weight)
			return null;

		for(int i = 0; i < 4; i++){
			updatedStates[i] = CopyState(initialStates[i]);
	        Random rand = new Random();
			int index = rand.nextInt(AllPlans[i].vehicles.size());
			
			updatedStates[i].vehicleTasks.get(index).add(task.id);
			updatedStates[i].vehicleTasks.get(index).add(task.id + totalTaskNumber);
			updatedStates[i] = AllPlans[i].SLS(updatedStates[i]);
		}
		
		double lowest = (updatedStates[0].cost - initialStates[0].cost) * alpha;
		double estimate = (updatedStates[1].cost + updatedStates[2].cost + updatedStates[3].cost) * ratio /3 - oppoTotalBids;
		
		companies[0].current_estimate = updatedStates[0].cost;
		companies[0].current_bid = Math.max(lowest, estimate * beta);
		companies[1].current_estimate = (updatedStates[1].cost + updatedStates[2].cost + updatedStates[3].cost) /3;
		
		if(companies[0].current_bid == 0) System.out.println("lowest = " + lowest + ",  estimate * beta = " + estimate + " * " + beta);
		
      return (long) companies[0].current_bid;
	}

	public TaskPlan CopyState(TaskPlan toCopy){
		TaskPlan Copied = new TaskPlan();
  	for(int i = 0; i < toCopy.vehicleTasks.size(); i++)
  		Copied.vehicleTasks.add(new ArrayList<Integer>(toCopy.vehicleTasks.get(i)));
		Copied.cost = toCopy.cost;
		return Copied;
	}
	
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		for(Task task: tasks){
			map.put(task.id, task);
			AllPlans[0].tasks_map.put(task.id, task.pickupCity);
			AllPlans[0].tasks_map.put(task.id + totalTaskNumber, task.deliveryCity);
		}

		List<Plan> plans = new ArrayList<Plan>();
		TaskPlan results = initialStates[0];
		double cost = 0;
		
		for(int i = 0; i < vehicles.size(); i++){
			Plan planVehicle = naivePlan(vehicles.get(i), results.vehicleTasks.get(i));
			plans.add(planVehicle);
			cost += planVehicle.totalDistance()*vehicles.get(i).costPerKm();
		}
		System.out.println("total cost: " + cost);
		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, List<Integer> list) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		if(list.size() == 0) 
			return Plan.EMPTY;
		
		for(int i = 0; i < list.size(); i++){
			City next_city = AllPlans[0].tasks_map.get(list.get(i));
			for (City city : current.pathTo(next_city))
				plan.appendMove(city);
			if(list.get(i) < totalTaskNumber)
				plan.appendPickup(map.get(list.get(i)));
			else
				plan.appendDelivery(map.get(list.get(i) - totalTaskNumber));
			current = next_city;
		}
		return plan;
	}
	
	public static int randInt(int min, int max) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;
	    return randomNum;
	}
	
	public static float randfloat(double d, double e) {
	    Random rand = new Random();
	    float randomNum = (float) Math.min(1.2, rand.nextFloat() + d);
	    return randomNum;
	}
}

class Company{
	double last_estimate = 0;
	double last_bid = 0;
	double current_estimate = 0;
	double current_bid = 0;
}

