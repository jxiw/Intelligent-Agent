package previous;

//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import previous.Action.Type;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Auction2 implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	
	private PDPlan myPlan;
	private PDPlan oppPlan;
	
	private double myCost;
	private double myNewCost;
	private double oppCost;
	private double oppNewCost;
	
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
		this.myPlan = new PDPlan(agent.vehicles());		
		this.oppPlan = new PDPlan(agent.vehicles());
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
			
			oppCost = oppNewCost;
			myPlan.useLastestPlan();
		}
		
		
		System.out.println(winner==agent.id());
		System.out.println(myBid+" VS "+oppBid);
	}
	
	@Override
	public Long askPrice(Task task) {

		if(myPlan.getBiggestVehicle().capacity() < task.weight)
			return null;

		myNewCost = myPlan.solveWithNewTask(task).cost();
		oppNewCost = oppPlan.solveWithNewTask(task).cost();
		
		double myMarginalCost = myNewCost - myCost;
		double oppMarginalCost = oppNewCost - oppCost;
		
		double mybid = oppMarginalCost*ratioOpp;
		
		System.out.println(oppMarginalCost);
		
		if(mybid < myMarginBidRatio*myMarginalCost){
			mybid = myMarginBidRatio*myMarginalCost;
		}
		
//		if(mybid > 1.3*myMarginalCost){
//			mybid = 1.1*myMarginalCost;
//		}
		
		if(round > 0 && mybid < bidOppMin){
			mybid = bidOppMin;
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
		
		PDP pdpAlg = new PDP(vehicles, tasks);
		pdpAlg.SLSAlgorithm();
		CentralizedPlan centralizedPlan = pdpAlg.getBestPlan();
		
		centralizedPlan.printPlan();
		
		
		List<Plan> plans = new ArrayList<Plan>(); 
		for(Vehicle vehicle:vehicles){
			plans.add(makePlan(vehicle,centralizedPlan.getVehicleActions().get(vehicle)));	
		}		

		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}
	
	private Plan makePlan(Vehicle vehicle, LinkedList<Action> linkedList) {
		
    	City currentCity = vehicle.homeCity();
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
