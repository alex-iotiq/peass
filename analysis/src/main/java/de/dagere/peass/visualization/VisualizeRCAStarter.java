package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.dagere.peass.analysis.all.RepoFolders;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Visualizes the root-cause-analysis-tree as HTML page", name = "visualizerca")
public class VisualizeRCAStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(VisualizeRCAStarter.class);

   private static final ObjectMapper MAPPER = new ObjectMapper();
   static {
      MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
   }

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   @Option(names = { "-visualizeFull", "--visualizeFull" }, description = "Whether to visualize full tree")
   protected boolean visualizeFull;

   @Option(names = { "-propertyFolder", "--propertyFolder" }, description = "Path to property folder", required = false)
   protected File propertyFolder;

   @Option(names = { "-out", "--out" }, description = "Path for storage of results, default results", required = false)
   protected File resultFolder = new File("results");

   // TODO Fix dirty hack
   final String projectName = "commons-fileupload";

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new VisualizeRCAStarter());
      System.exit(commandLine.execute(args));
   }

   public VisualizeRCAStarter() {
   }

   public VisualizeRCAStarter(final File[] data, final File resultFolder) {
      this.data = data;
      this.resultFolder = resultFolder;
   }

   @Override
   public Void call() throws Exception {
      if (!resultFolder.exists()) {
         resultFolder.mkdir();
      }

      List<File> rcaFolderToHandle = new RCAFolderSearcher(data).searchRCAFiles();
      LOG.info("Handling: " + rcaFolderToHandle);
      for (File rcaFolder : rcaFolderToHandle) {
         visualizeRCAFile(resultFolder, rcaFolder);
      }
      List<File> peassFolderToHandle = new RCAFolderSearcher(data).searchPeassFiles();
      for (File peassFolder : peassFolderToHandle) {
         visualizeRegularMeasurementFile(peassFolder);
      }

      return null;
   }

   private void visualizeRegularMeasurementFile(final File peassFolder) throws JsonProcessingException, FileNotFoundException, IOException {
      VisualizeRegularMeasurement measurementVisualizer = new VisualizeRegularMeasurement(resultFolder);
      measurementVisualizer.analyzeFile(peassFolder);
   }

   private void getFullTree(final RCAGenerator rcaGenerator, final CauseSearchData data, final File treeFolder) throws IOException, JsonParseException, JsonMappingException {
      if (treeFolder.exists()) {
         final File potentialCacheFileOld = new File(treeFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommitOld());
         final File potentialCacheFile = new File(treeFolder, data.getMeasurementConfig().getFixedCommitConfig().getCommit());

         final CallTreeNode rootPredecessor = Constants.OBJECTMAPPER.readValue(potentialCacheFileOld, CallTreeNode.class);
         final CallTreeNode rootVersion = Constants.OBJECTMAPPER.readValue(potentialCacheFile, CallTreeNode.class);

         rcaGenerator.setFullTree(rootPredecessor, rootVersion);
      }
   }

   private void visualizeRCAFile(final File versionResultFolder, final File treeFile)
         throws JsonParseException, JsonMappingException, IOException, JsonProcessingException, FileNotFoundException {
      final CauseSearchFolders folders = getCauseSearchFolders(treeFile);

      final RCAGenerator rcaGenerator = new RCAGenerator(treeFile, versionResultFolder, folders);
      final File propertyFolder = getPropertyFolder(projectName);
      rcaGenerator.setPropertyFolder(propertyFolder);

      if (visualizeFull) {
         final CauseSearchData data = rcaGenerator.getData();

         final File treeFolder = folders.getTreeCacheFolder(data.getMeasurementConfig().getFixedCommitConfig().getCommit(), data.getCauseConfig().getTestCase());
         getFullTree(rcaGenerator, data, treeFolder);
      }
      rcaGenerator.createVisualization();
   }

   private CauseSearchFolders getCauseSearchFolders(final File treeFile) {
      final File projectFolder = treeFile.getAbsoluteFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
      System.out.println(projectFolder + " " + treeFile);

      final CauseSearchFolders folders;
      if (projectFolder.getName().contentEquals("rca")) {
         folders = new CauseSearchFolders(projectFolder.getParentFile());
      } else {
         folders = new CauseSearchFolders(projectFolder);
      }
      return folders;
   }

   private File getPropertyFolder(final String projectName) {
      final File generatedPropertyFolder;
      if (propertyFolder != null) {
         if (!propertyFolder.exists()) {
            throw new RuntimeException("Property folder " + propertyFolder + " was defined, but did not exist!");
         }
         generatedPropertyFolder = propertyFolder;
      } else {
         generatedPropertyFolder = new File(new RepoFolders().getPropertiesFolder(), "properties" + File.separator + projectName);
      }
      return generatedPropertyFolder;
   }

   public File[] getData() {
      return data;
   }

   public void setData(final File[] data) {
      this.data = data;
   }

   public File getPropertyFolder() {
      return propertyFolder;
   }

   public void setPropertyFolder(final File propertyFolder) {
      this.propertyFolder = propertyFolder;
   }

   public File getResultFolder() {
      return resultFolder;
   }

   public void setResultFolder(final File resultFolder) {
      this.resultFolder = resultFolder;
   }

}
