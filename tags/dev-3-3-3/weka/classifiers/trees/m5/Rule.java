/*
 *    Rule.java
 *    Copyright (C) 2000 Mark Hall
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
package weka.classifiers.trees.m5;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.classifiers.*;
import weka.filters.*;

/**
 * Generates a single m5 tree or rule
 *
 * @author Mark Hall
 * @version $Revision: 1.2 $
 */
public class Rule {

  protected static int LEFT = 0;
  protected static int RIGHT = 1;

  /**
   * the instances covered by this rule
   */
  private Instances  m_instances;

  /**
   * the class index
   */
  private int        m_classIndex;

  /**
   * the number of attributes
   */
  private int        m_numAttributes;

  /**
   * the number of instances in the dataset
   */
  private int        m_numInstances;

  /**
   * the indexes of the attributes used to split on for this rule
   */
  private int[]      m_splitAtts;

  /**
   * the corresponding values of the split points
   */
  private double[]   m_splitVals;

  /**
   * the corresponding internal nodes. Used for smoothing rules.
   */
  private RuleNode[] m_internalNodes;

  /**
   * the corresponding relational operators (0 = "<=", 1 = ">")
   */
  private int[]      m_relOps;

  /**
   * the leaf encapsulating the linear model for this rule
   */
  private RuleNode   m_ruleModel;

  /**
   * the top of the m5 tree for this rule
   */
  protected RuleNode   m_topOfTree;

  /**
   * the standard deviation of the class for all the instances
   */
  private double     m_globalStdDev;

  /**
   * the absolute deviation of the class for all the instances
   */
  private double     m_globalAbsDev;

  /**
   * the instances covered by this rule
   */
  private Instances  m_covered;

  /**
   * the number of instances covered by this rule
   */
  private int        m_numCovered;

  /**
   * the instances not covered by this rule
   */
  private Instances  m_notCovered;

  /**
   * use a pruned m5 tree rather than make a rule
   */
  private boolean    m_useTree;

  /**
   * grow and prune a full m5 tree rather than use the PART heuristic
   */
  private boolean    m_growFullTree;

  /**
   * use the original m5 smoothing procedure
   */
  private boolean    m_smoothPredictions;

  /**
   * Save instances at each node in an M5 tree for visualization purposes.
   */
  private boolean m_saveInstances;

  /**
   * Make a regression tree instead of a model tree
   */
  private boolean m_regressionTree;

  /**
   * Constructor declaration
   *
   */
  public Rule() {
    m_useTree = false;
    m_growFullTree = true;
    m_smoothPredictions = false;
  }

  /**
   * Generates a single rule or m5 model tree.
   * 
   * @param data set of instances serving as training data
   * @exception Exception if the rule has not been generated
   * successfully
   */
  public void buildClassifier(Instances data) throws Exception {
    m_instances = null;
    m_topOfTree = null;
    m_covered = null;
    m_notCovered = null;
    m_ruleModel = null;
    m_splitAtts = null;
    m_splitVals = null;
    m_relOps = null;
    m_internalNodes = null;
    m_instances = data;
    m_classIndex = m_instances.classIndex();
    m_numAttributes = m_instances.numAttributes();
    m_numInstances = m_instances.numInstances();

    // first calculate global deviation of class attribute
    m_globalStdDev = Rule.stdDev(m_classIndex, m_instances);
    m_globalAbsDev = Rule.absDev(m_classIndex, m_instances);

    m_topOfTree = new RuleNode(m_globalStdDev, m_globalAbsDev, null);
    m_topOfTree.setSmoothing(m_smoothPredictions);
    m_topOfTree.setSaveInstances(m_saveInstances);
    m_topOfTree.setRegressionTree(m_regressionTree);
    m_topOfTree.buildClassifier(m_instances);


    // m_topOfTree.numLeaves(0);
    if (m_growFullTree) {
	m_topOfTree.prune();
	// m_topOfTree.printAllModels();
	m_topOfTree.numLeaves(0);
    } 

    if (!m_useTree) {      
      makeRule();
      // save space
      //      m_topOfTree = null;
    }

    // save space
    m_instances = new Instances(m_instances, 0);
  } 

