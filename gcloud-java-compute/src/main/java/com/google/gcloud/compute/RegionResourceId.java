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

package com.google.gcloud.compute;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects.ToStringHelper;

import java.util.Objects;

/**
 * A base class for the identity of Google Compute Engine resources that live in a region.
 */
public abstract class RegionResourceId extends ResourceId {

  private static final long serialVersionUID = 5569092266957249294L;

  private final String region;

  RegionResourceId(String project, String region) {
    super(project);
    this.region = checkNotNull(region);
  }

  RegionResourceId(RegionId regionId) {
    super(regionId.project());
    this.region = checkNotNull(regionId.region());
  }

  /**
   * Returns the name of the region this resource belongs to.
   */
  public final String region() {
    return region;
  }

  /**
   * Returns the identity of the region this resource belongs to.
   */
  public final RegionId regionId() {
    return RegionId.of(project(), region);
  }

  @Override
  public String toUrl() {
    return super.toUrl() + "/regions/" + region;
  }

  @Override
  ToStringHelper toStringHelper() {
    return super.toStringHelper().add("region", region);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseHashCode(), region);
  }

  @Override
  public boolean equals(Object obj) {
    return obj != null
        && obj.getClass().equals(RegionResourceId.class)
        && baseEquals((RegionResourceId) obj);
  }
}