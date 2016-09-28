package g16;

import java.util.Arrays;
import java.util.List;
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
 * Simple price prediction that aims at predicting the future price.
 *
 * rounds is the number of expected tasks... it a bit magic for now.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 * @see g16.AuctionGreedy
 */
public class AuctionPaperino extends AuctionBentina {

    private double costPerKm;
    private double capacity;

    // Magic number
    private int rounds = 8;
    private City[] cities;
    private double[][] segments;
    private double[][] expectations;

    @Override
    public void setup(Topology t, TaskDistribution td, Agent a) {
        super.setup(t, td, a);

        Vehicle big = a.vehicles().get(0);
        for (Vehicle v : a.vehicles()) {
            if (big.capacity() < v.capacity()) {
                big = v;
            }
        }
        capacity = big.capacity();
        costPerKm = big.costPerKm();

        init();
    }

    private void init() {
        cities = new City[topology.size()];
        segments = new double[topology.size()][topology.size()];
        expectations = new double[topology.size()][topology.size()];
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
                    f = t;
                    total += p;
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
            double distance = from.distanceTo(to);
            double e = expectations[from.id][to.id];
            double cost = Math.ceil((e * task.weight) / capacity) / e;
            cost *= distance;
            price += cost * costPerKm;
            from = to;
        }
        return price;
    }

    @Override
    public Long askPrice(Task task) {
        super.askPrice(task);

        double cost = getEstimateCost(task);
        // The tax for good measure
        double tax = 1;

        bid = Math.round(cost + tax);
        return bid;
    }
}
