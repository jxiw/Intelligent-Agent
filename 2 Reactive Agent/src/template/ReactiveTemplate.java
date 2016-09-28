package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private static final double INITVALUE=-999999; 
	
	private HashMap<State,AgentAction> stateBestAction = new HashMap<State,AgentAction>(); //Store the best action for each state.
	private HashMap<State,Double> stateBestValue = new HashMap<State,Double>();	//Store the best value for each state in learning phase.
	private HashMap<AgentAction,Double> actionReward = new HashMap<AgentAction, Double>(); //Store reward for each action.
	private HashMap<State,Double> stateProbability = new HashMap<State,Double>(); //Store probability for each state.
	private HashMap<State,List<AgentAction>> stateActionRelation = new HashMap<State, List<AgentAction>>(); //Store available actions for each state.
	private HashMap<City,List<State>> cityToState = new HashMap<City, List<State>>(); //Store all possible states for each city.
	private List<City> allCity; 
	
	//private random;
	private double pPickup;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95.
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
		//Reads all cities from topology.
		allCity=topology.cities();
		
		for(City cityA:allCity){
			/*For each city, find all possible states in which it is the current location of the agent.
			 * Also find corresponding actions and the reward of actions of the states.
			 */
			double isTaskProbability=0;
			List<State> nowCityToState = new ArrayList<State>(); 
			
			//First find all states in which there is a task.
			for(City cityB:allCity){	
				if(cityA!=cityB){
					State taskState = new State(true,cityA,cityB);
					stateProbability.put(taskState, td.probability(cityA, cityB));
					stateBestValue.put(taskState, INITVALUE);
					nowCityToState.add(taskState);
					
					List<AgentAction> stateActionList = new ArrayList<AgentAction>();
					AgentAction deliveryAction = new AgentAction(cityA, cityB,true);
					stateActionList.add(deliveryAction);
					double cost = cityA.distanceTo(cityB)*agent.vehicles().get(0).costPerKm();
					actionReward.put(deliveryAction, td.reward(cityA, cityB) - cost);
					for(City neighborCity:cityA.neighbors()){
						AgentAction noDeliveryAction = new AgentAction(cityA, neighborCity, false);
						stateActionList.add(noDeliveryAction);
						actionReward.put(noDeliveryAction, 0 - cityA.distanceTo(neighborCity) * agent.vehicles().get(0).costPerKm());
					}
					stateActionRelation.put(taskState, stateActionList);
					
					isTaskProbability += td.probability(cityA, cityB);
				}
			}
			
			//Then find all states in which there is no task.
			State noTaskState = new State(false,cityA,null);
			stateProbability.put(noTaskState, 1-isTaskProbability);
			stateBestValue.put(noTaskState, INITVALUE);
			nowCityToState.add(noTaskState);
			
			List<AgentAction> stateActionList = new ArrayList<AgentAction>();
			for(City neighborCity:cityA.neighbors()){
				AgentAction noDeliveryAction = new AgentAction(cityA, neighborCity, false);
				stateActionList.add(noDeliveryAction);
				actionReward.put(noDeliveryAction, 0 - cityA.distanceTo(neighborCity) * agent.vehicles().get(0).costPerKm());
			}
			
			stateActionRelation.put(noTaskState, stateActionList);
			cityToState.put(cityA, nowCityToState);
		}
		
		//this.random = new Random();
		this.pPickup = discount;
		plan();
		System.out.println(agent.name()+": ");
		printValue();
		System.out.println(); 
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		
		Action action;
		City currentCity = vehicle.getCurrentCity();
		
		if(availableTask != null && currentCity == availableTask.pickupCity){
			State nowState = new State(true, currentCity, availableTask.deliveryCity);
			AgentAction agentAction =  stateBestAction.get(nowState);
			if(agentAction.isPickup()){
				System.out.println(vehicle.name() + " picks up a task from "+ availableTask.pickupCity + " to " + availableTask.deliveryCity + ".");
				action = new Pickup(availableTask);
			}
			else{
				System.out.println(vehicle.name() + " refuses a task from "+ availableTask.pickupCity + " to "+  availableTask.deliveryCity + ". It moves to " + agentAction.getDestination() + ".");
				action = new Move(agentAction.getDestination());
			}
			
		}else{
						
			State nowState = new State(false, currentCity, null);
			AgentAction agentAction =  stateBestAction.get(nowState);
			action = new Move(agentAction.getDestination());
			System.out.println(vehicle.name() + " has no available task. It just moves from " + vehicle.getCurrentCity() + " to " + agentAction.getDestination() + ".");

		} 
		return action;
	}
	
	/*
	 * This method implements states value iteration for this Pickup and Delivery Problem.
	 */
	public void plan(){
		
		boolean isConvergence=false;
		while(!isConvergence){ //Loop until good enough.
			isConvergence=true;
			for(City city:allCity){
				for(State state:cityToState.get(city)){ 
					List<AgentAction> actionLists = stateActionRelation.get(state);
					AgentAction maxAction= stateBestAction.get(state);
					double maxActionValue = stateBestValue.get(state);
					for(AgentAction action:actionLists){
						double accumulateValue = 0;
						double reward = actionReward.get(action);
						accumulateValue = reward;
						City destination = action.getDestination();
						for(State nextState:cityToState.get(destination)){
							accumulateValue += this.pPickup * stateProbability.get(nextState)*stateBestValue.get(nextState);
						}
						
						if(maxActionValue<accumulateValue){
							maxActionValue = accumulateValue;
							maxAction=action;
							isConvergence=false;
						}
					}
					stateBestAction.put(state, maxAction);
					stateBestValue.put(state, maxActionValue);
				}
			}
		}
		
	}
	
	public void printValue(){
		for(City city:allCity){
			for(State state:cityToState.get(city)){
				System.out.print(stateBestAction.get(state).isPickup() + ": "+stateBestAction.get(state).getDeparture()+ "-->"+stateBestAction.get(state).getDestination()+" " + stateBestValue.get(state));
			}
			System.out.println();
		}
		
	}
	
	
}