/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package org.apache.datasketches.req;

import static org.apache.datasketches.req.ReqHelper.LS;

import java.util.Arrays;

import org.apache.datasketches.SketchesArgumentException;

/**
 * A special buffer of floats.
 *
 * @author Lee Rhodes
 */
class FloatBuffer {
  private float[] arr_;
  private int count_;
  private int capacity_;
  private int delta_;
  private boolean sorted_;
  private boolean spaceAtBottom_;

  /**
   * Constructs a new FloatBuffer with a default size of 1024 items,delta of 256 items, and
   * space at the top.
   */
  FloatBuffer() {
    this(1024, 256, false);
  }

  /**
   * Constructs an empty FloatBuffer with an initial capacity specified by
   * the <code>capacity</code> argument.
   *
   * @param capacity the initial capacity.
   * @param delta add space in increments of this size
   * @param spaceAtBottom if true, create any extra space at the bottom of the buffer,
   * otherwise, create any extra space at the top of the buffer.
   */
  FloatBuffer(final int capacity, final int delta, final boolean spaceAtBottom) {
    arr_ = new float[capacity];
    count_ = 0;
    capacity_ = capacity;
    delta_ = delta;
    sorted_ = true;
    spaceAtBottom_ = spaceAtBottom;
  }

  /**
   * Copy Constructor
   * @param buf the FloatBuffer to be copied into this one
   */
  FloatBuffer(final FloatBuffer buf) {
    arr_ = buf.arr_.clone();
    count_ = buf.count_;
    capacity_ = buf.capacity_;
    delta_ = buf.delta_;
    sorted_ = buf.sorted_;
    spaceAtBottom_ = buf.spaceAtBottom_;
  }

  /**
   * Private construction from elements
   * @param arr the array to be used directly as the internal array
   * @param count the number of active elements in the given array
   * @param delta add space in increments of this size
   * @param capacity the initial capacity
   * @param sorted true if already sorted
   * @param spaceAtBottom if true, create any extra space at the bottom of the buffer,
   * otherwise, create any extra space at the top of the buffer.
   */
  private FloatBuffer(final float[] arr, final int count, final int delta, final int capacity,
      final boolean sorted, final boolean spaceAtBottom) {
    arr_ = arr;
    count_ = count;
    capacity_ = capacity;
    delta_ = delta;
    sorted_ = sorted;
    spaceAtBottom_ = spaceAtBottom;
  }

  /**
   * Wraps the given array to use as the internal array; thus no copies. For internal use.
   * @param arr the given array
   * @param isSorted set true, if incomming array is already sorted.
   * @param spaceAtBottom if true, create any extra space at the bottom of the buffer,
   * otherwise, create any extra space at the top of the buffer.
   * @return this, which will be sorted
   */
  static FloatBuffer wrap(final float[] arr, final boolean isSorted, final boolean spaceAtBottom) {
    final FloatBuffer buf = new FloatBuffer(arr, arr.length, 0, arr.length, isSorted, spaceAtBottom);
    buf.sort();
    return buf;
  }

  /**
   * Appends the given item to the active array and increments length().
   * This will expand the array if necessary.
   * @param item the given item
   * @return this
   */
  FloatBuffer append(final float item) {
    ensureSpace(1);
    final int index = spaceAtBottom_ ? capacity_ - count_ - 1 : count_;
    arr_[index] = item;
    count_++;
    sorted_ = false;
    return this;
  }

  /**
   * Ensures that the capacity of this FloatBuffer is at least newCapacity.
   * If newCapacity &lt; capacity(), no action is taken.
   * @param newCapacity the new desired capacity
   * at the top.
   * @return this
   */
  FloatBuffer ensureCapacity(final int newCapacity) {
    if (newCapacity > capacity_) {
      final float[] out = new float[newCapacity];
      final int srcPos = spaceAtBottom_ ? capacity_ - count_ : 0;
      final int destPos = spaceAtBottom_ ? newCapacity - count_ : 0;
      System.arraycopy(arr_, srcPos, out, destPos, count_);
      arr_ = out;
      capacity_ = newCapacity;
    }
    return this;
  }

