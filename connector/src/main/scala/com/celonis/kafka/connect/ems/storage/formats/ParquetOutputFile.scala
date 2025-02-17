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

package com.celonis.kafka.connect.ems.storage.formats

import com.celonis.kafka.connect.ems.storage.FileAndStream
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.PositionOutputStream

class ParquetOutputFile(fileAndStream: FileAndStream) extends OutputFile {
  override def create(blockSizeHint: Long): PositionOutputStream =
    new PositionOutputStreamWrapper(fileAndStream)

  override def createOrOverwrite(blockSizeHint: Long): PositionOutputStream =
    new PositionOutputStreamWrapper(fileAndStream)

  override def supportsBlockSize(): Boolean = false

  override def defaultBlockSize(): Long = 0

  class PositionOutputStreamWrapper(fs: FileAndStream) extends PositionOutputStream {
    override def getPos: Long = fs.size
    override def write(b: Int): Unit = fs.write(b)
    override def write(b: Array[Byte]): Unit = fs.write(b)
    override def write(b: Array[Byte], off: Int, len: Int): Unit = fs.write(b, off, len)
    override def flush(): Unit = fs.flush()
  }
}
