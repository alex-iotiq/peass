package de.dagere.peass.dependency.traces.coverage;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class CoverageBasedSelector {

   private static final Logger LOG = LogManager.getLogger(CoverageBasedSelector.class);

   public static CoverageSelectionVersion selectBasedOnCoverage(final List<TraceCallSummary> summaries, final Set<ChangedEntity> changes) {
      List<TraceCallSummary> copiedSummaries = new LinkedList<>(summaries);
      Set<ChangedEntity> copiedChanges = new HashSet<>(changes);
      boolean changed = true;

      CoverageSelectionVersion resultingInfo = new CoverageSelectionVersion();

      LOG.debug("Searching CBS");
      while (copiedSummaries.size() > 0 && copiedChanges.size() > 0 && changed) {
         changed = false;

         TraceCallSummary selected = selectMaximumCalled(copiedChanges, copiedSummaries);

         LOG.debug("Selected: {}", selected);
         if (selected != null) {
            selected.setSelected(true);
            resultingInfo.getTestcases().put(selected.getTestcase(), selected);

            copiedSummaries.remove(selected);
            LOG.debug("Selected: {} with summary {}", selected.getTestcase(), selected);
            changed = removeUnneededChanges(copiedChanges, changed, selected);
         }

      }

      return resultingInfo;
   }

   private static boolean removeUnneededChanges(final Set<ChangedEntity> changes, boolean changed, final TraceCallSummary selected) {
      for (Iterator<ChangedEntity> changeIterator = changes.iterator(); changeIterator.hasNext();) {
         ChangedEntity change = changeIterator.next();
         String currentChangeSignature = change.toString();
         if (selected.getCallCounts().containsKey(currentChangeSignature) && selected.getCallCounts().get(currentChangeSignature) > 0) {
            changeIterator.remove();
            changed = true;
         }
      }
      return changed;
   }

   private static TraceCallSummary selectMaximumCalled(final Set<ChangedEntity> changes, final List<TraceCallSummary> copiedSummaries) {
      TraceCallSummary selected = copiedSummaries.get(0);
      int selectedCallSum = getCallSum(changes, selected);
      LOG.debug("Searching in {}", copiedSummaries.size());
      for (TraceCallSummary current : copiedSummaries) {
         int currentCallSum = getCallSum(changes, current);
         if (currentCallSum > selectedCallSum) {
            selectedCallSum = currentCallSum;
            selected = current;
         }
      }
      if (selectedCallSum > 0) {
         return selected;
      } else {
         return null;
      }
   }

   private static int getCallSum(final Set<ChangedEntity> changes, final TraceCallSummary summary) {
      int currentCallSum = 0;
      LOG.debug("Changes: ", changes.size());
      for (ChangedEntity change : changes) {
         String parameters = change.getParametersPrintable().length() > 0 ? "(" + change.getParametersPrintable() + ")" : "";
         String changeSignature = change.toString() + parameters;
         LOG.debug("Change signature: " + changeSignature);
         LOG.debug(summary.getCallCounts().keySet());
         if (change.getMethod() != null) {
            currentCallSum = addExactCallCount(summary, currentCallSum, changeSignature);
         } else {
            currentCallSum = addClassbasedCallCount(summary, currentCallSum, changeSignature);
         }
         LOG.debug("Sum: " + currentCallSum);
      }
      return currentCallSum;
   }

   private static int addClassbasedCallCount(final TraceCallSummary summary, int currentCallSum, final String changeSignature) {
      LOG.debug("Call counts: " + summary.getCallCounts().size());
      for (Map.Entry<String, Integer> callCount : summary.getCallCounts().entrySet()) {
         // The prefix needs to be used since otherwise inner classes are falsely selected (e.g. ChangedEntity de.Example would select de.Example$InnerClass#methodA)
         String signaturePrefix = changeSignature + ChangedEntity.METHOD_SEPARATOR;
         LOG.debug("Testing: " + signaturePrefix + " vs " + callCount.getKey());
         if (callCount.getKey().startsWith(signaturePrefix)) {
            currentCallSum += callCount.getValue();
         }
      }
      return currentCallSum;
   }

   private static int addExactCallCount(final TraceCallSummary summary, int currentCallSum, final String changeSignature) {
      if (summary.getCallCounts().containsKey(changeSignature)) {
         currentCallSum += summary.getCallCounts().get(changeSignature);
      }
      return currentCallSum;
   }
}
