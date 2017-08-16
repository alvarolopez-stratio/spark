#
# © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
#
# This software is a modification of the original software Apache Spark licensed under the Apache 2.0
# license, a copy of which is below. This software contains proprietary information of
# Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
# otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
# without express written authorization from Stratio Big Data Inc., Sucursal en España.
#

"""
 Shows the most positive words in UTF8 encoded, '\n' delimited text directly received the network
 every 5 seconds. The streaming data is joined with a static RDD of the AFINN word list
 (http://neuro.imm.dtu.dk/wiki/AFINN)

 Usage: network_wordjoinsentiments.py <hostname> <port>
   <hostname> and <port> describe the TCP server that Spark Streaming would connect to receive data.

 To run this on your local machine, you need to first run a Netcat server
    `$ nc -lk 9999`
 and then run the example
    `$ bin/spark-submit examples/src/main/python/streaming/network_wordjoinsentiments.py \
    localhost 9999`
"""

from __future__ import print_function

import sys

from pyspark import SparkContext
from pyspark.streaming import StreamingContext


def print_happiest_words(rdd):
    top_list = rdd.take(5)
    print("Happiest topics in the last 5 seconds (%d total):" % rdd.count())
    for tuple in top_list:
        print("%s (%d happiness)" % (tuple[1], tuple[0]))

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: network_wordjoinsentiments.py <hostname> <port>", file=sys.stderr)
        exit(-1)

    sc = SparkContext(appName="PythonStreamingNetworkWordJoinSentiments")
    ssc = StreamingContext(sc, 5)

    # Read in the word-sentiment list and create a static RDD from it
    word_sentiments_file_path = "data/streaming/AFINN-111.txt"
    word_sentiments = ssc.sparkContext.textFile(word_sentiments_file_path) \
        .map(lambda line: tuple(line.split("\t")))

    lines = ssc.socketTextStream(sys.argv[1], int(sys.argv[2]))

    word_counts = lines.flatMap(lambda line: line.split(" ")) \
        .map(lambda word: (word, 1)) \
        .reduceByKey(lambda a, b: a + b)

    # Determine the words with the highest sentiment values by joining the streaming RDD
    # with the static RDD inside the transform() method and then multiplying
    # the frequency of the words by its sentiment value
    happiest_words = word_counts.transform(lambda rdd: word_sentiments.join(rdd)) \
        .map(lambda (word, tuple): (word, float(tuple[0]) * tuple[1])) \
        .map(lambda (word, happiness): (happiness, word)) \
        .transform(lambda rdd: rdd.sortByKey(False))

    happiest_words.foreachRDD(print_happiest_words)

    ssc.start()
    ssc.awaitTermination()
