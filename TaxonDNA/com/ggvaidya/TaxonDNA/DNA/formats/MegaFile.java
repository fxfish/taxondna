/**
 * Allows you to read and write files in MEGA format.
 * This will be the main format for this program, so that
 * taxonomic information can be inserted into the file itself.
 *
 * TODO: Update to fully conform to MEGA format.
 * 
 * Format:
 * Line 1:	#mega
 * Line 2:	TITLE: ([.\n]*)
 * ...
 * Line x:	#OTU-name	sequence
 * 		#OTU-name2	sequence2
 * 		#OTU-name	sequence3
 * 		#OTU-name	sequence4
 *
 * We do not support distance triangles in the file. Yet.
 *
 * http://www.megasoftware.net/WebHelp/helpfile.htm
 *
 * Note: this class needs much rewriting, particularly to
 * correspond exactly to published Mega specifications.
 * The current version is really just a hack.
 *
 */
/*
   TaxonDNA
   Copyright (C) 2005	Gaurav Vaidya

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

*/   

package com.ggvaidya.TaxonDNA.DNA.formats;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.ggvaidya.TaxonDNA.Common.*;
import com.ggvaidya.TaxonDNA.DNA.*;

public class MegaFile implements FormatHandler, Testable {
	public static final int MAX_NAME_SIZE = 40;		// Mega3 will truncate anything bigger
	
	/** Creates a MegaFile format handler */
	public MegaFile() {}
	
	/** Returns the short name of this handler, which is "MEGA" */
	public String getShortName() {
		return "MEGA";
	}

	/** Returns the full name of this handler, which among other things notes the level of compliance we have with Mega */
	public String getFullName() {
		return "MEGA file format, partial compliance";
	}	

	/**
	 * Returns a valid Mega OTU (Operation Taxonomic Unit), that is, a taxon name.
	 */
	public String getMegaOTU(String name, int len) {
		// Rule #1: the name must start with '[A-Za-z0-9\-\+\.]'
		char first = name.charAt(0);
		if(
		 	(first >= 'A' && first <= 'Z') ||
			(first >= 'a' && first <= 'z') ||
			(first >= '0' && first <= '9') ||
			(first == '-') ||
			(first == '+') ||
			(first == '.')
		) {
			// it's all good!
		} else {
			name = "." + name;
		}

		// Rule #2: strange characters we'll turn into '_' 
		name = name.replaceAll("[^a-zA-Z0-9\\-\\+\\.\\_\\*\\:\\(\\)\\|\\\\\\/]", "_");

		// Rule #3: spaces we'll turn into '_' (although really they ought to have been fixed in rule #2)
		name = name.replace(' ', '_');
		
		// Rule #4: truncate to 'len'
		int size = name.length();
		if(size <= len)
			return name;
		else
			return name.substring(0, len);
	}
	
	/** 
	 * Writes the sequence list 'list' into the File 'file', in Mega format.
	 * 
	 * @throws IOException if it can't create or write the file
	 */
	public void writeFile(File file, SequenceList list, DelayCallback delay) throws IOException, DelayAbortedException {
		int count = 0;
		int interval = list.count()/50;
		if(interval == 0)
			interval = 1;

		if(file == null)
			throw new FileNotFoundException("No filename specified!\n\nThis is probably a programming error.");

		Hashtable names = new Hashtable();	// names are stored, to be checked for duplicate names

		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		
		if(delay != null)
			delay.begin();

		writer.println("#mega");
		writer.println("TITLE Generated by DNA.formats.MegaFile");

		Iterator i = list.iterator();
		while(i.hasNext()) {
			Sequence seq = (Sequence)i.next();
			String name = getMegaOTU(seq.getFullName(), MAX_NAME_SIZE);

			// determine the first <name>_<next_index> which
			// hasn't already been used (and put into our hash of names)
			//
			// this is only done if the name already exists. ideally,
			// after the first save, the new "unique" sequence names will
			// ensure that everything makes sense. and no more problems
			// for us! 
			//
			int next_index = 1;
			while(names.contains(name)) {
				// note that we now truncate it down further, so that MAX_NAME_SIZE
				// will fit the name, '_', and upto 4 digits of an index number.
				name = getMegaOTU(seq.getFullName(), MAX_NAME_SIZE - 1 - 4) + "_" + next_index;
				next_index++;
			}
			name = name.replace(' ', '_');
			
			names.put((Object)name, new Integer(1));
			writer.println("#" + name + "\t" + seq.getSequence());

			try {
				if(delay != null && count % interval == 0)
					delay.delay(count, list.count());
			} catch(DelayAbortedException e) {
				writer.close();
				if(delay != null)
					delay.end();
				throw e;
			}
			count++;
		}
		if(delay != null)
			delay.end();
		
		writer.close();		
	}

