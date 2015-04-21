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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import wasp.main.Config;

/**
 * A small program for printing subsets of a corpus.
 * 
 * @author ywwong
 *
 */
public class PrintExamplesWithIds {

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args.length != 4) {
			System.err.println("Usage: java wasp.data.PrintExamplesWithIds config-file in-xml-file in-id-file out-xml-file");
			System.err.println();
			System.err.println("config-file - the configuration file.");
			System.err.println("in-xml-file - the input XML file containing examples.");
			System.err.println("in-id-file - the input list of example IDs.");
			System.err.println("out-xml-file - a subset of the examples whose IDs are in the input list.");
			System.exit(1);
		}
		String configFilename = args[0];
		String inFilename = args[1];
		String inIdFilename = args[2];
		String outFilename = args[3];
		
		Config.read(configFilename);
		Examples examples = new Examples();
		examples.read(inFilename);
		ExampleMask mask = new ExampleMask();
		mask.read(inIdFilename);
		examples = mask.apply(examples);
		examples.write(outFilename);
	}
	
}
