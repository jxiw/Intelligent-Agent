package guo;

//the list of imports
import java.util.ArrayList;
import java.util.List;

import logist.Measures;
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
    private Agent agent;
    public  int iterationtimes=10000; //may want to reduce this to improve planning, and run 5*2000 instead, for example
    public  ArrayList<Task> tasksWon;
    public C latestSolution = null, assumedSolution = null;
    private int totalRewards =0; //our total won bids so far,
    private int standardBid = 0; //standard unit equal to the mean neighbor distance between cities
    private double bidMultiplier = 1.0;
    private static final int INITIAL_TASKS_TO_WIN = 3;
    private int maxCapacity = 0;

    @Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
       // System.out.println("latest 141129 1540");
        this.agent = agent;
        tasksWon =new ArrayList<Task>();
        int meanCounter = 0;
        double distanceSum = 0;
        for (City c: topology) {
            for (City n:c.neighbors()) {
                meanCounter++;
                distanceSum += n.distanceUnitsTo(c);
            }
        }
        double meanNeighborDistance = distanceSum / meanCounter;
        double meanDistanceKM = Measures.unitsToKM(Math.round(meanNeighborDistance));
        Vehicle cheapVehicle = agent.vehicles().get(cheapestVehiclePerKm());
        standardBid = cheapVehicle.costPerKm() * (int) Math.floor(meanDistanceKM);

        for (Vehicle v: agent.vehicles()) {
            if (v.capacity() > maxCapacity) {
                maxCapacity = v.capacity();
            }
        }
    }

    @Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
        if (winner == agent.id()) {
            if (bidMultiplier < 1.5 && tasksWon.size() >= INITIAL_TASKS_TO_WIN) {
                bidMultiplier += 0.10; //We increment our bids up to factor 1.5, except in the beginning phase
            }
            totalRewards += bids[agent.id()];
            tasksWon.add(previous);
            latestSolution = assumedSolution;
       //     System.out.println("bids value" + Arrays.toString(bids));  //to string to change long
        }
        else {
            bidMultiplier = 1.0;
        }
    }


    public C procedureSLS(ArrayList<Task> tasks) {   //transfer from return a C to double cost
        long startTime = System.currentTimeMillis();

        C init = new C();
        init.setupList(agent.vehicles(), tasks);
        init.makeInitialSolution();
        if (init.constraints() == false) {
          //  System.out.println("init solution bad");
        }
        else
        {
          //  System.out.println("init solution good");
         //   System.out.println(init);
         //   System.out.println(init.checkAllTasksHandled());
        }

      //  System.out.println("starting from initialsolution");
        int thousands = 0;
        for (int i = 0; i < iterationtimes; i++) {
            if (i == 1000) {
                thousands++;
               // System.out.println("iterationtimes " + thousands  + "000");
                i = 0;
                iterationtimes-= 1000;
              //  System.out.println(",\ncurrent best solution:" + init);
            }
            C solutionOld = new C(init);
            ArrayList<C> SolutionSet = C.chooseNeighbours(solutionOld);
            if (SolutionSet.size() <= 0) {
                throw new AssertionError("solutionset 0 assert");
            }
            //System.out.println("solution set size " + SolutionSet.size());
            init = C.localChoice(solutionOld, SolutionSet);//problems may appear
            if (System.currentTimeMillis()-startTime > 2000) { //60 seconds seems fine with logistPlatform
              //  System.out.println("time went too long. returning current best solution");
              //  System.out.println(init);
              //  System.out.println("iterations: " + i);
                return init;
            }
        }
        return init;
    }

    public int cheapestVehiclePerKm () {
        int lowestCost = 99999;
        int lowestId = -1;
        for (Vehicle v: agent.vehicles()) {
            if (v.costPerKm() < lowestCost) {
                lowestCost = v.costPerKm();
                lowestId = v.id();
            }
        }
        return lowestId;
    }

    public double deliveryCostCheapestVehicle (Task task) {
        Vehicle v = agent.vehicles().get(cheapestVehiclePerKm());
        long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
        double marginalCost = Measures.unitsToKM(distanceTask * v.costPerKm());
        return marginalCost;
    }
    @Override
	public Long askPrice(Task task) {
        if (task.weight > maxCapacity) {
            return null; //if this ever happens, it is really unfair since the opponent could bid a million and win the game for this 1 task. but anyway, we don't want to crash and lose becasue of it
            //perhaps they will be tricky and make tasks that are too heavy for both agents.
        }

        ArrayList <Task> assumedWonTasks = new ArrayList<Task>(tasksWon);
        assumedWonTasks.add(task);
        assumedSolution = procedureSLS(assumedWonTasks);

        if (tasksWon.size() < INITIAL_TASKS_TO_WIN){ // for the first few tasks, we have a different biddign strategy
            return Math.round( 0.97 * deliveryCostCheapestVehicle(task)); //we intentionally make a loss to win them
        } //the 0.97 is in case someone else is doing the same thing and bidding exactly the deliveryCost

        double solutionCost = assumedSolution.getCost();
        int currentProfit =  totalRewards - (int)solutionCost;
        //System.out.println("current catchup is: " + currentProfit);
        if (currentProfit > 0) {
            //System.out.println("already ahead. making small bid with multiplier ");
            return Math.round(bidMultiplier * deliveryCostCheapestVehicle(task));
        }
        else {
            int  bid = (int)deliveryCostCheapestVehicle(task);
            int maxbid =  bid + standardBid / 2;
            return (long) bidMultiplier*  Math.min (bid - currentProfit,maxbid);
        }
    }

    @Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        List<Plan> plans = new ArrayList<Plan>();
        if (tasks.size() == 0) {//We lost all the bids.return all empty plans and hope the opponent has a negative profit
            for (Vehicle v: vehicles) {
                plans.add(Plan.EMPTY);
            }
            return plans;
        }
        //we make a plan with the new task rewards, otherwise the platform will complain
        latestSolution.worldTasks = new ArrayList<Task>(tasks);
        for (Vehicle v:vehicles) {
            plans.add (slsPlan(v, latestSolution));
        }
        return plans;
    }

    public Plan slsPlan(Vehicle vehicle, C solution) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        int taskAction=solution.firstAction[vehicle.id()];
        if (taskAction == C.NULL) {
            return Plan.EMPTY;
        }
        while (taskAction !=C.NULL) {
            City next=solution.getCityForAction(taskAction);
            if (current != next) {
                for(City city: current.pathTo(next)){
                    plan.appendMove(city);
                }
            }
            plan.append(solution.makeLogistAction (taskAction));            //append pikcup or deliver corresponding to makeLogistAction (taskAction)
            taskAction = solution.next[taskAction];
            current = next; //update city for next loop
        }
        for (Task t: solution.worldTasks)
            System.out.println(t);
        return plan;
    }
}