/*
 *    ReliefFAttributeEval.java
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

package weka.attributeSelection;
import java.io.*;
import java.util.*;
import weka.core.*;

/** 
 * Class for Evaluating attributes individually using ReliefF. <p>
 *
 * For more information see: <p>
 *
 * Kira, K. and Rendell, L. A. (1992). A practical approach to feature
 * selection. In D. Sleeman and P. Edwards, editors, <i>Proceedings of
 * the International Conference on Machine Learning,</i> pages 249-256.
 * Morgan Kaufmann. <p>
 *
 * Kononenko, I. (1994). Estimating attributes: analysis and extensions of
 * Relief. In De Raedt, L. and Bergadano, F., editors, <i> Machine Learning:
 * ECML-94, </i> pages 171-182. Springer Verlag. <p>
 *
 * Marko Robnik Sikonja, Igor Kononenko: An adaptation of Relief for attribute
 * estimation on regression. In D.Fisher (ed.): <i> Machine Learning, 
 * Proceedings of 14th International Conference on Machine Learning ICML'97, 
 * </i> Nashville, TN, 1997. 
 *
 *
 * Valid options are:
 *
 * -M <number of instances> <br>
 * Specify the number of instances to sample when estimating attributes. <br>
 * If not specified then all instances will be used. <p>
 *
 * -D <seed> <br>
 * Seed for randomly sampling instances. <p>
 *
 * -K <number of neighbours> <br>
 * Number of nearest neighbours to use for estimating attributes. <br>
 * (Default is 10). <p>
 *
 * -W <br>
 * Weight nearest neighbours by distance. <p>
 *
 * -A <sigma> <br>
 * Specify sigma value (used in an exp function to control how quickly <br>
 * weights decrease for more distant instances). Use in conjunction with <br>
 * -W. Sensible values = 1/5 to 1/10 the number of nearest neighbours. <br>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class ReliefFAttributeEval 
  extends AttributeEvaluator 
  implements OptionHandler {

  /** The training instances */
  private Instances m_trainInstances;

  /** The class index */
  private int m_classIndex;
  
  /** The number of attributes */
  private int m_numAttribs;

  /** The number of instances */
  private int m_numInstances;

  /** Numeric class */
  private boolean m_numericClass;

  /** The number of classes if class is nominal */
  private int m_numClasses;

  /** 
   * Used to hold the probability of a different class val given nearest
   * instances (numeric class)
   */
  private double m_ndc;

  /** 
   * Used to hold the prob of different value of an attribute given
   * nearest instances (numeric class case)
   */
  private double [] m_nda;

  /**
   * Used to hold the prob of a different class val and different att
   * val given nearest instances (numeric class case)
   */
  private double [] m_ndcda;

  /** Holds the weights that relief assigns to attributes */
  private double [] m_weights;

  /** Prior class probabilities (discrete class case) */
  private double [] m_classProbs;

  /** 
   * The number of instances to sample when estimating attributes
   * default == -1, use all instances
   */
  private int m_sampleM;

  /** The number of nearest hits/misses */
  private int m_Knn;

  /** k nearest scores + instance indexes for n classes */
  private double [][][] m_karray;

  /** Upper bound for numeric attributes */
  private double [] m_maxArray;

  /** Lower bound for numeric attributes */
  private double [] m_minArray;

  /** Keep track of the farthest instance for each class */
  private double [] m_worst;

  /** Index in the m_karray of the farthest instance for each class */
  private int [] m_index;

  /** Number of nearest neighbours stored of each class */
  private int [] m_stored;

  /** Random number seed used for sampling instances */
  private int m_seed;

  /**
   *  used to (optionally) weight nearest neighbours by their distance
   *  from the instance in question. Each entry holds 
   *  exp(-((rank(r_i, i_j)/sigma)^2)) where rank(r_i,i_j) is the rank of
   *  instance i_j in a sequence of instances ordered by the distance
   *  from r_i. sigma is a user defined parameter, default=20
   **/
  private double [] weightsByRank;

  private int m_sigma;

  /** Weight by distance rather than equal weights */
  private boolean weightByDistance;

  /**
   * Constructor
   */
  public ReliefFAttributeEval ()
  {
    resetOptions();
  }

  /**
   * Returns an enumeration describing the available options
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions() 
  {
    
    Vector newVector = new Vector(4);
    
    newVector.addElement(new Option("\tSpecify the number of instances to\n"
				    +"\tsample when estimating attributes.\n"
				    +"\tIf not specified, then all instances\n"
				    +"\twill be used."
				    , "M", 1,"-M <num instances>"));
    newVector.addElement(new Option("\tSeed for randomly sampling instances.\n"
				    +"\t(Default = 1)"
				    ,"D",1,"-D <seed>"));
    newVector.addElement(new Option("\tNumber of nearest neighbours (k) used\n"
				    +"\tto estimate attribute relevances\n"
				    +"\t(Default = 10)."
				    ,"K",1,"-K <number of neighbours>"));
    newVector.addElement(new Option("\tWeight nearest neighbours by distance\n"
				    ,"W",0,"-W"));
    newVector.addElement(new Option("\tSpecify sigma value (used in an exp\n"
				    +"\tfunction to control how quickly\n"
				    +"\tweights for more distant instances\n"
				    +"\tdecrease. Use in conjunction with -W.\n"
				    +"\tSensible value=1/5 to 1/10 of the\n"
				    +"\tnumber of nearest neighbours.\n"
				    +"\t(Default = 2)"
				    ,"A",1,"-A <num>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are: <p>
   *
   * -M <number of instances> <br>
   * Specify the number of instances to sample when estimating attributes. <br>
   * If not specified then all instances will be used. <p>
   *
   * -D <seed> <br>
   * Seed for randomly sampling instances. <p>
   *
   * -K <number of neighbours> <br>
   * Number of nearest neighbours to use for estimating attributes. <br>
   * (Default is 10). <p>
   *
   * -W <br>
   * Weight nearest neighbours by distance. <p>
   *
   * -A <sigma> <br>
   * Specify sigma value (used in an exp function to control how quickly <br>
   * weights decrease for more distant instances). Use in conjunction with <br>
   * -W. Sensible values = 1/5 to 1/10 the number of nearest neighbours. <br>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions(String[] options) throws Exception
  {
    String optionString;
    resetOptions();
    
    weightByDistance = Utils.getFlag('W',options);

    optionString = Utils.getOption('M',options);
    if (optionString.length() != 0)
      {
	m_sampleM = Integer.parseInt(optionString);
      }

    optionString = Utils.getOption('D',options);
    if (optionString.length() != 0)
      {
	m_seed = Integer.parseInt(optionString);
      }

    optionString = Utils.getOption('K',options);
    if (optionString.length() != 0)
      {
	m_Knn = Integer.parseInt(optionString);
	if (m_Knn <=0)
	  {
	    throw new Exception("number of nearest neighbours must be > 0!");
	  }
      }
    
    optionString = Utils.getOption('A',options);
    if (optionString.length() != 0)
      {
	m_sigma = Integer.parseInt(optionString);
	if (m_sigma <=0)
	  {
	    throw new Exception("value of sigma must bee > 0!");
	  }
      }
  }

  /**
   * Gets the current settings of ReliefFAttributeEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions()
  {
    String [] options = new String [9];
    int current = 0;

    if (weightByDistance)
      {
	options[current++] = "-W";
      }
    options[current++] = "-M";options[current++] = ""+m_sampleM;
    options[current++] = "-D";options[current++] = ""+m_seed;
    options[current++] = "-K";options[current++] = ""+m_Knn;
    options[current++] = "-A";options[current++] = ""+m_sigma;

    while (current < options.length) 
      {
	options[current++] = "";
      }

    return options;
  }


  public String toString()
  {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null)
      {
	text.append("ReliefF feature evaluator has not been built yet\n");
      }
    else
      {
	text.append("\tReliefF Ranking Filter");
	text.append("\n\tInstances sampled: ");
	if (m_sampleM == -1)
	  {
	    text.append("all\n");
	  }
	else
	  {
	    text.append(m_sampleM+"\n");
	  }

	text.append("\tNumber of nearest neighbours (k): "+m_Knn+"\n");
	if (weightByDistance)
	  {
	    text.append("\tExponentially decreasing (with distance) "
			+"influence for\n"
			+"\tnearest neighbours. Sigma: "+m_sigma+"\n");
	  }
	else
	  {
	    text.append("\tEqual influence nearest neighbours\n");
	  }
      }
    return text.toString();
  }

  /**
   * Initializes a ReliefF attribute evaluator. 
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator(Instances data) throws Exception
  {
    int z, totalInstances;
    Random r = new Random(m_seed);

    if (data.checkForStringAttributes()) 
      {
	throw new Exception("Can't handle string attributes!");
      }

    m_trainInstances = data;
    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();

    if (m_trainInstances.attribute(m_classIndex).isNumeric())
      {
	m_numericClass = true;
      }
    else
      {
	m_numericClass = false;
      }

    if (!m_numericClass)
      {
	m_numClasses = m_trainInstances.attribute(m_classIndex).numValues();
      }
    else
      {
	m_ndc = 0;
	m_numClasses = 1;
	m_nda = new double [m_numAttribs];
	m_ndcda = new double [m_numAttribs];
      }

    if (weightByDistance) // set up the rank based weights
      {
	weightsByRank = new double [m_Knn];
	for (int i=0;i<m_Knn;i++)
	  {
	    weightsByRank[i] = 
	      Math.exp(-((i/(double)m_sigma)*(i/(double)m_sigma)));
	  }
      }
    // the final attribute weights
    m_weights = new double [m_numAttribs];

    // num classes (1 for numeric class) knn neighbours, 
    // and 0 = distance, 1 = instance index
    m_karray = new double [m_numClasses][m_Knn][2];

    if (!m_numericClass)
      {
	m_classProbs = new double [m_numClasses];
	for (int i=0;i<m_numInstances;i++)
	  {
	    m_classProbs[(int)m_trainInstances.instance(i).value(m_classIndex)]++;
	  }
	for (int i=0;i<m_numClasses;i++)
	  {
	    m_classProbs[i] /= m_numInstances;
	  }
      }
	
    m_worst = new double [m_numClasses];
    m_index = new int [m_numClasses];
    m_stored = new int [m_numClasses];

    m_minArray = new double [m_numAttribs];
    m_maxArray = new double [m_numAttribs];
    for (int i = 0; i < m_numAttribs; i++) 
      {
	m_minArray[i] = m_maxArray[i] = Double.NaN;
      }

    for (int i=0;i<m_numInstances;i++)
      {
	updateMinMax(m_trainInstances.instance(i));
      }

    if ((m_sampleM > m_numInstances) || (m_sampleM == -1))
      {
	totalInstances = m_numInstances;
      }
    else
      {
	totalInstances = m_sampleM;
      }

    // process each instance, updating attribute weights
    for (int i=0;i<totalInstances;i++)
      {
	if (totalInstances == m_numInstances)
	  {
	    z = i;
	  }
	else
	  {
	    z = r.nextInt() % m_numInstances;
	  }
	
	if (z < 0)
	  {
	    z *= -1;
	  }

	if (!(m_trainInstances.instance(z).isMissing(m_classIndex)))
	  {
	    // first clear the knn and worst index stuff for the classes
	    for (int j=0;j<m_numClasses;j++)
	      {
		m_index[j] = m_stored[j] = 0;
		for (int k=0;k<m_Knn;k++)
		  {
		    m_karray[j][k][0] = m_karray[j][k][1] = 0;
		  }
	      }
	  
	    findKHitMiss(z);

	    if (m_numericClass)
	      {
		updateWeightsNumericClass(z);
	      }
	    else
	      {
		updateWeightsDiscreteClass(z);
	      }
	  }
      }

    // now scale weights by 1/m_numInstances (nominal class) or
    // calculate weights numeric class
    //    System.out.println("num inst:"+m_numInstances+" r_ndc:"+r_ndc);
    for (int i=0;i<m_numAttribs;i++)
      if (i != m_classIndex)
	{
	  if (m_numericClass)
	    {
	      m_weights[i] = m_ndcda[i]/m_ndc - 
		((m_nda[i] - m_ndcda[i])/((double)totalInstances-m_ndc));
	    }
	  else
	    {
	      m_weights[i] *= (1.0 / (double)totalInstances);
	    }
	  //	  System.out.println(r_weights[i]);
	}
  }

  /**
   * Evaluates an individual attribute using ReliefF's instance based approach.
   * The actual work is done by buildEvaluator which evaluates all features.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */
  public double evaluateAttribute(int attribute) throws Exception
  {
    return m_weights[attribute];
  }

  /**
   * Reset options to their default values
   */
  protected void resetOptions()
  {
    m_trainInstances = null;
    m_sampleM = -1;
    m_Knn = 10;
    m_sigma = 2;
    weightByDistance = false;
    m_seed = 1;
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) 
  {
    if (Double.isNaN(m_minArray[i]) || Utils.eq(m_maxArray[i], m_minArray[i])) 
      {
	return 0;
      } 
    else 
      {
	return (x - m_minArray[i]) / (m_maxArray[i] - m_minArray[i]);
      }
  }

  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {
    
    for (int j = 0;j < m_numAttribs; j++) 
      {
	if ((m_trainInstances.attribute(j).isNumeric()) && 
	    (!instance.isMissing(j))) 
	  {
	    if (Double.isNaN(m_minArray[j])) 
	      {
		m_minArray[j] = instance.value(j);
		m_maxArray[j] = instance.value(j);
	      } 
	    else 
	      {
		if (instance.value(j) < m_minArray[j]) 
		  {
		    m_minArray[j] = instance.value(j);
		  } 
		else 
		  {
		    if (instance.value(j) > m_maxArray[j]) 
		      {
			m_maxArray[j] = instance.value(j);
		      }
		  }
	      }
	  }
      }
  }
  
  /**
   * Calculate the difference between two attribute values
   *
   * @param attrib the attribute in question
   * @param first the index of the first instance
   * @param second the index of the second instance
   * @return the difference
   */
  private double attributeDiff(int attrib, int first, int second)
  {
    double temp,d;

    // Nominal attribute
    if (m_trainInstances.attribute(attrib).isNominal())
      {
	if (m_trainInstances.instance(first).isMissing(attrib) ||
	    m_trainInstances.instance(second).isMissing(attrib))
	  {
	    temp =  (1.0 - (1.0 / 
			    ((double)m_trainInstances.
			     attribute(attrib).numValues())));
	  }
	else
	  {
	    if (m_trainInstances.instance(first).value(attrib) !=
		m_trainInstances.instance(second).value(attrib))
	      {
		temp = 1.0;
	      }
	    else
	      {
		temp = 0.0;
	      }
	  }
      }
    else
      // Numeric attribute
      {
	if  (m_trainInstances.instance(first).isMissing(attrib) &&
	    m_trainInstances.instance(second).isMissing(attrib))
	  {
	    temp = 1.0; // maximally different
	  }
	else if (m_trainInstances.instance(first).isMissing(attrib))
	  {
	    d = norm(m_trainInstances.instance(second).value(attrib), attrib);
	    if (d < 0.5)
	      {
		d = 1.0 - d;
	      }
	    temp = d;
	  }
	else if (m_trainInstances.instance(second).isMissing(attrib))
	  {
	    d = norm(m_trainInstances.instance(first).value(attrib), attrib);
	    if (d < 0.5)
	      {
		d = 1.0 -d;
	      }
	    temp = d;
	  }
	else
	  {
	    d = norm(m_trainInstances.instance(first).value(attrib), 
		     attrib) - 
	      norm(m_trainInstances.instance(second).value(attrib), 
		   attrib);
	    if (d < 0.0)
	      {
		d *= -1.0;
	      }
	    temp = d;
	  }
      }
    return temp;
  }

  /**
   * Calculate the difference between two instances as a sum of their
   * attribute differences
   *
   * @param first the index of the first instance
   * @param second the index of the second instance
   * @return the difference
   */
  private double diff(int first, int second)
  {
    int i,j;
    double temp = 0;

    for (i=0;i<m_numAttribs;i++)
      {
	if (i != m_classIndex)
	  {
	    temp += attributeDiff(i, first, second);
	  }
      }
    return temp;
  }

  /**
   * update attribute weights given an instance when the class is numeric
   *
   * @param instNum the index of the instance to use when updating weights
   */
  private void updateWeightsNumericClass(int instNum)
  {
    int i,j;
    double temp;
    int [] tempSorted = null;
    double [] tempDist = null;
    double distNorm = 1.0;

    // sort nearest neighbours and set up normalization variable
    if (weightByDistance)
      {
	tempDist = new double[m_stored[0]];
	for (j=0, distNorm = 0;j<m_stored[0];j++) 
	  {
	    // copy the distances
	    tempDist[j] = m_karray[0][j][0];
	    // sum normalizer
	    distNorm += weightsByRank[j];
	  }
	tempSorted = Utils.sort(tempDist);
      }

    for (i=0;i<m_stored[0];i++)
      {
	// P diff prediction (class) given nearest instances
	if (weightByDistance)
	  {
	    temp = attributeDiff(m_classIndex, instNum, 
				 (int)m_karray[0][tempSorted[i]][1]);
	    temp *= (weightsByRank[i] / distNorm);
	  }
	else
	  {
	    temp = attributeDiff(m_classIndex,instNum,
			     (int)m_karray[0][i][1]);
	    temp *= (1.0/(double)m_stored[0]); // equal influence
	  }
	m_ndc += temp;

	// now the attributes
	for (j=0;j<m_numAttribs;j++)
	  {
	    if (j != m_classIndex)
	      {
		// P of different attribute val given nearest instances
		if (weightByDistance)
		  {
		    temp = attributeDiff(j,instNum,
					 (int)m_karray[0][tempSorted[i]][1]);
		    temp *= (weightsByRank[i] / distNorm);
		  }
		else
		  {
		    temp = attributeDiff(j,instNum,
					 (int)m_karray[0][i][1]);
		    temp *= (1.0/(double)m_stored[0]); // equal influence
		  }
		m_nda[j] += temp;

		// P of different prediction and different att value given
		// nearest instances
		if (weightByDistance)
		  {
		    temp = attributeDiff(m_classIndex, instNum,
					 (int)m_karray[0][tempSorted[i]][1]) *
		      attributeDiff(j, instNum,
				    (int)m_karray[0][tempSorted[i]][1]);
		    temp *= (weightsByRank[i] / distNorm);
		  }
		else
		  {
		    temp = attributeDiff(m_classIndex,instNum,
					 (int)m_karray[0][i][1]) *
		      attributeDiff(j,instNum,
				    (int)m_karray[0][i][1]);
		    temp *= (1.0/(double)m_stored[0]); // equal influence
		  }
		m_ndcda[j] += temp;
		
	      }
	  }
      }
  }

  /**
   * update attribute weights given an instance when the class is discrete
   *
   * @param instNum the index of the instance to use when updating weights
   */
  private void updateWeightsDiscreteClass(int instNum)
  {
    int i,j,k;
    int cl;
    double cc = m_numInstances;
    double temp, temp_diff, w_norm=1.0;
    double [] tempDistClass;
    int [] tempSortedClass = null;
    double distNormClass = 1.0;
    double [] tempDistAtt;
    int [][] tempSortedAtt = null;
    double [] distNormAtt = null;

    // get the class of this instance
    cl = (int)m_trainInstances.instance(instNum).value(m_classIndex);

    // sort nearest neighbours and set up normalization variables
    if (weightByDistance)
      {
	// do class (hits) first
	// sort the distances
	tempDistClass = new double[m_stored[cl]];
	for (j=0, distNormClass = 0;j<m_stored[cl];j++) 
	  {
	    // copy the distances
	    tempDistClass[j] = m_karray[cl][j][0];
	    // sum normalizer
	    distNormClass += weightsByRank[j];
	  }
	tempSortedClass = Utils.sort(tempDistClass);

	// do misses (other classes)
	tempSortedAtt = new int[m_numClasses][1];
	distNormAtt = new double[m_numClasses];
	for (k=0;k<m_numClasses;k++)
	  {
	    if (k != cl) // already done cl
	      {
		// sort the distances
		tempDistAtt = new double[m_stored[k]];
		for (j=0, distNormAtt[k] = 0;j<m_stored[k];j++) 
		  {
		    // copy the distances
		    tempDistAtt[j] = m_karray[k][j][0];
		    // sum normalizer
		    distNormAtt[k] += weightsByRank[j];
		  }
		tempSortedAtt[k] = Utils.sort(tempDistAtt);
	      }
	  }
      }

    if (m_numClasses > 2)
      {
	// the amount of probability space left after removing the
	// probability of this instances class value
	w_norm = (1.0 - m_classProbs[cl]);
      }

    for (i=0;i<m_numAttribs;i++)
      {
	if (i != m_classIndex)
	  {
	    // first do k nearest hits
	    for (j=0, temp_diff = 0.0;j < m_stored[cl]; j++)
	      {
		if (weightByDistance)
		  {
		    temp_diff += 
		      attributeDiff(i, instNum, 
				    (int)m_karray[cl][tempSortedClass[j]][1]) *
		      (weightsByRank[j] / distNormClass);
		  }
		else
		  {
		    temp_diff += 
		      attributeDiff(i, instNum, (int)m_karray[cl][j][1]);
		  }
	      }
	

	    // average
	    if ((!weightByDistance) && (m_stored[cl] > 0))
	      {
		temp_diff /= (double)m_stored[cl];
	      }
	    
	    m_weights[i] -= temp_diff;
	    
	    // now do k nearest misses from each of the other classes
	    temp_diff = 0.0;
	    for (k=0;k<m_numClasses;k++)
	      if (k != cl) // already done cl
		{
		  for (j=0,temp=0.0;j<m_stored[k];j++)
		    {
		      if (weightByDistance)
			{
			  temp_diff += 
			    attributeDiff(i, instNum, 
				(int)m_karray[k][tempSortedAtt[k][j]][1]) * 
			    (weightsByRank[j] / distNormAtt[k]);
			}
		      else
			{
			  temp += attributeDiff(i, instNum, 
						(int)m_karray[k][j][1]);
			}
		    }
		  
		  if ((!weightByDistance) && (m_stored[k] > 0))
		    {
		      temp /= (double)m_stored[k];
		    }
		  
		  // now add temp to temp_diff weighted by the prob of this 
		  // class
		  if (m_numClasses > 2)
		    {
		      temp_diff += (m_classProbs[k] / w_norm) * temp;
		    }
		  else
		    {
		      temp_diff += temp;
		    }
		}
	    m_weights[i] += temp_diff;
	  }
      }
  }

  /**
   * Find the K nearest instances to supplied instance if the class is numeric,
   * or the K nearest Hits (same class) and Misses (K from each of the other
   * classes) if the class is discrete.
   *
   * @param instNum the index of the instance to find nearest neighbours of
   */
  private void findKHitMiss(int instNum)
  {
    int i,j;
    int cl;
    double ww;
    double temp_diff = 0.0;

    for (i=0;i<m_numInstances;i++)
      if (i != instNum)
	{
	  temp_diff = diff(i, instNum);

	  // class of this training instance or 0 if numeric
	  if (m_numericClass)
	    {
	      cl = 0;
	    }
	  else
	    {
	      cl = (int)m_trainInstances.instance(i).value(m_classIndex);
	    }

	  // add this diff to the list for the class of this instance
	  if (m_stored[cl] < m_Knn)
	    {
	      m_karray[cl][m_stored[cl]][0] = temp_diff;
	      m_karray[cl][m_stored[cl]][1] = i;
	      m_stored[cl]++;

	      // note the worst diff for this class
	      for (j=0,ww = -1.0;j<m_stored[cl];j++)
		{
		  if (m_karray[cl][j][0] > ww)
		    {
		      ww = m_karray[cl][j][0];
		      m_index[cl] = j;
		    }
		}
	      m_worst[cl] = ww;
	    }
	  else
	    /* if we already have stored knn for this class then check to
	       see if this instance is better than the worst */
	    {
	      if (temp_diff < m_karray[cl][m_index[cl]][0])
		{
		  m_karray[cl][m_index[cl]][0] = temp_diff;
		  m_karray[cl][m_index[cl]][1] = i;

		  for (j=0,ww = -1.0;j<m_stored[cl];j++)
		    {
		      if (m_karray[cl][j][0] > ww)
			{
			  ww = m_karray[cl][j][0];
			  m_index[cl] = j;
			}
		    }
		  m_worst[cl] = ww;
		}
	    }
	}
  }

  // ============
  // Test method.
  // ============
  
  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main(String [] args)
  {
    try 
      {
	System.out.println(AttributeSelection.
			   SelectAttributes(new ReliefFAttributeEval(), args));
      }
    catch (Exception e)
      {
	e.printStackTrace();
	System.out.println(e.getMessage());
      }
  }
}
