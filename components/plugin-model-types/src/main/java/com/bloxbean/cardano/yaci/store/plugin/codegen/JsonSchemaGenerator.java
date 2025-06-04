package com.bloxbean.cardano.yaci.store.plugin.codegen;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonSchemaGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: <output‐directory>");
            System.exit(1);
        }
        String outDirPath = args[0];
        File outDir = new File(outDirPath);
        outDir.mkdirs();

        ObjectMapper jacksonMapper = JsonMapper.builder().findAndAddModules().build();

        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                jacksonMapper, SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON
        );
        configBuilder = configBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);

        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

        List<Class> allClasses = new ArrayList<>();

        allClasses.addAll(PojoClassList.domainClasses);
        allClasses.addAll(PojoClassList.eventClasses);

        for (Class<?> cls : allClasses) {
            String simpleName = cls.getSimpleName();
            ObjectNode schemaNode = generator.generateSchema(cls);
            File outFile = new File(outDir, simpleName + ".json");
            jacksonMapper.writerWithDefaultPrettyPrinter().writeValue(outFile, schemaNode);
            System.out.println("Wrote schema for " + cls.getName() + " → " + outFile.getAbsolutePath());
        }

        System.out.println("Done: generated " + allClasses.size() + " schema files under " + outDir.getAbsolutePath());
    }
}
