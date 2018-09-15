package ForagerNet3_Demography_V3;

import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.util.ContextUtils;



public class Link {
	// "class" variables -- one value for all instances 
	
	private ContinuousSpace <Object > space ;
	private Grid <Object > grid ;
	
	public Link ( ContinuousSpace <Object > space , Grid <Object > grid ) {
		this.space = space ;
		this.grid = grid ;
	}

	public Context linkContext = null;
	

    public  static int          	        nextId = 0;       // to give each an id
	public int 	   		linkID;			        // unique id number for each link
	public int          linkType;               // type of link: 
	                                            // 1 = family (descent)
	                                            // 2 = co-resident (proximity)
	                                            // 3 = pair bond 
                                                // 4 = kin 
	                                            // 5 = acquaintance
	                                            // 6 = ex-mate
	public int          linkSubType;            // FAMILY:
                                                //  1 = parent/grandparent --> child
	                                            //  2 = child --> parent/grandparent
	                                            //  3 = sibling --> sibling
	                                            // KIN:
	                                            //  1 = father/mother-in-law -->husband/wife 
                                                //  2 = husband/wife --> father/mother-in-law
                                                //  3 = aunt/uncle --> child
                                                //  4 = child --> aunt/uncle
                                                //  5 = cousin --> cousin    
	                                            //  6 = husband/wife --> sib-in-law
                                                //  7 = husband/wife --> niece/nephew-in-law
                                                //  8 = niece/nephew-in-law --> husband/wife
	                                            //  9 = step-parent --> stepChild
	                                            // 10 = stepChild --> step-Parent
	                                            	                                           
	public Person       fromPerson;             // "from" person
	public Person       toPerson;               // "to" person
	public int x, y;

	// a Link constructor
	// note it assigns ID values in sequence as links are created.
	public Link ( ) {
		linkID = nextId++;
	}

	public int getLinkID() {  return linkID; }

	public int getLinkType() { return linkType; }
	public void setLinkType( int lt ) { linkType = lt; }

	public int getLinkSubType() { return linkSubType; }
	public void setLinkSubType( int lst ) { linkSubType = lst; }

	public Person getFromPerson() { return fromPerson; }
	public void setFromPerson( Person fp ) { 
		if (fp == null) {
			System.err.printf ("ERROR in setFromPerson: trying to create link with null fromPerson\n");
		}
		fromPerson = fp; 
	}

	public Person getToPerson() { return toPerson; }
	public void setToPerson( Person tp ) { 
		if (tp == null) {
			System.err.printf ("ERROR in setToPerson: trying to create link with null toPerson\n");
		}
		toPerson = tp; 
	}

	public int getX() { return x; }
	public int getY() { return y; }
		
	// note these are class (static) methods, to set class (static) variables
	public static void resetNextId() { nextId = 0; }                // call when we reset the model

}
