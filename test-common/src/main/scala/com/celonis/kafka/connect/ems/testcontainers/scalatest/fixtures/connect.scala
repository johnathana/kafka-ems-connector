/*
 * Copyright 2023 Celonis SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.celonis.kafka.connect.ems.testcontainers.scalatest.fixtures

import com.celonis.kafka.connect.ems.testcontainers.connect.EmsConnectorConfiguration
import com.celonis.kafka.connect.ems.testcontainers.connect.KafkaConnectClient
import eu.rekawek.toxiproxy.Proxy
import eu.rekawek.toxiproxy.model.ToxicDirection

import scala.concurrent.duration.FiniteDuration

object connect {

  def withParquetUploadLatency(
    latency:  FiniteDuration,
  )(testCode: => Unit,
  )(
    implicit
    proxy: Proxy) = {
    proxy.toxics().latency("LATENCY_UPSTREAM", ToxicDirection.UPSTREAM, latency.toMillis)
    try {
      testCode
    } finally {
      proxy.toxics().get("LATENCY_UPSTREAM").remove()
    }
  }

  def withConnectionCut(
    testCode: => Any,
  )(
    implicit
    proxy: Proxy): Unit = {
    proxy.toxics.bandwidth("CUT_CONNECTION_DOWNSTREAM", ToxicDirection.DOWNSTREAM, 0)
    proxy.toxics.bandwidth("CUT_CONNECTION_UPSTREAM", ToxicDirection.UPSTREAM, 0)
    try {
      val _ = testCode
    } finally {
      proxy.toxics.get("CUT_CONNECTION_DOWNSTREAM").remove()
      proxy.toxics.get("CUT_CONNECTION_UPSTREAM").remove()
    }
  }

  def withConnector(
    connectorConfig: EmsConnectorConfiguration,
  )(testCode:        => Any,
  )(
    implicit
    kafkaConnectClient: KafkaConnectClient): Unit = {
    kafkaConnectClient.registerConnector(connectorConfig)
    try {
      kafkaConnectClient.waitConnectorInRunningState(connectorConfig.name)
      val _ = testCode
    } finally {
      kafkaConnectClient.deleteConnector(connectorConfig.name)
    }
  }
}
