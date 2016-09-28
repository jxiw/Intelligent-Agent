
package guo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology;
public class C {
    public static final double P = 1; //  A value of p from 0.3 to 0.5 would be a good choice.
    public static ArrayList<Vehicle> agentVehicles; //global
    public ArrayList<Task> worldTasks;  //global

    public static final int DELIVER = 1;
    public static final int PICKUP = 0;
    public static final int NULL = -1;

    public static Random r = new Random();

    public static int ActionIndex (int taskId, int deliver) {
        return 2*taskId + deliver;
    }

    public static  int taskId (int actionIndex) {
        if (actionIndex < 0) {
            throw new AssertionError("Error: asking for taskId with negative actionIndex");
        }
        return actionIndex/2;
    }

    public static int deliver (int ActionIndex) {
        return ActionIndex%2;
    }

    public int[] prev;
    public int[] next;
    public int[] owner; //defaults to 0 which is ok (needs no other initialisation)
    public int[] firstAction; //defaults to 0 which is not ok (needs -1 initialisation)

    public Topology.City getCityForAction (int index) {

        Task t = worldTasks.get(taskId(index));
        if (deliver(index) == 1) {
            return t.deliveryCity;
        }
        return t.pickupCity;
    }

    public  Action makeLogistAction (int index) {
        int task = taskId(index);

        if (deliver (index) == 1) {
            return new Action.Delivery(worldTasks.get(task));
        }
        return new Action.Pickup(worldTasks.get(task));
    }

    public void reset () {
        final int length = 2*worldTasks.size();  //maybe this is done twice now unneeded, since we reset and then copy another solution most of the time
        prev = new int[length];
        if (length == 0)
            System.out.println("warning 0 size for prev and next (worldtasks size 0)");
        next = new int[length];
        owner = new int[worldTasks.size()];
        firstAction = new int[agentVehicles.size()];
        for (int i = 0; i<length; i++) {
            next[i] = i+1;
            prev[i] = i-1;
        }
        prev[0] = NULL;
        next[length-1] = NULL;

        for (int i=0; i<agentVehicles.size();i++) {
            firstAction[i] = NULL;
        }

        for(int j=0;j<worldTasks.size();j++){
            owner[j]=NULL;
        }
    }

    public void setupOnce (List<Vehicle> v, TaskSet t) {
        agentVehicles = new ArrayList<Vehicle> (v);
        worldTasks = new ArrayList<Task>(t);
        Collections.sort(worldTasks, taskComparatorId);//sort so we can iterate over tasks in increasing order of ID
        this.reset();
    }

    public void setupList (List<Vehicle> v, List<Task>  t) {
        agentVehicles = new ArrayList<Vehicle> (v);
        worldTasks = new ArrayList<Task>(t);
        Collections.sort(worldTasks, taskComparatorId); //sort so we can iterate over tasks in increasing order of ID
        this.reset();
    }



    public C selectInitialSolution() {
        C a = new C();
        a.reset();
        a.firstAction[0]=0; //vehicle with id 0 starts with action 0 (which means pickup first task)
        //vehicle 0 already owns every task from the default initialisation of the array
        for(int i=0;i<a.owner.length;i++){
            a.owner[i]=0;
        }
        return a;
    }

