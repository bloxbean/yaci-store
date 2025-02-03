package com.bloxbean.cardano.yaci.store.dbutils.index.model;

import com.bloxbean.cardano.yaci.store.dbutils.index.util.IndexLoader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
