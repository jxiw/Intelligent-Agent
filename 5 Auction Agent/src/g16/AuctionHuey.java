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
 * It computes the expectation of obtaining some trips and tries to avoid
 * going to the edge of the graph (where degree(city) = 1).
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 * @see g16.AuctionPaperino
 */
public class AuctionHuey extends AuctionBentina {

    private double minCostPerKm;
    private double costPerKm;
    private double capacity;

    // Magic number
    private int rounds = 10;
    private City[] cities;
    private double[][] distances;
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

        minCostPerKm = Integer.MAX_VALUE;
        for (Vehicle v : a.vehicles()) {
            minCostPerKm = Math.min(minCostPerKm, v.costPerKm());
        }

        init();
    }

    private void init() {
        cities = new City[topology.size()];
        distances = new double[topology.size()][topology.size()];
        segments = new double[topology.size()][topology.size()];
        expectations = new double[topology.size()][topology.size()];
        double total = 0;

        for (City from : topology.cities()) {
            cities[from.id] = from;
            for (City to : topology.cities()) {
                if (from.equals(to)) {
                    continue;
                }
                double distance = from.distanceTo(to);
                // Cities with poor neighborhood and less likely to give nice
                // schedule, try to avoid them.
                if (to.neighbors().size() == 1) {
                    distance *= 3;
                }
                if (to.neighbors().size() == 2) {
                    distance *= 1.5;
                }
                distances[from.id][to.id] = distance;

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
            double distance = distances[from.id][to.id];
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

        double minCost = task.pickupCity.distanceTo(task.deliveryCity) * minCostPerKm;
        double cost = getEstimateCost(task);

        bid = Math.round(Math.min(cost, Math.max(marginalCost, minCost)));
        // The tax for good measure
        return bid + 1;
    }
}
