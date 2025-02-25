package de.dagere.peass.execution.utils;

import java.io.File;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestShortener;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public abstract class KoPeMeExecutor extends TestExecutor {

   private static final Logger LOG = LogManager.getLogger(KoPeMeExecutor.class);

   protected final JUnitTestTransformer testTransformer;

   public KoPeMeExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      this.testTransformer = testTransformer;
   }

   public void transformTests() {
      final List<File> modules = getModules().getModules();
      testTransformer.determineVersions(modules);
      testTransformer.transformTests();
   }

   protected String getTestGoal() {
      String testGoal;
      ExecutionConfig executionConfig = testTransformer.getConfig().getExecutionConfig();
      if (isAndroid) {
         testGoal = executionConfig.getTestGoal() != null ? executionConfig.getTestGoal() : "testRelease";
      } else {
         testGoal = executionConfig.getTestGoal() != null ? executionConfig.getTestGoal() : "test";
      }
      return testGoal;
   }

   protected abstract void runTest(File moduleFolder, final File logFile, TestMethodCall test, final String testname, final long timeout);

   protected void runMethod(final File logFolder, final TestMethodCall test, final File moduleFolder, final long timeout) {
      try (final JUnitTestShortener shortener = new JUnitTestShortener(testTransformer, moduleFolder, test.toEntity(), test.getMethod())) {
         if (testTransformer.getConfig().isDirectlyMeasureKieker()) {
            File fileToInstrument = shortener.getCalleeClazzFile();
            
            HashSet<String> includedPatterns = new HashSet<>();
            includedPatterns.add("* " + test.getClazz() + "." + test.getMethod() + "()");
            InstrumentationConfiguration configuration = new InstrumentationConfiguration(AllowedKiekerRecord.DURATION, true, false, false, includedPatterns, null, false, testTransformer.getConfig().getRepetitions(), false);
            InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(configuration);
            instrumenter.instrument(fileToInstrument);
         }
         
         LOG.info("Cleaning...");
         final File cleanFile = getCleanLogFile(logFolder, test);
         clean(cleanFile);

         final File methodLogFile = getMethodLogFile(logFolder, test);
         runTest(moduleFolder, methodLogFile, test, test.getClazz(), timeout);
      } catch (Exception e1) {
         e1.printStackTrace();
      }
   }

   @Override
   public JUnitTestTransformer getTestTransformer() {
      return testTransformer;
   }
}
