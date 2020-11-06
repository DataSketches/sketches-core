/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.req;

import java.util.List;

import org.apache.datasketches.BinarySearch;
import org.apache.datasketches.Criteria;

/**
 * Supports searches for quantiles
 * @author Lee Rhodes
 */
class ReqAuxiliary {
  private static final String LS = System.getProperty("line.separator");
  private float[] items;
  private byte[] lgWeights;
  private double[] normRanks;
  private final boolean hra;
  private final Criteria criterion;

  ReqAuxiliary(final ReqSketch sk) {
    hra = sk.getHighRankAccuracy();
    criterion = sk.getCriterion();
    buildAuxTable(sk);
  }

  //For testing only
  ReqAuxiliary(final int arrLen, final boolean hra, final Criteria criterion) {
    this.hra = hra;
    this.criterion = criterion;
    items = new float[arrLen];
    lgWeights = new byte[arrLen];
    normRanks = new double[arrLen];
  }

  private void buildAuxTable(final ReqSketch sk) {
    final List<ReqCompactor> compactors = sk.getCompactors();
    final int numComp = compactors.size();
    final int totalItems = sk.getRetainedItems();
    final long N = sk.getN();
    items = new float[totalItems];
    lgWeights = new byte[totalItems];
    normRanks = new double[totalItems];
    int auxCount = 0;
    for (int i = 0; i < numComp; i++) {
      final ReqCompactor c = compactors.get(i);
      final FloatBuffer bufIn = c.getBuffer();
      final byte lgWeight = c.getLgWeight();
      final int bufInLen = bufIn.getLength();
      mergeSortIn(bufIn, lgWeight, auxCount);
      auxCount += bufInLen;
    }
    double sum = 0;
    for (int i = 0; i < totalItems; i++) {
      sum += 1 << lgWeights[i];
      normRanks[i] = sum / N;
    }
  }

  //Specially modified version of FloatBuffer.mergeSortIn(). Here spaceAtBottom is always false and
  // the ultimate array size has already been set.  However, this must simultaneously deal with
  // sorting the weights as well.
  void mergeSortIn(final FloatBuffer bufIn, final byte lgWeight, final int auxCount) {
    if (!bufIn.isSorted()) { bufIn.sort(); }
    final float[] arrIn = bufIn.getArray(); //may be larger than its item count.
    final int bufInLen = bufIn.getLength();
    final int totLen = auxCount + bufInLen;
    int i = auxCount - 1;
    int j = bufInLen - 1;
    int h = hra ? bufIn.getCapacity() - 1 : bufInLen - 1;
    for (int k = totLen; k-- > 0; ) {
      if (i >= 0 && j >= 0) { //both valid
        if (items[i] >= arrIn[h]) {
          items[k] = items[i];
          lgWeights[k] = lgWeights[i--];
        } else {
          items[k] = arrIn[h--]; j--;
          lgWeights[k] = lgWeight;
        }
      } else if (i >= 0) { //i is valid
        items[k] = items[i];
        lgWeights[k] = lgWeights[i--];
      } else if (j >= 0) { //j is valid
        items[k] = arrIn[h--]; j--;
        lgWeights[k] = lgWeight;
      } else {
        break;
      }
    }
  }

  /**
   * Gets the quantile of the largest normalized rank that is less than the given normalized rank,
   * which must be in the range [0.0, 1.0], inclusive, inclusive
   * @param normRank the given normalized rank
   * @return the largest quantile less than the given normalized rank.
   */
  float getQuantile(final double normRank) {
    final int len = normRanks.length;
    final int index = BinarySearch.find(normRanks, 0, len - 1, normRank, criterion);
    if (index == -1) { return Float.NaN; }
    return items[index];
  }

  //used for testing

  Row getRow(final int index) {
    return new Row(items[index], lgWeights[index], normRanks[index]);
  }

  class Row {
    float item;
    byte lgWeight;
    double normRank;

    Row(final float item, final byte lgWeight, final double normRank) {
      this.item = item;
      this.lgWeight = lgWeight;
      this.normRank = normRank;
    }
  }

  String toString(final int precision, final int fieldSize) {
    final StringBuilder sb = new StringBuilder();
    final int p = precision;
    final int z = fieldSize;
    final String ff = "%" + z + "." + p + "f";
    final String sf = "%" + z + "s";
    final String df = "%"  + z + "d";
    final String dfmt = ff + df + ff + LS;
    final String sfmt = sf + sf + sf + LS;
    sb.append("Aux Detail").append(LS);
    sb.append(String.format(sfmt, "Item", "Weight", "NormRank"));
    final int totalCount = items.length;
    for (int i = 0; i < totalCount; i++) {
      final Row row = getRow(i);
      sb.append(String.format(dfmt, row.item, 1 << row.lgWeight, row.normRank));
    }
    return sb.toString();
  }

}

