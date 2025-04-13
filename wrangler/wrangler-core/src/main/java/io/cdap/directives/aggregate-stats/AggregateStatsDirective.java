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

package io.cdap.wrangler.steps;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A directive that aggregates byte size and time duration data.
 */
@Plugin(type = Directive.Type)
@Name("aggregate-stats")
@Categories(categories = {"aggregate"})
@Description("Aggregates byte size and time duration data from specified columns")
public class AggregateStatsDirective implements Directive, Lineage {
  public static final String DIRECTIVE_NAME = "aggregate-stats";
  private String sizeColumnName;
  private String timeColumnName;
  private String totalSizeColumnName;
  private String totalTimeColumnName;
  private String sizeUnit;
  private String timeUnit;
  
  // Aggregation values stored in context
  private static final String SIZE_TOTAL_KEY = "aggregate-stats.size.total";
  private static final String TIME_TOTAL_KEY = "aggregate-stats.time.total";
  private static final String ROW_COUNT_KEY = "aggregate-stats.row.count";

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(DIRECTIVE_NAME);
    builder.define("size-column", TokenType.COLUMN_NAME);
    builder.define("time-column", TokenType.COLUMN_NAME);
    builder.define("total-size-column", TokenType.COLUMN_NAME);
    builder.define("total-time-column", TokenType.COLUMN_NAME);
    builder.define("size-unit", TokenType.STRING, Optional.TRUE);
    builder.define("time-unit", TokenType.STRING, Optional.TRUE);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    sizeColumnName = ((ColumnName) args.value("size-column")).value();
    timeColumnName = ((ColumnName) args.value("time-column")).value();
    totalSizeColumnName = ((ColumnName) args.value("total-size-column")).value();
    totalTimeColumnName = ((ColumnName) args.value("total-time-column")).value();
    
    // Default units are MB for size and seconds for time
    sizeUnit = args.contains("size-unit") ? ((Text) args.value("size-unit")).value() : "MB";
    timeUnit = args.contains("time-unit") ? ((Text) args.value("time-unit")).value() : "s";
    
    // Validate units
    validateSizeUnit(sizeUnit);
    validateTimeUnit(timeUnit);
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    // Get or initialize aggregation values from context
    Map<String, Object> transientStore = context.getTransientStore();
    Long totalSizeBytes = (Long) transientStore.getOrDefault(SIZE_TOTAL_KEY, 0L);
    Long totalTimeNanos = (Long) transientStore.getOrDefault(TIME_TOTAL_KEY, 0L);
    Long rowCount = (Long) transientStore.getOrDefault(ROW_COUNT_KEY, 0L);
    
    // Process each row
    for (Row row : rows) {
      // Process size column
      if (row.find(sizeColumnName) != -1) {
        Object sizeObj = row.getValue(sizeColumnName);
        long bytes = extractBytes(sizeObj);
        totalSizeBytes += bytes;
      }
      
      // Process time column
      if (row.find(timeColumnName) != -1) {
        Object timeObj = row.getValue(timeColumnName);
        long nanos = extractNanos(timeObj);
        totalTimeNanos += nanos;
      }
      
      rowCount++;
    }
    
    // Update aggregation values in context
    transientStore.put(SIZE_TOTAL_KEY, totalSizeBytes);
    transientStore.put(TIME_TOTAL_KEY, totalTimeNanos);
    transientStore.put(ROW_COUNT_KEY, rowCount);
    
    // Check if this is the last batch (end of input)
    if (context.isLast()) {
      // Convert totals to desired units
      double totalSizeInDesiredUnit = convertSizeToUnit(totalSizeBytes, sizeUnit);
      double totalTimeInDesiredUnit = convertTimeToUnit(totalTimeNanos, timeUnit);
      
      // Create a new row with aggregation results
      Row resultRow = new Row();
      resultRow.add(totalSizeColumnName, totalSizeInDesiredUnit);
      resultRow.add(totalTimeColumnName, totalTimeInDesiredUnit);
      
      // Clear aggregation values from context
      transientStore.remove(SIZE_TOTAL_KEY);
      transientStore.remove(TIME_TOTAL_KEY);
      transientStore.remove(ROW_COUNT_KEY);
      
      List<Row> result = new ArrayList<>();
      result.add(resultRow);
      return result;
    }
    
