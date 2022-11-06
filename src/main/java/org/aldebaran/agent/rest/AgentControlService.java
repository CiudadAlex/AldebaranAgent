package org.aldebaran.agent.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aldebaran.agent.constants.AgentProperties;
import org.aldebaran.agent.security.SecurityChecker;
import org.aldebaran.common.dto.FileDTO;
import org.aldebaran.common.dto.JobDTO;
import org.aldebaran.common.dto.ProgressAndResultDTO;
import org.aldebaran.common.dto.RedistributionOfJARsDTO;
import org.aldebaran.common.utils.file.FileUtils;
import org.aldebaran.common.utils.jar.JarManagementUtils;
import org.aldebaran.common.utils.jobs.JobManagementUtils;
import org.aldebaran.common.utils.os.OS;
import org.aldebaran.common.utils.os.OSUtils;
import org.aldebaran.common.utils.run.ProcessUtils;
import org.aldebaran.common.utils.serialization.HashUtils;
import org.aldebaran.common.utils.text.TextUtils;
import org.aldebaran.worker.WorkerApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * AgentControlService.
 *
 * @author Alejandro
 *
 */
@RestController
public class AgentControlService {

    // FIXME - HTTPS
    // https://www.baeldung.com/spring-boot-https-self-signed-certificate

    private static final Logger LOG = Logger.getLogger(AgentControlService.class.getName());

    @Autowired
    private Environment env;

    /**
     * Echo service.
     *
     * @param name
     *            name
     * @return echo
     */
    @GetMapping("/echo/{name}")
    public String echo(@PathVariable final String name) {

        LOG.log(Level.INFO, "echo called");

        return "echo " + name;
    }

    private void securityCheck(final HttpHeaders headers) throws Exception {

        final String servicePassword = env.getProperty(AgentProperties.SERVICE_PASSWORD);
        SecurityChecker.securityCheck(headers, servicePassword);
    }

    /**
     * Redistribution of JARs.
     *
     * @param headers
     *            headers
     * @param redistributionOfJARsDTO
     *            redistributionOfJARsDTO
     * @throws IOException
     */
    @PostMapping("/redistributeJars/")
    public void redistributeJars(@RequestHeader final HttpHeaders headers,
            @RequestBody final RedistributionOfJARsDTO redistributionOfJARsDTO) throws Exception {

        securityCheck(headers);

        LOG.log(Level.INFO, "redistributeJars called");

        final String applicationName = redistributionOfJARsDTO.getApplicationName();
        final String pathToStore = buildPathJARS(applicationName);
        JarManagementUtils.storeJARsReceived(redistributionOfJARsDTO, pathToStore);

        final String servicePassword = env.getProperty(AgentProperties.SERVICE_PASSWORD);
        JarManagementUtils.redistributeJARs(servicePassword, redistributionOfJARsDTO);
    }

    private String buildPathJARS(final String applicationName) {

        final String pathToStore = env.getProperty(AgentProperties.PATH_STORE_JARS) + File.separatorChar + applicationName;
        return pathToStore;
    }

    private String buildPathJobSerializations(final String applicationName) {

        final String pathToStore = env.getProperty(AgentProperties.PATH_STORE_JOB_SERIALIZATION) + File.separatorChar + applicationName;
        return pathToStore;
    }

    private boolean isWorkerDebugOn() {

        final String strWorkerDebug = env.getProperty(AgentProperties.WORKER_DEBUG);
        return Boolean.parseBoolean(strWorkerDebug);
    }

    /**
     * Checks if the JARs are up to date.
     *
     * @param headers
     *            headers
     * @param applicationName
     *            applicationName
     * @param hash
     *            hash
     * @return if the JARs are up to date
     */
    @GetMapping("/checkJarsUpToDate/{applicationName}/{hash}")
    public Boolean checkJarsUpToDate(@RequestHeader final HttpHeaders headers, @PathVariable final String applicationName,
            @PathVariable final Integer hash) throws Exception {

        securityCheck(headers);

        LOG.log(Level.INFO, "checkJarsUpToDate called");

        final String path = buildPathJARS(applicationName);
        final File dir = new File(path);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        LOG.log(Level.INFO, "Absolute jar path: " + dir.getAbsolutePath());

        final RedistributionOfJARsDTO redistributionOfJARsDTO = JarManagementUtils.prepareJARsToSend(applicationName, path,
                new ArrayList<String>(), 0);

        final Integer hashAgent = HashUtils.getHash(redistributionOfJARsDTO);
        final boolean correctHash = hash.equals(hashAgent);

        LOG.log(Level.INFO, "correctHash = " + correctHash + " >> " + hash + " " + hashAgent);

        return correctHash;
    }

