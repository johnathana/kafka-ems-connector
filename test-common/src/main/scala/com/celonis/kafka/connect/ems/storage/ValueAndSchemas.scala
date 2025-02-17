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

package com.celonis.kafka.connect.ems.storage

import org.apache.avro.Schema
import org.apache.kafka.connect.data.{ Schema => ConnectSchema }
import org.apache.kafka.connect.data.{ SchemaBuilder => ConnectSchemaBuilder }

final case class ValueAndSchemas(
  name:                 String,
  avroValue:            Any,
  connectValue:         Any,
  parquetValue:         Any,
  avroSchema:           Schema,
  connectSchemaBuilder: ConnectSchemaBuilder,
  parquetSchema:        String,
) {
  lazy val connectSchema:         ConnectSchema = connectSchemaBuilder.schema()
  lazy val optionalConnectSchema: ConnectSchema = connectSchemaBuilder.optional().schema()
}
