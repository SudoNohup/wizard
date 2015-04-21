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
package wasp.scfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import wasp.align.Link;
import wasp.align.NTo1WordAlign;
import wasp.align.WordAlign;
import wasp.data.Anaphor;
import wasp.data.Example;
import wasp.data.Examples;
import wasp.data.Node;
import wasp.data.Nonterminal;
import wasp.data.Symbol;
import wasp.data.Terminal;
import wasp.data.Variable;
import wasp.data.VariableAssignment;
import wasp.main.Config;
import wasp.math.Math;
import wasp.mrl.Production;
import wasp.mrl.ProductionSymbol;
import wasp.nl.BasicGapModel;
import wasp.nl.GapFiller;
import wasp.nl.GapModel;
import wasp.nl.RelativeFreq;
import wasp.util.Arrays;
import wasp.util.Bool;
import wasp.util.Double;
import wasp.util.Matrices;
import wasp.util.Short;
import wasp.util.ShortSymbol;

/**
 * Code for extracting SCFG rules or lambda-SCFG rules from word-aligned sentence pairs.
 * 
 * @author ywwong
 *
 */
public class RuleExtractor {

	private static Logger logger = Logger.getLogger(RuleExtractor.class.getName());
	static {
		logger.setLevel(Level.FINER);
	}
	
	/** The minimum number of words that an extracted rule must have if it is not unary.  This number
	 * should be greater than zero. */
	private static final short MIN_WORD_COUNT = 1;
	/** If true, then a right-branching structure will be imposed when constructing a "deep" MR parse,
	 * bypassing the use of word alignments. */
	private static final boolean DO_RIGHT_BRANCHING = false;
	
	/** The maximum number of tokens in the MRL string. */
	private short MAX_LENGTH;
	/** The maximum number of non-terminals on the RHS. */
	private short MAX_ARITY;
	/** Indicates if word gaps are allowed in the extracted NL phrases. */
	private boolean ALLOW_GAPS;
	
	private static class Lex extends RelativeFreq.Event {
		private Symbol sym;
		public Lex(Symbol sym) {
			this.sym = sym;
		}
		private Lex(Symbol sym, double freq) {
			this.sym = sym;
			this.freq = freq;
		}
		public Object copy() {
			return new Lex(sym, freq);
		}
		public boolean equals(Object o) {
			return o instanceof Lex && ((sym==null) ? ((Lex) o).sym==null : sym.equals(((Lex) o).sym));
		}
		public int hashCode() {
			return (sym==null) ? 0 : sym.hashCode();
		}
	}
	
	private static class PhrE extends RelativeFreq.Event {
		private Symbol[] E;
		private short[] gaps;
		public PhrE(Symbol[] E, short[] gaps) {
			this.E = E;
			this.gaps = gaps;
		}
		public PhrE(Symbol[] E) {
			this.E = E;
			gaps = new short[E.length+1];
		}
		private PhrE(Symbol[] E, short[] gaps, double freq) {
			this.E = E;
			this.gaps = gaps;
			this.freq = freq;
		}
		public Object copy() {
			return new PhrE(E, gaps, freq);
		}
		public boolean equals(Object o) {
			return o instanceof PhrE && Arrays.equal(E, ((PhrE) o).E)
			&& Arrays.equal(gaps, ((PhrE) o).gaps);
		}
		public int hashCode() {
			int hash = 1;
			hash = 31*hash + Arrays.hashCode(E);
			hash = 31*hash + Arrays.hashCode(gaps);
			return hash;
		}
	}
	
	private static class PhrF extends RelativeFreq.Event {
		private Symbol[] F;
		public PhrF(Symbol[] F) {
			this.F = F;
		}
		private PhrF(Symbol[] F, double freq) {
			this.F = F;
			this.freq = freq;
		}
		public Object copy() {
			return new PhrF(F, freq);
		}
		public boolean equals(Object o) {
			return o instanceof PhrF && ((F==null) ? ((PhrF) o).F==null : Arrays.equal(F, ((PhrF) o).F));
		}
		public int hashCode() {
			return (F==null) ? 0 : Arrays.hashCode(F);
		}
	}
	
