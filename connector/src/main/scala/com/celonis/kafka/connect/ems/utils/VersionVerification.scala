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

package com.celonis.kafka.connect.ems.utils

import scala.util.Try

object VersionVerification extends App {

  Try(args(0)).toOption match {
    case Some(expectedVersion) if expectedVersion.trim != Version.implementationVersion =>
      System.err.println(
        s"Version check failed.  Expected : $expectedVersion, but was ${Version.implementationVersion} ",
      )
      System.exit(1)
    case _ =>
      System.out.println(
        s"Celonis EMS Connector ${Version.implementationVersion}\n\n" +
          s"Please see docs at https://github.com/celonis/kafka-ems-connector/wiki to get started.",
      )
  }

}
