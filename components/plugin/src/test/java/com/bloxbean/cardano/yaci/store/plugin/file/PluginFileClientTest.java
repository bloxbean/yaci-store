package com.bloxbean.cardano.yaci.store.plugin.file;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PluginFileClientTest {

    private PluginFileClient fileClient;

    @TempDir
    Path tempDir; // used as the sandbox root

    @BeforeEach
    void setUp() {
        StoreProperties storeProperties = new StoreProperties();
        storeProperties.setPluginFilesRootPath(tempDir.toString());
        storeProperties.setPluginsEnabled(true);
        storeProperties.setPluginFilesEnableLocks(false);
        fileClient = new PluginFileClient(storeProperties);
    }

    @Test
    void testBasicFileOperations() {
        String filePath = "test.txt";
        String content = "Hello, World!";

        // Write file
        FileOperationResult writeResult = fileClient.write(filePath, content);
        assertThat(writeResult.isSuccess()).isTrue();

        // Check existence
        assertThat(fileClient.exists(filePath)).isTrue();

        // Read file
        FileOperationResult readResult = fileClient.read(filePath);
        assertThat(readResult.isSuccess()).isTrue();
        assertThat(readResult.getAsString()).isEqualTo(content);

        // Delete file
        FileOperationResult deleteResult = fileClient.delete(filePath);
        assertThat(deleteResult.isSuccess()).isTrue();
        assertThat(fileClient.exists(filePath)).isFalse();
    }

    @Test
    void testAppendOperations() {
        String filePath = "append_test.txt";

        // Write initial content
        fileClient.write(filePath, "Line 1\n");

        // Append content
        FileOperationResult appendResult = fileClient.append(filePath, "Line 2\n");
        assertThat(appendResult.isSuccess()).isTrue();

        // Read and verify
        FileOperationResult readResult = fileClient.read(filePath);
        assertThat(readResult.getAsString()).isEqualTo("Line 1\nLine 2\n");

        // Test write with append flag
        FileOperationResult writeAppendResult = fileClient.write(filePath, "Line 3\n", true);
        assertThat(writeAppendResult.isSuccess()).isTrue();

        readResult = fileClient.read(filePath);
        assertThat(readResult.getAsString()).isEqualTo("Line 1\nLine 2\nLine 3\n");
    }

    @Test
    void testDirectoryOperations() {
        String dirPath = "test_dir";

        // Create directory
        FileOperationResult createResult = fileClient.createDir(dirPath);
        assertThat(createResult.isSuccess()).isTrue();
        assertThat(fileClient.exists(dirPath)).isTrue();

        // Create files in directory
        fileClient.write("test_dir/file1.txt", "Content 1");
        fileClient.write("test_dir/file2.json", "{}");

        // List files
        DirectoryListing listing = fileClient.listFiles(dirPath);
        assertThat(listing.isSuccess()).isTrue();
        assertThat(listing.getFiles()).hasSize(2);

        // Filter by extension
        List<FileInfo> jsonFiles = listing.filterByExtension("json");
        assertThat(jsonFiles).hasSize(1);
        assertThat(jsonFiles.get(0).getName()).isEqualTo("file2.json");
    }

    @Test
    void testPathOperations() {
        // Create a nested path inside the sandbox so safeResolve() works
        String nested = "path/to/file.txt";
        fileClient.createDir("path/to");
        fileClient.write(nested, "x");

        // Join paths
        String joinedPath = fileClient.joinPath("base", "sub", "file.txt");
        assertThat(joinedPath).contains("base").contains("sub").contains("file.txt");

        // Get filename
        String filename = fileClient.getFileName(nested);
        assertThat(filename).isEqualTo("file.txt");
    }

    @Test
    void testJsonOperations() {
        String jsonPath = "test.json";

        // Create test data
        Map<String, Object> testData = Map.of(
                "name", "Test",
                "value", 123,
                "active", true
        );

        // Write JSON
        FileOperationResult writeResult = fileClient.writeJson(jsonPath, testData);
        assertThat(writeResult.isSuccess()).isTrue();

        // Read JSON
        FileOperationResult readResult = fileClient.readJson(jsonPath);
        assertThat(readResult.isSuccess()).isTrue();

        // The readJson now returns Object, cast it appropriately
        Object data = readResult.getData();
        assertThat(data).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> readData = (Map<String, Object>) data;
        assertThat(readData.get("name")).isEqualTo("Test");
        assertThat(readData.get("value")).isEqualTo(123);
        assertThat(readData.get("active")).isEqualTo(true);
    }

    @Test
    void testJsonArrayAppend() {
        String jsonPath = "array.json";

        // Append to non-existent file (should create new array)
        FileOperationResult appendResult1 = fileClient.appendJson(jsonPath, Map.of("id", 1, "name", "First"));
        assertThat(appendResult1.isSuccess()).isTrue();

        // Append second element
        FileOperationResult appendResult2 = fileClient.appendJson(jsonPath, Map.of("id", 2, "name", "Second"));
        assertThat(appendResult2.isSuccess()).isTrue();

        // Read and verify array
        FileOperationResult readResult = fileClient.readJson(jsonPath);
        assertThat(readResult.isSuccess()).isTrue();

        Object data = readResult.getData();
        assertThat(data).isInstanceOf(List.class);
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) data;
        assertThat(list).hasSize(2);
    }

    @Test
    void testCsvOperations() {
        String csvPath = "test.csv";

        // Write CSV
        List<String> headers = List.of("name", "age", "city");
        List<List<?>> rows = List.of(
                List.of("Alice", "30", "New York"),
                List.of("Bob", "25", "Los Angeles"),
                List.of("Charlie", "35", "Chicago")
        );

        FileOperationResult writeResult = fileClient.writeCsv(csvPath, headers, rows, false);
        assertThat(writeResult.isSuccess()).isTrue();

        // Read CSV
        FileOperationResult readResult = fileClient.readCsv(csvPath);
        assertThat(readResult.isSuccess()).isTrue();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> records = readResult.getAs(List.class);
        assertThat(records).hasSize(3);
        assertThat(records.get(0).get("name")).isEqualTo("Alice");
        assertThat(records.get(0).get("age")).isEqualTo("30");
        assertThat(records.get(1).get("name")).isEqualTo("Bob");
    }

    @Test
    void testCsvAutoCoercionOfTypes() {
        String csvPath = "coerce.csv";

        // Headers/rows with mixed types (Integer/Long/String)
        List<?> headers = List.of("txHash", "outputIndex", "lovelaceAmount");
        List<List<?>> rows = List.of(
                List.of("abcd", 0, 1000L),
                List.of("efgh", 1, 2000000L)
        );

        FileOperationResult writeResult = fileClient.writeCsv(csvPath, headers, rows, false);
        assertThat(writeResult.isSuccess()).isTrue();

        FileOperationResult readResult = fileClient.readCsv(csvPath);
        assertThat(readResult.isSuccess()).isTrue();

        @SuppressWarnings("unchecked")
        List<Map<String, String>> records = readResult.getAs(List.class);
        assertThat(records).hasSize(2);
        assertThat(records.get(0).get("outputIndex")).isEqualTo("0");
        assertThat(records.get(0).get("lovelaceAmount")).isEqualTo("1000");
        assertThat(records.get(1).get("lovelaceAmount")).isEqualTo("2000000");
    }

    @Test
    void testCsvAppend() {
        String csvPath = "append.csv";

        // Write initial CSV with headers
        List<String> headers = List.of("id", "value");
        List<List<?>> initialRows = List.of(List.of("1", "first"));

        fileClient.writeCsv(csvPath, headers, initialRows, false);

        // Append new rows (mixed types OK)
        List<List<?>> newRows = List.of(
                List.of(2, "second"),
                List.of(3L, "third")
        );

        FileOperationResult appendResult = fileClient.appendCsv(csvPath, newRows);
        assertThat(appendResult.isSuccess()).isTrue();

        // Read and verify
        FileOperationResult readResult = fileClient.readCsv(csvPath);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> records = readResult.getAs(List.class);
        assertThat(records).hasSize(3);
        assertThat(records.get(2).get("value")).isEqualTo("third");
        assertThat(records.get(1).get("id")).isEqualTo("2");
    }

    @Test
    void testCsvWriteWithAppendFlag() {
        String csvPath = "write_append.csv";

        // Write initial CSV with headers
        List<String> headers = List.of("block", "tx_count");
        List<List<?>> initialRows = List.of(List.of("100", "5"));

        fileClient.writeCsv(csvPath, headers, initialRows, false);

        // Append using writeCsv with append=true
        List<List<?>> newRows = List.of(
                List.of("101", "3"),
                List.of("102", "7")
        );

        FileOperationResult appendResult = fileClient.writeCsv(csvPath, headers, newRows, true);
        assertThat(appendResult.isSuccess()).isTrue();

        // Read and verify
        FileOperationResult readResult = fileClient.readCsv(csvPath);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> records = readResult.getAs(List.class);
        assertThat(records).hasSize(3);
        assertThat(records.get(0).get("block")).isEqualTo("100");
        assertThat(records.get(1).get("block")).isEqualTo("101");
        assertThat(records.get(2).get("tx_count")).isEqualTo("7");
    }

    @Test
    void testErrorHandling() {
        // Non-existent path INSIDE sandbox → "does not exist"
        String insideNonExistent = "no/such/file.txt";

        FileOperationResult readResult = fileClient.read(insideNonExistent);
        assertThat(readResult.isError()).isTrue();
        assertThat(readResult.getErrorMessage()).contains("does not exist");

        FileOperationResult deleteResult = fileClient.delete(insideNonExistent);
        assertThat(deleteResult.isError()).isTrue();
        assertThat(deleteResult.getErrorMessage()).contains("does not exist");

        // Path OUTSIDE sandbox → "Access denied"
        String outsidePath = "/non/existent/path/file.txt";
        FileOperationResult denied = fileClient.read(outsidePath);
        assertThat(denied.isError()).isTrue();
        assertThat(denied.getErrorMessage()).contains("Access denied");
    }

    @Test
    void testExistsMethod() {
        String filePath = "exists_test.txt";
        
        // File doesn't exist
        assertThat(fileClient.exists(filePath)).isFalse();
        
        // Create file
        fileClient.write(filePath, "test content");
        
        // File exists
        assertThat(fileClient.exists(filePath)).isTrue();
        
        // Delete file
        fileClient.delete(filePath);
        
        // File doesn't exist again
        assertThat(fileClient.exists(filePath)).isFalse();
    }
}