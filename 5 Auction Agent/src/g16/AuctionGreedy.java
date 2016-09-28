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
 * Super greedy agent that will never take risk and bid its marginal cost or
 * 1 (to avoid working for free). Its goal is to pick all the tasks from its
 * peers and never let them do any work.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 */
public class AuctionGreedy extends AuctionBentina {
    @Override
    public Long askPrice(Task task) {
        super.askPrice(task);
        // Never work for free.
        bid = Math.max(1, Math.round(marginalCost + 1));
        return bid;
    }
}
