package de.dagere.peass;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependencyprocessors.CommitProcessor;
import de.dagere.peass.utils.StreamGobbler;
import de.dagere.peass.vcs.GitUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Starts running every version just for downloading the dependencies. After all dependencies have been downloaded, most operations can be run locally (as long as the version
 * control system can operate locally).
 * 
 * @author reichelt
 *
 */
@Command(description = "Starts running every version just for downloading the dependencies. After all dependencies have been downloaded, most operations can be run locally (as long as the version" + 
      " control system can operate locally).", name = "downloadDependencies")
public class VersionRunStarter extends CommitProcessor {

   public VersionRunStarter() throws  JsonParseException, JsonMappingException, IOException {
      super();
   }

   @Override
   protected void processVersion(final String version, final CommitStaticSelection versioninfo) {
      GitUtils.goToCommit(version, folders.getProjectFolder());
      try {
         final Process p = Runtime.getRuntime().exec("mvn clean package -DskipTests=true", null, folders.getProjectFolder());
         StreamGobbler.showFullProcess(p);
      } catch (final IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }

   public static void main(final String[] args) throws  JsonParseException, JsonMappingException, IOException {
      VersionRunStarter command = new VersionRunStarter();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
      command.processCommandline();
   }
}
