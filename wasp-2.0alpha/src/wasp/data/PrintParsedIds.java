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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import wasp.main.Config;
import wasp.util.FileWriter;

/**
 * A small program for printing the IDs of examples in a corpus with &lt;parse&gt; fields.
 * 
 * @author ywwong
 *
 */
public class PrintParsedIds {

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args.length != 3) {
			System.err.println("Usage: java wasp.data.PrintParsedIds config-file in-xml-file out-id-file");
			System.err.println();
			System.err.println("config-file - the configuration file.");
			System.err.println("in-xml-file - the input XML file containing examples.");
			System.err.println("out-id-file - a file for storing the IDs of the examples with <parse> fields.");
			System.exit(1);
		}
		String configFilename = args[0];
		String inFilename = args[1];
		String outIdFilename = args[2];
		
		Config.read(configFilename);
		Examples examples = new Examples();
		examples.read(inFilename);
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(outIdFilename)));
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			if (!ex.parses.isEmpty())
				out.println(ex.id);
		}
		out.close();
	}
	
}