  /**
   * Calculates a prediction for an instance using this rule
   * or M5 model tree
   * 
   * @param inst the instance whos class value is to be predicted
   * @return the prediction
   * @exception if a prediction can't be made.
   */
  public double classifyInstance(Instance instance) throws Exception {
    if (m_useTree) {
      return m_topOfTree.classifyInstance(instance);
    } 

    // does the instance pass the rule's conditions?
    if (m_splitAtts.length > 0) {
      for (int i = 0; i < m_relOps.length; i++) {
	if (m_relOps[i] == LEFT)    // left
	 {
	  if (instance.value(m_splitAtts[i]) > m_splitVals[i]) {
	    throw new Exception("Rule does not classify instance");
	  } 
	} else {
	  if (instance.value(m_splitAtts[i]) <= m_splitVals[i]) {
	    throw new Exception("Rule does not classify instance");
	  } 
	} 
      } 
    } 

    // the linear model's prediction for this rule
    // add smoothing code here
    if (m_smoothPredictions) {
      double pred = m_ruleModel.classifyInstance(instance);
      int n = m_ruleModel.m_numInstances;
      double supportPred;
      Instance tempInst;
      for (int i = 0; i < m_internalNodes.length; i++) {
	tempInst = m_internalNodes[i].applyNodeFilter(instance);
	supportPred = m_internalNodes[i].getModel().classifyInstance(tempInst);
	pred = RuleNode.smoothingOriginal(n, pred, supportPred);
	n = m_internalNodes[i].m_numInstances;
      }
      return pred;
    }
    return m_ruleModel.classifyInstance(instance);
  } 

  /**
   * Make the single best rule from a pruned m5 model tree
   * 
   * @exception if something goes wrong.
   */
  private void makeRule() throws Exception {
    RuleNode[] best_leaf = new RuleNode[1];
    double[]   best_cov = new double[1];
    RuleNode   temp;

    m_notCovered = new Instances(m_instances, 0);
    m_covered = new Instances(m_instances, 0);
    best_cov[0] = -1;
    best_leaf[0] = null;

    m_topOfTree.findBestLeaf(best_cov, best_leaf);

    temp = best_leaf[0];

    if (temp == null) {
      throw new Exception("Unable to generate rule!");
    } 

    // save the linear model for this rule
    m_ruleModel = temp;

    int count = 0;

    while (temp.parentNode() != null) {
      count++;
      temp = temp.parentNode();
    } 

    temp = best_leaf[0];
    m_relOps = new int[count];
    m_splitAtts = new int[count];
    m_splitVals = new double[count];
    if (m_smoothPredictions) {
      m_internalNodes = new RuleNode[count];
    }

    // trace back to the root
    int i = 0;

    while (temp.parentNode() != null) {
      m_splitAtts[i] = temp.parentNode().splitAtt();
      m_splitVals[i] = temp.parentNode().splitVal();

      if (temp.parentNode().leftNode() == temp) {
	m_relOps[i] = LEFT;
	//	temp.parentNode().m_right = null;
      } else {
	m_relOps[i] = RIGHT;
	//	temp.parentNode().m_left = null;
      }

      if (m_smoothPredictions) {
	m_internalNodes[i] = temp.parentNode();
      }

      temp = temp.parentNode();
      i++;
    } 

    // now assemble the covered and uncovered instances
    boolean ok;

    for (i = 0; i < m_numInstances; i++) {
      ok = true;

      for (int j = 0; j < m_relOps.length; j++) {
	if (m_relOps[j] == LEFT)
	 {
	  if (m_instances.instance(i).value(m_splitAtts[j]) 
		  > m_splitVals[j]) {
	    m_notCovered.add(m_instances.instance(i));
	    ok = false;
	    break;
	  } 
	} else {
	  if (m_instances.instance(i).value(m_splitAtts[j]) 
		  <= m_splitVals[j]) {
	    m_notCovered.add(m_instances.instance(i));
	    ok = false;
	    break;
	  } 
	} 
      } 

      if (ok) {
	m_numCovered++;
	//	m_covered.add(m_instances.instance(i));
      } 
    } 
  } 

  /**
   * Return a description of the m5 tree or rule
   * 
   * @return a description of the m5 tree or rule as a String
   */
  public String toString() {
    if (m_useTree) {
      return treeToString();
    } else {
      return ruleToString();
    } 
  } 

  /**
   * Return a description of the m5 tree
   * 
   * @return a description of the m5 tree as a String
   */
  private String treeToString() {
    StringBuffer text = new StringBuffer();

    if (m_topOfTree == null) {
      return "Tree/Rule has not been built yet!";
    } 

    text.append("Pruned training "
		+ ((m_regressionTree) 
		   ? "regression "
		   : "model ")
		+"tree:\n");

    if (m_smoothPredictions == true) {
      text.append("(using smoothed predictions)\n");
    } 

    text.append(m_topOfTree.treeToString(0));
    text.append(m_topOfTree.printLeafModels());
    text.append("\nNumber of Rules : " + m_topOfTree.numberOfLinearModels());

    return text.toString();
  } 

