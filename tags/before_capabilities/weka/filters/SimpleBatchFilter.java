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

/*
 * SimpleBatchFilter.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.filters;

import weka.core.Instance;
import weka.core.Instances;

/** 
 * This filter is a superclass for simple batch filters. 
 * <p/>
 * 
 * <b>General notes:</b><br/>
 * <ul>
 *   <li>After adding instances to the filter via input(Instance) one always
 *   has to call batchFinished() to make them available via output(). </li>
 *   <li>After the first call of batchFinished() the field m_FirstBatchDone is
 *   set to <code>true</code>. </li>
 * </ul>
 * <p/>
 * 
 * <b>Example:</b><br/>
 * The following code snippet uses the filter <code>SomeFilter</code> on a
 * dataset that is loaded from <code>filename</code>.
 * <pre>
 * import weka.core.*;
 * import weka.filters.*;
 * import java.io.*;
 * ...
 * SomeFilter filter = new SomeFilter();
 * // set necessary options for the filter
 * Instances data = new Instances(
 *                    new BufferedReader(
 *                      new FileReader(filename)));
 * Instances filteredData = Filter.useFilter(data, filter);
 * </pre>
 *
 * <b>Implementation:</b><br/>
 * Only the following abstract methods need to be implemented:
 * <ul>
 *   <li>globalInfo()</li>
 *   <li>determineOutputFormat(Instances)</li>
 *   <li>process(Instances)</li>
 * </ul>
 * <p/>
 * 
 * If more options are necessary, then the following methods need to be
 * overriden:
 * <ul>
 *   <li>listOptions()</li>
 *   <li>setOptions(String[])</li>
 *   <li>getOptions()</li>
 * </ul>
 * <p/>
 *
 * To make the filter available from commandline one must add the following
 * main method for correct execution (&lt;Filtername&gt; must be replaced
 * with the actual filter classname):
 * <pre>
 *  public static void main(String[] args) {
 *    try {
 *      if (Utils.getFlag('b', args))
 *        Filter.batchFilterFile(new &lt;Filtername&gt;(), args);
 *      else
 *        Filter.filterFile(new &lt;Filtername&gt;(), args);
 *    } 
 *    catch (Exception e) {
 *      e.printStackTrace();
 *    }
 *  }
 * </pre>
 * <p/>
 *
 * <b>Example implementation:</b><br/>
 * <pre>
 * import weka.core.*;
 * import weka.filters.*;
 *
 * public class SimpleBatch
 *   extends SimpleBatchFilter {
 *   
 *   public String globalInfo() {
 *     return "A simple batch filter that adds an additional attribute 'bla' at the end containing the index of the processed instance.";
 *   }
 * 
 *   protected Instances determineOutputFormat(Instances inputFormat) {
 *     Instances result = new Instances(inputFormat, 0);
 *     result.insertAttributeAt(new Attribute("bla"), result.numAttributes());
 *     return result;
 *   }
 * 
 *   protected Instances process(Instances inst) {
 *     Instances result = new Instances(determineOutputFormat(inst), 0);
 *     for (int i = 0; i &lt; inst.numInstances(); i++) {
 *       double[] values = new double[result.numAttributes()];
 *       for (int n = 0; n &lt; inst.numAttributes(); n++)
 *         values[n] = inst.instance(i).value(n);
 *       values[values.length - 1] = i;
 *       result.add(new Instance(1, values));
 *     }
 *     return result;
 *   }
 * 
 *   public static void main(String[] args) {
 *     try {
 *       if (Utils.getFlag('b', args))
 *         Filter.batchFilterFile(new SimpleBatch(), args);
 *       else
 *         Filter.filterFile(new SimpleBatch(), args);
 *     } 
 *     catch (Exception e) {
 *       e.printStackTrace();
 *     }
 *   }
 * }
 * </pre>
 * <p/>
 * 
 * <b>Options:</b><br/>
 * Valid filter-specific options are:<p/>
 *
 * -D <br/>
 * Turns on output of debugging information.<p/>
 *
 * @author  FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 * @see     SimpleStreamFilter 
 * @see     #input(Instance)
 * @see     #batchFinished()
 * @see     #m_FirstBatchDone
 */
public abstract class SimpleBatchFilter
  extends SimpleFilter {

  /**
   * returns true if the output format is immediately available after the
   * input format has been set and not only after all the data has been
   * seen (see batchFinished())
   *
   * @return      true if the output format is immediately available
   * @see         #batchFinished()
   * @see         #setInputFormat(Instances)
   */
  protected boolean hasImmediateOutputFormat() {
    return false;
  }

  /**
   * Input an instance for filtering. Filter requires all
   * training instances be read before producing output (calling the method
   * batchFinished() makes the data available). If this instance is part of
   * a new batch, m_NewBatch is set to false.
   *
   * @param instance    the input instance
   * @return            true if the filtered instance may now be
   *                    collected with output().
   * @throws  IllegalStateException if no input structure has been defined
   * @see     #batchFinished()
   */
  public boolean input(Instance instance) {
    if (getInputFormat() == null)
      throw new IllegalStateException("No input instance format defined");
    
    if (m_NewBatch) {
      resetQueue();
      m_NewBatch = false;
    }

    bufferInput(instance);

    return false;
  }

  /**
   * Signify that this batch of input to the filter is finished. If
   * the filter requires all instances prior to filtering, output()
   * may now be called to retrieve the filtered instances. Any
   * subsequent instances filtered should be filtered based on setting
   * obtained from the first batch (unless the setInputFormat has been
   * re-assigned or new options have been set). Sets m_FirstBatchDone
   * and m_NewBatch to true.
   *
   * @return true if there are instances pending output
   * @throws IllegalStateException if no input format has been set. 
   * @see    #m_NewBatch
   * @see    #m_FirstBatchDone 
   */
  public boolean batchFinished() {
    int         i;
    Instances   inst;
    
    if (getInputFormat() == null)
      throw new IllegalStateException("No input instance format defined");

    try {
      // get data, clear input queue
      inst = new Instances(getInputFormat());
      flushInput();

      // if output format hasn't been set yet, do it now
      if (!hasImmediateOutputFormat())
        setOutputFormat(determineOutputFormat(new Instances(inst, 0)));

      // process data
      inst = process(inst);

      // move it to the output
      for (i = 0; i < inst.numInstances(); i++)
        push(inst.instance(i));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    m_NewBatch       = true;
    m_FirstBatchDone = true;
    
    return (numPendingOutput() != 0);
  }
}

