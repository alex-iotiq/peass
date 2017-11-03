/*-
 * #%L
 * peran-measurement
 * %%
 * Copyright (C) 2015 - 2017 DaGeRe
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

/**
 * Example comment
 * @author reichelt
 *
 */
class Test2_Named{
	
	public static final int y = 438;
	public int w = 48;

	/**
	 * Comment 1
	 */
	public Test() {
		// Line-comment
		int a = 3 + 5 - 8;
		System.out.println(a);
	}
	
	/**
	 * Comment 2
	 * @param i
	 */
	public static void doStaticThing(int i){
		int y = i + 1;
		System.out.println(y);
		Runnable r3 = new Runnable(){
		
		@Override
		public void run() {
			System.out.println("Run R3");
			
		}};

		
		r3.run();
	}
	
	/**
	 * Comment 3
	 */
	public void run(){
		System.out.println("a");
	}


	static Object r1 = new Runnable() {

		@Override
		public void run() {
			System.out.println("Run R1");
			
		}

		public void run2() {
			System.out.println("Run R4");
			
		}
	};
	
	static Runnable r2 = new Runnable() {
		
		@Override
		public void run() {
			System.out.println("Run R2");
		}
	};

	static class MyStuff{
		public void doMyStuff1(){ System.out.println("stuff 1");}
		public void doMyStuff2(){ System.out.println("stuff A");}
	}

	static class MyStuff2{
		public void doMyStuff1(){ System.out.println("stuff A");}
		public void doMyStuff2(){ System.out.println("stuff 2");}
	}
}
