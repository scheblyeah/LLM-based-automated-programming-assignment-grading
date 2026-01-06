package com.backend.rest.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class DockerCodeCompilerService {

    private DockerClient createDockerClient() {
        OkDockerHttpClient dockerHttpClient = new OkDockerHttpClient.Builder()
                .dockerHost(URI.create("npipe:////./pipe/docker_engine"))  // For Windows npipe protocol
                .build();

        return DockerClientBuilder.getInstance()
                .withDockerHttpClient(dockerHttpClient)
                .build();
    }

    public static class TestResult {
        public String testName;
        public boolean passed;

        TestResult(String testName, boolean passed) {
            this.testName = testName;
            this.passed = passed;
        }
    }

    public static class RunTestsResult {
        public String consoleOutput;
        public List<TestResult> testResults;

        RunTestsResult(String consoleOutput, List<TestResult> testResults) {
            this.consoleOutput = consoleOutput;
            this.testResults = testResults;
        }
    }

    // Pull Docker image if not available locally
    private void pullDockerImageIfNecessary(DockerClient dockerClient, String imageName) throws InterruptedException {
        boolean imageExists = dockerClient.listImagesCmd()
                .exec()
                .stream()
                .anyMatch(image -> {
                    String[] repoTags = image.getRepoTags();
                    if (repoTags == null) return false;
                    for (String tag : repoTags) {
                        if (tag.equals(imageName)) {
                            return true;
                        }
                    }
                    return false;
                });

        if (!imageExists) {
            dockerClient.pullImageCmd(imageName).start().awaitCompletion();
        }
    }

    public RunTestsResult runJUnitTests(String assignmentSourceCode, String assignmentFileName, String testSourceCode, String testFileName) throws IOException, InterruptedException {
        // Step 1: Setup directories
        String localDir = new File(".").getAbsolutePath();
        File dataForDockerDir = new File(localDir, "datafordocker");
        if (!dataForDockerDir.exists()) {
            dataForDockerDir.mkdir();
        }
        File reportsDir = new File(dataForDockerDir, "reports");
        reportsDir.mkdirs();

        localDir = dataForDockerDir.getAbsolutePath();

        // Step 2: Write source and test code
        File sourceFile = new File(localDir, assignmentFileName);
        File testFile = new File(localDir, testFileName);
        writeToFile(sourceFile, assignmentSourceCode);
        writeToFile(testFile, testSourceCode);

        DockerClient dockerClient = createDockerClient();

        // Step 3: Setup Docker
        pullDockerImageIfNecessary(dockerClient, "java-junit:latest");
        String containerWorkingDir = "/usr/src/myapp/code";
        Volume containerVolume = new Volume(containerWorkingDir);

        CreateContainerResponse container = dockerClient.createContainerCmd("java-junit:latest")
                .withBinds(new Bind(localDir, containerVolume))
                .withWorkingDir("/usr/src/myapp")
                .withCmd("tail", "-f", "/dev/null")
                .exec();

        String containerId = container.getId();
        dockerClient.startContainerCmd(containerId).exec();

        StringBuilder output = new StringBuilder();

        try {
            // Step 4: Compile source and test files
            executeDockerCommand(dockerClient, containerId, output,
                    "javac", containerWorkingDir + "/" + assignmentFileName);
            executeDockerCommand(dockerClient, containerId, output,
                    "javac", "-cp", "junit-platform-console-standalone.jar:/usr/src/myapp/code:.", containerWorkingDir + "/" + testFileName);

            // Step 5: Run JUnit tests with XML reports
            executeDockerCommand(dockerClient, containerId, output,
                    "java", "-cp", "junit-platform-console-standalone.jar:/usr/src/myapp/code:.",
                    "org.junit.platform.console.ConsoleLauncher",
                    "--select-class", testFileName.replace(".java", ""),
                    "--reports-dir", "code/reports");

            // Step 6: Parse XML reports
            List<TestResult> testResults = parseJUnitReports(reportsDir);

            return new RunTestsResult(output.toString(), testResults);

        } finally {
            // Step 7: Cleanup
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
            deleteDirectory(dataForDockerDir);
        }
    }

    private List<TestResult> parseJUnitReports(File reportsDir) throws IOException {
        List<TestResult> results = new ArrayList<>();
        File[] reportFiles = reportsDir.listFiles((dir, name) -> name.endsWith(".xml"));
        if (reportFiles == null) return results;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            for (File reportFile : reportFiles) {
                Document doc = builder.parse(reportFile);
                doc.getDocumentElement().normalize();

                NodeList testCases = doc.getElementsByTagName("testcase");
                for (int i = 0; i < testCases.getLength(); i++) {
                    Element testCase = (Element) testCases.item(i);
                    String testName = testCase.getAttribute("name");
                    boolean passed = testCase.getElementsByTagName("failure").getLength() == 0;
                    results.add(new TestResult(testName, passed));
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse JUnit XML reports: " + e.getMessage(), e);
        }
        return results;
    }

    // Utility function to delete directories and their contents recursively
    private static boolean deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) { // Check for null in case the directory is empty
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // Recursively delete subdirectories
                } else {
                    boolean fileDeleted = file.delete(); // Delete files
                    if (!fileDeleted) {
                        System.out.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        return dir.delete(); // Delete the now-empty directory
    }

    private void writeToFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Replace literal "\n" with actual newlines
            String processedContent = content.replace("\\n", System.lineSeparator());
            writer.write(processedContent);
        }
    }
    private void executeDockerCommand(DockerClient dockerClient, String containerId, StringBuilder output, String... command) throws InterruptedException {
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(command)
                .exec();

        dockerClient.execStartCmd(exec.getId())
                .exec(new ExecStartResultCallback() {
                    @Override
                    public void onNext(Frame item) {
                        output.append(new String(item.getPayload()));
                        super.onNext(item);
                    }
                }).awaitCompletion();
    }
}