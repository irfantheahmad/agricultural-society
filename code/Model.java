package ForagerNet3_Demography_V3;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import repast.simphony.batch.distributed.RepastJobImpl;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunInfo;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.SimUtilities;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.SimpleCartesianAdder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.StrictBorders;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.schedule.ScheduledMethod;

public class Model {

	private static ContinuousSpace <Object > modelSpace;
	private static Grid <Object > modelGrid;
		
	public Model () {
	}
	
	public static void setSpaceAndGrid (ContinuousSpace <Object > space , Grid <Object > grid) {
		modelSpace = space;
		modelGrid = grid;
		
	//	System.out.printf("MODEL: space/grid model constructed\n");
		
	}
	
	//DECLARE VARIABLES HERE SO THEY CAN BE USED THROUGHOUT

	//DEBUGGING
	public static int		rDebug;

	//SPACE AND TIME
	public static int     sizeX, sizeY;           // integer size of the world in grid terms
	public static int     stepsPerYear;           // number of steps that represent one year
	public static int     seasonTicks;            // track number of weeks to change seasons
	public static int	  currentTick;            // current tickCount
	
	//POPULATION PARAMETERS
	public static int 	  initNumPersons;         // initial number of people
	public static int     popMortAdjustPoint;     // pop size for positively/negatively adjusting mortality
	public static double  popMortAdjustMult;      // strength of population adjustment to mortality

	//MORTALITY AND FERTILITY PARAMETERS
	public static int     fertilitySchedule;      // age-specific fertility pattern
	public static double  fertilityMultiplier;    // to adjust baseline fertility rates
	public static int     ageAtMaturity;          // age at which persons become "adults"
	public static int     gestationWeeks;         // number of weeks for pregnancy term
	public static int     maxPPA;                 // max length of time (in steps) for postpartum amenorreha
	public static int     ageAtWeaning;           // age at which children are weaned
	public static int     ageAtWeaningWeight;     // weight of ageAtWeaning on PPA (0 = no weight)
	public static int     mortalitySchedule;      // age-specific mortality pattern
	public static double  mortalityMultiplier;    // to adjust baseline mortality rates
	public static int     maxAge;                 // adults can't get any older than this	
	public static double  nonEconInfanticideRisk; // risk for infanticide not based on household econ

	//ECONOMIC PARAMETERS
	public static int     ageAtProduction;        // age at which children become producers
	public static double  sustainableCP;          // definition of "viable" C:P ratio (dependency ratio)

	//MARRIAGE/MATING PARAMETERS
	public static int     pairBondMode;            // 1=monogamy only; 2=polygyny permitted
	public static double  pairBondStability;       // 0-1 to adjust stabiity of pair bonds (marriage)
	public static int     pairBondRestrictionMode; // define marriage restrictions based on social relationship
	public static int     numMarriageDivisions;    // number of marriage divisions in addition to "standard" taboos
	public static double  householdEconWeight;     // 0-1 adjusting weight of CP ratio in econ/pair bond calc
	public static double  upperMSLimit;            // highest that MS figure can go for calculations
	public static double  lowerMSLimit;            // lowest that MS figure can go for calculations

	//STATISTICS UPDATED EACH STEP
	public static double  eligibleMaleFemaleRatio;  // ratio of adult males to eligible females
	public static double  mateScarcity;             // number reflecting relative scarcity of males or females
	public static double  mateScarcityAdjustment;  // number 0-1 to adjust pair bond prob based on mate scarcity
	public static double  popMortAdjustment;        // adjustment to mortality rates based on population

	//"SWITCHES" used to turn things on and off and set modes for behavior rules
	public static boolean avoidanceOn;              // turn avoidance of reproduction (based on CP) on/off
	public static boolean infanticideOn;            // turn infanticide (based on CP) on/off
	public static boolean mateScarcityAdjustSwitch; // mateScarcity calculations on (true) off (false)

	//VARIABLES USED TO CONTROL DATA COLLECTION TIMES
	public static int     timePeriod;         // time period we're in (T0="burn in", T1 = data collection)
	public static int     t1Start;            // T1 start tick
	public static int     t1Stop;             // T1 stop tick
	public static int     recordingSteps;     // number of steps over which data are being recorded
	public static int     recordingYears;     // number of years over which data are being recorded

	//VARIABLES USED TO TRACK/CHECK VARIOUS MODEL OPERATIONS
	public static int     numPairBondAttempts = 0;  // number of pair bonds attempted
	public static int     numPairBonds = 0;         // number of pair bonds created
	public static int     numLinksDeleted = 0;          // number of links deleted
	public static int     numLinksCreated = 0;          // number of links created

	//CLASS-LEVEL LISTS TO STORE CURRENT "LIVE" PARTS OF SYSTEM
	public static ArrayList<Person>      personList = new ArrayList <Person> ();        // live people
	public static ArrayList<Link>        linkList = new ArrayList<Link> ();             // existing links
	public static ArrayList<Household>   householdList = new ArrayList <Household> ();  // live households

	//LISTS THAT ARE CLEARED AND REGENERATED EACH STEP TO BE USED IN OPERATIONS
	public static ArrayList<Person>      adultMaleList = new ArrayList <Person> ();     // adult males
	public static ArrayList<Person>      eligibleFemaleList = new ArrayList <Person> ();// eligible females
	public static ArrayList<Person>      pairBondOrder = new ArrayList <Person> ();     // order pair bond attempts
	public static ArrayList<Person>      addList = new ArrayList <Person> ();           // to add (births)
	public static ArrayList<Person>      subtractList = new ArrayList <Person> ();      // to subtract (deaths)
	public static ArrayList<Person>      peopleToNull = new ArrayList <Person> ();        //list to null all people at end of run
	//public static ArrayList<Link>      	linksToNull = new ArrayList <Link> ();        //list to null all links at end of run
	public static ArrayList<Household>   housesToNull = new ArrayList <Household> ();        //list to null all people at end of run
		
	//LISTS TO STORE ENTITIES FOR ANALYSIS
	public static ArrayList<Person>      t1deadList = new ArrayList <Person> ();    // dead persons during T1

	//VARIABLES USED TO HOLD SUMMARY DATA DURING DATA COLLECTION
	public static int             summaryPop;                     // cumulative sum of population size
	public static double          summaryPercentPoly;             // cumulative sum of percent polygyny
	public static int             summaryMaxHarem;                // cumulative sum of max harem size
	public static int             largestMaxHarem;                // largest harem size
	public static int             summaryNumChildrenAll;          // n births of all women
	public static int             summaryNumChildrenFull;         // n births of women surviving reproductive years
	public static int             summaryNumAdultFemales;         // n all adult females
	public static int             summaryNumFemalesSurviveRepro;  // n females surviving reproductive years
	public static int             summaryIBIweeks;                // cumulative sum of IBI (in weeks)
	public static int             summaryNumBirthIntervals;       // n of births included in sum IBI
	public static int             summaryNumMarriedWomen;         // cumulative sum n married women
	public static int             summaryAgeMarriedWomen;         // cumulative sum age married women
	public static int             summaryNumMarriedMen;           // cumulative sum n married men
	public static int             summaryAgeMarriedMen;           // cumulative sum age married men
	public static int             summaryHouseholdSize;           // cumulative sum of household size
	public static int             summaryNumHouseholds;           // cumulative sum n households
	public static int             maxHouseholdSize;               // size of largest household

	//counts of living persons entering age groups
	public static int             numAge0;
	public static int             numAge1;
	public static int             numAge2;
	public static int             numAge3;
	public static int             numAge4;
	public static int             numAge5;
	public static int             numAge6_10;
	public static int             numAge11_15;
	public static int             numAge16_20;
	public static int             numAge21_25;
	public static int             numAge26_30;
	public static int             numAge31_35;
	public static int             numAge36_40;
	public static int             numAge41_45;
	public static int             numAge46_50;
	public static int             numAge51_55;
	public static int             numAge56_60;
	public static int             numAge61_65;
	public static int             numAge66_70;
	public static int             numAge71_75;
	public static int             numAge76_80;
	public static int             numAgeOver80;

	//counts for age-specific fertility outcomes
	public static int            numBirthsAge6_10;
	public static int            numBirthsAge11_15;
	public static int            numBirthsAge16_20;
	public static int            numBirthsAge21_25;
	public static int            numBirthsAge26_30;
	public static int            numBirthsAge31_35;
	public static int            numBirthsAge36_40;
	public static int            numBirthsAge41_45;
	public static int            numBirthsAge46_50;
	public static int            numBirthsAge51_55;

	public static int            numFemalesAge6_10;
	public static int            numFemalesAge11_15;
	public static int            numFemalesAge16_20;
	public static int            numFemalesAge21_25;
	public static int            numFemalesAge26_30;
	public static int            numFemalesAge31_35;
	public static int            numFemalesAge36_40;
	public static int            numFemalesAge41_45;
	public static int            numFemalesAge46_50;
	public static int            numFemalesAge51_55;

	//DEMOGRAPHIC OUTCOMES
	public static double  livingOY;               // ratio of Old to Young adults at single point in time  
	public static double  deadOY;                 // ratio of Old to Young adults in dead population
	public static double  t1meanAge;              // mean age of population at single point in time
	public static int     stepPopLessTwo;         // step at which the population size dropped below 2
	public static int     maxPopSize;             // maximum size of population
	public static int     minPopSize;             // minimum size of population

