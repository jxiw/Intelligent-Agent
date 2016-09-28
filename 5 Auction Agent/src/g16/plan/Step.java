package g16.plan;

import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

/**
 * Task's step which can be pickup or delivery.
 *
 * @author Yoan Blanc
 */
public class Step {
    public final Types type;
    public final Task task;
    public final City city;
    public final int weight;

    public static enum Types {
        PICKUP {
            @Override
            public String toString() {
                return "p";
            }
        },
        DELIVERY {
            @Override
            public String toString() {
                return "d";
            }
        }
    }

    /**
     * Initiate a step.
     *
     * @param ty type of action
     * @param ta parent task
     * @param c city of action
     * @param w weight change (negative for delivery)
     */
    private Step(Types ty, Task ta, City c, int w) {
        type = ty;
        task = ta;
        city = c;
        weight = w;
    }

    public static Step newPickup(Task t) {
        return new Step(Types.PICKUP, t, t.pickupCity, t.weight);
    }

    public static Step newDelivery(Task t) {
        return new Step(Types.DELIVERY, t, t.deliveryCity, -t.weight);
    }

    /**
     * Move and do.
     *
     * @param plan the plan to act on
     * @param position the starting point
     */
    public void applyTo(Plan plan, City position) {
        for (City c : position.pathTo(city)) {
            plan.appendMove(c);
        }
        // Do
        switch (type) {
            case PICKUP:
                plan.appendPickup(task);
                break;
            case DELIVERY:
            default:
                plan.appendDelivery(task);
                break;
        }
    }

    /**
     * Give the new position / load.
     *
     * Warning: it works in time directly.
     *
     * @param time position -> load.
     */
    public void tick(int[] time, City position) {
        time[0] += position.distanceTo(city);
        switch (type) {
            case PICKUP:
                time[1] += task.weight;
                break;
            case DELIVERY:
            default:
                time[1] -= task.weight;
                break;
        }
    }

    @Override
    public String toString() {
        return type.toString() + task.id;
    }
}
