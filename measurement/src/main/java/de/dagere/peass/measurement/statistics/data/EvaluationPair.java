package de.dagere.peass.measurement.statistics.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

/**
 * Represents an pair of measurement results that should be evaluated, i.e. the commits of both measurements and its results.
 * 
 * @author reichelt
 *
 */
public class EvaluationPair {

   private final String previousCommit, currentCommit;
   private final List<VMResult> previous = new LinkedList<>();
   private final List<VMResult> current = new LinkedList<>();
   private final TestMethodCall testcase;

   public EvaluationPair(final String currentCommit, final String previousCommit, final TestMethodCall testcase) {
      this.currentCommit = currentCommit;
      this.previousCommit = previousCommit;
      this.testcase = testcase;
      if (currentCommit.equals(previousCommit)) {
         throw new RuntimeException("Unexpected behaviour: Previous " + previousCommit + " == Current " + currentCommit + " commit.");
      }
      if (currentCommit == null || previousCommit == null) {
         throw new RuntimeException("Version == null: " + currentCommit + " " + previousCommit + " " + testcase);
      }
   }
   
   public TestMethodCall getTestcase() {
      return testcase;
   }

   public List<VMResult> getPrevius() {
      return previous;
   }

   public List<VMResult> getCurrent() {
      return current;
   }

   public void createHistogramFiles(final File currentFile, final File previousFile) {
      printMeans(currentFile, current);
      printMeans(previousFile, previous);
	}

   public static final NumberFormat FORMAT = NumberFormat.getInstance();

   static {
      FORMAT.setGroupingUsed(false);
   }
   
   private void printMeans(final File current, final List<VMResult> results) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(current))){
         for (final VMResult result : results) {
            final double value = result.getValue();
            writer.write(FORMAT.format(value)+"\n");
         }
         writer.flush();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public boolean isComplete() {
      boolean isComplete = previous.size() > 0 && previous.size() == current.size();
      if (isComplete) {
         isComplete &= previous.get(0).getFulldata() != null;
         isComplete &= current.get(0).getFulldata() != null;
         if (isComplete) {
            isComplete &= previous.get(0).getFulldata().getValues().size() > 0;
            isComplete &= current.get(0).getFulldata().getValues().size() > 0;
         }
      }
      return isComplete;
   }

   public String getPreviousCommit() {
      return previousCommit;
   }

   public String getCommit() {
      return currentCommit;
   }
}