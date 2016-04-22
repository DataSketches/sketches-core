/*
 * Copyright 2016, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE file
 * at the project root for terms.
 */

package com.yahoo.sketches.frequencies;

import static com.yahoo.sketches.Util.LS;
import static com.yahoo.sketches.Util.zeroPad;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.memory.Memory;

// @formatter:off
/**
 * This class defines the preamble data structure and provides basic utilities for some of the key
 * fields.
 * <p>
 * The intent of the design of this class was to isolate the detailed knowledge of the bit and byte
 * layout of the serialized form of the sketches derived from the Sketch class into one place. This
 * allows the possibility of the introduction of different serialization schemes with minimal impact
 * on the rest of the library.
 * </p>
 * 
 * <p>
 * MAP: Low significance bytes of this <i>long</i> data structure are on the right. However, the
 * multi-byte integers (<i>int</i> and <i>long</i>) are stored in native byte order. The <i>byte</i>
 * values are treated as unsigned.
 * </p>
 * 
 * <p>
 * An empty FrequentItems only requires 8 bytes. All others require 40 bytes of preamble.
 * </p>
 * 
 * <pre>
 *  * Long || Start Byte Adr:
 * Adr: 
 *      ||    7     |    6   |    5   |    4   |    3   |    2   |    1   |     0          |
 *  0   ||----------|--Type--|-Flags--|-LgCur--| LgMax  | FamID  | SerVer | PreambleLongs  |
 *      ||    15    |   14   |   13   |   12   |   11   |   10   |    9   |     8          |
 *  1   ||------------(unused)-----------------|--------ActiveItems------------------------|
 *      ||    23    |   22   |   21   |   20   |   19   |   18   |   17   |    16          |
 *  2   ||-----------------------------------streamLength----------------------------------|
 *      ||    31    |   30   |   29   |   28   |   27   |   26   |   25   |    24          |
 *  3   ||---------------------------------offset------------------------------------------|
 *      ||    39    |   38   |   37   |   36   |   35   |   34   |   33   |    32          |
 *  4   ||---------------------------------mergeError--------------------------------------|
 *      ||    47    |   46   |   45   |   44   |   43   |   42   |   41   |    40          |
 *  5   ||----------start of values buffer, followed by keys buffer------------------------|
 * </pre>
 * 
 * @author Justin Thaler
 */
final class PreambleUtil {

  private PreambleUtil() {}

