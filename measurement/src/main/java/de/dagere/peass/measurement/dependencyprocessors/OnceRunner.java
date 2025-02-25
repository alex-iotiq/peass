package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class OnceRunner {

   private static final Logger LOG = LogManager.getLogger(OnceRunner.class);

   protected final PeassFolders folders;
   private final VersionControlSystem vcs;
   protected final TestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected final ResultOrganizer currentOrganizer;
   private final KiekerResultHandler resultHandler;

   public OnceRunner(final PeassFolders folders, final TestExecutor testExecutor, final ResultOrganizer currentOrganizer, final KiekerResultHandler resultHandler) {
      this.folders = folders;
      this.vcs = folders.getVCS();
      this.testTransformer = testExecutor.getTestTransformer();
      this.testExecutor = testExecutor;
      this.currentOrganizer = currentOrganizer;
      this.resultHandler = resultHandler;
      
      try {
         FileUtils.cleanDirectory(folders.getTempDir());
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public void runOnce(final TestMethodCall testcase, final String commit, final int vmid, final File logFolder) {
      if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         GitUtils.goToCommit(commit, folders.getProjectFolder());
      }

      final File vmidFolder = initVMFolder(commit, vmid, logFolder);

      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         testExecutor.loadClasses();
      }
      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));
      final long outerTimeout = 10 + (int) (this.testTransformer.getConfig().getTimeoutInSeconds() * 1.2);
      testExecutor.executeTest(testcase, vmidFolder, outerTimeout);

      if (testTransformer.getConfig().isDirectlyMeasureKieker()) {
         DirectKiekerMeasurementTransformer measurementTransformer = new DirectKiekerMeasurementTransformer(folders);
         measurementTransformer.transform(testcase);
      }

      LOG.debug("Handling Kieker results");
      resultHandler.handleKiekerResults(commit, currentOrganizer.getTempResultsFolder(commit));

      LOG.info("Organizing result paths");
      currentOrganizer.saveResultFiles(commit, vmid);

      cleanup();
   }

   private File initVMFolder(final String commit, final int vmid, final File logFolder) {
      File vmidFolder = new File(logFolder, "vm_" + vmid + "_" + commit);
      vmidFolder.mkdirs();

      LOG.info("Initial checkout finished, VM-Folder " + vmidFolder.getAbsolutePath() + " exists: " + vmidFolder.exists());
      return vmidFolder;
   }

   void cleanup() {
      emptyFolder(folders.getTempDir());
      emptyFolder(folders.getKiekerTempFolder());
      System.gc();
      try {
         Thread.sleep(1);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   public ResultOrganizer getCurrentOrganizer() {
      return currentOrganizer;
   }

   private void emptyFolder(final File tempDir) {
      for (final File createdTempFile : tempDir.listFiles()) {
         try {
            if (createdTempFile.isDirectory()) {
               FileUtils.deleteDirectory(createdTempFile);
            } else {
               createdTempFile.delete();
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }
}