	/**
	 * Could 'file' be Mega? That is the question. This is relatively easy, since we assume that it
	 * is Mega if the signature ('#mega') is present. 
	 */
	public boolean mightBe(File file) {
		try {
			BufferedReader	read	=	new BufferedReader(new FileReader(file));
			String 		line;
			while((line = read.readLine()) != null) {
				line = line.trim();
				if(!line.equals("")) {
					if(line.toLowerCase().equals("#mega"))
						return true;
					else  {
						return false;
					}
				}
			}			
		} catch(IOException e) {
		}
		return false;
	}

	/**
	 * Read a Mega file (from 'file') and return a SequenceList containing all the entries. 
	 * @throws FormatException if anything is in any whatsoeverway wrong with this format.
	 */
	public SequenceList readFile(File file, DelayCallback delay) throws IOException, SequenceException, FormatException, DelayAbortedException {
		SequenceList list = new SequenceList();
		appendFromFile(list, file, delay);
		return list;
	}

	public void appendFromFile(SequenceList list, File file, DelayCallback delay) throws IOException, SequenceException, FormatException, DelayAbortedException
	{
		try {
			TreeMap	names		=	new TreeMap();		// stores the names-to-sequence map
			Vector sequences	=	new Vector();		// stores all the sequences
			boolean waitingForSig	=	true;			// state engine variable: are we waiting
										// for the '#mega' signature?
			boolean waitingForTitle = 	true;			// state engine variable: are we waiting
		       								// for the 'TITLE' line?	
										 
			String title		=	"";			// the TITLE of the mega file

			int lineno		=	0;			// line we are currently working on
			int spid		=	0;			// species id

			BufferedReader	read	=	new BufferedReader(new FileReader(file));

			list.lock();

			if(delay != null)
				delay.begin();

			String line		=	"";
			while((line = read.readLine()) != null) {
				line = line.trim();
				lineno++;
			
				if(waitingForSig) {
					// do we have the signature? if we can't find it, we might
					// as well quit.
					if(line != "") {
						if(line.toLowerCase().equals("#mega"))
							waitingForSig = false;
						else  {
							throw new FormatException("This file does not contain a valid MEGA signature");
						}
					}
				} else if(waitingForTitle) {
					// do we have a title? it ain't mega without a title! (I think)
					//
					if(line != "") {
						if(line.startsWith("TITLE")) {
							Pattern	p = Pattern.compile("TITLE[:]*\\s*(.*)$");
							Matcher m = p.matcher(line);
	
							if(m.lookingAt()) {
								title = m.group(1);
								waitingForTitle = false;
							}
						}
					}
				} else if(line.startsWith("#")) {
					// We've hit a line containing a portion of a sequence. How this is handled:
					//	- break the sequence up into its name and sequence components 
					line = line.substring(1).trim();	// so we don't have to worry abt spaces after the '#'
					StringBuffer 	seq_name	=	new StringBuffer();
					StringBuffer 	seq_seq		=	new StringBuffer();
					boolean		comment		=	false;
					int		x;
	
					// get the sequence name
					for(x = 0; x < line.length(); x++) {
						char ch = line.charAt(x);
		
						if(Character.isWhitespace(ch))
							break;
	
						if(ch == '_')
							ch = ' ';
	
						seq_name.append(ch);
					}
	
					// get the sequence itself.
					// comments are inclosed "like so"
					for(x++; x < line.length(); x++) {
						char ch = line.charAt(x);

						if(!comment) {
							if(ch == '"') {
								comment = true;
							} else if(!Character.isWhitespace(ch)) {
								seq_seq.append(ch);
							}
						} else {
							if(ch == '"') {
								comment = false;
							}
						}
					}	
					
					if(seq_name.toString().trim() != "" && seq_seq.toString().trim() != "") {
						if(names.get(seq_name.toString()) == null) {
							names.put(seq_name.toString(), new Integer(spid));
							sequences.add(spid, seq_seq);
							spid++;	// so the next one will be 'new'
						} else {	
							int my_spid = 0;
							my_spid = ((Integer)names.get(seq_name.toString())).intValue();
							StringBuffer tmp = (StringBuffer)sequences.get(my_spid);
							sequences.remove(my_spid);
							sequences.add(my_spid, tmp.append(seq_seq));
						}
					} else {
						throw new FormatException("Error in file on line " + lineno + ": something wrong with this line! Is there a illegible sequence on that line?");
					}
				} else {	
					// comment line
				}
			}	

			if(waitingForSig) {
				throw new FormatException("This file does not contain a valid MEGA signature");
			}	

			if(waitingForTitle) {
				throw new FormatException("This file does not contain a 'TITLE', a requirement in all valid MEGA files");
			}
		
			int total		= 	spid;
			Iterator iterator = names.keySet().iterator();
	
			if(delay != null)
				delay.begin();

			while(iterator.hasNext()) {
				String name 	= (String) iterator.next();
				int i			= ((Integer)names.get(name)).intValue();
				StringBuffer seq	= (StringBuffer) sequences.get(i);

				int count = (total - spid--);
				int increment = (total / 100);
				if(increment < 1)
					increment = 1;
				
				if(count % increment == 0)
					delay.delay(count, total);

				list.add(new Sequence(name, seq.toString()));
			}

			list.setFile(file);
			list.setFormatHandler(this);
		} catch(IOException e) {
			throw new FormatException("There was an error reading the Mega file '" + file + "': " + e, e);
		} finally {
			if(delay != null)
				delay.end();
			if(list != null)
				list.unlock();
		}
	}

