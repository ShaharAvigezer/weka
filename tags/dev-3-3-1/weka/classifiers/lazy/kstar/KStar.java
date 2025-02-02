/*
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

/**
 *    KS.java
 *    Copyright (c) 1995-97 by Len Trigg (trigg@cs.waikato.ac.nz).
 *    Java port to Weka by Abdelaziz Mahoui (am14@cs.waikato.ac.nz).
 *
 */


package weka.classifiers.lazy.kstar;

import weka.classifiers.lazy.IB1;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;

//import java.text.NumberFormat;

/**
 * K* is an instance-based classifier, that is the class of a test
 * instance is based upon the class of those training instances
 * similar to it, as determined by some similarity function.  The
 * underlying assumption of instance-based classifiers such as K*,
 * IB1, PEBLS, etc, is that similar instances will have similar
 * classes.
 *
 * For more information on K*, see <p>
 * 
 * John, G. Cleary and Leonard, E. Trigg (1995) "K*: An Instance-
 * based Learner Using an Entropic Distance Measure",
 * <i>Proceedings of the 12th International Conference on Machine
 * learning</i>, pp. 108-114.<p>
 *
 * @author Len Trigg (len@intelligenesis.net)
 * @author Abdelaziz Mahoui (am14@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 */

public class KStar extends DistributionClassifier
  implements KStarConstants, OptionHandler, UpdateableClassifier, WeightedInstancesHandler {

  /** The training instances used for classification. */
  protected Instances m_Train; 

  /** The number of instances in the dataset */
  protected int m_NumInstances;

  /** The number of class values */
  protected int m_NumClasses;

  /** The number of attributes */
  protected int m_NumAttributes;

  /** The class attribute type */
  protected int m_ClassType;

  /** Table of random class value colomns */
  protected int [][] m_RandClassCols;

  /** Flag turning on and off the computation of random class colomns */
  protected int m_ComputeRandomCols = ON;

  /** Flag turning on and off the initialisation of config variables */
  protected int m_InitFlag = ON;

  /**
   * A custom data structure for caching distinct attribute values
   * and their scale factor or stop parameter.
   */
  protected KStarCache [] m_Cache;

  /** missing value treatment */
  protected int m_MissingMode = M_AVERAGE;

  /** 0 = use specified blend, 1 = entropic blend setting */
  protected int m_BlendMethod = B_SPHERE;

  /** default sphere of influence blend setting */
  protected int m_GlobalBlend = 20;

  /** Define possible missing value handling methods */
  public static final Tag [] TAGS_MISSING = {
    new Tag(M_DELETE, "Ignore the instance with the missing value"),
    new Tag(M_MAXDIFF, "Treat missing values as maximally different"),
    new Tag(M_NORMAL, "Normilize over the attributes"),
    new Tag(M_AVERAGE, "Average column entropy curves")
      };

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {
    String debug = "(KStar.buildClassifier) ";

    if (instances.classIndex() < 0)
      throw new Exception ("No class attribute assigned to instances");
    if (instances.checkForStringAttributes())
      throw new Exception("Can't handle string attributes!");
    m_Train = new Instances(instances, 0, instances.numInstances());
    // Throw away training instances with missing class
    m_Train.deleteWithMissingClass();
    // initializes class attributes ** java-speaking! :-) **
    init_m_Attributes();
  }
  
  /**
   * Adds the supplied instance to the training set
   *
   * @param instance the instance to add
   * @exception Exception if instance could not be incorporated successfully
   */
  public void updateClassifier(Instance instance) throws Exception {
    String debug = "(KStar.updateClassifier) ";
    if (m_Train.equalHeaders(instance.dataset()) == false)
      throw new Exception("Incompatible instance types");
    if ( instance.classIsMissing() )
      return;
    m_Train.add(instance);
    // update relevant attributes ...
    update_m_Attributes();
  }

  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if an error occurred during the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception {

    String debug = "(KStar.distributionForInstance) ";
    double transProb = 0.0, temp = 0.0;
    double [] classProbability = new double[m_NumClasses];
    double [] predictedValue = new double[1];

    // initialization ...
    for (int i=0; i<classProbability.length; i++) {
      classProbability[i] = 0.0;
    }
    predictedValue[0] = 0.0;
    if (m_InitFlag == ON) {
	// need to compute them only once and will be used for all instances.
	// We are doing this because the evaluation module controls the calls. 
      if (m_BlendMethod == B_ENTROPY) {
	generateRandomClassColomns();
      }
      m_Cache = new KStarCache[m_NumAttributes];
      for (int i=0; i<m_NumAttributes;i++) {
	m_Cache[i] = new KStarCache();
      }
      m_InitFlag = OFF;
      //      System.out.println("Computing...");
    }
    // init done.
    Instance trainInstance;
    Enumeration enum = m_Train.enumerateInstances();
    while ( enum.hasMoreElements() ) {
      trainInstance = (Instance)enum.nextElement();
      transProb = instanceTransformationProbability(instance, trainInstance);      
      switch ( m_ClassType )
	{
	case Attribute.NOMINAL:
	  classProbability[(int)trainInstance.classValue()] += transProb;
	  break;
	case Attribute.NUMERIC:
	  predictedValue[0] += transProb * trainInstance.classValue();
	  temp += transProb;
	  break;
	}
    }
    if (m_ClassType == Attribute.NOMINAL) {
      double sum = Utils.sum(classProbability);
      if (sum <= 0.0)
	for (int i=0; i<classProbability.length; i++)
	  classProbability[i] = 1/m_NumClasses;
      else Utils.normalize(classProbability, sum);
      return classProbability;
    }
    else {
      predictedValue[0] = (temp != 0) ? predictedValue[0] / temp : 0.0;
      return predictedValue;
    }
  }


  /**
   * Calculate the probability of the first instance transforming into the 
   * second instance:
   * the probability is the product of the transformation probabilities of 
   * the attributes normilized over the number of instances used.
   * 
   * @param first the test instance
   * @param second the train instance
   * @return transformation probability value
   */
  private double instanceTransformationProbability(Instance first, 
						   Instance second) {
    String debug = "(KStar.instanceTransformationProbability) ";
    double transProb = 1.0;
    int numMissAttr = 0;
    for (int i = 0; i < m_NumAttributes; i++) {
      if (i == m_Train.classIndex()) {
	continue; // ignore class attribute
      }
      if (first.isMissing(i)) { // test instance attribute value is missing
	numMissAttr++;
	continue;
      }
      transProb *= attrTransProb(first, second, i);
      // normilize for missing values
      if (numMissAttr != m_NumAttributes) {
	transProb = Math.pow(transProb, (double)m_NumAttributes / 
			     (m_NumAttributes - numMissAttr));
      }
      else { // weird case!
	transProb = 0.0;
      }
    }
    // normilize for the train dataset
     return transProb / m_NumInstances;
  }


  /**
   * Calculates the transformation probability of the indexed test attribute 
   * to the indexed train attribute.
   *
   * @param first the test instance.
   * @param second the train instance.
   * @param col the index of the attribute in the instance.
   * @return the value of the transformation probability.
   */

  private double attrTransProb(Instance first, Instance second, int col) {
    String debug = "(KStar.attrTransProb)";
    double transProb = 0.0;
    KStarNominalAttribute ksNominalAttr;
    KStarNumericAttribute ksNumericAttr;
    switch ( m_Train.attribute(col).type() )
      {
      case Attribute.NOMINAL:
	ksNominalAttr = new KStarNominalAttribute(first, second, col, m_Train, 
						  m_RandClassCols, 
						  m_Cache[col]);
	ksNominalAttr.setOptions(m_MissingMode, m_BlendMethod, m_GlobalBlend);
	transProb = ksNominalAttr.transProb();
	ksNominalAttr = null;
	break;

      case Attribute.NUMERIC:
	ksNumericAttr = new KStarNumericAttribute(first, second, col, 
						  m_Train, m_RandClassCols, 
						  m_Cache[col]);
	ksNumericAttr.setOptions(m_MissingMode, m_BlendMethod, m_GlobalBlend);
	transProb = ksNumericAttr.transProb();
	ksNumericAttr = null;
	break;
      }
    return transProb;
  }

  /**
   * Gets the method to use for handling missing values. Will be one of
   * M_NORMAL, M_AVERAGE, M_MAXDIFF or M_DELETE.
   *
   * @return the method used for handling missing values.
   */
  public SelectedTag getMissingMode() {

    return new SelectedTag(m_MissingMode, TAGS_MISSING);
  }
  
  /**
   * Sets the method to use for handling missing values. Values other than
   * M_NORMAL, M_AVERAGE, M_MAXDIFF and M_DELETE will be ignored.
   *
   * @param newMode the method to use for handling missing values.
   */
  public void setMissingMode(SelectedTag newMode) {
    
    if (newMode.getTags() == TAGS_MISSING) {
      m_MissingMode = newMode.getSelectedTag().getID();
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector optVector = new Vector( 3 );
    optVector.addElement(new Option(
	      "\tManual blend setting (default 20%)\n",
	      "B", 1, "-B <num>"));
    optVector.addElement(new Option(
	      "\tEnable entropic auto-blend setting (symbolic class only)\n",
	      "E", 0, "-E"));
    optVector.addElement(new Option(
	      "\tSpecify the missing value treatment mode (default a)\n"
	      +"\tValid options are: a(verage), d(elete), m(axdiff), n(ormal)\n",
	      "M", 1,"-M <char>"));
    return optVector.elements();
  }

  /**
   * Set the global blend parameter
   * @param b the value for global blending
   */
  public void setGlobalBlend(int b) {
     m_GlobalBlend = b;
      if ( m_GlobalBlend > 100 ) {
	m_GlobalBlend = 100;
      }
      if ( m_GlobalBlend < 0 ) {
	m_GlobalBlend = 0;
      }
  }

  /**
   * Get the value of the global blend parameter
   * @return the value of the global blend parameter
   */
  public int getGlobalBlend() {
    return m_GlobalBlend;
  }

  /**
   * Set whether entropic blending is to be used.
   * @param e true if entropic blending is to be used
   */
  public void setEntropicAutoBlend(boolean e) {
    if (e) {
      m_BlendMethod = B_ENTROPY;
    } else {
      m_BlendMethod = B_SPHERE;
    }
  }

  /**
   * Get whether entropic blending being used
   * @return true if entropic blending is used
   */
  public boolean getEntropicAutoBlend() {
    if (m_BlendMethod == B_ENTROPY) {
      return true;
    }

    return false;
  }

  /**
   * Parses a given list of options. Valid options are:
   * ...
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String debug = "(KStar.setOptions)";
    String blendStr = Utils.getOption('B', options);
    if (blendStr.length() != 0) {
      setGlobalBlend(Integer.parseInt(blendStr));
    }

    setEntropicAutoBlend(Utils.getFlag('E', options));
    
    String missingModeStr = Utils.getOption('M', options);
    if (missingModeStr.length() != 0) {
      switch ( missingModeStr.charAt(0) ) {
      case 'a':
	setMissingMode(new SelectedTag(M_AVERAGE, TAGS_MISSING));
	break;
      case 'd':
	setMissingMode(new SelectedTag(M_DELETE, TAGS_MISSING));
	break;
      case 'm':
	setMissingMode(new SelectedTag(M_MAXDIFF, TAGS_MISSING));
	break;
      case 'n':
	setMissingMode(new SelectedTag(M_NORMAL, TAGS_MISSING));
	break;
      default:
	setMissingMode(new SelectedTag(M_AVERAGE, TAGS_MISSING));
      }
    }
    Utils.checkForRemainingOptions(options);
  }


  /**
   * Gets the current settings of K*.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {
    // -B <num> -E -M <char>
    String [] options = new String [ 5 ];
    int itr = 0;
    options[itr++] = "-B";
    options[itr++] = "" + m_GlobalBlend;

    if (getEntropicAutoBlend()) {
      options[itr++] = "-E";
    }

    options[itr++] = "-M";
    if (m_MissingMode == M_AVERAGE) {
      options[itr++] = "" + "a";
    }
    else if (m_MissingMode == M_DELETE) {
      options[itr++] = "" + "d";
    }
    else if (m_MissingMode == M_MAXDIFF) {
      options[itr++] = "" + "m";
    }
    else if (m_MissingMode == M_NORMAL) {
      options[itr++] = "" + "n";
    }
    while (itr < options.length) {
      options[itr++] = "";
    }
    return options;
  }

  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {
    StringBuffer st = new StringBuffer();
    st.append("KStar Beta Verion (0.1b).\n"
	      +"Copyright (c) 1995-97 by Len Trigg (trigg@cs.waikato.ac.nz).\n"
	      +"Java port to Weka by Abdelaziz Mahoui "
	      +"(am14@cs.waikato.ac.nz).\n\nKStar options : ");
    String [] ops = getOptions();
    for (int i=0;i<ops.length;i++) {
      st.append(ops[i]+' ');
    }
    return st.toString();
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv should contain command line options (see setOptions)
   */
  public static void main(String [] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new KStar(), argv));
    } catch (Exception e) {
      // System.err.println(e.getMessage());
      e.printStackTrace();
    }
  }


  /**
   * Initializes the m_Attributes of the class.
   */
  private void init_m_Attributes() {
    try {
      m_NumInstances = m_Train.numInstances();
      m_NumClasses = m_Train.numClasses();
      m_NumAttributes = m_Train.numAttributes();
      m_ClassType = m_Train.classAttribute().type();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Updates the m_attributes of the class.
   */
  private void update_m_Attributes() {
    m_NumInstances = m_Train.numInstances();
    m_InitFlag = ON;
  }

  /**
   * Note: for Nominal Class Only!
   * Generates a set of random versions of the class colomn.
   */
  private void generateRandomClassColomns() {
    String debug = "(KStar.generateRandomClassColomns)";
    Random generator = new Random(42);
    //    Random generator = new Random();
    m_RandClassCols = new int [NUM_RAND_COLS+1][];
    int [] classvals = classValues();
    for (int i=0; i < NUM_RAND_COLS; i++) {
      // generate a randomized version of the class colomn
      m_RandClassCols[i] = randomize(classvals, generator);
    }
    // original colomn is preserved in colomn NUM_RAND_COLS
    m_RandClassCols[NUM_RAND_COLS] = classvals;
  }

  /**
   * Note: for Nominal Class Only!
   * Returns an array of the class values
   *
   * @return an array of class values
   */
  private int [] classValues() {
    String debug = "(KStar.classValues)";
    int [] classval = new int[m_NumInstances];
    for (int i=0; i < m_NumInstances; i++) {
      try {
	classval[i] = (int)m_Train.instance(i).classValue();
      } catch (Exception ex) {
	ex.printStackTrace();
      }
    }
    return classval;
  }

  /**
   * Returns a copy of the array with its elements randomly redistributed.
   *
   * @param array the array to randomize.
   * @return a copy of the array with its elements randomly redistributed.
   */
  private int [] randomize(int [] array, Random generator) {
    String debug = "(KStar.randomize)";
    int index;
    int temp;
    int [] newArray = new int[array.length];
    System.arraycopy(array, 0, newArray, 0, array.length);
    for (int j = newArray.length - 1; j > 0; j--) {
      index = (int) ( generator.nextDouble() * (double)j );
      temp = newArray[j];
      newArray[j] = newArray[index];
      newArray[index] = temp;
    }
    return newArray;
  }

} // class end

