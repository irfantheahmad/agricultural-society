package ForagerNet3_Demography_V3;

import java.util.ArrayList;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;
import repast.simphony.context.Context;
import repast.simphony.random.RandomHelper;

public class Person {

	private ContinuousSpace <Object > space ;
	private Grid <Object > grid ;
	
//////////////////////////////////////////////////////////////////////////////////////////
// PERSON CONSTRUCTOR
/////////////////////////////////////////////////////////////////////////////////////////	
	
	public Person (ContinuousSpace <Object > space, Grid <Object > grid ) {
		
	//	System.out.printf("Person space/grid creator called\n");
		
		this.space = space ;
		this.grid = grid ;
				
		if (space == null) {System.err.printf("ERROR: space is null in person creator\n");}
		if (grid == null) {System.err.printf("ERROR: grid is null in person creator\n");}
		
		id = nextId++;
		previouslyMarried = false;
		live = true;
		ageAtMarriage = 0;
		x = 0;           		
		y = 0;
		infanticideRiskExposed = false;
		birthStep = 0;
		fertile = false;
		stepsPPA = 0;
		usedForOY = false;
	}

	public static int          nextId = 0;       // to give each an id

	// instance variables  
	public int 	   		id;			        // unique id number for each person instance
	public Household    currentHousehold;   // current household of person
	public Person       father;             // DESCENT: father of person
	public Person       mother;             // DESCENT: mother of person
	public int 			x, y;		        // cache the person's x,y location
	public int          age;                // age of the person
	public int          sex;                // sex; male = 0, female = 1
	public int          marriageDivision;   // marriage division for kinRestrictionMode 3
	public boolean      live;               // live (true) or dead (false)
	public int          birthWeek;          // week number (1-52) of person's birth
	public int          birthStep;          // step of person's birth
	public int          pregnancyWeeks;     // # weeks since female became pregnant
	public boolean      fertile;            // female is either fertile or not
	public int          stepsPPA;           // number of steps female has been infertile following birth
	public boolean      previouslyMarried;  // previously married (true) or not (false)
	public int          ageAtMarriage;      // tracks person's age at marriage
	public int          ageAtDeath;         // tracks person's age at death
	public boolean      infanticideRiskExposed; //so infants can only be exposed to infanticide risk once
	public boolean      usedForOY;          // so that people don't get double counted for OY ratio calc
	
	//lists to hold marriage partners, consanguines, children, co-residents, tools, etc.
	public ArrayList <Person> femaleMateList =  new ArrayList <Person> (); // Current mates of male
	public ArrayList <Person> maleMateList = new ArrayList <Person> ();    // Current mate of female
	public ArrayList <Person> childList = new ArrayList <Person> ();       // DESCENT: children of each person
	public ArrayList <Person> familyList = new ArrayList <Person> ();      // DESCENT: blood-related family
	public ArrayList <Person> siblingList = new ArrayList <Person> ();     // DESCENT: blood-related siblings
	public ArrayList <Person> coResidentList = new ArrayList <Person> ();  // PROXIMITY
	public ArrayList <Person> kinList = new ArrayList <Person> ();         // KIN
	public ArrayList <Person> cousinList = new ArrayList <Person> ();      // COUSINS
	public ArrayList <Person> stepFamilyList = new ArrayList <Person> ();  // STEPCHILDREN and STEPPARENTS
	public ArrayList <Link> personLinkList = new ArrayList <Link> (); //holds all links from person

	/////////////////////////////////////////////////////////////////////////////////////////////
	// SETTERS AND GETTERS
	/////////////////////////////////////////////////////////////////////////////////////////////

	public int getId() {  return id; }

	public Household getCurrentHousehold() {return currentHousehold; }
	public void setCurrentHousehold (Household curH) {currentHousehold = curH; }

	public Person getFather() {return father; }
	public void setFather (Person f) {father = f;}

	public Person getMother() {return mother; }
	public void setMother (Person m) {mother = m;}

	public int getAge() { return age; }
	public void setAge( int a ) { age = a; }

	public boolean getLive() { return live; }
	public void setLive( boolean l ) { live = l; }

	public int getSex() { return sex; }
	public void setSex( int s ) { sex = s; }

	public int getBirthWeek() { return birthWeek; }
	public void setBirthWeek( int bw ) { birthWeek = bw; }

	public int getBirthStep() { return birthStep; }
	public void setBirthStep( int bs ) { birthStep = bs; }

	public int getPregnancyWeeks() {return pregnancyWeeks; }
	public void setPregnancyWeeks (int pw) {pregnancyWeeks = pw; }

	public boolean getFertile() {return fertile;}
	public void setFertile (boolean fert) {fertile = fert; }

	public int getStepsPPA() { return stepsPPA; }
	public void setStepsPPA (int sppa) { stepsPPA = sppa; }

	public boolean getPreviouslyMarried() { return previouslyMarried; }
	public void setPreviouslyMarried( boolean pm ) { previouslyMarried = pm; }

	public int getMarriageDivision () {return marriageDivision; }
	public void setMarriageDivision (int md ) {marriageDivision = md; }

	public int getAgeAtMarriage() { return ageAtMarriage; }
	public void setAgeAtMarriage (int afm) {ageAtMarriage = afm; }

	public int getAgeAtDeath() { return ageAtDeath; }
	public void setAgeAtDeath (int ad) {ageAtDeath = ad; }

	public boolean getUsedForOY() { return usedForOY; }
	public void setUsedForOY (boolean ofy) { usedForOY = ofy; }

	public int getNumChildren() { return childList.size(); }
	public int getNumSiblings() { return siblingList.size(); }
	public int getNumLinks() { return personLinkList.size(); }

	public int getX() { return x; }
	public void setX( int i ) { x = i; }

	public int getY() { return y; }
	public void setY( int i ) { y = i; }

	public static void resetNextId() { nextId = 0; }  // call when we reset the model

