/*
 *    GeneratorPropertyIteratorPanel.java
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

import weka.gui.GenericArrayEditor;
import weka.gui.PropertySelectorDialog;
import weka.experiment.PropertyNode;
import weka.experiment.Experiment;
import weka.experiment.ResultProducer;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


/** 
 * This panel controls setting a list of values for an arbitrary
 * resultgenerator property for an experiment to iterate over.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class GeneratorPropertyIteratorPanel extends JPanel
  implements ActionListener {

  /** Click to select the property to iterate over */
  protected JButton m_ConfigureBut = new JButton("Select property...");

  /** Controls whether the custom iterator is used or not */
  protected JComboBox m_StatusBox = new JComboBox();

  /** Allows editing of the custom property values */
  protected GenericArrayEditor m_ArrayEditor = new GenericArrayEditor();

  /** The experiment this all applies to */
  protected Experiment m_Exp;
  
  /**
   * Creates the property iterator panel initially disabled.
   */
  public GeneratorPropertyIteratorPanel() {

    String [] options = {"Disabled", "Enabled"};
    ComboBoxModel cbm = new DefaultComboBoxModel(options);
    m_StatusBox.setModel(cbm);
    m_StatusBox.setSelectedIndex(0);
    m_StatusBox.addActionListener(this);
    m_StatusBox.setEnabled(false);
    m_ConfigureBut.setEnabled(false);
    m_ConfigureBut.addActionListener(this);
    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1, 2));
    buttons.add(m_StatusBox);
    buttons.add(m_ConfigureBut);
    buttons.setMaximumSize(new Dimension(buttons.getMaximumSize().width,
					   buttons.getMinimumSize().height));
    setBorder(BorderFactory.createTitledBorder("Generator properties"));
    setLayout(new BorderLayout());
    add(buttons, BorderLayout.NORTH);
    //    add(Box.createHorizontalGlue());
    m_ArrayEditor.setBorder(BorderFactory.createEtchedBorder());
    m_ArrayEditor.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
	System.err.println("Updating experiment property iterator array");
	m_Exp.setPropertyArray(m_ArrayEditor.getValue());
      }
    });
    add(m_ArrayEditor, BorderLayout.CENTER);
  }

  /**
   * Creates the property iterator panel and sets the experiment.
   *
   * @param exp a value of type 'Experiment'
   */
  public GeneratorPropertyIteratorPanel(Experiment exp) {

    this();
    setExperiment(exp);
  }
  
  /**
   * Sets the experiment which will have the custom properties edited.
   *
   * @param exp a value of type 'Experiment'
   */
  public void setExperiment(Experiment exp) {

    m_Exp = exp;
    m_StatusBox.setEnabled(true);
    m_ArrayEditor.setValue(m_Exp.getPropertyArray());
    if (m_Exp.getPropertyArray() == null) {
      m_StatusBox.setSelectedIndex(0);
      m_ConfigureBut.setEnabled(false);
    } else {
      m_StatusBox.setSelectedIndex(m_Exp.getUsePropertyIterator() ? 1 : 0);
      m_ConfigureBut.setEnabled(m_Exp.getUsePropertyIterator());
    }
    validate();
  }

  /**
   * Gets the user to select a property of the current resultproducer.
   *
   * @return APPROVE_OPTION if the selection went OK, otherwise the selection
   * was cancelled.
   */
  protected int selectProperty() {
    
    final PropertySelectorDialog jd = new PropertySelectorDialog(null,
					  m_Exp.getResultProducer());
    jd.setLocationRelativeTo(this);
    int result = jd.showDialog();
    if (result == PropertySelectorDialog.APPROVE_OPTION) {
      System.err.println("Property Selected");
      PropertyNode [] path = jd.getPath();
      Object value = path[path.length - 1].value;
      PropertyDescriptor property = path[path.length - 1].property;
      // Make an array containing the propertyValue
      Class propertyClass = property.getPropertyType();
      m_Exp.setPropertyPath(path);
      m_Exp.setPropertyArray(Array.newInstance(propertyClass, 1));
      Array.set(m_Exp.getPropertyArray(), 0, value);	
      // Pass it to the arrayeditor
      m_ArrayEditor.setValue(m_Exp.getPropertyArray());
      m_ArrayEditor.repaint();
      System.err.println("Set new array to array editor");
    } else {
      System.err.println("Cancelled");
    }
    return result;
  }

  /**
   * Handles the various button clicking type activities.
   *
   * @param e a value of type 'ActionEvent'
   */
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == m_ConfigureBut) {
      selectProperty();
    } else if (e.getSource() == m_StatusBox) {
      // Toggles whether the custom property is used
      if (m_StatusBox.getSelectedIndex() == 0) {
	m_Exp.setUsePropertyIterator(false);
	m_ConfigureBut.setEnabled(false);
	m_ArrayEditor.setEnabled(false);
	validate();
      } else {
	if (m_Exp.getPropertyArray() == null) {
	  selectProperty();
	}
	if (m_Exp.getPropertyArray() == null) {
	  m_StatusBox.setSelectedIndex(0);
	} else {
	  m_Exp.setUsePropertyIterator(true);
	  m_ConfigureBut.setEnabled(true);
	  m_ArrayEditor.setEnabled(true);
	}
	validate();
      }
    }
  }
  
  /**
   * Tests out the panel from the command line.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    try {
      final JFrame jf = new JFrame("Generator Property Iterator");
      jf.getContentPane().setLayout(new BorderLayout());
      GeneratorPropertyIteratorPanel gp = new GeneratorPropertyIteratorPanel();
      jf.getContentPane().add(gp, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      System.err.println("Short nap");
      Thread.currentThread().sleep(3000);
      System.err.println("Done");
      gp.setExperiment(new Experiment());
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