  /**
   * Return a description of the rule
   * 
   * @return a description of the rule as a String
   */
  private String ruleToString() {
    StringBuffer text = new StringBuffer();

    if (m_splitAtts.length > 0) {
      text.append("IF\n");

      for (int i = m_splitAtts.length - 1; i >= 0; i--) {
	text.append("\t" + m_covered.attribute(m_splitAtts[i]).name() + " ");

	if (m_relOps[i] == 0) {
	  text.append("<= ");
	} else {
	  text.append("> ");
	} 

	text.append(Utils.doubleToString(m_splitVals[i], 1, 3) + "\n");
      } 

      text.append("THEN\n");
    } 

    if (m_ruleModel != null) {
      try {
	text.append(m_ruleModel.printNodeLinearModel());
	text.append(" [" + m_numCovered/*m_covered.numInstances()*/);

	if (m_globalAbsDev > 0.0) {
	  text.append("/"+Utils.doubleToString((100 * 
						   m_ruleModel.
						   rootMeanSquaredError() / 
						   m_globalAbsDev), 1, 3) 
		      + "%]\n\n");
	} else {
	  text.append("]\n\n");
	} 
      } catch (Exception e) {
	return "Can't print rule";
      } 
    } 
    
    //    System.out.println(m_instances);
    return text.toString();
  } 

  /**
   * Use an m5 tree rather than generate rules
   * 
   * @param u true if m5 tree is to be used
   */
  public void setUseTree(boolean u) {
    m_useTree = u;
  } 

  /**
   * get whether an m5 tree is being used rather than rules
   * 
   * @return true if an m5 tree is being used.
   */
  public boolean getUseTree() {
    return m_useTree;
  } 

  /**
   * Grow a full tree instead of using the PART heuristic
   * 
   * @param g true if a full tree is to be grown rather than using
   * the part heuristic
   */
  public void setGrowFullTree(boolean g) {
    m_growFullTree = g;
  } 

  /**
   * Get whether or not a full tree has been grown
   * 
   * @return true if a full tree has been grown
   */
  public boolean getGrowFullTree() {
    return m_growFullTree;
  } 

  /**
   * Smooth predictions
   * 
   * @param s true if smoothing is to be used
   */
  public void setSmoothing(boolean s) {
    m_smoothPredictions = s;
  } 

  /**
   * Get whether or not smoothing has been turned on
   * 
   * @return true if smoothing is being used
   */
  public boolean getSmoothing() {
    return m_smoothPredictions;
  } 

  /**
   * Get the instances not covered by this rule
   * 
   * @return the instances not covered
   */
  public Instances notCoveredInstances() {
    return m_notCovered;
  } 

//    /**
//     * Get the instances covered by this rule
//     * 
//     * @return the instances covered by this rule
//     */
//    public Instances coveredInstances() {
//      return m_covered;
//    } 

  /**
   * Returns the standard deviation value of the supplied attribute index.
   *
   * @param attr an attribute index
   * @param inst the instances
   * @return the standard deviation value
   */
  protected final static double stdDev(int attr, Instances inst) {
    int i,count=0;
    double sd,va,sum=0.0,sqrSum=0.0,value;
    
    for(i = 0; i <= inst.numInstances() - 1; i++) {
      count++;
      value = inst.instance(i).value(attr);
      sum +=  value;
      sqrSum += value * value;
    }
    
    if(count > 1) {
      va = (sqrSum - sum * sum / count) / count;
      va = Math.abs(va);
      sd = Math.sqrt(va);
    } else {
      sd = 0.0;
    }

    return sd;
  }

  /**
   * Returns the absolute deviation value of the supplied attribute index.
   *
   * @param attr an attribute index
   * @param inst the instances
   * @return the absolute deviation value
   */
  protected final static double absDev(int attr, Instances inst) {
    int i;
    double average=0.0,absdiff=0.0,absDev;
    
    for(i = 0; i <= inst.numInstances()-1; i++) {
      average  += inst.instance(i).value(attr);
    }
    if(inst.numInstances() > 1) {
      average /= (double)inst.numInstances();
      for(i=0; i <= inst.numInstances()-1; i++) {
	absdiff += Math.abs(inst.instance(i).value(attr) - average);
      }
      absDev = absdiff / (double)inst.numInstances();
    } else {
      absDev = 0.0;
    }
   
    return absDev;
  }

  /**
   * Sets whether instances at each node in an M5 tree should be saved
   * for visualization purposes. Default is to save memory.
   *
   * @param save a <code>boolean</code> value
   */
  protected void setSaveInstances(boolean save) {
    m_saveInstances = save;
  }

  /**
   * Get the value of regressionTree.
   *
   * @return Value of regressionTree.
   */
  public boolean getRegressionTree() {
    
    return m_regressionTree;
  }
  
  /**
   * Set the value of regressionTree.
   *
   * @param newregressionTree Value to assign to regressionTree.
   */
  public void setRegressionTree(boolean newregressionTree) {
    
    m_regressionTree = newregressionTree;
  }
}