	//SETUP PARAMS
	public static void setupParams() {
		
		//System.out.printf("MODEL: setupParams called\n");
		//SET INITIAL VALUES OF PARAMETERS
		//SPACE AND TIME
		sizeX = 50;                       // non-spatial world has a size of "1"
		sizeY = 50;                       // non-spatial world has a size of "1"
		timePeriod = 0;                  // start run in T0
		t1Start = 1;                 // T1 starts at this tick (5200 ticks = 100 years)
		t1Stop = 20800;                  // T1 stops at this tick
		recordingSteps = 0;              // set to 0;
		recordingYears = 0;              // set to 0;
		seasonTicks = 1;                 // seasonal clock to track years starts at 1
		stepsPerYear = 52;               // each step represents 1 week
		currentTick = 0;
			
		t1survived = true;

		//POPULATION SETTINGS AND MEASURES
		//initNumPersons = 300;                   // starting population size
		initNumPersons = RandomHelper.nextIntFromTo (2,500);
		popMortAdjustPoint = initNumPersons;    // +/- mortality adjustment pivots on initial pop size
		maxPopSize = 0;                         // track max population size
		minPopSize = initNumPersons;            // track min population size
		popMortAdjustment = 1;                  // adjustment to mortality based on current population size
		popMortAdjustMult = 1;
		stepPopLessTwo = 0;

		//GROWTH, AGING, FERTILITY, MORTALITY SETTINGS
		fertilitySchedule = 1;           // schedule of age-specific pregnancy probabilties; 
		fertilityMultiplier = 1;         // adjustment to baseline fertility probabilities
		ageAtMaturity = 15;              // age when persons become "adults"
		gestationWeeks = 40;             // length of gestation period in weeks
		maxPPA = 72;                     // max duration of post partum amenorrhea
		ageAtWeaning = 3;                // age that infants are weaned
		nonEconInfanticideRisk = 0;      // risk of infanticide for non-economic reasons
		mortalitySchedule = 1;           // schedule of age-specific mortality probabilities
		mortalityMultiplier = 1;         // adjustment to baseline mortality probabilities
		maxAge = 86;                     // maximum allowable age
		avoidanceOn = true;              // "true" = CP-related calculations to avoid pregnancy = on
		infanticideOn = true;            // "true" = CP-related calculations to terminate infant = on

		//ECONOMIC SETTINGS
		ageAtProduction = 14;            // age at which persons are counted as producers
		sustainableCP = 1.75;            // ratio of C:P considered to be "sustainable"
		householdEconWeight = 1;         // weight given to CP ratio in economic calculations
		
		//PAIR BOND SETTINGS
		mateScarcity = 0;                  // adjustment to pair bond probabilities based on mate scarcity
		mateScarcityAdjustSwitch = false;  // "true" = mate scarcity affects pair bond calculations
		upperMSLimit = 10;                 // upper limit to mateScarcity adjustment
		lowerMSLimit = upperMSLimit * -1;  // lower limit to mateScarcity adjustment
		pairBondMode = 2;                  // 1 = monogamy only; 2 = polygyny permitted
		pairBondRestrictionMode = 0;       // 0 = no restrictions
		// 1 = family, all kin except cousins; coresidents; stepfamily
		// 2 = family, all kin (inc. cousins); coresidents; stepfamily
		// 3 = endogamous marriage divisions + all in 1
		// 4 = family only
		numMarriageDivisions = 1;          // n of numbered marriage divisions
		pairBondStability = 1;             // p that a pair bond will dissolve

		if (t1deadList.size() != 0) { 
			for (Person deadPerson : t1deadList) {       
				deadPerson = null;                         
			}
		}
		
		if (linkList.size() != 0) { 
			for (Link deleteLink : linkList) {       
				deleteLink = null;                         
			}
		}
		
		if (householdList.size() != 0) { 
			for (Household deleteHouse : householdList) {       
				deleteHouse = null;                         
			}
		}
		
		if (personList.size() != 0) { //if there are people
			for (Person deletePerson : personList) {       //go through personList
				deletePerson = null;                         //null out the people
			}
		}
			
		Person.resetNextId();			 // reset person IDs to start at 0
		Household.resetNextId();         // reset household IDs to start at 0
		Link.resetNextId();              // reset link IDs to start at 0 
		rDebug = 0;                      // >0 turns on messages for debugging
		personList.clear();              // clear personList
		linkList.clear();                // clear linkList
		householdList.clear();           // clear householdList
		t1deadList.clear();
		//peopleToNull.clear();
		//housesToNull.clear();

		recordingSteps = 0;
		recordingYears = 0;
		summaryPop = 0;
		livingOY = 0;

		summaryIBIweeks = 0;
		summaryNumBirthIntervals = 0;
		summaryNumAdultFemales = 0;
		summaryNumFemalesSurviveRepro = 0;
		summaryNumChildrenAll = 0;
		summaryNumChildrenFull = 0;
		summaryNumMarriedWomen = 0;
		summaryNumMarriedMen = 0;
		summaryAgeMarriedWomen = 0;
		summaryAgeMarriedMen = 0;
		summaryPercentPoly = 0;
		summaryMaxHarem = 0;
		maxHouseholdSize = 0;
		summaryHouseholdSize = 0;
		summaryNumHouseholds = 0;
		largestMaxHarem = 0;

		//reset counts of living persons
		numAge0 = 0;
		numAge1 = 0;
		numAge2 = 0;
		numAge3 = 0;
		numAge4 = 0;
		numAge5 = 0;
		numAge6_10 = 0;
		numAge11_15 = 0;
		numAge16_20 = 0;
		numAge21_25 = 0;
		numAge26_30 = 0;
		numAge31_35 = 0;
		numAge36_40 = 0;
		numAge41_45 = 0;
		numAge46_50 = 0;
		numAge51_55 = 0;
		numAge56_60 = 0;
		numAge61_65 = 0;
		numAge66_70 = 0;
		numAge71_75 = 0;
		numAge76_80 = 0;
		numAgeOver80 = 0;

		numBirthsAge6_10 = 0;
		numBirthsAge11_15 = 0;
		numBirthsAge16_20 = 0;
		numBirthsAge21_25 = 0;
		numBirthsAge26_30 = 0;
		numBirthsAge31_35 = 0;
		numBirthsAge36_40 = 0;
		numBirthsAge41_45 = 0;
		numBirthsAge46_50 = 0;
		numBirthsAge51_55 = 0;

		numFemalesAge6_10 = 0;
		numFemalesAge11_15 = 0;
		numFemalesAge16_20 = 0;
		numFemalesAge21_25 = 0;
		numFemalesAge26_30 = 0;
		numFemalesAge31_35 = 0;
		numFemalesAge36_40 = 0;
		numFemalesAge41_45 = 0;
		numFemalesAge46_50 = 0;
		numFemalesAge51_55 = 0;

		//reset link and pair bond counts
		numPairBondAttempts = 0;  // number of pair bonds attempted
		numPairBonds = 0;         // number of pair bonds created
		numLinksDeleted = 0;          // number of links deleted
		numLinksCreated = 0;          // number of links created
	}

/////CREATE PEOPLE METHOD
	public static void createPeople(ContinuousSpace<Object> space , Grid<Object> grid) {
		
		//System.out.printf("%d people on t1deadList at model setup\n", t1deadList.size());
		
		int numMales = 0;                   // number of males created
		int numFemales = 0;                 // number of females created
		
		for ( int personCount = 0; personCount < initNumPersons; ++personCount ) {
			int age = RandomHelper.nextIntFromTo(ageAtMaturity, 20); // random age between limits- trying to convert this
			int sex = RandomHelper.nextIntFromTo (0, 1);                // random sex
			int md = RandomHelper.nextIntFromTo (1, numMarriageDivisions);  //random marriage division

			if (sex == 0)
				numMales = numMales + 1;                // track number of males created
			else
				numFemales = numFemales + 1;              // track number of females created

			Person person = new Person(space, grid);  // create a new person
			person.setAge (age);           // set age
			person.setSex (sex);           // set sex
			
			if (sex == 1)                  // if female
				person.setFertile(true);   // make her fertile for start-up

			person.setBirthWeek(RandomHelper.nextIntFromTo (1, 52));  // set birth week
			person.setMarriageDivision (md);                         // set marriage division
			personList.add( person );			        	         // add to personList
			peopleToNull.add(person) ;                                 //add to list to null at end of run

			person.setX(25);                      // set X of person 
			person.setY(25);                      // set Y or person
		}
	}
		
	@ScheduledMethod ( start = 1, interval = 1)
	public void step () {

	//System.out.printf("MODEL: run %d, step %d\n", ForagerNet3Builder.getRunNumber(), currentTick);

		clearLists();                   // 1. clear lists and reset variables that are refreshed each step
		calculatePopMortAdjustment(); 	// 2. calculate adjustment to mortality based on population size
		updateClock();                  // 3. update seasonal clock and currentTick
		incrementPersonAges();          // 4. call method to check/increment person ages

		SimUtilities.shuffle (personList, RandomHelper.getUniform());	//shuffle personList

		//PAIR BOND METHODS
		updateAdultMaleList();          // 5. generate list of males eligible for pair bond
		updateEligibleFemaleList();     // 6. generate list of females eligible for pair bond
		calculateMaleFemaleRatio();     // 7. calc ratio of available males : available females
		initiatePairBondMethods();      // 8. call method to initiate pair bonding methods

		//METHODS TO CATCH ERRORS
		linkTotalCheck();            // make sure total number of links matches count of links by person
		nullLinkCheck();             // makes sure no links have a null for fromPerson or toPerson
		deadLinkCheck();             // makes sure no links have a dead person for fromPerson or toPerson
		nullPersonCheck();           // makes sure no people on personList are null
		deadPersonCheck();           // makes sure no people on personList are dead

		//REPRODUCTION
		personCheckFertility();         // 9. check and adjust fertility status of persons
		personReproductionMethods();    // 10. initiate reproduction methods
		addNewPeople();                 // 11. add newborns to world
		checkPairBond();                // 12. expose pair-bonds to risk of dissolution

		//DEATH
		deathMethods();                 // 13. call death methods
		removeDeadPeople();             // 14. remove dead people from world (collect data as we do)

		//ORPHANS
		orphanMethods();                // 15. call methods to identify and relocate orphans

		purgeDeadHouseholds();          // 16. get rid of empty households
		checkHouseholdAge();            // 17. increment age of households if appropriate

		if (seasonTicks == 1) {         // only do this once per year
			updateHouseholdStats();     // 18. calculate yearly surplus/deficit of household
		}

		if (personList.size() < 2) {                    // if we're down to less than 2 people
			if (stepPopLessTwo == 0) {                  // and this hasn't been triggered yet
				stepPopLessTwo = currentTick;   		// record step
				t1survived = false;                     // set this to "false"
				recordT1Data();                         // record all the data from T1
				reportSummaryData();                    // report that data
				endRun();
			}
		}

		checkTickCount();                // 19. triggers data collection, etc.
	}

	public void endRun() {

		//System.out.printf ("%d people on peopleToNull list\n", peopleToNull.size());
		//System.out.printf ("%d houses on housesToNull list\n", housesToNull.size());

		for (Person nullPerson : peopleToNull) {  //go through list of all people
			//	Context <Object> personContext;
			//	personContext = ContextUtils.getContext(nullPerson); 

			//	if (personContext == null) {System.out.printf("context is null\n"); }
			//		if (nullPerson == null) {System.out.printf("person is null\n"); }

			//	personContext.remove(this);

			nullPerson = null;                  //null out the person
		}

		//	for (Link nullLink : linksToNull) {
		//		nullLink = null;
		//	}

		for (Household nullHouse : housesToNull) {
			//	Context <Object> houseContext;
			//	houseContext = ContextUtils.getContext(nullHouse); 
			//	houseContext.remove(this);

			nullHouse = null;
		}

		peopleToNull.clear();
		housesToNull.clear();

		ForagerNet3Builder.nullContext();            //ask the builder to null stuff

		System.gc();     //garbage collector
		RunEnvironment.getInstance().endRun(); // end the run
	}
			
	///////////////////////////////////////////////////////////////////////////////////////
	// 1. CLEAR LISTS
	//     -clears lists that are re-generated each step
	//////////////////////////////////////////////////////////////////////////////////////

