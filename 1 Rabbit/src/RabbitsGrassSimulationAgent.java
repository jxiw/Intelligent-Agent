import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.
   We set the initial energy 3
	
 * @author Junxiong Wang, Zixuan Liang
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	private int signal;
	private boolean move;
	
	private static int rabbitId = 0;
	private final int id;
	private RabbitsGrassSimulationSpace space;

	private void setVxVy(){	  		//Sets direction of rabbits randomly
	    vX=0;
	    vY=0;
		int direction = (int)Math.floor(Math.random() * 4);
	    if(direction == 0){
	    	vX = -1;
	    	signal |= 0b1000;
	    }
	    else if(direction == 1){
	    	vX = 1;
	    	signal |= 0b0100;
	    }
	    else if(direction == 2){
	    	vY= -1;
	    	signal |= 0b0010;
		}
	    else if(direction == 3){
	    	vY = 1;
	    	signal |= 0b0001;
	    }
	}
	
	public void setSpace(RabbitsGrassSimulationSpace space) {
		this.space = space;
	}

	@Override
	public void draw(SimGraphics graphic) {
		graphic.drawFastRoundRect(Color.white);		//White is rabbit cell
	}

	public RabbitsGrassSimulationAgent(int rabbitInitialEnergy){
	    x = -1;
	    y = -1;
	    energy = rabbitInitialEnergy;
	    rabbitId++;
	    id=rabbitId;
	  }
	
	
	public void setXY(int x,int y){
		this.x=x;
		this.y=y;
	}
	
	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}
	
	public int getId() {
		return id;
	}

	public int getEnergy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void report(){
		System.out.println(getId() + 
                " at " + 
                x + ", " + y + 
                " has " + 
                getEnergy() + " energy");
	}

	public void step() {		//Update status of rabbits after movement
		
		int newX = 0;
	    int newY = 0;
	    signal = 0b0000;
	    
		do{
			if(signal == 0b1111){		//No available directions and break
				break;
			}
			setVxVy();
			newX = x + vX;
		    newY = y + vY;
			Object2DGrid grid = space.getCurrentAgentSpace();
		    newX = (newX + grid.getSizeX()) % grid.getSizeX();
		    newY = (newY + grid.getSizeY()) % grid.getSizeY();
		    
		}while(!tryMove(newX, newY));
		
		if(move){
			x=newX;
			y=newY;
			energy += space.obtainEnergy(newX,newY);
		}
		energy--;
	}

	private boolean tryMove(int newX, int newY){
		move = space.moveAgentAt(x, y, newX, newY);
	    return move;
	}
}
