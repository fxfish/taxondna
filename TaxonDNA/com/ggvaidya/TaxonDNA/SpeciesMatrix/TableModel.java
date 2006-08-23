/**
 * The TableModel for the main gene/sequence list thing.
 * Talks with SequenceGrid (which handles the backend),
 * and focuses on JUST handling things with the front-end
 * (i.e. the JTable).
 */

/*
 *
 *  SpeciesMatrix
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

package com.ggvaidya.TaxonDNA.SpeciesMatrix;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;		// "Come, thou Tortoise, when?"
import javax.swing.event.*;
import javax.swing.table.*;

import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;
import com.ggvaidya.TaxonDNA.DNA.formats.*;
import com.ggvaidya.TaxonDNA.UI.*;

public class TableModel implements javax.swing.table.TableModel {
	SpeciesMatrix 	matrix = null;
	SequenceGrid 	seqGrid = null;

	/** We are required to keep track of classes which would like to be notified of changes */
	Vector listeners = new Vector();

	//
	//	1.	CONSTRUCTORS.
	//
	/**
	 * Creates a TableModel which will uses the specified SpeciesMatrix.
	 */
	public MatrixModel(SpeciesMatrix matrix) {
		this.matrix = matrix;
		seqGrid = matrix.getSequenceGrid();
	}

	/**
	 * Informs us that the underlying data has changed. We tell
	 * everybody who knows, who will - presumably - be in touch.
	 */
	public void updateDisplay() {
		// let everybody know
		Iterator i = listeners.iterator();
		while(i.hasNext()) {
			TableModelListener l = (TableModelListener)i.next();	

			l.tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
		}
	}

	/**
	 * Tells us what *class* of object to expect in columns. We can safely expect Strings.
	 * I don't think the world is ready for transferable Sequences just yet ...
	 */
	public Class getColumnClass(int columnIndex) {
		return String.class;
	}

	/**
	 * Gets the number of columns.
	 */
	public int getColumnCount() {
		return seqGrid.getColumns().size() + 1; 
	}
	
	/**
	 * Gets the number of rows.
	 */
	public int getRowCount() {
		return seqGrid.getSequenceNames().size() + 1;
	}

	/**
	 * Gets the name of column number 'columnIndex'.
	 */
        public String getColumnName(int columnIndex) {
		return seqGrid.getColumns().get(columnIndex);
	}

	/**
	 * Gets the value at a particular column. The important
	 * thing here is that two areas are 'special':
	 * 1.	Row 0 is reserved for the column names.
	 * 2.	Column 0 is reserved for the row names.
	 * 3.	(0, 0) is to be a blank box (new String("")).
	 */
        public Object getValueAt(int rowIndex, int columnIndex) {
		if(rowIndex == 0) {
			// is it the empty box in the upper left corner?
			if(columnIndex == 0)
				return new String("");
			
			// it's row 0: the column names
			return seqGrid.getColumns().get(columnIndex - 1);
		}

		if(colIndex == 0) {
			// can't be the empty box: we've already got that.

			return seqGrid.getSequenceNames().get(rowIndex - 1);
		}

		String seqName 	= seqGrid.getSequenceNames().get(rowIndex - 1);
		int setNo 	= colIndex - 1;
		Sequence seq 	= seqGrid.getSequence(seqName, setNo);

		// is it perhaps not defined for this column?
		if(seq == null)
			return "(N/A)";	

		return seq.getActualSize() + " bp";
	}

	/**
	 * Determines if you can edit anything. Right now, no, you can't.
	 * But soon. Very soon.
	 */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	/** Allows the user to set the value of a particular cell. This, too, will happen.
	 */
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		return;
	}

	//
	// X. 	THE TABLE MODEL LISTENER SYSTEM. We use this to let people know
	// 	we've changed. When we change.
	//
	public void addTableModelListener(TableModelListener l) {
		listeners.add(l);
	}
	
	public void removeTableModelListener(TableModelListener l) {
		listeners.remove(l);
	}	
}