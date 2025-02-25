package de.dagere.peass;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.PairProcessor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.measurement.dependencyprocessors.DependencyTester;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Runs the dependency test by running the test, where something could have changed, pairwise for every new commit. This makes it faster to get potential change candidates, but it
 * takes longer for a whole project.
 * 
 * @author reichelt
 *
 */
@Command(description = "Measures the defined tests and commits until the number of VMs is reached", name = "measure")
public class MeasureStarter extends PairProcessor {

   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;
   
   @Mixin
   protected StatisticsConfigMixin statisticConfigMixin;

   @Option(names = { "-test", "--test" }, description = "Name of the test to execute")
   String testName;

   private static final Logger LOG = LogManager.getLogger(MeasureStarter.class);

   protected DependencyTester tester;
   private final List<String> commits = new LinkedList<>();
   private int startindex, endindex;
   private TestMethodCall test;

   @Override
   public Void call() throws Exception {
      super.call();
      final MeasurementConfig measurementConfiguration = createConfig();
      createTester(measurementConfiguration);

      if (testName != null) {
         test = TestMethodCall.createFromString(testName);
         LOG.info("Test: {}", test);
      } else {
         test = null;
      }

      commits.add(staticTestSelection.getInitialcommit().getCommit());

      staticTestSelection.getCommits().keySet().forEach(commit -> commits.add(commit));

      startindex = getStartVersionIndex();
      endindex = getEndVersion();

      processCommandline();
      return null;
   }

   private void createTester(final MeasurementConfig measurementConfiguration) throws IOException {
      if (measurementConfigMixin.getDuration() != 0) {
         throw new RuntimeException("Time-based running currently not supported; eventually fix commented-out code to get it running again");
      } else {
         EnvironmentVariables env = new EnvironmentVariables(measurementConfiguration.getExecutionConfig().getProperties());
         CommitComparatorInstance comparator = new CommitComparatorInstance(staticTestSelection);
         tester = new DependencyTester(folders, measurementConfiguration, env, comparator);
      }
   }

   private MeasurementConfig createConfig() {
      final MeasurementConfig measurementConfiguration = new MeasurementConfig(measurementConfigMixin, executionMixin, 
            statisticConfigMixin, new KiekerConfigMixin());
      return measurementConfiguration;
   }

   /**
    * Calculates the index of the start commit
    * 
    * @return index of the start commit
    */
   private int getStartVersionIndex() {
      int currentStartindex = startcommit != null ? commits.indexOf(startcommit) : 0;
      // Only bugfix if static selection file and execution file do not fully match
      if (executionData != null) {
         if (startcommit != null && currentStartindex == -1) {
            String potentialStart = "";
            if (executionData.getCommits().containsKey(startcommit)) {
               for (final String executionVersion : executionData.getCommits().keySet()) {
                  for (final String dependencyVersion : staticTestSelection.getCommits().keySet()) {
                     if (dependencyVersion.equals(executionVersion)) {
                        potentialStart = dependencyVersion;
                        break;
                     }
                  }
                  if (executionVersion.equals(startcommit)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executefile, next commit in static selection file: {}", potentialStart);
            currentStartindex = commits.indexOf(potentialStart);
            if (currentStartindex == -1) {
               throw new RuntimeException("Did not find " + startcommit + " in given PRONTO-files!");
            }
         }
      }
      return currentStartindex;
   }

   /**
    * Calculates the index of the end commit.
    * 
    * @return index of the end commit
    */
   private int getEndVersion() {
      int currentEndindex = endcommit != null ? commits.indexOf(endcommit) : commits.size();
      // Only bugfix if static selection file and execution file do not fully match
      if (executionData != null) {
         if (endcommit != null && currentEndindex == -1) {
            String potentialStart = "";
            if (executionData.getCommits().containsKey(endcommit)) {
               for (final String executionVersion : executionData.getCommits().keySet()) {
                  boolean next = false;
                  for (final String dependencyVersion : staticTestSelection.getCommits().keySet()) {
                     if (next) {
                        potentialStart = dependencyVersion;
                        break;
                     }
                     if (dependencyVersion.equals(executionVersion)) {
                        next = true;
                     }
                  }
                  if (executionVersion.equals(endcommit)) {
                     break;
                  }
               }
            }
            LOG.debug("Version only in executionfile, next commit in static selection file: {}", potentialStart);
            currentEndindex = commits.indexOf(potentialStart);
         }
      }
      return currentEndindex;
   }

   @Override
   protected void processVersion(final String commit, final CommitStaticSelection commitinfo) {
      LOG.debug("Configuration: VMs: {} Warmup: {} Iterations: {} Repetitions: {}", measurementConfigMixin.getVms(),
            measurementConfigMixin.getWarmup(), measurementConfigMixin.getIterations(), measurementConfigMixin.getRepetitions());
      try {
         final int currentIndex = commits.indexOf(commit);
         final boolean executeThisVersion = currentIndex >= startindex && currentIndex <= endindex;

         LOG.trace("Processing Version {} Executing Tests: {}", commit, executeThisVersion);

         final Set<TestMethodCall> testcases = commitinfo.getTests().getTestMethods();
         final String commitOld = commitinfo.getPredecessor();

         for (final TestMethodCall testcase : testcases) {
            if (executeThisVersion) {
               if (lastTestcaseCalls.containsKey(testcase)) {
                  boolean executeThisTest = true;
                  if (test != null) {
                     executeThisTest = checkTestName(testcase, executeThisTest);
                  }

                  if (executeThisTest) {
                     executeThisTest = checkExecutionData(commit, testcase, executeThisTest);
                  }
                  if (executeThisTest) {
                     tester.setVersions(commit, commitOld);
                     tester.evaluate((TestMethodCall) testcase);
                  }
               }
            }
            lastTestcaseCalls.put(testcase, commit);
         }
      } catch (IOException | InterruptedException  | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected void postEvaluate() {
      tester.postEvaluate();
   }

   public boolean checkExecutionData(final String commit, final TestMethodCall testcase, boolean executeThisTest) {
      if (executionData != null) {
         final TestSet calls = executionData.getCommits().get(commit);
         boolean hasChanges = false;
         if (calls != null) {
            for (final Entry<TestClazzCall, Set<String>> clazzCalls : calls.entrySet()) {
               final String changedClazz = clazzCalls.getKey().getClazz();
               if (changedClazz.equals(testcase.getClazz()) && clazzCalls.getValue().contains(testcase.getMethodWithParams())) {
                  hasChanges = true;
               }
            }
         }
         if (!hasChanges) {
            LOG.debug("Skipping " + testcase + " because of execution-JSON in " + commit);
            executeThisTest = false;
         }
      }
      return executeThisTest;
   }

   public boolean checkTestName(final TestCase testcase, boolean executeThisTest) {
      LOG.debug("Checking " + test + " " + testcase);
      if (!test.equals(testcase)) {
         executeThisTest = false;
         LOG.debug("Skipping: " + testcase);
      } else {
         LOG.debug("Success!");
      }
      return executeThisTest;
   }

   public static void main(final String[] args) throws  IOException {
      final MeasureStarter command = new MeasureStarter();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

}
