package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.dagere.peass.TestConstants;
import de.dagere.peass.ci.helper.GitProjectBuilder;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencytests.DependencyTestConstants;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitUtils;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ContinuousDependencyReaderIT {

   public static final ResultsFolders resultsFolders = new ResultsFolders(new File("target/results-test"), "test");

   private static GitProjectBuilder builder;

   @BeforeAll
   public static void cleanDependencies() throws IOException, InterruptedException {
      FileUtils.deleteDirectory(resultsFolders.getStaticTestSelectionFile().getParentFile());
      Assert.assertFalse(resultsFolders.getStaticTestSelectionFile().exists());

      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      builder = new GitProjectBuilder(TestConstants.CURRENT_FOLDER, new File("../dependency/src/test/resources/dependencyIT/basic_state"));
   }

   @Order(1)
   @Test
   public void testBasicVersionReading() throws Exception {
      builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/normal_change"), "test 1");

      CommitIteratorGit iterator = new CommitIteratorGit(TestConstants.CURRENT_FOLDER);
      iterator.goToFirstCommit();
      iterator.goToNextCommit();

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setEndcommit(iterator.getCommitName());
      executionConfig.setStartcommit(iterator.getPredecessor());

      ContinuousDependencyReader reader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG_WITH_VIEWS, executionConfig, new KiekerConfig(true),
            new PeassFolders(TestConstants.CURRENT_FOLDER), resultsFolders, new EnvironmentVariables());
      StaticTestSelection dependencies = reader.getDependencies(iterator, "");

      final String lastTag = builder.getTags().get(builder.getTags().size() - 1);
      checkVersion(dependencies, lastTag, 1);

      ExecutionData executions = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);
      Assert.assertEquals(2, executions.getCommits().size());
      System.out.println(executions.getCommits().keySet());
   }

   @Order(2)
   @Test
   public void testAnotherVersion() throws Exception {
      final String prevTag = builder.getTags().get(builder.getTags().size() - 1);
      GitUtils.goToCommit(prevTag, TestConstants.CURRENT_FOLDER);

      String newVersion = builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/basic_state"), "test 2");

      CommitIteratorGit iterator = new CommitIteratorGit(TestConstants.CURRENT_FOLDER);

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setEndcommit(newVersion);
      executionConfig.setStartcommit(iterator.getPredecessor());

      final ContinuousDependencyReader spiedReader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG_WITH_VIEWS, executionConfig,
            new KiekerConfig(true),
            new PeassFolders(TestConstants.CURRENT_FOLDER), resultsFolders, new EnvironmentVariables());
      StaticTestSelection dependencies = spiedReader.getDependencies(iterator, "");

      final String lastTag = builder.getTags().get(builder.getTags().size() - 1);
      checkVersion(dependencies, lastTag, 2);

      ExecutionData executions = Constants.OBJECTMAPPER.readValue(resultsFolders.getTraceTestSelectionFile(), ExecutionData.class);
      Assert.assertEquals(3, executions.getCommits().size());
   }

   @Order(3)
   @Test
   public void testEmptyVersion() throws Exception {
      final String prevTag = builder.getTags().get(builder.getTags().size() - 1);
      GitUtils.goToCommit(prevTag, TestConstants.CURRENT_FOLDER);

      String newVersion = builder.addVersion(new File("../dependency/src/test/resources/dependencyIT/only_comment_change"), "test 2");

      CommitIteratorGit iterator = new CommitIteratorGit(TestConstants.CURRENT_FOLDER);

      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setEndcommit(newVersion);
      executionConfig.setStartcommit(iterator.getPredecessor());

      ContinuousDependencyReader reader = new ContinuousDependencyReader(DependencyTestConstants.DEFAULT_CONFIG_WITH_VIEWS, executionConfig, new KiekerConfig(true),
            new PeassFolders(TestConstants.CURRENT_FOLDER), resultsFolders, new EnvironmentVariables());
      RTSResult result = reader.getTests(iterator, "", newVersion, new MeasurementConfig(1));
      Set<TestMethodCall> tests = result.getTests();

      Assert.assertEquals(tests.size(), 0);
   }

   public static void checkVersion(final StaticTestSelection dependencies, final String newestVersion, final int versions) {
      Assert.assertTrue(resultsFolders.getStaticTestSelectionFile().exists());
      MatcherAssert.assertThat(dependencies.getCommits(), Matchers.aMapWithSize(versions));

      MatcherAssert.assertThat(dependencies.getCommits().get(newestVersion), Matchers.notNullValue());
      final TestSet testSet = getTestset(dependencies, newestVersion);
      Assert.assertEquals(new TestMethodCall("defaultpackage.TestMe", "testMe"), testSet.getTestMethods().toArray()[0]);
   }

   private static TestSet getTestset(final StaticTestSelection dependencies, final String newestVersion) {
      final TestSet testSet = dependencies.getCommits().get(newestVersion)
            .getChangedClazzes()
            .get(new ChangedEntity("defaultpackage.NormalDependency", "", ""));
      return testSet;
   }
}
