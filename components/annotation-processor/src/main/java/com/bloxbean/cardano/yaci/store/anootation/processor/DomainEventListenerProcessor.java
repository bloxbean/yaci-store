package com.bloxbean.cardano.yaci.store.anootation.processor;
import com.bloxbean.cardano.yaci.store.events.annotation.DomainEventListener;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.Writer;
import java.util.*;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.bloxbean.cardano.yaci.store.events.annotation.DomainEventListener")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class DomainEventListenerProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();

        for (int i = 0; i < 100; i++) {
            messager.printMessage(Diagnostic.Kind.NOTE, "DomainEventListenerProcessor initialized>>>>");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, List<ExecutableElement>> handlerMethodsByClass = new HashMap<>();

        for (int i = 0; i < 100; i++) {
            messager.printMessage(Diagnostic.Kind.NOTE, "$$$$$$$$DomainEventListenerProcessor initialized>>>>");
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(DomainEventListener.class)) {
            if (!(element instanceof ExecutableElement method)) continue;
            TypeElement classElement = (TypeElement) method.getEnclosingElement();
            String className = classElement.getQualifiedName().toString();

            handlerMethodsByClass.computeIfAbsent(className, k -> new ArrayList<>()).add(method);
        }

        for (Map.Entry<String, List<ExecutableElement>> entry : handlerMethodsByClass.entrySet()) {
            String handlerClass = entry.getKey();
            List<ExecutableElement> methods = entry.getValue();
            String simpleClassName = handlerClass.substring(handlerClass.lastIndexOf('.') + 1);
            String handlerPackage = handlerClass.substring(0, handlerClass.lastIndexOf('.'));
            String generatedPackage = handlerPackage + ".generated";

            generateSpringAdapter(generatedPackage, simpleClassName, handlerClass, methods);
            generateQuarkusAdapter(generatedPackage, simpleClassName, handlerClass, methods);
        }

        return true;
    }

    private void generateSpringAdapter(String packageName, String handlerSimple, String handlerClass, List<ExecutableElement> methods) {
        try (Writer writer = filer.createSourceFile(packageName + "." + handlerSimple + "_SpringAdapter").openWriter()) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("import org.springframework.context.event.EventListener;\n");
            writer.write("import org.springframework.stereotype.Component;\n\n");
            writer.write("@Component\n");
            writer.write("public class " + handlerSimple + "_SpringAdapter {\n\n");
            writer.write("    private final " + handlerClass + " delegate;\n\n");
            writer.write("    public " + handlerSimple + "_SpringAdapter(" + handlerClass + " delegate) {\n");
            writer.write("        this.delegate = delegate;\n    }\n\n");

            for (ExecutableElement method : methods) {
                String eventType = method.getParameters().get(0).asType().toString();
                String methodName = method.getSimpleName().toString();
                writer.write("    @EventListener\n");
                writer.write("    public void " + methodName + "(" + eventType + " event) {\n");
                writer.write("        delegate." + methodName + "(event);\n    }\n\n");
            }
            writer.write("}\n");
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate Spring adapter: " + e.getMessage());
        }
    }

    private void generateQuarkusAdapter(String packageName, String handlerSimple, String handlerClass, List<ExecutableElement> methods) {
        try (Writer writer = filer.createSourceFile(packageName + "." + handlerSimple + "_QuarkusAdapter").openWriter()) {
            writer.write("package " + packageName + ";\n\n");
            writer.write("import jakarta.enterprise.context.ApplicationScoped;\n");
            writer.write("import jakarta.enterprise.event.Observes;\n");
            writer.write("import jakarta.inject.Inject;\n\n");
            writer.write("@ApplicationScoped\n");
            writer.write("public class " + handlerSimple + "_QuarkusAdapter {\n\n");
            writer.write("    @Inject\n");
            writer.write("    " + handlerClass + " delegate;\n\n");

            for (ExecutableElement method : methods) {
                String eventType = method.getParameters().get(0).asType().toString();
                String methodName = method.getSimpleName().toString();
                writer.write("    public void " + methodName + "(@Observes " + eventType + " event) {\n");
                writer.write("        delegate." + methodName + "(event);\n    }\n\n");
            }
            writer.write("}\n");
        } catch (Exception e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Failed to generate Quarkus adapter: " + e.getMessage());
        }
    }
}
