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

import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.MockExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.parser.RecipeCompiler;
import io.cdap.wrangler.parser.TextDirectives;
import io.cdap.wrangler.registry.DirectiveRegistry;
import io.cdap.wrangler.TestingRig;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for {@link AggregateStatsDirective}.
 */
public class AggregateStatsDirectiveTest {

  @Test
  public void testBasicAggregation() throws Exception {
    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
    };

    // Create test rows with various byte sizes and time durations
    Row row1 = new Row();
    row1.add("data_size", new ByteSize("10MB"));
    row1.add("response_time", new TimeDuration("500ms"));

    Row row2 = new Row();
    row2.add("data_size", new ByteSize("5MB"));
    row2.add("response_time", new TimeDuration("750ms"));

    Row row3 = new Row();
    row3.add("data_size", new ByteSize("15MB"));
    row3.add("response_time", new TimeDuration("250ms"));

    // Create an executor context that marks this as the last batch
    MockExecutorContext context = new MockExecutorContext();
    context.setLast(true);

    // Execute the directive
    AggregateStatsDirective directive = new AggregateStatsDirective();
    directive.initialize(TestingRig.parse(directives[0], directive));
    
    List<Row> rows = Arrays.asList(row1, row2, row3);
    List<Row> results = directive.execute(rows, context);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Total size should be 10MB + 5MB + 15MB = 30MB
    Assert.assertEquals(30.0, (Double) result.getValue("total_size_mb"), 0.001);
    
    // Total time should be 500ms + 750ms + 250ms = 1500ms = 1.5s
    Assert.assertEquals(1.5, (Double) result.getValue("total_time_sec"), 0.001);
  }

  @Test
  public void testAggregationWithDifferentUnits() throws Exception {
    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_gb total_time_min 'GB' 'min'"
    };

    // Create test rows with various byte sizes and time durations
    Row row1 = new Row();
    row1.add("data_size", new ByteSize("1024MB"));
    row1.add("response_time", new TimeDuration("30s"));

    Row row2 = new Row();
    row2.add("data_size", new ByteSize("2GB"));
    row2.add("response_time", new TimeDuration("1.5m"));

    // Create an executor context that marks this as the last batch
    MockExecutorContext context = new MockExecutorContext();
    context.setLast(true);

    // Execute the directive
    AggregateStatsDirective directive = new AggregateStatsDirective();
    directive.initialize(TestingRig.parse(directives[0], directive));
    
    List<Row> rows = Arrays.asList(row1, row2);
    List<Row> results = directive.execute(rows, context);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Total size should be 1GB + 2GB = 3GB
    Assert.assertEquals(3.0, (Double) result.getValue("total_size_gb"), 0.001);
    
    // Total time should be 30s + 1.5m = 30s + 90s = 120s = 2min
    Assert.assertEquals(2.0, (Double) result.getValue("total_time_min"), 0.001);
  }

  @Test
  public void testAggregationWithStringValues() throws Exception {
    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_mb total_time_sec"
    };

    // Create test rows with string values instead of ByteSize/TimeDuration objects
    Row row1 = new Row();
    row1.add("data_size", "10MB");
    row1.add("response_time", "500ms");

    Row row2 = new Row();
    row2.add("data_size", "5MB");
    row2.add("response_time", "750ms");

    // Create an executor context that marks this as the last batch
    MockExecutorContext context = new MockExecutorContext();
    context.setLast(true);

    // Execute the directive
    AggregateStatsDirective directive = new AggregateStatsDirective();
    directive.initialize(TestingRig.parse(directives[0], directive));
    
    List<Row> rows = Arrays.asList(row1, row2);
    List<Row> results = directive.execute(rows, context);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Total size should be 10MB + 5MB = 15MB
    Assert.assertEquals(15.0, (Double) result.getValue("total_size_mb"), 0.001);
    
    // Total time should be 500ms + 750ms = 1250ms = 1.25s
    Assert.assertEquals(1.25, (Double) result.getValue("total_time_sec"), 0.001);
  }

  @Test
  public void testAggregationWithNumericValues() throws Exception {
    String[] directives = new String[] {
      "aggregate-stats :data_size :response_time total_size_kb total_time_ms"
    };

    // Create test rows with numeric values (assumed to be bytes and milliseconds)
    Row row1 = new Row();
    row1.add("data_size", 10240L); // 10KB
    row1.add("response_time", 500L); // 500ms

    Row row2 = new Row();
    row2.add("data_size", 5120L); // 5KB
    row2.add("response_time", 750L); // 750ms

    // Create an executor context that marks this as the last batch
    MockExecutorContext context = new MockExecutorContext();
    context.setLast(true);

    // Execute the directive
    AggregateStatsDirective directive = new AggregateStatsDirective();
    directive.initialize(TestingRig.parse(directives[0], directive));
    
    List<Row> rows = Arrays.asList(row1, row2);
    List<Row> results = directive.execute(rows, context);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    
    // Total size should be 10KB + 5KB = 15KB
    Assert.assertEquals(15.0, (Double) result.getValue("total_size_kb"), 0.001);
    
    // Total time should be 500ms + 750ms = 1250ms
    Assert.assertEquals(1250.0, (Double) result.getValue("total_time_ms"), 0.001);
  }
  
  @Test
  public void testParserIntegration() throws Exception {
    String recipe = "aggregate-stats :data_size :response_time total_size_mb total_time_sec";
    RecipeCompiler compiler = new RecipeCompiler();
    compiler.compile(new TextDirectives(recipe), DirectiveRegistry.getInstance());
    
    // This test just verifies that the directive can be parsed without exceptions
    Assert.assertTrue(true);
  }
}