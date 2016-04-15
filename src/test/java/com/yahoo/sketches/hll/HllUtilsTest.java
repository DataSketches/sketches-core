package com.yahoo.sketches.hll;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 */
public class HllUtilsTest
{
  @Test
  public void testInvPow2ComputesEmptyBuckets() throws Exception
  {
    Assert.assertEquals(
        20.0,
        HllUtils.computeInvPow2Sum(20, new ArrayBucketIterator(new int[]{}, new byte[]{}))
    );
  }

  @Test
  public void testInvPow2AggregatesBuckets() throws Exception
  {
    Assert.assertEquals(
        19.0 + Math.pow(2.0, -1.0 * 3),
        HllUtils.computeInvPow2Sum(20, new ArrayBucketIterator(new int[]{49}, new byte[]{3}))
    );
  }

  @Test(expectedExceptions = AssertionError.class,
          expectedExceptionsMessageRegExp = "e cannot be negative or greater than 1023: " + -1)
  public void testInvPow2InputShouldBeGreaterThan0() {
    HllUtils.invPow2(-1);
  }

  @Test(expectedExceptions = AssertionError.class,
          expectedExceptionsMessageRegExp = "e cannot be negative or greater than 1023: " + 1024)
  public void testInvPow2InputShouldBeLessThan1023() {
    HllUtils.invPow2(1024);
  }

  private static class ArrayBucketIterator implements BucketIterator
  {
    private final int[] keys;
    private final byte[] vals;

    private int i = -1;

    public ArrayBucketIterator(int[] keys, byte[] vals)
    {
      this.keys = keys;
      this.vals = vals;
    }

    @Override
    public boolean next()
    {
      return ++i < keys.length;
    }

    @Override
    public int getKey()
    {
      return keys[i];
    }

    @Override
    public byte getValue()
    {
      return vals[i];
    }
  }
}
