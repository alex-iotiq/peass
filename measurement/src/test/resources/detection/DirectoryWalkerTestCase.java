
package org.apache.commons.io;

/*-
 * #%L
 * peran-measurement
 * %%
 * Copyright (C) 2015 - 2017 Hanns-Seidel-Stiftung
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import de.dagere.kopeme.junit3.KoPeMeTestcase;
import de.dagere.kopeme.datacollection.DataCollectorList;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;

/**
 * This is used to test DirectoryWalker for correctness.
 *
 * @version $Id$
 * @see DirectoryWalker
 *
 */
public class DirectoryWalkerTestCase extends KoPeMeTestcase {	protected int getMaximalTime() { return 3600000; }
	protected int getWarmupExecutions() { return 0; }
	protected int getExecutionTimes() { return  1; }
	protected boolean logFullData() { return false; }
	protected boolean useKieker() { return true; }

    // Directories
    private static final File current      = new File(".");
    private static final File javaDir      = new File("src/java");
    private static final File orgDir       = new File(javaDir, "org");
    private static final File apacheDir    = new File(orgDir, "apache");
    private static final File commonsDir   = new File(apacheDir, "commons");
    private static final File ioDir        = new File(commonsDir, "io");
    private static final File outputDir    = new File(ioDir, "output");
    private static final File[] dirs       = new File[] {orgDir, apacheDir, commonsDir, ioDir, outputDir};

    // Files
    private static final File copyUtils     = new File(ioDir, "CopyUtils.java");
    private static final File ioUtils       = new File(ioDir, "IOUtils.java");
    private static final File proxyWriter   = new File(outputDir, "ProxyWriter.java");
    private static final File nullStream    = new File(outputDir, "NullOutputStream.java");
    private static final File[] ioFiles     = new File[] {copyUtils, ioUtils};
    private static final File[] outputFiles = new File[] {proxyWriter, nullStream};
    
    // Filters
    private static final IOFileFilter dirsFilter        = createNameFilter(dirs);
    private static final IOFileFilter iofilesFilter     = createNameFilter(ioFiles);
    private static final IOFileFilter outputFilesFilter = createNameFilter(outputFiles);
    private static final IOFileFilter ioDirAndFilesFilter = new OrFileFilter(dirsFilter, iofilesFilter);
    private static final IOFileFilter dirsAndFilesFilter = new OrFileFilter(ioDirAndFilesFilter, outputFilesFilter);

    // Filter to exclude SVN files
    private static final IOFileFilter NOT_SVN = FileFilterUtils.makeSVNAware(null);

    public static Test suite() {
        return new TestSuite(DirectoryWalkerTestCase.class);
    }

    /** Construct the TestCase using the name */
    public DirectoryWalkerTestCase(String name) {
        super(name);
    }

    /** Set Up */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /** Tear Down */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    //-----------------------------------------------------------------------

    /**
     * Test Filtering
     */
    public void testFilter() {
        List results = new TestFileFinder(dirsAndFilesFilter, -1).find(javaDir);
        assertEquals("Result Size", (1 + dirs.length + ioFiles.length + outputFiles.length), results.size());
        assertTrue("Start Dir", results.contains(javaDir));
        checkContainsFiles("Dir", dirs, results);
        checkContainsFiles("IO File", ioFiles, results);
        checkContainsFiles("Output File", outputFiles, results);
    }

    /**
     * Test Filtering and limit to depth 0
     */
    public void testFilterAndLimitA() {
        List results = new TestFileFinder(NOT_SVN, 0).find(javaDir);
        assertEquals("[A] Result Size", 1, results.size());
        assertTrue("[A] Start Dir",   results.contains(javaDir));
    }

    /**
     * Test Filtering and limit to depth 1
     */
    public void testFilterAndLimitB() {
        List results = new TestFileFinder(NOT_SVN, 1).find(javaDir);
        assertEquals("[B] Result Size", 2, results.size());
        assertTrue("[B] Start Dir",   results.contains(javaDir));
        assertTrue("[B] Org Dir",     results.contains(orgDir));
    }

    /**
     * Test Filtering and limit to depth 3
     */
    public void testFilterAndLimitC() {
        List results = new TestFileFinder(NOT_SVN, 3).find(javaDir);
        assertEquals("[C] Result Size", 4, results.size());
        assertTrue("[C] Start Dir",   results.contains(javaDir));
        assertTrue("[C] Org Dir",     results.contains(orgDir));
        assertTrue("[C] Apache Dir",  results.contains(apacheDir));
        assertTrue("[C] Commons Dir", results.contains(commonsDir));
    }