    // Return empty list for intermediate batches
    return new ArrayList<>();
  }

  /**
   * Extracts bytes from a size object.
   *
   * @param sizeObj the size object (String, ByteSize, Number)
   * @return the size in bytes
   */
  private long extractBytes(Object sizeObj) throws DirectiveExecutionException {
    if (sizeObj instanceof ByteSize) {
      return ((ByteSize) sizeObj).getBytes();
    } else if (sizeObj instanceof String) {
      try {
        ByteSize byteSize = new ByteSize((String) sizeObj);
        return byteSize.getBytes();
      } catch (IllegalArgumentException e) {
        try {
          // Try parsing as a number
          return Long.parseLong((String) sizeObj);
        } catch (NumberFormatException ex) {
          throw new DirectiveExecutionException(
            String.format("Unable to parse '%s' as byte size", sizeObj));
        }
      }
    } else if (sizeObj instanceof Number) {
      return ((Number) sizeObj).longValue();
    } else {
      throw new DirectiveExecutionException(
        String.format("Column '%s' is not a valid byte size: %s", sizeColumnName, sizeObj));
    }
  }

  /**
   * Extracts nanoseconds from a time object.
   *
   * @param timeObj the time object (String, TimeDuration, Number)
   * @return the time in nanoseconds
   */
  private long extractNanos(Object timeObj) throws DirectiveExecutionException {
    if (timeObj instanceof TimeDuration) {
      return ((TimeDuration) timeObj).getNanoseconds();
    } else if (timeObj instanceof String) {
      try {
        TimeDuration timeDuration = new TimeDuration((String) timeObj);
        return timeDuration.getNanoseconds();
      } catch (IllegalArgumentException e) {
        try {
          // Try parsing as a number (assumed to be in milliseconds)
          return Long.parseLong((String) timeObj) * 1_000_000L;
        } catch (NumberFormatException ex) {
          throw new DirectiveExecutionException(
            String.format("Unable to parse '%s' as time duration", timeObj));
        }
      }
    } else if (timeObj instanceof Number) {
      // Assume milliseconds for raw numbers
      return ((Number) timeObj).longValue() * 1_000_000L;
    } else {
      throw new DirectiveExecutionException(
        String.format("Column '%s' is not a valid time duration: %s", timeColumnName, timeObj));
    }
  }

  /**
   * Converts size in bytes to the desired unit.
   *
   * @param bytes the size in bytes
   * @param unit the target unit (B, KB, MB, GB, TB, PB)
   * @return the size in the target unit
   */
  private double convertSizeToUnit(long bytes, String unit) {
    unit = unit.toUpperCase();
    switch (unit) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024.0);
      case "GB":
        return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      case "PB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);

default:
        throw new IllegalArgumentException("Unknown byte unit: " + unit);
    }
  }

  /**
   * Converts time in nanoseconds to the desired unit.
   *
   * @param nanos the time in nanoseconds
   * @param unit the target unit (ms, s, m/min, h, d)
   * @return the time in the target unit
   */
  private double convertTimeToUnit(long nanos, String unit) {
    switch (unit) {
      case "ns":
        return nanos;
      case "ms":
        return nanos / 1_000_000.0;
      case "s":
        return nanos / 1_000_000_000.0;
      case "m":
      case "min":
        return nanos / (60.0 * 1_000_000_000.0);
      case "h":
        return nanos / (60.0 * 60.0 * 1_000_000_000.0);
      case "d":
        return nanos / (24.0 * 60.0 * 60.0 * 1_000_000_000.0);
      default:
        throw new IllegalArgumentException("Unknown time unit: " + unit);
    }
  }

  /**
   * Validates that the size unit is supported.
   *
   * @param unit the size unit to validate
   * @throws DirectiveParseException if the unit is not supported
   */
  private void validateSizeUnit(String unit) throws DirectiveParseException {
    unit = unit.toUpperCase();
    if (!unit.equals("B") && !unit.equals("KB") && !unit.equals("MB") &&
        !unit.equals("GB") && !unit.equals("TB") && !unit.equals("PB")) {
      throw new DirectiveParseException(
        String.format("Invalid size unit '%s'. Supported units are B, KB, MB, GB, TB, PB", unit));
    }
  }

  /**
   * Validates that the time unit is supported.
   *
   * @param unit the time unit to validate
   * @throws DirectiveParseException if the unit is not supported
   */
  private void validateTimeUnit(String unit) throws DirectiveParseException {
    if (!unit.equals("ns") && !unit.equals("ms") && !unit.equals("s") &&
        !unit.equals("m") && !unit.equals("min") && !unit.equals("h") && !unit.equals("d")) {
      throw new DirectiveParseException(
        String.format("Invalid time unit '%s'. Supported units are ns, ms, s, m, min, h, d", unit));
    }
  }

  @Override
  public Mutation lineage() {
    return Mutation.builder()
      .readable("Aggregate byte size from column '%s' and time duration from column '%s' into '%s' and '%s'",
                sizeColumnName, timeColumnName, totalSizeColumnName, totalTimeColumnName)
      .relation(sizeColumnName, totalSizeColumnName)
      .relation(timeColumnName, totalTimeColumnName)
      .build();
  }

  @Override
  public String toString() {
    return String.format("%s %s %s %s %s %s %s",
                        DIRECTIVE_NAME, sizeColumnName, timeColumnName,
                        totalSizeColumnName, totalTimeColumnName,
                        sizeUnit, timeUnit);
  }
}