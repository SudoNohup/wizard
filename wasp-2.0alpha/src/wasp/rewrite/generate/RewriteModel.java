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
package wasp.rewrite.generate;

import java.io.IOException;

import wasp.data.Dictionary;
import wasp.data.Examples;
import wasp.data.Symbol;
import wasp.main.Generator;
import wasp.main.generate.GenerateModel;
import wasp.mrl.Production;

/**
 * A tactical generation model based on IBM Model 4/Rewrite.  This class also implements the Rewrite++
 * variant of the model, which uses linearized MR parse-trees as the input to the tactical generator.
 * 
 * @author ywwong
 *
 */
public class RewriteModel extends GenerateModel {

	private GIZAPlusPlus tm;
	private CMUCamBinaryModel lm;
	
	/**
	 * Constructs a new NL generation model based on IBM Model 4/Rewrite.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public RewriteModel() throws IOException {
		tm = new GIZAPlusPlus();
		lm = new CMUCamBinaryModel();
	}
	
	public Generator getGenerator() throws IOException {
		return new RewriteGenerator(tm, lm);
	}

	public void train(Examples examples) throws IOException {
		lm.train(examples);
		tm.train(examples);
	}

	public void read() throws IOException {
		tm.read();
		lm.read();
	}

	protected static String token(Production prod) {
		if (prod.isWildcardMatch())
			return prod.getRhs((short) 0).toString();
		else
			return prod.toString().replaceAll("\\W|_", "").toLowerCase();
	}
	
	protected static String token(Symbol sym) {
		String token = sym.toString();
		if (Dictionary.isSpecial(token))
			return token;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < token.length(); ++i) {
			char c = token.charAt(i);
			if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || c == '_' || ('0' <= c && c <= '9'))
				sb.append(c);
			else
				sb.append("c"+((int) c));
		}
		return sb.toString();
	}
	
}
