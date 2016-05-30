/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.WaitForOption.CheckingPeriod;
import com.google.cloud.WaitForOption.Option;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WaitForOptionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final WaitForOption CHECKING_PERIOD_OPTION =
      WaitForOption.checkEvery(42, TimeUnit.MILLISECONDS);
  private static final WaitForOption TIMEOUT_OPTION =
      WaitForOption.timeout(43, TimeUnit.MILLISECONDS);

  @Test
  public void testCheckEvery() {
    assertEquals(Option.CHECKING_PERIOD, CHECKING_PERIOD_OPTION.option());
    assertTrue(CHECKING_PERIOD_OPTION.value() instanceof CheckingPeriod);
    CheckingPeriod checkingPeriod = (CheckingPeriod) CHECKING_PERIOD_OPTION.value();
    assertEquals(42, checkingPeriod.period());
    assertEquals(TimeUnit.MILLISECONDS, checkingPeriod.unit());
  }

  @Test
  public void testCheckEvery_InvalidPeriod() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("checkEvery must be >= 0");
    WaitForOption.checkEvery(-1, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testTimeout() {
    assertEquals(Option.TIMEOUT, TIMEOUT_OPTION.option());
    assertEquals(43L, TIMEOUT_OPTION.value());
  }

  @Test
  public void testTimeout_InvalidTimeout() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("timeout must be >= 0");
    WaitForOption.timeout(-1, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testEqualsAndHashCode() {
    assertEquals(CHECKING_PERIOD_OPTION, CHECKING_PERIOD_OPTION);
    assertEquals(TIMEOUT_OPTION, TIMEOUT_OPTION);
    assertEquals(CHECKING_PERIOD_OPTION.hashCode(), CHECKING_PERIOD_OPTION.hashCode());
    assertEquals(TIMEOUT_OPTION.hashCode(), TIMEOUT_OPTION.hashCode());
    WaitForOption checkingPeriodOption = WaitForOption.checkEvery(42, TimeUnit.MILLISECONDS);
    assertEquals(CHECKING_PERIOD_OPTION, checkingPeriodOption);
    assertEquals(CHECKING_PERIOD_OPTION.hashCode(), checkingPeriodOption.hashCode());
    WaitForOption timeoutOption = WaitForOption.timeout(43, TimeUnit.MILLISECONDS);
    assertEquals(TIMEOUT_OPTION, timeoutOption);
    assertEquals(TIMEOUT_OPTION.hashCode(), timeoutOption.hashCode());
    assertNotEquals(CHECKING_PERIOD_OPTION, TIMEOUT_OPTION);
    assertNotEquals(CHECKING_PERIOD_OPTION.hashCode(), TIMEOUT_OPTION.hashCode());
    checkingPeriodOption = WaitForOption.checkEvery(43, TimeUnit.MILLISECONDS);
    assertNotEquals(CHECKING_PERIOD_OPTION, checkingPeriodOption);
    assertNotEquals(CHECKING_PERIOD_OPTION.hashCode(), checkingPeriodOption.hashCode());
    checkingPeriodOption = WaitForOption.checkEvery(42, TimeUnit.SECONDS);
    assertNotEquals(CHECKING_PERIOD_OPTION, checkingPeriodOption);
    assertNotEquals(CHECKING_PERIOD_OPTION.hashCode(), checkingPeriodOption.hashCode());
    timeoutOption = WaitForOption.timeout(42, TimeUnit.MILLISECONDS);
    assertNotEquals(TIMEOUT_OPTION, timeoutOption);
    assertNotEquals(TIMEOUT_OPTION.hashCode(), timeoutOption.hashCode());
    timeoutOption = WaitForOption.timeout(43, TimeUnit.SECONDS);
    assertNotEquals(TIMEOUT_OPTION, timeoutOption);
    assertNotEquals(TIMEOUT_OPTION.hashCode(), timeoutOption.hashCode());
  }

  @Test
  public void testAsMap() {
    Map<Option, Object> optionMap = WaitForOption.asMap(CHECKING_PERIOD_OPTION, TIMEOUT_OPTION);
    CheckingPeriod checkingPeriod = Option.CHECKING_PERIOD.getCheckingPeriod(optionMap);
    assertEquals(42, checkingPeriod.period());
    assertEquals(TimeUnit.MILLISECONDS, checkingPeriod.unit());
    assertEquals(43, (long) Option.TIMEOUT.getLong(optionMap));
  }

  @Test
  public void testAsMap_DuplicateOption() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(String.format("Duplicate option %s", CHECKING_PERIOD_OPTION));
    WaitForOption.asMap(CHECKING_PERIOD_OPTION, CHECKING_PERIOD_OPTION);
  }
}
