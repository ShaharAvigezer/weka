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
 *    LogWriter.java
 *    Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */


package weka.gui.beans;

/**
 * Interface to be implemented by classes that should be able to write their
 * own output to the Weka logger. This is useful, for example, for filters
 * that provide detailed processing instructions.
 * 
 * @author Carsten Pohle (cp AT cpohle de)
 * @version $Revision$
 */
public interface LogWriter {
  /**
   * Set a logger
   *
   * @param logger a <code>weka.gui.Logger</code> value
   */
  void setLog(weka.gui.Logger logger);
}
