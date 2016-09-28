//package template;
//
////the list of imports
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//
//import logist.agent.Agent;
//import logist.behavior.AuctionBehavior;
//import logist.plan.Plan;
//import logist.simulation.Vehicle;
//import logist.task.Task;
//import logist.task.TaskDistribution;
//import logist.task.TaskSet;
//import logist.topology.Topology;
//import logist.topology.Topology.City;
//import template.Action.Type;
//
///**
// * A very simple auction agent that assigns all tasks to its first vehicle and
// * handles them sequentially.
// * 
// */
//@SuppressWarnings("unused")
//public class Auction1 implements AuctionBehavior {
//
//	private Topology topology;
//	private TaskDistribution distribution;
//	private Agent agent;
//	
//	private PDPlan pdplan;
//	private double cost;
//	private double newCost;
//	private double ratio=0.85;
//
//	@Override
//	public void setup(Topology topology, TaskDistribution distribution,
//			Agent agent) {
//
//		this.topology = topology;
//		this.distribution = distribution;
//		this.agent = agent;
//		this.pdplan = new PDPlan(agent.vehicles());
//	}
//
//	@Override
//	public void auctionResult(Task previous, int winner, Long[] bids) {
//		if (winner == agent.id()) {
//			ratio -= 0.05;
//			ratio = (ratio<0.85?0.85:ratio);
//			cost = newCost;
//		}else{
//			ratio += 0.1;
//			ratio = (ratio>1.2?1.2:ratio);
//			pdplan.useLastestPlan();
//		}
//		
//		System.out.println(winner==agent.id());
//		
//	}
//	
//	@Override
//	public Long askPrice(Task task) {
//
//		if(pdplan.getBiggestVehicle().capacity() < task.weight)
//			return null;
//
//		newCost = pdplan.solveWithNewTask(task).cost();
//		//System.out.println(pdplan.getSearchPlan());
//		double marginalCost = newCost - cost;
//		
//		//ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
//		double bid = ratio * marginalCost;
//		bid = (bid>200?bid:200);
//		
//		System.out.println(Math.round(marginalCost));
//
//		return (long) Math.round(marginalCost*1.2);
//	}
//
//	@Override
//	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
//		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
//		System.out.println(tasks.size());
//		
//		PDP pdpAlg = new PDP(vehicles, tasks);
//		pdpAlg.SLSAlgorithm();
//		CentralizedPlan centralizedPlan = pdpAlg.getBestPlan();
//		
//		//CentralizedPlan centralizedPlan = pdplan.getBestPlan();
//		//System.out.println(centralizedPlan.getTaskNum());
//		centralizedPlan.printPlan();
//		
//		
//		List<Plan> plans = new ArrayList<Plan>(); 
//		for(Vehicle vehicle:vehicles){
//			plans.add(makePlan(vehicle,centralizedPlan.getVehicleActions().get(vehicle)));	
//		}		
//
//		while (plans.size() < vehicles.size())
//			plans.add(Plan.EMPTY);
//
//		return plans;
//	}
//	
//	private Plan makePlan(Vehicle vehicle, LinkedList<Action> linkedList) {
//		
//    	City currentCity = vehicle.homeCity();
//    	Plan plan = new Plan(currentCity);
//    	
//		for(Action action:linkedList){
//			if(action.type == Type.PICKUP){
//				City nextCity = action.currentTask.pickupCity;
//				for (City city : currentCity.pathTo(nextCity)) {
//					plan.appendMove(city);
//	            }
//				currentCity = nextCity;
//				plan.appendPickup(action.currentTask);
//			}else{
//				City nextCity = action.currentTask.deliveryCity;
//				for (City city : currentCity.pathTo(nextCity)) {
//					plan.appendMove(city);
//	            }
//				currentCity = nextCity;
//				plan.appendDelivery(action.currentTask);
//			}
//		}
//		
//		return plan;
//	}
//	
//}