    /**
     * Executes the worker.
     *
     * @param headers
     *            headers
     * @param jobDTO
     *            jobDTO
     * @throws IOException
     */
    @PostMapping("/executeWorker/")
    public void executeWorker(@RequestHeader final HttpHeaders headers, @RequestBody final JobDTO jobDTO) throws Exception {

        securityCheck(headers);

        LOG.log(Level.INFO, "executeWorker called");

        final String applicationName = jobDTO.getApplicationName();
        final String baMapStringCallableB64 = jobDTO.getBaMapStringCallableB64();

        final String pathJobSerialization = buildPathJobSerializations(applicationName);

        JobManagementUtils.storeInitializationOfJob(pathJobSerialization, baMapStringCallableB64);

        final String javaExecutable = env.getProperty(AgentProperties.WORKER_JAVA_EXECUTABLE);
        final String classpathSeparator = env.getProperty(AgentProperties.WORKER_JAVA_CLASSPATH_SEPARATOR).trim();

        final List<String> listFileClasspath = getFilesCurrentClasspath();

        final String pathToStoreJars = buildPathJARS(applicationName);
        final String commandLine = javaExecutable + " -cp \"" + TextUtils.concatList(listFileClasspath, classpathSeparator)
                + classpathSeparator + pathToStoreJars + "/*\" " + WorkerApplication.class.getName() + " \"" + pathJobSerialization + "\"";

        LOG.log(Level.INFO, "executing command line: " + commandLine);

        final OS os = OSUtils.getOS();

        if (OS.LINUX.equals(os)) {
            executeCommandLineLinux(pathJobSerialization, commandLine);

        } else {
            executeCommandLine(commandLine);
        }
    }

    private void executeCommandLine(final String commandLine) throws Exception {

        final boolean isWorkerDebugOn = isWorkerDebugOn();

        final Process process = Runtime.getRuntime().exec(commandLine);

        if (isWorkerDebugOn) {
            ProcessUtils.logProcessOutput(process);
        }
    }

    private void executeCommandLineLinux(final String pathJobSerialization, final String commandLine) throws Exception {

        final boolean isWorkerDebugOn = isWorkerDebugOn();
        final String execFilename = "exec.sh";

        FileUtils.buildFile(pathJobSerialization, execFilename, commandLine.getBytes());
        final Process processCh = Runtime.getRuntime().exec("chmod +x " + pathJobSerialization + File.separatorChar + execFilename);
        processCh.waitFor();

        if (isWorkerDebugOn) {
            ProcessUtils.logProcessOutput(processCh);
        }

        final Process process = Runtime.getRuntime().exec("sh " + pathJobSerialization + File.separatorChar + execFilename);

        if (isWorkerDebugOn) {
            ProcessUtils.logProcessOutput(process);
        }
    }

    private static List<String> getFilesCurrentClasspath() throws Exception {

        String classpath = System.getProperty("java.class.path");
        String[] entries = classpath.split(File.pathSeparator);

        final List<String> listFileClasspath = new ArrayList<>();

        for(String classpathEntry : entries) {
            String cannonClasspathEntry = Paths.get(classpathEntry).toAbsolutePath().toUri().toURL().getFile();
            listFileClasspath.add(cannonClasspathEntry);
        }

        return listFileClasspath;
    }

    /**
     * Checks the progress of the task.
     *
     * @param headers
     *            headers
     * @param applicationName
     *            applicationName
     * @return echo
     */
    @GetMapping("/checkProgress/{applicationName}")
    public ProgressAndResultDTO checkProgress(@RequestHeader final HttpHeaders headers, @PathVariable final String applicationName)
            throws Exception {

        securityCheck(headers);

        LOG.log(Level.INFO, "checkProgress called");

        final String pathJobSerialization = buildPathJobSerializations(applicationName);
        final int progress = JobManagementUtils.readProgress(pathJobSerialization);
        String baMapStringObjectB64 = null;
        String baFailureDetailsB64 = null;

        if (JobManagementUtils.isJobFailed(pathJobSerialization)) {

            final FileDTO resultFileDTO = JobManagementUtils.readFailure(pathJobSerialization);
            baFailureDetailsB64 = resultFileDTO.getContentB64();
        }

        if (JobManagementUtils.isJobEnded(pathJobSerialization)) {

            final FileDTO resultFileDTO = JobManagementUtils.readResult(pathJobSerialization);
            baMapStringObjectB64 = resultFileDTO.getContentB64();
        }

        return new ProgressAndResultDTO(progress, baMapStringObjectB64, baFailureDetailsB64);
    }

}
