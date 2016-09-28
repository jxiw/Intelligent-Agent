import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @ Junxiong Wang, Zixuan Liang
 */

public class RabbitsGrassSimulationSpace {

	private final Object2DGrid grassSpace;	
	private final Object2DGrid rabbitSpace;
	private final int grassEnergy;
	
	public RabbitsGrassSimulationSpace(int gridX,int gridY,int grassEnergy){ 
		grassSpace = new Object2DGrid(gridX, gridY);
		rabbitSpace = new Object2DGrid(gridX, gridY);
		this.grassEnergy = grassEnergy;
		
		for(int i=0;i<gridX;i++){		//Initialize grass space with 0
			for(int j=0;j<gridY;j++){
				grassSpace.putObjectAt(i, j, new Integer(0));
				
			}
		}
	}
	
	public void spreadGrass(int grassNum){
		
		int counter=0;
		for(int i=0;i<grassNum;counter++){

	      // Choose coordinates
	      int x = (int)(Math.random()*(grassSpace.getSizeX()));
	      int y = (int)(Math.random()*(grassSpace.getSizeY()));

	      // Get the value of the object at those coordinates
	      if((Integer)grassSpace.getObjectAt(x,y) == 0){
	    	  grassSpace.putObjectAt(x,y,new Integer(1));
	    	  i++;
	      }
	      
	      if(counter>10000){
	    	  break;
	      }
	   }
	}
	
	public Object2DGrid getSpace(){
		return grassSpace;
	}
	
	public boolean isCellOccupied(int x, int y){
	    boolean retVal = false;
	    if(rabbitSpace.getObjectAt(x, y)!=null){
	    	retVal = true;
	    }
	    return retVal;
	 }

	public boolean addAgent(RabbitsGrassSimulationAgent agent){
	    boolean retVal = false;
	    while(!retVal){
	      int x = (int)(Math.random()*(rabbitSpace.getSizeX()));
	      int y = (int)(Math.random()*(rabbitSpace.getSizeY()));
	      if(isCellOccupied(x,y) == false){
	        rabbitSpace.putObjectAt(x,y,agent);
	        agent.setXY(x,y);
	        agent.setSpace(this);
	        retVal = true;
	      }
	    }
	    return retVal;
	}
	
	public Object2DGrid getCurrentAgentSpace(){
	    return rabbitSpace;
	  }

	public void removeAgentAt(int x, int y) {
		rabbitSpace.putObjectAt(x, y, null);
	}

	public int getEnergyAt(int x, int y){
	    if((Integer)grassSpace.getObjectAt(x,y) == 1){
	      return grassEnergy;
	    }
	    return 0;
	  }
	
	public int obtainEnergy(int x,int y) {
		int energy = getEnergyAt(x, y);
	    grassSpace.putObjectAt(x, y, new Integer(0));
	    return energy;
	}

	public boolean moveAgentAt(int x, int y, int newX, int newY) {
		boolean retVal = false;
	    if(!isCellOccupied(newX, newY) || newX<0 || newY<0){
	      RabbitsGrassSimulationAgent agent  = (RabbitsGrassSimulationAgent) rabbitSpace.getObjectAt(x, y);
	      removeAgentAt(x,y);
	      agent.setXY(newX, newY);
	      rabbitSpace.putObjectAt(newX, newY, agent);
	      retVal = true;
	    }
	    return retVal;
	}

	public int getGrassNum(){
	    int grassNum = 0;
	    for(int i = 0; i < grassSpace.getSizeX(); i++){
	      for(int j = 0; j < grassSpace.getSizeY(); j++){
	    	  if((Integer)grassSpace.getObjectAt(i, j) == 1){
		    	  grassNum++;  
	    	  }
	      }
	    }
	    return grassNum;
	  }
}