	////////////////////////////////////////////////////////////////////////////////////
	//1. INCREMENT AGE
	//////////////////////////////////////////////////////////////////////////////////
	public void incrementAge () {
		age = age + 1;                                   // make person one year older

		if (sex == 1) {                                  // if female
			if (age == Model.getAgeAtMaturity()) {       // and if just becoming mature
				fertile = true;                          // set fertile to true
			}
		}

		int t1Start = Model.getT1Start();                // start of data collection period (T1)
		int t1Stop = Model.getT1Stop();                  // end of data collection period (T1)
		if (birthStep >= t1Start) {                      // if person is born in T1
			if (birthStep < (t1Stop - Model.getMaxAge()- 1)) { // but soon enough to live out life in T1
				Model.countLivingPerson(this);           // send to tally
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	//2. BEGIN PERSON-LEVEL PAIR BOND METHODS
	////////////////////////////////////////////////////////////////////////////////////////////
	public void beginPersonPairBondMethods () {
		
	//	System.out.printf ("PERSON: initiating pairBond methods\n");

		int pairBondMode = Model.getPairBondMode();

		if (getNumCurrentFemaleMates() == 0)            // if male has no current mates
			findMate();                                 // go straight to attempt to find mate

		if (getNumCurrentFemaleMates() > 0) {           // if male already has mate(s)				
			if (pairBondMode == 2)                      // and polygyny is permitted
				evaluatePairBondEconomics();            // do male side economic calculations
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	//3. EVALUATE PAIR BOND HOUSEHOLD ECONOMICS
	// -do analysis to evaluate the household economics of adding an additional female pair bond
	// -final probablities subject to model parameter controlling relevance of household economics
	// -if "householdEconWeight" = 1, the p of NOT attempting to add a female is unaltered
	// - the less the value of "householdEconWeight", the more the p of NOT adding a female is reduced
	// - when "householdEconWeight" = 0, an attempt will be made to add another female with 100% chance
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void evaluatePairBondEconomics() {

		double econWeight = Model.getHouseholdEconWeight();        // get value of weighting parameter

		int curNumProducers = currentHousehold.getNumProducers();  // current number of producers
		int curNumConsumers = currentHousehold.getSize();          // current number of consumers
		double curCPR = currentHousehold.getCPRatio();             // current CPR
		int condNumProducers = curNumProducers + 1;                // numProducers if new female is added
		int condNumConsumers = curNumConsumers + 1;                // numConsumers if new female is added
		double condCPR = (double) condNumConsumers / (double) condNumProducers;  // conditional CPR

		double econBenefit = (curCPR - condCPR) / curCPR;          // relative econ benefit of adding female  


		double pNotAttempt = (1 - econBenefit) * econWeight;       // p of not attempting to add female
		double pAttempt = 1 - pNotAttempt;           // convert back to p of attempting to add mate

		if (Model.getMateScarcityAdjustSwitch() == true) {         // if mate scarity is relevant
			pAttempt = eligibleFemaleScarcityAdjustment(pAttempt); // send p to be adjusted
		}

		double roll = RandomHelper.nextDoubleFromTo(0,1);            // roll the dice

		if (roll < pAttempt) {                                      // if successful
			findMate();                                             // attempt to find another mate
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 4. ELIGIBLE FEMALE SCARCITY ADJUSTMENT
	// -this method adjusts the p of attempting to add a mate based on the scarcity of mates
	// -only called when: 
	//  (1) male is NOT on first marriage
	//  (2) mateScarcity adjustment switch is "on"
	// -alters pAttempt calculated in evaluatePairBondEconomics method and returns new pAttempt
	////////////////////////////////////////////////////////////////////////////////////////////////
	public double eligibleFemaleScarcityAdjustment (double pAttempt) {

		double mateScarcityAdjustment = Model.getMateScarcityAdjustment(); // will be between -1 and 1

		if (mateScarcityAdjustment > 0) {                                  // if we're in positive territory
			pAttempt = pAttempt + (pAttempt * mateScarcityAdjustment); // if MS is max, p will be doubled
		}

		return pAttempt;                                                   // return altered pAttempt
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	//5. FIND MATE
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void findMate() {

		boolean found = false;
		int numFemales = Model.getNumEligibleFemales();                 // n of potential mates

		for (int i = 0; i < numFemales; ++i) {                          // loop - try them all if necessary
			if (found == false) {                                       // if this is false
				Person potentialMate = Model.getPotentialFemaleMate(i); // get first/next one
				if (potentialMate.getLive() == true) {                  // make sure she's alive
					if (potentialMate.getNumCurrentMaleMates() == 0) {  // and not pair-bonded
						if (potentialMate.getPregnancyWeeks() == 0) {   // and not pregnant
							found = true;                               // set this to true to end loop
							attemptPairBond (potentialMate);            // attempt pair bond
						}
					}
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	//6. ATTEMPT PAIR BOND
	// - called after a potential mate is selected in findMate 
	// - goes through a series of checks: incest violation, economic liability, female econ calculations,
	/////////////////////////////////////////////////////////////////////////////////////////////////
	public void attemptPairBond (Person potentialMate) {

		Model.incrementNumPairBondAttempts();

		boolean successfulPairBond = true;                      // start with this being "true"

		if (potentialMate.checkForIncest(this) == true)         // if bond would violate incest prohibition
			successfulPairBond = false;                         // set this to false

		if (econLiabilityCheck(potentialMate) == true)          // if match fails economic liability test
			successfulPairBond = false;                         // set this to false

		if (potentialMate.calcFemaleEconomics(this) == false)   // if female does not accept bond economics
			successfulPairBond = false;                         // set this to false

		if (successfulPairBond == true)                         // if match passed all the tests
			createPairBond (potentialMate);                     // create the pair bond
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 7. CHECK FOR MARRIAGE INCEST PROHIBITION
	// -this checks for incest by consulting lists according to pairBondRestrictionMode
	////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean checkForIncest (Person potentialMate) {

		boolean incest = false;
		int mode = Model.getPairBondRestrictionMode();                  // get the value from model

		if (mode == 0) {                                                // if we're in mode 0
			incest = false;                                             // this will always be false
			return incest;                                              // return result
		}

		if (mode == 1) {                                                // if we're in mode 1
			for (Person kinPerson : kinList)                            // go through kinList
				if (kinPerson == potentialMate) {incest = true; }       // true if kin
			for (Person cousinPerson : cousinList)                      // but go through cousinList
				if (cousinPerson == potentialMate) {incest = false; }   // if cousin, make false
			for (Person familyPerson : familyList)                      // now go through familyList
				if (familyPerson == potentialMate) {incest = true; }    // true if family
			for (Person coPerson : coResidentList)                      // go through coResidentList
				if (coPerson == potentialMate) { incest = true; }       // true if coResident
			for (Person stepPerson : stepFamilyList)                    // go through stepFamilyLIst
				if (stepPerson == potentialMate) { incest = true; }     // true if stepfamily

			return incest;                                              // return result
		}

		if (mode == 2) {                                                // if we're in mode 2
			for (Person kinPerson : kinList)                            // go through kinList
				if (kinPerson == potentialMate) {incest = true; }       // true if kin (no cousin excpetion)
			for (Person familyPerson : familyList)                      // now go through familyList
				if (familyPerson == potentialMate) {incest = true; }    // true if family
			for (Person coPerson : coResidentList)                      // go through coResidentList
				if (coPerson == potentialMate) { incest = true; }       // true if coResident
			for (Person stepPerson : stepFamilyList)                    // go through stepFamilyLIst
				if (stepPerson == potentialMate) { incest = true; }     // true if stepfamily

			return incest;                                              // return result
		}

		if (mode == 3) {                                          //if we're in mode 3 (marriage divisions)
			for (Person kinPerson : kinList)                           // go through kinList
				if (kinPerson == potentialMate) {incest = true; }      // true if kin
			for (Person cousinPerson : cousinList)                     // but go through cousinList
				if (cousinPerson == potentialMate) {incest = false; }  // if cousin, make false
			for (Person familyPerson : familyList)                     // now go through familyList
				if (familyPerson == potentialMate) {incest = true; }   // true if family
			for (Person coPerson : coResidentList)                     // go through coResidentList
				if (coPerson == potentialMate) { incest = true; }      // true if coResident
			for (Person stepPerson : stepFamilyList)                   // go through stepFamilyLIst
				if (stepPerson == potentialMate) { incest = true; }    // true if stepfamily

			if (marriageDivision != potentialMate.getMarriageDivision())  //if no match
				incest = true;                                         // make true

			return incest;                                             // return result

		}

		if (mode == 4) {                                               // if in mode 4 (family only)
			for (Person familyPerson : familyList)                     // go through familyList
				if (familyPerson == potentialMate) {incest = true; }   // true if family

			return incest;                                             // return result
		}

		System.err.printf ("ERROR: went through checkForIncest method with no result\n");
		return incest;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	// 8. ECONOMIC LIABILITY CHECK
	// -evaluates to what degree the addition of the chosen female would push male's household over
	//  sustainable CP ratio
	// -only kicks in if female has existing dependents
	// -subject to householdEconWeight parameter from model
	////////////////////////////////////////////////////////////////////////////////////////////
	public boolean econLiabilityCheck(Person potentialMate) {

		boolean rejectFemale = false;
		double pAccept = 0;
		double econWeight = Model.getHouseholdEconWeight();
		double sustainableCP = Model.getSustainableCP();
		double condCP = 0;                                                  // CP ratio if households blend

		if (potentialMate.getNumDependents() == 0) {                        // if female has no dependents
			return rejectFemale;                                            // approve bond and send result
		}

		else {                                                              // if female has dependents
			int numDependents = potentialMate.getNumDependents();           // get n female's dependents

			if (getPreviouslyMarried() == true) {                          // if M has been previously married
				int numMaleConsumers = currentHousehold.getSize();         // n consumers male household
				int numMaleProducers = currentHousehold.getNumProducers(); // n producers male household
				int totalConsumers = numMaleConsumers + numDependents + 1; // total consumers 
				int totalProducers = numMaleProducers + 1;  	           // total producers
				condCP = (double) totalConsumers / (double) totalProducers;  // CP if households blend
			}

			if (getPreviouslyMarried() == false) {         // if male has never been married before
				condCP = (numDependents + 2) / 2;          // male and female are only producers
			}

			if (condCP <= sustainableCP) {                 // if resulting household is below sustainable CP
				return rejectFemale;                   // return positive result
			}

			else {
				pAccept = (condCP - sustainableCP) / sustainableCP;   // proportion above sustainable CP
				double pReject = 1 - pAccept;                         // p of rejecting match
				pReject = pReject * econWeight;                       // apply weighting parameter from model
				pAccept = 1 - pReject;                                // convert back to pAccept
			}

			double roll = RandomHelper.nextDoubleFromTo(0,1);          // roll the dice

			if (roll > pAccept) {                                     // if roll is over     
				rejectFemale = true;     // female's dependents are too much of a liability, reject marriage
			}

			return rejectFemale;         // return result
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// 9. CALCULATE FEMALE-SIDE ECONOMICS
	// -female bases her decision to pair bond on difference in economic situations between her 
	// current situation and the one that she would be in if pair-bonded with the male
	///////////////////////////////////////////////////////////////////////////////////////////////
	public boolean calcFemaleEconomics (Person male) {

		double econWeight = Model.getHouseholdEconWeight();
		boolean femaleWilling = true;                              // variable that gets returned
		Household femaleHouse = currentHousehold;                  // female's household
		Household maleHouse = male.getCurrentHousehold();          // male's household

		double femaleHouseCPR = 1;                
		double maleHouseCPR = 1;

		if (femaleHouse != null)                                   // if we're not at model startup
			femaleHouseCPR = femaleHouse.getCPRatio();             // CP ratio of female's current house

		if (male.getPreviouslyMarried() == true) {                 // if male has own household
			int curNumProducers = maleHouse.getNumProducers();     // current number of producers
			int curNumConsumers = maleHouse.getSize();             // current number of consumers
			int condProducers  = curNumProducers + 1;              // producers if female joins
			int condConsumers = curNumConsumers + 1;               // consumers if female joins
			maleHouseCPR = (double) condConsumers / (double) condProducers;    // CPR if female joins
		}

		double pAccept = 1 / (maleHouseCPR / femaleHouseCPR);  
		// if maleHouseCPR is < femaleHouseCPR, result will be over 1; when situation 2x as much work, p = 0.5

		double pReject = 1 - pAccept;             // chance of rejecting pair bond
		pReject = pReject * econWeight;           // adjust based on household economics parameter
		pAccept = 1 - pReject;                    // change back into pAccept

		if (Model.getMateScarcityAdjustSwitch() == true) {     // if scarcity of mates matters
			pAccept = eligibleMaleScarcityAdjustment(pAccept); // adjust pAccept
		}

		double roll = RandomHelper.nextDoubleFromTo (0, 1);     // roll the dice

		if (roll >  pAccept)                                   // if roll comes in over
			femaleWilling = false;                             // female rejects pairbond

		return femaleWilling;                                  // return result
	}

	///////////////////////////////////////////////////////////////////////////////////////////////
	//10.  ELIGIBLE MALE SCARCITY ADJUSTMENT
	// -this method adjusts the p of females accepting a pair bond based on the scarcity of male mates
	// -only called when mateScarcityAdjustment switch is "on"
	// -alters pAccept calculated in calcFemaleEconomics method and returns new pAccept
	/////////////////////////////////////////////////////////////////////////////////////////////////
	public double eligibleMaleScarcityAdjustment (double pAccept) {

		double mateScarcityAdjustment = Model.getMateScarcityAdjustment(); // will be between -1 and 1

		if (mateScarcityAdjustment < 0) {                                  // if we're in negative territory
			pAccept = pAccept + (pAccept * mateScarcityAdjustment * -1);   // adjust pAccept
		}

		return pAccept;                                                    // return altered pAccept
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	//11. CREATE PAIR BOND
	///////////////////////////////////////////////////////////////////////////////////////////
	public void createPairBond (Person female) {

		//System.out.printf ("PERSON: creating pairBond . . .\n");
		
		Model.incrementNumPairBonds();

		addFemaleMate (female);                            // add female to male's current mate list
		female.addMaleMate (this);                         // add male to female's current mate list
		Model.createMarriageLink (this, female, 3, 0);     // create link from male to female
		Model.createMarriageLink (female, this, 3, 1);     // create link from female to male

		if (Model.getPairBondRestrictionMode() != 0) {     // if there are marriage prohibitions
			identifyInLaws (female);                       // identify male's in-laws (add to kinList)
			female.identifyInLaws (this);                  // identify female's in-laws (add to kinList)
			identifyStepChildren (female);                 // identify female's existing children
			female.identifyStepChildren (this);            // identify male's existing children
		}

		Household oldFemaleHousehold = female.getCurrentHousehold(); // get female's old household

		if (oldFemaleHousehold != null)                    // if female is in a household
			oldFemaleHousehold.removeSelf (female);        // remove her from household

		if (previouslyMarried == true) {                   // if male has establlished own household          
			currentHousehold.addSelf (female);             // add female to male's established household
		}

		if (previouslyMarried == false) {                  // if male is previously unmarried . . .

			if (currentHousehold != null)                  // if male is in a house (childhood house)
				currentHousehold.removeSelf (this);        // remove him from household (group stays same)

			Household newHousehold = new Household (this, female);   // create new household
			Model.addHouseholdToList(newHousehold);        // add it to the lists kept in model
			female.setCurrentHousehold (newHousehold);     // set female's household to new household
			currentHousehold = newHousehold;               // set male's household to new household
			ageAtMarriage = age;                           // record male's marriage age
			previouslyMarried = true;                      // change to "true"
		}

		female.identifyCoResidents (currentHousehold);     // check for non-family co-residents to add to list
		female.switchChildrenToNewHousehold (oldFemaleHousehold, currentHousehold); 
		Model.removeFromEligibleFemaleList (female);       // remove female from list of eligible females

		if (female.getPreviouslyMarried() == false) {      // if this is female's first marriage
			female.setAgeAtMarriage(female.getAge());      // record marriage age
			female.setPreviouslyMarried(true);                       
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// 12. IDENTIFY IN-LAWS
	// -called alternately by male and female when pairbond is created
	// -adds parents, and grandparents of marrying people to kinLists
	// -creates kin links with (living) mothers-in-law and fathers-in-law, siblings-in-law, nieces and
	//  nephews-in-law
	///////////////////////////////////////////////////////////////////////////////////////////////

	public void identifyInLaws (Person mate) {

		if (father != null) {                                          // if ego has a father
			mate.addMeToYourKinList (father);                          // add him to mate's kinList      
			father.addMeToYourKinList (mate);                          // add mate to father's kinList

			if (father.getLive() == true) {                            // if father is alive
				Model.createKinLink (father, mate, 4, 1);              // create kin links between them
				Model.createKinLink (mate, father, 4, 2);
			}

			Person paternalGF = father.getFather();                    // now get father's father
			Person paternalGM = father.getMother();                    // and father's mother

			if (paternalGF != null) {                                  // if paternal grandfather exists
				mate.addMeToYourKinList (paternalGF);                  // add to mate's kinList      
				paternalGF.addMeToYourKinList (mate);                  // add mate to grandfather's kinList

				if (paternalGF.getLive() == true) {                    // if grandfather is alive
					Model.createKinLink (paternalGF, mate, 4, 1);      // create kin links
					Model.createKinLink (mate, paternalGF, 4, 2);
				}
			}

			if (paternalGM != null) {                                   // if paternal grandmother exists
				mate.addMeToYourKinList (paternalGM);                   // add to mate's kinList      
				paternalGM.addMeToYourKinList (mate);                   // add mate to grandmother's kinList

				if (paternalGM.getLive() == true) {                     // if grandmother is alive
					Model.createKinLink (paternalGM, mate, 4, 1);       // create kin links
					Model.createKinLink (mate, paternalGM, 4, 2);
				}
			}
		}

		if (mother != null) {                                           // if ego has a mother
			mate.addMeToYourKinList (mother);                           // add to mate's kinList
			mother.addMeToYourKinList (mate);                           // add mate to mother's kinList
			if (mother.getLive() == true) {                             // if mother is alive
				Model.createKinLink (mother, mate, 4, 1);               // create kin links
				Model.createKinLink (mate, mother, 4, 2);
			}

			Person maternalGF = mother.getFather();                    // now get mother's father
			Person maternalGM = mother.getMother();                    // and mother's mother

			if (maternalGF != null) {                                  // if grandfather exists
				mate.addMeToYourKinList (maternalGF);                  // add to mate's kinList      
				maternalGF.addMeToYourKinList (mate);                  // add mate to grandfather's kinList

				if (maternalGF.getLive() == true) {                    // if grandfather is alive
					Model.createKinLink (maternalGF, mate, 4, 1);      // create kin links
					Model.createKinLink (mate, maternalGF, 4, 2);
				}
			}

			if (maternalGM != null) {                                  // if mate's grandmother exists
				mate.addMeToYourKinList (maternalGM);                  // add her to mate's kinList      
				maternalGM.addMeToYourKinList (mate);                  // add mate to grandmother's kinList

				if (maternalGM.getLive() == true) {                    // if she's alive
					Model.createKinLink (maternalGM, mate, 4, 1);      // create kin links
					Model.createKinLink (mate, maternalGM, 4, 2);
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 13. IDENTIFY STEP-CHILDREN
	// -called when pair bond created to identify children existing from previous pair bond
	// - called from both male and female sides
	/////////////////////////////////////////////////////////////////////////////////////////////
	public void identifyStepChildren (Person mate) {

		int numMateChildren = mate.getNumChildren();              // get number of mate's offspring

		if (numMateChildren > 0) {                                // if mate has offpsring
			for (int i = 0; i < numMateChildren; ++i) {           // go through mate's offspring
				Person child = mate.getChild (i);                 // get one
				if (child.getLive() == true) {                    // if he/she is alive
					if (checkFamilyList(child) == false) {        // and not on ego's familyList
						stepFamilyList.add(child);                // add to stepFamily list
						child.addMeToYourStepFamilyList(this);    // add person to child's stepFamily list
						Model.createKinLink(this, child, 4, 9);   // create kin links
						Model.createKinLink(child, this, 4, 10);
					}
				}
			}
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// 14. IDENTIFY CO-RESIDENTS
	// -called at birth to identify otherwise unrelated co-residents of household
	// -called when person enters new household (marriage, orphan, etc.)
	// -adds them to coResident list, creates links
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void identifyCoResidents(Household newHouse) {

		int numInHousehold = newHouse.getSize();                         // current total size of household

		for (int i = 0; i < numInHousehold; ++i) {                       // go through household
			Person householdPerson = newHouse.getResident(i);            // get person
			if (householdPerson != this) {                               // if not this person
				if (checkCoResidentList (householdPerson) == false) {    // if not already on coResidentList
					coResidentList.add (householdPerson);                // add to coResidentList
					householdPerson.addMeToYourCoResidentList (this);

					if (checkListForExistingLink(householdPerson) == true) {       // if a link exists
						Link existingLink = Model.getLink(this, householdPerson);  // get it

						if (existingLink.getLinkType() == 5) {      // if link is an acquaintance link
							existingLink.setLinkType (2);           // update to coRes link type
							existingLink.setLinkSubType (0);        // udpate to coRes link subtype
							//NOTE: links other than acquaintance links are just left alone
						}
					}

					else {                                          // if no link exists

						Model.createCoResidentLink (this, householdPerson, 2, 0);  // create links
						Model.createCoResidentLink (householdPerson, this, 2, 0);    
					}
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// 15. SWITCH CHILDREN FROM OLD TO NEW HOUSEHOLD
	//////////////////////////////////////////////////////////////////////////////////////////////
	public void switchChildrenToNewHousehold (Household oldHousehold, Household newHousehold) {  
		if (childList.size() != 0) {                                // if there are children on the list
			for (Person child : childList) {                        // cycle through child list
				if (child.getCurrentHousehold() == oldHousehold) {  // if child is still with mother
					if (child.getLive() == true) {                  // and still alive
						oldHousehold.removeSelf(child);             // remove child from old household
						newHousehold.addSelf(child);                // add child to new household
						child.identifyCoResidents(newHousehold);    // call method to identify coResidents
					}
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// 16. CHECK FERTILITY
	// -adjusts fertility status based on age, post partum ammenorrhea, etc.
	// -this method can only change a status of "false" to true, not other way around
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void checkFertility() {

		int maxPPA = Model.getMaxPPA();
		int ageAtWeaningWeight = Model.getAgeAtWeaningWeight();
		int ageAtWeaning = Model.getAgeAtWeaning();

		if (fertile == false) {                             // if status is currently "false"
			if (pregnancyWeeks == 0) {                      // but female is not pregnant
				boolean unweanedChild = false;              // variable to track if female has unweaned child

				for (Person child : childList) {            // go through female's childList
					if (child.getAge() < ageAtWeaning)      // if female has unweaned child
						if (child.getLive() == true)        // and that child is alive
							unweanedChild = true;           // set this to true
				}

				if (unweanedChild == true) {                // if unweaned child present
					if (ageAtWeaningWeight == 0) {          // and weaning is irrelevant
						double pFertile = (1 / maxPPA);     // base p of becoming fertile each step
						double roll = RandomHelper.nextDoubleFromTo (0, 1);   //roll the dice
						if (roll < pFertile) {              // if roll successful  
							fertile = true;                 // set fertile to "true"
							stepsPPA = 0;                   // reset this to 0
						}

						if (roll > pFertile) {              // if roll unsuccessful
							++stepsPPA;                     // increment stepsPPA
						}

						if (stepsPPA >= maxPPA) {           // if maxPPA period has passed
							fertile = true;                 // set fertile to "true"
							stepsPPA = 0;                   // reset this to 0
						}
					}
				}
				if (unweanedChild == false) {               // if no unweaned child present
					fertile = true;                         // set fertile to "true"
					stepsPPA = 0;                           // reset this to 0
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 17. PREGNANCY
	// - obtains base p of pregnancy from model based on age-specific fertility schedule
	// - opportunity to avoid pregnancy based on household economics
	////////////////////////////////////////////////////////////////////////////////////////////////////	
	public void pregnancy() {

		double baseFert = Model.getBaseFertility (age);           // send age, get age-specific fertility
		double pPregnancy = baseFert / Model.getStepsPerYear() ;  // calc prob of pregnancy each step
		boolean pregnant = false;
		double p = RandomHelper.nextDoubleFromTo (0, 1);           // roll the dice	

		if (p <= pPregnancy)                                      // if dice roll successful
			pregnant = true;                                      // set this to true

		if (pregnant == true) {                                   // if this became true
			if (avoidPregnancy() == true)                         // check avoidance
				pregnant = false;                                 // set this to false
		}

		if (pregnant == true)   {                                 // if this remained true
			pregnancyWeeks = 1;                                   // female has become pregnant
			fertile = false;                                      // set fertile to false
			stepsPPA = 0;                                         // reset this to 0
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 18. AVOID PREGNANCY 
	// -this method represents a mechanism for pregnancy/birth to be avoided through contraception or
	//  abortion
	// -female compares the CP ratio of the household to the sustainable CP ratio (from model), makes 
	//  proportional roll to see if pregnancy is avoided or would be terminated
	///////////////////////////////////////////////////////////////////////////////////////////////
	public boolean avoidPregnancy() {

		boolean avoid = false;
		double econWeight = Model.getHouseholdEconWeight();

		if (Model.getAvoidanceOn() == true) {                   // if avoidance is turned "on" in model

			// calculate CP raio of the household if another child is added
			double condHouseholdCP = currentHousehold.getConditionalCPRatio();    
			double sustainableCP = Model.getSustainableCP();    // get the standard for "viability"

			if (condHouseholdCP > sustainableCP) {              // if CP is higher than it should be

				//calculate % above sustainable; 2x CP --> pInfanticide = 1
				double pAvoid = ((condHouseholdCP - sustainableCP) / sustainableCP);
				pAvoid = pAvoid * econWeight;                   // adjust based on econ param
				double roll = RandomHelper.nextDoubleFromTo(0, 1);     //roll dice

				if (roll < pAvoid)                              // if roll is low enough
					avoid = true;                               // avoid pregnancy
			}	
		}
		return avoid;                                           // return result
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 19. GIVE BIRTH
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void giveBirth() {

	//	System.out.printf("PERSON: giving birth . . .\n");
		
		Person child = Model.createChild(this);          // get new person from model
		
		Context <Person> momContext;
		momContext = ContextUtils.getContext (this); //get mother's context
		momContext.add(child);
		
		if (space == null) {System.err.printf("PERSON.giveBirth . . . space is null\n");}
		
		NdPoint spacePt = space.getLocation(this);  //get mother's location
		space.moveTo (child, spacePt.getX(), spacePt.getY());
		
		GridPoint pt = grid.getLocation(this);      //get mother's gridPoint
		grid.moveTo(child, pt.getX(), pt.getY());
		
		if (maleMateList.size() == 1) {                  // if there is a current male mate
			Person father = getMaleMate(0);              // get him from list
			father.addChild (child);                     // add child to father's list
			child.setFather (father);                    // set father of child
		}

		child.setMother (this);                          // set mother of child
		child.setX(x);                                   // set coordinates to mom's X and Y
		child.setY(y);
		
	//	System.out.printf("mother's x y = %d %d\n", x, y);
		
		addChild (child);                                // add child to mother's list
		currentHousehold.addSelf(child);                 // add child to household

		if (Model.getPairBondRestrictionMode() != 0) {   // if we're not in "no incest" mode
			child.identifyFamily();                      // add descent-related family to child's familyList
			child.identifyMaternalKin();                 // add kin on mom's side to kinList
			child.identifySiblingKin();                  // identify children of siblings and add to kinList

			if (child.getFather() != null) {             // if there is a father
				child.identifyPaternalKin();             // do paternal kin
			}

			child.identifyCoResidents(currentHousehold); // add co-residents to coResidentList
		}
		
		pregnancyWeeks = 0;                              // set pregnancyWeeks back to 0
	}

	//////////////////////////////////////////////////////////////////////////////////////////////
	// 20. IDENTIFY FAMILY MEMBERS
	// -called from birth by child to put family on familyList
	// -and puts child on family members' lists
	// -creates family-level links between child and family members
	// -descent: parents, grandparents, and siblings of ego
	////////////////////////////////////////////////////////////////////////////////////////////
	public void identifyFamily () {

		//add mother; create mother/child links
		familyList.add (mother);                          // add mother to child's familyList
		mother.addMeToYourFamilyList (this);              // mother adds child to her familyList
		Model.createFamilyLink (this, mother, 1, 2);      // create link from child to mother
		Model.createFamilyLink (mother, this, 1, 1);      // create link from mother to child

		if (mother.getLive() != true)                     // if mother is dead
			System.err.printf ("ERROR: mother is dead at child's birth\n");      // something wrong

		if (father != null) {                             // if there is a father
			familyList.add (father);                      // add father to child's familyList
			father.addMeToYourFamilyList (this);          // father adds child to his familylist
			Model.createFamilyLink (this, father, 1, 2);  // create link from child to father
			Model.createFamilyLink (father, this, 1, 1);  // create link from father to child
		}

		//for adding grandparents if they exist
		if (father != null) {                             // if there is a father
			Person paternalGF = father.getFather();       // get father's father
			Person paternalGM = father.getMother();       // get father's mother

			if (paternalGF != null) {                                  // if not null
				familyList.add (paternalGF);                           // add to child's familyList
				paternalGF.addMeToYourFamilyList(this);                // add child to GF's familyList
				if (paternalGF.getLive() == true) {                    // if GF is alive
					Model.createFamilyLink (this, paternalGF, 1, 2);   // create link from child to GF
					Model.createFamilyLink (paternalGF, this, 1, 1);   // create link from GF to child
				}
			}

			if (paternalGM != null) {                                  // if not null
				familyList.add (paternalGM);                           // add to child's familyList
				paternalGM.addMeToYourFamilyList(this);                // add child to GM's familyList
				if (paternalGM.getLive() == true) {                    // if GM is alive
					Model.createFamilyLink (this, paternalGM, 1, 2);   // create link from child to GM
					Model.createFamilyLink (paternalGM, this, 1, 1);   // create link from GM to child
				}
			}
		}

		Person maternalGF = mother.getFather();                        // get mother's father
		Person maternalGM = mother.getMother();                        // get mother's mother

		if (maternalGF != null) {                                      // if maternal GF exists
			familyList.add (maternalGF);                               // add GF to child's familyList
			maternalGF.addMeToYourFamilyList(this);                    // add child to GF's familyList
			if (maternalGF.getLive() == true) {                        // if GF is alive
				Model.createFamilyLink (this, maternalGF, 1, 2);       // create link from child to GF
				Model.createFamilyLink (maternalGF, this, 1, 1);       // create link from GF to child
			}
		}

		if (maternalGM != null) {                                      // if maternal GM exists
			familyList.add (maternalGM);                               // add GM to child's familyList
			maternalGM.addMeToYourFamilyList(this);                    // add child to GM's familyList
			if (maternalGM.getLive() == true) {                        // if GM is alive
				Model.createFamilyLink (this, maternalGM, 1, 2);       // create link from child to GM
				Model.createFamilyLink (maternalGM, this, 1, 1);       // create link from GM to child
			}
		}

		//adding siblings (all mother's and father's offspring, whether or not in household)
		if (father != null) {                                         // if father exists
			int numFatherChildren = father.getNumChildren();          // number of father's offspring
			for (int i = 0; i < numFatherChildren; ++i) {             // go through father's offspring
				Person potentialSibling = father.getChild (i);        // get one
				if (potentialSibling != this) {                       // if child is not this child
					if (checkFamilyList (potentialSibling) == false) {  // if child is not already on list
						familyList.add (potentialSibling);              // add sibling to child's familyList
						potentialSibling.addMeToYourFamilyList (this);  // add child to sib's familyList

						if (potentialSibling.getLive() == true) {       // if sibling is still alive
							Model.createFamilyLink (this, potentialSibling, 1, 3); //create to link
							Model.createFamilyLink (potentialSibling, this, 1, 3); //create from link
						}
					}

					if (checkSiblingList (potentialSibling) == false) { //if sibling is not on siblingList
						siblingList.add (potentialSibling);             // add sibling to child's siblingList
						potentialSibling.addMeToYourSiblingList (this); // add child to sibling's siblingList
					}
				}
			}
		}

		int numMotherChildren = mother.getNumChildren();            // number of mother's offspring
		for (int i = 0; i < numMotherChildren; ++i) {               // go through mother's offspring
			Person potentialSibling = mother.getChild (i);          // get child
			if (potentialSibling != this) {                         // if child is not this child
				if (checkFamilyList (potentialSibling) == false) {  // if child is not already on list
					familyList.add (potentialSibling);              // add sibling to list
					potentialSibling.addMeToYourFamilyList (this);  // add this child to sibling's list

					if (potentialSibling.getLive() == true) {       // if child is alive
						Model.createFamilyLink (this, potentialSibling, 1, 3); // create link to sibling
						Model.createFamilyLink (potentialSibling, this, 1, 3); // create link from sibiling
					}
				}

				if (checkSiblingList (potentialSibling) == false) {  // if sibling is not on siblingList
					siblingList.add (potentialSibling);              // add sibling to siblingList
					potentialSibling.addMeToYourSiblingList (this);  // add child to sibling's siblingList
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// 21. IDENTIFY MATERNAL KIN
	// - identifies maternal aunts, uncles, and cousins and puts them on kin list
	// - creates kin ties with living kin
	// - identifies cousins and puts them on a separate cousin list (for use in marriage discrimination)
	// - called at birth by child
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void identifyMaternalKin() {

		int numMotherSiblings = mother.getNumSiblings();         // number of mother's siblings
		for (int i = 0; i < numMotherSiblings; ++i) {            // go through mother's siblings
			Person auntUncle = mother.getSibling (i);            // get one
			kinList.add (auntUncle);                             // add to kinList
			auntUncle.addMeToYourKinList (this);                 // add child to aunt/uncle's kinList

			if (auntUncle.getLive() == true) {                   // if aunt/uncle is alive
				Model.createKinLink (this, auntUncle, 4, 4);     // create kin links
				Model.createKinLink (auntUncle, this, 4, 3);
			}

			int numCousins = auntUncle.getNumChildren();         // get number of aunt/uncle's  children

			for (int c = 0; c < numCousins; ++c) {               // loop to go through potential cousins
				Person cousin = auntUncle.getChild(c);           // get one
				kinList.add (cousin);                            // add to kinList
				cousin.addMeToYourKinList (this);                // add child to cousin's kinList
				cousinList.add (cousin);                         // add to cousinList
				cousin.addMeToYourCousinList (this);             // add child to cousin's cousinList

				if (cousin.getLive() == true) {                  // if cousin is alive
					Model.createKinLink (this, cousin, 4, 5);    // create kin links
					Model.createKinLink (cousin, this, 4, 5);
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 22. IDENTIFY SIBLING KIN
	// -create kin ties with the children of siblings (i.e., nieces and nephews)
	//////////////////////////////////////////////////////////////////////////////////////////////
	public void identifySiblingKin () {

		for (Person sibling : siblingList) {                           // go through siblingList
			int numSiblings = sibling.getNumChildren();                // get number of sibling's kids
			for (int v = 0; v < numSiblings; ++v) {                    // go through them
				Person nieceNephew = sibling.getChild(v);              // get one
				kinList.add (nieceNephew);                             // add to kin list
				nieceNephew.addMeToYourKinList(this);                  // add me to your kin list

				if (nieceNephew.getLive() == true) {                   // if niece/nephew is alive
					Model.createKinLink (this, nieceNephew, 4, 3);     // create kin links
					Model.createKinLink (nieceNephew, this, 4, 4);
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// 23. IDENTIFY PATERNAL KIN
	// - this is only called at birth if father != null
	// - kin on paternal side
	//////////////////////////////////////////////////////////////////////////////////////////////////
	public void identifyPaternalKin () {

		int numFatherSiblings = father.getNumSiblings();       // number of father's siblings
		for (int i = 0; i < numFatherSiblings; ++i) {          // go through father's siblings
			Person auntUncle = father.getSibling (i);          // et one
			kinList.add (auntUncle);                           // add to kinList
			auntUncle.addMeToYourKinList (this);               // add child to aunt/uncle's kinList

			if (auntUncle.getLive() == true) {                 // if aunt/uncle is alive
				Model.createKinLink (this, auntUncle, 4, 4);   // create kin links
				Model.createKinLink (auntUncle, this, 4, 3);
			}

			int numCousins = auntUncle.getNumChildren();       // get number of aunt/uncle's children
			for (int c = 0; c < numCousins; ++c) {             // loop to go through potential cousins
				Person cousin = auntUncle.getChild(c);         // get one
				kinList.add (cousin);                          // add to kinList
				cousin.addMeToYourKinList (this);              // add child to cousin's kinList
				cousinList.add (cousin);                       // add to cousinList
				cousin.addMeToYourCousinList (this);           // add child to cousin's cousinList

				if (cousin.getLive() == true) {                // if cousin is alive
					Model.createKinLink (this, cousin, 4, 5);  // create kin links
					Model.createKinLink (cousin, this, 4, 5);
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 24. DISSOLVE PAIR BOND
	// - called from model on the female side
	///////////////////////////////////////////////////////////////////////////////////////////////
	public void dissolvePairBond () {
		Person currentMaleMate = maleMateList.get(0);             // get male mate
		currentMaleMate.removeFemaleMate(this);                   // remove female from his mate list
		removeMaleMate (currentMaleMate);                         // remove male from female's mate list

		Model.createExMateLink (this, currentMaleMate, 6, 0);     // convert links to ex-mate links
		Model.createExMateLink (currentMaleMate, this, 6, 0);

		if (maleMateList.size() != 0) {                           // if female still has mate
			System.err.printf ("Female has mate after pairbond dissolved\n");       // problem
		}

		currentHousehold.removeSelf(this);                        // remove female from household
		Household newHousehold = new Household (null, this);      // create new household for female
		switchChildrenToNewHousehold (currentHousehold, newHousehold); // put any children in it
		currentHousehold = newHousehold;                          // switch female to new household
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// 25. DEATH
	// - obtains age-specific yearly base probability of death
	// - adjusts for population size (porMortAdjustment)
	// - divides by number of steps in a year
	// - rolls the dice
	// - calls method to expose infants to infanticide (additional risk of mortality)
	///////////////////////////////////////////////////////////////////////////////////////////////////
	public void death() {

		double yearlyMortality = Model.getBaseMortality (age);                 // get base yearly mortality
		double popMortAdjustment = Model.getPopMortAdjustment();               // get mortality adjustment
		int stepsPerYear = Model.getStepsPerYear();                            // get steps per year
		double pDeath = (yearlyMortality * popMortAdjustment) / stepsPerYear;  // calc. probability of death
		double roll = RandomHelper.nextDoubleFromTo (0, 1);                     // get random number
		int maxAge = Model.getMaxAge();                                        // get maximum age

		if (roll < pDeath)                                                     // if roll < pDeath
			Model.killPerson(this);                                            // kill the person

		if (roll >= pDeath) {                                                  // if roll >= pDeath
			if (age == 0) {                                                    // and if person is an infant
				if (infanticideRiskExposed == false) {       // and not previously exposed to infanticide risk
					boolean infanticideDeath = calculateInfanticide (mother);  // send to infanticide method
					infanticideRiskExposed = true;                             // set this to true
					if (infanticideDeath == true) {                            // if result comes back "true"
						Model.killPerson (this);                               // kill the person
					}
				}
			}
		}

		if (age >= maxAge) {                                                   // if person is at maxAge
			Model.killPerson (this);                                           // kill the person
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 26. CALCULATE INFANTICIDE 
	// - called from death method to expose newborn infant to infanticide risk based on
	//   dependency ratio of household
	// - also exposes infant to risk of infanticide based on value of nonEconInfanticideRisk
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean calculateInfanticide (Person mother) {

		boolean infanticide = false;
		double nonEconInfanticideRisk = Model.getNonEconInfanticideRisk();

		if (Model.getInfanticideOn() == true) {                       // if infanticide is "on" in model

			Household motherHouse = mother.getCurrentHousehold();     // get household
			double econWeight = Model.getHouseholdEconWeight();       // get weighting parameter
			double householdCP = motherHouse.getCPRatio();            // get the CP raio of the household
			double sustainableCP = Model.getSustainableCP();          // get the standard for "viability"

			if (householdCP > sustainableCP) {                        // if CP is higher than sustainable CP

				//calculate % above sustainable; 2x CP --> pInfanticide = 1
				double pInfanticide = ((householdCP - sustainableCP) / sustainableCP);   
				pInfanticide = pInfanticide * econWeight;             // adjust by weight parameter
				double roll = RandomHelper.nextDoubleFromTo(0, 1);     // roll the dice

				if (roll < pInfanticide)                              // if roll is low enough
					infanticide = true;                               // commit infanticide
			}

			if (infanticide == false) {                               // if this has remained false
				if (nonEconInfanticideRisk != 0) {                    // but this parameter is > 0
					double roll = RandomHelper.nextDoubleFromTo(0, 1); // roll dice

					if (roll < nonEconInfanticideRisk) {              // if roll is low enough
						infanticide = true;                           // commit infanticide
					}
				}
			}
		}
		return infanticide;                                           // return result
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// 27. REMOVE LINKS TO ME
	// -called when person dies
	//////////////////////////////////////////////////////////////////////////////////////////////////
	public void removeLinksToMe () {

		int numPersonLinks = personLinkList.size();                // record original size of personLinkList
		
	//	System.out.printf("PERSON.removeLinksToMe . . . Dead person has %d links\n", numPersonLinks);
		
		int numLinksDeleted = 0;                                   // track number of links deleted

		ArrayList <Link> removeLinkList = new ArrayList <Link> (); // holds links to remove

		for (Link link : personLinkList) {                         // go through dead person's linkList
			Person toPerson = link.getToPerson();                  // get "to" person

			if (toPerson.getLive() == false)                                       // if "to" person is dead
				System.err.printf ("ERROR in REMOVE LINKS: toPerson is dead\n");   // something is wrong

			if (toPerson == this)                                  // if person was linked to himself . . .
				System.err.printf ("ERROR in REMOVE LINKS:  toPerson and fromPerson are identical\n"); 

			toPerson.deleteLinkToMe (this);                        // call method to delete link from toPerson
			removeLinkList.add(link);                              // add this link to the remove list
		}

		for (Link link : removeLinkList) {                         // now go through remove list
			personLinkList.remove(link);                           // remove link from dead person's list
			Model.deleteLink (link);                               // delete from model's linkList
			++numLinksDeleted;                                     // increment number of links deleted
		} 

		if (numPersonLinks != numLinksDeleted) {          // if the "delete" # doesn't match the original #
			System.err.printf ("ERROR in REMOVE LINKS TO ME (person %d): n= %d links, %d found\n", 
					id, numPersonLinks, numLinksDeleted);           // something went wrong
		}

		if (personLinkList.size() != 0)                            // if there are still links on list . . .
			System.err.printf ("ERROR in REMOVE LINKS TO ME - linkList size > 0 after completing method\n");
		
		removeLinkList.clear();
		removeLinkList = null;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// 28. FIND AND REMOVE LINK TO DEAD PERSON
	//     - called from removeLinksToMe to delete link to dead person
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public void deleteLinkToMe (Person deadPerson) {

		//System.out.printf("PERSON.deleteLinkToMe . . . trying to remove link\n");
		
		if (live == false)                                                                // if person dead
			System.err.printf ("ERROR in DELETE LINK TO ME: toPerson is already dead\n"); // problem

		Link deadLink = null;                        // store the link to deadPerson
		boolean found = false;                       // track if we've found it

		for (Link link : personLinkList) {           // go through person's linkList
			Person toPerson = link.getToPerson();    // get "to" person
			if (toPerson == deadPerson) {            // if "to" person matches deadPerson
				deadLink = link;                     // this is the correct link
				found = true;                        // update to true
			}
		}

		if (found == false) {                // if we didn't find the link to person on our list
			System.err.printf ("Could not find link from person %d (age %d) to person (%d) (age %d)\n", 
					id, age, deadPerson.getId(), deadPerson.getAge()); //something wrong

			boolean linkFoundInModel = Model.checkForExistingLink(this, deadPerson);  // check the model

			if (linkFoundInModel == true) {           // if link found
				System.err.printf ("ERROR in DELETE LINK TO ME: link found in model\n");

			}

			if (linkFoundInModel == false) {         // if link not found
				System.err.printf ("ERROR in DELETE LINK TO ME: link aslo not found in model\n");
			}
		}

		personLinkList.remove(deadLink);             // remove the link from person's linkList

		if (deadLink == null) {                      // if this is null we've got a problem
			System.err.printf ("ERROR in DELETE LINK TO ME: about to send null link to model\n");
		}

		Model.deleteLink(deadLink);                  // send link to model to delete from list and nullify
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// 29. CHECK ORPHAN STATUS
	// -if any resident of a household is of productive age (including the person for whom the
	//  status check is being run) then no resident of household is an orphan
	////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean checkOrphanStatus() {
		boolean orphan = true;                        // assume an individual is an orphan

		if (age >= Model.getAgeAtProduction()) {      // if person is producer age
			orphan = false;                           // person cannot be an orphan
		}

		else {                                        // if person not a producer
			if (currentHousehold != null) {                                // to get past start-up of model
				int numResidents = currentHousehold.getSize();             // get size of household
				for (int i = 0; i < numResidents; ++i) {                   // go through household residents
					Person resident = currentHousehold.getResident(i);     // get one
					if (resident.getAge() >= Model.getAgeAtProduction()) { // if resident is producer age
						orphan = false;                                    // person cannot be orphan
					}
				}
			}

			if (currentHousehold == null) {                               // if at startup of model 
				orphan = false;                                           // make this false
			}
		}

		return orphan;                                                    // return result
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////
	// 30. RE-HOUSE ORPHAN
	// -look for family relative of producing age living in different household, move there
	// -if no blood relative, look for former co-resident in different household
	// -if no co-relative, look for any linked person
	////////////////////////////////////////////////////////////////////////////////////////////////////
	public void reHouseOrphan () {

		Household newHouse = null;                                    // variable to hold orphan's new home
		boolean newHouseFound = false;                                // track if new household located
		int numFamily = familyList.size();                            // number on familyList
		int numCoResidents = coResidentList.size();                   // number on coResidentList
		int numOtherLinks = personLinkList.size();                    // number of links

		//check for a family member first
		if (numFamily > 0) {                                          // if there are people on familyList
			for (int i = 0; i < numFamily; ++i) {                     // go through list
				if (newHouseFound == false) {                         // if this is false
					Person familyMember = familyList.get(i);          // get family person
					if (familyMember.getLive() == true) {             // if person is alive
						if (familyMember.getAge() >= Model.getAgeAtProduction()) {  // if person is producer
							if (familyMember.getCurrentHousehold() != null) {       // if person has household
								newHouseFound = true;                 // set to true to cut off loop
								newHouse = familyMember.getCurrentHousehold();      // get household
								currentHousehold.removeSelf(this);    // remove self from old household
								newHouse.addSelf(this);               // put self in relative's household
							}
						}
					}
				}
			}
		}

		if (newHouseFound == false) {                                // if this is still false
			if (numCoResidents > 0) {                                // if person has co-residents on list
				for (int i = 0; i < numCoResidents; ++i) {           // go through list
					if (newHouseFound == false) {                    // if this is still false
						Person coRes = coResidentList.get(i);        // get person
						if (coRes.getLive() == true) {               // if person is alive
							if (coRes.getAge() >= Model.getAgeAtProduction()) {  // if person is producer
								if (coRes.getCurrentHousehold() != null) {       // and has household
									newHouseFound = true;            // set to true to cut off loop
									newHouse = coRes.getCurrentHousehold();      // get household
									currentHousehold.removeSelf(this);           // remove from old household
									newHouse.addSelf(this);          // put self in new household
								}
							}
						}
					}
				}
			}
		}

		if (newHouseFound == false) {                               // if this is still false
			if (numOtherLinks > 0) {                                // if there are linked people
				for (int i = 0; i < numOtherLinks; ++i) {           // cycle through
					if (newHouseFound == false) {                   // if this is false
						Link link = personLinkList.get(i);          // get link
						Person linkedPerson = link.getToPerson();   // get person
						if (linkedPerson.getLive() == true) {       // if person is alive
							if (linkedPerson.getAge() >= Model.getAgeAtProduction()) { // if person producer
								if (linkedPerson.getCurrentHousehold() != null) {      // and has household
									newHouseFound = true;           // set to true to cut off loop
									newHouse = linkedPerson.getCurrentHousehold();    // get household
									currentHousehold.removeSelf(this);   // remove self from old household
									newHouse.addSelf(this);         // put self in new household
								}
							}
						}
					}
				}
			}
		}

		if (newHouse != null) {                                    // if we have found a new household
			identifyCoResidents (newHouse);                        // call method to identify co-residents
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	// MINOR LINK METHODS (not described separately in documentation
	////////////////////////////////////////////////////////////////////////////////////////////////////////

	////////////////////////////////////////////
	// GET NUMBER OF KIN LINKS
	public int getNumKinLinks () {
		int numKinLinks = 0;
		for (Link link : personLinkList) {                          // go through linkList
			if (link.getLinkType() == 4)                            // if it's a kin link
				++numKinLinks;                                      // count it
		}
		return numKinLinks;                                         // return result
	}

	////////////////////////////////////////////
	// CHECK FOR NULL LINKS
	public void checkForNullLinks () {
		for (Link personLink : personLinkList) {                   // go through personLinkList
			if (personLink == null) {                              // if link is null
				System.err.printf ("ERROR: Link is null in person\n");    // error message
			}
		}
	}

	///////////////////////////////////////////
	// ADD TO LINK LIST
	public void addToLinkList (Link addLink) {
		personLinkList.add(addLink);                              // add link to linkList
	}

	//////////////////////////////////////
	// GET LINK i
	public Link getLink (int i) {
		Link requestedLink = personLinkList.get(i);              // get link number i
		return requestedLink;                                    // return link
	}

	////////////////////////////////////////////
	// GET iTH PERSON FROM LINK LIST
	public Person getLinkedPerson (int i) {

		Person linkedPerson = null;

		if (personLinkList.size() > 0)  {                       // if there are links on the list
			Link link = personLinkList.get(i);                  // get link i
			linkedPerson = link.getToPerson();                  // get person associated with that link
		}
		return linkedPerson;                                    // return person
	}

	/////////////////////////////////////
	// CHECK LIST FOR EXISTING LINK
	// -checks a person's link list to see if he/she is already linked to a given person
	public boolean checkListForExistingLink (Person askPerson) {

		boolean linkFound = false;

		if (personLinkList.size() != 0) {                      // if there are links on person's list
			for (Link link : personLinkList) {                 // go through linkList
				Person linkedPerson = link.getToPerson();      // get toPerson
				if (linkedPerson == askPerson) {               // if people match
					linkFound = true;                          // change to true
					return linkFound;                          // return result
				}
			}
		}
		return linkFound;                                      // return "false" result if no link found
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////
	// MINOR LIST METHOD (not described separately in documentation)
	/////////////////////////////////////////////////////////////////////////////////////////////////

	///////////////////////////////////////////////////////////////
	// ADD ME TO YOUR FAMILY LIST
	public void addMeToYourFamilyList (Person addPerson) {
		familyList.add (addPerson);                // add to familyList
	}

	///////////////////////////////////////////////////////////////
	// ADD ME TO YOUR SIBLING LIST
	public void addMeToYourSiblingList (Person addPerson) {
		siblingList.add (addPerson);              // add to siblingList
	}

	//////////////////////////////////////////////////////////////
	// ADD ME TO YOUR KIN LIST
	public void addMeToYourKinList (Person addPerson) {
		kinList.add (addPerson);                 // add to kinList
	}

	//////////////////////////////////////////////////////////////
	// ADD ME TO YOUR COUSIN LIST
	public void addMeToYourCousinList (Person addPerson) {
		cousinList.add (addPerson);              // add to cousinList
	}

	///////////////////////////////////////////////////////////////
	// ADD ME TO YOUR CO-RESIDENT LIST
	public void addMeToYourCoResidentList (Person addPerson) {
		coResidentList.add (addPerson);          // add to coResidentList
	}

	//////////////////////////////////////////////////////////////
	// ADD ME TO YOUR STEP-FAMILY LIST
	public void addMeToYourStepFamilyList (Person addPerson) {
		stepFamilyList.add (addPerson);          // add to stepFamilyList
	}

	///////////////////////////////////////////////////////////
	// CHECK FAMILY LIST
	public boolean checkFamilyList (Person checkPerson) {
		boolean found = false;
		int numFamily = familyList.size();                // size of familyList

		for (int i = 0; i < numFamily; ++i) {             // go through list
			if (found == false) {                         // if family member not found
				Person familyPerson = familyList.get(i);  // get person from list
				if (familyPerson == checkPerson) {        // if people match
					found = true;                         // set to true
					return found;                         // return result
				}
			}
		}
		return found;                                     // returns "false" if not found
	}	

	///////////////////////////////////////////////////////////
	// CHECK CO-RESIDENT LIST
	public boolean checkCoResidentList (Person checkPerson) {
		boolean found = false;
		int numCoResidents = coResidentList.size();            // size of coResidentList

		for (int i = 0; i < numCoResidents; ++i) {             // go through list
			Person coResidentPerson = coResidentList.get(i);   // get person from list
			if (coResidentPerson == checkPerson){              // if people match
				found = true;                                  // set to true
				return found;                                  // return true result
			}
		}
		return found;                                          // returns "false" if not found
	}

	///////////////////////////////////////////////////////////
	// CHECK SIBLING LIST
	public boolean checkSiblingList (Person checkPerson) {
		boolean found = false;
		int numSiblings = siblingList.size();                 // size of siblingList

		for (int i = 0; i < numSiblings; ++i) {               // go through list
			if (found == false) {
				Person siblingPerson = siblingList.get(i);    // get person from list
				if (siblingPerson == checkPerson){            // if people match
					found = true;                             // set to true
					return found;                             // return result
				}
			}
		} 
		return found;                                         // returns "false" if not found
	}

	//////////////////////////////////////////////////////////////
	// ADD FEMALE MATE TO FEMALE MATE LIST
	public void addFemaleMate (Person mate) {                 
		femaleMateList.add (mate);                            // add to male's list of current mates
	}

	////////////////////////////////////////////////////////////
	// REMOVE FEMALE MATE FROM FEMALE MATE LIST
	public void removeFemaleMate (Person mate) {
		femaleMateList.remove (mate);                         // remove from male's list of current mates
	}

	/////////////////////////////////////////////////////
	// GET FEMALE MATE FROM LIST
	// -returns a female mate from femaleMateList
	public Person getFemaleMate (int i) {
		Person mate = femaleMateList.get(i);                  // get ith person on list
		return mate;                                          // return the person
	}

	//////////////////////////////////////////////
	// GET NUMBER OF CURRENT FEMALE MATES
	public int getNumCurrentFemaleMates() {
		int numCurrentMates = 0;                            
		if (femaleMateList.size() != 0)                       // if mates on list
			for (Person female : femaleMateList) {            // go through list
				if (female.getLive() == true)                 // if mate is alive
					++numCurrentMates;                        // increment count
			}

		if (Model.getPairBondStability() == 0) {              // if pair bonds have no stability
			if (numCurrentMates > 1) {                        // but there is more than one mate
				System.err.printf ("Error:  pairBondStability = 0 but male has multiple mates\n"); //error
			}
		}

		return numCurrentMates;                               // return count
	}

	////////////////////////////////////////////////
	// ADD TO MALE MATE LIST
	public void addMaleMate (Person mate) {
		maleMateList.add (mate);                              // add to female's list of current mates
	}

	///////////////////////////////////////////
	// REMOVE FROM MALE MATE LIST
	public void removeMaleMate (Person mate) {               
		maleMateList.remove(mate);                            // remove from female's list of current mates
	}

	//////////////////////////////////////////////
	// GET MALE MATE FROM LIST
	public Person getMaleMate (int i) {
		Person currentMaleMate = maleMateList.get(i);        // get ith person on list
		return currentMaleMate;                              // return person
	}

	////////////////////////////////////////////////
	// GET CURRENT NUMBER OF MALE MATES
	public int getNumCurrentMaleMates() {
		int numCurrentMates = 0;
		if (maleMateList.size() != 0)                        // if list not empty
			for (Person male : maleMateList) {               // go through list
				if (male.getLive() == true)                  // if mate alive
					++numCurrentMates;                       // increment this
			}

		if (numCurrentMates > 1)  {                          // if more than 1
			System.err.printf ("ERROR: female has more than one mate\n");   //error
		}

		return numCurrentMates;                              // return count
	}

	//////////////////////////////////////////////
	// ADD CHILD TO CHILD LIST
	public void addChild (Person child) {                       
		childList.add (child);                               // add child to childList
	}

	//////////////////////////////////////////
	// CHECK:  CHILD LIST?
	// -check to see if person is on child list
	public boolean childListCheck (Person potentialC) {
		boolean c = false;

		if (childList.size() != 0) {                        // if there are children on list
			for (Person child : childList) {                // go through list
				if (child == potentialC) {                  // if persons match
					c = true;                               // set this to "true"
				}
			}
		}
		return c;                                           // return result
	}	

	/////////////////////////////////////////////		
	// GET CHILD
	public Person getChild (int i) {
		Person child = childList.get(i);                    // get the ith person on the list
		return child;                                       // return person
	}

	////////////////////////////////////////////			
	// GET SIBILING
	public Person getSibling (int i) {
		Person sib = siblingList.get(i);                    // get the ith person on the list
		return sib;                                         // send it back
	}

	//////////////////////////////////////////////
	// GET NUMBER OF DEPENDENTS
	public int getNumDependents() {
		int numDependents = 0;
		if (childList.size() != 0) {                                 // if chilren on list
			for (Person child : childList)                           // go through list
				if (child.getLive() == true)                         // if child is alive
					if (child.getAge() < Model.getAgeAtProduction()) // if child is not a producer
						if (child.getPreviouslyMarried() == false)   // if child is not previously married
							++numDependents;                         // increment count
		}
		return numDependents;                                        // return count
	}
}




