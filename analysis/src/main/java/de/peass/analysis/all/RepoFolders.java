package de.peass.analysis.all;

import java.io.File;

import de.peass.utils.Constants;
import de.peran.analysis.helper.all.CleanAll;

public class RepoFolders {

   private final File DEFAULT_REPO = new File("/home/reichelt/daten3/diss/repos/");

   private final File dependencyFolder;
   private final File dataFolder;
   private final File reexecuteFolder;
   

   private final File propertiesRepo;
   private final File classificationFolder;
   private final File resultsFolder;
   private final File allViewFolder;

   public RepoFolders() {
      final File repoFolder;
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolderName = System.getenv(Constants.PEASS_REPOS);
         repoFolder = new File(repofolderName);
      } else {
         repoFolder = DEFAULT_REPO;
      }
      dependencyFolder = new File(repoFolder, "dependencies-final");
      dataFolder = new File(repoFolder, "measurementdata" + File.separator + "confidentClean");
      reexecuteFolder = new File(repoFolder, "measurementdata" + File.separator + "reexecute");
      propertiesRepo = new File(repoFolder, "properties");
      classificationFolder = new File(propertiesRepo, "classification");
      resultsFolder = new File(repoFolder, "measurementdata" + File.separator + "results");

      allViewFolder = new File(repoFolder, "views-final");

      resultsFolder.mkdirs();
   }

   public File getDependencyFolder() {
      return dependencyFolder;
   }

   public File getCleanDataFolder() {
      return dataFolder;
   }
   
   public File getReexecuteFolder() {
      return reexecuteFolder;
   }

   public File getPropertiesFolder() {
      return propertiesRepo;
   }

   public File getResultsFolder() {
      return resultsFolder;
   }

   public File getAllViewFolder() {
      return allViewFolder;
   }

   public File getClassificationFolder() {
      return classificationFolder;
   }

   public File getProjectPropertyFile(String project) {
      return new File(propertiesRepo, "properties" + File.separator + project + File.separator + project + ".json");
   }

   public File getProjectStatisticsFolder(String project) {
      File statisticsFolder = new File(resultsFolder, project + File.separator + "statistics");
      if (!statisticsFolder.exists()) {
         statisticsFolder.mkdirs();
      }
      return statisticsFolder;
   }

   public File getDependencyFile(String project) {
      return new File(dependencyFolder, "deps_" + project + ".json");
   }
   
   public File getExecutionFile(String project) {
      return new File(dependencyFolder, "execute_" + project + ".json");
   }

   public File getChangeFile(String project) {
      return new File(resultsFolder, project + File.separator + project + ".json");
   }

   public File getViewFolder(String project) {
      return new File(allViewFolder, "views_" + project);
   }
   
}
