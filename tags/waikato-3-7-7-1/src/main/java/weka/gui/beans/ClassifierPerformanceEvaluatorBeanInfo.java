/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    ClassifierPerformanceEvaluatorBeanInfo.java
 *    Copyright (C) 2002 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.beans.*;

/**
 * Bean info class for the classifier performance evaluator
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision$
 */
public class ClassifierPerformanceEvaluatorBeanInfo extends SimpleBeanInfo {
  
  public EventSetDescriptor [] getEventSetDescriptors() {
    try {
      EventSetDescriptor [] esds = { 
	new EventSetDescriptor(ClassifierPerformanceEvaluator.class, 
			       "text", 
			       TextListener.class, 
			       "acceptText"),
	new EventSetDescriptor(ClassifierPerformanceEvaluator.class, 
			       "thresholdData", 
			       ThresholdDataListener.class, 
			       "acceptDataSet"),
      	new EventSetDescriptor(ClassifierPerformanceEvaluator.class, 
			       "visualizableError", 
			       VisualizableErrorListener.class, 
			       "acceptDataSet")};
      return esds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
  
  /**
   * Get the property descriptors for this bean
   *
   * @return a <code>PropertyDescriptor[]</code> value
   */
  public PropertyDescriptor[] getPropertyDescriptors() {
    try {
      PropertyDescriptor p1;
      p1 = new PropertyDescriptor("executionSlots", ClassifierPerformanceEvaluator.class);
      PropertyDescriptor [] pds = { p1 };
      return pds;
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
  
  /**
   * Get the bean descriptor for this bean
   *
   * @return a <code>BeanDescriptor</code> value
   */
  public BeanDescriptor getBeanDescriptor() {
    return new BeanDescriptor(weka.gui.beans.ClassifierPerformanceEvaluator.class,
                              ClassifierPerformanceEvaluatorCustomizer.class);
  }
}
