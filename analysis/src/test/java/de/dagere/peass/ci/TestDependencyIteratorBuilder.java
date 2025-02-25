package de.dagere.peass.ci;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitUtils;

public class TestDependencyIteratorBuilder {

   private static final File TEMPORARY_FOLDER = new File("target/temp");
   private static final String SIMPLE_PREDECESSOR = "000001";
   private static final String LAST_RUNNING_VERSION = "00000A";
   private static final String VERSION_2 = "000002";

   @Test
   public void testRegularIteratorCreation() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(SIMPLE_PREDECESSOR);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildVersionDependencies(LAST_RUNNING_VERSION);
         
         CommitIteratorGit iterator = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER)).getIterator();
         Assert.assertEquals(2, iterator.getSize());
         Assert.assertEquals(VERSION_2, iterator.getCommitName());
         Assert.assertEquals(SIMPLE_PREDECESSOR, iterator.getPredecessor());
      }
   }
   
   @Test
   public void testNightlyBuildWithoutPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildVersionDependencies();
         
         CommitIteratorBuilder builder = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         CommitIteratorGit iterator = builder.getIterator();
         Assert.assertNull(iterator);
         Assert.assertEquals(VERSION_2, builder.getCommit());
         Assert.assertEquals(null, builder.getCommitOld());
      }
   }
   
   @Test
   public void testNightlyBuildWithoutRunningPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildVersionDependencies(SIMPLE_PREDECESSOR);
         dependencies.getCommits().get(SIMPLE_PREDECESSOR).setRunning(false);
         
         CommitIteratorBuilder builder = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         CommitIteratorGit iterator = builder.getIterator();
         Assert.assertNull(iterator);
         Assert.assertEquals(VERSION_2, builder.getCommit());
         Assert.assertEquals(SIMPLE_PREDECESSOR, builder.getCommitOld());
      }
   }
   
   @Test
   public void testNightlyBuildWithPrePredecessor() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         StaticTestSelection dependencies = buildVersionDependencies(SIMPLE_PREDECESSOR, VERSION_2);
         
         CommitIteratorBuilder builder = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER));
         CommitIteratorGit iterator = builder.getIterator();
         Assert.assertNull(iterator);
         Assert.assertEquals(VERSION_2, builder.getCommit());
         Assert.assertEquals(SIMPLE_PREDECESSOR, builder.getCommitOld());
      }
   }

   @Test
   public void testNightlyBuildIteratorCreation() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName(Mockito.any(), Mockito.any())).thenReturn(VERSION_2);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");
         
         StaticTestSelection dependencies = buildVersionDependencies(LAST_RUNNING_VERSION);

         CommitIteratorGit iterator = new CommitIteratorBuilder(config, dependencies, new PeassFolders(TEMPORARY_FOLDER)).getIterator();
         Assert.assertEquals(2, iterator.getSize());
         Assert.assertEquals(VERSION_2, iterator.getCommitName());
         Assert.assertEquals(LAST_RUNNING_VERSION, iterator.getPredecessor());
      }
   }

   @Test
   public void testInitialRun() {
      try (MockedStatic<GitUtils> gitUtil = Mockito.mockStatic(GitUtils.class)) {
         gitUtil.when(() -> GitUtils.getName("HEAD", TEMPORARY_FOLDER)).thenReturn(VERSION_2);
         gitUtil.when(() -> GitUtils.getName("HEAD~1", TEMPORARY_FOLDER)).thenReturn(SIMPLE_PREDECESSOR);
         
         FixedCommitConfig config = new FixedCommitConfig();
         config.setCommitOld(null);
         config.setCommit("HEAD");

         CommitIteratorGit iterator = new CommitIteratorBuilder(config, null, new PeassFolders(TEMPORARY_FOLDER)).getIterator();
         Assert.assertEquals(2, iterator.getSize());
         Assert.assertEquals(VERSION_2, iterator.getCommitName());
         Assert.assertEquals(SIMPLE_PREDECESSOR, iterator.getPredecessor());
      }
   }
   
   private StaticTestSelection buildVersionDependencies(final String... versionNames) {
      StaticTestSelection dependencies = new StaticTestSelection();
      for (String versionName : versionNames) {
         CommitStaticSelection version = new CommitStaticSelection();
         version.setRunning(true);
         dependencies.getCommits().put(versionName, version);
      }
      
      return dependencies;
   }
}
