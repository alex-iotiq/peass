package de.dagere.peass.dependency.traces.coverage;

import java.util.List;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.traces.TraceElementContent;
import de.dagere.requitur.content.Content;

public class TraceSummaryTransformer {
   public static TraceCallSummary transform(final TestMethodCall testcase, final List<Content> expandedTrace) {
      TraceCallSummary resultSummary = new TraceCallSummary();
      resultSummary.setTestcase(testcase);
      for (Content traceElement : expandedTrace) {
         if (traceElement instanceof TraceElementContent) {
            TraceElementContent traceElementContent = (TraceElementContent) traceElement;
            resultSummary.addCall(traceElementContent.toString());
         } else {
            throw new RuntimeException("Adding unexpected trace element: " + traceElement.getClass());
         }
      }
      return resultSummary;
   }
}
