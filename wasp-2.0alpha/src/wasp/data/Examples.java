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
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import wasp.main.Config;
import wasp.main.Parse;
import wasp.mrl.Production;
import wasp.nl.NL;
import wasp.util.Arrays;
import wasp.util.ComparablePair;
import wasp.util.Double;
import wasp.util.FileWriter;
import wasp.util.Int;
import wasp.util.Pair;

/**
 * A set of examples, i.e. a corpus.
 * 
 * @author ywwong
 *
 */
public class Examples {

	private static Logger logger = Logger.getLogger(Examples.class.getName());
	
	private class ExampleIterator implements Iterator {
		private Iterator it;
		public ExampleIterator() {
			it = map.values().iterator();
		}
		public boolean hasNext() {
			return it.hasNext();
		}
		public Object next() {
			return it.next();
		}
		public void remove() {
			it.remove();
			ids = null;
		}
	}
	
	private TreeMap map;
	private Int[] ids;

	public Examples() {
		map = new TreeMap();
		ids = null;
	}
	
	public Iterator iterator() {
		return new ExampleIterator();
	}
	
	public Example get(int id) {
		return (Example) map.get(new Int(id));
	}
	
	public Example getNth(int n) {
		if (ids == null)
			ids = (Int[]) map.keySet().toArray(new Int[0]);
		return (Example) map.get(ids[n]);
	}
	
	public int size() {
		return map.size();
	}
	
	public void add(Example ex) {
		map.put(new Int(ex.id), ex);
		ids = null;
	}
	
	public Examples[] split(double first) {
		if (ids == null)
			ids = (Int[]) map.keySet().toArray(new Int[0]);
		Int[] rand = (Int[]) Arrays.randomSort(ids);
		ExampleMask mask1 = new ExampleMask();
		ExampleMask mask2 = new ExampleMask();
		int mid = (int) (ids.length*first);
		for (int i = 0; i < mid; ++i)
			mask1.add(rand[i].val);
		for (int i = mid; i < ids.length; ++i)
			mask2.add(rand[i].val);
		Examples[] split = new Examples[2];
		split[0] = mask1.apply(this);
		split[1] = mask2.apply(this);
		return split;
	}
	
	public Examples[][] crossValidate(int k) {
		if (ids == null)
			ids = (Int[]) map.keySet().toArray(new Int[0]);
		Int[] rand = (Int[]) Arrays.randomSort(ids);
		Examples[][] splits = new Examples[k][2];
		for (int i = 0; i < k; ++i) {
			int from = ids.length*i/k;
			int to = ids.length*(i+1)/k;
			ExampleMask trainMask = new ExampleMask();
			ExampleMask testMask = new ExampleMask();
			for (int j = 0; j < from; ++j)
				trainMask.add(rand[j].val);
			for (int j = from; j < to; ++j)
				testMask.add(rand[j].val);
			for (int j = to; j < ids.length; ++j)
				trainMask.add(rand[j].val);
			splits[i][0] = trainMask.apply(this);
			splits[i][1] = testMask.apply(this);
		}
		return splits;
	}
	
	///
	/// File I/O
	///
	
	private static boolean addMRLParse = false;
	
