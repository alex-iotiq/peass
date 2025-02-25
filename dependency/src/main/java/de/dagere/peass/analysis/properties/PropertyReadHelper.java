package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.properties.ChangeProperty.TraceChange;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.RTSTestTransformerBuilder;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.CommitDiff;
import de.dagere.peass.dependency.analysis.data.EntityUtil;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileManager;
import de.dagere.peass.dependency.traces.diff.TraceFileUtil;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.requitur.Sequitur;
import difflib.Delta;
import difflib.Delta.TYPE;
import difflib.DiffUtils;
import difflib.Patch;

public class PropertyReadHelper {

   private static final Logger LOG = LogManager.getLogger(PropertyReadHelper.class);

   public static final String keywords[] = { "abstract", "assert", "boolean",
         "break", "byte", "case", "catch", "char", "class", "const",
         "continue", "default", "do", "double", "else", "extends", "false",
         "final", "finally", "float", "for", "goto", "if", "implements",
         "import", "instanceof", "int", "interface", "long", "native",
         "new", "null", "package", "private", "protected", "public",
         "return", "short", "static", "strictfp", "super", "switch",
         "synchronized", "this", "throw", "throws", "transient", "true",
         "try", "void", "volatile", "while",
         "System.out.println", "System.gc", "Thread.sleep" };

   private final ExecutionData changedTests;
   private final ChangedEntity testClazz;
   private final ExecutionConfig config;
   private final String commit, commitOld;
   private final Change change;
   private final File projectFolder;

   private final File viewFolder;
   private final File methodSourceFolder;

   private final PeassFolders folders;
   private final TestTransformer testTransformer;
   private final TestExecutor testExecutor;

   /**
    * Just for local debugging purposes - no public use intended
    * 
    * @param args
    * @throws IOException
    */
   public static void main(final String[] args) throws IOException {
      final ChangedEntity ce = new ChangedEntity("org.apache.commons.fileupload.StreamingTest", "");
      final Change change = new Change();
      change.setChangePercent(-8.0);
      change.setMethod("testFILEUPLOAD135");
      final File projectFolder2 = new File("../../projekte/commons-fileupload");
      final File viewFolder2 = new File("/home/reichelt/daten3/diss/repos/preprocessing/4/commons-fileupload/views_commons-fileupload/");
      FixedCommitConfig demoConfig = new FixedCommitConfig();
      demoConfig.setCommit("96f8f56556a8592bfed25c82acedeffc4872ac1f");
      demoConfig.setCommitOld("09d16c");

      final PropertyReadHelper propertyReadHelper = new PropertyReadHelper(new ExecutionConfig(), demoConfig, ce, change, projectFolder2, viewFolder2,
            new File("/tmp/"), null);
      propertyReadHelper.read();
   }

   public PropertyReadHelper(final ExecutionConfig config, FixedCommitConfig commitConfig, final ChangedEntity clazz,
         final Change change, final File projectFolder, final File viewFolder, final File methodSourceFolder, final ExecutionData changedTests) {
      this.commit = commitConfig.getCommit();
      this.commitOld = commitConfig.getCommitOld();
      this.config = config;
      if (clazz.getMethod() != null) {
         throw new RuntimeException("Method must not be set!");
      }
      this.testClazz = clazz;
      this.change = change;
      this.projectFolder = projectFolder;
      this.viewFolder = viewFolder;
      this.methodSourceFolder = methodSourceFolder;
      this.changedTests = changedTests;

      folders = new PeassFolders(projectFolder);
      testTransformer = RTSTestTransformerBuilder.createTestTransformer(folders, config, new KiekerConfig(true));
      testExecutor = ExecutorCreator.createExecutor(folders, testTransformer, new EnvironmentVariables());
   }

   public ChangeProperty read() throws IOException {
      final ChangeProperty property = new ChangeProperty(change);

      getSourceInfos(property);

      LOG.debug("Comparing " + commit + " " + property.getMethod());
      final File folder = new File(viewFolder, "view_" + commit + File.separator + testClazz + File.separator + property.getMethod());
      if (folder.exists()) {
         // analyseTraceChange(folder, property);
         return property;
      } else {
         LOG.error("Folder {} does not exist", folder);
         return property;
      }
   }

