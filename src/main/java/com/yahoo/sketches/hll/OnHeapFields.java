package com.yahoo.sketches.hll;

public class OnHeapFields implements Fields
{
  private final Preamble preamble;
  private final byte[] buckets;

  public OnHeapFields(Preamble preamble) {
    this.preamble = preamble;
    buckets = new byte[preamble.getConfigK()];
  }

  @Override
  public Preamble getPreamble() {
    return preamble;
  }

  @Override
  public Fields updateBucket(int index, byte val, UpdateCallback callback)
  {
    if (val > buckets[index]) {
      callback.bucketUpdated(index, buckets[index], val);
      buckets[index] = val;
    }
    return this;
  }

  @Override
  public int intoByteArray(byte[] array, int offset)
  {
    int numBytesNeeded = numBytesToSerialize();
    if (array.length - offset < numBytesNeeded) {
      throw new IllegalArgumentException(
          String.format("array too small[%,d] < [%,d]", array.length - offset, numBytesNeeded)
      );
    }

    array[offset++] = Fields.NAIVE_DENSE_VERSION;
    for (byte bucket : buckets) {
      array[offset++] = bucket;
    }
    return offset;
  }

  @Override
  public int numBytesToSerialize()
  {
    return 1 + buckets.length;
  }

  @Override
  public Fields toCompact()
  {
    return this;
  }

  @Override
  public BucketIterator getBucketIterator()
  {
    return new BucketIterator()
    {
      private int i = -1;

      @Override
      public boolean next()
      {
        ++i;
        while (i < buckets.length && buckets[i] == 0) {
          ++i;
        }
        return i < buckets.length;
      }

      @Override
      public int getKey()
      {
        return i;
      }

      @Override
      public byte getValue()
      {
        return buckets[i];
      }
    };
  }
}
