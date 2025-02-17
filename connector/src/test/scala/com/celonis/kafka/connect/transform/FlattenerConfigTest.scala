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

package com.celonis.kafka.connect.transform

import cats.syntax.either._
import cats.syntax.option._
import org.scalatest.Inside

class FlattenerConfigTest extends org.scalatest.funsuite.AnyFunSuite with Inside {
  test("returns None unless explicitly enabled") {
    Seq(Some(1000), None).foreach { fallbackVarcharLength =>
      assertResult(Option.empty[FlattenerConfig].asRight)(FlattenerConfig.extract(
        Map(
          "connect.ems.flattener.enable" -> false,
        ),
        fallbackVarcharLength,
      ))
    }
  }
  test("returns an error message when a flattener setting is supplied without the flattener being enabled") {
    Seq(
      "connect.ems.flattener.collections.discard" -> "true",
      "connect.ems.flattener.jsonblob.chunks"     -> 5,
    ).foreach {
      case (key, value) =>
        val result = FlattenerConfig.extract(Map(key -> value), None)
        inside(result) {
          case Left(errorMsg) =>
            assert(errorMsg.contains(key))
            assert(errorMsg.contains("connect.ems.flattener.enable"))
        }

    }
  }
  test("returns an error message when jsonblob.chunks is set without a value fallback varchar length") {
    inside(FlattenerConfig.extract(Map(
                                     "connect.ems.flattener.enable"          -> "true",
                                     "connect.ems.flattener.jsonblob.chunks" -> 5,
                                   ),
                                   None,
    )) {
      case Left(errorMsg) =>
        assert(errorMsg.contains("connect.ems.data.fallback.varchar.length"))
    }
  }

  test("returns config value when all the depending configs are supplied") {
    val fallbackVarcharLength = 65000

    assertResult(FlattenerConfig(discardCollections = true,
                                 Some(FlattenerConfig.JsonBlobChunks(5, fallbackVarcharLength)),
    ).some.asRight)(
      FlattenerConfig.extract(
        Map(
          "connect.ems.flattener.enable"              -> "true",
          "connect.ems.flattener.jsonblob.chunks"     -> 5,
          "connect.ems.flattener.collections.discard" -> true,
        ),
        Some(fallbackVarcharLength),
      ),
    )
    assertResult(FlattenerConfig(discardCollections = false, None).some.asRight)(
      FlattenerConfig.extract(
        Map(
          "connect.ems.flattener.enable" -> "true",
        ),
        Some(fallbackVarcharLength),
      ),
    )
  }

}
