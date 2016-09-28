package g16.plan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.HashMap;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.LogistSettings;
import logist.LogistSettings.TimeoutKey;


/**
 * Aggregation of all the plans.
 *
 * @author Yoan Blanc <yoan.blanc@epfl.ch>
 */
public class Planning {
    private Schedule[] schedules;
    private double cost;
    private boolean isValid;

    /**
     * All the vehicles and tasks.
     *
     * @param cars set of vehicles
     * @param ts   set of tasks
     */
    public Planning(List<Vehicle> cars) {
        cost = 0;
        schedules = new Schedule[cars.size()];
        int i = 0;
        isValid = true;
        for (Vehicle car : cars) {
            schedules[i] = new Schedule(i, car);
            i += 1;
        }
    }

    /**
     * Builds an identical planning using the given new set of cars and tasks.
     *
     * @param p model planning
     * @param cars new list of vehicles (must match the model one)
     * @param set new set of tasks (must match the model one)
     */
    public Planning(Planning p, List<Vehicle> cars, TaskSet set) {
        cost = 0;
        schedules = new Schedule[cars.size()];
        isValid = true;

        HashMap<Integer,Task> tasks = new HashMap<Integer,Task>();
        for (Task t : set) {
            tasks.put(t.id, t);
        }
        for (int i=0; i < p.schedules.length; i++) {
            Schedule sch = p.schedules[i];
            if (cars.get(i).id() != sch.vehicle.id()) {
                throw new IllegalArgumentException("The vehicles changed");
            }
            schedules[i] = new Schedule(i, cars.get(i));
            int j = 0;
            for (Step s : sch.steps) {
                Task t = tasks.get(s.task.id);
                Step step = null;
                switch (s.type) {
                    case PICKUP:
                        step = Step.newPickup(t);
                        break;
                    case DELIVERY:
                    default:
                        step = Step.newDelivery(t);
                        break;
                }
                schedules[i].insertAt(j, step);
                j++;
            }
            cost += schedules[i].getCost();
        }
    }

    /**
     * Private constructor for `.clone()`.
     *
     * <b>NB:</b> It does a shallow copy.
     *
     * @param p other planning
     */
    private Planning(Planning p) {
        cost = p.cost;
        isValid = p.isValid;
        schedules = new Schedule[p.schedules.length];
        System.arraycopy(p.schedules, 0, schedules, 0, schedules.length);
    }

    /**
     * Add the given task to a vehicle.
     *
     * @param task the task to add
     */
    public Schedule add(Task newTask) {
        for (int i=0; i < schedules.length; i++) {
            Schedule s = schedules[i];
            if (s.vehicle.capacity() >= newTask.weight) {
                Schedule n = (Schedule) s.clone();
                n.add(newTask);
                cost -= s.getCost();
                cost += n.getCost();
                schedules[i] = n;
                return n;
            }
        }
        return null;
    }

    /**
     * Remove the given task
     *
     * @param task the task to remove
     * @return true if successfully removed
     */
    public boolean remove(Task task) {
        for (int i=0; i < schedules.length; i++) {
            Schedule s = schedules[i];
            Schedule c = (Schedule) s.clone();
            if (c.remove(task)) {
                cost -= s.getCost();
                cost += c.getCost();
                schedules[i] = c;
                return true;
            }
        }
        return false;
    }

    /**
     * Give all the possible plans for the given task.
     *
     * @param from its parent schedule
     * @param task the task to move.
     * @return list of neighbor plans
     */
    public List<Planning> chooseNeighbors(Schedule from, Task task) {
        ArrayList<Planning> neighbors = new ArrayList<Planning>();
        for (Schedule to : schedules) {
            for (Planning p : changingTask(task, from, to)) {
                neighbors.add(p);
            }
        }
        return neighbors;
    }

    /**
     * Pick a random task and give back all the possible position for it.
     *
     * @return list of neighbor plans
     */
    public List<Planning> chooseNeighbors() {
        Schedule from = randomNonEmptySchedule();
        Task task = from.randomTask();
        return chooseNeighbors(from, task);
    }

    /**
     * Compute the cost.
     *
     * @return the total cost
     */
    public double getCost() {
        return cost;
    }

    /**
     * Tells if the planning is valid.
     *
     * @return false if some schedule are violating the constraint
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Pick a random non empty schedule from the list.
     *
     * @return non empty schedule
     */
    private Schedule randomNonEmptySchedule() {
        Schedule pick;
        Random r = new Random();
        do {
            pick = schedules[r.nextInt(schedules.length)];
        } while (pick.isEmpty());
        return pick;
    }