    /**
     * Test Filtering and limit to depth 5
     */
    public void testFilterAndLimitD() {
        List results = new TestFileFinder(dirsAndFilesFilter, 5).find(javaDir);
        assertEquals("[D] Result Size", (1 + dirs.length + ioFiles.length), results.size());
        assertTrue("[D] Start Dir", results.contains(javaDir));
        checkContainsFiles("[D] Dir", dirs, results);
        checkContainsFiles("[D] File", ioFiles, results);
    }

    /**
     * Test separate dir and file filters
     */
    public void testFilterDirAndFile1() {
        List results = new TestFileFinder(dirsFilter, iofilesFilter, -1).find(javaDir);
        assertEquals("[DirAndFile1] Result Size", (1 + dirs.length + ioFiles.length), results.size());
        assertTrue("[DirAndFile1] Start Dir", results.contains(javaDir));
        checkContainsFiles("[DirAndFile1] Dir", dirs, results);
        checkContainsFiles("[DirAndFile1] File", ioFiles, results);
    }

    /**
     * Test separate dir and file filters
     */
    public void testFilterDirAndFile2() {
        List results = new TestFileFinder((IOFileFilter) null, (IOFileFilter) null, -1).find(javaDir);
        assertTrue("[DirAndFile2] Result Size", results.size() > (1 + dirs.length + ioFiles.length));
        assertTrue("[DirAndFile2] Start Dir", results.contains(javaDir));
        checkContainsFiles("[DirAndFile2] Dir", dirs, results);
        checkContainsFiles("[DirAndFile2] File", ioFiles, results);
    }

    /**
     * Test separate dir and file filters
     */
    public void testFilterDirAndFile3() {
        List results = new TestFileFinder(dirsFilter, (IOFileFilter) null, -1).find(javaDir);
        List resultDirs = directoriesOnly(results);
        assertEquals("[DirAndFile3] Result Size", (1 + dirs.length), resultDirs.size());
        assertTrue("[DirAndFile3] Start Dir", results.contains(javaDir));
        checkContainsFiles("[DirAndFile3] Dir", dirs, resultDirs);
    }

    /**
     * Test separate dir and file filters
     */
    public void testFilterDirAndFile4() {
        List results = new TestFileFinder((IOFileFilter) null, iofilesFilter, -1).find(javaDir);
        List resultFiles = filesOnly(results);
        assertEquals("[DirAndFile4] Result Size", ioFiles.length, resultFiles.size());
        assertTrue("[DirAndFile4] Start Dir", results.contains(javaDir));
        checkContainsFiles("[DirAndFile4] File", ioFiles, resultFiles);
    }

    /**
     * Test Limiting to current directory
     */
    public void testLimitToCurrent() {
        List results = new TestFileFinder(null, 0).find(current);
        assertEquals("Result Size", 1, results.size());
        assertTrue("Current Dir", results.contains(new File(".")));
    }

    /**
     * test an invalid start directory
     */
    public void testMissingStartDirectory() {

        // TODO is this what we want with invalid directory?
        File invalidDir = new File("invalid-dir");
        List results = new TestFileFinder(null, -1).find(invalidDir);
        assertEquals("Result Size", 1, results.size());
        assertTrue("Current Dir", results.contains(invalidDir));
 
        try {
            new TestFileFinder(null, -1).find(null);
            fail("Null start directory didn't throw Exception");
        } catch (NullPointerException ignore) {
            // expected result
        }
    }

    /**
     * test an invalid start directory
     */
    public void testHandleStartDirectoryFalse() {

        List results = new TestFalseFileFinder(null, -1).find(current);
        assertEquals("Result Size", 0, results.size());

    }

    // ------------ Convenience Test Methods ------------------------------------

    /**
     * Check the files in the array are in the results list.
     */
    private void checkContainsFiles(String prefix, File[] files, Collection results) {
        for (int i = 0; i < files.length; i++) {
            assertTrue(prefix + "["+i+"] " + files[i], results.contains(files[i]));
        }
    }

    /**
     * Extract the directories.
     */
    private List directoriesOnly(Collection results) {
        List list = new ArrayList(results.size());
        for (Iterator it = results.iterator(); it.hasNext(); ) {
            File file = (File) it.next();
            if (file.isDirectory()) {
                list.add(file);
            }
        }
        return list;
    }

    /**
     * Extract the files.
     */
    private List filesOnly(Collection results) {
        List list = new ArrayList(results.size());
        for (Iterator it = results.iterator(); it.hasNext(); ) {
            File file = (File) it.next();
            if (file.isFile()) {
                list.add(file);
            }
        }
        return list;
    }

