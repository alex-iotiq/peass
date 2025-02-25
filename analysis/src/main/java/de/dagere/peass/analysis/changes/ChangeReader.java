package de.dagere.peass.analysis.changes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.analysis.helper.read.CommitData;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.dataloading.KoPeMeDataHelper;
import de.dagere.peass.measurement.statistics.ConfidenceIntervalInterpretion;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;
import de.dagere.peass.measurement.statistics.data.DescribedChunk;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.measurement.utils.RunCommandWriterRCA;
import de.dagere.peass.measurement.utils.RunCommandWriterSlurmRCA;
import de.dagere.peass.utils.Constants;

/**
 * Reads changes of fulldata - data need to be cleaned!
 * 
 * @author reichelt
 *
 */
public class ChangeReader {

   private static final Logger LOG = LogManager.getLogger(ChangeReader.class);

   private int folderMeasurements = 0;
   private int measurements = 0;
   private int testcases = 0;

   private final StatisticsConfig config;

   private double minChange = 0;
   
   private final CommitData allData = new CommitData();
   private final ResultsFolders resultsFolders;

   private final RunCommandWriterRCA runCommandWriter;
   private final RunCommandWriterSlurmRCA runCommandWriterSlurm;
   private final SelectedTests selectedTests;

   private Map<String, TestSet> tests;
   
   final ProjectChanges changes;
   final ProjectStatistics statistics;

   public ChangeReader(final ResultsFolders resultsFolders, final SelectedTests selectedTests, StatisticsConfig config) throws FileNotFoundException {
      this.selectedTests = selectedTests;
      this.resultsFolders = resultsFolders;
      File statisticsFolder = resultsFolders.getStatisticsFile().getParentFile();
      if (selectedTests.getUrl() != null && !selectedTests.getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(statisticsFolder, "run-rca-" + resultsFolders.getProjectName() + ".sh"));
         runCommandWriter = new RunCommandWriterRCA(new MeasurementConfig(30), runCommandPrinter, "default", selectedTests);
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticsFolder, "run-rca-slurm-" + resultsFolders.getProjectName() + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(new MeasurementConfig(30), runCommandPrinterRCA, "default", selectedTests);
      } else {
         runCommandWriter = null;
         runCommandWriterSlurm = null;
      }
      this.config = config;
      
