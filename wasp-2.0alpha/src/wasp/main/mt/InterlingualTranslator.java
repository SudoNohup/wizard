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
package wasp.main.mt;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import wasp.data.Meaning;
import wasp.data.Terminal;
import wasp.main.Generator;
import wasp.main.Parse;
import wasp.main.Parser;
import wasp.main.Translator;
import wasp.main.generate.GenerateModel;
import wasp.main.parse.ParseModel;

/**
 * The interlingual machine translator.
 * 
 * @author ywwong
 *
 */
public class InterlingualTranslator extends Translator {

	private Parser parser;
	private Generator generator;
	
	public InterlingualTranslator(ParseModel pm, GenerateModel gm) throws IOException {
		parser = pm.getParser();
		generator = gm.getGenerator();
	}
	
	public boolean batch() {
		return parser.batch() || generator.batch();
	}

	public Iterator translate(Terminal[] E) throws IOException {
		Iterator it = parser.parse(E);
		if (!it.hasNext())
			return it;
		Parse parse = (Parse) it.next();
		Meaning F = new Meaning(parse.toStr());
		return generator.generate(F);
	}
	
	private static class EmptyIterator implements Iterator {
		public boolean hasNext() {
			return false;
		}
		public Object next() {
			throw new NoSuchElementException();
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	public Iterator[] translate(Terminal[][] E) throws IOException {
		Iterator[] parses;
		if (parser.batch())
			parses = parser.parse(E);
		else {
			parses = new Iterator[E.length];
			for (int i = 0; i < E.length; ++i)
				parses[i] = parser.parse(E[i]);
		}
		Meaning[] F = new Meaning[parses.length];
		int np = 0;
		for (int i = 0; i < parses.length; ++i)
			if (parses[i].hasNext()) {
				F[i] = new Meaning(((Parse) parses[i].next()).toStr());
				++np;
			}
		Iterator[] E2 = new Iterator[F.length];
		if (generator.batch()) {
			Meaning[] Fnn = new Meaning[np];
			if (np > 0) {
				for (int i = 0, j = 0; i < F.length; ++i)
					if (F[i] != null)
						Fnn[j++] = F[i];
				Iterator[] E2nn = generator.generate(Fnn);
				for (int i = 0, j = 0; i < F.length; ++i)
					if (F[i] != null)
						E2[i] = E2nn[j++];
			}
			for (int i = 0; i < F.length; ++i)
				if (F[i] == null)
					E2[i] = new EmptyIterator();
		} else
			for (int i = 0; i < F.length; ++i) {
				if (F[i] == null)
					E2[i] = new EmptyIterator();
				else
					E2[i] = generator.generate(F[i]);
			}
		return E2;
	}
	
}
