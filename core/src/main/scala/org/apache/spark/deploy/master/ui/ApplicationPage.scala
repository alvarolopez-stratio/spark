/*
 * © 2017 Stratio Big Data Inc., Sucursal en España. All rights reserved
 *
 * This software is a modification of the original software Apache Spark licensed under the Apache 2.0
 * license, a copy of which is below. This software contains proprietary information of
 * Stratio Big Data Inc., Sucursal en España and may not be revealed, sold, transferred, modified, distributed or
 * otherwise made available, licensed or sublicensed to third parties; nor reverse engineered, disassembled or decompiled,
 * without express written authorization from Stratio Big Data Inc., Sucursal en España.
 */
package org.apache.spark.deploy.master.ui

import javax.servlet.http.HttpServletRequest

import scala.xml.Node

import org.apache.spark.deploy.DeployMessages.{MasterStateResponse, RequestMasterState}
import org.apache.spark.deploy.ExecutorState
import org.apache.spark.deploy.master.ExecutorDesc
import org.apache.spark.ui.{ToolTips, UIUtils, WebUIPage}
import org.apache.spark.util.Utils

private[ui] class ApplicationPage(parent: MasterWebUI) extends WebUIPage("app") {

  private val master = parent.masterEndpointRef

  /** Executor details for a particular application */
  def render(request: HttpServletRequest): Seq[Node] = {
    val appId = request.getParameter("appId")
    val state = master.askWithRetry[MasterStateResponse](RequestMasterState)
    val app = state.activeApps.find(_.id == appId)
      .getOrElse(state.completedApps.find(_.id == appId).orNull)
    if (app == null) {
      val msg = <div class="row-fluid">No running application with ID {appId}</div>
      return UIUtils.basicSparkPage(msg, "Not Found")
    }

    val executorHeaders = Seq("ExecutorID", "Worker", "Cores", "Memory", "State", "Logs")
    val allExecutors = (app.executors.values ++ app.removedExecutors).toSet.toSeq
    // This includes executors that are either still running or have exited cleanly
    val executors = allExecutors.filter { exec =>
      !ExecutorState.isFinished(exec.state) || exec.state == ExecutorState.EXITED
    }
    val removedExecutors = allExecutors.diff(executors)
    val executorsTable = UIUtils.listingTable(executorHeaders, executorRow, executors)
    val removedExecutorsTable = UIUtils.listingTable(executorHeaders, executorRow, removedExecutors)

    val content =
      <div class="row-fluid">
        <div class="span12">
          <ul class="unstyled">
            <li><strong>ID:</strong> {app.id}</li>
            <li><strong>Name:</strong> {app.desc.name}</li>
            <li><strong>User:</strong> {app.desc.user}</li>
            <li><strong>Cores:</strong>
            {
              if (app.desc.maxCores.isEmpty) {
                "Unlimited (%s granted)".format(app.coresGranted)
              } else {
                "%s (%s granted, %s left)".format(
                  app.desc.maxCores.get, app.coresGranted, app.coresLeft)
              }
            }
            </li>
            <li>
              <span data-toggle="tooltip" title={ToolTips.APPLICATION_EXECUTOR_LIMIT}
                    data-placement="right">
                <strong>Executor Limit: </strong>
                {
                  if (app.executorLimit == Int.MaxValue) "Unlimited" else app.executorLimit
                }
                ({app.executors.size} granted)
              </span>
            </li>
            <li>
              <strong>Executor Memory:</strong>
              {Utils.megabytesToString(app.desc.memoryPerExecutorMB)}
            </li>
            <li><strong>Submit Date:</strong> {app.submitDate}</li>
            <li><strong>State:</strong> {app.state}</li>
            {
              if (!app.isFinished) {
                <li><strong>
                    <a href={UIUtils.makeHref(parent.master.reverseProxy,
                      app.id, app.desc.appUiUrl)}>Application Detail UI</a>
                </strong></li>
              }
            }
          </ul>
        </div>
      </div>

      <div class="row-fluid"> <!-- Executors -->
        <div class="span12">
          <h4> Executor Summary </h4>
          {executorsTable}
          {
            if (removedExecutors.nonEmpty) {
              <h4> Removed Executors </h4> ++
              removedExecutorsTable
            }
          }
        </div>
      </div>;
    UIUtils.basicSparkPage(content, "Application: " + app.desc.name)
  }

  private def executorRow(executor: ExecutorDesc): Seq[Node] = {
    val workerUrlRef = UIUtils.makeHref(parent.master.reverseProxy,
      executor.worker.id, executor.worker.webUiAddress)
    <tr>
      <td>{executor.id}</td>
      <td>
        <a href={workerUrlRef}>{executor.worker.id}</a>
      </td>
      <td>{executor.cores}</td>
      <td>{executor.memory}</td>
      <td>{executor.state}</td>
      <td>
        <a href={"%s/logPage?appId=%s&executorId=%s&logType=stdout"
          .format(workerUrlRef, executor.application.id, executor.id)}>stdout</a>
        <a href={"%s/logPage?appId=%s&executorId=%s&logType=stderr"
          .format(workerUrlRef, executor.application.id, executor.id)}>stderr</a>
      </td>
    </tr>
  }
}
