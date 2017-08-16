/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.streaming.dstream

import java.io.{IOException, ObjectInputStream, ObjectOutputStream}

import scala.collection.mutable.HashMap
import scala.reflect.ClassTag

import org.apache.hadoop.fs.{FileSystem, Path}

import org.apache.spark.internal.Logging
import org.apache.spark.streaming.Time
import org.apache.spark.util.Utils

private[streaming]
class DStreamCheckpointData[T: ClassTag](dstream: DStream[T])
  extends Serializable with Logging {
  protected val data = new HashMap[Time, AnyRef]()

  // Mapping of the batch time to the checkpointed RDD file of that time
  @transient private var timeToCheckpointFile = new HashMap[Time, String]
  // Mapping of the batch time to the time of the oldest checkpointed RDD
  // in that batch's checkpoint data
  @transient private var timeToOldestCheckpointFileTime = new HashMap[Time, Time]

  @transient private var fileSystem: FileSystem = null
  protected[streaming] def currentCheckpointFiles = data.asInstanceOf[HashMap[Time, String]]

  /**
   * Updates the checkpoint data of the DStream. This gets called every time
   * the graph checkpoint is initiated. Default implementation records the
   * checkpoint files at which the generated RDDs of the DStream have been saved.
   */
  def update(time: Time) {

    // Get the checkpointed RDDs from the generated RDDs
    val checkpointFiles = dstream.generatedRDDs.filter(_._2.getCheckpointFile.isDefined)
                                       .map(x => (x._1, x._2.getCheckpointFile.get))
    logDebug("Current checkpoint files:\n" + checkpointFiles.toSeq.mkString("\n"))

    // Add the checkpoint files to the data to be serialized
    if (!checkpointFiles.isEmpty) {
      currentCheckpointFiles.clear()
      currentCheckpointFiles ++= checkpointFiles
      // Add the current checkpoint files to the map of all checkpoint files
      // This will be used to delete old checkpoint files
      timeToCheckpointFile ++= currentCheckpointFiles
      // Remember the time of the oldest checkpoint RDD in current state
      timeToOldestCheckpointFileTime(time) = currentCheckpointFiles.keys.min(Time.ordering)
    }
  }

  /**
   * Cleanup old checkpoint data. This gets called after a checkpoint of `time` has been
   * written to the checkpoint directory.
   */
  def cleanup(time: Time) {
    // Get the time of the oldest checkpointed RDD that was written as part of the
    // checkpoint of `time`
    timeToOldestCheckpointFileTime.remove(time) match {
      case Some(lastCheckpointFileTime) =>
        // Find all the checkpointed RDDs (i.e. files) that are older than `lastCheckpointFileTime`
        // This is because checkpointed RDDs older than this are not going to be needed
        // even after master fails, as the checkpoint data of `time` does not refer to those files
        val filesToDelete = timeToCheckpointFile.filter(_._1 < lastCheckpointFileTime)
        logDebug("Files to delete:\n" + filesToDelete.mkString(","))
        filesToDelete.foreach {
          case (time, file) =>
            try {
              val path = new Path(file)
              if (fileSystem == null) {
                fileSystem = path.getFileSystem(dstream.ssc.sparkContext.hadoopConfiguration)
              }
              fileSystem.delete(path, true)
              timeToCheckpointFile -= time
              logInfo("Deleted checkpoint file '" + file + "' for time " + time)
            } catch {
              case e: Exception =>
                logWarning("Error deleting old checkpoint file '" + file + "' for time " + time, e)
                fileSystem = null
            }
        }
      case None =>
        logDebug("Nothing to delete")
    }
  }

  /**
   * Restore the checkpoint data. This gets called once when the DStream graph
   * (along with its output DStreams) is being restored from a graph checkpoint file.
   * Default implementation restores the RDDs from their checkpoint files.
   */
  def restore() {
    // Create RDDs from the checkpoint data
    currentCheckpointFiles.foreach {
      case(time, file) =>
        logInfo("Restoring checkpointed RDD for time " + time + " from file '" + file + "'")
        dstream.generatedRDDs += ((time, dstream.context.sparkContext.checkpointFile[T](file)))
    }
  }

  override def toString: String = {
    "[\n" + currentCheckpointFiles.size + " checkpoint files \n" +
      currentCheckpointFiles.mkString("\n") + "\n]"
  }

  @throws(classOf[IOException])
  private def writeObject(oos: ObjectOutputStream): Unit = Utils.tryOrIOException {
    logDebug(this.getClass().getSimpleName + ".writeObject used")
    if (dstream.context.graph != null) {
      dstream.context.graph.synchronized {
        if (dstream.context.graph.checkpointInProgress) {
          oos.defaultWriteObject()
        } else {
          val msg = "Object of " + this.getClass.getName + " is being serialized " +
            " possibly as a part of closure of an RDD operation. This is because " +
            " the DStream object is being referred to from within the closure. " +
            " Please rewrite the RDD operation inside this DStream to avoid this. " +
            " This has been enforced to avoid bloating of Spark tasks " +
            " with unnecessary objects."
          throw new java.io.NotSerializableException(msg)
        }
      }
    } else {
      throw new java.io.NotSerializableException(
        "Graph is unexpectedly null when DStream is being serialized.")
    }
  }

  @throws(classOf[IOException])
  private def readObject(ois: ObjectInputStream): Unit = Utils.tryOrIOException {
    logDebug(this.getClass().getSimpleName + ".readObject used")
    ois.defaultReadObject()
    timeToOldestCheckpointFileTime = new HashMap[Time, Time]
    timeToCheckpointFile = new HashMap[Time, String]
  }
}
