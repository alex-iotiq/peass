package de.dagere.peass.execution.maven;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;

public class MavenCleaner {

   private static final Logger LOG = LogManager.getLogger(MavenCleaner.class);

   private final PeassFolders folders;
   private final EnvironmentVariables env;

   public MavenCleaner(final PeassFolders folders, final EnvironmentVariables env) {
      this.folders = folders;
      this.env = env;
   }

   public void clean(final File logFile) {
      checkProjectFolder();
      checkLogParent(logFile);

      final ProcessBuilder pbClean = buildProcess(logFile);

      cleanSafely(pbClean);
   }

   private ProcessBuilder buildProcess(final File logFile) {
      final String[] originalsClean = new String[] { env.fetchMavenCall(), "--batch-mode", "clean" };
      final ProcessBuilder pbClean = new ProcessBuilder(originalsClean);
      pbClean.directory(folders.getProjectFolder());
      if (logFile != null) {
         pbClean.redirectOutput(Redirect.appendTo(logFile));
         pbClean.redirectError(Redirect.appendTo(logFile));
      }
      return pbClean;
   }

   private void checkProjectFolder() {
      if (!folders.getProjectFolder().exists()) {
         throw new RuntimeException("Can not execute clean - folder " + folders.getProjectFolder().getAbsolutePath() + " does not exist");
      } else {
         LOG.debug("Folder {} exists {} and is directory {} - cleaning should be possible",
               folders.getProjectFolder().getAbsolutePath(),
               folders.getProjectFolder().exists(),
               folders.getProjectFolder().isDirectory());
      }
   }

   private void checkLogParent(final File logFile) {
      File logParentFile = logFile.getParentFile();
      if (!logParentFile.exists()) {
         if (!logParentFile.mkdirs()) {
            throw new RuntimeException("Could not create log parent directory: " + logParentFile);
         }
      }
   }

   private void cleanSafely(final ProcessBuilder pbClean) {
      boolean finished = false;
      int count = 0;
      while (!finished && count < 10) {
         try {
            Process processClean = pbClean.start();
            finished = processClean.waitFor(60, TimeUnit.MINUTES);
            if (!finished) {
               LOG.info("Clean process " + processClean + " was not finished successfully; trying again to clean");
               processClean.destroyForcibly();
            }
            count++;
         } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
         }

      }
   }
}