  /**
   * Ensures that the space remaining (capacity() - length()) is at least the given space.
   * @param space the requested space remaining
   * @return this
   */
  private FloatBuffer ensureSpace(final int space) {
    if ((count_ + space) > capacity_) {
      final int newCap = count_ + space + delta_;
      ensureCapacity(newCap);
    }
    return this;
  }

  /**
   * Returns a reference to the internal item array. Be careful and don't modify this array!
   * @return the internal item array.
   */
  float[] getArray() {
    return arr_;
  }

  /**
   * Gets the current capacity of this FloatBuffer. The capacity is the total amount of storage
   * currently available without expanding the array.
   *
   * @return the current capacity
   */
  int getCapacity() {
    return capacity_;
  }

  /**
   * Returns non-normalized rank of the given value.
   * This is the count of items less-than (or equal to) the given value.
   * @param value the given value
   * @param lteq the less-than or less-than or equal to criterion.
   * @return count of items less-than (or equal to) the given value.
   */
  int getCountLtOrEq(final float value, final boolean lteq) {
    if (!sorted_) { sort(); } //we must be sorted!
    if (spaceAtBottom_) {
      final int low = capacity_ - count_;
      final int high = capacity_ - 1;
      final int index = ReqHelper.binarySearch(arr_, low, high, value, lteq);
      return (index == -1) ? 0 : (index + 1) - (capacity_ - count_);
    } else {
      final int index = ReqHelper.binarySearch(arr_, 0, count_ - 1, value, lteq);
      return (index == -1) ? 0 : index + 1;
    }

  }

  /**
   * Returns an array of counts corresponding to each of the values in the given array.
   * The counts will be the number of values that are &lt; or &le; to the given values, depending on
   * the state of <i>lteq</i>.
   * @param values the given values array
   * @param lteq if true, the criterion for the counts.
   * @return an array of counts corresponding to each of the values in the given array
   */
  int[] getCountsLtOrEq(final float[] values, final boolean lteq) {
    final int len = values.length;
    final int[] nnrArr = new int[len];
    for (int i = 0; i < len; i++) {
      nnrArr[i] = getCountLtOrEq(values[i], lteq);
    }
    return nnrArr;
  }

  /**
   * Returns a sorted FloatBuffer of the odd or even offsets from the range startOffset (inclusive)
   * to endOffset (exclusive). The size of the range must be of even size.
   * The offsets are with respect to the start of the active region and independent of the
   * location of the active region within the overall buffer. The requested region will be sorted
   * first.
   * @param startOffset the starting offset within the active region
   * @param endOffset the end offset within the active region, exclusive
   * @param odds if true, return the odds, otherwise return the evens.
   * @return the selected odds from the range
   */
  FloatBuffer getEvensOrOdds(final int startOffset, final int endOffset, final boolean odds) {
    final int start = spaceAtBottom_ ? (capacity_ - count_) + startOffset : startOffset;
    final int end = spaceAtBottom_ ? (capacity_ - count_) + endOffset : endOffset;
    Arrays.sort(arr_, start, end);
    final int range = endOffset - startOffset;
    if ((range & 1) == 1) {
      throw new SketchesArgumentException("Input range size must be even");
    }
    final int odd = odds ? 1 : 0;
    final float[] out = new float[range / 2];
    for (int i = start + odd, j = 0; i < end; i += 2, j++) {
      out[j] = arr_[i];
    }
    return wrap(out, true, spaceAtBottom_);
  }

  /**
   * Gets a value from the backing array given its index.
   * Only used in test or debug.
   * @param index the given index
   * @return a value given its backing array index
   */
  float getIndex(final int index) {
    return arr_[index];
  }

  /**
   * Gets an item given its offset in the active region
   * @param offset the given offset in the active region
   * @return an item given its offset
   */
  float getItem(final int offset) {
    final int index = (spaceAtBottom_) ? (capacity_ - count_) + offset : offset;
    return arr_[index];
  }

  /**
   * Returns the item count.
   *
   * @return the number of active items currently in this array.
   */
  int getItemCount() {
    return count_;
  }

  /**
   * Returns the active length = item count.
   *
   * @return the active length of this buffer.
   */
  int getLength() {
    return count_;
  }

  /**
   * Gets available space, which is getCapacity() - getLength().
   * When spaceAtBottom is true this is the start position for active data, otherwise it is zero.
   * @return available space
   */
  int getSpace() {
    return capacity_ - count_;
  }

