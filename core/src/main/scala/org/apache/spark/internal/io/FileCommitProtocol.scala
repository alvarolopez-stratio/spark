/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.internal.io

import org.apache.hadoop.mapreduce._

import org.apache.spark.util.Utils


/**
 * An interface to define how a single Spark job commits its outputs. Two notes:
 *
 * 1. Implementations must be serializable, as the committer instance instantiated on the driver
 *    will be used for tasks on executors.
 * 2. Implementations should have a constructor with either 2 or 3 arguments:
 *    (jobId: String, path: String) or (jobId: String, path: String, isAppend: Boolean).
 * 3. A committer should not be reused across multiple Spark jobs.
 *
 * The proper call sequence is:
 *
 * 1. Driver calls setupJob.
 * 2. As part of each task's execution, executor calls setupTask and then commitTask
 *    (or abortTask if task failed).
 * 3. When all necessary tasks completed successfully, the driver calls commitJob. If the job
 *    failed to execute (e.g. too many failed tasks), the job should call abortJob.
 */
abstract class FileCommitProtocol {
  import FileCommitProtocol._

  /**
   * Setups up a job. Must be called on the driver before any other methods can be invoked.
   */
  def setupJob(jobContext: JobContext): Unit

  /**
   * Commits a job after the writes succeed. Must be called on the driver.
   */
  def commitJob(jobContext: JobContext, taskCommits: Seq[TaskCommitMessage]): Unit

  /**
   * Aborts a job after the writes fail. Must be called on the driver.
   *
   * Calling this function is a best-effort attempt, because it is possible that the driver
   * just crashes (or killed) before it can call abort.
   */
  def abortJob(jobContext: JobContext): Unit

  /**
   * Sets up a task within a job.
   * Must be called before any other task related methods can be invoked.
   */
  def setupTask(taskContext: TaskAttemptContext): Unit

  /**
   * Notifies the commit protocol to add a new file, and gets back the full path that should be
   * used. Must be called on the executors when running tasks.
   *
   * Note that the returned temp file may have an arbitrary path. The commit protocol only
   * promises that the file will be at the location specified by the arguments after job commit.
   *
   * A full file path consists of the following parts:
   *  1. the base path
   *  2. some sub-directory within the base path, used to specify partitioning
   *  3. file prefix, usually some unique job id with the task id
   *  4. bucket id
   *  5. source specific file extension, e.g. ".snappy.parquet"
   *
   * The "dir" parameter specifies 2, and "ext" parameter specifies both 4 and 5, and the rest
   * are left to the commit protocol implementation to decide.
   *
   * Important: it is the caller's responsibility to add uniquely identifying content to "ext"
   * if a task is going to write out multiple files to the same dir. The file commit protocol only
   * guarantees that files written by different tasks will not conflict.
   */
  def newTaskTempFile(taskContext: TaskAttemptContext, dir: Option[String], ext: String): String

  /**
   * Similar to newTaskTempFile(), but allows files to committed to an absolute output location.
   * Depending on the implementation, there may be weaker guarantees around adding files this way.
   *
   * Important: it is the caller's responsibility to add uniquely identifying content to "ext"
   * if a task is going to write out multiple files to the same dir. The file commit protocol only
   * guarantees that files written by different tasks will not conflict.
   */
  def newTaskTempFileAbsPath(
      taskContext: TaskAttemptContext, absoluteDir: String, ext: String): String

  /**
   * Commits a task after the writes succeed. Must be called on the executors when running tasks.
   */
  def commitTask(taskContext: TaskAttemptContext): TaskCommitMessage

  /**
   * Aborts a task after the writes have failed. Must be called on the executors when running tasks.
   *
   * Calling this function is a best-effort attempt, because it is possible that the executor
   * just crashes (or killed) before it can call abort.
   */
  def abortTask(taskContext: TaskAttemptContext): Unit
}


object FileCommitProtocol {
  class TaskCommitMessage(val obj: Any) extends Serializable

  object EmptyTaskCommitMessage extends TaskCommitMessage(null)

  /**
   * Instantiates a FileCommitProtocol using the given className.
   */
  def instantiate(className: String, jobId: String, outputPath: String, isAppend: Boolean)
    : FileCommitProtocol = {
    val clazz = Utils.classForName(className).asInstanceOf[Class[FileCommitProtocol]]

    // First try the one with argument (jobId: String, outputPath: String, isAppend: Boolean).
    // If that doesn't exist, try the one with (jobId: string, outputPath: String).
    try {
      val ctor = clazz.getDeclaredConstructor(classOf[String], classOf[String], classOf[Boolean])
      ctor.newInstance(jobId, outputPath, isAppend.asInstanceOf[java.lang.Boolean])
    } catch {
      case _: NoSuchMethodException =>
        val ctor = clazz.getDeclaredConstructor(classOf[String], classOf[String])
        ctor.newInstance(jobId, outputPath)
    }
  }
}
