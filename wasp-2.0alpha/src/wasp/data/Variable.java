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

import wasp.util.Short;

/**
 * The class for logical variables.
 * 
 * @author ywwong
 *
 */
public class Variable extends Symbol {

	public Variable(short index) {
		this.id = index;
	}
	
	public Variable() {
		this.id = 0;
	}
	
	public boolean equals(Object o) {
		return o instanceof Variable && id == ((Variable) o).id;
	}

	public boolean matches(Symbol sym) {
		return sym instanceof Variable;
	}

	public int hashCode() {
		return id;
	}

	public Object copy() {
		return new Variable((short) id);
	}

	public short getVarId() {
		return (short) id;
	}
	
	///
	/// String representations
	///
	
	public String toString() {
		return (id==0) ? "*x:_" : "*x:"+id;
	}

	public static Symbol read(String token) {
		if (token.startsWith("*x:")) {
			token = token.substring(3);
			if (token.equals("_"))
				return new Variable();
			else
				return new Variable(Short.parseShort(token));
		}
		return null;
	}
	
}
