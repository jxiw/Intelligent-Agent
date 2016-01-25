package ourAgent;

//the list of imports

import java.util.ArrayList;
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

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 */
@SuppressWarnings("unused")
public class AuctionImpl implements AuctionBehavior {
    static int MAX_TIME = 29000; // max time in ms for bidding

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private Random random;

    private double myCurrentCost = 0;
    private A myNewSolution;
    private A[] opponentNewSolution = {null, null, null};
    private double newCost;

    SLS[] opponentSolution = {new SLS(), new SLS(), new SLS()};
    SLS mySolution;

    List<Long> opponentBids;
    List<Double> estimationRatio;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;

        // seed generation (based on ourAgent)
        long seed = -9019554669489983951L * agent.vehicles().get(0).homeCity().hashCode() * agent.id();
        this.random = new Random(seed);

        mySolution = new SLS();
        mySolution.domainVal = new DomainVal();
        // init our domain val
        mySolution.domainVal.initMyVehiclesAndActionsList(agent.vehicles());
        mySolution.domainVal.clearTasks();

        opponentBids = new ArrayList<Long>();
        estimationRatio = new ArrayList<Double>();


        // do the same for the opponent, suppose the parameters are the same
        for (int i = 0; i < 3; i++) {
            opponentSolution[i] = new SLS();
            opponentSolution[i].domainVal = new DomainVal();
            opponentSolution[i].domainVal.initShuffleMyVehiclesAndActionsList(agent.vehicles());
            opponentSolution[i].domainVal.clearTasks();
        }
    }

    void addOpponentBid(long bid) {
        opponentBids.add(bid);
    }

    long totalAcceptedOpponentBids() {
        long sum = 0;
        for (Long bid : opponentBids) {
            sum += bid;
        }
        return sum;
    }

    int roundNum = 0;
    int tasksWon = 0;
    double myTotalAcceptedBids = 0;

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        long myBid = agent.id() == 0 ? bids[0] : bids[1];

        if (winner == agent.id()) {
            tasksWon++;
            myTotalAcceptedBids += myBid;
            myCurrentCost = myNewSolution.cost();
            // update solution to start from in SLS
            mySolution.startFrom = myNewSolution;

            for (int i = 0; i < 3; i++) {
                opponentSolution[i].domainVal.removeTask(previous);
                if (opponentSolution[i].startFrom != null) opponentSolution[i].startFrom.removeTask(previous);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                opponentSolution[i].startFrom = opponentNewSolution[i];
            }
            mySolution.domainVal.removeTask(previous);
            if (mySolution.startFrom != null) mySolution.startFrom.removeTask(previous);
        }

        // see if we can increase the ratio based on opponents bid
        long opponentBid = agent.id() == 0 ? bids[1] : bids[0];

        estimationRatio.add((totalAcceptedOpponentBids() + opponentBid) /
                ((opponentNewSolution[0].cost() + opponentNewSolution[1].cost()
               + opponentNewSolution[2].cost()) / 3));
        if (winner != agent.id()) addOpponentBid(opponentBid);

        System.out.println("My bid " + myBid + " vs " + opponentBid);
        // update ratio
        int sum = 0;
        double changeRatio = 0;
        for (int ind = 0; ind < estimationRatio.size(); ind++) {
            changeRatio +=  (ind + 1) * estimationRatio.get(ind);
            sum += (ind + 1);
        }
        ratio = ((changeRatio / sum) + ratio) / 2;
        ratio = Math.min(2.5, ratio);
        ratio = Math.max(0.7, ratio);

        roundNum++;
        // lower the criteria if we are loosing a lot
        canAllow = canAllow + (winner == agent.id() ? 0.1 : -0.05);
        canAllow = Math.min(1, canAllow);
        canAllow = Math.max(0.45, canAllow);
    }

    double ratio = 1;
    double canAllow = 0.45;

    @Override
    public Long askPrice(Task task) {
        if (mySolution.startFrom != null) {
            // if cannot add to the biggest vehicle return max val in order to refuse
            if (!mySolution.startFrom.appendTaskToBiggestMyVehicle(task)) return Long.MAX_VALUE;
        }
        // add to my solution
        mySolution.domainVal.addTask(task);

        // same for the opponents
        for (int i = 0; i < 3; i++) {
            if (opponentSolution[i].startFrom != null) {
                // here it will pass always, as it is initialized with the same vehicles
                opponentSolution[i].startFrom.appendTaskToBiggestMyVehicle(task);
            }
            opponentSolution[i].domainVal.addTask(task);
        }

        // solve now both, and split available time according to the size
        int myTime = (int) Math.ceil(((double) mySolution.domainVal.actions.size() /
                (mySolution.domainVal.actions.size() +
                        opponentSolution[0].domainVal.actions.size() + opponentSolution[1].domainVal.actions.size())) * MAX_TIME);
        int opponentTime = (int) Math.ceil((MAX_TIME - myTime) / 3);
        myNewSolution = mySolution.solve(myTime);
        for (int i = 0; i < 3; i++) {
            opponentNewSolution[i] = opponentSolution[i].solve(opponentTime);
        }


        double myMarginalCost = myNewSolution.cost() - myTotalAcceptedBids;
        double opponentMarginalCost = ratio * (opponentNewSolution[0].cost() +
                opponentNewSolution[1].cost() + opponentNewSolution[2].cost()) / 3 - totalAcceptedOpponentBids();
        double bid = 0.85 * opponentMarginalCost;
        if (bid < (myMarginalCost * canAllow)) {
            bid = myMarginalCost * canAllow;
        }
        if (bid < 0) {
            bid = 1;
        }
        System.out.println("AGENT " + agent.id() + ": for round " + roundNum + " canAllow is "
                + canAllow + " and ratio " + ratio);
        System.out.println("AGENT " + agent.id() + ": for round " + roundNum + " cost is "
                + myMarginalCost + " and for opponent " + opponentMarginalCost);
        System.out.println("AGENT " + agent.id() + ": bid is " + bid);
        return (long) Math.ceil(Math.abs(bid));
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

        A finalSolution = null;
        List<Plan> plans = new ArrayList<Plan>();
        if (tasks != null && !tasks.isEmpty()) {
            mySolution.domainVal.clearTasks();
            mySolution.domainVal.initTasks(tasks);
            mySolution.startFrom = null;
            finalSolution = mySolution.solve(MAX_TIME);
        }

        if (finalSolution != null) {
            for (int i = 0; i < vehicles.size(); i++) {
                plans.add(finalSolution.getPlanForMyVehicle(i));
            }
        } else {
            for (int i = 0; i < vehicles.size(); i++) {
                plans.add(Plan.EMPTY);
            }
        }

        return plans;
    }
}