  // ###### DO NOT MESS WITH THIS FROM HERE ...
  // Preamble byte Addresses
  static final int PREAMBLE_LONGS_BYTE       = 0; // either 1 or 6
  static final int SER_VER_BYTE              = 1;
  static final int FAMILY_BYTE               = 2;
  static final int LG_MAX_MAP_SIZE_BYTE      = 3;
  static final int LG_CUR_MAP_SIZE_BYTE      = 4;
  static final int FLAGS_BYTE                = 5;
  static final int FREQ_SKETCH_TYPE_BYTE     = 6;
  static final int ACTIVE_ITEMS_INT          = 8;  // to 11 : 0 to 4 in pre1
  static final int STREAMLENGTH_LONG         = 16; // to 23 : pre2
  static final int OFFSET_LONG               = 24; // to 31 : pre3
  static final int MERGE_ERROR_LONG          = 32; // to 39 : pre4
  
  
  // flag bit masks
  static final int EMPTY_FLAG_MASK      = 4;
  
  
  // Specific values for this implementation
  static final int SER_VER = 1;
  static final int FREQ_SKETCH_TYPE = 1;

  
  /**
   * Returns a human readable string summary of the preamble state of the given Memory. 
   * Note: other than making sure that the given Memory size is large
   * enough for just the preamble, this does not do much value checking of the contents of the 
   * preamble as this is primarily a tool for debugging the preamble visually.
   * 
   * @param srcMem the given Memory.
   * @return the summary preamble string.
   */
  public static String preambleToString(Memory srcMem) {
    long pre0 = getAndCheckPreLongs(srcMem); //make sure we can get the assumed preamble
    int preLongs = extractPreLongs(pre0);   //byte 0
    int serVer = extractSerVer(pre0);       //byte 1
    Family family = Family.idToFamily(extractFamilyID(pre0)); //byte 2
    int lgMaxMapSize = extractLgMaxMapSize(pre0); //byte 3
    int lgCurMapSize = extractLgCurMapSize(pre0); //byte 4
    int flags = extractFlags(pre0);         //byte 5
    int type = extractFreqSketchType(pre0); //byte 6
    
    String flagsStr = zeroPad(Integer.toBinaryString(flags), 8) + ", " + (flags);
    boolean empty = (flags & EMPTY_FLAG_MASK) > 0;
    int maxMapSize = 1 << lgMaxMapSize;
    int curMapSize = 1 << lgCurMapSize;
    int maxPreLongs = Family.FREQUENCY.getMaxPreLongs();
    
    //Assumed if preLongs == 1
    int activeItems = 0;
    long streamLength = 0;
    long offset = 0;
    long mergeError = 0;
    
    //Assumed if preLongs == maxPreLongs
    
    if (preLongs == maxPreLongs) {
      //get full preamble
      long[] preArr = new long[preLongs];
      srcMem.getLongArray(0, preArr, 0, preLongs);
      activeItems =  extractActiveItems(preArr[1]);
      streamLength = preArr[2];
      offset = preArr[3];
      mergeError = preArr[4];
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append(LS)
      .append("### FREQUENCY SKETCH PREAMBLE SUMMARY:").append(LS)
      .append("Byte  0: Preamble Longs       : ").append(preLongs).append(LS)
      .append("Byte  1: Serialization Version: ").append(serVer).append(LS)
      .append("Byte  2: Family               : ").append(family.toString()).append(LS)
      .append("Byte  3: MaxMapSize           : ").append(maxMapSize).append(LS)
      .append("Byte  4: CurMapSize           : ").append(curMapSize).append(LS)
      .append("Byte  5: Flags Field          : ").append(flagsStr).append(LS)
      .append("  EMPTY                       : ").append(empty).append(LS)
      .append("Byte  6: Freq Sketch Type     : ").append(type).append(LS);
      
    if (preLongs == 1) {
      sb.append(" --ABSENT, ASSUMED:").append(LS);
    } else { //preLongs == maxPreLongs
      sb.append("Bytes 8-11 : ActiveItems    : ").append(activeItems).append(LS);
      sb.append("Bytes 16-23: StreamLength   : ").append(streamLength).append(LS)
        .append("Bytes 24-31: Offset         : ").append(offset).append(LS)
        .append("Bytes 32-40: MergeError     : ").append(mergeError).append(LS);
    }
    
    sb.append(  "Preamble Bytes                : ").append(preLongs * 8).append(LS);
    sb.append(  "TOTAL Sketch Bytes            : ").append((preLongs + activeItems*2) << 3).append(LS)
      .append("### END FREQUENCY SKETCH PREAMBLE SUMMARY").append(LS);
    return sb.toString();
  }

// @formatter:on
  
  static int extractPreLongs(final long pre0) { //Byte 0
    long mask = 0XFFL;
    return (int) (pre0 & mask);
  }

  static int extractSerVer(final long pre0) { //Byte 1
    int shift = SER_VER_BYTE << 3;
    long mask = 0XFFL;
    return (int) ((pre0 >>> shift) & mask);
  }

  static int extractFamilyID(final long pre0) { //Byte 2
    int shift = FAMILY_BYTE << 3;
    long mask = 0XFFL;
    return (int) ((pre0 >>> shift) & mask);
  }

  static int extractLgMaxMapSize(final long pre0) { //Byte 3
    int shift = LG_MAX_MAP_SIZE_BYTE << 3;
    long mask = 0XFFL;
    return (int) ((pre0 >>> shift) & mask);
  }
  
  static int extractLgCurMapSize(final long pre0) { //Byte 4
    int shift = LG_CUR_MAP_SIZE_BYTE << 3;
    long mask = 0XFFL;
    return (int) ((pre0 >>> shift) & mask);
  }
  
  static int extractFlags(final long pre0) { //Byte 5
    int shift = FLAGS_BYTE << 3;
    long mask = 0XFFL;
    return (int) ((pre0 >>> shift) & mask);
  }
  
  static int extractFreqSketchType(final long pre0) { //Byte 7
    int shift = FREQ_SKETCH_TYPE_BYTE << 3;
    long mask = 0XFFL;
    return (int) ((pre0 >>> shift) & mask);
  }
  
  static int extractActiveItems(final long pre1) { //Bytes 8 to 11
    long mask = 0XFFFFFFFFL;
    return (int) (pre1 & mask) ;
  }

  static long insertPreLongs(final int preLongs, final long pre0) { //Byte 0
    long mask = 0XFFL;
    return (preLongs & mask) | (~mask & pre0);
  }

  static long insertSerVer(final int serVer, final long pre0) { //Byte 1
    int shift = SER_VER_BYTE << 3;
    long mask = 0XFFL;
    return ((serVer & mask) << shift) | (~(mask << shift) & pre0); 
  }

  static long insertFamilyID(final int familyID, final long pre0) { //Byte 2
    int shift = FAMILY_BYTE << 3;
    long mask = 0XFFL;
    return ((familyID & mask) << shift) | (~(mask << shift) & pre0);
  }

  static long insertLgMaxMapSize(final int lgMaxMapSize, final long pre0) { //Byte 3
    int shift = LG_MAX_MAP_SIZE_BYTE << 3;
    long mask = 0XFFL;
    return ((lgMaxMapSize & mask) << shift) | (~(mask << shift) & pre0);
  }

  static long insertLgCurMapSize(final int lgCurMapSize, final long pre0) { //Byte 4
    int shift = LG_CUR_MAP_SIZE_BYTE << 3;
    long mask = 0XFFL;
    return ((lgCurMapSize & mask) << shift) | (~(mask << shift) & pre0);
  }

  static long insertFlags(final int flags, final long pre0) { //Byte 5
    int shift = FLAGS_BYTE << 3;
    long mask = 0XFFL;
    return ((flags & mask) << shift) | (~(mask << shift) & pre0);
  }

  static long insertFreqSketchType(final int freqSketchType, final long pre0) { //Byte 7
    int shift = FREQ_SKETCH_TYPE_BYTE << 3;
    long mask = 0XFFL;
    return ((freqSketchType & mask) << shift) | (~(mask << shift) & pre0);
  }
  
  static long insertActiveItems(final int activeItems, final long pre1) { //Bytes 8 to 11
    long mask = 0XFFFFFFFFL;
    return (activeItems & mask) | (~mask & pre1);
  }

  /**
   * Checks Memory for capacity to hold the preamble and returns the first 8 bytes.
   * @param mem the given Memory
   * @param max the max value for preLongs
   * @return the first 8 bytes of preamble as a long.
   */
  static long getAndCheckPreLongs(Memory mem) {
    long cap = mem.getCapacity();
    if (cap < 8) { throwNotBigEnough(cap, 8); }
    long pre0 = mem.getLong(0);
    int preLongs = extractPreLongs(pre0);
    int required = Math.max(preLongs << 3, 8);
    if (cap < required) { throwNotBigEnough(cap, required); }
    return pre0;
  }
  
  private static void throwNotBigEnough(long cap, int required) {
    throw new IllegalArgumentException(
        "Possible Corruption: Size of byte array or Memory not large enough: Size: " + cap 
        + ", Required: " + required);
  }
  
}
