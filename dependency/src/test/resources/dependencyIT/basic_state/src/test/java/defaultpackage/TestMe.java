package defaultpackage;

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

import org.junit.Test;

import defaultpackage.NormalDependency;
import defaultpackage.OtherDependency;

public class TestMe {	
	
	@Test
	public void testMe(){
		final NormalDependency normal = new NormalDependency();
		normal.executeThing();
		System.out.println("Test1");
	}
	
	@Test
	public void removeMe(){
		final OtherDependency other = new OtherDependency();
		other.executeThing();
	}
}