      changes = new ProjectChanges(config, new CommitComparatorInstance(selectedTests));
      statistics = new ProjectStatistics(new CommitComparatorInstance(selectedTests));
   }
   
   public ChangeReader(final ResultsFolders resultsFolders, final SelectedTests selectedTests, StatisticsConfig config, ProjectChanges oldChanges, ProjectStatistics oldStatistics) throws FileNotFoundException {
      this.selectedTests = selectedTests;
      this.resultsFolders = resultsFolders;
      File statisticsFolder = resultsFolders.getStatisticsFile().getParentFile();
      if (selectedTests.getUrl() != null && !selectedTests.getUrl().isEmpty()) {
         final PrintStream runCommandPrinter = new PrintStream(new File(statisticsFolder, "run-rca-" + resultsFolders.getProjectName() + ".sh"));
         runCommandWriter = new RunCommandWriterRCA(new MeasurementConfig(30), runCommandPrinter, "default", selectedTests);
         final PrintStream runCommandPrinterRCA = new PrintStream(new File(statisticsFolder, "run-rca-slurm-" + resultsFolders.getProjectName() + ".sh"));
         runCommandWriterSlurm = new RunCommandWriterSlurmRCA(new MeasurementConfig(30), runCommandPrinterRCA, "default", selectedTests);
      } else {
         runCommandWriter = null;
         runCommandWriterSlurm = null;
      }
      this.config = config;
      
      changes = oldChanges;
      statistics = oldStatistics;
   }

   public ChangeReader(final ResultsFolders resultsFolders, final RunCommandWriterRCA runCommandWriter, final RunCommandWriterSlurmRCA runCommandWriterSlurm,
         final SelectedTests selectedTests, StatisticsConfig config) throws FileNotFoundException {
      this.resultsFolders = resultsFolders;
      this.runCommandWriter = runCommandWriter;
      this.runCommandWriterSlurm = runCommandWriterSlurm;
      this.selectedTests = selectedTests;
      this.config = config;
      
      changes = new ProjectChanges(config, new CommitComparatorInstance(selectedTests));
      statistics = new ProjectStatistics(new CommitComparatorInstance(selectedTests));
   }

   public void setTests(final Map<String, TestSet> tests) {
      this.tests = tests;
   }

   public ChangeReader(final String projectName, final SelectedTests selectedTests) {
      this.resultsFolders = null;
      runCommandWriter = null;
      runCommandWriterSlurm = null;
      this.selectedTests = selectedTests;
      
      this.config = new StatisticsConfig();
      
      changes = new ProjectChanges(config, new CommitComparatorInstance(selectedTests));
      statistics = new ProjectStatistics(new CommitComparatorInstance(selectedTests));
   }

   public ProjectChanges readFolder(final File measurementFolder) {
      LOG.debug("Reading from " + measurementFolder.getAbsolutePath());
      readFile(measurementFolder);

      changes.setTestcaseCount(measurements);
      changes.setCommitCount(statistics.getStatistics().size());
      writeResults(measurementFolder);
      System.out.println("Measurements: " + folderMeasurements + " Tests: " + testcases + " (last read: " + measurementFolder + ")");
      return changes;
   }

   private void readFile(final File measurementFolder) {
      if (measurementFolder.isDirectory()) {
         for (final File file : measurementFolder.listFiles()) {
            String fileName = file.getName();
            if (fileName.matches("[0-9]+_[0-9]+")) {
               File slurmCleanFolder = new File(file, "peass/clean");
               readCleanFolder(measurementFolder, slurmCleanFolder);
            } else if (fileName.equals("clean")) {
               readCleanFolder(measurementFolder, file);
            } else if ((fileName.endsWith(".json") || fileName.endsWith(".xml")) 
                  && !fileName.equals("changes.json") && !fileName.equals("statistics.json")) {
               readFile(measurementFolder, file);
            }
         }
      } else {
         if (measurementFolder.getName().endsWith(".json") || measurementFolder.getName().endsWith(".xml")) {
            readFile(measurementFolder, measurementFolder);
         }
      }
   }

   private void readCleanFolder(final File measurementFolder, final File cleanParentFolder) {
      LOG.info("Handling: {}", cleanParentFolder);
      for (File cleanedFolder : cleanParentFolder.listFiles()) {
         for (File childFile : cleanedFolder.listFiles()) {
            if (childFile.getName().endsWith(".json") || childFile.getName().endsWith(".xml")) {
               readFile(measurementFolder, childFile);
            }
         }
      }
   }

   private void writeResults(final File measurementFolder) {
      if (resultsFolders != null) {
         final String measurementFolderName = measurementFolder.getName();
         String executorName = measurementFolderName.substring(measurementFolderName.lastIndexOf(File.separator) + 1);
         if (executorName.endsWith("_peass")) {
            executorName = executorName.substring(0, executorName.length() - "_peass".length());
         }
         final File resultfile = resultsFolders.getChangeFile();
         final File statisticFile = resultsFolders.getStatisticsFile();
         try {
            Constants.OBJECTMAPPER.writeValue(resultfile, changes);
            Constants.OBJECTMAPPER.writeValue(statisticFile, statistics);
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   private void readFile(final File measurementFolder, final File file) {
      final Kopemedata data = new JSONDataLoader(file).getFullData();
      for (final TestMethod testcaseMethod : data.getMethods()) {
         LOG.info(file.getAbsolutePath());
         readTestcase(measurementFolder.getName(), data, testcaseMethod);
         measurements += testcaseMethod.getDatacollectorResults().get(0).getChunks().size();
         testcases++;
      }
   }

   private int readTestcase(final String fileName, final Kopemedata data, final TestMethod testcaseMethod) {
      for (final VMResultChunk chunk : testcaseMethod.getDatacollectorResults().get(0).getChunks()) {
         folderMeasurements++;
         final String[] commits = KoPeMeDataHelper.getCommits(chunk, selectedTests);
         LOG.debug(commits[1]);
         if (commits[1] != null) {
            readChunk(fileName, data, chunk, commits);
         }
      }
      return folderMeasurements;
   }

   private void readChunk(final String fileName, final Kopemedata data, final VMResultChunk chunk,
         final String[] commits) {
      final DescribedChunk describedChunk = new DescribedChunk(chunk, commits[0], commits[1]);
      describedChunk.removeOutliers();

      if (describedChunk.getDescPrevious().getN() > 1 && describedChunk.getDescCurrent().getN() > 1) {
         getIsChange(fileName, data, commits, describedChunk);
      } else {
         LOG.error("Too few measurements: {} - {} measurements, {} - {} measurements ", commits[0], describedChunk.getDescPrevious().getN(), commits[1],
               describedChunk.getDescCurrent().getN());
      }
   }

   public void getIsChange(final String fileName, final Kopemedata data, final String[] commits, final DescribedChunk describedChunk) {
      LOG.debug(data.getClazz());
      final TestcaseStatistic statistic = describedChunk.getStatistic(config);
      statistic.setPredecessor(commits[0]);
      // if (! (statistic.getTvalue() == Double.NaN)){
      CompareData cd = new CompareData(describedChunk.getPrevious(), describedChunk.getCurrent());
      final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(cd);
      final TestMethodCall testcase = getTestcase(data, commits, describedChunk);

      final double diff = describedChunk.getDiff();
      final boolean isBigEnoughDiff = Math.abs(diff) > minChange;
      allData.addStatistic(commits[1], testcase, fileName, statistic,
            statistic.isChange() && isBigEnoughDiff,
            !confidenceResult.equals(Relation.EQUAL));
      statistics.addMeasurement(commits[1], testcase, statistic);
      
      if (statistic.getDeviationOld() / statistic.getMeanOld() > config.getMaximumRelativeDeviation()) {
         System.out.println("Skipping " + testcase + " because of deviation: " + statistic.getDeviationOld() / statistic.getMeanOld());
         return;
      }
      if (statistic.getDeviationCurrent() / statistic.getMeanCurrent() > config.getMaximumRelativeDeviation()) {
         System.out.println("Skipping " + testcase + " because of deviation: " + statistic.getDeviationCurrent() / statistic.getMeanCurrent());
         return;
      }
      
      if (statistic.isChange() && isBigEnoughDiff) {
         changes.addChange(testcase, commits[1],
               confidenceResult,
               statistic.isChange() ? Relation.GREATER_THAN : Relation.EQUAL,
               describedChunk.getDescPrevious().getMean(), diff,
               statistic.getTvalue(),
               statistic.getVMs());

         writeRunCommands(commits, describedChunk, testcase);
      }
      
   }

   private TestMethodCall getTestcase(final Kopemedata data, final String[] commits, final DescribedChunk describedChunk) {
      TestMethodCall testcase = new TestMethodCall(data);
      if (tests != null) {
         TestSet testsOfThisVersion = tests.get(commits[1]);
         for (TestMethodCall test : testsOfThisVersion.getTestMethods()) {
            if (paramsEqual(testcase.getParams(), test)) {
               if (test.getClazz().equals(testcase.getClazz()) && test.getMethod().equals(testcase.getMethod())) {
                  testcase = test;
               }
            }
         }
      }
      return testcase;
   }

   private boolean paramsEqual(final String paramString, final TestMethodCall test) {
      boolean bothNull = test.getParams() == paramString && test.getParams() == null;
      boolean stringEmptyAndParamsNull = "".equals(paramString) && test.getParams() == null;
      return bothNull
            || stringEmptyAndParamsNull
            || (test.getParams() != null && test.getParams().equals(paramString)); // last should only be evaluated if both are not null
   }

   private void writeRunCommands(final String[] commits, final DescribedChunk describedChunk, final TestMethodCall testcase) {
      if (runCommandWriter != null) {
         final VMResult exampleResult = describedChunk.getCurrent().get(0);
         final int iterations = (int) exampleResult.getIterations();
         final int repetitions = (int) exampleResult.getRepetitions();

         final int commitIndex = Arrays.binarySearch(selectedTests.getCommitNames(), commits[1]);

         runCommandWriter.getConfig().setIterations(iterations);
         runCommandWriter.getConfig().setRepetitions(repetitions);
         
         runCommandWriter.createSingleMethodCommand(commitIndex, commits[1], testcase.getExecutable());

         runCommandWriterSlurm.createSingleMethodCommand(commitIndex, commits[1], testcase.getExecutable(),
               iterations, repetitions, runCommandWriter.getConfig().getVms());
      }
   }

   public CommitData getAllData() {
      return allData;
   }

   public double getMinChange() {
      return minChange;
   }

   public void setMinChange(final double minChange) {
      this.minChange = minChange;
   }
}
