package de.dagere.peass.dependency;

import de.dagere.peass.dependency.analysis.data.TestSet;

public class RunnableTestInformation {
   private final TestSet testsToUpdate = new TestSet();
   private final TestSet ignoredTests = new TestSet();

   public TestSet getTestsToUpdate() {
      return testsToUpdate;
   }

   public TestSet getIgnoredTests() {
      return ignoredTests;
   }
}
