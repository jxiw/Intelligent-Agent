package g16;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import g16.plan.Planning;
import g16.plan.Schedule;

/**
 * A mix between Paperino (for our predictions) and Picsou to take into account
 * what it does cost to the other player (by having a same environment) as ours.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 * @see g16.AuctionPaperino
 * @see g16.AuctionPicsou
 */
public class AuctionDewey extends AuctionBentina {

    private double minCost;
    private Planning otherCandidate;
    private Planning otherCurrent;
    private int otherReward;
    private double otherMarginalCost;

    // the prediction difference
    private double diff;

    // The learning phase size
    private int rounds = 8;
    private City[] cities;
    private double[][] segments;
    private double[][] distances;
    private double[][] expectations;

    // Learning structures
    private double capacity;
    private double costPerKm;
    private double minCostPerKm;
    private int counters[][];
    private double costs[][];

    // Current round
    private int round = 0;

    @Override
    public void setup(Topology t, TaskDistribution td, Agent a) {
        super.setup(t, td, a);

        Vehicle big = a.vehicles().get(0);
        minCostPerKm = big.costPerKm();
        for (Vehicle v : a.vehicles()) {
            if (big.capacity() < v.capacity()) {
                big = v;
            }
            minCostPerKm = Math.min(minCostPerKm, v.costPerKm());
        }

        capacity = big.capacity();
        costPerKm = big.costPerKm();

        // Smartly estimating what the other is doing.
        otherCurrent = new Planning(agent.vehicles());
        otherCandidate = null;
        otherReward = 0;
        diff = 0;

        cities = new City[topology.size()];
        segments = new double[topology.size()][topology.size()];
        distances = new double[topology.size()][topology.size()];
        expectations = new double[topology.size()][topology.size()];
        counters = new int[topology.size()][topology.size()];
        costs = new double[topology.size()][topology.size()];

        init();
    }

    private void init() {
        double total = 0;

        for (City from : topology.cities()) {
            cities[from.id] = from;
            for (City to : topology.cities()) {
                if (from.equals(to)) {
                    continue;
                }

                double p = distribution.probability(from, to);
                City f = from;
                for (City t : from.pathTo(to)) {
                    segments[f.id][t.id] += p;
                    total += p;

                    // Deform the reality about the map, so the less accessible
                    // parts of the graph are reflected in our estimation. The
                    // rational behind this is that it's very likely that we will
                    // have to travel back from such locations.
                    double distance = f.distanceTo(t);
                    switch (Math.min(f.neighbors().size(), t.neighbors().size())) {
                        case 1:
                            distance *= 1.5;
                            break;
                        case 2:
                            distance *= 1.2;
                            break;
                        default:
                            break;
                    }
                    distances[f.id][t.id] = distance;

                    f = t;
                }
            }
        }

        // Normalizing to 1 and computing expectation
        int n = rounds;
        for (int i=0; i < segments.length; i++) {
            for (int j=0; j < segments[i].length; j++) {
                segments[i][j] /= total;
                // Expectation
                double exp = 0;
                double p = segments[i][j];
                for (int k=1; p > 0 && k <= n; k++) {
                    double x = fact(n) / (fact(k) * fact(n - k));
                    x *= Math.pow(p, k) * Math.pow(1 - p, n - k);
                    exp += k * p;
                }
                expectations[i][j] = exp;
            }
        }
    }

    /**
     * Give a price prediction for the given task based on the expected
     * occurence of the path.
     *
     * @param task task to estimate
     * @return a fair price
     */
    private double getEstimateCost(Task task) {
        double price = 0;
        City from = task.pickupCity;
        for(City to : from.pathTo(task.deliveryCity)) {
            double distance = distances[from.id][to.id];
            double e = expectations[from.id][to.id];
            double cost = Math.ceil((e * task.weight) / capacity) / e;
            cost *= distance;
            price += cost * costPerKm;
            from = to;
        }
        return price;
    }

    private void learn(Task task, long bid) {
        double distance = task.pickupCity.distanceTo(task.deliveryCity);
        double value = bid / distance;
        City from = task.pickupCity;
        for (City to : from.pathTo(task.deliveryCity)) {
            double cost = costs[from.id][to.id];
            int r = counters[from.id][to.id];
            cost = (cost * r) + value;
            cost /= (r + 1);
            costs[from.id][to.id] = cost;
            counters[from.id][to.id] += 1;

            from = to;
        }
        costPerKm = (costPerKm * round) + value;
        costPerKm /= round + 1;
    }

    private double getEstimateOtherCost(Task task) {
        City from = task.pickupCity;
        double price = 0;
        for (City to : from.pathTo(task.deliveryCity)) {
            double distance = from.distanceTo(to);
            double cpk = counters[from.id][to.id] == 0 ?
                    costPerKm :
                    costs[from.id][to.id];
            double p = cpk * distance;
            price += p;
            from = to;
        }
        return price;
    }

    @Override
    public Long askPrice(Task task) {
        super.askPrice(task);

        // the assuming no movements
        minCost = task.pickupCity.distanceTo(task.deliveryCity) * minCostPerKm;

        otherCandidate = Planning.addAndSimulate(otherCurrent, task);
        otherMarginalCost = otherCandidate.getCost() - otherCurrent.getCost();

        double cost = getEstimateCost(task);
        double otherCost = getEstimateOtherCost(task);

        // Huey
        cost = Math.min(cost, minCost - 1);
        if (round > rounds) {
            // If losing
            double diff = reward - otherReward;
            boolean significant = .3 < (
                    Math.abs(diff) / (Math.abs(otherReward) + Math.abs(reward)));
            // Losing by 30% is bad.
            if (significant) {
                System.err.println("Derp: " + reward + "/" + otherReward);
                if (diff < 0) {
                    cost = otherCost;
                } else {
                    cost = Math.max(cost, (cost + otherCost) / 2);
                }
                // We could use the marginal cost, or otherMargin cost as well
            }
        }

        // The tax
        bid = Math.round(cost + 1);
        return bid;
    }

    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        super.auctionResult(previous, winner, bids);

        // The other
        for (long b : bids) {
            if (b != bid) {
                learn(previous, b);
                round += 1;

                double d = b - otherMarginalCost;
                if (winner != agent.id()) {
                    otherReward += (b - otherMarginalCost);
                    otherCurrent = otherCandidate;
                    otherCandidate = null;
                }
                // AVG difference of guess
                diff = ((round * diff) + d) / (double) (round + 1);
            }
        }
    }
}