   private String getShortPrevVersion() {
      // This happens for the initial version
      if (commitOld == null) {
         return "";
      }
      if (commitOld.endsWith("~1")) {
         return commitOld.substring(0, 6) + "~1";
      } else {
         return commitOld.substring(0, 6);
      }
   }

   public void getSourceInfos(final ChangeProperty property) throws FileNotFoundException, IOException {
      final File folder = new File(viewFolder, "view_" + commit + File.separator + testClazz + File.separator + property.getMethod());
      final File traceFileCurrent = TraceFileManager.getExistingTraceFile(folder, commit.substring(0, 6), OneTraceGenerator.METHOD);
      File traceFileOld = TraceFileManager.getExistingTraceFile(folder, getShortPrevVersion(), OneTraceGenerator.METHOD); 
      if (changedTests != null) {
         traceFileOld = searchOldTraceFile(property, traceFileOld);
      }

      if (traceFileCurrent.exists() && traceFileOld.exists()) {
         analyzeTraceFiles(property, traceFileCurrent, traceFileOld);
      } else {
         readExpandedFileTrace(folder);

         if (!traceFileCurrent.exists()) {
            LOG.error("Tracefile not found: {}", traceFileCurrent);
         } else {
            LOG.error("Tracefile not found: {}", traceFileOld);
         }

      }

   }

   private void readExpandedFileTrace(final File folder) throws IOException, FileNotFoundException {
      File expandedFile = new File(folder, commit.substring(0, 6) + OneTraceGenerator.METHOD_EXPANDED + TraceFileManager.TXT_ENDING);
      if (expandedFile.exists()) {
         LOG.info("Reading method sources from expanded tracefile {}", expandedFile);
         final List<String> traceCurrent = Sequitur.getExpandedTrace(expandedFile);
         final PeassFolders folders = new PeassFolders(projectFolder);

         // Only to read old sources
         getChanges(folders);

         readMethodSources(new ChangeProperty(), folders, new HashSet<>(traceCurrent));
      }
   }

   private File searchOldTraceFile(final ChangeProperty property, File traceFileOld) {
      List<String> versions = new ArrayList<>(changedTests.getCommits().keySet());
      int index = versions.indexOf(commitOld);
      if (index == -1) {
         index = versions.indexOf(commit) - 1;
      }
      LOG.debug("Trying old versions starting with {} Versions: {}", index, changedTests.getCommits().keySet());
      while (!traceFileOld.exists() && index >= 0) {
         String tryVersion = versions.get(index);
         File versionFolder = new File(viewFolder, "view_" + tryVersion);
         File predecessorFolder = new File(versionFolder, testClazz + File.separator + property.getMethod());
         String tryVersionShort = tryVersion.substring(0, 6);
         traceFileOld = TraceFileManager.getExistingTraceFile(predecessorFolder, tryVersionShort, OneTraceGenerator.METHOD);
         LOG.debug("Trying file " + traceFileOld.getAbsolutePath());
         index--;
      }
      return traceFileOld;
   }

   private void analyzeTraceFiles(final ChangeProperty property, final File traceFileCurrent, final File traceFileOld) throws IOException, FileNotFoundException {
      final PeassFolders folders = new PeassFolders(projectFolder);
      final Map<ChangedEntity, ClazzChangeData> changes = getChanges(folders);

      final List<String> traceCurrent = Sequitur.getExpandedTrace(TraceFileUtil.getText(traceFileCurrent));
      final List<String> traceOld = Sequitur.getExpandedTrace(TraceFileUtil.getText(traceFileOld));
      determineTraceSizeChanges(property, traceCurrent, traceOld);

      final Set<String> merged = getMergedCalls(traceCurrent, traceOld);

      readMethodSources(property, folders, merged);

      identifyAffectedClasses(property, merged);

      LOG.trace("Calls: " + merged);

      getTestSourceAffection(property, merged, folders, changes);
   }

   private Map<ChangedEntity, ClazzChangeData> getChanges(final PeassFolders folders) {
      List<String> commits = Arrays.asList(new String[] { commit, commitOld });
      final CommitIteratorGit iterator = new CommitIteratorGit(projectFolder, commits, commitOld);
      final ChangeManager changeManager = new ChangeManager(folders, iterator, config, testExecutor);
      final Map<ChangedEntity, ClazzChangeData> changes = changeManager.getChanges(commitOld, commit);
      return changes;
   }

