import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.analysis.BinDataSource;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.util.SimUtilities;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author Junxiong Wang, Zixuan Liang
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		

	//Default Value
	private static final int RABBITNUM=10;	
	private static final int GRIDX=20;
	private static final int GRIDY=20;
	private static final int GRASSNUM=100;
	private static final int RABBITINITIALENERGY=10;
	private static final int GRASSENERGY=5;	
	private static final int GRASSGROWNUM=4;
	private static final int BIRTHTHRESHOLD=30;

	private int rabbitNum = RABBITNUM;		//Initial number of rabbits
	private int gridX = GRIDX;		//X size of grid
	private int gridY = GRIDY;		//Y size of grid
	private int grassNum = GRASSNUM;		//Initial number of grass
	private int rabbitInitialEnergy = RABBITINITIALENERGY;		//Energy of a new rabbit
	private int grassEnergy = GRASSENERGY;			//Energy provided by one unit of grass
	private int grassGrowNum = GRASSGROWNUM;		//Grass growth rate
	private int birthThreshold = BIRTHTHRESHOLD;	//Rabbit reproduction energy threshold
	
	private RabbitsGrassSimulationSpace space;		
	private DisplaySurface displaySurf;
	private Schedule schedule;
	
	private ArrayList<RabbitsGrassSimulationAgent> rabbitList;		//Rabbits list
	private OpenSequenceGraph amountOfTotalEnergyInRabbit;			//Graph of total energy of rabbits
	private OpenSequenceGraph amountOfTotalRabbit;					//Graph of number of rabbits
	private OpenSequenceGraph amountOfGrass;						//Graph of number of grass

	public static void main(String[] args) {
		
		System.out.println("Rabbit skeleton");
		
		SimInit init = new SimInit();
	    RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();		//Load model
	    init.loadModel(model, "", false);
	}

	@Override
	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();
		displaySurf.display();
		amountOfTotalEnergyInRabbit.display();
		amountOfTotalRabbit.display();
		amountOfGrass.display();
	}

	@Override
	public String[] getInitParam() {
		String[] initParams = {"RabbitNum","GridX","GridY","GrassNum","GrassEnergy","RabbitInitialEnergy","GrassGrowNum","BirthShrehold"};
		return initParams;
	}

	@Override
	public String getName() {
		return "A Rabbits Grass Simulation";
	}
	
	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	@Override
	public void setup() {
		rabbitList = new ArrayList<RabbitsGrassSimulationAgent>();
		schedule = new Schedule(1);
		
		if(displaySurf != null){			//Dispose old graphs, grid and create new ones
			displaySurf.dispose();
		}
		displaySurf = null;
		displaySurf = new DisplaySurface(this, "Rabbits Grass Simulation Model Window");
	    registerDisplaySurface("Rabbits Grass Simulation Model Window", displaySurf);
		
		if(amountOfTotalEnergyInRabbit != null){
			amountOfTotalEnergyInRabbit.dispose();
		}
	    amountOfTotalEnergyInRabbit = new OpenSequenceGraph("Total Rabbit Energy",this);
	    
		if(amountOfTotalRabbit != null){
			amountOfTotalRabbit.display();
		}
		amountOfTotalRabbit = new OpenSequenceGraph("Rabbit Num", this);
	    
		if(amountOfGrass != null){
			amountOfGrass.display();
		}
		amountOfGrass = new OpenSequenceGraph("Grass Num", this);
		
		this.registerMediaProducer("Plot", amountOfTotalRabbit);
		this.registerMediaProducer("Plot", amountOfTotalEnergyInRabbit);
		this.registerMediaProducer("Plot", amountOfGrass);
		
	}
	
	public int getRabbitInitialEnergy() {
		return rabbitInitialEnergy;
	}

	public void setRabbitInitialEnergy(int rabbitInitialEnergy) {
		this.rabbitInitialEnergy = rabbitInitialEnergy;
	}

	public int getGrassEnergy() {
		return grassEnergy;
	}

	public void setGrassEnergy(int grassEnergy) {
		this.grassEnergy = grassEnergy;
	}

	public int getRabbitNum() {
		return rabbitNum;
	}

	public void setRabbitNum(int rabbitNum) {
		this.rabbitNum = rabbitNum;
	}

	public int getGridX() {
		return gridX;
	}

	public void setGridX(int gridX) {
		this.gridX = gridX;
	}

	public int getGridY() {
		return gridY;
	}

	public void setGridY(int gridY) {
		this.gridY = gridY;
	}

	public int getGrassNum() {
		return grassNum;
	}

	public void setGrassNum(int grassNum) {
		this.grassNum = grassNum;
	}

	private void buildModel(){				//Spread grass and add rabbits
		System.out.println("Model Building");
		space = new RabbitsGrassSimulationSpace(gridX,gridY,grassEnergy);
		space.spreadGrass(grassNum);
		for(int i = 0; i < rabbitNum; i++){
		   addNewAgent();
		}
		
		for(int i = 0; i < rabbitList.size(); i++){
			RabbitsGrassSimulationAgent cda = rabbitList.get(i);
	        cda.report();
		}
	}

	private void addNewAgent() {
		RabbitsGrassSimulationAgent rabbitAgent = new RabbitsGrassSimulationAgent(rabbitInitialEnergy);
		rabbitList.add(rabbitAgent);
		space.addAgent(rabbitAgent);
	}

	private void buildSchedule(){
		System.out.println("Schedule Building");
		class RabbitGrassStep extends BasicAction {		
		      @Override
			public void execute() {
		      System.out.println("update");
		      SimUtilities.shuffle(rabbitList);
		      space.spreadGrass(grassGrowNum);		//Spreads grass
		      for(int i =0; i < rabbitList.size(); i++){		//Update each rabbit's status
		          RabbitsGrassSimulationAgent rgsa = rabbitList.get(i);
		          rgsa.step();
		          if(rgsa.getEnergy()>=birthThreshold){		//Rabbits reproduction
		        	  addNewAgent();
		        	  rgsa.setEnergy(rgsa.getEnergy()-rabbitInitialEnergy);
		          }
		      }
		      
		      reapDeadAgents();		//Removes dead rabbits
		      
		      
		      for(int i = 0; i < rabbitList.size(); i++){
					RabbitsGrassSimulationAgent cda = rabbitList.get(i);
			        cda.report();
			  }
		      
		      displaySurf.updateDisplay();
		   }
		}

		
		schedule.scheduleActionBeginning(0, new RabbitGrassStep());
		
		class RabbitsCountLiving extends BasicAction {
		    @Override
			public void execute(){
		        countLivingAgents();
		      }
		    }

		schedule.scheduleActionAtInterval(10, new RabbitsCountLiving());
		
		class RabbitUpdateEnergyInSpace extends BasicAction {
		    @Override
			public void execute(){
		        amountOfTotalEnergyInRabbit.step();
		      }
		 	}

		schedule.scheduleActionAtInterval(10, new RabbitUpdateEnergyInSpace());
		
		class RabbitUpdateNumInSpace extends BasicAction {
		    @Override
			public void execute(){
		        amountOfTotalRabbit.step();
		      }
		 	}

		schedule.scheduleActionAtInterval(10, new RabbitUpdateNumInSpace());

		class GrassUpdateNumInSpace extends BasicAction {
		    @Override
			public void execute(){
		        amountOfGrass.step();
		      }
		 	}

		schedule.scheduleActionAtInterval(10, new GrassUpdateNumInSpace());
	}

	public void reapDeadAgents() {			//Removes dead rabbits if its energy is 0
		for(int i = (rabbitList.size() - 1); i >= 0 ; i--){
	      RabbitsGrassSimulationAgent cda = rabbitList.get(i);
	      if(cda.getEnergy() < 1){
	        space.removeAgentAt(cda.getX(), cda.getY());
	        rabbitList.remove(i);
	        System.out.println("dead");
	      }
	    }
	}

	public void countLivingAgents() {
		int livingAgents = 0;
	    for(int i = 0; i < rabbitList.size(); i++){
	      RabbitsGrassSimulationAgent cda = rabbitList.get(i);
	      if(cda.getEnergy() > 0){ 
	    	  livingAgents++;
	      }
	    }
	    
	    System.out.println("Number of living rabbits is: " + livingAgents);
	}

	private void buildDisplay(){
		System.out.println("Display building");
		ColorMap map = new ColorMap();
		map.mapColor(0, Color.black);		//Black is empty cell
		map.mapColor(1, Color.green);		//Green is grass cell
		
		Value2DDisplay displayGrass = new Value2DDisplay(space.getSpace(), map);
		
		Object2DDisplay displayAgents = new Object2DDisplay(space.getCurrentAgentSpace());
	    displayAgents.setObjectList(rabbitList);
		
		displaySurf.addDisplayableProbeable(displayGrass, "Grass");
		displaySurf.addDisplayableProbeable(displayAgents, "Rabbit");
		
		amountOfTotalEnergyInRabbit.addSequence("Total energy In Rabbits", new EnergyInSpace());
		amountOfTotalRabbit.addSequence("Rabbit Num", new RabbitNumInSpace());
		amountOfGrass.addSequence("Grass Num", new GrassNumInSpcace());
		
	}
	
	public int getGrassGrowNum() {
		return grassGrowNum;
	}

	public void setGrassGrowNum(int grassGrowNum) {
		this.grassGrowNum = grassGrowNum;
	}
	
   public int getBirthThreshold() {
		return birthThreshold;
	}

	public void setBirthThreshold(int birthThreshold) {
		this.birthThreshold = birthThreshold;
	}



  class EnergyInSpace implements DataSource, Sequence {

    @Override
	public Object execute() {
      return new Double(getSValue());
    }

    @Override
	public double getSValue() {
      int energy=0;
      for(RabbitsGrassSimulationAgent agent:rabbitList){
    	 energy += agent.getEnergy();
      }
      return energy;
    }
  }

  class RabbitEnergy implements BinDataSource{
    @Override
	public double getBinValue(Object o) {
      RabbitsGrassSimulationAgent rgsa = (RabbitsGrassSimulationAgent)o;
      return rgsa.getEnergy();
    }
  }
  
  class RabbitNumInSpace implements DataSource, Sequence{

	@Override
	public double getSValue() {
		return rabbitList.size();
	}

	@Override
	public Object execute() {
		return new Double(getSValue());
	}
  }
  
  class GrassNumInSpcace implements DataSource,Sequence{

	@Override
	public double getSValue() {
		return space.getGrassNum();
	}

	@Override
	public Object execute() {
		return new Double(getSValue());
	}
	  
  }
}
