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

from pyspark import SparkContext
# $example on$
from pyspark.mllib.stat import KernelDensity
# $example off$

if __name__ == "__main__":
    sc = SparkContext(appName="KernelDensityEstimationExample")  # SparkContext

    # $example on$
    # an RDD of sample data
    data = sc.parallelize([1.0, 1.0, 1.0, 2.0, 3.0, 4.0, 5.0, 5.0, 6.0, 7.0, 8.0, 9.0, 9.0])

    # Construct the density estimator with the sample data and a standard deviation for the Gaussian
    # kernels
    kd = KernelDensity()
    kd.setSample(data)
    kd.setBandwidth(3.0)

    # Find density estimates for the given values
    densities = kd.estimate([-1.0, 2.0, 5.0])
    # $example off$

    print(densities)

    sc.stop()