   private void readMethodSources(final ChangeProperty property, final PeassFolders folders, final Set<String> merged) throws FileNotFoundException, IOException {
      for (final String calledInOneMethod : merged) {
         LOG.debug("Loading: " + calledInOneMethod);
         final ChangedEntity entity = EntityUtil.determineEntity(calledInOneMethod);
         final MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, folders.getProjectFolder(), folders.getOldSources(), entity, commit, config);
         reader.readMethodChangeData();
         getKeywordChanges(property, reader, entity);
      }
   }

   private void identifyAffectedClasses(final ChangeProperty property, final Set<String> calls) throws FileNotFoundException, IOException {
      List<File> modules = testExecutor.getModules().getModules();
      final CommitDiff diff = GitUtils.getChangedFiles(projectFolder, modules, commit, config);
      removeUncalledClasses(calls, diff);
      property.setAffectedClasses(diff.getChangedClasses().size());
      final int changedLines = GitUtils.getChangedLines(projectFolder, commit, diff.getChangedClasses(), config);
      property.setAffectedLines(changedLines);
   }

   private void removeUncalledClasses(final Set<String> calls, final CommitDiff diff) {
      for (final Iterator<ChangedEntity> it = diff.getChangedClasses().iterator(); it.hasNext();) {
         final ChangedEntity entity = it.next();
         boolean called = false;
         for (final String call : calls) {
            if (call.startsWith(entity.getJavaClazzName())) {
               called = true;
               break;
            }
         }
         if (!called)
            it.remove();
      }
   }

   public void getKeywordChanges(final ChangeProperty property, final MethodChangeReader changeManager, final ChangedEntity entity) throws FileNotFoundException {
      final Patch<String> patch = changeManager.getKeywordChanges(entity);

      final Map<String, Integer> vNewkeywords = new HashMap<>();
      final Map<String, Integer> vOldkeywords = new HashMap<>();
      for (final Delta<String> changeSet : patch.getDeltas()) {
         for (final String line : changeSet.getOriginal().getLines()) {
            getKeywordCount(vOldkeywords, line);
         }
         for (final String line : changeSet.getRevised().getLines()) {
            getKeywordCount(vNewkeywords, line);
         }
      }
      for (final Map.Entry<String, Integer> vNew : vNewkeywords.entrySet()) {
         property.getAddedMap().put(vNew.getKey(), vNew.getValue());
      }
      for (final Map.Entry<String, Integer> vOld : vOldkeywords.entrySet()) {
         // System.out.println("Removed: " + v2.getKey() + " " + v2.getValue());
         property.getRemovedMap().put(vOld.getKey(), vOld.getValue());
      }
   }

   public Set<String> getMergedCalls(final List<String> traceCurrent, final List<String> traceOld) {
      final Set<String> merged = new HashSet<>();
      final Set<String> calledCurrent = new HashSet<>(traceCurrent);
      final Set<String> calledOld = new HashSet<>(traceOld);
      merged.addAll(calledCurrent);
      merged.addAll(calledOld);

      // intersection.retainAll(calledOld);
      return merged;
   }

   void getTestSourceAffection(final ChangeProperty property, final Set<String> calls, final PeassFolders folders, final Map<ChangedEntity, ClazzChangeData> changes)
         throws FileNotFoundException {
      final ClazzChangeData clazzChangeData = changes.get(testClazz);
      if (clazzChangeData != null) {
         if (clazzChangeData.isOnlyMethodChange()) {
            for (final Set<String> methodsOfClazz : clazzChangeData.getChangedMethods().values()) {
               if (methodsOfClazz.contains(property.getMethod())) {
                  property.setAffectsTestSource(true);
               }
            }
         } else {
            property.setAffectsTestSource(true);
         }
      }

      // Prinzipiell: Man müsste schauen, wo der Quelltext liegt, nicht, wie er heißt..
      for (final Entry<ChangedEntity, ClazzChangeData> changedEntity : changes.entrySet()) {
         // final Set<String> guessedTypes = new PropertyChangeGuesser().getGuesses(folders, changedEntity);
         // property.getGuessedTypes().addAll(guessedTypes);

         final ChangedEntity outerClazz = changedEntity.getKey();
         if (!changedEntity.getValue().isOnlyMethodChange()) {
            for (final String call : calls) {
               final String clazzCall = call.substring(0, call.indexOf("#"));
               if (outerClazz.getJavaClazzName().equals(clazzCall)) {
                  processFoundCall(property, changedEntity);
               }
            }
         } else {
            for (final Map.Entry<String, Set<String>> changedClazz : changedEntity.getValue().getChangedMethods().entrySet()) {
               for (final String changedMethod : changedClazz.getValue()) {
                  String fqn;
                  if (changedMethod.contains(ChangedEntity.METHOD_SEPARATOR)) {
                     fqn = outerClazz.getPackage() + "." + changedClazz.getKey() + ChangedEntity.CLAZZ_SEPARATOR + changedMethod;
                  } else {
                     fqn = outerClazz.getPackage() + "." + changedClazz.getKey() + ChangedEntity.METHOD_SEPARATOR + changedMethod;
                  }
                  if (fqn.contains("(") && changedClazz.getKey().contains(ChangedEntity.CLAZZ_SEPARATOR)) {
                     final String innerParameter = changedClazz.getKey().substring(0, changedClazz.getKey().lastIndexOf(ChangedEntity.CLAZZ_SEPARATOR));
                     fqn = fqn.substring(0, fqn.indexOf("(") + 1) + innerParameter + "," + fqn.substring(fqn.indexOf("(") + 1);
                  }
                  if (calls.contains(fqn)) {
                     processFoundCall(property, changedEntity);
                  }
               }
            }
         }
      }
   }

   /**
    * Determines how the trace has changed viewed from trace1, e.g. ADDED_CALLS means that trace2 has more calls than trace1.
    * 
    * @param property
    * @param traceCurrent
    * @param traceOld
    * @throws IOException
    */
   public static void determineTraceSizeChanges(final ChangeProperty property, final List<String> traceCurrent, final List<String> traceOld) throws IOException {
      LOG.debug("Trace sizes: {}, {}", traceCurrent.size(), traceOld.size());
      if (traceCurrent.size() + traceOld.size() < 10000) {
         final Patch<String> patch = DiffUtils.diff(traceOld, traceCurrent);

         LOG.debug(patch);

         int added = 0, removed = 0;
         for (final Delta<String> delta : patch.getDeltas()) {
            if (delta.getType().equals(TYPE.DELETE)) {
               removed++;
            } else if (delta.getType().equals(TYPE.INSERT)) {
               added++;
            } else if (delta.getType().equals(TYPE.CHANGE)) {
               added++;
               removed++;
            }
         }

         if (added > 0 && removed > 0) {
            property.setTraceChangeType(TraceChange.BOTH);
         } else if (added > 0) {
            property.setTraceChangeType(TraceChange.ADDED_CALLS);
         } else if (removed > 0) {
            property.setTraceChangeType(TraceChange.REMOVED_CALLS);
         } else {
            property.setTraceChangeType(TraceChange.NO_CALL_CHANGE);
         }
      } else {
         property.setTraceChangeType(TraceChange.UNKNOWN);
      }

      property.setCalls(traceCurrent.size());
      property.setCallsOld(traceOld.size());
   }

   private void processFoundCall(final ChangeProperty property, final Entry<ChangedEntity, ClazzChangeData> changedEntity) {
      final ChangedEntity call = changedEntity.getKey();
      if (call.getClazz().toLowerCase().contains("test")) {
         property.setAffectsTestSource(true);
      } else {
         property.setAffectsSource(true);
      }
      final String packageName = call.getPackage();
      for (final Map.Entry<String, Set<String>> methods : changedEntity.getValue().getChangedMethods().entrySet()) {
         if (methods.getValue() != null && methods.getValue().size() > 0) {
            for (final String method : methods.getValue()) {
               property.getAffectedMethods().add(packageName + "." + methods.getKey() + ChangedEntity.METHOD_SEPARATOR + method);
            }
         } else {
            property.getAffectedMethods().add(call.getJavaClazzName());
         }
      }
   }

   private static void getKeywordCount(final Map<String, Integer> v1keywords, final String line) {
      for (final String keyword : keywords) {
         if (line.contains(keyword)) {
            final Integer integer = v1keywords.get(keyword);
            final int count = integer != null ? integer : 0;
            v1keywords.put(keyword, count + 1);
         }
      }
   }

}
