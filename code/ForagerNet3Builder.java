package ForagerNet3_Demography_V3;

import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.schedule.ScheduledMethod;

public class ForagerNet3Builder implements ContextBuilder<Object> {
	
	public Model newModel = new Model (); //create a model
	
	public static int runNumber = 0;
	
	public static Context<Object> context = null;     //create a null context
	public static ContinuousSpace<Object> space = null;
	public static Grid<Object> grid = null;
	
	public static ArrayList<Model> modelList = new ArrayList<Model>();       //list to hold model instances
		
	///BUILDER
	@Override 
	public Context<Object> build (Context<Object> context) {

		++runNumber; //increment runNumber
		System.out.printf("Run Number: %d\n", runNumber);
		
		context.setId("ForagerNet3_Demography_V3");

		//DELETE OLD MODEL
		 if (modelList.size() != 0) {                        //if there's a model on the list
			 Model oldModel = modelList.get(0);                //get it
			 oldModel = null;                                //set it to null
		 }
		 
		 modelList.add(newModel);                              //add the newModel to the model list
		 		
		//CREATE SPACE

		Model.setupParams();  //call this first to establish parameters, clear lists, etc.
		int sizeX = Model.getSizeX();
		int sizeY = Model.getSizeY();

		ContinuousSpaceFactory spaceFactory =
				ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);

		ContinuousSpace <Object > space =
				spaceFactory.createContinuousSpace ("space", context ,
						new SimpleCartesianAdder <Object >() ,
						new repast.simphony.space.continuous.WrapAroundBorders (),
						sizeX, sizeY);
		GridFactory gridFactory = GridFactoryFinder.createGridFactory ( null );
		Grid <Object > grid = gridFactory.createGrid ("grid", context,
				new GridBuilderParameters <Object >( new StrictBorders (),
						new SimpleGridAdder <Object >(), true , sizeX, sizeY));

		Model.setSpaceAndGrid(space, grid);  //send the space and grid to the model class (used to be instance)
		Model.createPeople(space, grid);  	//call method to create people
			
		//Loop to add people to context
		int numPeople = Model.getInitNumPersons();
		int i = 0;
		
		for (i=0; i < numPeople; ++i ) {
			Person addPerson = Model.getPersonFromList(i);
			context.add(addPerson);
			int personX = addPerson.getX();
			int personY = addPerson.getY();
			space.moveTo(addPerson, personX, personY);
			grid.moveTo(addPerson, personX, personY);
		}
		
		context.add(newModel); //add the model to the context
						
	//	System.out.printf("BUILDER: %d people added\n", i);
	//	System.out.printf("BUILDER: context built\n");
		
		return context;
	}
	//this calls the model's step method every step
	@ScheduledMethod (start = 1, interval = 1)
	public void step() {
		newModel.step();
	}
	
	public static void nullContext() {
		
		context = null;
		space = null;
		grid = null;
	}
	
	public static int getRunNumber () {
		return runNumber;
	}
}

	




