#
# © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
#
# This software is a modification of the original software Apache Spark licensed under the Apache 2.0
# license, a copy of which is below. This software contains proprietary information of
# Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
# otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
# without express written authorization from Stratio Big Data Inc., Sucursal en España.
#

from __future__ import print_function

import time
import threading
import Queue

from pyspark import SparkConf, SparkContext


def delayed(seconds):
    def f(x):
        time.sleep(seconds)
        return x
    return f


def call_in_background(f, *args):
    result = Queue.Queue(1)
    t = threading.Thread(target=lambda: result.put(f(*args)))
    t.daemon = True
    t.start()
    return result


def main():
    conf = SparkConf().set("spark.ui.showConsoleProgress", "false")
    sc = SparkContext(appName="PythonStatusAPIDemo", conf=conf)

    def run():
        rdd = sc.parallelize(range(10), 10).map(delayed(2))
        reduced = rdd.map(lambda x: (x, 1)).reduceByKey(lambda x, y: x + y)
        return reduced.map(delayed(2)).collect()

    result = call_in_background(run)
    status = sc.statusTracker()
    while result.empty():
        ids = status.getJobIdsForGroup()
        for id in ids:
            job = status.getJobInfo(id)
            print("Job", id, "status: ", job.status)
            for sid in job.stageIds:
                info = status.getStageInfo(sid)
                if info:
                    print("Stage %d: %d tasks total (%d active, %d complete)" %
                          (sid, info.numTasks, info.numActiveTasks, info.numCompletedTasks))
        time.sleep(1)

    print("Job results are:", result.get())
    sc.stop()

if __name__ == "__main__":
    main()
