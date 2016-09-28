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
 * Built upon `AuctionGreedy` but tries to:
 *  1) block the other guys from getting tasks
 *  2) earn as much money as it can.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 * @see g16.AuctionGreedy
 */
public class AuctionPicsou extends AuctionBentina {

    private double minCostPerKm;

    @Override
    public void setup(Topology t, TaskDistribution td, Agent a) {
        super.setup(t, td, a);

        minCostPerKm = Integer.MAX_VALUE;
        for (Vehicle v : a.vehicles()) {
            minCostPerKm = Math.min(minCostPerKm, v.costPerKm());
        }
    }

    @Override
    public Long askPrice(Task task) {
        super.askPrice(task);

        double minCost = task.pickupCity.distanceTo(task.deliveryCity) * minCostPerKm;

        // Our best vs the others' best
        bid = Math.round(Math.min(marginalCost, minCost - reward));
        // Try to win back what we've lost
        bid = Math.max(bid, -reward);
        // But never work for less than the others may, we are making an
        // educated guess here. Nothing more.
        bid = Math.max(bid, Math.round(minCost - 1));
        // The tax
        bid += 1;
        return bid;
    }
}
