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
package wasp.util;

import java.io.File;
import java.io.IOException;

/**
 * This version of <code>FileOutputStream</code> creates all necessary but nonexistent parent directories
 * during the construction of <code>FileOutputStream</code> objects.
 * 
 * @author ywwong
 *
 */
public class FileOutputStream extends java.io.FileOutputStream {

	private FileOutputStream(File file) throws IOException {
		super(file);
	}
	
	private FileOutputStream(String filename) throws IOException {
		super(filename);
	}
	
	/**
	 * Constructs a <code>FileOutputStream</code> object given an abstract pathname.  All nonexistent
	 * parent directories are also created.
	 * 
	 * @param file the abstract pathname.
	 * @return a newly-constructed <code>FileOutputStream</code> object based on the given abstract
	 * pathname.
	 * @throws IOException if the file exists but is a directory rather than a regular file, or cannot 
	 * be opened for any other reason.
	 */
	public static FileOutputStream createNew(File file) throws IOException {
		if (file.getParentFile() != null)
			file.getParentFile().mkdirs();
		return new FileOutputStream(file);
	}
	
	/**
	 * Constructs a <code>FileOutputStream</code> object given a file name.  All nonexistent parent
	 * directories are also created.
	 * 
	 * @param filename the system-dependent file name.
	 * @return a newly-constructed <code>FileOutputStream</code> object based on the given file name.
	 * @throws IOException if the named file exists but is a directory rather than a regular file, or 
	 * cannot be opened for any other reason.
	 */
	public static FileOutputStream createNew(String filename) throws IOException {
		File file = new File(filename);
		if (file.getParentFile() != null)
			file.getParentFile().mkdirs();
		return new FileOutputStream(filename);
	}
	
}
