/*
 *    RunPanel.java
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

package weka.gui.experiment;

import weka.experiment.Experiment;
import weka.core.Utils;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;



/** 
 * This panel controls the running of an experiment.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public class RunPanel extends JPanel implements ActionListener {

  /** The message displayed when no experiment is running */
  protected final static String NOT_RUNNING = "Not running";

  /** Click to start running the experiment */
  protected JButton m_StartBut = new JButton("Start");

  /** Click to signal the running experiment to halt */
  protected JButton m_StopBut = new JButton("Stop");

  /** Shows current status of the run */
  protected JLabel m_StatusLab = new JLabel(NOT_RUNNING,
					    SwingConstants.CENTER);

  /** Displays error and some status messages */
  protected JTextArea m_ErrorLog = new JTextArea();

  /** The experiment to run */
  protected Experiment m_Exp;

  /** The thread running the experiment */
  protected Thread m_RunThread = null;

  /*
   * A class that handles running a copy of the experiment
   * in a separate thread
   */
  class ExperimentRunner extends Thread {

    Experiment m_ExpCopy;
    
    public ExperimentRunner(final Experiment exp) throws Exception {

      // Create a full copy using serialization
      if (exp == null) {
	System.err.println("Null experiment!!!");
      } else {
	System.err.println("Running experiment: " + exp.toString());
      }
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      BufferedOutputStream bbo = new BufferedOutputStream(bo);
      ObjectOutputStream oo = new ObjectOutputStream(bbo);
      System.err.println("Writing experiment copy");
      oo.writeObject(exp);
      oo.close();
      
      System.err.println("Reading experiment copy");
      ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
      BufferedInputStream bbi = new BufferedInputStream(bi);
      ObjectInputStream oi = new ObjectInputStream(bbi);
      m_ExpCopy = (Experiment) oi.readObject();
      oi.close();
      System.err.println("Made experiment copy");
    }
    
    /**
     * Starts running the experiment.
     */
    public void run() {
      
      m_StartBut.setEnabled(false);
      m_StopBut.setEnabled(true);
      try {
	logMessage("Started");
	m_StatusLab.setText("Initializing...");
	m_ExpCopy.initialize();
	int errors = 0;
	m_StatusLab.setText("Iterating...");
	while (m_RunThread != null && m_ExpCopy.hasMoreIterations()) {
	  try {
	    String current = "Iteration:";
	    if (m_ExpCopy.getUsePropertyIterator()) {
	      int cnum = m_ExpCopy.getCurrentPropertyNumber();
	      String cname = " Custom="
		+ (cnum + 1)
		+ m_ExpCopy.getPropertyArrayValue(cnum).getClass().getName();
	      current += cname;
	    }
	    String dname = ((File) m_ExpCopy.getDatasets()
	      .elementAt(m_ExpCopy.getCurrentDatasetNumber())).getName();
	    current += " Dataset=" + dname
	      + " Run=" + (m_ExpCopy.getCurrentRunNumber());
	    m_StatusLab.setText(current);
	    m_ExpCopy.nextIteration();
	  } catch (Exception ex) {
	    errors ++;
	    logMessage(ex.getMessage());
	    boolean continueAfterError = false;
	    if (continueAfterError) {
	      m_ExpCopy.advanceCounters(); // Try to keep plowing through
	    } else {
	      m_RunThread = null;
	    }
	  }
	}
	m_StatusLab.setText("Postprocessing...");
	m_ExpCopy.postProcess();
	if (m_RunThread == null) {
	  logMessage("Interrupted");
	} else {
	  logMessage("Finished");
	}
	if (errors == 1) {
	  logMessage("There was " + errors + " error");
	} else {
	  logMessage("There were " + errors + " errors");
	}
	m_StatusLab.setText(NOT_RUNNING);
      } catch (Exception ex) {
	ex.printStackTrace();
	System.err.println(ex.getMessage());
	m_StatusLab.setText(ex.getMessage());
      } finally {
	m_RunThread = null;
	m_StartBut.setEnabled(true);
	m_StopBut.setEnabled(false);
      }
      System.err.println("Done...");
    }
  }

  
  /**
   * Creates the run panel with no initial experiment.
   */
  public RunPanel() {

    m_StartBut.addActionListener(this);
    m_StopBut.addActionListener(this);
    m_StartBut.setEnabled(false);
    m_StopBut.setEnabled(false);
    m_StatusLab.setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createTitledBorder("Status"),
			  BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    m_ErrorLog.setEditable(false);
    m_ErrorLog.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


    // Set the GUI layout
    JPanel controls = new JPanel();
    controls.setLayout(new GridLayout(1,2));
    controls.add(m_StartBut);
    controls.add(m_StopBut);

    JPanel output = new JPanel();
    output.setLayout(new BorderLayout());
    output.add(m_StatusLab, BorderLayout.NORTH);
    JScrollPane logOut = new JScrollPane(m_ErrorLog);
    logOut.setBorder(BorderFactory.createTitledBorder("Log"));
    output.add(logOut, BorderLayout.CENTER);

    setLayout(new BorderLayout());
    add(controls, BorderLayout.NORTH);
    add(output, BorderLayout.CENTER);
  }
    
  /**
   * Creates the panel with the supplied initial experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public RunPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }

  /**
   * Sets the experiment the panel operates on.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {
    
    m_Exp = exp;
    m_StartBut.setEnabled(true);
    m_StopBut.setEnabled(false);
    m_RunThread = null;
  }
  
  /**
   * Controls starting and stopping the experiment.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_StartBut) {
      if (m_RunThread == null) {
	try {
	  m_RunThread = new ExperimentRunner(m_Exp);
	  m_RunThread.setPriority(Thread.MIN_PRIORITY); // UI has most priority
	  m_RunThread.start();
	} catch (Exception ex) {
	  logMessage("Problem creating experiment copy to run: "
		     + ex.getMessage());
	}
      }
    } else if (e.getSource() == m_StopBut) {
      m_StopBut.setEnabled(false);
      m_RunThread = null;
    }
  }
  
  /**
   * Gets a string containing current date and time.
   *
   * @return a string containing the date and time.
   */
  protected static String getTimestamp() {

    return (new SimpleDateFormat("yyyy.MM.dd hh:mm:ss")).format(new Date());
  }

  /**
   * Sends the supplied message to the log area. The current timestamp will
   * be prepended.
   *
   * @param message a value of type 'String'
   */
  protected void logMessage(String message) {
    
    m_ErrorLog.append(RunPanel.getTimestamp() + ' ' + message + '\n');
  }
  
  /**
   * Tests out the run panel from the command line.
   *
   * @param args may contain options specifying an experiment to run.
   */
  public static void main(String [] args) {

    try {
      boolean readExp = Utils.getFlag('l', args);
      final String expFile = Utils.getOption('f', args);
      if (readExp && (expFile.length() == 0)) {
	throw new Exception("A filename must be given with the -f option");
      }
      Experiment exp = null;
      if (readExp) {
	FileInputStream fi = new FileInputStream(expFile);
	ObjectInputStream oi = new ObjectInputStream(
			       new BufferedInputStream(fi));
	exp = (Experiment)oi.readObject();
	oi.close();
      } else {
	exp = new Experiment();
      }
      System.err.println("Initial Experiment:\n" + exp.toString());
      final JFrame jf = new JFrame("Run Weka Experiment");
      jf.getContentPane().setLayout(new BorderLayout());
      final RunPanel sp = new RunPanel(exp);
      //sp.setBorder(BorderFactory.createTitledBorder("Setup"));
      jf.getContentPane().add(sp, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.err.println("\nExperiment Configuration\n"
			     + sp.m_Exp.toString());
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