    /**
     * Create an name filter containg the names of the files
     * in the array.
     */
    private static IOFileFilter createNameFilter(File[] files) {
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName();
        }
        return new NameFileFilter(names);
    }

    /**
     * Test Cancel
     */
    public void testCancel() {
        String cancelName = null;

        // Cancel on a file
        try {
            cancelName = "DirectoryWalker.java";
            List results = new TestCancelWalker(cancelName, false).find(javaDir);
            fail("CancelException not thrown for '" + cancelName + "'");
        } catch (DirectoryWalker.CancelException cancel) {
            assertEquals("File:  " + cancelName,   cancelName, cancel.getFile().getName());
            assertEquals("Depth: " + cancelName,  5, cancel.getDepth());
        } catch(IOException ex) {
            fail("IOException: " + cancelName + " " + ex);
        }

        // Cancel on a directory
        try {
            cancelName = "commons";
            List results = new TestCancelWalker(cancelName, false).find(javaDir);
            fail("CancelException not thrown for '" + cancelName + "'");
        } catch (DirectoryWalker.CancelException cancel) {
            assertEquals("File:  " + cancelName,   cancelName, cancel.getFile().getName());
            assertEquals("Depth: " + cancelName,  3, cancel.getDepth());
        } catch(IOException ex) {
            fail("IOException: " + cancelName + " " + ex);
        }

        // Suppress CancelException (use same file name as preceeding test)
        try {
            List results = new TestCancelWalker(cancelName, true).find(javaDir);
            File lastFile = (File)results.get(results.size() - 1);
            assertEquals("Suppress:  " + cancelName,   cancelName, lastFile.getName());
        } catch(IOException ex) {
            fail("Suppress threw " + ex);
        }

    }

    // ------------ Test DirectoryWalker implementation --------------------------

    /**
     * Test DirectoryWalker implementation that finds files in a directory hierarchy
     * applying a file filter.
     */
    private static class TestFileFinder extends DirectoryWalker {

        protected TestFileFinder(FileFilter filter, int depthLimit) {
            super(filter, depthLimit);
        }

        protected TestFileFinder(IOFileFilter dirFilter, IOFileFilter fileFilter, int depthLimit) {
            super(dirFilter, fileFilter, depthLimit);
        }

        /** find files. */
        protected List find(File startDirectory) {
           List results = new ArrayList();
           try {
               walk(startDirectory, results);
           } catch(IOException ex) {
               Assert.fail(ex.toString());
           }
           return results;
        }

        /** Handles a directory end by adding the File to the result set. */
        protected void handleDirectoryEnd(File directory, int depth, Collection results) {
            results.add(directory);
        }

        /** Handles a file by adding the File to the result set. */
        protected void handleFile(File file, int depth, Collection results) {
            results.add(file);
        }
    }

    // ------------ Test DirectoryWalker implementation --------------------------

    /**
     * Test DirectoryWalker implementation that always returns false
     * from handleDirectoryStart()
     */
    private static class TestFalseFileFinder extends TestFileFinder {

        protected TestFalseFileFinder(FileFilter filter, int depthLimit) {
            super(filter, depthLimit);
        }

        /** Always returns false. */
        protected boolean handleDirectory(File directory, int depth, Collection results) {
            return false;
        }
    }

    // ------------ Test DirectoryWalker implementation --------------------------

    /**
     * Test DirectoryWalker implementation that finds files in a directory hierarchy
     * applying a file filter.
     */
    static class TestCancelWalker extends DirectoryWalker {
        private String cancelFileName;
        private boolean suppressCancel;

        TestCancelWalker(String cancelFileName,boolean suppressCancel) {
            super();
            this.cancelFileName = cancelFileName;
            this.suppressCancel = suppressCancel;
        }

        /** find files. */
        protected List find(File startDirectory) throws IOException {
           List results = new ArrayList();
           walk(startDirectory, results);
           return results;
        }

        /** Handles a directory end by adding the File to the result set. */
        protected void handleDirectoryEnd(File directory, int depth, Collection results) throws IOException {
            results.add(directory);
            if (cancelFileName.equals(directory.getName())) {
                throw new CancelException(directory, depth);
            }
        }

        /** Handles a file by adding the File to the result set. */
        protected void handleFile(File file, int depth, Collection results) throws IOException {
            results.add(file);
            if (cancelFileName.equals(file.getName())) {
                throw new CancelException(file, depth);
            }
        }

        /** Handles Cancel. */
        protected void handleCancelled(File startDirectory, Collection results,
                       CancelException cancel) throws IOException {
            if (!suppressCancel) {
                super.handleCancelled(startDirectory, results, cancel);
            }
        }
    }

}
