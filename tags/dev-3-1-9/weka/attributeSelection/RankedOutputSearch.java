/*
 *    RankedOutputSearch.java
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
import weka.core.*;


/** 
 * Interface for search methods capable of producing a
 * ranked list of attributes.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision 1.1 $
 */
public interface RankedOutputSearch {


  // ===============
  // Public methods.
  // ===============
  
  /**
   * Returns a X by 2 list of attribute indexes and corresponding
   * evaluations from best (highest) to worst.
   * @return the ranked list of attribute indexes in an array of ints
   * @exception Exception if the ranking can't be produced
   */
  public abstract double[][] rankedAttributes() throws Exception;

  /**
   * Sets a threshold by which attributes can be discarded from the
   * ranking. This threshold is used by the AttributeSelection module
   * which does the actual discarding of attributes---the implementer
   * of this method needs only to provide a variable in which to store the
   * supplied threshold. -Double.MAX_VALUE is reserved to mean no threshold,
   * ie, retain all attributes.
   * @param threshold the threshold.
   */
  public abstract void setThreshold(double threshold);

  /**
   * Gets the threshold by which attributes can be discarded. Discarding
   * of attributes is done by the AttributeSelection module using the
   * threshold returned by this method.
   * @return a threshold by which to discard attributes
   */
  public abstract double getThreshold();

  /**
   * Specify the number of attributes to select from the ranked list. -1
   * indicates that all attributes are to be retained. NumToSelect has
   * precedence over threshold, ie. if there is a non -1 value for NumToSelect
   * then this will take precedence over any threshold value.
   * @param numToSelect the number of attributes to retain
   */
  public abstract void setNumToSelect(int numToSelect);

  /**
   * Gets the number of attributes to be retained.
   * @return the number of attributes to retain
   */
  public abstract int getNumToSelect();
  
  /**
   * Sets whether or not ranking is to be performed.
   * When a search method is capable of producing a ranked list
   * of attributes, the user has the choice of seeing the results of a
   * normal search or seeing a ranked list.
   * @param doRanking true if ranked list is to be produced
   */
  public abstract void setGenerateRanking(boolean doRanking);

  /**
   * Gets whether the user has opted to see a ranked list of
   * attributes rather than the normal result of the search
   * @return true if a ranked list has been requested.
   */
  public abstract boolean getGenerateRanking();

}