	private static class Span {
		public Nonterminal lhs;
		public short from;
		public short to;
		public Span(Nonterminal lhs) {
			this.lhs = lhs;
			from = Short.MAX_VALUE;
			to = 0;
		}
		public void update(short e) {
			if (from > e)
				from = e;
			if (to < e+1)
				to = (short) (e+1);
		}
		public void update(Span s) {
			if (from > s.from)
				from = s.from;
			if (to < s.to)
				to = s.to;
		}
	}
	
	// for calculating relative frequencies
	private RelativeFreq lexFE;
	private RelativeFreq lexEF;
	private RelativeFreq phrFE;
	private RelativeFreq phrEF;
	
	// for detecting infinite loops
	private boolean[][] dep;
	
	public RuleExtractor() {
		MAX_LENGTH = Short.parseShort(Config.get(Config.SCFG_MAX_MR_LENGTH));
		ALLOW_GAPS = Bool.parseBool(Config.get(Config.SCFG_ALLOW_GAPS));
		String s = Config.get(Config.SCFG_MAX_ARITY);
		if (s == null)
			MAX_ARITY = Short.MAX_VALUE;
		else
			MAX_ARITY = Short.parseShort(s);
	}
	
	/**
	 * Extracts rules from the specified examples.  The extracted rules are added to the specified SCFG.
	 * This method requires word alignments, which should be in the <code>aligns</code> field of the
	 * examples.  These word alignments can be obtained using a <code>WordAlignModel</code>.
	 *
	 * @param gram an SCFG.
	 * @param gm a word-gap model.
	 * @param examples a set of training examples that have been through a word alignment model.
	 * @throws RuntimeException if rule extraction fails.
	 */
	public void extract(SCFG gram, GapModel gm, Examples examples) {
		logger.info("Extracting SCFG rules from word alignments");
		initDep(gram);
		initFreq();
		addTopLinks(examples);
		countLex(examples);
		setLexWeights(examples);
		transformAC(examples);
		bindVars(examples);
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			logger.fine("example "+ex.id);
			WordAlign[] aligns = (WordAlign[]) ex.getSortedAligns();
			for (int j = 0; j < aligns.length; ++j) {
				logger.fine("alignment "+j);
				extract(gram, gm, (NTo1WordAlign) aligns[j]);
			}
		}
		countPhr(gram, gm);
		logger.info("SCFG rules have been extracted");
	}

	private void initDep(SCFG gram) {
		int nlhs = gram.countNonterms();
		dep = new boolean[nlhs][nlhs];
		Rule[] rules = gram.getRules();
		for (int i = 0; i < rules.length; ++i)
			if (rules[i].lengthE() == 1) {
				Symbol sym = rules[i].getE((short) 0);
				if (sym instanceof Nonterminal)
					dep[rules[i].getLhs().getId()][sym.getId()] = true;
			}
		// sanity check
		if (loopy()) {
			logger.severe("The initial rules would cause infinite loops during parsing");
			throw new RuntimeException();
		}
	}
	
	private boolean loopy() {
		boolean[][] depTrans = Matrices.transitive(dep);
		for (int i = 0; i < depTrans.length; ++i)
			if (depTrans[i][i])
				return true;
		return false;
	}
	
	private void initFreq() {
		lexFE = new RelativeFreq();
		lexEF = new RelativeFreq();
		phrFE = new RelativeFreq();
		phrEF = new RelativeFreq();
	}
	
	private void countLex(Examples examples) {
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			for (int j = 0; j < ex.aligns.size(); ++j) {
				NTo1WordAlign align = (NTo1WordAlign) ex.aligns.get(j);
				for (short k = 0; k < align.lengthE(); ++k) {
					Lex e = new Lex(align.getE(k));
					Link link = align.getFirstLinkFromE(k);
					Lex f;
					if (link == null)
						f = new Lex(null);
					else
						f = new Lex(removeVarIndices(align.getF(link.f).getSymbol()));
					lexFE.add(f, e);
					lexEF.add(e, f);
				}
				for (short k = 0; k < align.lengthF(); ++k)
					if (align.countLinksFromF(k) == 0) {
						Lex e = new Lex(null);
						Lex f = new Lex(removeVarIndices(align.getF(k).getSymbol()));
						lexFE.add(f, e);
						lexEF.add(e, f);
					}
			}
		}
		lexFE.normalize();
		lexEF.normalize();
	}
	
	private Symbol removeVarIndices(Symbol sym) {
		return new ProductionSymbol(new Production(((ProductionSymbol) sym).getProduction()));
	}
	
	private void setLexWeights(Examples examples) {
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			for (int j = 0; j < ex.aligns.size(); ++j) {
				NTo1WordAlign align = (NTo1WordAlign) ex.aligns.get(j);
				align.initw();
				for (short k = 0; k < align.lengthE(); ++k) {
					Lex e = new Lex(align.getE(k));
					Link link = align.getFirstLinkFromE(k);
					// if align is N-to-N, then this code has to be changed
					Lex f;
					if (link == null)
						f = new Lex(null);
					else
						f = new Lex(removeVarIndices(align.getF(link.f).getSymbol()));
					align.wE[k] = lexEF.get(e, f);
				}
				for (short k = 0; k < align.lengthF(); ++k) {
					Lex f = new Lex(removeVarIndices(align.getF(k).getSymbol()));
					Link link = align.getFirstLinkFromF(k);
					if (link == null)
						align.wF[k] = lexFE.get(f, new Lex(null));
					else {
						align.wF[k] = 0;
						while (link != null) {
							Lex e = new Lex(align.getE(link.e));
							align.wF[k] += lexFE.get(f, e);
							link = link.next;
						}
						align.wF[k] /= align.countLinksFromF(k);
					}
				}
			}
		}
	}
	
	private void addTopLinks(Examples examples) {
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			for (int j = 0; j < ex.aligns.size(); ++j) {
				WordAlign align = (WordAlign) ex.aligns.get(j);
				// add links to sentence boundaries
				short top = 0;
				while (ex.F.lprods[top].isUnary())
					++top;
				align.addLink((short) 0, top);
				align.addLink((short) (ex.E().length-1), top);
			}
		}
	}

	private void transformAC(Examples examples) {
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			//logger.finer("example "+ex.id);
			for (int j = 0; j < ex.aligns.size(); ++j) {
				WordAlign align = (WordAlign) ex.aligns.get(j);
				//logger.finer("alignment "+j);
				HashMap Fmap = new HashMap();
				Node n = transformAC((NTo1WordAlign) align, align.getF((short) 0), Fmap);
				align = align.mapF(n.getDescends(), Fmap);
				ex.aligns.set(j, align);
				//logger.finer(align.getF((short) 0).toPrettyString());
				//logger.finer(align.toString());
			}
		}
	}
	
	private Node transformAC(NTo1WordAlign align, Node node, HashMap Fmap) {
		Production prod = ((ProductionSymbol) node.getSymbol()).getProduction();
		short nc = node.countChildren();
		if (!prod.isAC()) {
			// no transformation necessary
			Node n = node.shallowCopy();
			Fmap.put(node, n);
			for (short i = 0; i < nc; ++i)
				n.addChild(transformAC(align, node.getChild(i), Fmap));
			return n;
		} else if (DO_RIGHT_BRANCHING) {
			Graph graph = new Graph(nc);
			// impose right branching
			for (short i = 0; i < nc-1; ++i)
				graph.addEdge(i, (short) (i+1), 1);
			graph.findComponents();
			graph.setTreeRoot((short) 0, (short) 0);
			graph.findMinSpanForest();
			Node mst = graph.getMinSpanTree((short) 0);
			return transformAC(align, node, mst, Fmap);
		} else {
			// add edges to graph
			Graph graph = new Graph(nc);
			for (short i = 0; i < nc; ++i) {
				Node c1 = node.getChild(i);
				for (short j = (short) (i+1); j < nc; ++j) {
					Node c2 = node.getChild(j);
					double w = edgeWeight(align, c1, c2);
					if (!Double.isInfinite(w))
						graph.addEdge(i, j, w);
				}
			}
			graph.findComponents();
			// find minimum spanning forest
			Node p = node.getParent();  // should be non-null for the Geoquery domain
			for (short i = 0; i < graph.countComponents(); ++i) {
				short[] comp = graph.getComponent(i);
				short root = -1;
				double min = Double.POSITIVE_INFINITY;
				for (short j = 0; j < comp.length; ++j) {
					double w = edgeWeight(align, p, node.getChild(comp[j]), true);
					if (min > w) {
						min = w;
						root = comp[j];
					}
				}
				if (root < 0) {
					root = comp[0];
					for (short j = 0; j < comp.length; ++j) {
						double w = edgeWeight(align, p, node.getChild(comp[j]), false);
						if (min > w) {
							min = w;
							root = comp[j];
						}
					}
				}
				graph.setTreeRoot(i, root);
			}
			graph.findMinSpanForest();
			// actual transformation
			Node n;
			if (graph.countComponents() == 1) {
				Node mst = graph.getMinSpanTree((short) 0);
				n = transformAC(align, node, mst, Fmap);
			} else {
				n = new Node(new ProductionSymbol(new Production(prod, graph.countComponents())));
				Fmap.put(node, n);
				for (short i = 0; i < graph.countComponents(); ++i) {
					Node mst = graph.getMinSpanTree(i);
					n.addChild(transformAC(align, node, mst, Fmap));
				}
			}
			return n;
		}
	}
	
	private double edgeWeight(NTo1WordAlign align, Node n1, Node n2) {
		// check shared variables
		Variable[] v1 = ((ProductionSymbol) n1.getSymbol()).getProduction().getVars();
		Variable[] v2 = ((ProductionSymbol) n2.getSymbol()).getProduction().getVars();
		if (Arrays.intersect(v1, v2).length == 0)
			return Double.POSITIVE_INFINITY;
		// check crossing links
		short min = Short.MAX_VALUE;
		short max = -1;
		Link[] links1 = getAllLinks(align, n1);
		Link[] links2 = getAllLinks(align, n2);
		for (short i = 0; i < links1.length; ++i) {
			if (min > links1[i].e)
				min = links1[i].e;
			if (max < links1[i].e)
				max = links1[i].e;
		}
		for (short i = 0; i < links2.length; ++i) {
			if (min > links2[i].e)
				min = links2[i].e;
			if (max < links2[i].e)
				max = links2[i].e;
		}
		Node p = n1.getParent();
		for (short i = min; i <= max; ++i) {
			Link link = align.getFirstLinkFromE(i);
			if (link != null) {
				Node n = align.getF(link.f);
				if (!n.isDescendOf(p) || n == p)
					return Double.POSITIVE_INFINITY;
			}
		}
		// return minimum word distance
		double minDist = Double.POSITIVE_INFINITY;
		for (short i = 0; i < links1.length; ++i)
			for (short j = 0; j < links2.length; ++j) {
				double dist = Math.abs(links1[i].e-links2[j].e);
				if (minDist > dist)
					minDist = dist;
			}
		return minDist;
	}
	
	private double edgeWeight(NTo1WordAlign align, Node p, Node c, boolean shareVars) {
		// check shared variables
		if (shareVars) {
			Variable[] v1 = ((ProductionSymbol) p.getSymbol()).getProduction().getVars();
			Variable[] v2 = ((ProductionSymbol) c.getSymbol()).getProduction().getVars();
			if (Arrays.intersect(v1, v2).length == 0)
				return Double.POSITIVE_INFINITY;
		}
		// return minimum word distance
		double minDist = Double.POSITIVE_INFINITY;
		Link link1 = align.getFirstLinkFromF(align.getF(p));
		Link[] links2 = getAllLinks(align, c);
		for (; link1 != null; link1 = link1.next)
			for (short j = 0; j < links2.length; ++j) {
				double dist = Math.abs(link1.e-links2[j].e);
				if (minDist > dist)
					minDist = dist;
			}
		return minDist;
	}
	
	private Link[] getAllLinks(NTo1WordAlign align, Node n) {
		ArrayList links = new ArrayList();
		Node[] descends = n.getDescends();
		for (short j = 0; j < descends.length; ++j) {
			Link link = align.getFirstLinkFromF(align.getF(descends[j]));
			while (link != null) {
				links.add(link);
				link = link.next;
			}
		}
		return (Link[]) links.toArray(new Link[0]);
	}
	
	private Node transformAC(NTo1WordAlign align, Node node, Node mst, HashMap Fmap) {
		short i = ((ShortSymbol) mst.getSymbol()).val();
		short nargs = mst.countChildren();
		if (nargs == 0)
			return transformAC(align, node.getChild(i), Fmap);
		else {
			Node c = transformAC(align, node.getChild(i), Fmap);
			Production p1 = ((ProductionSymbol) node.getSymbol()).getProduction();
			Production p2 = ((ProductionSymbol) c.getSymbol()).getProduction();
			Production p3 = new Production(p1, (short) (nargs+1));
			Production p4 = new Production(p3, p2, (short) 0);
			Node n = new Node(new ProductionSymbol(p4));
			Fmap.put(node.getChild(i), n);
			for (short j = 0; j < c.countChildren(); ++j)
				n.addChild(c.getChild(j));
			for (short j = 0; j < nargs; ++j)
				n.addChild(transformAC(align, node, mst.getChild(j), Fmap));
			return n;
		}
	}
	
	private void bindVars(Examples examples) {
		for (Iterator it = examples.iterator(); it.hasNext();) {
			Example ex = (Example) it.next();
			//logger.finer("example "+ex.id);
			for (int j = 0; j < ex.aligns.size(); ++j) {
				WordAlign align = (WordAlign) ex.aligns.get(j);
				//logger.finer("alignment "+j);
				Node[] F = align.getF();
				// find variables
				HashMap vmap = new HashMap();
				for (short k = 0; k < F.length; ++k) {
					Variable[] vars = ((ProductionSymbol) F[k].getSymbol()).getProduction().getVars();
					for (short l = 0; l < vars.length; ++l) {
						ArrayList list = (ArrayList) vmap.get(vars[l]);
						if (list == null) {
							list = new ArrayList();
							vmap.put(vars[l], list);
						}
						list.add(F[k]);
					}
				}
				// assign minimal scope for each variable
				for (Iterator kt = vmap.entrySet().iterator(); kt.hasNext();) {
					Map.Entry entry = (Map.Entry) kt.next();
					ArrayList list = (ArrayList) entry.getValue();
					Node lca = null;
					for (Iterator lt = list.iterator(); lt.hasNext();) {
						Node node = (Node) lt.next();
						if (lca == null)
							lca = node;
						else
							lca = lca.lca(node);
					}
					entry.setValue(lca);
				}
				// do variable binding
				for (short k = (short) (F.length-1); k >= 0; --k) {
					Production prod = ((ProductionSymbol) F[k].getSymbol()).getProduction();
					Symbol[] rhs = prod.getRhs();
					ArrayList vars = new ArrayList();
					for (short l = 0, m = 0; l < rhs.length; ++l)
						if (rhs[l] instanceof Variable) {
							if (!vars.contains(rhs[l]) && vmap.get(rhs[l]) != F[k])
								vars.add(rhs[l]);
						} else if (rhs[l] instanceof Nonterminal) {
							ProductionSymbol sym = (ProductionSymbol) F[k].getChild(m++).getSymbol();
							Nonterminal lhs = sym.getProduction().getLhs();
							for (short n = 0; n < lhs.countArgs(); ++n) {
								Variable arg = lhs.getArg(n);
								if (!vars.contains(arg) && vmap.get(arg) != F[k])
									vars.add(arg);
							}
						}
					prod = new Production(prod, (Variable[]) vars.toArray(new Variable[0]));
					F[k].setSymbol(new ProductionSymbol(prod));
				}
				//logger.finer(align.getF((short) 0).toPrettyString());
			}
		}
	}
	
	private void extract(SCFG gram, GapModel gm, NTo1WordAlign align) {
		logger.finer(align.getF((short) 0).toPrettyString());
		logger.finer(align.toString());
		
		// Combine nodes in MR parse trees if necessary
		ArrayList spans = new ArrayList();
		OUTER: for (short i = (short) (align.lengthF()-1); i >= 0; --i) {
			Node node = align.getF(i);
			short nwords = align.countLinksFromF(i);
			short nargs = node.countChildren();
			// MERGE: pattern is empty
			if (nwords+nargs == 0) {
				logger.finest("merge "+node.getSymbol()+" with its parent");
				align = (NTo1WordAlign) align.combineF(i);
				logger.finest(align.toString());
				continue;
			}
			Span span = new Span(((ProductionSymbol) node.getSymbol()).getProduction().getLhs());
			for (Link l = align.getFirstLinkFromF(i); l != null; l = l.next)
				span.update(l.e);
			for (short j = 0; j < nargs; ++j) {
				short k = align.getF(node.getChild(j));
				span.update((Span) spans.get(k-i-1));
			}
			// MERGE: pattern is not consistent with alignment
			for (short j = span.from; j < span.to; ++j) {
				Link l = align.getFirstLinkFromE(j);
				if (l != null && !align.getF(l.f).isDescendOf(node)) {
					logger.finest("merge "+node.getSymbol()+" with its parent");
					align = (NTo1WordAlign) align.combineF(i);
					logger.finest(align.toString());
					continue OUTER;
				}
			}
			spans.add(0, span);
		}
		
		// Extract SCFG rules
		for (short i = (short) (align.lengthF()-1); i >= 0; --i) {
			Node node = align.getF(i);
			Production prod = ((ProductionSymbol) node.getSymbol()).getProduction();
			Span span = (Span) spans.get(i);
			short nwords = (short) ((ALLOW_GAPS) ? align.countLinksFromF(i) : span.to-span.from);
			short nargs = node.countChildren();
			Span[] argSpans = new Span[nargs];
			for (short j = 0; j < nargs; ++j) {
				short k = align.getF(node.getChild(j));
				argSpans[j] = (Span) spans.get(k);
				if (!ALLOW_GAPS)
					nwords -= argSpans[j].to-argSpans[j].from;
			}
			double wF = align.wF[i];
			// so that at least one phrase-pair is extracted whenever possible
			boolean extracted = false;
			for (Node p = node; p != null; node = p, p = p.getParent()) {
				Production pp = ((ProductionSymbol) p.getSymbol()).getProduction();
				if (p != node) {
					// combine node with its parent
					short pi = align.getF(p);
					short ai = p.indexOf(node);
					prod = new Production(pp, prod, ai);
					span = (Span) spans.get(pi);
					nwords += (ALLOW_GAPS) ? align.countLinksFromF(pi) : span.to-span.from;
					nargs += p.countChildren()-1;
					Span[] as = new Span[p.countChildren()];
					for (short j = 0; j < as.length; ++j) {
						short k = align.getF(p.getChild(j));
						as[j] = (Span) spans.get(k);
						if (!ALLOW_GAPS)
							nwords -= as[j].to-as[j].from;
					}
					argSpans = (Span[]) Arrays.replace(as, ai, argSpans);
					wF *= align.wF[pi];
				}
				// SKIP: RHS MR contains an Anaphor symbol
				if (!prod.isAnaphor() && Arrays.containsType(prod.getRhs(), Anaphor.class))
					break;
				// SKIP: RHS MR is too long
				if (extracted && prod.getRhs().length > MAX_LENGTH)
					break;
				// SKIP: rule's arity is too high
				if (extracted && prod.countArgs() > MAX_ARITY)
					continue;
				// SKIP: root of RHS MR is unary
				if (p != node && pp.isUnary())
					continue;
				// SKIP: too few words in pattern
				if (align.getScore() < 1 && nwords < MIN_WORD_COUNT && !prod.isUnary()
						&& !Config.getMRLGrammar().isZeroFertility(prod))
					continue;
				// SKIP: rule would cause infinite loops
				if (nwords == 0 && nargs == 1) {
					int lhs = span.lhs.getId();
					int rhs = argSpans[0].lhs.getId();
					if (!dep[lhs][rhs]) {
						dep[lhs][rhs] = true;
						if (loopy()) {
							dep[lhs][rhs] = false;
							continue;
						}
					}
				}
				// actual rule extraction
				Symbol[] E = new Symbol[nwords+nargs];
				short[] gaps = new short[nwords+nargs+1];
				Symbol[] F = (Symbol[]) Arrays.copy(prod.getRhs());
				double PwFE = wF;
				double PwEF = 1;
				double gwF = 0;
				short gn = 0;
				short gfrom = -1;
				for (short j = span.from, k = 0; j < span.to;) {
					short index = -1;
					for (short l = 0; l < nargs; ++l)
						if (argSpans[l].from == j) {
							index = l;
							continue;
						}
					if (index >= 0) {
						E[k] = (Nonterminal) argSpans[index].lhs.copy();
						E[k].setIndex((short) (index+1));
						short l = (short) Arrays.findInstance(F, Nonterminal.class, index);
						F[l] = (Nonterminal) argSpans[index].lhs.copy();
						F[l].setIndex((short) (index+1));
						if (gfrom >= 0) {
							extractFiller(gm, align, gfrom, j, E[k-1], E[k]);
							gfrom = -1;
						}
						j = argSpans[index].to;
						++k;
					} else if (align.isLinkedFromE(j)) {
						E[k] = (Symbol) align.getE(j).copy();
						PwEF *= align.wE[j];
						if (gfrom >= 0) {
							extractFiller(gm, align, gfrom, j, E[k-1], E[k]);
							gfrom = -1;
						}
						++j;
						++k;
					} else if (ALLOW_GAPS) {
						++gaps[k];
						if (gfrom < 0)
							gfrom = j;
						++j;
					} else {
						E[k] = (Symbol) align.getE(j).copy();
						Lex e = new Lex(align.getE(j));
						Lex f = new Lex(null);
						PwEF *= lexEF.get(e, f);
						gwF += lexFE.get(f, e);
						++gn;
						++j;
						++k;
					}
				}
				Production pnorm = normalizeVars(prod, E, F);
				Rule rule = new Rule(pnorm, E, gaps, F, false);
				if (gram.containsRule(gram.tied(rule)))
					logger.finer("use "+rule.toString());
				else {
					logger.fine("add "+rule.toString());
					Config.getMRLGrammar().addProduction(pnorm);
					gram.addRule(rule);
					rule.ruleId = gram.getId(rule);
					rule.partialRuleId = gram.getPartialRuleId(new PartialRule(rule), true);
				}
				Rule r = gram.tied(rule);
				r.addCount();
				PhrE e = new PhrE(r.getE(), r.getGaps());
				PhrF f = new PhrF(r.getF());
				phrFE.add(f, e);
				phrEF.add(e, f);
				if (gn > 0)
					PwFE *= gwF/gn;
				if (r.getScores().PwFE < PwFE)
					r.getScores().PwFE = PwFE;
				if (r.getScores().PwEF < PwEF)
					r.getScores().PwEF = PwEF;
				extracted = true;
			}
		}
	}

	private void extractFiller(GapModel gm, NTo1WordAlign align, short from, short to, Symbol before, 
			Symbol after) {
		if (!(gm instanceof BasicGapModel))
			return;
		Terminal[] filler = new Terminal[to-from];
		double PwFE = 0;
		double PwEF = 1;
		for (short i = from; i < to; ++i) {
			filler[i-from] = (Terminal) align.getE(i).copy();
			((BasicGapModel) gm).addCount(filler[i-from]);
			Lex e = new Lex(filler[i-from]);
			Lex f = new Lex(null);
			PwFE += lexFE.get(f, e);
			PwEF *= lexEF.get(e, f);
		}
		PwFE /= to-from;
		before = (Symbol) before.copy();
		before.removeIndex();
		after = (Symbol) after.copy();
		after.removeIndex();
		GapFiller fill = new GapFiller(filler, before, after);
		fill.scores.PwFE = Math.log(PwFE);
		fill.scores.PwEF = Math.log(PwEF);
		((BasicGapModel) gm).addFiller(fill);
		PhrE e = new PhrE(filler);
		PhrF f = new PhrF(null);
		phrFE.add(f, e);
		phrEF.add(e, f);
	}
	
	private Production normalizeVars(Production prod, Symbol[] E, Symbol[] F) {
		VariableAssignment assign = new VariableAssignment();
		short nextVar = 1;
		for (short i = 0; i < F.length; ++i)
			if (F[i] instanceof Variable) {
				Variable v = assign.get((Variable) F[i]);
				if (v == null) {
					v = new Variable(nextVar++);
					assign.put((Variable) F[i], v);
				}
				F[i] = v;
			} else if (F[i] instanceof Nonterminal)
				for (short j = 0; j < ((Nonterminal) F[i]).countArgs(); ++j) {
					Variable arg = ((Nonterminal) F[i]).getArg(j);
					Variable v = assign.get(arg);
					if (v == null) {
						v = new Variable(nextVar++);
						assign.put(arg, v);
					}
				}
		for (short i = 0; i < E.length; ++i)
			if (E[i] instanceof Nonterminal)
				E[i] = new Nonterminal((Nonterminal) E[i], assign);
		for (short i = 0; i < F.length; ++i)
			if (F[i] instanceof Nonterminal)
				F[i] = new Nonterminal((Nonterminal) F[i], assign);
		return new Production(prod, assign);
	}
	
	private void countPhr(SCFG gram, GapModel gm) {
		Rule[] rules = gram.getRules();
		for (int i = 0; i < rules.length; ++i) {
			rules[i].getScores().PwFE = Math.log(rules[i].getScores().PwFE);
			rules[i].getScores().PwEF = Math.log(rules[i].getScores().PwEF);
		}
		phrFE.normalize();
		phrEF.normalize();
		for (int i = 0; i < rules.length; ++i) {
			Rule r = gram.tied(rules[i]);
			PhrE e = new PhrE(r.getE(), r.getGaps());
			PhrF f = new PhrF(r.getF());
			rules[i].getScores().PFE = Math.log(phrFE.get(f, e));
			rules[i].getScores().PEF = Math.log(phrEF.get(e, f));
			rules[i].getScores().PwEF = r.getScores().PwEF;
			rules[i].getScores().PwFE = r.getScores().PwFE;
		}
		if (gm instanceof BasicGapModel) {
			ArrayList fillers = ((BasicGapModel) gm).getFillers();
			for (Iterator it = fillers.iterator(); it.hasNext();) {
				GapFiller fill = (GapFiller) it.next();
				PhrE e = new PhrE(fill.filler);
				PhrF f = new PhrF(null);
				fill.scores.PFE = Math.log(phrFE.get(f, e));
				fill.scores.PEF = Math.log(phrEF.get(e, f));
			}
		}
	}
	
}
