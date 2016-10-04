/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.google.cloud.storage;

import static org.junit.Assert.assertEquals;

import com.google.api.services.storage.model.BucketAccessControl;
import com.google.api.services.storage.model.ObjectAccessControl;
import com.google.cloud.storage.Acl.Domain;
import com.google.cloud.storage.Acl.Entity;
import com.google.cloud.storage.Acl.Entity.Type;
import com.google.cloud.storage.Acl.Group;
import com.google.cloud.storage.Acl.Project;
import com.google.cloud.storage.Acl.Project.ProjectRole;
import com.google.cloud.storage.Acl.RawEntity;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Acl.User;

import org.junit.Test;

public class AclTest {

  private static final Role ROLE = Role.OWNER;
  private static final Entity ENTITY = User.ofAllAuthenticatedUsers();
  private static final String ETAG = "etag";
  private static final String ID = "id";
  private static final Acl ACL = Acl.newBuilder(ENTITY, ROLE).setEtag(ETAG).setId(ID).build();
  private static final Acl DEPRECATED_ACL = Acl.builder(ENTITY, ROLE).etag(ETAG).id(ID).build();

  @Test
  public void testBuilder() {
    assertEquals(ROLE, ACL.getRole());
    assertEquals(ENTITY, ACL.getEntity());
    assertEquals(ETAG, ACL.getEtag());
    assertEquals(ID, ACL.getId());
  }

  @Test
  public void testBuilderDeprecated() {
    assertEquals(ROLE, DEPRECATED_ACL.role());
    assertEquals(ENTITY, DEPRECATED_ACL.entity());
    assertEquals(ETAG, DEPRECATED_ACL.etag());
    assertEquals(ID, DEPRECATED_ACL.id());
  }

  @Test
  public void testToBuilder() {
    assertEquals(ACL, ACL.toBuilder().build());
    Acl acl = ACL.toBuilder()
        .setEtag("otherEtag")
        .setId("otherId")
        .setRole(Role.READER)
        .setEntity(User.ofAllUsers())
        .build();
    assertEquals(Role.READER, acl.getRole());
    assertEquals(User.ofAllUsers(), acl.getEntity());
    assertEquals("otherEtag", acl.getEtag());
    assertEquals("otherId", acl.getId());
  }

  @Test
  public void testToBuilderDeprecated() {
    assertEquals(DEPRECATED_ACL, DEPRECATED_ACL.toBuilder().build());
    Acl acl = DEPRECATED_ACL.toBuilder()
        .etag("otherEtag")
        .id("otherId")
        .role(Role.READER)
        .entity(User.ofAllUsers())
        .build();
    assertEquals(Role.READER, acl.role());
    assertEquals(User.ofAllUsers(), acl.entity());
    assertEquals("otherEtag", acl.etag());
    assertEquals("otherId", acl.id());
  }

  @Test
  public void testToAndFromPb() {
    assertEquals(ACL, Acl.fromPb(ACL.toBucketPb()));
    assertEquals(ACL, Acl.fromPb(ACL.toObjectPb()));
  }

  @Test
  public void testDomainEntity() {
    Domain acl = new Domain("d1");
    assertEquals("d1", acl.getDomain());
    assertEquals(Type.DOMAIN, acl.getType());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testDomainEntityDeprecated() {
    Domain acl = new Domain("d1");
    assertEquals("d1", acl.domain());
    assertEquals(Type.DOMAIN, acl.type());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testGroupEntity() {
    Group acl = new Group("g1");
    assertEquals("g1", acl.getEmail());
    assertEquals(Type.GROUP, acl.getType());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testGroupEntityDeprecated() {
    Group acl = new Group("g1");
    assertEquals("g1", acl.email());
    assertEquals(Type.GROUP, acl.type());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testUserEntity() {
    User acl = new User("u1");
    assertEquals("u1", acl.getEmail());
    assertEquals(Type.USER, acl.getType());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testUserEntityDeprecated() {
    User acl = new User("u1");
    assertEquals("u1", acl.email());
    assertEquals(Type.USER, acl.type());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testProjectEntity() {
    Project acl = new Project(ProjectRole.VIEWERS, "p1");
    assertEquals(ProjectRole.VIEWERS, acl.getProjectRole());
    assertEquals("p1", acl.getProjectId());
    assertEquals(Type.PROJECT, acl.getType());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testProjectEntityDeprecated() {
    Project acl = new Project(ProjectRole.VIEWERS, "p1");
    assertEquals(ProjectRole.VIEWERS, acl.projectRole());
    assertEquals("p1", acl.projectId());
    assertEquals(Type.PROJECT, acl.type());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testRawEntity() {
    Entity acl = new RawEntity("bla");
    assertEquals("bla", acl.getValue());
    assertEquals(Type.UNKNOWN, acl.getType());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testRawEntityDeprecated() {
    Entity acl = new RawEntity("bla");
    assertEquals("bla", acl.value());
    assertEquals(Type.UNKNOWN, acl.type());
    String pb = acl.toPb();
    assertEquals(acl, Entity.fromPb(pb));
  }

  @Test
  public void testOf() {
    Acl acl = Acl.of(User.ofAllUsers(), Role.READER);
    assertEquals(User.ofAllUsers(), acl.getEntity());
    assertEquals(Role.READER, acl.getRole());
    ObjectAccessControl objectPb = acl.toObjectPb();
    assertEquals(acl, Acl.fromPb(objectPb));
    BucketAccessControl bucketPb = acl.toBucketPb();
    assertEquals(acl, Acl.fromPb(bucketPb));
  }
}
