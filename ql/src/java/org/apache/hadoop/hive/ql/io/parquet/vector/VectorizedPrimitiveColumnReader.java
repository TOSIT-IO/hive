/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hive.ql.io.parquet.vector;

import org.apache.hadoop.hive.common.type.Timestamp;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DecimalColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.page.PageReader;
import org.apache.parquet.schema.DecimalMetadata;
import org.apache.parquet.schema.Type;

import java.io.IOException;
import java.time.ZoneId;

import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT32;
import static org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName.INT64;

/**
 * It's column level Parquet reader which is used to read a batch of records for a column,
 * part of the code is referred from Apache Spark and Apache Parquet.
 */
public class VectorizedPrimitiveColumnReader extends BaseVectorizedColumnReader {

  public VectorizedPrimitiveColumnReader(
      ColumnDescriptor descriptor,
      PageReader pageReader,
      boolean skipTimestampConversion,
      ZoneId writerTimezone,
      Type type,
      TypeInfo hiveType)
      throws IOException {
    super(descriptor, pageReader, skipTimestampConversion, writerTimezone, type, hiveType);
  }

  @Override
  public void readBatch(
      int total,
      ColumnVector column,
      TypeInfo columnType) throws IOException {
    int rowId = 0;
    while (total > 0) {
      // Compute the number of values we want to read in this page.
      int leftInPage = (int) (endOfPageValueCount - valuesRead);
      if (leftInPage == 0) {
        readPage();
        leftInPage = (int) (endOfPageValueCount - valuesRead);
      }

      int num = Math.min(total, leftInPage);
      if (isCurrentPageDictionaryEncoded) {
        LongColumnVector dictionaryIds = new LongColumnVector();
        // Read and decode dictionary ids.
        readDictionaryIDs(num, dictionaryIds, rowId);
        decodeDictionaryIds(rowId, num, column, columnType, dictionaryIds);
      } else {
        // assign values in vector
        readBatchHelper(num, column, columnType, rowId);
      }
      rowId += num;
      total -= num;
    }
  }

  private void readBatchHelper(
      int num,
      ColumnVector column,
      TypeInfo columnType,
      int rowId) throws IOException {
    PrimitiveTypeInfo primitiveColumnType = (PrimitiveTypeInfo) columnType;

    switch (primitiveColumnType.getPrimitiveCategory()) {
    case INT:
    case BYTE:
    case SHORT:
      readIntegers(num, (LongColumnVector) column, rowId);
      break;
    case DATE:
    case INTERVAL_YEAR_MONTH:
    case LONG:
      readLongs(num, (LongColumnVector) column, rowId);
      break;
    case BOOLEAN:
      readBooleans(num, (LongColumnVector) column, rowId);
      break;
    case DOUBLE:
      readDoubles(num, (DoubleColumnVector) column, rowId);
      break;
    case BINARY:
      readBinaries(num, (BytesColumnVector) column, rowId);
      break;
    case STRING:
      readString(num, (BytesColumnVector) column, rowId);
      break;
    case VARCHAR:
      readVarchar(num, (BytesColumnVector) column, rowId);
      break;
    case CHAR:
      readChar(num, (BytesColumnVector) column, rowId);
      break;
    case FLOAT:
      readFloats(num, (DoubleColumnVector) column, rowId);
      break;
    case DECIMAL:
      readDecimal(num, (DecimalColumnVector) column, rowId);
      break;
    case TIMESTAMP:
      readTimestamp(num, (TimestampColumnVector) column, rowId);
      break;
    case INTERVAL_DAY_TIME:
    default:
      throw new IOException("Unsupported type: " + type);
    }
  }

  private static void setNullValue(ColumnVector c, int rowId) {
    c.isNull[rowId] = true;
    c.isRepeating = false;
    c.noNulls = false;
  }

