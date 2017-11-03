
package de.peran.vcs;

/*-
 * #%L
 * peran-dependency
 * %%
 * Copyright (C) 2017 Hanns-Seidel-Stiftung
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

/**
 * Iterator for VCS, moving always the position of the iterator alongside with the version saved in the folder.
 * @author reichelt
 *
 */
public abstract class VersionIterator {

	protected final File projectFolder;
	protected int tagid = 0;

	public VersionIterator(final File projectFolder) {
		this.projectFolder = projectFolder;
	}

	/**
	 * Returns the count of commits
	 * 
	 * @return count of commits
	 */
	public abstract int getSize();

	/**
	 * Returns the current tag
	 * 
	 * @return current tag
	 */
	public abstract String getTag();

	/**
	 * Whether a next commit is present
	 * @return True, if a next commit is present, false otherwise
	 */
	public abstract boolean hasNextCommit();

	/**
	 * Goes to next commit, also checking out next version in the folder
	 * @return True for success, false otherwise
	 */
	public abstract boolean goToNextCommit();

	/**
	 * Goes to first commit, both in the iterator and the folder
	 * @return True for success, false otherwise
	 */
	public abstract boolean goToFirstCommit();

	/**
	 * Checkout the Commit before the start (if no one is given, just move the iterator and do not change folder state)
	 * @return Whether the 0th commit is not equal to the current commit
	 */
	public abstract boolean goTo0thCommit();

}