     /**
     * Change the task schedule and order.
     *
     * It computes all the possible task distribution for the given task by
     * moving the pickup and delivery positions.
     *
     * @param task the t to be moved.
     * @param from the schedule to pick the task from
     * @param to the schedule to put the task into
     */
    private Planning[] changingTask(Task task, Schedule from, Schedule to) {
        /*
         * j i := positions
         * | |
         * v v
         * p d - - - - -
         * p - d - - - -
         * - p d - - - -
         * p - - d - - -
         * - p - d - - -
         * - - p d - - -
         * p - - - d - -
         * - p - - d - -
         * - - p - d - -
         * - - - p d - -
         * p - - - - d -
         * ...
         * - - - - - p d
         */
        int k = 0, n = to.steps.size();
        // If the destination is different from the source, it's gonna be
        // bigger with a factor of 2 afterwards.
        if (from != to) {
            n += 2;
        }
        Planning[] plans = new Planning[(n - 1) * (n / 2)];
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                Planning p = (Planning) clone();
                Schedule newFrom = (Schedule) from.clone();
                Schedule newTo = newFrom;
                if (from != to) {
                    newTo = (Schedule) to.clone();
                }

                // Move task
                newFrom.remove(task);
                newTo.insertAt(j, Step.newPickup(task));
                newTo.insertAt(i, Step.newDelivery(task));
                p.schedules[from.id] = newFrom;
                p.schedules[to.id] = newTo;

                // Update cost
                p.cost -= from.getCost();
                p.cost += newFrom.getCost();
                p.isValid &= newFrom.isValid();
                if (from != to) {
                    p.cost -= to.getCost();
                    p.cost += newTo.getCost();
                    p.isValid &= newTo.isValid();
                }
                plans[k] = p;
                k++;
            }
        }
        return plans;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=> ");
        sb.append(Math.round(cost));
        for (Schedule s : schedules) {
            sb.append("\n  ");
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Shallow copy
     *
     * @return dolly
     */
    @Override
    public Object clone() {
        return (Object) new Planning(this);
    }

    /**
     * Time serie of the planning.
     *
     * For each schedule, return a time (dist) -> load% array.
     *
     * @return schedule's time serie for stats and stuff.
     */
    public int[][][] toTimeSerie() {
        int[][][] series = new int[schedules.length][][];
        int i = 0;
        for (Schedule s : schedules) {
            series[i] = s.toTimeSerie();
            i++;
        }
        return series;
    }

    /**
     * Generate the plan to feed back the behaviour.
     *
     * @return list of plan to from the schedules.
     */
    public List<Plan> toList() {
        ArrayList<Plan> list = new ArrayList<Plan>(schedules.length);
        for (Schedule s : schedules) {
            list.add(s.toPlan());
        }
        return list;
    }

    /**
     * Adds a task and run some simulations.
     *
     * @param current the planning to work from
     * @param task added task
     */
    public static Planning addAndSimulate(Planning current, Task task) {
        Planning candidate = (Planning) current.clone();
        Planning best = candidate;

        Schedule from = candidate.add(task);
        List<Planning> neighbors = candidate.chooseNeighbors(from, task);
        long time = new LogistSettings().get(TimeoutKey.BID);
        long end = System.currentTimeMillis() + (time / 5);
        long i = end - System.currentTimeMillis();
        long rounds = i;
        while (i > 0) {
            Planning next = Planning.localChoiceSimulatedAnnealing(candidate, neighbors, rounds / (double) i);

            if (best.getCost() > next.getCost()) {
                best = next;
            }

            candidate = next;

            neighbors = candidate.chooseNeighbors();
            i = end - System.currentTimeMillis();
        }

        return best;
    }

    /**
     * Choose to replace the old plan with the best one from the plan.
     *
     * Simulated annealing.
     *
     * @param old   the old plan
     * @param plans set of new plans
     * @param round current round
     * @return best new planning
     */
    private static Planning localChoiceSimulatedAnnealing(Planning old, List<Planning> plans, double temp) {
        ArrayList<Planning> valids = new ArrayList<Planning>();
        for (Planning plan : plans) {
            if (!plan.isValid()) {
                continue;
            }
            valids.add(plan);
        }

        if (valids.size() > 0) {
            double cost = old.getCost();
            Random rand = new Random();
            Planning best;

            int tries = valids.size();
            while (tries-- > 0) {
                best = valids.get(rand.nextInt(valids.size()));
                double badness = cost - best.getCost();
                if (badness > 0) {
                    return best;
                } else if (rand.nextDouble() < (Math.exp(badness / temp))) {
                    return best;
                }
            }
        }
        return old;
    }
}