  private void readDictionaryIDs(
      int total,
      LongColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.vector[rowId] = dataColumn.readValueDictionaryId();
        c.isNull[rowId] = false;
        c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readIntegers(
      int total,
      LongColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.vector[rowId] = dataColumn.readInteger();
        if (dataColumn.isValid(c.vector[rowId])) {
          c.isNull[rowId] = false;
          c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
        } else {
          c.vector[rowId] = 0;
          setNullValue(c, rowId);
        }
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readDoubles(
      int total,
      DoubleColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.vector[rowId] = dataColumn.readDouble();
        if (dataColumn.isValid(c.vector[rowId])) {
          c.isNull[rowId] = false;
          c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
        } else {
          c.vector[rowId] = 0;
          setNullValue(c, rowId);
        }
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readBooleans(
      int total,
      LongColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.vector[rowId] = dataColumn.readBoolean() ? 1 : 0;
        c.isNull[rowId] = false;
        c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readLongs(
      int total,
      LongColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.vector[rowId] = dataColumn.readLong();
        if (dataColumn.isValid(c.vector[rowId])) {
          c.isNull[rowId] = false;
          c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
        } else {
          c.vector[rowId] = 0;
          setNullValue(c, rowId);
        }
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readFloats(
      int total,
      DoubleColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.vector[rowId] = dataColumn.readFloat();
        if (dataColumn.isValid(c.vector[rowId])) {
          c.isNull[rowId] = false;
          c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
        } else {
          c.vector[rowId] = 0;
          setNullValue(c, rowId);
        }
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readDecimal(
      int total,
      DecimalColumnVector c,
      int rowId) throws IOException {

    DecimalMetadata decimalMetadata = type.asPrimitiveType().getDecimalMetadata();
    fillDecimalPrecisionScale(decimalMetadata, c);

    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        if (decimalMetadata != null) {
          c.vector[rowId].set(dataColumn.readDecimal(), c.scale);
          c.isNull[rowId] = false;
          c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
        } else {
          long value = dataColumn.readLong();
          if (dataColumn.isValid(value)) {
            c.vector[rowId].setFromLong(value);
            c.isNull[rowId] = false;
            c.isRepeating = c.isRepeating && (c.vector[0] == c.vector[rowId]);
          } else {
            setNullValue(c, rowId);
          }
        }
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readString(
      int total,
      BytesColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.setVal(rowId, dataColumn.readString());
        c.isNull[rowId] = false;
        // TODO figure out a better way to set repeat for Binary type
        c.isRepeating = false;
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readChar(
      int total,
      BytesColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.setVal(rowId, dataColumn.readChar());
        c.isNull[rowId] = false;
        // TODO figure out a better way to set repeat for Binary type
        c.isRepeating = false;
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readVarchar(
      int total,
      BytesColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.setVal(rowId, dataColumn.readVarchar());
        c.isNull[rowId] = false;
        // TODO figure out a better way to set repeat for Binary type
        c.isRepeating = false;
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readBinaries(
      int total,
      BytesColumnVector c,
      int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        c.setVal(rowId, dataColumn.readBytes());
        c.isNull[rowId] = false;
        // TODO figure out a better way to set repeat for Binary type
        c.isRepeating = false;
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  private void readTimestamp(int total, TimestampColumnVector c, int rowId) throws IOException {
    int left = total;
    while (left > 0) {
      readRepetitionAndDefinitionLevels();
      if (definitionLevel >= maxDefLevel) {
        switch (descriptor.getType()) {
        //INT64 is not yet supported
        case INT96:
          c.set(rowId, dataColumn.readTimestamp().toSqlTimestamp());
          break;
        default:
          throw new IOException(
              "Unsupported parquet logical type: " + type.getOriginalType() + " for timestamp");
        }
        c.isNull[rowId] = false;
        c.isRepeating =
            c.isRepeating && ((c.time[0] == c.time[rowId]) && (c.nanos[0] == c.nanos[rowId]));
      } else {
        setNullValue(c, rowId);
      }
      rowId++;
      left--;
    }
  }

  /**
   * Reads `num` values into column, decoding the values from `dictionaryIds` and `dictionary`.
   */
  private void decodeDictionaryIds(
      int rowId,
      int num,
      ColumnVector column,
      TypeInfo columnType,
      LongColumnVector dictionaryIds) {
    System.arraycopy(dictionaryIds.isNull, rowId, column.isNull, rowId, num);
    if (column.noNulls) {
      column.noNulls = dictionaryIds.noNulls;
    }
    column.isRepeating = column.isRepeating && dictionaryIds.isRepeating;


    PrimitiveTypeInfo primitiveColumnType = (PrimitiveTypeInfo) columnType;

    switch (primitiveColumnType.getPrimitiveCategory()) {
    case INT:
    case BYTE:
    case SHORT:
      for (int i = rowId; i < rowId + num; ++i) {
        ((LongColumnVector) column).vector[i] =
            dictionary.readInteger((int) dictionaryIds.vector[i]);
        if (!(dictionary.isValid(((LongColumnVector) column).vector[i]))) {
          setNullValue(column, i);
          ((LongColumnVector) column).vector[i] = 0;
        }
      } break;
    case DATE:
    case INTERVAL_YEAR_MONTH:
    case LONG:
      for (int i = rowId; i < rowId + num; ++i) {
        ((LongColumnVector) column).vector[i] =
            dictionary.readLong((int) dictionaryIds.vector[i]);
      }
      break;
    case BOOLEAN:
      for (int i = rowId; i < rowId + num; ++i) {
        ((LongColumnVector) column).vector[i] =
            dictionary.readBoolean((int) dictionaryIds.vector[i]) ? 1 : 0;
      }
      break;
    case DOUBLE:
      for (int i = rowId; i < rowId + num; ++i) {
        ((DoubleColumnVector) column).vector[i] =
            dictionary.readDouble((int) dictionaryIds.vector[i]);
      }
      break;
    case BINARY:
      for (int i = rowId; i < rowId + num; ++i) {
        ((BytesColumnVector) column)
            .setVal(i, dictionary.readBytes((int) dictionaryIds.vector[i]));
      }
      break;
    case STRING:
      for (int i = rowId; i < rowId + num; ++i) {
        ((BytesColumnVector) column)
            .setVal(i, dictionary.readString((int) dictionaryIds.vector[i]));
      }
      break;
    case VARCHAR:
      for (int i = rowId; i < rowId + num; ++i) {
        ((BytesColumnVector) column)
            .setVal(i, dictionary.readVarchar((int) dictionaryIds.vector[i]));
      }
      break;
    case CHAR:
      for (int i = rowId; i < rowId + num; ++i) {
        ((BytesColumnVector) column)
            .setVal(i, dictionary.readChar((int) dictionaryIds.vector[i]));
      }
      break;
    case FLOAT:
      for (int i = rowId; i < rowId + num; ++i) {
        ((DoubleColumnVector) column).vector[i] =
            dictionary.readFloat((int) dictionaryIds.vector[i]);
      }
      break;
    case DECIMAL:
      DecimalMetadata decimalMetadata = type.asPrimitiveType().getDecimalMetadata();
      DecimalColumnVector decimalColumnVector = ((DecimalColumnVector) column);

      fillDecimalPrecisionScale(decimalMetadata, decimalColumnVector);

      if (decimalMetadata != null) {
        for (int i = rowId; i < rowId + num; ++i) {
          decimalColumnVector.vector[i].set(dictionary.readDecimal((int) dictionaryIds.vector[i]),
              decimalColumnVector.scale);
        }
      } else {
        for (int i = rowId; i < rowId + num; ++i) {
          long value = dictionary.readLong((int) dictionaryIds.vector[i]);
          if (dictionary.isValid(value)) {
            decimalColumnVector.vector[i]
                .setFromLong(dictionary.readLong((int) dictionaryIds.vector[i]));
          } else {
            setNullValue(column, i);
          }
        }
      }
      break;
    case TIMESTAMP:
      for (int i = rowId; i < rowId + num; ++i) {
        ((TimestampColumnVector) column)
            .set(i, dictionary.readTimestamp((int) dictionaryIds.vector[i]).toSqlTimestamp());
      }
      break;
    case INTERVAL_DAY_TIME:
    default:
      throw new UnsupportedOperationException("Unsupported type: " + type);
    }
  }

  private void fillDecimalPrecisionScale(DecimalMetadata decimalMetadata,
      DecimalColumnVector decimalColumnVector) {
    if (decimalMetadata != null) {
      decimalColumnVector.precision =
          (short) type.asPrimitiveType().getDecimalMetadata().getPrecision();
      decimalColumnVector.scale = (short) type.asPrimitiveType().getDecimalMetadata().getScale();
    } else if (type.asPrimitiveType().getPrimitiveTypeName() == INT32) {
      decimalColumnVector.precision = 10;
      decimalColumnVector.scale = 0;
    } else if (type.asPrimitiveType().getPrimitiveTypeName() == INT64) {
      decimalColumnVector.precision = 19;
      decimalColumnVector.scale = 0;
    } else {
      throw new UnsupportedOperationException(
          "The underlying Parquet type cannot be converted to Hive Decimal type: " + type);
    }
  }
}