    public void makeInitialSolution() {
        //C a = new C();
        //a.reset();
        this.firstAction[0]=0; //vehicle with id 0 starts with action 0 (which means pickup first task)
        //vehicle 0 already owns every task from the default initialisation of the array
        for(int i=0;i<this.owner.length;i++){
            this.owner[i]=0;
        }

    }
    public  C selectInitialSolutionOld() {

        C a = new C();
        a.reset();
        int[] firstTask=new int[agentVehicles.size()]; //the first task Actionindex of each vehicle
        System.out.println("creating initial solution");
        for(Vehicle v :agentVehicles) {
            firstTask[v.id()]=worldTasks.size()*2/agentVehicles.size()*v.id();
            if(firstTask[v.id()]%2==1){     //deliver and pickup should be in the same vehicle
                firstTask[v.id()]= firstTask[v.id()]-1;
            }
            a.firstAction[v.id()] = firstTask[v.id()];
            // if(v.id()!=0&&v.id()!=agentVehicles.size()&&(a.firstAction[v.id()]-1)!=NULL){
            if(v.id()!=0&&v.id()!=agentVehicles.size()){   // need to change the pointer of vehilce's last and first task
                a.next[a.firstAction[v.id()]-1]=NULL;
                a.prev[a.firstAction[v.id()]]=NULL;}
        }

        for(Vehicle v :agentVehicles) {
            if(v.id()!=agentVehicles.size()-1) {
                for (int j = firstTask[v.id()]; j < firstTask[v.id() + 1]; j++) {
                    a.owner[taskId(j)] = v.id();
                }
            }

            else for(int m=firstTask[v.id()];m<worldTasks.size()*2;m++){
                a.owner[taskId(m)] = v.id();
            }

        }
        System.out.println("created initial solution");
        for(int n=0;n<a.owner.length;n++) {
            System.out.println(a.owner[n]);
        }

        return a;
    }


    public int chooseRandomVehicleWhichHasAction (){
        final int size = agentVehicles.size();
        int vehicleId = 0;
        int actionFound = NULL;
        while (actionFound == NULL) {
            vehicleId = (int) Math.floor(Math.random() * size);
            actionFound = firstAction[vehicleId];
        }
        return vehicleId;
    }

    public static ArrayList<C> chooseNeighbours(final C solution) {
        ArrayList<C> neighbors = new ArrayList<C>();
        int vi = solution.chooseRandomVehicleWhichHasAction();
        for (int vj = 0; vj < agentVehicles.size(); vj++) {
            if (vj != vi) {
                neighbors.add (changingVehicle1TaskRandom(solution, vi, vj)); //just 1 per vehicle per iteration
            }
        }
        //applying the Changing task order operator
        final int numberOfTasks = solution.findNumberOfTasksForVehicle(vi);
        if (numberOfTasks >= 2) {
            final int numberOfActions = 2*numberOfTasks;
            for (int tIdx1 = 0; tIdx1 < numberOfActions - 2; tIdx1++) {
                for (int tIdx2 = tIdx1 + 1; tIdx2 < numberOfActions-1; tIdx2++) {
                    C a1 = C.changingTaskOrder(solution, vi, tIdx1, tIdx2);
                    neighbors.add(a1);
                }
            }
        }
        if (neighbors.size() == 0) {
            throw new AssertionError("solutionset size 0 before reutrn in choose neighbours");
        }
        return neighbors;
    }

    int findNumberOfTasksForVehicle (int vehicleId) {
        int count = 0;
        for (int i = 0; i< worldTasks.size(); i++) {
            if (owner[i] == vehicleId) {
                count++;
            }
        }
        return count;
    }

    public static C changingVehicle1TaskRandom (final C a0, int vfrom, int vto) {
        int numberOfTasks = a0.findNumberOfTasksForVehicle(vfrom); //find the numbr of tasks in vfrom
        int sequenceId = r.nextInt(2* numberOfTasks);             //choose 1 at random
        int actionToRemove = a0.findActionNumberForVehicle(vfrom, sequenceId);
        int taskToRemove = taskId(actionToRemove); //lol... sequence Id ?
        C a1 = new C(a0);
        a1.disconnectTask(taskToRemove);         //remove its pickup and its deliver
        a1.insertTaskFirst(taskToRemove,vto);            //add those to the other
        return a1;
    }

    public void disconnectTask (int task) {
        int oldOwner = owner[task];
        int p = ActionIndex(task, PICKUP);
        int d = ActionIndex(task, DELIVER);
        if (firstAction[oldOwner] == p) {
            firstAction[oldOwner] = next[p];
            if (firstAction[oldOwner] == d) {
                firstAction[oldOwner] = next[d];
            }
        }
        owner[task] = NULL;
        connect(prev[p], next[p]); //reconnect the pre and next of the task with each other
        connect(prev[d], next[d]);
        prev[p] = prev[d] = next[p] = next[d] = NULL;
    }

