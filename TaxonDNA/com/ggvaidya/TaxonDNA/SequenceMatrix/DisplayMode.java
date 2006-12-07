/**
 * A DisplayMode handles the actual display of content. It does this by
 * being a TableModel, so the JTable can just use this as its backing
 * database. The trick is to make sure that the JTable doesn't realise
 * that the model is coming from three completely different objects.
 *
 * The way we're going to try to play this is like this: every 
 * DisplayMode inherits from this class, so they all have a repertoire
 * of functions they don't need to reimplement, etc. They will
 * also have a set of functions that TableManager can use as an
 * interface to talk to them - for instance, something like
 * activateDisplay(JTable) will allow the class to set itself up
 * as the TableModel (and active display), then have a deactivateDisplay()
 * to turn it off or something. Or something.
 */

/*
 *
 *  SequenceMatrix
 *  Copyright (C) 2006 Gaurav Vaidya
 *  
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *  
 */

package com.ggvaidya.TaxonDNA.SequenceMatrix;

import java.util.*;	// Vectors, Lists and the like

import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public abstract class DisplayMode implements TableModel {
// The Table we're displaying onto
	/** Please do put your JTable value here, or put in 'null' if you're doing something funky.
	 * There's several functions who expect to find the table here.
	 */
	// my first protected variable! ::sniffs::
	protected JTable		table =			new JTable();

// Table model listeners
	private Vector			tableModelListeners =	new Vector();

//
// 1.	GETTERS. Returns the state or instance variable of the table
// 	at the moment.
//
	public Class getColumnClass(int columnIndex) { return String.class; }

	public abstract int getColumnCount();
	public abstract String getColumnName(int columnIndex);
	public abstract int getRowCount();
	public abstract Object	getValueAt(int rowIndex, int columnIndex);
	public abstract boolean	isCellEditable(int rowIndex, int columnIndex);

// 
// 2.	SETTERS. Lets you set states or variables for us.
//
	public abstract void setValueAt(Object aValue, int rowIndex, int columnIndex);

//
// 3.	FUNCTIONAL CODE. Does something.
//
	
	/** 
	 * Saves the column widths into a Hashtable. You can use restoreWidths() to
	 * restore your widths to where they were a while ago.
	 */
	protected Hashtable saveWidths() {
		Hashtable widths = new Hashtable();
		JTable j = table; 
		if(j == null)
			return null;

		// save all widths
		TableColumnModel tcm = j.getColumnModel();
		if(tcm == null)
			return null;

		Enumeration e = tcm.getColumns();
		while(e.hasMoreElements()) {
			TableColumn tc = (TableColumn) e.nextElement();
			widths.put(tc.getIdentifier(), new Integer(tc.getWidth()));
		}

		return widths;
	}

	/**
	 * Restores the column widths saved into the Hashtable as the column widths
	 * on the JTable.
	 */
	protected void restoreWidths(Hashtable widths) {
		if(widths == null)
			return;
		
		JTable j = table;
		if(j == null)
			return;
		
		// load all widths
		TableColumnModel tcm = j.getColumnModel();
		if(tcm == null)
			return;

		Enumeration e = tcm.getColumns();
		while(e.hasMoreElements()) {
			TableColumn tc = (TableColumn) e.nextElement();

			Integer oldWidth = (Integer) widths.get(tc.getIdentifier());
			if(oldWidth != null)
				tc.setPreferredWidth(oldWidth.intValue());
		}	
	}


//
// 4.	TABLE MODEL LISTENERS.
//
	/** 
	 * Adds a new TableModelListener. Since much funkiness could (and probably will!)
	 * happen with TableModels being set repeatedly, it makes sense to check that
	 * we're not double adding a single listener.
	 */
	public void addTableModelListener(TableModelListener l) {
		if(!tableModelListeners.contains(l))
			tableModelListeners.add(l);
	}

	/**
	 * Removes a TableModelListener.
	 */
	public void removeTableModelListener(TableModelListener l) {
		tableModelListeners.remove(l);
	}

//
// 5.	DISPLAY MODE METHODS	
//
	/** Activate this display on the table mentioned. */
	public void activateDisplay(JTable table) {
		this.table = table;
	}

	/** Deactivate this display from the table mentioned. */
	public void deactivateDisplay() {
		this.table = null;
	}

	/** Update the display (generally by firing an event at all the tableModelListeners). 
	 * Feel free to overload this if you think you've got a better idea - but PLEASE
	 * remember to save and reload the table headers before you do!
	 */
	public void updateDisplay() {
		fireTableModelEvent(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
	}
}