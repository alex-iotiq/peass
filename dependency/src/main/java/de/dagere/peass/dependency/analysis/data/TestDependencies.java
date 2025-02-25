/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.dependency.analysis.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;

/**
 * Represents information about the tests and their dependencies, i.e. the classes they call.
 * 
 * @author reichelt
 *
 */
public class TestDependencies {

   private static final Logger LOG = LogManager.getLogger(TestDependencies.class);

   /**
    * Map from testcase (package.clazz.method) to dependent class to the list of called methods of this class
    */
   private final Map<TestMethodCall, CalledMethods> dependencyMap = new HashMap<>();

   public TestDependencies() {

   }

   public Map<TestMethodCall, CalledMethods> getDependencyMap() {
      return dependencyMap;
   }

   /**
    * Gets the dependencies for a test, i.e. the used classes. If the test is not known yet, an empty Set is returned.
    * 
    * @param test
    */
   public Map<ChangedEntity, Set<String>> getOrAddDependenciesForTest(final TestMethodCall test) {
      CalledMethods tests = dependencyMap.get(test);
      if (tests == null) {
         tests = new CalledMethods();
         dependencyMap.put(test, tests);
         final ChangedEntity onlyClass = new ChangedEntity(test.getClazz(), test.getModule());
         final HashSet<String> calledMethods = new HashSet<>();
         tests.getCalledMethods().put(onlyClass, calledMethods);
         String method = test.getMethod();
         if (test.getParams() != null) {
            method += "(" + test.getParams() + ")";
         }
         calledMethods.add(method);
      }
      return tests.getCalledMethods();
   }
   
   public void setDependencies(final TestMethodCall testClassName, final Map<ChangedEntity, Set<String>> allCalledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = getOrAddDependenciesForTest(testClassName);
      testDependencies.putAll(allCalledClasses);
   }
   
   /**
    * Since we have no information about complete dependencies when reading an old static selection file, just add dependencies
    * 
    * @param testMethod
    * @param testMethodName
    * @param calledClasses Map from name of the called class to the methods of the class that are called
    */
   public void addDependencies(final TestMethodCall testMethod, final Map<ChangedEntity, Set<String>> calledClasses) {
      final Map<ChangedEntity, Set<String>> testDependencies = getOrAddDependenciesForTest(testMethod);
      for (final Map.Entry<ChangedEntity, Set<String>> calledEntity : calledClasses.entrySet()) {
         LOG.debug("Adding call: " + calledEntity.getKey());
         LOG.debug(testDependencies.keySet());
         final Set<String> oldSet = testDependencies.get(calledEntity.getKey());
         if (oldSet != null) {
            oldSet.addAll(calledEntity.getValue());
         } else {
            testDependencies.put(calledEntity.getKey(), calledEntity.getValue());
         }
      }
   }

   public void removeTest(final TestCase entity) {
      dependencyMap.remove(entity);
   }

   public int size() {
      return dependencyMap.size();
   }

   public Map<TestMethodCall, Map<ChangedEntity, Set<String>>> getCopiedDependencies() {
      final Map<TestMethodCall, Map<ChangedEntity, Set<String>>> copy = new HashMap<>();
      for (final Map.Entry<TestMethodCall, CalledMethods> entry : dependencyMap.entrySet()) {
         final Map<ChangedEntity, Set<String>> dependencies = new HashMap<>();
         for (final Map.Entry<ChangedEntity, Set<String>> testcase : entry.getValue().getCalledMethods().entrySet()) {
            final Set<String> copiedMethods = new HashSet<>();
            copiedMethods.addAll(testcase.getValue());
            dependencies.put(testcase.getKey(), copiedMethods);
         }
         copy.put(entry.getKey(), dependencies);
      }
      return copy;
   }

   @Override
   public String toString() {
      return dependencyMap.toString();
   }

   /**
    * Returns a list of all tests that changed based on given changed classes and the dependencies of the current version. So the result mapping is changedclass to a set of tests,
    * that could have been changed by this changed class.
    * 
    * @param staticTestSelection
    * @param changes
    * @return Map from changed class to the influenced tests
    */
   public ChangeTestMapping getChangeTestMap(final Map<ChangedEntity, ClazzChangeData> changes) {
      final ChangeTestMapping changeTestMap = new ChangeTestMapping();
      for (final Entry<TestMethodCall, CalledMethods> dependencyEntry : dependencyMap.entrySet()) {
         final TestMethodCall currentTestcase = dependencyEntry.getKey();
         final CalledMethods currentTestDependencies = dependencyEntry.getValue();
         for (ClazzChangeData changedEntry : changes.values()) {
            for (ChangedEntity change : changedEntry.getChanges()) {
               final ChangedEntity changedClass = change.onlyClazz();
               final Set<ChangedEntity> calledClasses = currentTestDependencies.getCalledClasses();
               if (calledClasses.contains(changedClass)) {
                  addCall(changeTestMap, currentTestcase, currentTestDependencies, changedEntry, change, changedClass);
               }
            }
         }
      }
      for (final Map.Entry<ChangedEntity, Set<TestMethodCall>> element : changeTestMap.getChanges().entrySet()) {
         LOG.debug("Change: {} Calling: {} {}", element.getKey(), element.getValue().size(), element.getValue());
      }

      return changeTestMap;
   }

   private void addCall(final ChangeTestMapping changeTestMap, final TestMethodCall currentTestcase, final CalledMethods currentTestDependencies,
         final ClazzChangeData changedEntry, final ChangedEntity change, final ChangedEntity changedClass) {
      boolean clazzLevelChange = !changedEntry.isOnlyMethodChange();
      if (clazzLevelChange) {
         changeTestMap.addChangeEntry(change, currentTestcase);
         changeTestMap.addChangeEntry(change.onlyClazz(), currentTestcase);
      } else { 
         String method = change.getMethod() + change.getParameterString();
         final Map<ChangedEntity, Set<String>> calledMethods = currentTestDependencies.getCalledMethods();
         final Set<String> calledMethodsInChangeClass = calledMethods.get(changedClass);
         if (calledMethodsInChangeClass.contains(method)) {
            final ChangedEntity classWithMethod = new ChangedEntity(changedClass.getClazz(), changedClass.getModule(), method);
            changeTestMap.addChangeEntry(classWithMethod, currentTestcase);
         }
      }
   }
}