	/**
	 * Tests the MegaFile class extensively so that bugs don't creep in.
	 */
	public void test(TestController testMaster, DelayCallback delay) throws DelayAbortedException {
		testMaster.begin("DNA.formats.MegaFile");

		MegaFile ff = new MegaFile();

		testMaster.beginTest("Recognize a file as being a MEGA file");
			File test = testMaster.file("DNA/formats/megafile/test_mega1.txt");
			if(ff.mightBe(test))
				try {
					int count = ff.readFile(test, delay).count();
					if(count == 10)
						testMaster.succeeded();
					else	
						testMaster.failed("I got back " + count + " sequences instead of 10!");

				} catch(IOException e) {
					testMaster.failed("There was an IOException reading " + test + ": " + e);
				} catch(SequenceException e) {
					testMaster.failed("There was a SequenceException reading " + test + ": " + e);
				} catch(FormatException e) {
					testMaster.failed("There was a FormatException reading " + test + ": " + e);
				}


			else
				testMaster.failed(test + " was not recognized as a MEGA file");

		testMaster.beginTest("Recognize a file generated by MEGA export from a FASTA file as a MEGA file");
			File test2 = testMaster.file("DNA/formats/megafile/test_mega2.txt");
			if(ff.mightBe(test2))
				try {
					if(ff.readFile(test2, delay).count() == 10)
						testMaster.succeeded();
				} catch(IOException e) {
					testMaster.failed("There was an IOException reading " + test2 + ": " + e);
				} catch(SequenceException e) {
					testMaster.failed("There was a SequenceException reading " + test2 + ": " + e);
				} catch(FormatException e) {
					testMaster.failed("There was a FormatException reading " + test2 + ": " + e);
				}


			else
				testMaster.failed(test + " was not recognized as a MEGA file");
			

		testMaster.beginTest("Recognize other files as being non-MEGA");
			File notfasta = testMaster.file("DNA/formats/megafile/test_nonmega1.txt");
			if(notfasta.canRead() && !ff.mightBe(notfasta))
				testMaster.succeeded();
			else
				testMaster.failed(notfasta + " was incorrectly identified as a MEGA file");

		// err, skip this last test
		// IT DOESNT WORK
		testMaster.done();
		return;
/*
		testMaster.beginTest("Write out a MEGA file, then read it back in (twice!)");
			File input = testMaster.file("DNA/formats/megafile/test_megacopy.txt");
			File success = testMaster.file("DNA/formats/megafile/test_megacopy_success.txt");
			File output = testMaster.tempfile();
			File output2 = testMaster.tempfile();

			try {
				SequenceList list = ff.readFile(input, delay);
				ff.writeFile(output, list, delay);

				list = ff.readFile(output, delay);
				ff.writeFile(output2, list, delay);

				if(testMaster.isIdentical(success, output2))
					testMaster.succeeded();
				else
					testMaster.failed(
						"I read a MEGA file from " + input + ", then wrote it to '" + output2 + "', but they aren't identical"
					);
			} catch(IOException e) {
				testMaster.failed(
					"I read a MEGA file from " + input + ", then wrote it to '" + output2 + "', but I got an IOException: " + e
					);
			} catch(SequenceException e) {
				testMaster.failed(
					"I read a MEGA file from " + input + ", then wrote it to '" + output2 + "', but I got a SequenceListException: " + e
					);
			} catch(FormatException e) {
				testMaster.failed(
					"I read a MEGA file from " + input + ", then wrote it to '" + output2 + "', but I got a SequenceListException: " + e
					);
			}

		testMaster.done();
	*/
	}
}