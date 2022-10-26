package org.aldebaran.worker;

import java.io.IOException;
import java.util.Map;

import org.aldebaran.common.utils.jobs.JobManagementUtils;
import org.aldebaran.common.utils.log.AppLogger;
import org.aldebaran.common.utils.run.Callable;
import org.aldebaran.common.utils.run.ParallelExecutor;
import org.aldebaran.common.utils.run.TaskFinishedListener;
import org.aldebaran.common.utils.serialization.SerializationUtils;

/**
 * WorkerApplication.
 *
 * @author Alejandro
 *
 */
public final class WorkerApplication {

    private static String pathJobSerialization;
    private static int tasksFinished;

    private WorkerApplication() {
    }

    /**
     * Main worker method.
     *
     * @param args
     *            args
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {

        pathJobSerialization = args[0];

        try {
            executeWorker();

        } catch (final Exception e) {

            AppLogger.logError("Error executing worker", e);
            JobManagementUtils.storeFailedJob(pathJobSerialization, e);

        } finally {
            System.exit(0);
        }
    }

    private static void executeWorker() throws Exception {

        final byte[] serializedJob = JobManagementUtils.readJob(pathJobSerialization);

        final Map<String, Callable<?>> mapIdJob = SerializationUtils.deserializeObjectFromByteArray(serializedJob);

        final TaskFinishedListener taskFinishedListener = new TaskFinishedListener(WorkerApplication::onTaskFinished);

        final Map<String, Object> mapIdResult = ParallelExecutor.execute(20, mapIdJob, taskFinishedListener);

        final byte[] baObj = SerializationUtils.serializeObject(mapIdResult);

        JobManagementUtils.storeEndOfJob(pathJobSerialization, baObj);
    }

    private static Void onTaskFinished(final String idTask) {

        tasksFinished++;

        try {
            JobManagementUtils.storeProgress(pathJobSerialization, tasksFinished);

        } catch (final IOException e) {
            AppLogger.logError("Error storing the progress", e);
        }

        return null;
    }
}
