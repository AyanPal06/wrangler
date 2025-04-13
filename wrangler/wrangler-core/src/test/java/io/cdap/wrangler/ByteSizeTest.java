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
 * Tests for {@link ByteSize} class.
 */
public class ByteSizeTest {

  @Test
  public void testByteSizeParsing() {
    // Test bytes (B)
    ByteSize byteSize1 = new ByteSize("100B");
    Assert.assertEquals(100L, byteSize1.getBytes());
    Assert.assertEquals(100.0, byteSize1.getValue(), 0.001);
    Assert.assertEquals("B", byteSize1.getUnit());
    Assert.assertEquals("100B", byteSize1.getOriginal());
    
    // Test kilobytes (KB)
    ByteSize byteSize2 = new ByteSize("5KB");
    Assert.assertEquals(5 * 1024L, byteSize2.getBytes());
    Assert.assertEquals(5.0, byteSize2.getValue(), 0.001);
    Assert.assertEquals("KB", byteSize2.getUnit());
    
    // Test megabytes (MB)
    ByteSize byteSize3 = new ByteSize("2.5MB");
    Assert.assertEquals(Math.round(2.5 * 1024 * 1024), byteSize3.getBytes());
    Assert.assertEquals(2.5, byteSize3.getValue(), 0.001);
    Assert.assertEquals("MB", byteSize3.getUnit());
    
    // Test gigabytes (GB)
    ByteSize byteSize4 = new ByteSize("1GB");
    Assert.assertEquals(1024L * 1024L * 1024L, byteSize4.getBytes());
    Assert.assertEquals(1.0, byteSize4.getValue(), 0.001);
    Assert.assertEquals("GB", byteSize4.getUnit());
    
    // Test terabytes (TB)
    ByteSize byteSize5 = new ByteSize("0.5TB");
    Assert.assertEquals(Math.round(0.5 * 1024L * 1024L * 1024L * 1024L), byteSize5.getBytes());
    Assert.assertEquals(0.5, byteSize5.getValue(), 0.001);
    Assert.assertEquals("TB", byteSize5.getUnit());
    
    // Test lowercase units
    ByteSize byteSize6 = new ByteSize("10kb");
    Assert.assertEquals(10 * 1024L, byteSize6.getBytes());
    Assert.assertEquals(10.0, byteSize6.getValue(), 0.001);
    Assert.assertEquals("kb", byteSize6.getUnit());
  }
  
  @Test
  public void testConversions() {
    ByteSize byteSize = new ByteSize("1024KB");
    Assert.assertEquals(1024 * 1024L, byteSize.getBytes());
    Assert.assertEquals(1024.0, byteSize.getKilobytes(), 0.001);
    Assert.assertEquals(1.0, byteSize.getMegabytes(), 0.001);
    Assert.assertEquals(1.0 / 1024, byteSize.getGigabytes(), 0.001);
    Assert.assertEquals(1.0 / (1024 * 1024), byteSize.getTerabytes(), 0.001);
    Assert.assertEquals(1.0 / (1024 * 1024 * 1024), byteSize.getPetabytes(), 0.001);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new ByteSize("not-a-byte-size");
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new ByteSize("10XB");
  }
}