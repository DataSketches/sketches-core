package com.yahoo.sketches.theta;

import com.yahoo.memory.WritableMemory;
import com.yahoo.sketches.Family;
import com.yahoo.sketches.ResizeFactor;
import com.yahoo.sketches.theta.UpdateReturnState;
import com.yahoo.sketches.theta.UpdateSketch;

/**
 * @author eshcar
 */
public class UpdateSketchComposition extends UpdateSketch {
  protected UpdateSketch delegatee_;

  protected UpdateSketchComposition(final UpdateSketch delegattee) {
    super();
    delegatee_ = delegattee;
  }

  @Override public int getCurrentBytes(boolean compact) {
    return delegatee_.getCurrentBytes(compact);
  }

  @Override public Family getFamily() {
    return delegatee_.getFamily();
  }

  @Override public int getRetainedEntries(boolean valid) {
    return delegatee_.getRetainedEntries(valid);
  }

  @Override public boolean isDirect() {
    return delegatee_.isDirect();
  }

  @Override public boolean isEmpty() {
    return delegatee_.isEmpty();
  }

  @Override public byte[] toByteArray() {
    return delegatee_.toByteArray();
  }

  @Override long[] getCache() {
    return delegatee_.getCache();
  }

  @Override int getCurrentPreambleLongs(boolean compact) {
    return delegatee_.getCurrentPreambleLongs(compact);
  }

  @Override short getSeedHash() {
    return delegatee_.getSeedHash();
  }

  @Override long getThetaLong() {
    return delegatee_.getThetaLong();
  }

  @Override public void reset() {
    delegatee_.reset();
  }

  @Override public UpdateSketch rebuild() {
    return delegatee_.rebuild();
  }

  @Override public ResizeFactor getResizeFactor() {
    return delegatee_.getResizeFactor();
  }

  @Override UpdateReturnState hashUpdate(long hash) {
    return delegatee_.hashUpdate(hash);
  }

  @Override int getLgArrLongs() {
    return delegatee_.getLgArrLongs();
  }

  @Override public int getLgNomLongs() {
    return delegatee_.getLgNomLongs();
  }

  @Override float getP() {
    return delegatee_.getP();
  }

  @Override long getSeed() {
    return delegatee_.getSeed();
  }

  @Override boolean isDirty() {
    return delegatee_.isDirty();
  }

  @Override WritableMemory getMemory() {
    return delegatee_.getMemory();
  }

  @Override void setThetaLong(long theta) {
    delegatee_.setThetaLong(theta);
  }

  @Override boolean isOutOfSpace(int numEntries) {
    return delegatee_.isOutOfSpace(numEntries);
  }
}
