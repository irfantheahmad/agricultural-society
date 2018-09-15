//ALL SPATIAL REFERENCES REMOVED FROM HOUSEHOLD FOR NOW

package ForagerNet3_Demography_V3;

import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.util.ContextUtils;

public class Household {

	public  static int          	        nextId = 1;       // to give each an id
		
	// instance variables  
	public int 	   		id;			        // unique ID number for each household
	public Person       adultMale;          // adult male at origination of household
	public int          birthWeek;          // tick that household was "born" in
	public int          year;               // year of household's existence (first year is "year 1")
	public int          size;               // number of members in household
	public int          x, y;               // coordinates (not relevant to FN3_D)
	public double       cPRatio;            // ratio of consumers to producers in household
	public double       currentSurplus;     // surplus for the current year (not relevant to FN3_D)
	public double       householdAssets;    // current 'bank account' (not relevant to FN3_D)
	public int          lifespan;           // total length (in years) of household's existence
	public int          peakSize;           // stores the largest size of household
	public double       peakCPRatio;        // stores the highest CP ratio of household
	public int          peakFemaleMates;    // stores the highest number of concurrent female mates
	public int          peakProducers;      // stores the highest number of producers in household
	public int          peakDependents;     // stores the highest number of consumers in household
	public double       lifespanSurplus;    // surplus over the course of the household's existence
	public int          surplusYears;       // number of years of surplus production
	public int          deficitYears;       // number of years of deficit production

	public ArrayList <Person> memberList =  new ArrayList<Person>();   // list of people in each household
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// HOUSEHOLD CONSTRUCTOR
	//////////////////////////////////////////////////////////////////////////////////////////
	public Household (Person maleMate, Person femaleMate) {
		id = nextId++;
		
		if (maleMate != null) {             // if the household is formed through pair bonding
			adultMale = maleMate;          // adult male of household
			memberList.add(maleMate);      // add male to household memberList
		}

		if (maleMate == null) {            // if the household is formed through dissolution of a pair bond
		}

		memberList.add(femaleMate);        // add female to household memberList
	
		birthWeek = Model.getSeasonTicks();     // get time of "birth" of household
		year = 0;
		peakSize = 0;
		peakCPRatio = 0;
		currentSurplus = 0;
		lifespanSurplus = 0;
		peakDependents = 0;
		peakFemaleMates = 0;
		peakProducers = 0;
		deficitYears = 0;
		surplusYears = 0;
		
		Context <Object> houseContext;
		houseContext = ContextUtils.getContext(femaleMate); //get femaleMate's context
		houseContext.add(this);
	}

	///////////////////////////////////////////////////////////////////////////////////////
	// SETTERS AND GETTERS
	////////////////////////////////////////////////////////////////////////////////////////
	public int getId() {  return id; }
	
	public int getBirthWeek() { return birthWeek; }
	public int getYear() { return year; }

	public double getHouseholdAssets() { return householdAssets; }
	public void setHouseholdAssets (double assets) { householdAssets = assets; }

	public int getLifespan() { return lifespan; }
	public double getPeakCPRatio() {return peakCPRatio; }
	public int getSize() {return memberList.size();}
	public int getPeakSize() {return peakSize; }
	public int getPeakDependents() {return peakDependents; }
	public int getPeakFemaleMates() { return peakFemaleMates; }
	public int getPeakProducers() {return peakProducers; }
	public double getLifespanSurplus() {return lifespanSurplus; }
	public int getSurplusYears() {return surplusYears; }
	public int getDeficitYears() {return deficitYears; }
	public Person getAdultMale() { return adultMale; }
	public double getCurrentSurplus() { return currentSurplus; }

   	public static void resetNextId() { nextId = 1; }  // call when we reset the model