  /**
   * Returns true if getLength() == 0.
   * @return true if getLength() == 0.
   */
  boolean isEmpty() {
    return count_ == 0;
  }

  /**
   * Returns true if this FloatBuffer is sorted.
   * @return true if sorted
   */
  boolean isSorted() {
    return sorted_;
  }

  /**
   * Merges the incoming sorted buffer into this sorted buffer.
   * @param bufIn sorted buffer in
   * @return this
   */
  FloatBuffer mergeSortIn(final FloatBuffer bufIn) {
    if (!sorted_ || !bufIn.isSorted()) {
      throw new SketchesArgumentException("Both buffers must be sorted.");
    }
    final float[] arrIn = bufIn.getArray(); //may be larger than its item count.
    final int inLen = bufIn.getLength();
    ensureSpace(inLen);
    final int totLen = count_ + inLen;
    if (spaceAtBottom_) { //scan up, insert at bottom
      final int tgtStart = capacity_ - totLen;
      int i = capacity_ - count_;
      int j = bufIn.capacity_ - bufIn.count_;
      for (int k = tgtStart; k < capacity_; k++) {
        if ((i < capacity_) && (j < bufIn.capacity_)) { //both valid
          arr_[k] = (arr_[i] <= arrIn[j]) ? arr_[i++] : arrIn[j++];
        } else if (i < capacity_) { //i is valid
          arr_[k] = arr_[i++];
        } else if (j <  bufIn.capacity_) { //j is valid
          arr_[k] = arrIn[j++];
        } else {
          break;
        }
      }
    } else { //scan down, insert at top
      int i = count_ - 1;
      int j = inLen - 1;
      for (int k = totLen; k-- > 0; ) {
        if ((i >= 0) && (j >= 0)) { //both valid
          arr_[k] = (arr_[i] >= arrIn[j]) ? arr_[i--] : arrIn[j--];
        } else if (i >= 0) { //i is valid
          arr_[k] = arr_[i--];
        } else if (j >= 0) { //j is valid
          arr_[k] = arrIn[j--];
        } else {
          break;
        }
      }
    }
    count_ += inLen;
    sorted_ = true;
    return this;
  }

  /**
   * Sorts the active region;
   * @return this
   */
  FloatBuffer sort() {
    if (!sorted_) {
      final int start = spaceAtBottom_ ? capacity_ - count_ : 0;
      final int end = spaceAtBottom_ ? capacity_ : count_;
      Arrays.sort(arr_, start, end);
      sorted_ = true;
    }
    return this;
  }

  /**
   * Returns a printable formatted string of the values of this buffer separated by a single space.
   * @param fmt The format for each printed item.
   * @param width the number of items to print per line
   * @param indent the number of spaces at the beginning of a new line
   * @return a printable, formatted string of the values of this buffer.
   */
  String toHorizList(final String fmt, final int width, final int indent) {
    final StringBuilder sb = new StringBuilder();
    final char[] spaces = new char[indent];
    Arrays.fill(spaces, ' ');
    final int start = spaceAtBottom_ ? capacity_ - count_ : 0;
    final int end   = spaceAtBottom_ ? capacity_ : count_;
    int cnt = 0;
    for (int i = start; i < end; i++) {
      final float v = arr_[i];
      final String str = String.format(fmt, v);
      if ((i > start) && ((++cnt % width) == 0)) { sb.append(LS).append(spaces); }
      sb.append(str);
    }
    return sb.toString();
  }

  /**
   * Trims the capacity of this FloatBuffer to length().
   * @return this
   */
  FloatBuffer trimCapacity() {
    if (count_ < capacity_) {
      final float[] out = new float[count_];
      final int start = spaceAtBottom_ ? capacity_ - count_ : 0;
      System.arraycopy(arr_, start, out, 0, count_);
      capacity_ = count_;
    }
    return this;
  }

  /**
   * Trims the length to newLength. If newLength &gt; length() this does nothing and returns.
   * Otherwise, the internal length is reduced to the given length. There is no clearing of
   * the remainder of the capacity. Any values there are considered garbage.
   *
   * @param newLength the new length
   * @return this
   */
  FloatBuffer trimLength(final int newLength) {
    if (newLength < count_) {
      count_ = newLength;
    }
    return this;
  }
}
