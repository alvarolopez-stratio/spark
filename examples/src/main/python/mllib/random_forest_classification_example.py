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
Random Forest Classification Example.
"""
from __future__ import print_function

from pyspark import SparkContext
# $example on$
from pyspark.mllib.tree import RandomForest, RandomForestModel
from pyspark.mllib.util import MLUtils
# $example off$

if __name__ == "__main__":
    sc = SparkContext(appName="PythonRandomForestClassificationExample")
    # $example on$
    # Load and parse the data file into an RDD of LabeledPoint.
    data = MLUtils.loadLibSVMFile(sc, 'data/mllib/sample_libsvm_data.txt')
    # Split the data into training and test sets (30% held out for testing)
    (trainingData, testData) = data.randomSplit([0.7, 0.3])

    # Train a RandomForest model.
    #  Empty categoricalFeaturesInfo indicates all features are continuous.
    #  Note: Use larger numTrees in practice.
    #  Setting featureSubsetStrategy="auto" lets the algorithm choose.
    model = RandomForest.trainClassifier(trainingData, numClasses=2, categoricalFeaturesInfo={},
                                         numTrees=3, featureSubsetStrategy="auto",
                                         impurity='gini', maxDepth=4, maxBins=32)

    # Evaluate model on test instances and compute test error
    predictions = model.predict(testData.map(lambda x: x.features))
    labelsAndPredictions = testData.map(lambda lp: lp.label).zip(predictions)
    testErr = labelsAndPredictions.filter(lambda (v, p): v != p).count() / float(testData.count())
    print('Test Error = ' + str(testErr))
    print('Learned classification forest model:')
    print(model.toDebugString())

    # Save and load model
    model.save(sc, "target/tmp/myRandomForestClassificationModel")
    sameModel = RandomForestModel.load(sc, "target/tmp/myRandomForestClassificationModel")
    # $example off$