	public void clearLists() {
		adultMaleList.clear();                  // all males eligible for marriage
		eligibleFemaleList.clear();             // females eligible for marriage
		pairBondOrder.clear();                  // order that males will initiate pair bond methods
		addList.clear();                        // new persons to be added
		subtractList.clear();                   // dead persons to be subtracted
	
		numPairBondAttempts = 0;                // set this to 0
		numPairBonds = 0;                       // set this to 0
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// 2. CALCULATE MORTALITY ADJUSTMENT BASED ON POPULATION SIZE
	////////////////////////////////////////////////////////////////////////////////////// 	
	public void calculatePopMortAdjustment () {

		popMortAdjustment = ((double) personList.size() / (double) popMortAdjustPoint) * popMortAdjustMult;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// 3. UPDATE CLOCK
	///////////////////////////////////////////////////////////////////////////////////////////
	public void updateClock() {

		++seasonTicks;                      // add 1 to season tick count

		if (seasonTicks > stepsPerYear) {   // if the year is over
			seasonTicks = 1;                // reset the seasonTicks clock back to the beginning
		}

		currentTick = (int) RepastEssentials.GetTickCount(); //get current run tick
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// 4. INCREMENT PERSON AGES
	///////////////////////////////////////////////////////////////////////////////////////////
	public void incrementPersonAges () {

		for (Person person : personList) {                // go through personList
			if (person.getBirthWeek() == seasonTicks)     // if it is person's birth week
				person.incrementAge();                    // make that person a year older
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 5. UPDATE ADULT MALE LIST
	/////////////////////////////////////////////////////////////////////////////////////////
	public void updateAdultMaleList() {
		for (Person person : personList) {               // go through personList
			if (person.getSex() == 0) {                  // if person is male
				if (person.getAge() >= ageAtMaturity) {  // and is mature
					adultMaleList.add(person);           // add person to adultMaleList
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// 6. UPDATE ELIGIBLE FEMALE LIST
	////////////////////////////////////////////////////////////////////////////////////////
	public void updateEligibleFemaleList() {
		for (Person person : personList) {                        // go through personList
			if (person.getSex() == 1) {                           // if person is female
				if (person.getAge() >= ageAtMaturity) {           // and is mature
					if (person.getNumCurrentMaleMates() == 0) {   // and has no current male mate
						if (person.getPregnancyWeeks() == 0) {    // and is not pregnant
							eligibleFemaleList.add(person);       // add person to eligibleFemaleList
						}
					}
				}
			}
		}
		SimUtilities.shuffle(eligibleFemaleList, RandomHelper.getUniform());  //shuffle list
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// 7. CALCULATE THE RATIO OF ADULT MALES TO ELIGIBLE FEMALES
	//////////////////////////////////////////////////////////////////////////////////////////
	public void calculateMaleFemaleRatio () {

		double previousRatio = eligibleMaleFemaleRatio;                    // preserve old ratio
		int numM = adultMaleList.size();                                   // n adult males
		int numF = eligibleFemaleList.size();                              // n eligible females
		double adj = RandomHelper.nextDoubleFromTo (0, 1);                  // value of adjustment

		if (numM != 0) {                                                   // if adult males on list
			if (numF != 0) {                                               // and eligible females
				eligibleMaleFemaleRatio = (double) numM / (double) numF;   // calc ratio
				if (eligibleMaleFemaleRatio > previousRatio)               // if ratio increased
					mateScarcity = mateScarcity + adj;                     // increase MS
				if (eligibleMaleFemaleRatio < previousRatio)               // if ratio decreased
					mateScarcity = mateScarcity - adj;                     // decrease MS
			}
			if (numF == 0)                                                 // if no eligible females
				mateScarcity = mateScarcity + adj;                         // increase MS
		}

		if (numM == 0)                                                     // if no eligible males
			mateScarcity = mateScarcity - adj;                             // decrease MS

		// cap it
		if (mateScarcity > upperMSLimit) {                         // if resulting MS is > upper limit
			mateScarcity = upperMSLimit;                           // cap it at limit
		}

		if (mateScarcity < lowerMSLimit) {                         // if resulting MS is < lower limit
			mateScarcity = lowerMSLimit;                           // bring it up to lower limit
		}

		mateScarcityAdjustment = (mateScarcity / upperMSLimit);    // MS adjustment is prop of total possible
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 8. INITIATE PAIR BOND METHODS
	//    - creates random order males will seek mates
	//    - calls person-level methods in order by list
	////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void initiatePairBondMethods () {

		if (adultMaleList.size() != 0) {                 // if there are men eligible for pair bond

			for (Person eligibleMale : adultMaleList){   // go through eligible list
				pairBondOrder.add (eligibleMale);        // add person to the pair bond order
			}

			SimUtilities.shuffle (pairBondOrder, RandomHelper.getUniform());  //shuffle list

			for (Person person : pairBondOrder)          // go through list
				person.beginPersonPairBondMethods();     // begin the person-level pair bond-related methods
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// 9. PERSON CHECK FERTILITY METHODS
	//////////////////////////////////////////////////////////////////////////////////////////////
	public void personCheckFertility() {
		for (Person person : personList) {                  // for each person on personList
			if (person.getSex() == 1) {                     // if person is female
				if (person.getAge() >= ageAtMaturity) {     // and person is mature
					person.checkFertility();                // call method to check/adjust fertility status
				}
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// 10. PERSON REPRODUCTION METHODS
	////////////////////////////////////////////////////////////////////////////////////////////
	public void personReproductionMethods() {
		for (Person person : personList) {                           // for each person on personList
			if (person.getSex() == 1) {                              // if person is female
				if (person.getAge() >= ageAtMaturity) {              // and person is mature
					int pregW = person.getPregnancyWeeks();          // get pregnancyWeeks
					if (person.getNumCurrentMaleMates() == 1 ) {     // if female has current mate . . .
						if (pregW == 0)   {                          // and if not pregnant
							if (person.getFertile() == true) {       // and if fertile
								person.pregnancy();                  // call the pregnancy method
							}
						}
					}

					if (pregW == gestationWeeks)                     // if at term
						person.giveBirth();                          // give birth

					if (pregW > 0)  {                                // if pregnant
						if (pregW < gestationWeeks) {                // but not at term
							person.setPregnancyWeeks(pregW + 1);     // increment pregnancyWeeks
						}
					}
				}			
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 11. ADD NEW PEOPLE
	//     -goes through list of newly born people to add
	//     -collects data if requested
	//     -this has to be its own step because you can't modify personList while the adults are 
	//      going through their individual steps
	//////////////////////////////////////////////////////////////////////////////////////////////////
	public void addNewPeople() {

		for (Person person : addList) {                               // for each person on addList
			personList.add(person);                                   // add to personList

			if (person.getBirthStep() >= t1Start) {                   // if person is born in T1
				if (person.getBirthStep() < (t1Stop - maxAge - 1)) {  // but soon enough to die in T1
					countLivingPerson(person);                        // A. send to tally
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 12. CHECK PAIR BOND
	//     - risk of dissolving pair bond ("marriage") based on pairBondStability
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public void checkPairBond () {

		for (Person person : personList) {                         // for each person on personList
			if (person.getNumCurrentMaleMates() == 1) {            // if person has male mate (bonded F)
				double roll = RandomHelper.nextDoubleFromTo(0,1);   // roll the dice
				if (roll > pairBondStability) {                    // if roll is greater than PB stability
					person.dissolvePairBond();                     // dissolve the pair bond
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 13. DEATH METHODS
	///////////////////////////////////////////////////////////////////////////////////////////////

	public void deathMethods() {
		for (Person person : personList)            // for each living person
			person.death();                         // call death method
	}


	////////////////////////////////////////////////////////////////////////////////////////////////
	// 14. REMOVE DEAD PEOPLE
	//     -removes dead people from households and personList 
	//     -collects data on people as they are removed if we're in a data collection period
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void removeDeadPeople() {
		if (subtractList.size() != 0) {                          // if there are people to remove
			for (Person deadPerson : subtractList) {             // for each person on list
				Household personHouse = deadPerson.getCurrentHousehold();  // get person's household

				if (personHouse != null)                         // if person is in a household
					personHouse.removeSelf(deadPerson);          // remove person from household

				int age = deadPerson.getAge();                   // get person's age 
				deadPerson.setAgeAtDeath(age);                   // set age at death
				deadPerson.setLive(false);                       // set status to dead

				if (deadPerson.getSex() == 1) {                          // if person is female
					if (deadPerson.getAge() >= ageAtMaturity) {          // and mature
						if (deadPerson.getNumCurrentMaleMates() == 1) {  // if she had mate
							Person maleMate = deadPerson.getMaleMate(0); // get him
							maleMate.removeFemaleMate(deadPerson);       // remove her from his list
						}
					}
				}

				if (deadPerson.getSex() == 0) {                                   // if person is male
					if (deadPerson.getAge() >= ageAtMaturity) {                   // and mature
						if (deadPerson.getNumCurrentFemaleMates() > 0) {          // if he had mate(s)
							int numMates = deadPerson.getNumCurrentFemaleMates(); // get number
							for (int i = 0; i < numMates; ++i) {                  // go through list
								Person femaleMate = deadPerson.getFemaleMate(i);  // get mate
								femaleMate.removeMaleMate (deadPerson);           // remove him from her list
							}
						}
					}
				}

				deadPerson.removeLinksToMe();                    // remove all links to/from person

				if (currentTick >= t1Start) {                       // if we're in T1

					if (deadPerson.getAge() >= ageAtMaturity) {        // if person is adult
						t1deadList.add(deadPerson);                        // add person to list of T1 dead
					}

					if (deadPerson.getSex() == 1) {                    // if it's a female
						if (deadPerson.getAge() >= ageAtMaturity) {    // of reproductive age
							++summaryNumAdultFemales;                  // add to total of women
							int numChildren = deadPerson.getNumChildren();   // n of children
							summaryNumChildrenAll = summaryNumChildrenAll + numChildren;  //add to total 

							if (numChildren > 1) {                     // if female had mutliple children
								countForIBI (deadPerson);              // send to calc IBI
							}

							if (deadPerson.getAge() >= 45) {           // if survived most repro years	
								++summaryNumFemalesSurviveRepro; //add to total of women surviving repro years
								summaryNumChildrenFull = summaryNumChildrenFull + numChildren;
							} 
						}

						if (deadPerson.getAgeAtMarriage() != 0) {      // if F was married
							++summaryNumMarriedWomen;                  // add to summary n of married F
							summaryAgeMarriedWomen = summaryAgeMarriedWomen + deadPerson.getAgeAtMarriage();
						}
					}

					if (deadPerson.getSex() == 0) {                    // if person was male
						if (deadPerson.getAgeAtMarriage() != 0) {      // if he was married
							++summaryNumMarriedMen;                    // add to summary n of married M
							summaryAgeMarriedMen = summaryAgeMarriedMen + deadPerson.getAgeAtMarriage();
						} 
					}
				}
				
				Context <Object> context;                                 
				context = ContextUtils.getContext(this); 				//get person's context
				context.remove(deadPerson); 							// remove person from context
				
				personList.remove(deadPerson);                          // remove person from personList
	                  								}
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// 15. ORPHAN METHODS
	///////////////////////////////////////////////////////////////////////////////////////////
	public void orphanMethods() {

		for (Person person : personList) {                  // for each person on personList
			if (person.checkOrphanStatus() == true) {       // if person is an orphan
				person.reHouseOrphan();                     // rehouse the orphan
			}
		}	
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 16. PURGE DEAD HOUSEHOLDS
	////////////////////////////////////////////////////////////////////////////////////////////
	public void purgeDeadHouseholds () {

		ArrayList <Household> removeHouseholdList = new ArrayList<Household> (); //households to remove

		for (Household household : householdList) {        // for each household on householdList
			if	(household.getSize() == 0) {               // if household has no members
				removeHouseholdList.add(household);        // add it to list to remove
			}
		}

		for (Household deadhouse : removeHouseholdList) {  // for each house on removeList
			householdList.remove(deadhouse);               // take it off the live list
		
			Context <Object> context;
			context = ContextUtils.getContext(deadhouse); //get household's context
			context.remove(deadhouse);
		}
		removeHouseholdList.clear();
		removeHouseholdList = null;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// 17. CHECK HOUSEHOLD AGE
	//////////////////////////////////////////////////////////////////////////////////////
	public void checkHouseholdAge() {

		for (Household household : householdList) {         // for each household on householdList
			if (household.getBirthWeek() == seasonTicks) {  // if it's the household's anniversary
				household.incrementHouseholdYear();         // increment the year (age)
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// 18. UPDATE HOUSEHOLD STATS
	///////////////////////////////////////////////////////////////////////////////////////
	public void updateHouseholdStats() {

		for (Household household : householdList) {      // for each household on householdList
			household.calculateSurplusForYear();         // calc the CP ratio, update peak size and peak CP
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// 19. CHECK TICK COUNT
	//      -compares to tickCount to the various stop/start/change points that are established in setup
	////////////////////////////////////////////////////////////////////////////////////////////
	public void checkTickCount() {

		if (getSeasonTicks() == 1)
			//reportNumLinks();                       // reports pop size, etc.

			if (currentTick == t1Start) {        // if reached start of T1
				timePeriod = 1;                     // set this to "1"
			}

		if (currentTick >= t1Start) {        // if we're past the "burn in" period 
			if (currentTick <= t1Stop) {     // and still in T1
				collectSummaryData();           // collect summary data
				if (personList.size() > maxPopSize) {      // if pop is higher than previous max
					maxPopSize = personList.size();        // record it
				}
				if (personList.size() < minPopSize) {      // if pop is lower than previous min
					minPopSize = personList.size();        // record it
				}
			}
		}

		int demoDataTick = (t1Stop - t1Start) / 2 + t1Start;   //halfway through T1

		if (currentTick == demoDataTick) {   // if halfway through T1
			//reportDemographicData();        // report demographic data in detail
			calculateLivingOY();                // calculate living OY ratio
			calculateMeanAgeT1();               // calculate the mean age of the living population
		}

		if (currentTick == t1Stop) {         // if we're at the end of T1
			stepPopLessTwo = t1Stop;            // set this to end of run (i.e., population survived)
			recordT1Data();                     // record data (recording and reporting methods at end)
			reportSummaryData();                // report data (recording and reporting methods at end)
			endRun();
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 20. COUNT LIVING PERSON
	// -counts living people by age category for purposes of age-specific mortality
	// - when a person is added in T1, he/she is sent here for tallying
	// - when a person ages in T1, he/she sent here for tallying
	/////////////////////////////////////////////////////////////////////////////////////////////

	public static void countLivingPerson (Person person) {

		if (person.getAge() == 0 ) ++numAge0;                  //if person is this age, increment this count
		if (person.getAge() == 1 ) ++numAge1;
		if (person.getAge() == 2 ) ++numAge2;
		if (person.getAge() == 3 ) ++numAge3;
		if (person.getAge() == 4 ) ++numAge4;
		if (person.getAge() == 5 ) ++numAge5;
		if (person.getAge() == 6 ) { ++numAge6_10;
		if (person.getSex() == 1) { ++numFemalesAge6_10; }  //if female, also add to this count
		}
		if (person.getAge() == 11) { ++numAge11_15;
		if (person.getSex() == 1) { ++numFemalesAge11_15; }
		}
		if (person.getAge() == 16) { ++numAge16_20;
		if (person.getSex() == 1) { ++numFemalesAge16_20; }
		}
		if (person.getAge() == 21) { ++numAge21_25;
		if (person.getSex() == 1) { ++numFemalesAge21_25; }
		}
		if (person.getAge() == 26) { ++numAge26_30; 
		if (person.getSex() == 1) { ++numFemalesAge26_30; }
		}
		if (person.getAge() == 31) { ++numAge31_35;
		if (person.getSex() == 1) { ++numFemalesAge31_35; }
		}
		if (person.getAge() == 36) { ++numAge36_40; 
		if (person.getSex() == 1) { ++numFemalesAge36_40; }
		}
		if (person.getAge() == 41) { ++numAge41_45;
		if (person.getSex() == 1) { ++numFemalesAge41_45; }
		}
		if (person.getAge() == 46) { ++numAge46_50;
		if (person.getSex() == 1) { ++numFemalesAge46_50; }
		}
		if (person.getAge() == 51) { ++numAge51_55;
		if (person.getSex() == 1) { ++numFemalesAge51_55; }
		}
		if (person.getAge() == 55) ++numAge56_60; 
		if (person.getAge() == 61) ++numAge61_65; 
		if (person.getAge() == 66) ++numAge66_70; 
		if (person.getAge() == 71) ++numAge71_75; 
		if (person.getAge() == 76) ++numAge76_80; 
		if (person.getAge() == 80) ++numAgeOver80;
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 21. GET BASE FERTILITY
	//////////////////////////////////////////////////////////////////////////////////////////
	public static double getBaseFertility (int personAge) {

		double baseFertility = 0;                // variable to hold fertility

		if (fertilitySchedule == 1) {           
			if (personAge < 6) { baseFertility = 0; }                           //age < 6
			if (personAge > 5) { if (personAge < 11) baseFertility = 0; }       //age 6-10
			if (personAge > 10) { if (personAge < 16) baseFertility = 0.01; }   //age 11-15
			if (personAge > 15) { if (personAge < 21) baseFertility = 0.15; }   //age 16-20
			if (personAge > 20) { if (personAge < 26) baseFertility = 0.25; }   //age 21-25
			if (personAge > 25) { if (personAge < 31) baseFertility = 0.28; }   //age 26-30
			if (personAge > 30) { if (personAge < 36) baseFertility = 0.28; }   //age 31-35
			if (personAge > 35) { if (personAge < 41) baseFertility = 0.25; }   //age 36-40
			if (personAge > 40) { if (personAge < 46) baseFertility = 0.15; }   //age 41-45
			if (personAge > 45) { if (personAge < 51) baseFertility = 0.08; }   //age 46-50
			if (personAge > 50) { if (personAge < 56) baseFertility = 0.01; }   //age 51-55
			if (personAge > 55) { baseFertility = 0;}                           //age > 55
		}

		if (fertilitySchedule == 2) {            // fertilitySchedule 2 = chimp-like
			if (personAge < 6) { baseFertility = 0; }                           //age < 6
			if (personAge > 5) { if (personAge < 11) baseFertility = 0.01; }    //age 6-10
			if (personAge > 10) { if (personAge < 16) baseFertility = 0.16; }   //age 11-15
			if (personAge > 15) { if (personAge < 21) baseFertility = 0.21; }   //age 16-20
			if (personAge > 20) { if (personAge < 26) baseFertility = 0.22; }   //age 21-25
			if (personAge > 25) { if (personAge < 31) baseFertility = 0.23; }   //age 26-30
			if (personAge > 30) { if (personAge < 36) baseFertility = 0.21; }   //age 31-35
			if (personAge > 35) { if (personAge < 41) baseFertility = 0.18; }   //age 36-40
			if (personAge > 40) { if (personAge < 46) baseFertility = 0.14; }   //age 41-45
			if (personAge > 45) { if (personAge < 51) baseFertility = 0.08; }   //age 46-50
			if (personAge > 50) { if (personAge < 56) baseFertility = 0.02; }   //age 51-55
			if (personAge > 55) { baseFertility = 0;}                           //age > 55
		}


		baseFertility = baseFertility * fertilityMultiplier;          //adjust based on parameter setting

		return baseFertility;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// 22. CREATE CHILD
	/////////////////////////////////////////////////////////////////////////////////////
	public static Person createChild (Person mother) {
		int birthStep = (int) currentTick;
		Person child = new Person(modelSpace , modelGrid);                     // create new person
		int sex = RandomHelper.nextIntFromTo (0, 1);      // random sex
		int marriageDivision = RandomHelper.nextIntFromTo (1, numMarriageDivisions);  //random marriage div
		child.setAge (0);                                // set age
		child.setSex (sex);                              // set sex
		child.setBirthWeek(seasonTicks);                 // set birth week
		child.setBirthStep (birthStep);                  // set birth step
		child.setMarriageDivision(marriageDivision);     // set marriage division
		addList.add (child);			        	     // add to addList
		peopleToNull.add (child);                        // add to list to null at end of run
		

		if (currentTick >= t1Start) {                 // if we're collecting stats
			int motherAge = mother.getAge();             // get mother's age

			if (motherAge > 5) {if (motherAge < 11) ++numBirthsAge6_10; }     //increment appropriate tally
			if (motherAge > 10) {if (motherAge < 16) ++numBirthsAge11_15; }
			if (motherAge > 15) {if (motherAge < 21) ++numBirthsAge16_20; }
			if (motherAge > 20) {if (motherAge < 26) ++numBirthsAge21_25; }
			if (motherAge > 25) {if (motherAge < 31) ++numBirthsAge26_30; }
			if (motherAge > 30) {if (motherAge < 36) ++numBirthsAge31_35; }
			if (motherAge > 35) {if (motherAge < 41) ++numBirthsAge36_40; }
			if (motherAge > 40) {if (motherAge < 46) ++numBirthsAge41_45; }
			if (motherAge > 45) {if (motherAge < 51) ++numBirthsAge46_50; }
			if (motherAge > 50) {if (motherAge < 56) ++numBirthsAge51_55; }
		}

		return child;                                    // send child back to Person
	}


	//////////////////////////////////////////////////////////////////////////////////////
	// 23. GET BASE MORTALITY
	// -called from Person to look up base age-specific mortality rate
	////////////////////////////////////////////////////////////////////////////////////////
	public static double getBaseMortality (int personAge) {

		double baseMortality = 0;                           // variable to hold mortality

		if (mortalitySchedule == 1) {                       
			if (personAge == 0) { baseMortality = 0.07; }                       // age < 1
			if (personAge == 1) { baseMortality = 0.07; }                       // age 1
			if (personAge == 2) { baseMortality = 0.06; }                       // age 2
			if (personAge == 3) { baseMortality = 0.05; }                       // age 3
			if (personAge == 4) { baseMortality = 0.04; }                       // age 4
			if (personAge == 5) { baseMortality = 0.03; }                       // age 5
			if (personAge > 5) { if (personAge < 11)  baseMortality = 0.02; }   // age 6-10
			if (personAge > 10) { if (personAge < 16) baseMortality = 0.015; }  // age 11-15
			if (personAge > 15) { if (personAge < 21) baseMortality = 0.015; }  // age 16-20
			if (personAge > 20) { if (personAge < 26) baseMortality = 0.015; }  // age 21-25
			if (personAge > 25) { if (personAge < 31) baseMortality = 0.015; }  // age 26-30
			if (personAge > 30) { if (personAge < 36) baseMortality = 0.015; }  // age 31-35
			if (personAge > 35) { if (personAge < 41) baseMortality = 0.015; }  // age 36-40
			if (personAge > 40) { if (personAge < 46) baseMortality = 0.018; }  // age 41-45
			if (personAge > 45) { if (personAge < 51) baseMortality = 0.02; }   // age 46-50
			if (personAge > 50) { if (personAge < 56) baseMortality = 0.03; }   // age 51-55
			if (personAge > 55) { if (personAge < 61) baseMortality = 0.04; }   // age 56-60
			if (personAge > 60) { if (personAge < 66) baseMortality = 0.08; }   // age 61-65
			if (personAge > 65) { if (personAge < 71) baseMortality = 0.12; }   // age 66-70
			if (personAge > 70) { if (personAge < 76) baseMortality = 0.2; }    // age 71-75
			if (personAge > 75) { baseMortality = 0.3; }                        // age > 75
			if (personAge > maxAge) { baseMortality = 1;}                       // age > max allowed
		}

		if (mortalitySchedule == 2) {            // mortalitySchedule 2 = chimp-like
			if (personAge == 0) { baseMortality = 0.2; }                        //age < 1
			if (personAge == 1) { baseMortality = 0.14; }                       //age 1
			if (personAge == 2) { baseMortality = 0.12; }                       //age 2
			if (personAge == 3) { baseMortality = 0.1; }                        //age 3
			if (personAge == 4) { baseMortality = 0.08; }                       //age 4
			if (personAge == 5) { baseMortality = 0.06; }                       //age 5
			if (personAge > 5) { if (personAge < 11) baseMortality = 0.05; }    //age 6-10
			if (personAge > 10) { if (personAge < 16) baseMortality = 0.04; }   //age 11-15
			if (personAge > 15) { if (personAge < 21) baseMortality = 0.045; }  //age 16-20
			if (personAge > 20) { if (personAge < 26) baseMortality = 0.05; }   //age 21-25
			if (personAge > 25) { if (personAge < 31) baseMortality = 0.06; }   //age 26-30
			if (personAge > 30) { if (personAge < 36) baseMortality = 0.08; }   //age 31-35
			if (personAge > 35) { if (personAge < 41) baseMortality = 0.13; }   //age 36-40
			if (personAge > 40) { if (personAge < 46) baseMortality = 0.2; }    //age 41-45
			if (personAge > 45) { if (personAge < 51) baseMortality = 0.3; }    //age 46-50
			if (personAge > 50) { if (personAge < 56) baseMortality = 0.4; }    //age 51-55
			if (personAge > 55) { baseMortality = 0.5; }                        //age > 55
			if (personAge > maxAge) { baseMortality = 1;}                       //age > max allowed
		}

		if (mortalitySchedule == 3) {            // mortalitySchedule 3
			if (personAge == 0) { baseMortality = 0.07; }                       //age < 1
			if (personAge == 1) { baseMortality = 0.07; }                       //age 1
			if (personAge == 2) { baseMortality = 0.06; }                       //age 2
			if (personAge == 3) { baseMortality = 0.05; }                       //age 3
			if (personAge == 4) { baseMortality = 0.04; }                       //age 4
			if (personAge == 5) { baseMortality = 0.03; }                       //age 5
			if (personAge > 5) { if (personAge < 11) baseMortality = 0.03; }    //age 6-10
			if (personAge > 10) { if (personAge < 16) baseMortality = 0.04; }   //age 11-15
			if (personAge > 15) { if (personAge < 21) baseMortality = 0.10; }   //age 16-20
			if (personAge > 20) { if (personAge < 26) baseMortality = 0.15; }   //age 21-25
			if (personAge > 25) { if (personAge < 31) baseMortality = 0.25; }   //age 26-30
			if (personAge > 30) { if (personAge < 36) baseMortality = 0.30; }   //age 31-35
			if (personAge > 35) { if (personAge < 41) baseMortality = 0.25; }   //age 36-40
			if (personAge > 40) { if (personAge < 46) baseMortality = 0.2; }    //age 41-45
			if (personAge > 45) { if (personAge < 51) baseMortality = 1; }      //age 46-50
			if (personAge > 50) { if (personAge < 56) baseMortality = 1; }      //age 51-55
			if (personAge > 55) { baseMortality = 1; }                          //age > 55
			if (personAge > maxAge) { baseMortality = 1;}                       //age > max allowed
		}

		baseMortality = baseMortality * mortalityMultiplier;          // adjust based on parameter setting

		return baseMortality;                                         //return result
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// 24. KILL PERSON
	//////////////////////////////////////////////////////////////////////////////////////
	public static void killPerson (Person x) {
		subtractList.add (x);                 //add dead person to list of people to subtract from world
	}


	///////////////////////////////////////////////////////////////////////////////////////
	// 25. COUNT FOR IBI
	// -calculates interval (in weeks) between female's childrens' births
	// -adds to total
	/////////////////////////////////////////////////////////////////////////////////////////
	public void countForIBI (Person female) {

		int numChildren = female.getNumChildren();        // get num children

		for (int i = 0; i < numChildren - 1; ++i) {       // loop to calculate IBIs
			Person childA = female.getChild(i);           // get first child
			Person childB = female.getChild(i + 1);       // get second child
			int birthStepA = childA.getBirthStep();       // get birth of first child
			int birthStepB = childB.getBirthStep();       // get birth of second child
			int interval = birthStepB - birthStepA;       // steps between births
			summaryIBIweeks = summaryIBIweeks + interval; // add to total
			++summaryNumBirthIntervals;                   // increment count
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// 26. COLLECT SUMMARY DATA
	//   -called every step during data collection period to collect data
	////////////////////////////////////////////////////////////////////////////////////////
	public void collectSummaryData() {
		++recordingSteps;                                // increment number of steps in recording period 
		summaryPop = summaryPop + personList.size();     // add the current population to the summary

		int maxHarem = 1;                 // temp variable to hold largest harem size
		double percentPoly = 0;           // temp variable to hold percent polygyny
		int numPB = 0;                    // temp variable to hold number of pair bonds (marriages)
		int numPolyPB = 0;                // temp variable to hold number of polygynous households

		for (Person adultMale : adultMaleList) {                   // go through adultMaleList
			int numMates = adultMale.getNumCurrentFemaleMates();   // get current number of mates/wives
			if (numMates != 0)                                     // if man currently has mate(s)
				++numPB;                                           // add 1 to number of marriages
			if (numMates > 1)                                      // if man has more than 1 mate
				++numPolyPB;                                       // add 1 to poly marriage count
			if (numMates > maxHarem)                               // if this is the biggest harem yet
				maxHarem = numMates;                               // record it
			if (maxHarem > largestMaxHarem) {                      // if this is the biggest harem this run
				largestMaxHarem = maxHarem;                        // record it
			}
		}

		summaryMaxHarem = summaryMaxHarem + maxHarem;       //add to total for meanIntensityPoly calc

		if (numPB != 0) {                                             // if pair bonds are present
			percentPoly = (double) numPolyPB / (double) numPB * 100;  // calc percent that are polygynous
			summaryPercentPoly = summaryPercentPoly + percentPoly;    // add to cumulative sum
		}

		//update summary stats for household size data - only include households with size of 2 or more.
		for (Household household : householdList) {                  // for each household on list
			int size = household.getSize();                          // get size

			if (size > 1) {                                          // if more than two persons
				if (household.getAdultMale() != null) {              // and if there is a "father"
					summaryHouseholdSize = summaryHouseholdSize + size;  //add to cumulative sum of size
					++summaryNumHouseholds;                          // increment number we've counted

					if (size > maxHouseholdSize) {                   // if this is the biggest one yet
						maxHouseholdSize = size;                     // record it
					}
				}
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// 27. REPORT DEMOGRAPHIC DATA
	//	 -reports age, sex, and number of links for each living person
	//////////////////////////////////////////////////////////////////////////////////////////////
		public void reportDemographicData () {
			for (Person person  : personList) {                                   // go through personList
	
				String s = String.format ( "%d  ", ForagerNet3Builder.getRunNumber());               // run number
				s += String.format ( "%d" , currentTick);
				s += String.format ( "%d ", timePeriod);                          // time period
				s += String.format ( "%d ", person.getId());                      // person's ID
				s += String.format ( "%d ", person.getSex());                     // person's sex
				s += String.format ( "%d ", person.getAge());                     // person's age
				s += String.format ( "%d ", person.getNumLinks());                // number of all links
				s += String.format ( "%d ", person.getNumKinLinks());             // number of kin links
				s += String.format ( "%d ", person.getNumCurrentFemaleMates());   // number of female mates
				doAppendDemographicData (s);
			}
		}

		public void doAppendDemographicData (String s) {
			try {
				String fileName = "DemographicData.txt";
				BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
				out.write(s);
				out.write("\n");
				out.close();
	
			} 
			catch (IOException e) {
				System.out.println("IOException:");
				e.printStackTrace();
			}
		}	

	//////////////////////////////////////////////////////////////////////////////////////
	// 28. CALCULATE LIVING OY RATIO
	//    - goes through current person list 
	//    - counts number of old, number of young,
	//    - computes and stores ratio of OY
	////////////////////////////////////////////////////////////////////////////////////////
	public void calculateLivingOY() {

		int livingYoungCount = 0;                           // count of living young adults
		int livingOldCount = 0;                             // count of living old adults

		for (Person person : personList) {                  // go through personList
			if (person.getAge() >= ageAtMaturity) {         // if person is above "adult" age
				if (person.getAge() < ageAtMaturity * 2) {  // but not twice "adult" age
					++livingYoungCount;                     // increment young count
				}
			}
			if (person.getAge() >= (ageAtMaturity * 2)) {   // if person is "old" adult
				++livingOldCount;                           // increment old count
			}
		}

		livingOY = (double) livingOldCount / (double) livingYoungCount;   // calculate livingOY ratio
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// 29. CALCULATE MEAN AGE OF LIVING POPULATION
	/////////////////////////////////////////////////////////////////////////////////////
	public void calculateMeanAgeT1() {

		int sumAge = 0;                                   // summary of ages
		int numPersons = personList.size();               // size of population

		for (Person person : personList) {               // go through personList
			sumAge = sumAge + (person.getAge());         // add ages
		}

		t1meanAge = (double) sumAge / (double) numPersons;   // calc mean age
	}



	/////////////////////////////////////////////////////////////////////////////////
	// SUMMARY REPORT & DATA RECORDING METHODS 
	/////////////////////////////////////////////////////////////////////////////////

	//variables to hold values for reporting from T1
	public int             t1popMortAdjustPoint;
	public double          t1popMortAdjustMult;
	public double          t1upperMSLimit;
	public int             t1ageAtProduction;
	public int             t1fertilitySchedule;
	public double          t1fertilityMultiplier;
	public int             t1ageAtWeaning;
	public int             t1ageAtWeaningWeight;
	public int             t1maxPPA;
	public int             t1ageAtMaturity;
	public int             t1gestationWeeks;
	public int             t1mortalitySchedule;
	public double          t1mortalityMultiplier;
	public int             t1maxAge;
	public double          t1sustainableCP;
	public double          t1householdEconWeight;
	public int             t1pairBondMode;
	public double          t1pairBondStability;
	public boolean         t1mateScarcityAdjustSwitch;
	public boolean         t1avoidanceOn;
	public boolean         t1infanticideOn;
	public double          t1nonEconInfanticideRisk;
	public int             t1pairBondRestrictionMode;
	public int             t1numMarriageDivisions;

	public double          t1fertAge6_10;
	public double          t1fertAge11_15;
	public double          t1fertAge16_20;
	public double          t1fertAge21_25;
	public double          t1fertAge26_30;
	public double          t1fertAge31_35;
	public double          t1fertAge36_40;
	public double          t1fertAge41_45;
	public double          t1fertAge46_50;
	public double          t1fertAge51_55;

	public double          t1mortAge0;                // percent mortality before age 1
	public double          t1mortAge1;                // percent mortality between ages 1 and 2
	public double          t1mortAge2;
	public double          t1mortAge3;
	public double          t1mortAge4;
	public double          t1mortAge5;
	public double          t1mortAge6_10;
	public double          t1mortAge11_15;
	public double          t1mortAge16_20;
	public double          t1mortAge21_25;
	public double          t1mortAge26_30;
	public double          t1mortAge31_35;
	public double          t1mortAge36_40;
	public double          t1mortAge41_45;
	public double          t1mortAge46_50;
	public double          t1mortAge51_55;
	public double          t1mortAge56_60;
	public double          t1mortAge61_65;
	public double          t1mortAge66_70;
	public double          t1mortAge71_75;
	public double          t1mortAge76_80;
	public double          t1meanAdultMort;
	public double          t1meanChildMort;

	public double          t1meanPop;                   // mean population size during T1
	public int             t1maxPopSize;                // maximum population size during T1
	public int             t1minPopSize;                // minimum population size during T1
	public static boolean         t1survived = true;           // did population survive?
	public int             t1stepPopLessTwo;            // if not, what step did pop drop < 2?
	public double          t1meanHouseholdSize;         // mean size of households during T1
	public int             t1maxHouseholdSize;          // size of largest household during T1
	public double          t1meanFertFullReproSpan;     // mean n children/female surviving repro years
	public double          t1meanFertAllFemales;        // mean n children/female (all females)
	public double          t1IBIyears;                  // mean inter-birth interval (years)
	public double          t1meanMaleAgeAtMarriage;     // mean male age at first marriage
	public double          t1meanFemaleAgeAtMarriage;   // mean female age at first marriage
	public double          t1meanPercentPolygyny;       // mean % of households with more than one female mate
	public double          t1meanIntensityPolygyny;     // mean maximum harem size
	public int             t1maxIntensityPolygyny;      // size of largest harem during T1
	public double          t1meanLifespanSurplus;       // mean amount of surplus per household
	public double          t1livingOY;                  // living OY calcuated mid-way through T1
	public int             t1deadOYlistSize;            // num persons on dead list
	public double          t1deadOYall;                 // dead OY calc from all dead persons
	public double          t1deadOY10;                  // dead OY calc from random sample of 10
	public double          t1deadOY50;                  // dead OY calc from random sample of 50
	public double          t1deadOY100;
	public double          t1deadOY250;
	public double          t1deadOY500;
	public double          t1deadOY750;
	public double          t1deadOY1000;


	///////////////////////////////////////////////////////////////////////////////////////////////
	// 30. RECORD T1 DATA
	//  -records summary data from T1
	/////////////////////////////////////////////////////////////////////////////////////////////////
	public void recordT1Data () {

		//parameters
		t1popMortAdjustPoint = popMortAdjustPoint;
		t1popMortAdjustMult = popMortAdjustMult;
		t1upperMSLimit = upperMSLimit;
		t1ageAtProduction = ageAtProduction;
		t1sustainableCP = sustainableCP;
		t1pairBondMode = pairBondMode;
		t1pairBondStability = pairBondStability;
		t1householdEconWeight = householdEconWeight;
		t1mateScarcityAdjustSwitch = mateScarcityAdjustSwitch;
		t1avoidanceOn = avoidanceOn;
		t1infanticideOn = infanticideOn;
		t1fertilitySchedule = fertilitySchedule;
		t1fertilityMultiplier = fertilityMultiplier;
		t1gestationWeeks = gestationWeeks;
		t1ageAtWeaning = ageAtWeaning;
		t1ageAtWeaningWeight = ageAtWeaningWeight;
		t1maxPPA = maxPPA;
		t1ageAtMaturity = ageAtMaturity;
		t1mortalitySchedule = mortalitySchedule;
		t1mortalityMultiplier = mortalityMultiplier;
		t1maxAge = maxAge;						
		t1nonEconInfanticideRisk = nonEconInfanticideRisk;
		t1pairBondRestrictionMode = pairBondRestrictionMode;
		t1numMarriageDivisions = numMarriageDivisions;

		//outcomes: population size
		t1meanPop = (double) summaryPop / (double) recordingSteps;    
		t1maxPopSize = maxPopSize;
		t1minPopSize = minPopSize;
		t1stepPopLessTwo = stepPopLessTwo;  

		//outcomes: household size, marriage
		t1meanHouseholdSize = (double) summaryHouseholdSize / (double) summaryNumHouseholds; 
		t1maxHouseholdSize = maxHouseholdSize;    
		t1meanMaleAgeAtMarriage = (double) summaryAgeMarriedMen / (double) summaryNumMarriedMen;
		t1meanFemaleAgeAtMarriage = (double) summaryAgeMarriedWomen / (double) summaryNumMarriedWomen;
		t1meanPercentPolygyny =  summaryPercentPoly / (double) recordingSteps;
		t1meanIntensityPolygyny = (double) summaryMaxHarem / (double) recordingSteps;
		t1maxIntensityPolygyny = largestMaxHarem;

		//outcomes: OY calculations
		t1livingOY = livingOY;
		t1deadOYlistSize = t1deadList.size();
		calculateDeadOYratioSamples();               // calculate OY ratios from samples of dead population

		//outcomes: reproduction
		t1meanFertFullReproSpan = (double) summaryNumChildrenFull / (double) summaryNumFemalesSurviveRepro; 
		t1meanFertAllFemales = (double) summaryNumChildrenAll / (double) summaryNumAdultFemales;
		t1IBIyears = ((double) summaryIBIweeks / (double) stepsPerYear) / summaryNumBirthIntervals;
		t1fertAge6_10 = (double) (numBirthsAge6_10) / (double) numFemalesAge6_10 / 5;
		t1fertAge11_15 = (double) (numBirthsAge11_15) / (double) numFemalesAge11_15 / 5;
		t1fertAge16_20 = (double) (numBirthsAge16_20) / (double) numFemalesAge16_20 / 5;
		t1fertAge21_25 = (double) (numBirthsAge21_25) / (double) numFemalesAge21_25 / 5;
		t1fertAge26_30 = (double) (numBirthsAge26_30) / (double) numFemalesAge26_30 / 5;
		t1fertAge31_35 = (double) (numBirthsAge31_35) / (double) numFemalesAge31_35 / 5;
		t1fertAge36_40 = (double) (numBirthsAge36_40) / (double) numFemalesAge36_40 / 5;
		t1fertAge41_45 = (double) (numBirthsAge41_45) / (double) numFemalesAge41_45 / 5;
		t1fertAge46_50 = (double) (numBirthsAge46_50) / (double) numFemalesAge46_50 / 5;
		t1fertAge51_55 = (double) (numBirthsAge51_55) / (double) numFemalesAge51_55 / 5;

		//outcomes: mortality
		t1mortAge0 = (double) (numAge0 - numAge1)/ (double) numAge0;
		t1mortAge1 = (double) (numAge1 - numAge2)/ (double) numAge1;
		t1mortAge2 = (double) (numAge2 - numAge3)/ (double) numAge2;
		t1mortAge3 = (double) (numAge3 - numAge4)/ (double) numAge3;
		t1mortAge4 = (double) (numAge4 - numAge5)/ (double) numAge4;
		t1mortAge5 = (double) (numAge5 - numAge6_10) / (double) numAge5;
		t1mortAge6_10 = (double) (numAge6_10 - numAge11_15) / (double) numAge6_10 / 5;
		t1mortAge11_15 = (double) (numAge11_15 - numAge16_20) / (double) numAge11_15 / 5;
		t1mortAge16_20 = (double) (numAge16_20 - numAge21_25) / (double) numAge16_20 / 5;
		t1mortAge21_25 = (double) (numAge21_25 - numAge26_30) / (double) numAge21_25 / 5;
		t1mortAge26_30 = (double) (numAge26_30 - numAge31_35) / (double) numAge26_30 / 5;
		t1mortAge31_35 = (double) (numAge31_35 - numAge36_40) / (double) numAge31_35 / 5;
		t1mortAge36_40 = (double) (numAge36_40 - numAge41_45) / (double) numAge36_40 / 5;
		t1mortAge41_45 = (double) (numAge41_45 - numAge46_50) / (double) numAge41_45 / 5;
		t1mortAge46_50 = (double) (numAge46_50 - numAge51_55) / (double) numAge46_50 / 5;
		t1mortAge51_55 = (double) (numAge51_55 - numAge56_60) / (double) numAge51_55 / 5;
		t1mortAge56_60 = (double) (numAge56_60 - numAge61_65) / (double) numAge56_60 / 5;
		t1mortAge61_65 = (double) (numAge61_65 - numAge66_70) / (double) numAge61_65 / 5;
		t1mortAge66_70 = (double) (numAge66_70 - numAge71_75) / (double) numAge66_70 / 5;
		t1mortAge71_75 = (double) (numAge71_75 - numAge76_80) / (double) numAge71_75 / 5;
		t1mortAge76_80 = (double) (numAge76_80 - numAgeOver80) / (double) numAge76_80 / 5;

		t1meanAdultMort = (t1mortAge16_20 + t1mortAge21_25 + t1mortAge26_30 + t1mortAge31_35 + 
				t1mortAge36_40 + t1mortAge41_45 + t1mortAge46_50 ) / 7;

		t1meanChildMort = (t1mortAge2 + t1mortAge3 + t1mortAge4 + t1mortAge5 + t1mortAge6_10) / 5;

	}

	/////////////////////////////////////////////////////////////////////////////////////////
	//31. REPORT SUMMARY DATA
	// -reports data recorded during T1
	/////////////////////////////////////////////////////////////////////////////////////////
		public void reportSummaryData () {
	
			String s = String.format( "%d ", ForagerNet3Builder.getRunNumber());   //run number
	
			//parameters
			s += String.format( "%d ", initNumPersons);
			s += String.format( "%d ", t1popMortAdjustPoint); 
			s += String.format( "%5.2f ", t1popMortAdjustMult);
			s += String.format( "%d ", t1fertilitySchedule);
			s += String.format( "%5.2f ", t1fertilityMultiplier);
			s += String.format( "%d ", t1ageAtWeaning);
			s += String.format( "%d ", t1ageAtWeaningWeight);
			s += String.format( "%d ", t1maxPPA);
			s += String.format( "%d ", t1ageAtMaturity);
			s += String.format( "%d ", t1gestationWeeks);
			s += String.format( "%d ", t1mortalitySchedule);
			s += String.format( "%5.2f ", t1mortalityMultiplier);
			s += String.format( "%d ", t1maxAge);
	
			if (t1avoidanceOn == true) { s += String.format ( "On "); }
			if (t1avoidanceOn == false) { s += String.format ( "Off "); }
			if (t1infanticideOn == true) { s += String.format ( "On "); }
			if (t1infanticideOn == false) { s += String.format ( "Off "); }
	
			s += String.format( "%5.2f ", t1nonEconInfanticideRisk);
			s += String.format( "%d ", t1ageAtProduction);                
			s += String.format( "%5.2f ", t1sustainableCP);
	
			if (t1mateScarcityAdjustSwitch == true) { s += String.format ( "On "); }
			if (t1mateScarcityAdjustSwitch == false) { s += String.format ( "Off "); }
	
			s += String.format( "%5.2f ", t1householdEconWeight);
			s += String.format( "%5.3f ", t1pairBondStability);
			s += String.format( "%5.0f ", t1upperMSLimit);             
			s += String.format( "%d ", t1pairBondMode);                 
			s += String.format( "%d ", t1pairBondRestrictionMode);         
			s += String.format( "%d ", t1numMarriageDivisions);
	
			//outcomes
			if (t1survived == true) { s += String.format( "survived "); }
			else { s += String.format( "extinct "); }
	
			s += String.format ("%d ", t1stepPopLessTwo);     
			s += String.format( "%d ", t1minPopSize);           
			s += String.format( "%5.0f ", t1meanPop);          
			s += String.format( "%d ", t1maxPopSize);          
			s += String.format( "%5.2f ", t1meanFertFullReproSpan);   
			s += String.format( "%5.2f ", t1meanFertAllFemales);     
			s += String.format( "%5.2f ", t1IBIyears);              
	
			s += String.format( "%5.4f ", t1fertAge6_10);
			s += String.format( "%5.4f ", t1fertAge11_15);
			s += String.format( "%5.4f ", t1fertAge16_20);
			s += String.format( "%5.4f ", t1fertAge21_25);
			s += String.format( "%5.4f ", t1fertAge26_30);
			s += String.format( "%5.4f ", t1fertAge31_35);
			s += String.format( "%5.4f ", t1fertAge36_40);
			s += String.format( "%5.4f ", t1fertAge41_45);
			s += String.format( "%5.4f ", t1fertAge46_50);
			s += String.format( "%5.4f ", t1fertAge51_55);
	
			s += String.format( "%5.4f ", t1mortAge0);
			s += String.format( "%5.4f ", t1mortAge1);
			s += String.format( "%5.4f ", t1mortAge2);
			s += String.format( "%5.4f ", t1mortAge3);
			s += String.format( "%5.4f ", t1mortAge4);
			s += String.format( "%5.4f ", t1mortAge5);
			s += String.format( "%5.4f ", t1mortAge6_10);
			s += String.format( "%5.4f ", t1mortAge11_15);
			s += String.format( "%5.4f ", t1mortAge16_20);
			s += String.format( "%5.4f ", t1mortAge21_25);
			s += String.format( "%5.4f ", t1mortAge26_30);
			s += String.format( "%5.4f ", t1mortAge31_35);
			s += String.format( "%5.4f ", t1mortAge36_40);
			s += String.format( "%5.4f ", t1mortAge41_45);
			s += String.format( "%5.4f ", t1mortAge46_50);
			s += String.format( "%5.4f ", t1mortAge51_55);
			s += String.format( "%5.4f ", t1mortAge56_60);
			s += String.format( "%5.4f ", t1mortAge61_65);
			s += String.format( "%5.4f ", t1mortAge66_70);
			s += String.format( "%5.4f ", t1mortAge71_75);
			s += String.format( "%5.4f ", t1mortAge76_80);
			s += String.format( "%5.4f ", t1meanAdultMort);
			s += String.format( "%5.4f ", t1meanChildMort);
			s += String.format( "%5.2f ", t1livingOY); 
			s += String.format( "%5.2f ", t1meanAge);
	
			s += String.format( "%d ", t1deadOYlistSize);
			s += String.format( "%5.2f ", t1deadOYall);
			s += String.format( "%5.2f ", t1deadOY10);
			s += String.format( "%5.2f ", t1deadOY50);
			s += String.format( "%5.2f ", t1deadOY100);
			s += String.format( "%5.2f ", t1deadOY250);
			s += String.format( "%5.2f ", t1deadOY500);
			s += String.format( "%5.2f ", t1deadOY750);
			s += String.format( "%5.2f ", t1deadOY1000);
	
			s += String.format( "%5.2f ", t1meanHouseholdSize);            
			s += String.format( "%d ", t1maxHouseholdSize);                
			s += String.format( "%5.2f ", t1meanMaleAgeAtMarriage);
			s += String.format( "%5.2f ", t1meanFemaleAgeAtMarriage);
			s += String.format( "%5.2f ", t1meanPercentPolygyny);             
			s += String.format( "%5.2f ", t1meanIntensityPolygyny);
			s += String.format( "%d ", t1maxIntensityPolygyny);
	
			doSummaryAppend(s);      //send data to be appended to a file
		}
	
		//each line of summary data is appended to this file
		public void doSummaryAppend (String s) {
	
			try {
				String fileName = "SummaryData_MVP_debug_meanHouseholdSize.txt";
				BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
	
				out.write(s);
				out.write("\n");
				out.close();
			} 
	
			catch (IOException e) {
				System.out.println("IOException:");
				e.printStackTrace();
			}
		}

	///////////////////////////////////////////////////////////////////////////////////
	// 32. CALCULATE DEAD OY RATIOS FROM SAMPLES
	///////////////////////////////////////////////////////////////////////////////////
	public void calculateDeadOYratioSamples () {

		int listSize = t1deadList.size() - 1;                       // number of persons on dead OY list
		int numYoung = 0;
		int numOld = 0;
		int numFound = 0;

		//calculate ratio for entire dead population
		for (int i = 0; i < listSize; ++i) {                        // go through list
			Person deadOYPerson = t1deadList.get (i);               // get next person from list
			if (deadOYPerson.getAge() >= ageAtMaturity) {           // if person at or above reproductive age
				if (deadOYPerson.getAge() < ageAtMaturity * 2) {    // but not twice reproductive age
					++numYoung;                                     // increment young count
				}
			}
			if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {     // if person is "old"
					++numOld;                                           // increment old count
			}
		}

		t1deadOYall = (double) numOld / (double) numYoung;          // calc and record ratio

		//loop for sample of 10
		if (listSize >= 10) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 10) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY10 = (double) numOld / (double) numYoung;            //calc and record ratio
		}

		//loop for sample of 50
		if (listSize >= 50) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 50) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY50 = (double) numOld / (double) numYoung;            //calc and record ratio
		}

		//loop for sample of 100
		if (listSize >= 100) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 100) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY100 = (double) numOld / (double) numYoung;            //calc and record ratio
		}

		//loop for sample of 250
		if (listSize >= 250) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 250) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY250 = (double) numOld / (double) numYoung;            //calc and record ratio
		}

		//loop for sample of 500
		if (listSize >= 500) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 500) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY500 = (double) numOld / (double) numYoung;            //calc and record ratio
		}

		//loop for sample of 750
		if (listSize >= 750) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 750) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY750 = (double) numOld / (double) numYoung;            //calc and record ratio
		}
		//loop for sample of 1000
		if (listSize >= 1000) {                                       // if there are enough on the list
			for (Person deadPerson : t1deadList) {                  // got though list
				deadPerson.setUsedForOY(false);                     // set these to false
			}	
			numYoung = 0;
			numOld = 0;
			numFound = 0;                                           // set this to 0
			int numTries = 0;                                       // track number of tries
			while (numFound < 1000) {                                      // if 10 haven't been found
				++numTries;                                              // increment numTries
				int sampleN = RandomHelper.nextIntFromTo(0, listSize);    // generate random number
				Person deadOYPerson = t1deadList.get (sampleN);          // get that person from list
				if (deadOYPerson.getUsedForOY() == false) {              // if person has not yet been counted
					deadOYPerson.setUsedForOY(true);                     // prevent double counting
					int deadAge = deadOYPerson.getAge();                      // get person's age
					if (deadAge >= ageAtMaturity) {                      // if above reproductive age
						if (deadAge < ageAtMaturity * 2) {               // but not twice reproductive age
							++numYoung;                                      // increment young count
							++numFound;                                      // increment numFound
						}
					}

					if (deadOYPerson.getAge() >= (ageAtMaturity * 2)) {  // if person is "old"
							++numOld;                                        // increment old count
							++numFound;                                      // increment numFound
					}
				}
				if (numTries > 100000) {                                 //circuit breaker
					//	processEndOfRun();                                   //something wrong
				}
			}
			t1deadOY1000 = (double) numOld / (double) numYoung;            //calc and record ratio
		}
	}


	/////////////////////////////////////////////////////////////////////////////////////////
	// METHODS RELATED TO CREATING LINKS (not individually described in documentation)
	/////////////////////////////////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////////////////////////////
	// ADD LINK TO LINKLIST
	////////////////////////////////////////////////////////////////////////////////////////
	public static void addLinkToLinkList(Link newLink) {

		if (newLink == null) {
			System.err.printf ("ERROR: attempting to add link=null to linkList\n");
		}

		if (newLink != null) {
			linkList.add (newLink);
		//	linksToNull.add (newLink);       //add to list to null at end of run
		}
	} 

	///////////////////////////////////////////////////////////////////////////////////////
	// CREATE FAMILY LINK (linkType 1)
	// -creates link between family members related by descent
	// -called from identifyFamily method in Person
	// -only called at birth, so there should never be a previously existing link
	////////////////////////////////////////////////////////////////////////////////////////
	public static void createFamilyLink (Person fromPerson, Person toPerson, int linkType, int linkSubType) {
		
		//System.out.printf("CreateFamilyLink called - linkList size = %d\n", linkList.size());
		
		boolean alreadyLinked = checkForExistingLink (fromPerson, toPerson);

		if (alreadyLinked == true) 
			System.err.printf ("ERROR in FAMILY LINK: two persons already linked\n");

		if (alreadyLinked == false) {
							
			Link newFamilyLink = new Link ();                   // create link
			newFamilyLink.setFromPerson (fromPerson);           // set link origin
			newFamilyLink.setToPerson (toPerson);               // set link destination
			newFamilyLink.setLinkType (linkType);               // set link type
			newFamilyLink.setLinkSubType (linkSubType);         // set link subtype
			addLinkToLinkList (newFamilyLink);                  // add to linkList kept by model
			fromPerson.addToLinkList (newFamilyLink);           // add link to linkList of fromPerson
			++numLinksCreated;
			checkLink (fromPerson, toPerson, linkType, linkSubType);             //error check
			
			Context <Object> linkContext;
			linkContext = ContextUtils.getContext(fromPerson); //get fromPerson's context
			linkContext.add(newFamilyLink);
			
		//	System.out.printf("Family link created\n");
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// CREATE CO-RESIDENT LINK (linkType 2)
	// -creates link between household members not related by descent
	// -called from identifyCoResidents method in Person (trigged by birth, marriage, orphan movement)
	/////////////////////////////////////////////////////////////////////////////////////////////////
	public static void createCoResidentLink (Person fromPerson, Person toPerson, int linkType, int linkSubType) {

		boolean alreadyLinked = checkForExistingLink (fromPerson, toPerson);

		if (alreadyLinked == true) {                             // if there is already a link
			Link existingLink = getLink (fromPerson, toPerson);  // get existing link

			if (existingLink.getLinkType() == 5)  {              // if it's an acquaintance link
				existingLink.setLinkType (linkType);             // change it to coResident link
				existingLink.setLinkSubType(linkSubType);  
				checkLink (fromPerson, toPerson, linkType, linkSubType);
			}	
		}

		if (alreadyLinked == false) {                            // if no previous link
			Link newCoResidentLink = new Link ();                // create link
			newCoResidentLink.setFromPerson (fromPerson);        // set link origin
			newCoResidentLink.setToPerson (toPerson);            // set link destination
			newCoResidentLink.setLinkType (linkType);            // set link type
			newCoResidentLink.setLinkSubType (linkSubType);      // set link subtype
			addLinkToLinkList (newCoResidentLink);               // add to linkList kept by model
			fromPerson.addToLinkList (newCoResidentLink);        // add link to linkList of fromPerson
			++numLinksCreated;
			checkLink (fromPerson, toPerson, linkType, linkSubType);           // error check
			
			Context <Object> linkContext;
			linkContext = ContextUtils.getContext(fromPerson); //get fromPerson's context
			linkContext.add(newCoResidentLink);
			
		//	System.out.printf("co-resident link created\n");
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// CREATE MARRIAGE LINK (linkType 3)
	// -creates link between male and female at marriage
	/////////////////////////////////////////////////////////////////////////////////////////////
	public static void createMarriageLink (Person fromPerson, Person toPerson, int linkType, int linkSubType) {

		boolean alreadyLinked = checkForExistingLink (fromPerson, toPerson);   // check for existing link

		if (alreadyLinked == true) {                                           // if there is one
			Link existingLink = getLink (fromPerson, toPerson);                // get existing link

			if (existingLink.getLinkType() == 1)                               // if it's a family link
				if (pairBondRestrictionMode != 0) {                            // unless we're in mode 0
					System.err.printf ("ERROR IN MARRIAGE LINK: family members marrying\n");  // problem
				}

			existingLink.setLinkType (linkType);                               // change the link type
			existingLink.setLinkSubType (linkSubType);                         // change the link subtype
			checkLink (fromPerson, toPerson, linkType, linkSubType);           // check link
		}

		if (alreadyLinked == false) {                             // if no previous link
			Link newMarriageLink = new Link ();                   // create link
			newMarriageLink.setFromPerson (fromPerson);           // set link origin
			newMarriageLink.setToPerson (toPerson);               // set link destination
			newMarriageLink.setLinkType (linkType);               // set link type
			newMarriageLink.setLinkSubType (linkSubType);         // set link subtype
			addLinkToLinkList(newMarriageLink);                   // add to linkList kept by model
			fromPerson.addToLinkList (newMarriageLink);           // add link to linkList of fromPerson
			++numLinksCreated;
			checkLink (fromPerson, toPerson, linkType, linkSubType);                     //check link
			
			Context <Object> linkContext;
			linkContext = ContextUtils.getContext(fromPerson); //get fromPerson's context
			linkContext.add(newMarriageLink);
			
		//	System.out.printf("marriage link created\n");
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// CREATE KIN LINK (linkType 4)
	// -this creates kin links
	// - co-resident links can be "upgraded" to kin status
	// -family links are left as is
	// -affine links are left as is (incest taboo does not include all "kin")
	//////////////////////////////////////////////////////////////////////////////////////////////////

	public static void createKinLink ( Person fromPerson, Person toPerson, int linkType, int linkSubType) {

		boolean alreadyLinked = checkForExistingLink (fromPerson, toPerson);   //check for existing link

		if (alreadyLinked == true) {                                // if there is one
			Link existingLink = getLink (fromPerson, toPerson);     // get existing link

			if (existingLink.getLinkType() == 2 ) {                 // if existing link is co-resident
				existingLink.setLinkType (linkType);                // set link type
				existingLink.setLinkSubType (linkSubType);          // set link subtype
			}

			if (existingLink.getLinkType() == 5 ) {                 // if existing link is acquaintance
				existingLink.setLinkType (linkType);                // set link type
				existingLink.setLinkSubType (linkSubType);          // set link subtype
			}

			//if existing link is kin (4), family (1), or marriage (3) it is left as-is
			checkLink (fromPerson, toPerson, linkType, linkSubType);
		}

		if (alreadyLinked == false) {                        // if no previous link
			Link newKinLink = new Link ();                   // create link
			newKinLink.setFromPerson (fromPerson);           // set link origin
			newKinLink.setToPerson (toPerson);               // set link destination
			newKinLink.setLinkType (linkType);               // set link type
			newKinLink.setLinkSubType (linkSubType);         // set link subtype
			addLinkToLinkList(newKinLink);                   // add to linkList kept by model
			fromPerson.addToLinkList (newKinLink);           // add link to linkList of fromPerson
			++numLinksCreated;
			checkLink (fromPerson, toPerson, linkType, linkSubType);
			
			Context <Object> linkContext;
			linkContext = ContextUtils.getContext(fromPerson); //get fromPerson's context
			linkContext.add(newKinLink);
			
			//System.out.printf("kin link created\n");
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// CREATE ACQUAINTANCE LINK (linkType 5)
	// - create link between otherwise-unrelated acquaintances
	// - this is not used on ForagerNet3_Demography
	/////////////////////////////////////////////////////////////////////////////////////////////
	public static void createAcquaintanceLink (Person fromPerson, Person toPerson, int linkType, int linkSubType) {

		boolean alreadyLinked = checkForExistingLink(fromPerson, toPerson);    // check for existing link

		if (alreadyLinked == true) {                                           // if there is one
			System.err.printf ("ERROR in CREATE ACQUAINTANCE LINK: link already exists\n");
		}

		if (alreadyLinked == false) {                                 // if no existing link
			Link newAcquaintanceLink = new Link ();                   // create link
			newAcquaintanceLink.setFromPerson (fromPerson);           // set link origin
			newAcquaintanceLink.setToPerson (toPerson);               // set link destination
			newAcquaintanceLink.setLinkType (linkType);               // set link type
			newAcquaintanceLink.setLinkSubType (linkSubType);         // set link subtype
			addLinkToLinkList(newAcquaintanceLink);                   // add to linkList kept by model
			fromPerson.addToLinkList (newAcquaintanceLink);           // add link to linkList of fromPerson
			++numLinksCreated;
			checkLink (fromPerson, toPerson, linkType, linkSubType);
			
			Context <Object> linkContext;
			linkContext = ContextUtils.getContext(fromPerson); //get fromPerson's context
			linkContext.add(newAcquaintanceLink);
			
		//	System.out.printf("acquaintance link created\n");
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// CREATE EX-MATE LINK (linkType 6)
	// - when pair bonds are dissolved, marriage links turn into these
	///////////////////////////////////////////////////////////////////////////////////////////
	public static void createExMateLink (Person fromPerson, Person toPerson, int linkType, int linkSubType) {

		boolean alreadyLinked = checkForExistingLink(fromPerson, toPerson);    // check for existing link

		if (alreadyLinked == true) {                                           // if there is one
			Link existingLink = getLink (fromPerson, toPerson);                // get link
			if (existingLink.getLinkType() == 3) {                             // if it is marriage link
				existingLink.setLinkType (linkType);                           // set it to ex-mate
				existingLink.setLinkSubType (linkSubType);                     // set subtype
				checkLink (fromPerson, toPerson, linkType, linkSubType);
			}
			else
				System.err.printf ("ERROR in CREATE EX-MATE LINK: previous link not marriage link\n");
		}

		if (alreadyLinked == false) {                                          // if no existing link
			System.err.printf ("ERROR in CREATE EX-MATE LINK: no previous link\n");   //error
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// CHECK LINK
	// -makes sure neither person is dead
	// -makes sure linked people aren't the same person
	////////////////////////////////////////////////////////////////////////////////////////
	public static void checkLink (Person fromPerson, Person toPerson, int linkType, int linkSubType) {

		if (fromPerson == toPerson)
			System.err.printf ("ERROR in NEW LINK TYPE %d SUBTYPE %d: fromPerson and toPerson = identical\n", 
					linkType, linkSubType);

		if (fromPerson.getLive() == false)
			System.err.printf ("ERROR in NEW LINK TYPE %d: fromPerson (%d) is dead\n", 
					linkType, fromPerson.getId());

		if (toPerson.getLive() == false)
			System.err.printf ("ERROR in NEW LINK TYPE %d: toPerson (%d) is dead\n", 
					linkType, toPerson.getId());
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// CHECK FOR EXISTING LINK
	// - called to confirm that a link between two people does not already exist
	// - called in circumstances when an existing link would indicate an error
	///////////////////////////////////////////////////////////////////////////////////////////
	public static boolean checkForExistingLink (Person fromPerson1, Person toPerson1) {

		boolean linkFound = false;

		if (fromPerson1 == null)
			System.err.printf ("ERROR in CHECK FOR EXISTING LINK: fromPerson1 is null\n");

		if (toPerson1 == null)
			System.err.printf ("ERROR in CHECK FOR EXISTING LINK: toPerson1 is null\n");

		if (linkList == null)
			System.err.printf ("ERROR in CHECK FOR EXISTING LINK: linkList is null\n");

		for (Link link : linkList) {                         // go through linkList
			Person fromPerson = link.getFromPerson();        // get "from" person
			Person toPerson = link.getToPerson();            // get "to" person
			if (fromPerson == fromPerson1) {                 // if "from" people match . . .
				if (toPerson == toPerson1) {                 // and "to" people match . . .
					linkFound = true;                        // this is "true"
					return linkFound;                        // return result
				}
			}
		}

		return linkFound;              // result will be false if no matching link found
	}

	////////////////////////////////////////////////////////////////////////////////
	// GET SPECIFIC LINK
	// - returns a specific link based on fromPerson and toPerson
	/////////////////////////////////////////////////////////////////////////////////////
	public static Link getLink (Person fromPerson1, Person toPerson1) {

		if (fromPerson1 == null)
			System.err.printf ("ERROR in GET SPECIFIC LINK: From person is null\n");

		if (toPerson1 == null)
			System.err.printf ("ERROR in GET SPECIFIC LINK: To person is null\n");

		if (linkList.size() == 0)                                  // if no-one on link list
			if (currentTick > 1)                                // and we're past first step
				System.err.printf ("ERROR in GET SPECIFIC LINK: linkList is empty, step %5.0f, pop=%d\n", 
						currentTick, personList.size());

		boolean linkFound = false;

		for (Link candLink : linkList) {                          // go through linkList
			Person fromPerson = candLink.getFromPerson();         // get "from" person

			if (fromPerson == null)                               // if null
				System.err.printf ("ERROR: fromPerson is null\n");// problem

			Person toPerson = candLink.getToPerson();             // get "to" person

			if (toPerson == null)                                 // if null
				System.err.printf ("ERROR: toPerson is null\n");  // problem

			if (fromPerson == fromPerson1) {                      // if "from" people match . . .
				if (toPerson == toPerson1) {                      // and "to" people match . . .
					linkFound = true;                             // set this to true
					return candLink;                              // return the link
				}
			}
		}

		Link nullLink = null;                                     // now create a null link

		if (linkFound == false)                                   // if this remained false
			System.err.printf ("ERROR: Existing link not located by getSpecificLink\n");

		return nullLink;                                          // send back null if link not found
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// MINOR METHODS (not individually described in documentation)
	/////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////
	// GET POTENTIAL FEMALE MATE
	public static Person getPotentialFemaleMate ( int i ) {
		Person potentialMate = eligibleFemaleList.get(i);
		return potentialMate;
	}

	////////////////////////////////
	// GET NUMBER OF ELIGIBLE FEMALES
	public static int getNumEligibleFemales() {
		return eligibleFemaleList.size();
	}

	////////////////////////////////
	// DELETE LINK
	public static void deleteLink (Link linkToDelete) {

	//	System.out.printf ("MODEL.deleteLink . . . ");
		
		linkList.remove(linkToDelete);
		
		Context <Object> context;
		context = ContextUtils.getContext(linkToDelete); //get link's context
		context.remove(linkToDelete);
		
		linkToDelete = null;

		++numLinksDeleted;

		if (linkList.size() != numLinksCreated - numLinksDeleted) 
			System.err.printf ("ERROR in DELETE LINK: link creation/deletion numbers do not square\n");
	}

	/////////////////////////////////
	// INCREMEMNT NUMBER OF PAIR BONDS
	public static void incrementNumPairBonds () {
		++numPairBonds;
	}

	////////////////////////////////
	// INCREMENT NUMBER OF PAIR BOND ATTEMPTS
	public static void incrementNumPairBondAttempts () {
		++numPairBondAttempts;
	}

	//////////////////////////////////
	// GET RANDOM HOUSEHOLD
	public Household getRandomHousehold() {
		int numHouseholds = householdList.size();
		int choice = RandomHelper.nextIntFromTo(0, numHouseholds-1);
		Household randomHouse = householdList.get(choice);
		return randomHouse;
	}

	/////////////////////////////////
	// ADD HOUSEHOLD TO LIST
	public static void addHouseholdToList (Household newHousehold) {
		householdList.add(newHousehold);
		housesToNull.add (newHousehold);     //add to list to null at end of run
	}

	//////////////////////////////////////
	// REMOVE PERSON FROM ELIGIBLE FEMALE LIST
	public static void removeFromEligibleFemaleList (Person female) {
		eligibleFemaleList.remove(female);                    
	}	

	/////////////////////////////////////
	// GET HOUSEHOLD WITH SPECIFIC ID
	public Household getHousehold (int householdID) {
		Household correctHousehold = null;             
		for (Household household : householdList) {
			if (householdID == household.getId()) {
				correctHousehold = household;
			}
		}
		return correctHousehold;
	}

	///////////////////////////////////
	// GET HOUSEHOLD i
	// -sends back household i from the householdList
	public Household getHouseholdFromList (int i) {
		Household household = householdList.get(i);             
		return household;
	}
	///////////////////////////////////
	// GET PERSON i
	// -sends back person i from the personList
	public static Person getPersonFromList (int i) {
		Person person = personList.get(i);             
		return person;
	}	

	//	////////////////////////////////////////
	//	// REPORT POPULATION AND STEP
	//	// -reports number of links, people, groups, and aggParcels each step
	//	public void reportNumLinks () {
	//		String s = String.format( "%d  ", getRunNumber());
	//		s += String.format (" %5.0f ", currentTick);
	//		s += String.format( "%d  ", timePeriod);  
	//		s += String.format (" %d ", linkList.size());
	//		s += String.format (" %d ", personList.size());
	//		int pop = personList.size();
	//		int numPossibleLinks = (pop * (pop - 1)) / 2;
	//		double netDensity = (double) linkList.size() / (double) numPossibleLinks;
	//		s += String.format ( "%5.4f " , netDensity);
	//		s += String.format ( "%d ", adultMaleList.size());
	//		s += String.format ( "%d ", eligibleFemaleList.size());
	//		s += String.format ( "%d ", numPairBondAttempts);
	//		s += String.format ( "%d ", numPairBonds);
	//		doAppendLinkNumberData (s);
	//	}
	//
	//	public void doAppendLinkNumberData (String s) {
	//		try {
	//			String fileName = "LinkNumberData.txt";
	//			BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
	//			out.write(s);
	//			out.write("\n");
	//			out.close();
	//
	//		} 
	//		catch (IOException e) {
	//			System.out.println("IOException:");
	//			e.printStackTrace();
	//		}
	//	}	
	//
	//	//////////////////////////////////////////////
	//	//REPORT LIVE HOUSEHOLDS
	//	// - reports households with both a live male and female (productively and reproductively viable unit)
	//	public void reportLiveHouseholdData () {
	//		for (Household household : householdList) {
	//
	//			int numFemaleMates = 0;
	//			int houseSize = household.getSize();
	//
	//			Person maleMate = household.getAdultMale();
	//			
	//				if (maleMate != null) {                                 // only report households with adult male
	//				if (maleMate.getNumCurrentFemaleMates() != 0) {     // and with at least one female mate
	//					numFemaleMates = maleMate.getNumCurrentFemaleMates();
	//
	//					String s = String.format ("%5.0f ", currentTick );            // step
	//
	//					s += String.format( " %d ", getRunNumber() );                    // run number
	//					s += String.format( " %d ", timePeriod);                         // time period
	//					s += String.format( " %d ", household.getId() );                 // ID
	//					s += String.format( " %d ", household.getSize() );               // number of members
	//					s += String.format( " %d ", numFemaleMates );                    // n female mates 
	//					s += String.format( " %d ", household.getNumChildProducers() );  // n child producers
	//					s += String.format( " %d ", household.getNumNonProducers() );    // n non-producers
	//					s += String.format( " %5.3f", household.getCPRatio() );          // dependency ratio
	//					s += String.format( " %5.3f ", household.getCurrentSurplus() );  // current surplus
	//					s += String.format( " %5.3f ", household.getLifespanSurplus() ); // lifespan surplus
	//					s += String.format( " %d ", household.getYear() );               // age of household
	//					s += String.format( "   %d ", household.getSurplusYears() );     // n years in surplus
	//					s += String.format( "   %d ", household.getDeficitYears() );     // n years in deficit
	//
	//					doAppendLiveHouseholdData(s);
	//				}
	//			}
	//		}
	//	}
	//
	//	public void doAppendLiveHouseholdData (String s) {
	//		try {
	//			String fileName = "LiveHouseholdData.txt";
	//			BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
	//			out.write(s);
	//			out.write(" \n");
	//			out.close();
	//
	//		} 
	//		catch (IOException e) {
	//			System.out.println("IOException:");
	//			e.printStackTrace();
	//		}
	//	}		

	///////////////////////////////////////////////////////////////////////////////////////////////
	//ERROR CHECKERS (not described in documentation)
	//////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////
	// LINK TOTAL CHECK
	public void linkTotalCheck() {
		int totalPersonLinks = 0;
		for (Person person : personList) {
			int personLinks = person.getNumLinks();
			totalPersonLinks = totalPersonLinks + personLinks;
		}

		if (totalPersonLinks != linkList.size()) {
			System.err.printf ("%d on model linkLists, %d on person linkLists, step %.2f\n", 
					linkList.size(), totalPersonLinks, currentTick);
			System.err.printf (" --> to date %d links created, %d links removed\n", 
					numLinksCreated, numLinksDeleted);
			for (Link missingLink : linkList) {
				boolean found = false;
				for (Person person : personList) {
					int numPersonLinks = person.getNumLinks();
					for (int i = 0; i < numPersonLinks; ++i) {
						Link personLink = person.getLink(i);
						if (personLink == missingLink) {
							found = true;
						}
					}
				}
				if (found == false) {
					Person fromPerson = missingLink.getFromPerson();
					Person toPerson = missingLink.getToPerson();
					System.err.printf ("Missing link: type = %d, subtype = %d\n", 
							missingLink.getLinkType(), missingLink.getLinkSubType());
					System.err.printf (" --> From person %d, age %d, to person %d, age %d\n", 
							fromPerson.getId(), fromPerson.getAge(), toPerson.getId(), 
							toPerson.getAge());
				}
			}
		}
	}

	///////////////////////////////////////
	// NULL LINK CHECK
	public void nullLinkCheck() {

		if (linkList == null) 
			System.err.printf ("ERROR - linkList itself is apparently null\n");

		if (linkList.size() != numLinksCreated - numLinksDeleted) {
			System.err.printf ("ERROR IN NULL LINK CHECK: link numbers do not add up\n");
		}

		for (Person person : personList)         // go through personLinkList of each person
			person.checkForNullLinks();          // call method to check for null links

		for (Link link : linkList) {
			if (link.getFromPerson() == null)
				System.err.printf ("NULL LINK: fromPerson is null\n");
			if (link.getToPerson() == null)
				System.err.printf ("NULL LINK: toPerson is null\n");
			if (link == null)
				System.err.printf (" ERROR: link ==  null\n");
		}
	}

	//////////////////////////////////////
	// DEAD LINK CHECK
	public void deadLinkCheck() {
		for (Link link : linkList) {
			Person fromPerson = link.getFromPerson();
			Person toPerson = link.getToPerson();

			if (fromPerson.getLive() == false)
				System.err.printf ("DEAD LINK: fromPerson is dead\n");

			if (toPerson.getLive() == false)
				System.err.printf ("DEAD LINK: toPerson is dead\n");
		}
	}

	///////////////////////////////////////
	// NULL PERSON CHECK
	public void nullPersonCheck() {
		for (Person person : personList) {
			if (person == null)
				System.err.printf ("ERROR: NULL PERSON on personList!\n");
		}
	}

	//////////////////////////////////////
	// DEAD PERSON CHECK
	public void deadPersonCheck() {
		for (Person person : personList) {
			if (person.getLive() == false)
				System.err.printf ("ERROR: DEAD PERSON on personList!\n");
		}
	}

	/////////////////////////////////////////////////////////////////////////////
	// SETTERS AND GETTERS
	////////////////////////////////////////////////////////////////////////////

	public static int getSizeX () { return sizeX; }
	public void setSizeX ( int szX ) { sizeX = szX; }

	public static int getSizeY () { return sizeY; }
	public void setSizeY ( int szY ) { sizeY = szY;	}

	public static int getInitNumPersons () { return initNumPersons; }
	public void setInitNumPersons ( int inp ) { initNumPersons = inp;}

	public static int getAgeAtProduction () { return ageAtProduction; }
	public void setAgeAtProduction ( int pAge ) { ageAtProduction = pAge;}

	public static int getAgeAtMaturity () { return ageAtMaturity; }
	public void setAgeAMaturity ( int mAge ) { ageAtMaturity = mAge; }

	public int getGestationWeeks () { return gestationWeeks; }
	public void setGestationWeeks ( int gw ) {	gestationWeeks = gw;}

	public static int getMaxPPA () { return maxPPA; }
	public void setMaxPPA ( int mppa ) { maxPPA = mppa; }

	public static int getAgeAtWeaning () { return ageAtWeaning; }
	public void setAgeAtWeaning (int aaw ) { ageAtWeaning = aaw; }

	public static int getAgeAtWeaningWeight () { return ageAtWeaningWeight; }
	public void setAgeAtWeaningWeight ( int aaww ) { ageAtWeaningWeight = aaww; }

	public static double getSustainableCP () { return sustainableCP; }
	public void setSustainableCP ( double sCP ) { sustainableCP = sCP; }

	public static int getMaxAge () { return maxAge; }
	public void setMaxAge ( int ma ) { maxAge = ma;	}

	public static double getNonEconInfanticideRisk () { return nonEconInfanticideRisk; }
	public void setNonEconInfanticideRisk (double neir) { nonEconInfanticideRisk = neir; }

	public double getEligibleMaleFemaleRatio () { return eligibleMaleFemaleRatio; }
	public void setEligibleMaleFemaleRatio ( double emfr ) { eligibleMaleFemaleRatio = emfr;}

	public double getMateScarcity () { return mateScarcity; }
	public void setMateScarcity ( double ms) {mateScarcity = ms; }

	public static double getMateScarcityAdjustment () { return mateScarcityAdjustment; }
	public void setMateScarcityAdjustment ( double msa) {mateScarcityAdjustment = msa; }

	public static double getPairBondStability() { return pairBondStability; }
	public void setPairBondStability (double pbs) {pairBondStability = pbs; }

	public static int getPairBondMode() { return pairBondMode; }
	public void setPairBondMode ( int pbm) {pairBondMode = pbm; }

	public static int getPairBondRestrictionMode() { return pairBondRestrictionMode; }
	public void setPairBondRestrictionMode ( int pbrm) {pairBondRestrictionMode = pbrm; }

	public static double getHouseholdEconWeight() { return householdEconWeight; }
	public void setHouseholdEconWeight ( double hew) {householdEconWeight = hew; }

	public static boolean getMateScarcityAdjustSwitch() { return mateScarcityAdjustSwitch; }
	public void setMateScarcityAdjustSwitch (boolean msas) {mateScarcityAdjustSwitch = msas; }

	public int getRDebug () { return rDebug; }
	public void setRDebug ( int rdb ) { rDebug = rdb; }

	public static int getPopMortAdjustPoint () { return popMortAdjustPoint; }
	public void setPopMortAdjustPoint ( int pmap ) { popMortAdjustPoint = pmap; }

	public static double getPopMortAdjustment () { return popMortAdjustment; }
	public void setPopMortAdjustment ( int pma ) { popMortAdjustment = pma; }

	public static boolean getAvoidanceOn () {return avoidanceOn; }
	public void setAvoidanceOn ( boolean avoid ) { avoidanceOn = avoid; }

	public static boolean getInfanticideOn() {return infanticideOn; }
	public void setInfanticideOn (boolean infant ) {infanticideOn = infant; }

	public static int getSeasonTicks() {return seasonTicks;}
	public void setSeasonTicks (int st) {seasonTicks = st; }

	public static int getStepsPerYear() {return stepsPerYear;}
	public void setStepsPerYear (int spy) {stepsPerYear = spy; }

	// T1 START AND STOP
	public static int getT1Start () { return t1Start; }
	public static int getT1Stop () { return t1Stop; }
	public int getTimePeriod() { return timePeriod;}

	// AGGREGATE MEASURES
	public int getPopSize() { return personList.size(); }
	public int getNumHouseholds() { return householdList.size(); }
	public int getNumAvailableFemales () { return eligibleFemaleList.size(); }
	public int getNumLinks() { return linkList.size()/2; }
}



