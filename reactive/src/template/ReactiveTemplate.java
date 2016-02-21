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

	private HashMap<State,AgentAction> stateBestAction = new HashMap<State,AgentAction>();
	private HashMap<State,Double> stateBestValue = new HashMap<State,Double>();
	
	private HashMap<AgentAction,Integer> actionReward = new HashMap<AgentAction, Integer>();
	private HashMap<State,Double> stateProbability = new HashMap<State,Double>();
	private HashMap<State,List<AgentAction>> stateActionRelation = new HashMap<State, List<AgentAction>>();
	
	private HashMap<City,List<State>> cityToState = new HashMap<City, List<State>>();
	
	private List<City> allCity; 
	
	//private Random random;
	private double pPickup;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,0.95);
		
		allCity=topology.cities();
		
		for(City cityA:allCity){
			double isTaskProbability=0;
			List<State> nowCityToState = new ArrayList<State>(); 
			
			for(City cityB:allCity){
				if(cityA!=cityB){
					State taskState = new State(true,cityA,cityB);
					stateProbability.put(taskState, td.probability(cityA, cityB));
					stateBestValue.put(taskState, 0.0);
					nowCityToState.add(taskState);
					
					List<AgentAction> stateActionList = new ArrayList<AgentAction>();
					AgentAction deliveryAction = new AgentAction(cityA, cityB,true);
					stateActionList.add(deliveryAction);
					actionReward.put(deliveryAction, td.reward(cityA, cityB));
					for(City neighborCity:cityA.neighbors()){
						AgentAction noDeliveryAction = new AgentAction(cityA, neighborCity, false);
						stateActionList.add(noDeliveryAction);
						actionReward.put(noDeliveryAction, 0);
					}
					stateActionRelation.put(taskState, stateActionList);
					
					isTaskProbability += td.probability(cityA, cityB);
				}
			}
			
			State noTaskState = new State(false,cityA,null);
			stateProbability.put(noTaskState, 1-isTaskProbability);
			stateBestValue.put(noTaskState, 0.0);
			nowCityToState.add(noTaskState);
			
			List<AgentAction> stateActionList = new ArrayList<AgentAction>();
			for(City neighborCity:cityA.neighbors()){
				AgentAction noDeliveryAction = new AgentAction(cityA, neighborCity, false);
				stateActionList.add(noDeliveryAction);
				actionReward.put(noDeliveryAction, 0);
			}
			
			stateActionRelation.put(noTaskState, stateActionList);
			cityToState.put(cityA, nowCityToState);
		}
		
		
		
		//this.random = new Random();
		this.pPickup = discount;
		plan();
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		
		Action action;
		City currentCity = vehicle.getCurrentCity();
		
		if(availableTask != null && currentCity == availableTask.pickupCity){
			State nowState = new State(true, currentCity, availableTask.deliveryCity);
			AgentAction agentAction =  stateBestAction.get(nowState);
			if(agentAction.isPickup()){
				action = new Pickup(availableTask);
			}
			else{
				action = new Move(agentAction.getDestination());
			}
			
		}else{
			
			State nowState = new State(false, currentCity, null);
			AgentAction agentAction =  stateBestAction.get(nowState);
			action = new Move(agentAction.getDestination());
		} 
		return action;
	}
	
	
	public void plan(){
		
		int i=0;
		boolean isConvergence=false;
		while(!isConvergence){
			
			i++;
			isConvergence=true;
			for(City city:allCity){
				for(State state:cityToState.get(city)){
					List<AgentAction> actionLists = stateActionRelation.get(state);
					AgentAction maxAction= stateBestAction.get(state);
					double maxActionValue = stateBestValue.get(state);
					for(AgentAction action:actionLists){
						double accumulateValue = 0;
						int reward = actionReward.get(action);
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
		
		System.out.println(i);
	}
}
