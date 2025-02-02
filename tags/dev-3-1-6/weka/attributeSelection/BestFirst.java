/*
 *    BestFirst.java
 *    Copyright (C) 1999 Mark Hall
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;

/** 
 * Class for performing a best first search. <p>
 *
 * Valid options are: <p>
 *
 * -P <start set> <br>
 * Specify a starting set of attributes. Eg 1,4,7-9. <p>
 *
 * -D <-1 = backward | 0 = bidirectional | 1 = forward> <br>
 * Direction of the search. (default = 1). <p>
 *
 * -N <num> <br>
 * Number of non improving nodes to consider before terminating search.
 * (default = 5). <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $
 */
public class BestFirst extends ASSearch 
  implements OptionHandler, StartSetHandler
{

  // Inner classes
  /**
   * Class for a node in a linked list. Used in best first search.
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   **/
  public class Link2 {

    BitSet group;
    double merit;


    // Constructor
    public Link2 (BitSet gr, double mer) {
      group = (BitSet)gr.clone();
      merit = mer;
    }


    /** Get a group */
    public BitSet getGroup () {
      return  group;
    }


    public String toString () {
      return  ("Node: " + group.toString() + "  " + merit);
    }

  }


  /**
   * Class for handling a linked list. Used in best first search.
   * Extends the Vector class.
   * @author Mark Hall (mhall@cs.waikato.ac.nz)
   **/
  public class LinkedList2
    extends FastVector
  {
    // Max number of elements in the list
    int m_MaxSize;

    // ================
    // Public methods
    // ================
    public LinkedList2 (int sz) {
      super();
      m_MaxSize = sz;
    }


    /**
     * removes an element (Link) at a specific index from the list.
     * @param index the index of the element to be removed.
     **/
    public void removeLinkAt (int index)
      throws Exception
    {
      if ((index >= 0) && (index < size())) {
        removeElementAt(index);
      }
      else {
        throw  new Exception("index out of range (removeLinkAt)");
      }
    }


    /**
     * returns the element (Link) at a specific index from the list.
     * @param index the index of the element to be returned.
     **/
    public Link2 getLinkAt (int index)
      throws Exception
    {
      if (size() == 0) {
        throw  new Exception("List is empty (getLinkAt)");
      }
      else {if ((index >= 0) && (index < size())) {
        return  ((Link2)(elementAt(index)));
      }
      else {
        throw  new Exception("index out of range (getLinkAt)");
      }
      }
    }


    /**
     * adds an element (Link) to the list.
     * @param gr the attribute set specification
     * @param mer the "merit" of this attribute set
     **/
    public void addToList (BitSet gr, double mer)
      throws Exception
    {
      Link2 newL = new Link2(gr, mer);

      if (size() == 0) {
	addElement(newL);
      }
      else {if (mer > ((Link2)(firstElement())).merit) {
	if (size() == m_MaxSize) {
	  removeLinkAt(m_MaxSize - 1);
	}

	//----------
	insertElementAt(newL, 0);
      }
      else {
	int i = 0;
	int size = size();
	boolean done = false;

	//------------
	// don't insert if list contains max elements an this
	// is worst than the last
	if ((size == m_MaxSize) && (mer <= ((Link2)(lastElement())).merit)) {

	}
	//---------------
	else {
	  while ((!done) && (i < size)) {
	    if (mer > ((Link2)(elementAt(i))).merit) {
	      if (size == m_MaxSize) {
		removeLinkAt(m_MaxSize - 1);
	      }

	      // ---------------------
	      insertElementAt(newL, i);
	      done = true;
	    }
	    else {if (i == size - 1) {
	      addElement(newL);
	      done = true;
	    }
	    else {
	      i++;
	    }
	    }
	  }
	}
      }
      }
    }

  }

  // member variables
  /** maximum number of stale nodes before terminating search */
  private int m_maxStale;

  /** 0 == backward search, 1 == forward search, 2 == bidirectional */
  private int m_searchDirection;

  /** search directions */
  private static final int SELECTION_BACKWARD = 0;
  private static final int SELECTION_FORWARD = 1;
  private static final int SELECTION_BIDIRECTIONAL = 2;
  public static final Tag [] TAGS_SELECTION = {
    new Tag(SELECTION_BACKWARD, "Backward"),
    new Tag(SELECTION_FORWARD, "Forward"),
    new Tag(SELECTION_BIDIRECTIONAL, "Bi-directional"),
  };

  /** holds an array of starting attributes */
  private int[] m_starting;

  /** holds the start set for the search as a Range */
  private Range m_startRange;

  /** does the data have a class */
  private boolean m_hasClass;

  /** holds the class index */
  private int m_classIndex;

  /** number of attributes in the data */
  private int m_numAttribs;

  /** total number of subsets evaluated during a search */
  private int m_totalEvals;

  /** for debugging */
  private boolean m_debug;

  /** holds the merit of the best subset found */
  private double m_bestMerit;

  /** 
   *Constructor 
   */
  public BestFirst () {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   *
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(3);
    
    newVector.addElement(new Option("\tSpecify a starting set of attributes." 
				    + "\n\tEg. 1,3,5-7."
				    ,"P",1
				    , "-P <start set>"));
    newVector.addElement(new Option("\tDirection of search. (default = 1)."
				    , "D", 1
				    , "-D <0 = backward | 1 = forward " 
				    + "| 2 = bi-directional>"));
    newVector.addElement(new Option("\tNumber of non-improving nodes to" 
				    + "\n\tconsider before terminating search."
				    , "N", 1, "-N <num>"));
    return  newVector.elements();
  }


  /**
   * Parses a given list of options.
   *
   * Valid options are: <p>
   *
   * -P <start set> <br>
   * Specify a starting set of attributes. Eg 1,4,7-9. <p>
   *
   * -D <-1 = backward | 0 = bidirectional | 1 = forward> <br>
   * Direction of the search. (default = 1). <p>
   *
   * -N <num> <br>
   * Number of non improving nodes to consider before terminating search.
   * (default = 5). <p>
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception
  {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setStartSet(optionString);
    }

    optionString = Utils.getOption('D', options);

    if (optionString.length() != 0) {
      setDirection(new SelectedTag(Integer.parseInt(optionString),
				   TAGS_SELECTION));
    } else {
      setDirection(new SelectedTag(SELECTION_FORWARD, TAGS_SELECTION));
    }

    optionString = Utils.getOption('N', options);

    if (optionString.length() != 0) {
      setSearchTermination(Integer.parseInt(optionString));
    }

    m_debug = Utils.getFlag('Z', options);
  }

  /**
   * Sets a starting set of attributes for the search. It is the
   * search method's responsibility to report this start set (if any)
   * in its toString() method.
   * @param startSet a string containing a list of attributes (and or ranges),
   * eg. 1,2,6,10-15.
   * @exception if start set can't be set.
   */
  public void setStartSet (String startSet) throws Exception {
    m_startRange.setRanges(startSet);
  }

  /**
   * Returns a list of attributes (and or attribute ranges) as a String
   * @return a list of attributes (and or attribute ranges)
   */
  public String getStartSet () {
    return m_startRange.getRanges();
  }

  /**
   * Set the numnber of non-improving nodes to consider before terminating
   * search.
   *
   * @param t the number of non-improving nodes
   * @exception Exception if t is less than 1
   */
  public void setSearchTermination (int t)
    throws Exception
  {
    if (t < 1) {
      throw  new Exception("Value of -N must be > 0.");
    }

    m_maxStale = t;
  }


  /**
   * Get the termination criterion (number of non-improving nodes).
   *
   * @return the number of non-improving nodes
   */
  public int getSearchTermination () {
    return  m_maxStale;
  }


  /**
   * Set the search direction
   *
   * @param d the direction of the search
   */
  public void setDirection (SelectedTag d) {
    
    if (d.getTags() == TAGS_SELECTION) {
      m_searchDirection = d.getSelectedTag().getID();
    }
  }


  /**
   * Get the search direction
   *
   * @return the direction of the search
   */
  public SelectedTag getDirection () {
    try {
      return new SelectedTag(m_searchDirection, TAGS_SELECTION);
    } catch (Exception ex) {
      return null;
    }
  }


  /**
   * Gets the current settings of BestFirst.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[6];
    int current = 0;

    if (!(getStartSet().equals(""))) {
      options[current++] = "-P";
      options[current++] = ""+startSetToString();
    }
    options[current++] = "-D";
    options[current++] = "" + m_searchDirection;
    options[current++] = "-N";
    options[current++] = "" + m_maxStale;

    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }

  /**
   * converts the array of starting attributes to a string. This is
   * used by getOptions to return the actual attributes specified
   * as the starting set. This is better than using m_startRanges.getRanges()
   * as the same start set can be specified in different ways from the
   * command line---eg 1,2,3 == 1-3. This is to ensure that stuff that
   * is stored in a database is comparable.
   * @return a comma seperated list of individual attribute numbers as a String
   */
  private String startSetToString() {
    StringBuffer FString = new StringBuffer();
    boolean didPrint;

    if (m_starting == null) {
      return getStartSet();
    }
    for (int i = 0; i < m_starting.length; i++) {
      didPrint = false;
      
      if ((m_hasClass == false) || 
	  (m_hasClass == true && i != m_classIndex)) {
	FString.append((m_starting[i] + 1));
	didPrint = true;
      }
      
      if (i == (m_starting.length - 1)) {
	FString.append("");
      }
      else {
	if (didPrint) {
	  FString.append(",");
	  }
      }
    }

    return FString.toString();
  }

  /**
   * returns a description of the search as a String
   * @return a description of the search
   */
  public String toString () {
    StringBuffer BfString = new StringBuffer();
    BfString.append("\tBest first.\n\tStart set: ");

    if (m_starting == null) {
      BfString.append("no attributes\n");
    }
    else {
      BfString.append(startSetToString()+"\n");
    }

    BfString.append("\tSearch direction: ");

    if (m_searchDirection == SELECTION_BACKWARD) {
      BfString.append("backward\n");
    }
    else {if (m_searchDirection == SELECTION_FORWARD) {
      BfString.append("forward\n");
    }
    else {
      BfString.append("bi-directional\n");
    }
    }

    BfString.append("\tStale search after " 
		    + m_maxStale + " node expansions\n");
    BfString.append("\tTotal number of subsets evaluated: " 
		    + m_totalEvals + "\n");
    BfString.append("\tMerit of best subset found: "
		    +Utils.doubleToString(Math.abs(m_bestMerit),8,3)+"\n");
    return  BfString.toString();
  }


  private void printGroup (BitSet tt, int numAttribs) {
    int i;

    for (i = 0; i < numAttribs; i++) {
      if (tt.get(i) == true) {
	System.out.print((i + 1) + " ");
      }
    }

    System.out.println();
  }


  /**
   * Searches the attribute subset space by best first search
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public int[] search (ASEvaluation ASEval, Instances data)
    throws Exception
  {
    if (!(ASEval instanceof SubsetEvaluator)) {
      throw  new Exception(ASEval.getClass().getName() 
			   + " is not a " 
			   + "Subset evaluator!");
    }

    if (ASEval instanceof UnsupervisedSubsetEvaluator) {
      m_hasClass = false;
    }
    else {
      m_hasClass = true;
      m_classIndex = data.classIndex();
    }

    SubsetEvaluator ASEvaluator = (SubsetEvaluator)ASEval;
    m_numAttribs = data.numAttributes();
    int i, j;
    int best_size = 0;
    int size = 0;
    int done;
    int sd = m_searchDirection;
    int evals = 0;
    BitSet best_group, temp_group;
    int stale;
    double best_merit;
    boolean ok = true;
    double merit;
    boolean z;
    boolean added;
    Link2 tl;
    Hashtable lookup = new Hashtable((int)(200.0*m_numAttribs*1.5));
    LinkedList2 bfList = new LinkedList2(m_maxStale);
    best_merit = -Double.MAX_VALUE;
    stale = 0;
    best_group = new BitSet(m_numAttribs);

    m_startRange.setUpper(m_numAttribs-1);
    if (!(getStartSet().equals(""))) {
      m_starting = m_startRange.getSelection();
    }
    // If a starting subset has been supplied, then initialise the bitset
    if (m_starting != null) {
      for (i = 0; i < m_starting.length; i++) {
	if ((m_starting[i]) != m_classIndex) {
	  best_group.set(m_starting[i]);
	}
      }

      best_size = m_starting.length;
      m_totalEvals++;
    }
    else {
      if (m_searchDirection == SELECTION_BACKWARD) {
	setStartSet("1-last");
	m_starting = new int[m_numAttribs];

	// init initial subset to all attributes
	for (i = 0, j = 0; i < m_numAttribs; i++) {
	  if (i != m_classIndex) {
	    best_group.set(i);
	    m_starting[j++] = i;
	  }
	}

	best_size = m_numAttribs - 1;
	m_totalEvals++;
      }
    }

    // evaluate the initial subset
    best_merit = ASEvaluator.evaluateSubset(best_group);
    // add the initial group to the list and the hash table
    bfList.addToList(best_group, best_merit);
    BitSet tt = (BitSet)best_group.clone();
    lookup.put(tt, "");

    while (stale < m_maxStale) {
      added = false;

      if (m_searchDirection == SELECTION_BIDIRECTIONAL) {
	// bi-directional search
	  done = 2;
	  sd = SELECTION_FORWARD;
	}
      else {
	done = 1;
      }

      // finished search?
      if (bfList.size() == 0) {
	stale = m_maxStale;
	break;
      }

      // copy the attribute set at the head of the list
      tl = bfList.getLinkAt(0);
      temp_group = (BitSet)(tl.getGroup().clone());
      // remove the head of the list
      bfList.removeLinkAt(0);
      // count the number of bits set (attributes)
      int kk;

      for (kk = 0, size = 0; kk < m_numAttribs; kk++) {
	if (temp_group.get(kk)) {
	  size++;
	}
      }

      do {
	for (i = 0; i < m_numAttribs; i++) {
	  if (sd == SELECTION_FORWARD) {
	    z = ((i != m_classIndex) && (!temp_group.get(i)));
	  }
	  else {
	    z = ((i != m_classIndex) && (temp_group.get(i)));
	  }

	  if (z) {
	    // set the bit (attribute to add/delete)
	    if (sd == SELECTION_FORWARD) {
	      temp_group.set(i);
	    }
	    else {
	      temp_group.clear(i);
	    }

	    /* if this subset has been seen before, then it is already 
	       in the list (or has been fully expanded) */
	    tt = (BitSet)temp_group.clone();

	    if (lookup.containsKey(tt) == false) {
	      merit = ASEvaluator.evaluateSubset(temp_group);
	      m_totalEvals++;

	      if (m_debug) {
		System.out.print("Group: ");
		printGroup(tt, m_numAttribs);
		System.out.println("Merit: " + merit);
	      }

	      // is this better than the best?
	      if (sd == SELECTION_FORWARD) {
		z = ((merit - best_merit) > 0.00001);
	      }
	      else {
		z = ((merit >= best_merit) && ((size) < best_size));
	      }

	      if (z) {
		added = true;
		stale = 0;
		best_merit = merit;
		best_size = (size + best_size);
		best_group = (BitSet)(temp_group.clone());
	      }

	      // insert this one in the list and in the hash table
	      bfList.addToList(tt, merit);
	      lookup.put(tt, "");
	    }

	    // unset this addition(deletion)
	    if (sd == SELECTION_FORWARD) {
	      temp_group.clear(i);
	    }
	    else {
	      temp_group.set(i);
	    }
	  }
	}

	if (done == 2) {
	  sd = SELECTION_BACKWARD;
	}

	done--;
      } while (done > 0);

      /* if we haven't added a new attribute subset then full expansion 
	 of this node hasen't resulted in anything better */
      if (!added) {
	stale++;
      }
    }

    m_bestMerit = best_merit;
    return  attributeList(best_group);
  }


  /**
   * Reset options to default values
   */
  protected void resetOptions () {
    m_maxStale = 5;
    m_searchDirection = SELECTION_FORWARD;
    m_starting = null;
    m_startRange = new Range();
    m_classIndex = -1;
    m_totalEvals = 0;
    m_debug = false;
  }


  /**
   * converts a BitSet into a list of attribute indexes 
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   **/
  private int[] attributeList (BitSet group) {
    int count = 0;

    // count how many were selected
    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	count++;
      }
    }

    int[] list = new int[count];
    count = 0;

    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	list[count++] = i;
      }
    }

    return  list;
  }

}

