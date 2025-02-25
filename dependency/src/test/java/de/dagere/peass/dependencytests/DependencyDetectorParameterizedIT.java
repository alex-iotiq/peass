package de.dagere.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.reader.DependencyReader;
import de.dagere.peass.dependencytests.helper.FakeFileIterator;
import de.dagere.peass.vcs.CommitIterator;

public class DependencyDetectorParameterizedIT {
   
   public static final File CONTAINING_FOLDER = new File(DependencyTestConstants.VERSIONS_FOLDER, "parameterized");
   public static final File BASIC_STATE = new File(CONTAINING_FOLDER, "basic_state");
   public static final File NORMAL_CHANGE = new File(CONTAINING_FOLDER, "normal_change");

   @BeforeEach
   public void initialize() throws IOException, InterruptedException {
      Assert.assertTrue(DependencyTestConstants.VERSIONS_FOLDER.exists());

      FileUtils.deleteDirectory(DependencyTestConstants.CURRENT);
      FileUtils.copyDirectory(BASIC_STATE, DependencyTestConstants.CURRENT);

   }

   @Test
   public void testNormalChange() throws IOException, InterruptedException, XmlPullParserException, ParseException {
      final ChangeManager changeManager = changeManagerWithParameter();

      final CommitIterator fakeIterator = new FakeFileIterator(DependencyTestConstants.CURRENT, Arrays.asList(NORMAL_CHANGE));

      final DependencyReader reader = DependencyDetectorTestUtil.readTwoVersions(changeManager, fakeIterator);

      System.out.println(reader.getDependencies().getInitialcommit().getInitialDependencies());
      CommitStaticSelection firstVersion = reader.getDependencies().getCommits().get(DependencyTestConstants.VERSION_1);
      System.out.println(firstVersion.getChangedClazzes());

      Assert.assertEquals(3, reader.getDependencies().getInitialcommit().getInitialDependencies().size());
      TestSet selectedTest = firstVersion.getChangedClazzes().get(new ChangedEntity("defaultpackage.NormalDependency#onlyCalledWithOne"));
      Assert.assertEquals(TestMethodCall.createFromString("TestMe#testMe(JUNIT_PARAMETERIZED-2)"), selectedTest.getTestMethods().iterator().next());
   }

   public static ChangeManager changeManagerWithParameter() {
      final Map<ChangedEntity, ClazzChangeData> changes = DependencyDetectorTestUtil.buildChanges("", "defaultpackage.NormalDependency", "onlyCalledWithOne()");

      final ChangeManager changeManager = Mockito.mock(ChangeManager.class);
      Mockito.when(changeManager.getChanges(Mockito.any())).thenReturn(changes);
      return changeManager;
   }
}
