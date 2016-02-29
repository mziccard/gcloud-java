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

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identity for a Google Compute Engine disk type.
 */
public final class DiskTypeId extends ZoneResourceId {

  static final Function<String, DiskTypeId> FROM_URL_FUNCTION = new Function<String, DiskTypeId>() {
    @Override
    public DiskTypeId apply(String pb) {
      return DiskTypeId.fromUrl(pb);
    }
  };
  static final Function<DiskTypeId, String> TO_URL_FUNCTION = new Function<DiskTypeId, String>() {
    @Override
    public String apply(DiskTypeId diskTypeId) {
      return diskTypeId.selfLink();
    }
  };

  private static final String REGEX = ZoneResourceId.REGEX + "diskTypes/([^/]+)";
  private static final Pattern PATTERN = Pattern.compile(REGEX);
  private static final long serialVersionUID = 7337881474103686219L;

  private final String diskType;

  private DiskTypeId(String project, String zone, String diskType) {
    super(project, zone);
    this.diskType = checkNotNull(diskType);
  }

  /**
   * Returns the name of the disk type.
   */
  public String diskType() {
    return diskType;
  }

  @Override
  public String selfLink() {
    return super.selfLink() + "/diskTypes/" + diskType;
  }

  @Override
  MoreObjects.ToStringHelper toStringHelper() {
    return super.toStringHelper().add("diskType", diskType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.baseHashCode(), diskType);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DiskTypeId
        && baseEquals((DiskTypeId) obj)
        && Objects.equals(diskType, ((DiskTypeId) obj).diskType);
  }

  @Override
  DiskTypeId setProjectId(String projectId) {
    if (project() != null) {
      return this;
    }
    return DiskTypeId.of(projectId, zone(), diskType);
  }

  /**
   * Returns a disk type identity given the zone identity and the disk type name.
   */
  public static DiskTypeId of(ZoneId zoneId, String diskType) {
    return new DiskTypeId(zoneId.project(), zoneId.zone(), diskType);
  }

  /**
   * Returns a disk type identity given the zone and disk type names.
   */
  public static DiskTypeId of(String zone, String diskType) {
    return of(ZoneId.of(null, zone), diskType);
  }

  /**
   * Returns a disk type identity given project disk, zone and disk type names.
   */
  public static DiskTypeId of(String project, String zone, String diskType) {
    return of(ZoneId.of(project, zone), diskType);
  }

  /**
   * Returns {@code true} if the provided string matches the expected format of a disk type URL.
   * Returns {@code false} otherwise.
   */
  static boolean matchesUrl(String url) {
    return PATTERN.matcher(url).matches();
  }

  static DiskTypeId fromUrl(String url) {
    Matcher matcher = PATTERN.matcher(url);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(url + " is not a valid disk type URL");
    }
    return DiskTypeId.of(matcher.group(1), matcher.group(2), matcher.group(3));
  }
}
