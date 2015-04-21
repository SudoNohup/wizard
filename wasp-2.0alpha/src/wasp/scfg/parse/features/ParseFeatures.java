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
package wasp.scfg.parse.features;

import java.util.ArrayList;
import java.util.StringTokenizer;

import wasp.main.Config;
import wasp.math.Math;
import wasp.scfg.SCFGModel;

/**
 * The complete feature set for semantic parsing.
 * 
 * @author ywwong
 *
 */
public class ParseFeatures {

	public static boolean USE_VAR_TYPES = true;
	public static short NUM_BINS_FREE_VARS = 3;
	
	public ParseFeature[] all;
	public ParseFeature[] predict;
	public ParseFeature[] scan;
	public ParseFeature[] complete;
	
	public boolean useFreeVars = false;
	public boolean useRuleBigrams = false;
	
	public static ParseFeatures createNew(SCFGModel model) {
		ParseFeatures pf = new ParseFeatures();
		ArrayList plist = new ArrayList();
		ArrayList slist = new ArrayList();
		ArrayList clist = new ArrayList();
		String str = Config.get(Config.PARSE_FEATURES);
		if (str == null)
			str = "";
		StringTokenizer toks = new StringTokenizer(str, ",");
		while (toks.hasMoreTokens()) {
			String tok = toks.nextToken();
			if (tok.equals("gap-weight"))
				slist.add(new GapWeight(model));
			else if (tok.equals("parent-rule-free-vars")) {
				clist.add(new ParentRuleFreeVars(model));
				pf.useFreeVars = true;
			} else if (tok.equals("rule-bigrams")) {
				clist.add(new RuleBigrams(model));
				pf.useRuleBigrams = true;
			} else if (tok.equals("rule-bigrams-free-vars")) {
				clist.add(new RuleBigramsFreeVars(model));
				pf.useFreeVars = true;
				pf.useRuleBigrams = true;
			} else if (tok.equals("rule-weight"))
				plist.add(new RuleWeight(model));
			else if (tok.equals("two-level-rules"))
				clist.add(new TwoLevelRules(model));
			else if (tok.equals("two-level-rules-free-vars")) {
				clist.add(new TwoLevelRulesFreeVars(model));
				pf.useFreeVars = true;
			}
		}
		ArrayList alist = new ArrayList();
		alist.addAll(plist);
		alist.addAll(slist);
		alist.addAll(clist);
		pf.all = (ParseFeature[]) alist.toArray(new ParseFeature[0]);
		pf.predict = (ParseFeature[]) plist.toArray(new ParseFeature[0]);
		pf.scan = (ParseFeature[]) slist.toArray(new ParseFeature[0]);
		pf.complete = (ParseFeature[]) clist.toArray(new ParseFeature[0]);
		return pf;
	}
	
	public short binFreeVars(short n) {
		return Math.min(n, (short) (NUM_BINS_FREE_VARS-1));
	}
	
}
