package de.peran.dependency;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 DaGeRe
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Searches for all classes in a maven project. Used for instrumeting them.
 * 
 * @author reichelt
 *
 */
public class ClazzFinder {

	/**
	 * Returns a list of all classes of a maven project
	 * 
	 * @param projectFolder Folder where to search for classes
	 * @return list of classes
	 */
	public static List<String> getLowestPackageOverall(final File projectFolder) {
		List<String> clazzes = new LinkedList<>();
		File src = new File(projectFolder, "src");
		File main = new File(src, "main");
		File mainJava = new File(src, "java");
		File test = new File(src, "test");
		if (mainJava.exists()) {
			addClazzes(clazzes, mainJava);
		} else {
			if (main.exists()) {
				File java = new File(main, "java");
				if (java.exists()) {
					addClazzes(clazzes, java);
				} else {
					addClazzes(clazzes, main);
				}
			}
		}

		if (test.exists()) {
			File java = new File(test, "java");
			if (java.exists()) {
				addClazzes(clazzes, java);
			} else {
				addClazzes(clazzes, test);
			}
		}
		return clazzes;
	}

	/**
	 * Searches for classes in a specific folder
	 * 
	 * @param clazzes List where classes should be added
	 * @param folder Main folder that should be searched
	 */
	private static void addClazzes(final List<String> clazzes, final File folder) {
		for (File clazzFile : FileUtils.listFiles(folder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
			String path = clazzFile.getAbsolutePath();
			path = path.replace(folder.getAbsolutePath() + File.separator, "");
			path = path.substring(0, path.length() - 5);
			String clazz = path.replace(File.separator, ".");
			clazzes.add(clazz);
		}
	}

}
