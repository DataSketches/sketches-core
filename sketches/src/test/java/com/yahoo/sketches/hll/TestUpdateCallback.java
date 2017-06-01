/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.hll;

import org.testng.Assert;

/**
 */
public class TestUpdateCallback implements Fields.UpdateCallback {
  public static void assertVals(TestUpdateCallback cb, int count, int oldVal, int newVal) {
    Assert.assertEquals(cb.getCount(), count, "count is off");
    Assert.assertEquals(cb.getOldVal(), (byte) oldVal, "oldVal_ is off");
    Assert.assertEquals(cb.getNewVal(), (byte) newVal, "newVal_ is off");
  }


  int count = 0;

  int expectedBucket;
  byte oldVal_;
  byte newVal_;

  @Override
  public void bucketUpdated(int bucket, byte oldVal, byte newVal)
  {
    Assert.assertEquals(bucket, expectedBucket);
    oldVal_ = oldVal;
    newVal_ = newVal;
    ++count;
  }

  public byte getOldVal()
  {
    return oldVal_;
  }

  public byte getNewVal()
  {
    return newVal_;
  }

  public int getCount()
  {
    return count;
  }

  public int getExpectedBucket()
  {
    return expectedBucket;
  }

  public void setExpectedBucket(int expectedBucket)
  {
    this.expectedBucket = expectedBucket;
  }
}