    public void insertTaskFirst (int task, int vId) {
        owner[task] = vId;
        final int d = ActionIndex(task,DELIVER);
        final int p = ActionIndex(task, PICKUP);
        connect(d, firstAction[vId]);
        connect(p,d);
        prev[p]=NULL;
        firstAction[vId] = p;
    }

    public static C localChoice(C oldSolution, ArrayList<C> SolutionSet) {
        int sizeBefore = SolutionSet.size();
        
        for(int i=0;i<SolutionSet.size();i++){
        	if(!SolutionSet.get(i).constraints()){
        		SolutionSet.remove(i);
        	}
        }
        
        if (SolutionSet.size() == 0) {
            throw new AssertionError( "solutionset size 0 after constraints,  size beofre:" + sizeBefore) ;
        }
        double minCost = Collections.min(SolutionSet, solutionComparatorCost).getCost();
        
        for(int i=0;i<SolutionSet.size();i++){
        	if(SolutionSet.get(i).getCost() > minCost){
        		SolutionSet.remove(i);
        	}
        }//only leaves those with minimum cost
        
        int index = r.nextInt(SolutionSet.size());
        C randomBestSolution = SolutionSet.get(index);
        double willChange = r.nextDouble(); // between 0.0 and 0.l -- http://docs.oracle.com/javase/7/docs/api/java/util/Random.html#nextDouble()
        if(SolutionSet.size()<=0){
            return oldSolution;
        }
        if (willChange < P) { //Then with probability p it returns A, with probability 1 鈭�p it returns the current assignment Aold
            return randomBestSolution;
        }
        return oldSolution;
    }

    public double getCost() {
        double cost = 0;
        for (Vehicle v: agentVehicles){
            Topology.City current =v.getCurrentCity();
            int taskAction=firstAction[v.id()];
            while (taskAction != C.NULL) {
                long part = current.distanceUnitsTo((getCityForAction(taskAction)));
                double part2 = Measures.unitsToKM(part) * v.costPerKm();
                cost += part2;
                current=getCityForAction(taskAction);
                taskAction = next[taskAction];
            }
        }
        return cost;
    }

    public boolean checkAllTasksHandled () {
        int markPickedUp[] = new int[worldTasks.size()];
        int markDeliver[] = new int[worldTasks.size()];
        for (Vehicle v : agentVehicles) {
            int action = firstAction[v.id()];
            while (action != NULL) {
                if (deliver(action) == 1){
                    markDeliver[taskId(action)]++;
                }
                else {
                    markPickedUp[taskId(action)]++;
                }
                action = next[action];
            }
        }

        //they should all be 1 now or something is wrong along the path
        for (int i = 0; i<worldTasks.size();i++) {
            if (markDeliver[i] != 1 || markPickedUp[i] != 1) {
                return false;
            }
        }
        return true;
    }

    public boolean checkweight(Vehicle v){
        int taskAction=firstAction[v.id()];
        if (taskAction==C.NULL) {
            return true;
        }
        int currentWeight=0;
        int capacity=v.capacity();
        while(taskAction != NULL){
            int taskid=taskId(taskAction);
            Task t=worldTasks.get(taskid);
            if( deliver(taskAction)==0){  //pickup
                currentWeight=currentWeight+t.weight;
                if(currentWeight>capacity) {
                    return false;
                }
            }
            if(deliver(taskAction)==1){ //deliver
                currentWeight=currentWeight-t.weight;
            }
            taskAction=next[taskAction];
        }
        return true;
    }

