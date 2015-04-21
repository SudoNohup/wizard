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
package wasp.main.generate;

import wasp.main.Parse;

/**
 * Parse trees produced by the log-linear generation model.
 *  
 * @author ywwong
 *
 */
public abstract class LogLinearGen extends Parse {

	/** The component scores of this parse. */
	public LogLinearModel.Scores scores;
	
	protected LogLinearGen(double score, LogLinearModel.Scores scores) {
		super(score);
		this.scores = scores;
		comment = "tm: "+scores.tm
				+", P(f|e): "+scores.PFE
				+", P(e|f): "+scores.PEF
				+", Pw(f|e): "+scores.PwFE
				+", Pw(e|f): "+scores.PwEF
				+", lm: "+scores.lm
				+", wp: "+scores.wp+"\n";
	}
	
}
