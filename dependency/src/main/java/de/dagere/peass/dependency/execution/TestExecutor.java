package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Base functionality for executing performance tests, both instrumented and not instrumented. The executor automates changing the buildfile, changing the tests both with and
 * without Kieker.
 * 
 * @author reichelt
 *
 */
public abstract class TestExecutor {

   private static final Logger LOG = LogManager.getLogger(TestExecutor.class);

   public static final String GENERATED_TEST_NAME = "GeneratedTest";

   protected final PeassFolders folders;
   protected File lastTmpFile;
   protected int jdk_version = 8;
   protected final TestTransformer testTransformer;
   protected List<String> existingClasses;
   protected Set<String> includedMethodPattern;
   protected boolean isAndroid;

   protected boolean buildfileExists = false;
   protected final EnvironmentVariables env;

   public TestExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      this.folders = folders;
      this.testTransformer = testTransformer;
      this.env = env;
   }

   public void setJDKVersion(final int jdk_version) {
      this.jdk_version = jdk_version;
   }

   public int getJDKVersion() {
      return jdk_version;
   }

   public abstract void prepareKoPeMeExecution(File logFile) throws IOException, InterruptedException, XmlPullParserException;

   public abstract void executeTest(final TestCase test, final File logFolder, long timeout);

   protected File getCleanLogFile(final File logFolder, final TestCase test) {
      File clazzLogFolder = getClazzLogFolder(logFolder, test);
      final File logFile = new File(clazzLogFolder, "clean" + File.separator + test.getMethod() + ".txt");
      if (!logFile.getParentFile().exists()) {
         logFile.getParentFile().mkdir();
      }
      return logFile;
   }

   protected File getMethodLogFile(final File logFolder, final TestCase test) {
      File clazzLogFolder = getClazzLogFolder(logFolder, test);
      final File logFile = new File(clazzLogFolder, test.getMethod() + ".txt");
      return logFile;
   }
   
   private File getClazzLogFolder(final File logFolder, final TestCase test) {
      File clazzLogFolder = new File(logFolder, "log_" + test.getClazz());
      if (!clazzLogFolder.exists()) {
         clazzLogFolder.mkdir();
      }
      return clazzLogFolder;
   }

   protected void prepareKiekerSource() throws IOException, XmlPullParserException, InterruptedException {
      if (testTransformer.getConfig().isUseKieker()) {
         final KiekerEnvironmentPreparer kiekerEnvironmentPreparer = new KiekerEnvironmentPreparer(includedMethodPattern, existingClasses, folders, testTransformer, getModules().getModules());
         kiekerEnvironmentPreparer.prepareKieker();
      }
   }

   private final List<String> aborted = new LinkedList<>();

   protected void execute(final String testname, final long timeoutInSeconds, final Process process) throws InterruptedException, IOException {
      if (timeoutInSeconds == -1) {
         LOG.info("Executing without timeout!");
         process.waitFor();
      } else if (timeoutInSeconds > 0) {
         LOG.debug("Executing: {} Timeout: {}", testname, timeoutInSeconds);
         process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
         if (process.isAlive()) {
            LOG.debug("Killing: {}", testname);
            process.destroyForcibly().waitFor();
            aborted.add(testname);
            FileUtils.writeStringToFile(new File(folders.getFullMeasurementFolder(), "aborted.txt"), aborted.toString(), Charset.defaultCharset());
         }
      } else {
         throw new RuntimeException("Illegal timeout: " + timeoutInSeconds);
      }
   }

   /**
    * Tells whether currently checkout out version has a buildfile - is only set correctly after isVersionRunning has been called for current version.
    * 
    * @return
    */
   public boolean doesBuildfileExist() {
      return buildfileExists;
   }

   public abstract boolean isVersionRunning(String version);

   /**
    * Deletes temporary files, in order to not get memory problems
    */
   public void deleteTemporaryFiles() {
      try {
         if (lastTmpFile != null && lastTmpFile.exists()) {
            final File[] tmpKiekerStuff = lastTmpFile.listFiles((FilenameFilter) new WildcardFileFilter("kieker*"));
            for (final File kiekerFolder : tmpKiekerStuff) {
               LOG.debug("Deleting: {}", kiekerFolder.getAbsolutePath());
               FileUtils.deleteDirectory(kiekerFolder);
            }
            FileUtils.deleteDirectory(lastTmpFile);
         }

      } catch (final IOException | IllegalArgumentException e) {
         LOG.info("Problems deleting last temp file..");
         e.printStackTrace();
      }
   }

   public abstract ProjectModules getModules();

   public List<String> getExistingClasses() {
      return existingClasses;
   }

   protected abstract void clean(final File logFile) throws IOException, InterruptedException;

   public void loadClasses() {
      existingClasses = new LinkedList<>();
      for (final File module : getModules().getModules()) {
         final List<String> currentClasses = ClazzFileFinder.getClasses(module);
         existingClasses.addAll(currentClasses);
      }
   }

   public File getProjectFolder() {
      return folders.getProjectFolder();
   }

   public boolean isAndroid() {
      return false;
   }

   public void setIncludedMethods(final Set<String> includedMethodPattern) {
      this.includedMethodPattern = includedMethodPattern;
   }

   public TestTransformer getTestTransformer() {
      return testTransformer;
   }
}
