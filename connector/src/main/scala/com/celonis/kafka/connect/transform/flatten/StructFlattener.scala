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

import com.celonis.kafka.connect.transform.flatten.SchemaFlattener.FlatSchema
import com.celonis.kafka.connect.transform.flatten.SchemaFlattener.Field
import com.celonis.kafka.connect.transform.flatten.SchemaFlattener.Path
import org.apache.kafka.connect.data.Schema
import org.apache.kafka.connect.data.SchemaBuilder
import org.apache.kafka.connect.data.Struct

import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

private final class StructFlattener(schemaFlattener: SchemaFlattener) extends Flattener {
  override def flatten(value: Any, schema: Option[Schema]): Any =
    StructFlattener.flatten(value, schemaFlattener.flatten(schema.getOrElse(Schema.BYTES_SCHEMA)))
}

private object StructFlattener {
  def flatten(value: Any, flatSchema: FlatSchema): Any = {
    val schema = flatSchema.connectSchema

    schema.`type` match {
      case Schema.Type.STRUCT =>
        flatSchema.fields.foldLeft(new Struct(schema))(processField(value))
      case _ => value
    }
  }

  private def processField(value: Any)(struct: Struct, field: Field): Struct = {
    val extractedValue = extractValue(value, field.path)
    val fieldValue = extractedValue match {
      case _: java.util.Map[_, _] | _: java.util.List[_] =>
        jsonEncodeCollection(extractedValue)
      case _ => extractedValue
    }
    struct.put(field.path.name, fieldValue)
    struct
  }

  private def jsonEncodeCollection(value: Any): String = {
    val convertSchema = schemaOfCollection(value).orNull
    new String(
      ConnectJsonConverter.converter.fromConnectData(converterTopicName, convertSchema, value),
      StandardCharsets.UTF_8,
    )
  }

  /** Weird hack due to the fact that the connect json converter does not handle `Map`s as structs. This should go once
    * we are converting maps into structs upstream
    */
  private def schemaOfCollection(value: Any): Option[Schema] = value match {
    case value: java.util.Map[_, _] =>
      value.asScala.collectFirst {
        case (_, struct: Struct) =>
          SchemaBuilder.map(Schema.STRING_SCHEMA, struct.schema()).build()
      }

    case value: java.util.Collection[_] =>
      value.asScala.collectFirst {
        case struct: Struct =>
          SchemaBuilder.array(struct.schema()).build()
      }

    case _ => None
  }

  @tailrec
  private def extractValue(value: Any, at: Path): Any =
    at.segments match {
      case head +: tail => value match {
          case value: Struct              => extractValue(value.get(head), Path(tail))
          case value: java.util.Map[_, _] => extractValue(value.get(head), Path(tail))
          case value if value == null => value
          case _                      => throw new RuntimeException(s"Field values can only be extracted from Structs and Maps")
        }
      case _ => value
    }

  private val converterTopicName = "irrelevant-topic-name"
}
