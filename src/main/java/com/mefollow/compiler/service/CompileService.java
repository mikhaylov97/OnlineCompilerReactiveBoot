package com.mefollow.compiler.service;

import com.mefollow.compiler.domain.CompileData;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static java.io.File.separator;
import static reactor.core.publisher.Mono.just;

@Service
public class CompileService {

    /**
     * Name of the folder where classes will be created
     */
    private static final String CLASSES = "classes";

    /**
     * Property of the system which helps us to find tomcat root folder
     */
    private static final String TOMCAT_HOME_PROPERTY = "catalina.home";

    /**
     * Absolute path to the tomcat root folder
     */
    private static final String TOMCAT_HOME_PATH = System.getProperty(TOMCAT_HOME_PROPERTY);

    /**
     * Path to the classes folder inside the tomcat root catalog
     */
    private static final String CLASSES_PATH = TOMCAT_HOME_PATH + separator + CLASSES;

    /**
     * Field which stores Java API of images folder
     */
    private static final File CLASSES_DIR = new File(CLASSES_PATH);

    /**
     * Absolute path to the images folder inside the tomcat root catalog
     */
    private static final String CLASSES_DIR_ABSOLUTE_PATH = CLASSES_DIR.getAbsolutePath() + separator;

    private static final String JAVAC_COMMAND = "javac";
    private static final String JAVA_COMMAND = "java";
    private static final String CLASS_NAME = "Test";
    private static final String JAVA_EXTENSION = ".java";
    private static final String CLASS_EXTENSION = ".class";
    private static final String ADDITIONAL_COMMAND = "-cp";
    private static final String SPACE = " ";

    private static final String JAVAC_COMPILING_COMMAND = JAVAC_COMMAND + SPACE + CLASSES_DIR_ABSOLUTE_PATH + CLASS_NAME + JAVA_EXTENSION;
    private static final String JAVA_RUN_COMMAND = JAVA_COMMAND + SPACE + ADDITIONAL_COMMAND + SPACE + CLASSES_DIR_ABSOLUTE_PATH + SPACE + CLASS_NAME;

    public Mono<String> compileCode(CompileData payload) {
        final var code = payload.getCode();
        final var inputParams = payload.getInputParams();
        if (!CLASSES_DIR.exists()) {
            CLASSES_DIR.mkdirs();
        }

        File javaFile = new File(CLASSES_DIR_ABSOLUTE_PATH + CLASS_NAME + JAVA_EXTENSION);
        try(BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(javaFile))) {
            stream.write(code.getBytes());
            stream.close();
        } catch (Exception e) {
            return just("Something was wrong during compiling code.");
        }

        try {
            Process compileProcess = Runtime.getRuntime().exec(JAVAC_COMPILING_COMMAND);
            String compileErrors = printLines(compileProcess.getErrorStream());
            compileProcess.waitFor();
            if (!compileErrors.isEmpty()) {
                clearDirectory();
                return just(compileErrors);
            }

            List<String> inputParameters = prepareInputParameters(inputParams);
            Process runProcess = Runtime.getRuntime().exec(JAVA_RUN_COMMAND);
            OutputStream outputStream = runProcess.getOutputStream();
            for (String parameter : inputParameters) {
                outputStream.write((parameter + System.lineSeparator()).getBytes());
                outputStream.flush();
            }
            outputStream.close();

            String runErrors = printLines(runProcess.getErrorStream());
            if (!runErrors.isEmpty()) {
                runProcess.waitFor();
                clearDirectory();

                return just(runErrors);
            }

            String runResult = printLines(runProcess.getInputStream());
            runProcess.waitFor();

            clearDirectory();

            return just(runResult);
        } catch (Exception e) {
            return just("Something was wrong during compiling code.");
        }
    }

    private List<String> prepareInputParameters(String inputParameters) {
        return Arrays.asList(inputParameters.split(System.lineSeparator()));
    }

    private void clearDirectory() {
        File javaFile = new File(CLASSES_DIR_ABSOLUTE_PATH + CLASS_NAME + JAVA_EXTENSION);
        javaFile.delete();

        File classFile = new File(CLASSES_DIR_ABSOLUTE_PATH + CLASS_NAME + CLASS_EXTENSION);
        classFile.delete();
    }

    private String printLines(InputStream inputStream) throws Exception {
        String line;
        StringBuilder result = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = in.readLine()) != null) {
            result.append(line).append(System.lineSeparator());
        }
        in.close();

        return result.toString();
    }
}
