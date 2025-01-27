package io.jenkins.plugins.util;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import io.jenkins.plugins.util.AgentFileVisitor.FileSystemFacade;
import io.jenkins.plugins.util.AgentFileVisitor.FileVisitorResult;
import io.jenkins.plugins.util.AgentFileVisitorTest.StringScanner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link AgentFileVisitor}.
 *
 * @author Ullrich Hafner
 */
class AgentFileVisitorTest extends SerializableTest<StringScanner> {
    private static final String CONTENT = "Hello World!";
    private static final String PATTERN = "**/*.txt";

    @TempDir
    private File workspace;

    @DisplayName("Should report error on empty results")
    @CsvSource({"true, enabled", "false, disabled"})
    @ParameterizedTest(name = "{index} => followSymbolicLinks={0}, message={1}")
    void shouldReportErrorOnEmptyResults(final boolean followLinks, final String message) {
        StringScanner scanner = new StringScanner(PATTERN, "UTF-8", followLinks,
                createFileSystemFacade(followLinks));

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);

        assertThat(actualResult.getResults()).isEmpty();
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '" + PATTERN + "'",
                "Traversing of symbolic links: " + message);
        assertThat(actualResult.getLog().getErrorMessages()).containsExactly(
                "Errors during parsing",
                "No files found for pattern '**/*.txt'. Configuration error?");
        assertThat(actualResult.hasErrors()).isTrue();
    }

    @DisplayName("Should report error on single result")
    @CsvSource({"true, enabled", "false, disabled"})
    @ParameterizedTest(name = "{index} => followSymbolicLinks={0}, message={1}")
    void shouldReturnSingleResult(final boolean followLinks, final String message) {
        StringScanner scanner = new StringScanner(PATTERN, "UTF-8", followLinks,
                createFileSystemFacade(followLinks, "/one.txt"));

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1);
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "Traversing of symbolic links: " + message,
                "-> found 1 file",
                "Successfully processed file '/one.txt'");
        assertThat(actualResult.getLog().getErrorMessages()).isEmpty();
        assertThat(actualResult.hasErrors()).isFalse();
    }

    @DisplayName("Should report error on single result")
    @CsvSource({"true, enabled", "false, disabled"})
    @ParameterizedTest(name = "{index} => followSymbolicLinks={0}, message={1}")
    void shouldReturnMultipleResults(final boolean followLinks, final String message) {
        StringScanner scanner = new StringScanner(PATTERN, "UTF-8", followLinks,
                createFileSystemFacade(followLinks, "/one.txt", "/two.txt"));

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1, CONTENT + 2);
        assertThat(actualResult.getLog().getInfoMessages()).containsExactly(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "Traversing of symbolic links: " + message,
                "-> found 2 files",
                "Successfully processed file '/one.txt'",
                "Successfully processed file '/two.txt'");
        assertThat(actualResult.getLog().getErrorMessages()).isEmpty();
        assertThat(actualResult.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Should handle empty or forbidden files")
    void shouldReturnMultipleResults() {
        FileSystemFacade fileSystemFacade = createFileSystemFacade(true,
                "/one.txt", "/two.txt", "empty.txt", "not-readable.txt");

        Path empty = workspace.toPath().resolve("empty.txt");
        when(fileSystemFacade.resolve(workspace, "empty.txt")).thenReturn(empty);
        when(fileSystemFacade.isNotReadable(empty)).thenReturn(true);

        Path notReadable = workspace.toPath().resolve("not-readable.txt");
        when(fileSystemFacade.resolve(workspace, "not-readable.txt")).thenReturn(notReadable);
        when(fileSystemFacade.isEmpty(notReadable)).thenReturn(true);

        StringScanner scanner = new StringScanner(PATTERN, "UTF-8", true,
                fileSystemFacade);

        FileVisitorResult<String> actualResult = scanner.invoke(workspace, null);
        assertThat(actualResult.getResults()).containsExactly(CONTENT + 1, CONTENT + 2);
        assertThat(actualResult.getLog().getInfoMessages()).contains(
                "Searching for all files in '/absolute/path' that match the pattern '**/*.txt'",
                "-> found 4 files",
                "Successfully processed file '/one.txt'",
                "Successfully processed file '/two.txt'");
        assertThat(actualResult.hasErrors()).isTrue();
        assertThat(actualResult.getLog().getErrorMessages()).containsExactly("Errors during parsing",
                "Skipping file 'empty.txt' because Jenkins has no permission to read the file",
                "Skipping file 'not-readable.txt' because it's empty");
    }

    private FileSystemFacade createFileSystemFacade(final boolean followLinks, final String... files) {
        FileSystemFacade fileSystem = mock(FileSystemFacade.class);

        when(fileSystem.getAbsolutePath(any())).thenReturn("/absolute/path");
        when(fileSystem.find(PATTERN, followLinks, workspace)).thenReturn(files);

        return fileSystem;
    }

    @Override
    protected StringScanner createSerializable() {
        return new StringScanner(PATTERN, "UTF-8", true, createFileSystemFacade(true));
    }

    static class StringScanner extends AgentFileVisitor<String> {
        private static final long serialVersionUID = -6902473746775046311L;
        private int counter = 1;

        @VisibleForTesting
        protected StringScanner(final String filePattern, final String encoding, final boolean followSymbolicLinks, final FileSystemFacade fileSystemFacade) {
            super(filePattern, encoding, followSymbolicLinks, fileSystemFacade);
        }

        @Override
        protected String processFile(final Path file, final Charset charset, final FilteredLog log) {
            return CONTENT + counter++;
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @SuppressFBWarnings(value = "EQ_ALWAYS_TRUE", justification = "Required for serializable test")
        @Override
        public boolean equals(final Object o) {
            return true;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
