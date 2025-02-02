/*
 *    Explorer.java
 *    Copyright (C) 1999 Len Trigg
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

package weka.gui.explorer;

import weka.core.Utils;
import weka.gui.LogPanel;

import java.io.File;
import java.io.FileInputStream;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.text.SimpleDateFormat;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.SwingConstants;


/** 
 * The main class for the Weka explorer. Lets the user create,
 * open, save, configure, datasets, and perform ML analysis.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class Explorer extends JPanel {

  /** The panel for preprocessing instances */
  protected PreprocessPanel m_PreprocessPanel = new PreprocessPanel();

  /** The panel for running classifiers */
  protected ClassifierPanel m_ClassifierPanel = new ClassifierPanel();

  /** Label for a panel that still need to be implemented */
  protected JLabel m_ClustererPanel = new JLabel("Not implemented yet :-)",
						 SwingConstants.CENTER);

  /** Label for a panel that still need to be implemented */
  protected AssociationsPanel m_AssociationPanel = new AssociationsPanel();

  /** Label for a panel that still need to be implemented */
  protected AttributeSelectionPanel m_AttributeSelectionPanel =
    new AttributeSelectionPanel();

  /** Label for a panel that still need to be implemented */
  protected JLabel m_VisualizePanel =
    new JLabel("Not implemented yet :-)", SwingConstants.CENTER);
  
  /** The tabbed pane that controls which sub-pane we are working with */
  protected JTabbedPane m_TabbedPane = new JTabbedPane();
  
  /** The panel for log and status messages */
  protected LogPanel m_LogPanel = new LogPanel();

  /**
   * Creates the experiment environment gui with no initial experiment
   */
  public Explorer() {

    String date = (new SimpleDateFormat("EEEE, d MMMM yyyy"))
      .format(new Date());
    m_LogPanel.logMessage("Weka Knowledge Explorer was written by Len Trigg, Mark Hall and Eibe Frank");
    m_LogPanel.logMessage("(c) 1999 The University of Waikato, Hamilton,"
			  + " New Zealand");
    m_LogPanel.logMessage("web: http://www.cs.waikato.ac.nz/~ml/");
    m_LogPanel.logMessage("email: wekasupport@cs.waikato.ac.nz");
    m_LogPanel.logMessage("Started on " + date);
    m_LogPanel.statusMessage("Welcome to the Weka Knowledge Explorer");
    m_PreprocessPanel.setLog(m_LogPanel);
    m_ClassifierPanel.setLog(m_LogPanel);
    m_AssociationPanel.setLog(m_LogPanel);
    m_AttributeSelectionPanel.setLog(m_LogPanel);
    m_TabbedPane.addTab("Preprocess", null, m_PreprocessPanel,
			"Open/Edit/Save instances");
    m_TabbedPane.addTab("Classify", null, m_ClassifierPanel,
			"Classify instances");
    m_TabbedPane.addTab("Cluster", null, m_ClustererPanel,
		      "Identify instance clusters");
    m_TabbedPane.addTab("Associate", null, m_AssociationPanel,
		      "Discover association rules");
    m_TabbedPane.addTab("Select features", null, m_AttributeSelectionPanel,
		      "Determine relevance of attributes");
    m_TabbedPane.addTab("Visualize", null, m_VisualizePanel,
		      "Explore the data");
    m_TabbedPane.setSelectedIndex(0);
    m_TabbedPane.setEnabledAt(1, false);
    m_TabbedPane.setEnabledAt(2, false);
    m_TabbedPane.setEnabledAt(3, false);
    m_TabbedPane.setEnabledAt(4, false);
    m_TabbedPane.setEnabledAt(5, false);
    m_PreprocessPanel.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	m_ClassifierPanel.setInstances(m_PreprocessPanel
				       .getWorkingInstances());
	m_AssociationPanel.setInstances(m_PreprocessPanel
				       .getWorkingInstances());
	m_AttributeSelectionPanel.setInstances(m_PreprocessPanel
					       .getWorkingInstances());
	m_TabbedPane.setEnabledAt(1, true);
	m_TabbedPane.setEnabledAt(2, true);
	m_TabbedPane.setEnabledAt(3, true);
	m_TabbedPane.setEnabledAt(4, true);
	m_TabbedPane.setEnabledAt(5, true);
      }
    });
    
    setLayout(new BorderLayout());
    add(m_TabbedPane, BorderLayout.CENTER);
    add(m_LogPanel, BorderLayout.SOUTH);
  }

  /**
   * Tests out the explorer environment.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Weka Knowledge Explorer");
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(new Explorer(), BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
