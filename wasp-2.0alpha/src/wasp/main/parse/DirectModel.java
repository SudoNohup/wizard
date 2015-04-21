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
package wasp.main.parse;

import java.io.IOException;

import wasp.data.Examples;
import wasp.main.Parser;
import wasp.main.TranslationModel;
import wasp.scfg.SCFGModel;
import wasp.scfg.parse.SCFGParser;

/**
 * The direct parsing model.
 * 
 * @author ywwong
 *
 */
public class DirectModel extends ParseModel {

	private TranslationModel tm;
	
	public DirectModel() throws IOException {
		tm = TranslationModel.createNew();
	}
	
	public Parser getParser() throws IOException {
		if (tm instanceof SCFGModel)
			return new SCFGParser((SCFGModel) tm);
		return null;
	}

	public void train(Examples examples, boolean full) throws IOException {
		tm.train(examples, full);
	}

	public void read() throws IOException {
		tm.read();
	}

}
