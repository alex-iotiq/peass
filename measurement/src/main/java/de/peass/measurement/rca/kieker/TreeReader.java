package de.peass.measurement.rca.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.PeASSFilter;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.tools.trace.analysis.filter.traceReconstruction.TraceReconstructionFilter;

public class TreeReader extends KiekerResultManager {

   private boolean ignoreEOIs = true;
   private final MeasurementConfiguration config;
   
   TreeReader(final File projectFolder, final MeasurementConfiguration config) throws InterruptedException, IOException {
      super(new PeASSFolders(projectFolder), config.getTimeoutInMinutes(), config.getTestGoal());
      this.config = config;
   }

   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   @Override
   public void executeKoPeMeKiekerRun(final TestSet testsToUpdate, final String version) throws IOException, XmlPullParserException, InterruptedException {
      executor.loadClasses();
      super.executeKoPeMeKiekerRun(testsToUpdate, version);
   }

   public CallTreeNode getTree(final TestCase testcase, final String version)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException, InterruptedException {
      executeMeasurements(testcase, version);
      
      File resultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      File kiekerTraceFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder);

      CallTreeNode root = readTree(testcase, kiekerTraceFolder);
      return root;
   }

   private CallTreeNode readTree(final TestCase testcase, final File kiekerTraceFolder) throws AnalysisConfigurationException, IOException, XmlPullParserException {
      KiekerReader reader = new KiekerReader(kiekerTraceFolder);
      reader.initBasic();
      TraceReconstructionFilter traceReconstructionFilter = reader.initTraceReconstruction();

      final ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder());
      
      AnalysisController analysisController = reader.getAnalysisController();
      final TreeFilter treeFilter = new TreeFilter(null, analysisController, testcase, ignoreEOIs, config, mapping);
      analysisController.connect(traceReconstructionFilter, TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
            treeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

      analysisController.run();

      CallTreeNode root = treeFilter.getRoot();
      if (root == null) {
         throw new RuntimeException("An error occured - root node of " + testcase + " could not be identified!");
      }
      return root;
   }

   private void executeMeasurements(final TestCase testcase, final String version) throws IOException, XmlPullParserException, InterruptedException {
      executeKoPeMeKiekerRun(new TestSet(testcase), version);
   }

}
