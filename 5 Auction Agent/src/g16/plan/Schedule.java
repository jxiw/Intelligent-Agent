package g16.plan;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;


/**
 * A vehicle schedule of tasks action interleaving.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 */
public class Schedule {
    public final int id;
    public final LinkedList<Step> steps;
    public final Vehicle vehicle;
    public double cost;

    /**
     * @param index id
     * @param v the vehicle link with the schedule
     */
    public Schedule (int index, Vehicle v) {
        this(index, 0, v, new LinkedList<Step>());
    }

    private Schedule(int index, double c, Vehicle v, LinkedList<Step> s) {
        cost = c;
        id = index;
        vehicle = v;
        steps = s;
    }

    @Override
    public Object clone() {
        return (Object) new Schedule(id, cost, vehicle, new LinkedList<Step>(steps));
    }

    public double getCost() {
        return cost * vehicle.costPerKm();
    }

    /**
     * A random task from the schedule.
     *
     * @return random task
     */
    public Task randomTask() {
        Random r = new Random();
        if (!steps.isEmpty()) {
            return steps.get(r.nextInt(steps.size())).task;
        }
        return null;
    }

    /**
     * Add the given task at the beginning of the schedule.
     *
     * @param t the task to be added
     */
    public void add(Task t) {
        insertAt(0, Step.newPickup(t));
        insertAt(1, Step.newDelivery(t));
    }

    /**
     * Insert (as in shift stuff to the right) a step in the schedule.
     *
     * @param position where to insert it
     * @param step what to insert
     */
    public void insertAt(int position, Step step) {
        City a, b;
        // position is: b.
        // before: a - b
        // after:  a - step - b
        if (position == 0) {
            a = vehicle.getCurrentCity();
        } else {
            a = steps.get(position-1).city;
        }
        if (steps.size() == position) {
            b = null;
        } else {
            b = steps.get(position).city;
        }

        // Update cost
        if (b != null) {
            cost -= a.distanceTo(b);
            cost += step.city.distanceTo(b);
        }
        cost += a.distanceTo(step.city);

        // Altering the list
        ListIterator<Step> iter = steps.listIterator(position);
        iter.add(step);
    }

    /**
     * Remove the task from the schedule alltogether.
     *
     * complexity: O(|steps|)
     * @param t the task to be removed
     * @return true if the task has been removed.
     */
    public boolean remove(Task t) {
        Step p = null, d = null;
        ListIterator<Step> iter = steps.listIterator(0);
        while (iter.hasNext() && d == null) {
            Step s = iter.next();
            if (s.task.id == t.id) {
                switch (s.type) {
                    case PICKUP:
                        p = s;
                        removeAt(iter, s);
                        break;
                    case DELIVERY:
                    default:
                        removeAt(iter, s);
                        d = s;
                }
            }
        }

        return p != null && d != null;
    }

    /**
     * Remove the step at the current position.
     *
     * NB: <var>step</var> is required in order to avoid doing a the
     *     previous/next dance. (premature optimization)
     *
     * @param iter list iterator at the position of step
     * @param step the about to be removed step
     */
    public void removeAt(ListIterator<Step> iter, Step step) {
        iter.remove();
        City a, b = null;
        // a - step - b
        if (iter.hasPrevious()) {
            iter.previous();
            a = iter.next().city;
        } else {
            a = vehicle.getCurrentCity();
        }
        if (iter.hasNext()) {
            b = iter.next().city;
            iter.previous();
        }

        cost -= a.distanceTo(step.city);
        if (b != null) {
            cost -= step.city.distanceTo(b);
            cost += a.distanceTo(b);
        }
    }

    /**
     * Remove the first task it finds and returns it.
     *
     * @return the task removed
     */
    public Task removeFirst() {
        if (isEmpty()) {
            return null;
        }

        Task t = steps.get(0).task;
        return remove(t) ? t : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(vehicle.name());
        sb.append(" (");
        sb.append(vehicle.getCurrentCity());
        sb.append(")");
        sb.append(" $");
        sb.append(Math.round(cost));
        for (Step s : steps) {
            sb.append(" -> ");
            sb.append(s);
            sb.append(" (");
            sb.append(s.city);
            sb.append(")");
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * Converts the schedule into a plan.
     *
     * @return a well built plan
     */
    public Plan toPlan() {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);
        for (Step s : steps) {
            s.applyTo(plan, current);
            current = s.city;
        }
        return plan;
    }

    /**
     * Return the time serie for it.
     *
     * distance -> load% (0-100)
     *
     * @return how the load was at each step.
     */
    public int[][] toTimeSerie() {
        // position / load
        int t = 0;
        int[] tick = new int[]{0, 0};
        int[][] out = new int[steps.size() * 2 + 1][];
        City current = vehicle.getCurrentCity();

        out[t] = tick.clone();
        t++;
        for (Step s : steps) {
            int before = tick[1];
            s.tick(tick, current);
            out[t] = new int[]{tick[0],
                    (int) Math.round((before / (double) vehicle.capacity()) * 100)};
            t++;
            out[t] = new int[]{tick[0],
                    (int) Math.round((tick[1] / (double) vehicle.capacity()) * 100)};
            t++;
            current = s.city;
        }
        return out;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    /**
     * Check the constraints
     *
     * @return false if one constraint has be violated
     */
    public boolean isValid() {
        int capacity = vehicle.capacity();
        for (Step step : steps) {
            capacity -= step.weight;
            if (capacity < 0) {
                return false;
            }
        }
        return true;
    }
}
