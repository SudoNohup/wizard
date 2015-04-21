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
package wasp.nl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import wasp.util.Copyable;

/**
 * A data structure for storing relative frequencies.
 * 
 * @author ywwong
 *
 */
public class RelativeFreq {

	/**
	 * Data structure for representing events, and how often they occur (absolute frequency).
	 * 
	 * @author ywwong
	 *
	 */
	public static abstract class Event implements Copyable {
		/** The absolute frequency of this event. */
		public double freq;
		/** Constructs a new event with the frequency of one. */
		public Event() {
			freq = 1;
		}
	}

	private HashMap map;
	
	public RelativeFreq() {
		map = new HashMap();
	}
	
	/**
	 * Adds an event to this data structure.
	 * 
	 * @param event the event to add.
	 * @param given the event being conditioned upon.
	 */
	public void add(Event event, Event given) {
		ArrayList list = (ArrayList) map.get(given);
		if (list == null) {
			list = new ArrayList();
			map.put(given, list);
		}
		for (Iterator it = list.iterator(); it.hasNext();) {
			Event e = (Event) it.next();
			if (e.equals(event)) {
				e.freq += event.freq;
				return;
			}
		}
		list.add(event.copy());
	}
	
	/**
	 * Returns the relative frequency of the specified event given another event.
	 * 
	 * @param event the event of interest.
	 * @param given the event being conditioned upon.
	 * @return the relative frequency of <code>event</code> conditioned on <code>given</code>.
	 */
	public double get(Event event, Event given) {
		ArrayList list = (ArrayList) map.get(given);
		if (list == null)
			return 0;
		for (Iterator it = list.iterator(); it.hasNext();) {
			Event e = (Event) it.next();
			if (e.equals(event))
				return e.freq;
		}
		return 0;
	}
	
	/**
	 * Normalizes the frequencies.  This has to be done before retrieving any relative frequencies
	 * (i.e. before calling the <code>get</code> method).
	 */
	public void normalize() {
		for (Iterator it = map.values().iterator(); it.hasNext();) {
			ArrayList list = (ArrayList) it.next();
			double sum = 0;
			for (Iterator jt = list.iterator(); jt.hasNext();)
				sum += ((Event) jt.next()).freq;
			for (Iterator jt = list.iterator(); jt.hasNext();) {
				Event e = (Event) jt.next();
				e.freq /= sum;
			}
		}
	}
	
}
