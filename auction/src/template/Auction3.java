package template;

//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
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
import logist.topology.Topology.City;
import template.Action.Type;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Auction3 implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	
	private PDPlan myPlan;
	private PDPlan oppPlan;
	
	private double myCost;
	private double myNewCost;
	private double oppCost;
	private double oppNewCost;
	
	private double oppTotalBid;
	//private List<City> oppCity;
	
	private List<MyVehicle> myVehicles;
	private List<MyVehicle> oppVehicles;
	
	private List<City> allCities;
	private List<City> myVehicleCities;
	private TaskSet oppTasks;
	
	private double ratioOpp=0.8;
	private double initialBidRatio=0.6;
	private double initialNum=4;
	private double myMarginBidRatio=0.8;
	
	private double bidOppMin = Double.MAX_VALUE;
	private int round = 0;
	
	//private double ratio=0.85;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		List<Vehicle> vehicles = agent.vehicles();
		myVehicles = new ArrayList<MyVehicle>(vehicles.size());
		oppVehicles = new ArrayList<MyVehicle>(vehicles.size());
		
		allCities = topology.cities();
		myVehicleCities = new ArrayList<Topology.City>();
		
		for(Vehicle vehicle:vehicles){			
			MyVehicle myVehicle = new MyVehicle(vehicle);
			myVehicles.add(myVehicle);
			myVehicleCities.add(vehicle.homeCity());
		}
		
		for(Vehicle vehicle:vehicles){
			Random random = new Random();
			City randomCity;
			do{
				int randomNum = random.nextInt(allCities.size());
				randomCity = allCities.get(randomNum);
			}while(myVehicleCities.contains(randomCity));
			
			MyVehicle oppVehicle = new MyVehicle(null, randomCity, vehicle.capacity() , vehicle.costPerKm());
			oppVehicles.add(oppVehicle);
		}
		
		this.myPlan = new PDPlan(myVehicles);		
		this.oppPlan = new PDPlan(oppVehicles);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		double myBid = bids[agent.id()];
		double oppBid = bids[1-agent.id()];
		if(oppBid<bidOppMin){
			bidOppMin = oppBid;
		}
		
		if (winner == agent.id()) {
			
			myCost = myNewCost;
			oppPlan.useLastestPlan();
		}else{
			
			oppTasks.add(previous);
			oppCost = oppNewCost;
			myPlan.useLastestPlan();
		}
		
		if (round == 1) {
			City predictCity = null;
			double costDiff = Double.MAX_VALUE;
			for (City city : allCities) {
				if (!myVehicleCities.contains(city)) {
					
				}
			}
			oppVehicles.get(0).setInitCity(predictCity);
			System.out.println("City: "+ predictCity);
		}
//		else{
//			City predA = null;
//			City predB = null;
//			for(City cityA:allCities){
//				for(City cityB:allCities){
//					PDPlan plan = new PDPlan(MyVehicles)
//					for(Task task:oppTasks){
//						
//					}
//				}
//			}
//		}
		
		//System.out.println(winner==agent.id());
		System.out.println(myBid+" VS "+oppBid);
	}
	
	@Override
	public Long askPrice(Task task) {

		if(myPlan.getBiggestVehicle().getCapacity() < task.weight)
			return null;

		myNewCost = myPlan.solveWithNewTask(task).cost();
		oppNewCost = oppPlan.solveWithNewTask(task).cost();
		
		double myMarginalCost = myNewCost - myCost;
		double oppMarginalCost = oppNewCost - oppCost;
		
		System.out.println("predict cost:"+oppMarginalCost);
		
		double mybid = oppMarginalCost*ratioOpp;
		
		//System.out.println(oppMarginalCost);
		
		if(mybid < myMarginBidRatio*myMarginalCost){
			mybid = myMarginBidRatio*myMarginalCost;
		}
		
//		if(mybid > 1.3*myMarginalCost){
//			mybid = 1.1*myMarginalCost;
//		}
		
		if(round > 0 && mybid < bidOppMin){
			mybid = bidOppMin - 1 > 0 ? bidOppMin - 1:0;
		}
		
		if(round<initialNum){
			mybid = initialBidRatio * mybid;
		}
		
		round++;
		//double bid = ratio * myMarginalCost;
		
		return (long) Math.round(mybid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		System.out.println(tasks.size());
		
		PDP pdpAlg = new PDP(myVehicles, tasks);
		//pdpAlg.SLSAlgorithm(30000L);
		CentralizedPlan centralizedPlan = pdpAlg.getBestPlan();
		
		centralizedPlan.printPlan();
		
		
		List<Plan> plans = new ArrayList<Plan>(); 
		for(MyVehicle vehicle:myVehicles){
			plans.add(makePlan(vehicle,centralizedPlan.getVehicleActions().get(vehicle)));	
		}		

		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}
	
	private Plan makePlan(MyVehicle vehicle, LinkedList<Action> linkedList) {
		
    	City currentCity = vehicle.getInitCity();
    	Plan plan = new Plan(currentCity);
    	
		for(Action action:linkedList){
			if(action.type == Type.PICKUP){
				City nextCity = action.currentTask.pickupCity;
				for (City city : currentCity.pathTo(nextCity)) {
					plan.appendMove(city);
	            }
				currentCity = nextCity;
				plan.appendPickup(action.currentTask);
			}else{
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
	
}
