package ourAgent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import ourAgent.Action.Type;

class ChosenAndBest {
    A chosenA;
    A potentialBest;
}

public class SLS {
    private final int MAX_ITERATIONS = 35000;
    private final double P = 0.5;
    // this is the latest valid solution of the SLS, a good point to start the search from
    public A startFrom = null;
    // domain values
    DomainVal domainVal;

    Random r = new Random(System.currentTimeMillis());

    public A solve(int allowedTime) {
        long startTime = System.currentTimeMillis();
        // Make initial solution
        A a = startFrom;
        if (a == null) {
            a = selectInitialSolution();
        }
        //System.out.println("Initial solution cost = " + a.cost());

        A globalBest = a;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // to prevent time out, after deadline the best solution will be found
            if (System.currentTimeMillis() - startTime > allowedTime) {
                return globalBest;
            }
            A aOld = a;

            // Generate all valid neighbours
            List<A> N = chooseNeighbours(aOld);

            // Choose next solution amongst best neighbours and old solution. Save best of those to potentialBest
            // We must always remember the best solution, even if we do not randomize it further
            ChosenAndBest lc = localChoice(aOld, N);

            // If potentialBest is better than globalBest, update globalBest
            if (lc.potentialBest.cost() < globalBest.cost()) {
                globalBest = lc.potentialBest;
            }

            // Assign chosen a to a for new iteration
            a = lc.chosenA;
        }
        System.out.println("Done in " + MAX_ITERATIONS + " iterations; final solution cost = " + globalBest.cost());
        return globalBest;
    }

    public A selectInitialSolution() {
        A init = nearestToHomeCity();
        return init;
    }

    private A allToBiggest() {
        A init = new A(domainVal);

        // Find the biggest vehicle
        MyVehicle biggestMyVehicle = domainVal.vehicles.get(0);
        for (MyVehicle v : domainVal.vehicles) {
            if (v.capacity() > biggestMyVehicle.capacity() && v.id() > biggestMyVehicle.id()) biggestMyVehicle = v;
        }

        // Assign all tasks to the biggest vehicle
        for (Action a : domainVal.actions) {
            if (a.task.weight > biggestMyVehicle.capacity()) {
                throw new RuntimeException("No vehicle can handle task with id " + a.getTask().id);
            }
            init.appendActionForMyVehicle(a, biggestMyVehicle);
        }

        return init;
    }

    private A assignRandom() {
        A init = new A(domainVal);

        // Find the biggest vehicle
        MyVehicle biggestMyVehicle = domainVal.vehicles.get(0);
        for (MyVehicle v : domainVal.vehicles) {
            if (v.capacity() > biggestMyVehicle.capacity()) biggestMyVehicle = v;
        }
        MyVehicle vehicle;

        // Assign all tasks to the biggest vehicle
        for (Action a : domainVal.actions) {
            if (a.type == Type.PICKUP) {
                if (biggestMyVehicle.capacity() < a.task.weight) {
                    throw new RuntimeException("No vehicle can handle task with id " + a.getTask().id);
                }

                vehicle = domainVal.vehicles.get(r.nextInt(domainVal.vehicles.size()));
                int i = 0;
                while (i++ < (domainVal.vehicles.size() * 10) && vehicle.capacity() < a.task.weight) {
                    vehicle = domainVal.vehicles.get(r.nextInt(domainVal.vehicles.size()));
                }
                if (vehicle.capacity() < a.task.weight) {
                    vehicle = biggestMyVehicle;
                }
                init.appendActionForMyVehicle(a, vehicle);
                init.appendActionForMyVehicle(a.oppositeAction(), vehicle);
            }
        }

        return init;
    }

    private A nearestToHomeCity() {
        A initialSolution = new A(domainVal);

        // Find the nearest (considering cost) vehicle for every pickup action (task)
        for (Action act : domainVal.actions) {
            if (act.type == Type.PICKUP) {
                double minCost = Double.MAX_VALUE;
                MyVehicle nearestMyVehicle = null;
                for (MyVehicle v : domainVal.vehicles) {
                    double cost = v.homeCity().distanceTo(act.getCity()) * v.costPerKm();
                    if (cost < minCost && v.capacity() >= act.getTask().weight) {
                        minCost = cost;
                        nearestMyVehicle = v;
                    }
                }
                if (nearestMyVehicle == null) {
                    throw new RuntimeException("No vehicle can handle task with id " + act.getTask().id);
                }
                initialSolution.appendActionForMyVehicle(act, nearestMyVehicle);
                initialSolution.appendActionForMyVehicle(act.oppositeAction(), nearestMyVehicle);
            }
        }

        return initialSolution;
    }

    private List<A> chooseNeighbours(A aOld) {
        List<A> N = new LinkedList<A>();

        Action replaceAction = null;
        // Get random V, that has a first task
        MyVehicle v = null;
        while (replaceAction == null) {
            v = domainVal.vehicles.get(r.nextInt(domainVal.vehicles.size()));
            replaceAction = aOld.vehicleActions.get(v).isEmpty() ? null : aOld.vehicleActions.get(v).get(0);
        }

        // Applying the Changing vehicle operator - move the first task of vi to all vj at the end
        for (MyVehicle vj : domainVal.vehicles) {
            if (vj != v && replaceAction.task.weight <= vj.capacity()) {
                // Can always change action, because it's the first one, has to be PICKUP
                N.add(changingMyVehicle(aOld, v, vj));
            }
        }

        // Applying the Changing task order operator
        int length = aOld.vehicleActions.get(v).size();

        if (length >= 2) {
            for (int front = 0; front < length - 1; front++) {
                for (int rear = front + 1; rear < length; rear++) {
                    A a = changingActionOrder(aOld, v, front, rear);
                    if (a != null) N.add(a);
                }
            }
        }

        return N;
    }

    /**
     * Move first task of vehicle v1 to be the last task of vehicle v2
     */
    private A changingMyVehicle(A a, MyVehicle v1, MyVehicle v2) {
        A generated = new A(a);

        // find pickup and delivery action in v1
        Action pickup = generated.vehicleActions.get(v1).get(0);
        Action delivery = pickup.oppositeAction();
        generated.removeActionForMyVehicle(pickup, v1);
        generated.removeActionForMyVehicle(delivery, v1);

        // append at the end of task list for v2
        generated.appendActionForMyVehicle(pickup, v2);
        generated.appendActionForMyVehicle(delivery, v2);

        return generated;
    }

    private A changingActionOrder(A a, MyVehicle v, int front, int rear) {
        A generated = new A(a);
        Action act1 = generated.vehicleActions.get(v).get(front);
        Action act2 = generated.vehicleActions.get(v).get(rear);

        if (canSwap(generated, v, act1, act2, front, rear)) {
            generated.replaceActionForMyVehicle(act2, v, front);
            generated.replaceActionForMyVehicle(act1, v, rear);

            int capacityTaken = 0;
            for (Action action : generated.vehicleActions.get(v)) {
                if (action.type == Type.DELIVERY) {
                    capacityTaken -= action.getTask().weight;
                } else {
                    capacityTaken += action.getTask().weight;
                }
                if (capacityTaken > v.capacity()) {
                    return null;
                }
            }
            return generated;
        } else {
            return null;
        }
    }

    private boolean canSwap(A a, MyVehicle v, Action act1, Action act2, int indAct1, int indAct2) {
        if (act1.task == act2.task) return false; // Cannot exchange pickup and corresponding delivery

        boolean res = true;
        if (act1.type == Type.PICKUP) {
            // delivery must be after the act2, where pickup will be put
            res &= a.vehicleActions.get(v).indexOf(act1.oppositeAction()) > indAct2;
        } else {
            // No problem - if delivery moves towards end, it's ok
        }

        if (act2.type == Type.DELIVERY) {
            // pickup must be before the act1, where delivery will be put
            res &= a.vehicleActions.get(v).indexOf(act2.oppositeAction()) < indAct1;
        } else {
            // No problem - if pickup moves towards start, it's ok
        }

        return res;
    }

    /**
     * Choose new A and potential best amongst all neighbours and old A.
     * New A can be worse than potential best (P chance roll).
     *
     * @param aOld
     * @param N
     * @return ChosenAndBest - chosen solution A and possible best solution (can be A or the one not chosen)
     */
    private ChosenAndBest localChoice(A aOld, List<A> N) {
        // TODO change to simulated annealing http://www.cs.ubc.ca/labs/beta/Courses/CPSC532D-02/ch2.pdf; http://profs.sci.univr.it/~farinelli/courses/ia/slides/localSearch-hnd.pdf
        // Make a list of the best solutions out of all neighbours
        List<A> bestList = new ArrayList<A>();
        double bestCost = Double.MAX_VALUE;
        for (A s : N) {
            if (s.cost() < bestCost) {
                bestList.clear();
                bestList.add(s);
                bestCost = s.cost();
            } else if (s.cost() == bestCost) {
                bestList.add(s);
            }
        }

        // Choose random amongst best neighbour solutions
        int randomBestIndex = r.nextInt(bestList.size());
        A randomBest = bestList.get(randomBestIndex);

        // Roll P. If rolled < P, return the new solution, otherwise return the old solution
        double pRoll = r.nextDouble();
        ChosenAndBest chosenAndBest = new ChosenAndBest();
        if (pRoll < P) {
            chosenAndBest.chosenA = randomBest;
        } else {
            chosenAndBest.chosenA = aOld;
        }

        // Always return the best solution out of new and old, to save it as global best
        chosenAndBest.potentialBest = (randomBest.cost() < aOld.cost()) ? randomBest : aOld;

        return chosenAndBest;
    }

}