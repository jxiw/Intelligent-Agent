package template;

import java.util.HashMap;
import java.util.LinkedList;

import logist.simulation.Vehicle;
import logist.task.Task;

public class Plan {
	
	private HashMap<Vehicle,Task> vehicleToStarTask;
	private HashMap<Task,LinkedList<Task>> nextTask;
	private HashMap<Task,Vehicle> taskToVehicle;
	private HashMap<Task,Integer> time;
	
	public HashMap<Vehicle, Task> getVehicleToStarTask() {
		return vehicleToStarTask;
	}
	public void setVehicleToStarTask(HashMap<Vehicle, Task> vehicleToStarTask) {
		this.vehicleToStarTask = vehicleToStarTask;
	}
	public HashMap<Task, LinkedList<Task>> getNextTask() {
		return nextTask;
	}
	public void setNextTask(HashMap<Task, LinkedList<Task>> nextTask) {
		this.nextTask = nextTask;
	}
	public HashMap<Task, Vehicle> getTaskToVehicle() {
		return taskToVehicle;
	}
	public void setTaskToVehicle(HashMap<Task, Vehicle> taskToVehicle) {
		this.taskToVehicle = taskToVehicle;
	}
	public HashMap<Task, Integer> getTime() {
		return time;
	}
	public void setTime(HashMap<Task, Integer> time) {
		this.time = time;
	}
	
}
