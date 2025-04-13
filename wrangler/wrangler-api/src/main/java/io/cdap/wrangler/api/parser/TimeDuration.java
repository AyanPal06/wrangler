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
 * A Token that represents time duration with units (ms, s, m, h, d)
 */
public class TimeDuration extends Token<Long> {
  // Pattern to match number + time unit, e.g., "150ms", "2.5s"
  private static final Pattern PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)(ms|s|m|min|h|d)");
  
  // Conversion factors to nanoseconds
  private static final long MS_FACTOR = 1_000_000L;
  private static final long S_FACTOR = 1_000_000_000L;
  private static final long M_FACTOR = 60 * S_FACTOR;
  private static final long H_FACTOR = 60 * M_FACTOR;
  private static final long D_FACTOR = 24 * H_FACTOR;
  
  private final String original;
  private final double value;
  private final String unit;
  private final long nanoseconds;

  public TimeDuration(String text) {
    super(TokenType.TIME_DURATION);
    this.original = text;

    Matcher matcher = PATTERN.matcher(text);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time duration format: " + text);
    }

    this.value = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(2);
    this.nanoseconds = convertToNanoseconds(value, unit);
  }

  /**
   * Converts the duration to nanoseconds based on the unit.
   *
   * @param value the numeric value
   * @param unit the unit (ms, s, m, h, d)
   * @return the duration in nanoseconds
   */
  private long convertToNanoseconds(double value, String unit) {
    switch (unit) {
      case "ms":
        return Math.round(value * MS_FACTOR);
      case "s":
        return Math.round(value * S_FACTOR);
      case "m":
      case "min":
        return Math.round(value * M_FACTOR);
      case "h":
        return Math.round(value * H_FACTOR);
      case "d":
        return Math.round(value * D_FACTOR);
      default:
        throw new IllegalArgumentException("Unknown time unit: " + unit);
    }
  }

  /**
   * @return the duration in nanoseconds
   */
  public long getNanoseconds() {
    return nanoseconds;
  }

  /**
   * @return the duration in milliseconds
   */
  public double getMilliseconds() {
    return nanoseconds / (double) MS_FACTOR;
  }

  /**
   * @return the duration in seconds
   */
  public double getSeconds() {
    return nanoseconds / (double) S_FACTOR;
  }

  /**
   * @return the duration in minutes
   */
  public double getMinutes() {
    return nanoseconds / (double) M_FACTOR;
  }

  /**
   * @return the duration in hours
   */
  public double getHours() {
    return nanoseconds / (double) H_FACTOR;
  }

  /**
   * @return the duration in days
   */
  public double getDays() {
    return nanoseconds / (double) D_FACTOR;
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
    return nanoseconds;
  }

  @Override
  public String toString() {
    return original;
  }
}