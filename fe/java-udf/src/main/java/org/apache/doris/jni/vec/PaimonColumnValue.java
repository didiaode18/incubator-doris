// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.jni.vec;

import org.apache.paimon.data.columnar.ColumnarRow;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class PaimonColumnValue implements ColumnValue {
    private int idx;
    private ColumnarRow record;

    public PaimonColumnValue() {
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public void setOffsetRow(ColumnarRow record) {
        this.record = record;
    }

    @Override
    public boolean getBoolean() {
        return record.getBoolean(idx);
    }

    @Override
    public byte getByte() {
        return record.getByte(idx);
    }

    @Override
    public short getShort() {
        return record.getShort(idx);
    }

    @Override
    public int getInt() {
        return record.getInt(idx);
    }

    @Override
    public float getFloat() {
        return record.getFloat(idx);
    }

    @Override
    public long getLong() {
        return record.getLong(idx);
    }

    @Override
    public double getDouble() {
        return record.getDouble(idx);
    }

    @Override
    public BigInteger getBigInteger() {
        return BigInteger.valueOf(record.getInt(idx));
    }

    @Override
    public BigDecimal getDecimal() {
        return BigDecimal.valueOf(getDouble());
    }

    @Override
    public String getString() {
        return record.getString(idx).toString();
    }

    @Override
    public LocalDate getDate() {
        return Instant.ofEpochMilli(record.getTimestamp(idx, 3)
                .getMillisecond()).atZone(ZoneOffset.ofHours(8)).toLocalDate();
    }

    @Override
    public LocalDateTime getDateTime() {
        return Instant.ofEpochMilli(record.getTimestamp(idx, 3)
            .getMillisecond()).atZone(ZoneOffset.ofHours(8)).toLocalDateTime();
    }

    @Override
    public boolean isNull() {
        return record.isNullAt(idx);
    }

    @Override
    public byte[] getBytes() {
        return record.getBinary(idx);
    }

    @Override
    public void unpackArray(List<ColumnValue> values) {

    }

    @Override
    public void unpackMap(List<ColumnValue> keys, List<ColumnValue> values) {

    }

    @Override
    public void unpackStruct(List<Integer> structFieldIndex, List<ColumnValue> values) {

    }
}
