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
 * The class for abstract corpus reader.  Currently only one implementation is available: the XML file
 * reader.
 * 
 * @author ywwong
 * 
 */
public class CorpusReader {

	public static CorpusReader createNew() {
		return new CorpusReader();
	}
	
	/**
	 * Reads the corpus of the current domain and returns labeled examples.
	 * 
	 * @return a set of examples from the given corpus.
	 * @throws IOException if an I/O error occurs.
	 */
	public Examples readCorpus() throws IOException, SAXException, ParserConfigurationException {
		Examples examples = new Examples();
		examples.read(Config.get(Config.CORPUS_FILE));
		return examples;
	}
	
}
