package com.mefollow.compiler.service;

import com.mefollow.compiler.domain.CompileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.getTempDirectoryPath;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static reactor.core.publisher.Mono.just;

@Service
public class CompileService {

    private final Logger log = LoggerFactory.getLogger(CompileService.class);

    /**
     * Absolute path to the images folder inside the tomcat root catalog
     */
    private static final String CLASSES_DIR_ABSOLUTE_PATH = getTempDirectoryPath();

    private static final String JAVAC_COMMAND = "javac";
    private static final String JAVA_COMMAND = "java";
    private static final String CLASS_NAME = "Test";
    private static final String JAVA_EXTENSION = ".java";
    private static final String CLASS_EXTENSION = ".class";
    private static final String ADDITIONAL_COMMAND = "-cp";
    private static final String SPACE = " ";
    private static final String SLASH = "/";

    private static final String JAVA_FILE_PATH = String.format("%s/%s%s", getTempDirectoryPath(), CLASS_NAME, JAVA_EXTENSION);
    private static final String JAVA_CLASS_PATH = String.format("%s/%s%s", getTempDirectoryPath(), CLASS_NAME, CLASS_EXTENSION);

    private static final String JAVAC_COMPILING_COMMAND = JAVAC_COMMAND + SPACE + "-verbose" + SPACE + CLASSES_DIR_ABSOLUTE_PATH + SLASH + CLASS_NAME + JAVA_EXTENSION;
    private static final String JAVA_RUN_COMMAND = JAVA_COMMAND + SPACE + ADDITIONAL_COMMAND + SPACE + CLASSES_DIR_ABSOLUTE_PATH + SPACE + CLASS_NAME;

    public Mono<String> compileCode(CompileData payload) {
        final var code = payload.getCode();
        final var inputParams = payload.getInputParams();

        log.info("Javac command: " + JAVAC_COMPILING_COMMAND);
        log.info("Java run command: " + JAVA_RUN_COMMAND);

        final var javaFile = new File(JAVA_FILE_PATH);
        log.info("Java file path: " + JAVA_FILE_PATH);
        log.info("Java file: " + javaFile.toString());
        try(BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(javaFile))) {
            stream.write(code.getBytes());
        } catch (Exception e) {
            clearDirectory();
            return just("Something was wrong during compiling code.");
        }

        log.info("Java file exists: " + new File(JAVA_FILE_PATH).exists());

        try {
            Process compileProcess = Runtime.getRuntime().exec(JAVAC_COMPILING_COMMAND);
            String compileErrors = printLines(compileProcess.getErrorStream());
            compileProcess.waitFor();
//            if (!compileErrors.isEmpty()) {
//                log.info("Compilation error is occurred - clearing folder.");
//                clearDirectory();
//                return just(compileErrors);
//            }

            log.info("Java class file exists: " + new File(JAVA_CLASS_PATH).exists());

            List<String> inputParameters = prepareInputParameters(inputParams);
            log.info("Input params: " + inputParameters);
            Process runProcess = Runtime.getRuntime().exec(JAVA_RUN_COMMAND);
            OutputStream outputStream = runProcess.getOutputStream();
            for (String parameter : inputParameters) {
                outputStream.write((parameter + System.lineSeparator()).getBytes());
                outputStream.flush();
            }
            outputStream.close();

            String runErrors = printLines(runProcess.getErrorStream());
            if (!runErrors.isEmpty()) {
                log.info("Execution error is occurred - clearing folder.");
                runProcess.waitFor();
                clearDirectory();

                return just(runErrors);
            }

            String runResult = printLines(runProcess.getInputStream());
            log.info("Run result: " + runResult);
            runProcess.waitFor();

            clearDirectory();

            return just(runResult);
        } catch (Exception e) {
            clearDirectory();
            return just("Something was wrong during compiling code.");
        }
    }

    private List<String> prepareInputParameters(String inputParameters) {
        return isNotBlank(inputParameters) ? Arrays.asList(inputParameters.split(System.lineSeparator())) : emptyList();
    }

    private void clearDirectory() {
        File javaFile = new File(JAVA_FILE_PATH);
        javaFile.delete();

        File classFile = new File(JAVA_CLASS_PATH);
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