    public boolean checkPickupFirst(Vehicle v) {
        int[] taskTable=new int[worldTasks.size()];
        int vehicleid=v.id();
        int taskAction=firstAction[vehicleid];
        if(taskAction==C.NULL){
            return true;
        }
        int taskid=taskId (taskAction);
        Task t = worldTasks.get(taskid);
        while(taskAction!=NULL) {
            taskid = taskId(taskAction);
            t = worldTasks.get(taskid);
            if (deliver(taskAction) == 0) {    //pickup
                taskTable[taskid] = 1;
            }
            if (deliver(taskAction) == 1) {
                if (taskTable[taskid] != 1) {
                    return false;
                }
            }
            taskAction = next[taskAction];
        }
        return true;
    }
    public boolean constraints(){
        for (Vehicle v:agentVehicles){
            if (checkweight(v) == false ){
                //System.out.println("constraint: checkweight false ");
                //System.out.println(this);
                return false;
            }
            if (checkPickupFirst(v) == false) {
                //System.out.println("constraint: pickupfirstfalse ");
                //System.out.println(this);
                return false;

            }
            if (this.checkAllTasksHandled() == false) {
                //System.out.println("constraints : alltaskhandled false");
                //System.out.println(this);
                return false;
            }

        }
        return true;
    }

    public int findActionNumberForVehicle (int vehicleId, int sequenceIndex) {
        int action = firstAction[vehicleId];  //action 0
        int i = 0;
        while (sequenceIndex > i) {
            action = next[action];
            i++;
        }
        return action;
    }

    public void connect (int action0, int action1) {
        if (action0 != NULL) {
            next[action0] = action1;
        }
        if (action1 != NULL) {
            prev[action1] = action0;
        }
        if(action1==NULL && action0 != NULL){//this can happen in some cases without being an error
            next[action0]=NULL;
        }
    }

    //TODO
    public static C changingTaskOrder(C solution, int vId, int tIdx1, int tIdx2) {//do not know the task set for vm
        C s1 = new C(solution);
        int action1 = s1.findActionNumberForVehicle(vId, tIdx1);
        int action2 = s1.findActionNumberForVehicle(vId, tIdx2);

        int pre1 = s1.prev[action1]; //could be null if it's the first action
        int pre2 = s1.prev[action2]; //cannot be null since index2 is higher than index1
        int pos1 = s1.next[action1]; //cannot be null since index2 is higher than index1
        int pos2 = s1.next[action2]; //could be null if it's the last action

        s1.connect (pre1, action2); //same always
        s1.connect (action1, pos2); //same always
        if (pos1 == action2) { //special case when they are next to each other
            s1.connect (action2,action1);
        } else {
            s1.connect(action2, pos1);  //bug if action2 == pos1
            s1.connect(pre2, action1);  //bug if pre2 == action1
        }
        if(tIdx1==0){
            s1.firstAction[vId]=action2;}
        return s1;
        //we may want to check constraints here for this vehicle. that would save time, since otherwise the constraint check will
        //be done for all the other vehicles too, which is a waste of time
    }

    public C(C a0) {
        this.prev = Arrays.copyOf(a0.prev, a0.prev.length); //deep copy of integers
        this.next = Arrays.copyOf(a0.next, a0.next.length);
        this.owner = Arrays.copyOf(a0.owner, a0.owner.length);
        this.firstAction = Arrays.copyOf(a0.firstAction, a0.firstAction.length);
        this.worldTasks= a0.worldTasks;//new ArrayList<>().copyOf(a0.worldTasks, a0.worldTasks.size())
    }

    public C() {
        //this.reset();
    }

    public static Comparator<Task> taskComparatorId = new Comparator<Task>() {
        @Override
        public int compare(Task o1, Task o2) {
            return o1.id - o2.id;
        }
    };

    public static Comparator<Vehicle> vehicleComparator = new Comparator<Vehicle>() {
        @Override
        public int compare(Vehicle o1, Vehicle o2) {
            return o1.capacity() - o2.capacity();
        }
    };

    public static Comparator<C> solutionComparatorCost = new Comparator<C>() {
        @Override
        public int compare(C o1, C o2) {
            return (int) o1.getCost() - (int) o2.getCost();
        }
    };

    @Override
	public String toString () {
        return getCost() + Arrays.toString(firstAction) + "\nowner: " + Arrays.toString(owner) + "\nnext " + Arrays.toString(next) + "\nprev " + Arrays.toString(prev);
    }




}
