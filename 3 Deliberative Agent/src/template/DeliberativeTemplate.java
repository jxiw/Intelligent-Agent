package template;

/* import table */
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeTemplate implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;
	double costPerKm;
	TaskSet carriedTasks;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		this.costPerKm = agent.vehicles().get(0).costPerKm();
		this.capacity = agent.vehicles().get(0).capacity();
		// ...
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {

		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = AStarPlan(currentCity, tasks);
			break;
		case BFS:
			// ...
			plan = BFSPlan(currentCity, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}

		System.out.println(tasks);
		return plan;
	}

	private Plan BFSPlan(City city, TaskSet tasks) {
		// TODO Auto-generated method stub

		System.out.println("BFS algorithm start");
		long startTime = System.currentTimeMillis();

		Plan plan = new Plan(city);
		double minCost = Double.POSITIVE_INFINITY;
		State minState = null;

		if (carriedTasks != null) {
			tasks.addAll(carriedTasks);
		}

		int size = tasks.size();
		Task[] tasksArray = tasks.toArray(new Task[size]);

		String intialTaskSign = "";
		for (int i = 0; i < size; i++) {
			if (carriedTasks != null && carriedTasks.contains(tasksArray[i])) {
				intialTaskSign = intialTaskSign + '1';
			} else {
				intialTaskSign = intialTaskSign + '0';
			}
		}

		HashSet<State> searchStateSet = new HashSet<State>();

		int iterationNum = 0;

		State startState = new State(null, null, city, 0, intialTaskSign, 0, true);
		LinkedList<State> queue = new LinkedList<State>();
		queue.add(startState);

		do {
			if (queue.isEmpty()) {
				break;
			}
			iterationNum++;

			State currentState = queue.pop();
			String curentTaskSign = currentState.getTaskSign();

			if (curentTaskSign.replace("2", "").length() == 0) {
				// System.out.println(currentState.getTaskSign());

				double currentCost = currentState.getCost();
				// System.out.println(currentCost);
				if (currentCost < minCost) {
					minCost = currentCost;
					minState = currentState;
				}
				continue;
			}

			if (currentState == null) {
				break;
			} else {

				if (!searchStateSet.contains(currentState)) {

					City currentLocation = currentState.getLocation();
					double currentCost = currentState.getCost();
					int currentWeight = currentState.getAccumulateWeight();
					// List<State> childStateList = new ArrayList<State>();
					searchStateSet.add(currentState);

					for (int i = 0; i < size; i++) {

						if (curentTaskSign.charAt(i) == '0') {

							Task possibleTask = tasksArray[i];
							int updatedWeight = possibleTask.weight + currentWeight;

							if (updatedWeight <= capacity) {
								City departure = possibleTask.pickupCity;
								double childCost = currentCost + costPerKm * currentLocation.distanceTo(departure);
								char[] childSign = curentTaskSign.toCharArray();
								childSign[i] = '1';

								State childState = new State(currentState, possibleTask, departure, childCost,
										new String(childSign), updatedWeight, true);
								queue.add(childState);
							}
						} else if (curentTaskSign.charAt(i) == '1') {

							Task possibleTask = tasksArray[i];
							City destination = possibleTask.deliveryCity;

							double childCost = currentCost + costPerKm * currentLocation.distanceTo(destination);
							char[] childSign = curentTaskSign.toCharArray();
							childSign[i] = '2';

							int updatedWeight = currentWeight - possibleTask.weight;
							State childState = new State(currentState, possibleTask, destination, childCost,
									new String(childSign), updatedWeight, false);
							queue.add(childState);
						}
					}
				}
			}

		} while (true);

		long endTime = System.currentTimeMillis();
		System.out.println(iterationNum + " " + "Minumum Cost: " + minCost);
		System.out.println("Execution time: " + (endTime - startTime) + "");

		getPlanFromTree(minState, plan);
		System.out.println(plan);
		return plan;
	}

	public Plan AStarPlan(City city, TaskSet tasks) {

		System.out.println("A* algorithm start");
		long startTime = System.currentTimeMillis();

		Plan plan = new Plan(city);
		double minCost = Double.POSITIVE_INFINITY;
		State minState = null;

		if (carriedTasks != null) {
			tasks.addAll(carriedTasks);
		}

		int size = tasks.size();
		Task[] tasksArray = tasks.toArray(new Task[size]);

		String intialTaskSign = "";
		for (int i = 0; i < size; i++) {
			if (carriedTasks != null && carriedTasks.contains(tasksArray[i])) {
				intialTaskSign = intialTaskSign + '1';
			} else {
				intialTaskSign = intialTaskSign + '0';
			}
		}

		HashSet<State> searchStateSet = new HashSet<State>();
		StateCompartor stateCompartor = new StateCompartor(tasksArray, costPerKm);

		State startState = new State(null, null, city, 0, intialTaskSign, 0, true);
		PriorityQueue<State> queue = new PriorityQueue<State>(100000, stateCompartor);
		queue.add(startState);
		int iterationNum = 0;

		do {
			if (queue.isEmpty()) {
				break;
			}
			iterationNum++;

			State currentState = queue.poll();

			String currentTaskSign = currentState.getTaskSign();
			if (currentTaskSign.replaceAll("2", "").length() == 0) {
				minCost = currentState.getCost();
				minState = currentState;
				break;
			}

			if (currentState == null) {
				break;
			} else {

				if (!searchStateSet.contains(currentState)) {
					City currentLocation = currentState.getLocation();
					double currentCost = currentState.getCost();
					int currentWeight = currentState.getAccumulateWeight();
					searchStateSet.add(currentState);

					for (int i = 0; i < size; i++) {
						if (currentTaskSign.charAt(i) == '0') {

							Task possibleTask = tasksArray[i];
							int updatedWeight = possibleTask.weight + currentWeight;
							if (updatedWeight <= capacity) {
								City departure = possibleTask.pickupCity;
								double childCost = currentCost + costPerKm * currentLocation.distanceTo(departure);
								char[] childSign = currentTaskSign.toCharArray();
								childSign[i] = '1';

								State childState = new State(currentState, possibleTask, departure, childCost,
										new String(childSign), updatedWeight, true);
								queue.add(childState);
							}
						} else if (currentTaskSign.charAt(i) == '1') {

							Task possibleTask = tasksArray[i];
							City destination = possibleTask.deliveryCity;
							double childCost = currentCost + costPerKm * currentLocation.distanceTo(destination);
							char[] childSign = currentTaskSign.toCharArray();
							childSign[i] = '2';
							int updatedWeight = currentWeight - possibleTask.weight;
							State childState = new State(currentState, possibleTask, destination, childCost,
									new String(childSign), updatedWeight, false);
							queue.add(childState);
						}
					}
				}
			}

		} while (true);

		long endTime = System.currentTimeMillis();
		System.out.println("Iteration Number: " + iterationNum + " " + "Minumum Cost: " + minCost);
		System.out.println("Execution Time: " + (endTime - startTime) + " ms");

		getPlanFromTree(minState, plan);
		return plan;
	}

	public void getPlanFromTree(State state, Plan plan) {
		State parentState = state.getParentSate();
		if (parentState != null) {
			getPlanFromTree(parentState, plan);
			Task rationTask = state.getTask();
			City parentCity = parentState.getLocation();
			City currentCity = state.getLocation();
			String signTask = state.getTaskSign();

			for (City city : parentCity.pathTo(currentCity))
				plan.appendMove(city);
			if (state.isPickup()) {
				plan.appendPickup(rationTask);
			} else {
				plan.appendDelivery(rationTask);
			}
		}
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed
			this.carriedTasks = carriedTasks;
		}
	}
}