	/////////////////////////////////////////////////////////////////////////////////////////
	// 1. CALCULATE THE SURPLUS PRODUCED OVER THE YEAR
	// -called from model at season tick 1 to update household stats
	// -calculates current ratio of consumers : producers
	// -updates peak size, peak CP ratio, peak dependents, peak wives
	// -calculates current year surplus (or deficit) and adds to lifespanSurplus
	////////////////////////////////////////////////////////////////////////////////////////
	public void calculateSurplusForYear () {

		double currentCPRatio = getCPRatio();                 // calculate CP ratio
		int currentSize = memberList.size();                  // current size of household
		int currentNumProducers = getNumProducers();          // current number of producers
		int currentNumNonProducers = getNumNonProducers();    // current number of producers
		
		if (currentCPRatio > peakCPRatio)                     // if the curren CP ratio is highest yet
			peakCPRatio = currentCPRatio;                     // update peak CP

		if (currentSize > peakSize)                           // if current size is highest yet
			peakSize = currentSize;                           // update peak size
		
		if (currentNumProducers > peakProducers)              // if current n producers is highest yet
			peakProducers = currentNumProducers;              // update peak producers

		if (currentNumNonProducers > peakDependents)          // if current n non-producers is highest yet
			peakDependents = currentNumNonProducers;          // update peak dependents

		if (adultMale != null) {                             // if the head male exists
			if (adultMale.getNumCurrentFemaleMates() > peakFemaleMates)    // update peak number of mates
				peakFemaleMates = adultMale.getNumCurrentFemaleMates();
		}

		currentSurplus = (currentNumProducers * 1.75) - currentSize; // current productive potential 

		householdAssets = householdAssets + currentSurplus;          // add to householdAssets

		lifespanSurplus = lifespanSurplus + currentSurplus;          //add to lifespan total

		
		if (currentSurplus < 0)           // if household is in deficit territory
			++deficitYears;               // add to deficit year count
		else                              // if in surplus territory
			++surplusYears;               // add to surplus year count
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// INCREMENT HOUSEHOLD YEAR
	public void incrementHouseholdYear() {
		++year;                                //add 1 to year
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// ADD PERSON TO HOUSEHOLD
	public void addSelf (Person x) {
		memberList.add (x);                   // add person to memberList
		x.setCurrentHousehold (this);         // set person's currentHousehold to this household
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// REMOVE PERSON FROM HOUSEHOLD
	public void removeSelf (Person x) {
		memberList.remove (x);                // remove person from memberList
		x.setCurrentHousehold (null);         // set currentHousehold to null
		
		if (x == adultMale) {                 // if this person is adultMale of household
			adultMale = null;                 // make that variable null
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	// GET PARTICULAR RESIDENT OF HOUSEHOLD
	public Person getResident (int x) {
		Person resident = memberList.get(x);  // get xth person on list
		return resident;
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// GET NUMBER OF PRODUCERS IN HOUSEHOLD
	public int getNumProducers() {

		int numProducers = 0;

		for (Person member : memberList) {                         // go through memberList
			if (member.getAge() >= Model.getAgeAtProduction()) {   // if member is producer
				++numProducers;                                    // increment n producers
			}
		}
		return numProducers;                                       // return result
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// GET NUMBER OF CHILD PRODUCERS
	public int getNumChildProducers() {

		int ageMaturity = Model.getAgeAtMaturity();               // get ageAtMaturity
		int numChildProducers = 0;

		for (Person member : memberList) {                        // go through memberList
			int memberAge = member.getAge();      
			if (memberAge >= Model.getAgeAtProduction()) {        // if member is producer
				if (memberAge < ageMaturity) {                    // but under adult age
					++numChildProducers;                          // increment count
				}
			}
		}
		return numChildProducers;                                // return result
	}

	///////////////////////////////////////////////////////////////////////////////////////
	//  GET NUMBER OF NON-PRODUCERS
	public int getNumNonProducers() {

		int numNonProducers = 0;

		for (Person member : memberList) {                       // go through memberList
			if (member.getAge() < Model.getAgeAtProduction()) {  // if person is under production age
				++numNonProducers;                               // increment count
			}
		}
		return numNonProducers;                                  // return result
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// GET CP RATIO
	public double getCPRatio() {

		int numConsumers = memberList.size();                   // number of people in household
		int numProducers = getNumProducers();                   // number of producers in household
		
		double cPRatio = (double) numConsumers / (double) numProducers;    //calculate ratio
		
		return cPRatio;                                         // return result
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// CALCULATE CONDITIONAL CP RATIO
	// -calculate what CP ratio will be if another child is added
	public double getConditionalCPRatio() {

		int condNumConsumers = memberList.size() + 1;            // n people that WILL BE in household
		int numProducers = getNumProducers();                    // n producers that WILL BE in household

		double condHouseholdCP = (double) condNumConsumers / (double) numProducers;    //calculate ratio

		return condHouseholdCP;                                  // return result
	}

	//////////////////////////////////////////////////////////////////////////////////////////
	// PRINT MEMBERS OF HOUSEHOLD
	public void printMembers() {
		for (Person member : memberList) {
			if (member.getSex() == 0)
				System.out.printf ("   %d: Male (%d), age %d, %d wives, %d children\n", 
								   id, member.getId(), member.getAge(), member.getNumCurrentFemaleMates(), 
								   member.getNumChildren());
			if (member.getSex() == 1)
				System.out.printf ("   %d: Female (%d), age %d, %d children\n", 
								   id, member.getId(), member.getAge(), member.getNumChildren()); 
		}
	}
}
