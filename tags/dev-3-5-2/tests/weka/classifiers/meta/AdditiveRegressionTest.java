/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.meta;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests AdditiveRegression. Run from the command line with:<p>
 * java weka.classifiers.AdditiveRegressionTest
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class AdditiveRegressionTest extends AbstractClassifierTest {

  public AdditiveRegressionTest(String name) { super(name);  }

  /** Creates a default AdditiveRegression */
  public Classifier getClassifier() {
    return new AdditiveRegression();
  }

  public static Test suite() {
    return new TestSuite(AdditiveRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
