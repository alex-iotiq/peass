package de.dagere.peass.measurement.cleaning;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.measurement.dataloading.DataAnalyser;
import de.dagere.peass.measurement.dataloading.MeasurementFileFinder;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;

/**
 * Cleans measurement data by reading all iteration-values of every VM, dividing them in the middle and saving the results in a clean-folder in single chunk-entries in a
 * measurement file for each test method.
 * 
 * This makes it possible to process the data faster, e.g. for determining performance changes or statistic analysis.
 * 
 * @author reichelt
 *
 */
public class Cleaner extends DataAnalyser {

   private static final Logger LOG = LogManager.getLogger(Cleaner.class);

   private final File cleanFolder;
   private int correct = 0;
   protected int read = 0;

   public int getCorrect() {
      return correct;
   }

   public int getRead() {
      return read;
   }

   public Cleaner(final File cleanFolder, CommitComparatorInstance comparator) {
      super(comparator);
      this.cleanFolder = cleanFolder;
   }

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         read++;
         cleanTestVersionPair(entry);
      }
   }

   public void cleanTestVersionPair(final Entry<String, EvaluationPair> entry) {
      TestMethodCall testcase = entry.getValue().getTestcase();
      if (entry.getValue().getPrevius().size() >= 2 && entry.getValue().getCurrent().size() >= 2) {
         final VMResultChunk cleanedChunk = new VMResultChunk();
         final long minExecutionCount = MultipleVMTestUtil.getMinIterationCount(entry.getValue().getPrevius());

         final List<VMResult> previous = getChunk(entry.getValue().getPreviousCommit(), minExecutionCount, entry.getValue().getPrevius());
         cleanedChunk.getResults().addAll(previous);

         final List<VMResult> current = getChunk(entry.getValue().getCommit(), minExecutionCount, entry.getValue().getCurrent());
         cleanedChunk.getResults().addAll(current);
         
         long chunkStartTime = getChunkStartTime(previous, current);
         cleanedChunk.setChunkStartTime(chunkStartTime);

         handleChunk(entry, testcase, cleanedChunk);
      }
   }

   private long getChunkStartTime(final List<VMResult> previous, final List<VMResult> current) {
      long chunkStartTime = Long.MAX_VALUE;
      for (VMResult previousResult : previous) {
         chunkStartTime = Math.min(previousResult.getDate(), chunkStartTime);
      }
      for (VMResult previousResult : current) {
         chunkStartTime = Math.min(previousResult.getDate(), chunkStartTime);
      }
      return chunkStartTime;
   }

   private void handleChunk(final Entry<String, EvaluationPair> entry, TestMethodCall testcase, final VMResultChunk cleanedChunk) {
      final MeasurementFileFinder finder = new MeasurementFileFinder(cleanFolder, testcase);
      final File measurementFile = finder.getMeasurementFile();
      final Kopemedata oneResultData = finder.getOneResultData();
      DatacollectorResult datacollector = finder.getDataCollector();

      if (checkChunk(cleanedChunk)) {
         datacollector.getChunks().add(cleanedChunk);
         JSONDataStorer.storeData(measurementFile, oneResultData);
         correct++;
      } else {
         printFailureInfo(entry, cleanedChunk, measurementFile);
      }
   }

   private void printFailureInfo(final Entry<String, EvaluationPair> entry, final VMResultChunk currentChunk, final File measurementFile) {
      for (final VMResult r : entry.getValue().getPrevius()) {
         LOG.debug("Value: {} Executions: {} Repetitions: {}", r.getValue(), r.getIterations(), r.getRepetitions());
      }
      for (final VMResult r : entry.getValue().getCurrent()) {
         LOG.debug("Value:  {} Executions: {} Repetitions: {}", r.getValue(), r.getIterations(), r.getRepetitions());
      }
      LOG.debug("Too few correct measurements: {} ", measurementFile.getAbsolutePath());
      LOG.debug("Measurements: {} / {}", currentChunk.getResults().size(), entry.getValue().getPrevius().size() + entry.getValue().getCurrent().size());
   }

   public boolean checkChunk(final VMResultChunk currentChunk) {
      return currentChunk.getResults().size() > 2;
   }

   private static final long ceilDiv(final long x, final long y) {
      return -Math.floorDiv(-x, y);
   }

   private List<VMResult> getChunk(final String commit, final long minExecutionCount, final List<VMResult> previous) {
      final List<VMResult> previousClean = StatisticUtil.shortenValues(previous);
      return previousClean.stream()
            .filter(result -> {
               final int resultSize = result.getFulldata().getValues().size();
               final long expectedSize = ceilDiv(minExecutionCount, 2);
               final boolean isCorrect = resultSize == expectedSize && !Double.isNaN(result.getValue());
               if (!isCorrect) {
                  LOG.debug("Wrong size: {} Expected: {}", resultSize, expectedSize);
               }
               return isCorrect;
            })
            .map(result -> cleanResult(commit, result))
            .collect(Collectors.toList());
   }

   private VMResult cleanResult(final String commit, final VMResult result) {
      result.setCommit(commit);
      result.setWarmup(result.getFulldata().getValues().size());
      result.setIterations(result.getFulldata().getValues().size());
      result.setRepetitions(result.getRepetitions());
      result.setMin(null);
      result.setMax(null);
      result.setFulldata(new Fulldata());
      return result;
   }
}
