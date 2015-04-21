/*
 * Copyright 2006, 2007 Yuk Wah Wong.
 * 
 * This file is part of the WASP distribution.
 *
 * WASP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * WASP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with WASP; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package wasp.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.TreeSet;

import wasp.util.FileWriter;
import wasp.util.Int;

/**
 * Masks for accessing subsets of a corpus.
 * 
 * @author ywwong
 *
 */
public class ExampleMask {

	private TreeSet mask;
	
	public ExampleMask() {
		mask = new TreeSet();
	}
	
	public Examples apply(Examples examples) {
		Examples subset = new Examples();
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			if (mask.contains(new Int(ex.id)))
				subset.add((Example) ex.copy());
		}
		return subset;
	}
	
	public void add(int id) {
		mask.add(new Int(id));
	}
	
	public void read(String filename) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line;
		while ((line = in.readLine()) != null)
			add(Int.parseInt(line));
		in.close();
	}
	
	public void write(String filename) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(filename)));
		for (Iterator it = mask.iterator(); it.hasNext();)
			out.println(it.next());
		out.close();
	}
	
}
