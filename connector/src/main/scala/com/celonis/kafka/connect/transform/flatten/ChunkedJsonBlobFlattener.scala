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

package com.celonis.kafka.connect.transform.flatten

import com.celonis.kafka.connect.ems.config.EmsSinkConfigConstants.FLATTENER_JSONBLOB_CHUNKS_KEY
import com.celonis.kafka.connect.transform.FlattenerConfig
import com.celonis.kafka.connect.transform.FlattenerConfig.JsonBlobChunks
import com.celonis.kafka.connect.transform.flatten.ChunkedJsonBlobFlattener.MisconfiguredJsonBlobMaxChunks
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct

import java.nio.charset.StandardCharsets

private final class ChunkedJsonBlobFlattener(config: JsonBlobChunks) extends Flattener {
  def flatten(value: Any, originalSchema: Option[Schema]): Struct = {
    val FlattenerConfig.JsonBlobChunks(maxChunks, emsVarcharLength) = config

    val jsonBlobBytes = value match {
      case struct: Struct => ConnectJsonConverter.converter.fromConnectData("some-topic", struct.schema(), struct)
      case str:    String => str.getBytes(StandardCharsets.UTF_8)
      case _ => jacksonMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)
    }
    val requiredChunks = jsonBlobBytes.length.toFloat / emsVarcharLength
    if (requiredChunks > maxChunks)
      throw MisconfiguredJsonBlobMaxChunks(maxChunks, jsonBlobBytes.length, emsVarcharLength)
    else
      jsonBlobBytes.grouped(emsVarcharLength).zipWithIndex.foldLeft(new Struct(schema)) {
        case (newStruct, (jsonBlobChunk, idx)) =>
          newStruct.put(s"payload_chunk${idx + 1}", new String(jsonBlobChunk, StandardCharsets.UTF_8))
      }
  }

  private val schema: Schema =
    (1 to config.chunks).foldLeft(SchemaBuilder.struct()) { (builder, idx) =>
      builder.field(s"payload_chunk$idx", Schema.OPTIONAL_STRING_SCHEMA)
    }.schema()

  private val jacksonMapper = new ObjectMapper()
}

private object ChunkedJsonBlobFlattener {
  case class MisconfiguredJsonBlobMaxChunks(configuredChunksSize: Int, blobByteSize: Int, emsVarcharLength: Int)
      extends Throwable {
    override def getMessage: String =
      s"Configured value ${configuredChunksSize} for ${FLATTENER_JSONBLOB_CHUNKS_KEY} is insufficient! Current JSON blob length: $blobByteSize, Ems VARCHAR Length: ${emsVarcharLength}."
  }
}