	public void read(String filename)
	throws IOException, SAXException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		SAXParser parser = factory.newSAXParser();
		parser.parse(new File(filename), new ExampleHandler());
	}

	private class ExampleHandler extends DefaultHandler {
		private Example ex;
		private String lang;
		private String mrl;
		private int nodeId;
		private double score;
		private ArrayList slist;
		private ArrayList plist;
		private StringBuffer buf;
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if (qName.equalsIgnoreCase("example")) {
				ex = new Example();
				ex.id = Int.parseInt(attributes.getValue("id"));
				logger.finest("example "+ex.id);
			} else if (qName.equalsIgnoreCase("nl")) {
				lang = attributes.getValue("lang");
				buf = new StringBuffer();
				logger.finest("nl "+lang);
			} else if (qName.equalsIgnoreCase("syn")) {
				lang = attributes.getValue("lang");
				buf = new StringBuffer();
				logger.finest("syn "+lang);
			} else if (qName.equalsIgnoreCase("augsyn")) {
				lang = attributes.getValue("lang");
				mrl = attributes.getValue("mrl");
				buf = new StringBuffer();
				logger.finest("augsyn "+lang);
			} else if (qName.equalsIgnoreCase("mrl")) {
				lang = attributes.getValue("lang");
				buf = new StringBuffer();
				logger.finest("mrl "+lang);
			} else if (qName.equalsIgnoreCase("mrl-parse")) {
				lang = attributes.getValue("lang");
				slist = new ArrayList();
				plist = new ArrayList();
				logger.finest("mrl-parse "+lang);
			} else if (qName.equalsIgnoreCase("node")) {
				nodeId = Int.parseInt(attributes.getValue("id"));
				buf = new StringBuffer();
				logger.finest("node "+nodeId);
			} else if (qName.equalsIgnoreCase("parse")) {
				String attr = attributes.getValue("score");
				score = (attr==null) ? 0 : Double.parseDouble(attr);
				buf = new StringBuffer();
				logger.finest("parse");
			}
		}
		public void characters(char[] text, int start, int length) {
			if (buf != null)
				buf.append(text, start, length);
		}
		public void endElement(String uri, String localName, String qName) {
			if (qName.equalsIgnoreCase("nl")) {
				String nl = buf.toString().trim();
				ex.nlMap.put(lang, nl);
				ex.E.put(lang, new NL().tokenize(nl));
			} else if (qName.equalsIgnoreCase("syn"))
				ex.synMap.put(lang, buf.toString().trim());
			else if (qName.equalsIgnoreCase("augsyn"))
				ex.augsynMap.put(new ComparablePair(lang, mrl), buf.toString().trim());
			else if (qName.equalsIgnoreCase("mrl")) {
				String mrl = buf.toString().trim();
				ex.mrlMap.put(lang, mrl);
				if (lang.equals(Config.getMRL())) {
					ex.F = new Meaning(mrl);
					if (ex.F.parse == null)
						logger.warning("example "+ex.id+": MR is not grammatical");
					if (addMRLParse) {
						ArrayList slist = new ArrayList();
						for (short i = 0; i < ex.F.lprods.length; ++i)
							slist.add(ex.F.lprods[i].toString());
						ex.mrlparseMap.put(lang, slist);
					}
				}
			} else if (qName.equalsIgnoreCase("node")) {
				String s = buf.toString().trim();
				slist.add(s);
				if (lang.equals(Config.getMRL())) {
					String[] line = Arrays.tokenize(s);
					Int index = new Int(0);
					Production.readOrig = false;
					Production prod = Production.read(line, index).intern();
					if (index.val == line.length)
						plist.add(prod);
					else
						logger.warning("example "+ex.id+": node "+nodeId+" of MR parse is invalid");
				}
			} else if (qName.equalsIgnoreCase("mrl-parse")) {
				ex.mrlparseMap.put(lang, slist);
				if (lang.equals(Config.getMRL()))
					ex.Fparse = plist;
			} else if (qName.equalsIgnoreCase("parse"))
				ex.parses.add(new Parse(buf.toString().trim(), score));
			else if (qName.equalsIgnoreCase("example")) {
				if (ex.F == null || ex.F.parse != null)
					add(ex);
			}
		}
	}

	public static boolean writeSyn = false;
	public static boolean writeAugsyn = false;
	public static boolean writeMRL = false;
	public static boolean writeMRLParse = false;
	public static boolean writeParses = true;

	public void write(String filename) throws IOException {
		PrintWriter out = new PrintWriter(new BufferedWriter(FileWriter.createNew(filename)));
		writeHeader(out);
		for (Iterator it = iterator(); it.hasNext();)
			write(out, (Example) it.next());
		writeFooter(out);
		out.close();
	}
	
	private static void writeHeader(PrintWriter out) throws IOException {
		out.println("<?xml version=\"1.0\"?>");
		out.println("<!DOCTYPE examples [");
		out.println("  <!ELEMENT examples (example*)>");
		out.println("  <!ELEMENT example (nl*,syn*,augsyn*,mrl*,mrl-parse*,parse*)>");
		out.println("  <!ELEMENT nl (#PCDATA)>");
		out.println("  <!ELEMENT syn (#PCDATA)>");
		out.println("  <!ELEMENT augsyn (#PCDATA)>");
		out.println("  <!ELEMENT mrl (#PCDATA)>");
		out.println("  <!ELEMENT mrl-parse (node*)>");
		out.println("  <!ELEMENT node (#PCDATA)>");
		out.println("  <!ELEMENT parse (#PCDATA)>");
		out.println("]>");
		out.println("<examples>");
		out.println();
	}
	
	private static void write(PrintWriter out, Example ex) throws IOException {
		out.println("<example id=\""+ex.id+"\">");
		for (Iterator it = ex.nlMap.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String lang = (String) entry.getKey();
			String nl = (String) entry.getValue();
			out.println("<nl lang=\""+lang+"\">");
			out.println(nl);
			out.println("</nl>");
		}
		if (writeSyn)
			for (Iterator it = ex.synMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String lang = (String) entry.getKey();
				String nl = (String) entry.getValue();
				out.println("<syn lang=\""+lang+"\">");
				out.println(nl);
				out.println("</syn>");
			}
		if (writeAugsyn)
			for (Iterator it = ex.augsynMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				Pair pair = (Pair) entry.getKey();
				String lang = (String) pair.first;
				String mrl = (String) pair.second;
				String nl = (String) entry.getValue();
				out.println("<augsyn lang=\""+lang+"\" mrl=\""+mrl+"\">");
				out.println(nl);
				out.println("</augsyn>");
			}
		if (writeMRL)
			for (Iterator it = ex.mrlMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String lang = (String) entry.getKey();
				String mrl = (String) entry.getValue();
				out.println("<mrl lang=\""+lang+"\">");
				out.println(mrl);
				out.println("</mrl>");
			}
		if (writeMRLParse)
			for (Iterator it = ex.mrlparseMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				String lang = (String) entry.getKey();
				ArrayList mrlparse = (ArrayList) entry.getValue();
				out.println("<mrl-parse lang=\""+lang+"\">");
				for (short i = 0; i < mrlparse.size(); ++i) {
					out.print("<node id=\""+i+"\"> ");
					out.print(mrlparse.get(i));
					out.println(" </node>");
				}
				out.println("</mrl-parse>");
			}
		if (writeParses) {
			Parse[] parses = ex.getSortedParses();
			String nl = NL.useNL;
			String mrl = Config.getMRL();
			for (int j = 0; j < parses.length; ++j) {
				String str = parses[j].toStr();
				if (str == null) {
					logger.finer("example "+ex.id+" parse "+j+" is bad");
					continue;
				}
				logger.finer("example "+ex.id+" parse "+j);
				out.println("<parse nl=\""+nl+"\" mrl=\""+mrl+"\" rank=\""+j+"\" score=\""+parses[j].score+"\">");
				out.println(str);
				out.println("</parse>");
				StringBuffer sb = new StringBuffer();
				if (parses[j].comment != null)
					sb.append(parses[j].comment);
				Node tree = parses[j].toTree();
				if (tree != null) {
					sb.append(tree.toPrettyString());
					sb.append("\n");
				}
				if (sb.length() > 0) {
					out.println("<!--");
					out.print(sb.toString().replaceAll("--", "=="));
					out.println("-->");
				}
			}
		}
		out.println("</example>");
		out.println();
	}
	
	private static void writeFooter(PrintWriter out) throws IOException {
		out.println("</examples>");
	}
	
	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		if (args.length != 3) {
			System.err.println("Usage: java wasp.data.Examples config-file in-corpus-file out-corpus-file");
			System.err.println();
			System.err.println("config-file - the configuration file that contains the current settings.");
			System.err.println("in-corpus-file - the file that contains the entire corpus for training and testing.");
			System.err.println("out-corpus-file - a copy of in-corpus-file, but with possibly more complete annotation.");
			System.exit(1);
		}
		String configFilename = args[0];
		String inFilename = args[1];
		String outFilename = args[2];
		
		addMRLParse = true;
		writeSyn = true;
		writeAugsyn = true;
		writeMRL = true;
		writeMRLParse = true;
		writeParses = false;
		Config.read(configFilename);
		Examples examples = new Examples();
		examples.read(inFilename);
		examples.write(outFilename);
	}
	
}
