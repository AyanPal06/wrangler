/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Token that represents byte size with units (B, KB, MB, GB, TB, PB)
 */
public class ByteSize extends Token<Long> {
  // Pattern to match number + byte unit, e.g., "10KB", "1.5MB"
  private static final Pattern PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(B|b|KB|Kb|kb|MB|Mb|mb|GB|Gb|gb|TB|Tb|tb|PB|Pb|pb)");
  
  // Conversion factors to bytes
  private static final long B_FACTOR = 1L;
  private static final long KB_FACTOR = 1024L;
  private static final long MB_FACTOR = 1024L * 1024L;
  private static final long GB_FACTOR = 1024L * 1024L * 1024L;
  private static final long TB_FACTOR = 1024L * 1024L * 1024L * 1024L;
  private static final long PB_FACTOR = 1024L * 1024L * 1024L * 1024L * 1024L;
  
  private final String original;
  private final double value;
  private final String unit;
  private final long bytes;

  public ByteSize(String text) {
    super(TokenType.BYTE_SIZE);
    this.original = text;

    Matcher matcher = PATTERN.matcher(text);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid byte size format: " + text);
    }

    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2);
    this.bytes = convertToBytes(value, unit);
  }

  /**
   * Converts the size to bytes based on the unit.
   *
   * @param value the numeric value
   * @param unit the unit (B, KB, MB, etc.)
   * @return the size in bytes
   */
  private long convertToBytes(double value, String unit) {
    unit = unit.toUpperCase();
    switch (unit) {
      case "B":
        return Math.round(value * B_FACTOR);
      case "KB":
      case "KB":
        return Math.round(value * KB_FACTOR);
      case "MB":
      case "MB":
        return Math.round(value * MB_FACTOR);
      case "GB":
      case "GB":
        return Math.round(value * GB_FACTOR);
      case "TB":
      case "TB":
        return Math.round(value * TB_FACTOR);
      case "PB":
      case "PB":
        return Math.round(value * PB_FACTOR);
      default:
        throw new IllegalArgumentException("Unknown byte unit: " + unit);
    }
  }

  /**
   * @return the size in bytes
   */
  public long getBytes() {
    return bytes;
  }

  /**
   * @return the size in kilobytes
   */
  public double getKilobytes() {
    return bytes / (double) KB_FACTOR;
  }

  /**
   * @return the size in megabytes
   */
  public double getMegabytes() {
    return bytes / (double) MB_FACTOR;
  }

  /**
   * @return the size in gigabytes
   */
  public double getGigabytes() {
    return bytes / (double) GB_FACTOR;
  }

  /**
   * @return the size in terabytes
   */
  public double getTerabytes() {
    return bytes / (double) TB_FACTOR;
  }

  /**
   * @return the size in petabytes
   */
  public double getPetabytes() {
    return bytes / (double) PB_FACTOR;
  }

  /**
   * @return the original numeric value provided by the user
   */
  public double getValue() {
    return value;
  }

  /**
   * @return the original unit provided by the user
   */
  public String getUnit() {
    return unit;
  }

  /**
   * @return the original text representation
   */
  public String getOriginal() {
    return original;
  }

  @Override
  public Long value() {
    return bytes;
  }

  @Override
  public String toString() {
    return original;
  }
}