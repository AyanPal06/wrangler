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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TimeDuration} class.
 */
public class TimeDurationTest {

  @Test
  public void testTimeDurationParsing() {
    // Test milliseconds (ms)
    TimeDuration timeDuration1 = new TimeDuration("100ms");
    Assert.assertEquals(100 * 1_000_000L, timeDuration1.getNanoseconds());
    Assert.assertEquals(100.0, timeDuration1.getValue(), 0.001);
    Assert.assertEquals("ms", timeDuration1.getUnit());
    Assert.assertEquals("100ms", timeDuration1.getOriginal());
    
    // Test seconds (s)
    TimeDuration timeDuration2 = new TimeDuration("5s");
    Assert.assertEquals(5 * 1_000_000_000L, timeDuration2.getNanoseconds());
    Assert.assertEquals(5.0, timeDuration2.getValue(), 0.001);
    Assert.assertEquals("s", timeDuration2.getUnit());
    
    // Test minutes (m)
    TimeDuration timeDuration3 = new TimeDuration("2.5m");
    Assert.assertEquals(Math.round(2.5 * 60 * 1_000_000_000L), timeDuration3.getNanoseconds());
    Assert.assertEquals(2.5, timeDuration3.getValue(), 0.001);
    Assert.assertEquals("m", timeDuration3.getUnit());
    
    // Test minutes (min)
    TimeDuration timeDuration4 = new TimeDuration("1min");
    Assert.assertEquals(60 * 1_000_000_000L, timeDuration4.getNanoseconds());
    Assert.assertEquals(1.0, timeDuration4.getValue(), 0.001);
    Assert.assertEquals("min", timeDuration4.getUnit());
    
    // Test hours (h)
    TimeDuration timeDuration5 = new TimeDuration("0.5h");
    Assert.assertEquals(Math.round(0.5 * 60 * 60 * 1_000_000_000L), timeDuration5.getNanoseconds());
    Assert.assertEquals(0.5, timeDuration5.getValue(), 0.001);
    Assert.assertEquals("h", timeDuration5.getUnit());
    
    // Test days (d)
    TimeDuration timeDuration6 = new TimeDuration("1d");
    Assert.assertEquals(24 * 60 * 60 * 1_000_000_000L, timeDuration6.getNanoseconds());
    Assert.assertEquals(1.0, timeDuration6.getValue(), 0.001);
    Assert.assertEquals("d", timeDuration6.getUnit());
  }
  
  @Test
  public void testConversions() {
    TimeDuration timeDuration = new TimeDuration("60s");
    Assert.assertEquals(60 * 1_000_000_000L, timeDuration.getNanoseconds());
    Assert.assertEquals(60 * 1000.0, timeDuration.getMilliseconds(), 0.001);
    Assert.assertEquals(60.0, timeDuration.getSeconds(), 0.001);
    Assert.assertEquals(1.0, timeDuration.getMinutes(), 0.001);
    Assert.assertEquals(1.0 / 60, timeDuration.getHours(), 0.001);
    Assert.assertEquals(1.0 / (24 * 60), timeDuration.getDays(), 0.001);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("not-a-time-duration");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new TimeDuration("10xy");
  }
}