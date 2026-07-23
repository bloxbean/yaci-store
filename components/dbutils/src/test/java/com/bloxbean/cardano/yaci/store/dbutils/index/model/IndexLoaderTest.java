package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndexLoaderTest {

    @Test
    void loadIndexes() {
        List<IndexDefinition> indexDefinitions = new IndexLoader().loadIndexes("index.yml");
        assertThat(indexDefinitions).hasSizeGreaterThan(5);
        assertThat(indexDefinitions.get(0)).isInstanceOf(IndexDefinition.class);
    }

    @Test
    void loadExtraIndexes() {
        List<IndexDefinition> indexDefinitions = new IndexLoader().loadIndexes("extra-index.yml");
        assertThat(indexDefinitions).hasSizeGreaterThan(0);
        assertThat(indexDefinitions.get(0)).isInstanceOf(IndexDefinition.class);

        assertThat(indexDefinitions.get(0).getIndexes().get(0).getName()).isNotBlank();
        assertThat(indexDefinitions.get(0).getIndexes().get(0).getType()).isEqualTo("gin");
        assertThat(indexDefinitions.get(0).getIndexes().get(0).getExcludes()).hasSizeGreaterThan(0);
    }

    @Test
    void loadBlockfrostIndexes() {
        List<IndexDefinition> indexDefinitions = new IndexLoader().loadIndexes("blockfrost-index.yml");
        assertThat(indexDefinitions).hasSizeGreaterThan(0);
        assertThat(indexDefinitions).hasSizeGreaterThan(5);
        assertThat(indexDefinitions.get(0)).isInstanceOf(IndexDefinition.class);
    }

    @Test
    void loadIndexesFromMultipleFiles() {
        List<IndexDefinition> indexDefinitions = new IndexLoader()
                .loadIndexesFromMultipleFiles("test-index-1.yml", "test-index-2.yml");

        assertThat(indexDefinitions).hasSize(2);
        assertThat(indexDefinitions)
                .extracting(IndexDefinition::getTable)
                .containsExactly("test_table_1", "test_table_2");
    }

    @Test
    void loadIndexesFromFileResourcePath() throws Exception {
        var resource = getClass().getClassLoader().getResource("test-index-1.yml");
        assertThat(resource).isNotNull();

        String filePath = Path.of(resource.toURI()).toUri().toString();
        List<IndexDefinition> indexDefinitions = new IndexLoader()
                .loadIndexesFromMultipleFiles(filePath);

        assertThat(indexDefinitions).hasSize(1);
        assertThat(indexDefinitions.get(0).getTable()).isEqualTo("test_table_1");
    }

    @Test
    void loadIndexesFromMultipleFilesShouldAppendYmlSuffix() {
        List<IndexDefinition> indexDefinitions = new IndexLoader()
                .loadIndexesFromMultipleFiles("test-index-1");

        assertThat(indexDefinitions).hasSize(1);
        assertThat(indexDefinitions.get(0).getTable()).isEqualTo("test_table_1");
    }

    @Test
    void loadIndexesFromMultipleFilesShouldThrowForMissingFile() {
        assertThrows(IllegalArgumentException.class, () ->
                new IndexLoader().loadIndexesFromMultipleFiles("missing-index"));
    }
}
