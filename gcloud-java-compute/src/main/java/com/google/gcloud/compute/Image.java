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

import com.google.gcloud.compute.Compute.ImageOption;
import com.google.gcloud.compute.Compute.OperationOption;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Objects;

/**
 * A Google Compute Engine Image. An image contains a boot loader, an operating system and a root
 * file system that is necessary for starting an instance. Compute Engine offers publicly-available
 * images of certain operating systems that you can use, or you can create a custom image. A custom
 * image is an image created from one of your virtual machine instances that contains your specific
 * instance configurations. To get an {@code Image} object with the most recent information use
 * {@link #reload}. {@code Image} adds a layer of service-related functionality
 * over {@link ImageInfo}.
 *
 * @see <a href="https://cloud.google.com/compute/docs/images">Images</a>
 */
public class Image extends ImageInfo {

  private static final long serialVersionUID = 4623766590317494020L;

  private final ComputeOptions options;
  private transient Compute compute;

  /**
   * A builder for {@code Image} objects.
   */
  public static class Builder extends ImageInfo.Builder {

    private final Compute compute;
    private final ImageInfo.BuilderImpl infoBuilder;

    Builder(Compute compute, ImageId imageId, ImageConfiguration configuration) {
      this.compute = compute;
      this.infoBuilder = new ImageInfo.BuilderImpl();
      this.infoBuilder.imageId(imageId);
      this.infoBuilder.configuration(configuration);
    }

    Builder(Image image) {
      this.compute = image.compute;
      this.infoBuilder = new ImageInfo.BuilderImpl(image);
    }

    @Override
    Builder id(String id) {
      infoBuilder.id(id);
      return this;
    }

    @Override
    Builder creationTimestamp(Long creationTimestamp) {
      infoBuilder.creationTimestamp(creationTimestamp);
      return this;
    }

    @Override
    public Builder imageId(ImageId imageId) {
      infoBuilder.imageId(imageId);
      return this;
    }

    @Override
    public Builder description(String description) {
      infoBuilder.description(description);
      return this;
    }

    @Override
    public Builder configuration(ImageConfiguration configuration) {
      infoBuilder.configuration(configuration);
      return this;
    }

    @Override
    Builder status(Status status) {
      infoBuilder.status(status);
      return this;
    }

    @Override
    Builder diskSizeGb(Long diskSizeGb) {
      infoBuilder.diskSizeGb(diskSizeGb);
      return this;
    }

    @Override
    Builder licenses(List<LicenseId> licenses) {
      infoBuilder.licenses(licenses);
      return this;
    }

    @Override
    Builder deprecationStatus(DeprecationStatus<ImageId> deprecationStatus) {
      infoBuilder.deprecationStatus(deprecationStatus);
      return this;
    }

    @Override
    public Image build() {
      return new Image(compute, infoBuilder);
    }
  }

  Image(Compute compute, ImageInfo.BuilderImpl infoBuilder) {
    super(infoBuilder);
    this.compute = checkNotNull(compute);
    this.options = compute.options();
  }

  /**
   * Checks if this image exists.
   *
   * @return {@code true} if this image exists, {@code false} otherwise
   * @throws ComputeException upon failure
   */
  public boolean exists() {
    return reload(ImageOption.fields()) != null;
  }

  /**
   * Fetches current image' latest information. Returns {@code null} if the image does not exist.
   *
   * @param options image options
   * @return an {@code Image} object with latest information or {@code null} if not found
   * @throws ComputeException upon failure
   */
  public Image reload(ImageOption... options) {
    return compute.get(imageId(), options);
  }

  /**
   * Deletes this image.
   *
   * @return a global operation if the delete request was successfully sent, {@code null} if the
   *     image was not found
   * @throws ComputeException upon failure or if this image is a publicly-available image
   */
  public Operation delete(OperationOption... options) {
    return compute.delete(imageId(), options);
  }

  /**
   * Deprecates this image.
   *
   * @return a global operation if the deprecation request was successfully sent, {@code null} if
   *     the image was not found
   * @throws ComputeException upon failure or if this image is a publicly-available image
   */
  public Operation deprecate(DeprecationStatus<ImageId> deprecationStatus,
      OperationOption... options) {
    return compute.deprecate(imageId(), deprecationStatus, options);
  }

  /**
   * Returns the image's {@code Compute} object used to issue requests.
   */
  public Compute compute() {
    return compute;
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof Image
        && Objects.equals(toPb(), ((Image) obj).toPb())
        && Objects.equals(options, ((Image) obj).options);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(super.hashCode(), options);
  }

  private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    input.defaultReadObject();
    this.compute = options.service();
  }

  static Image fromPb(Compute compute, com.google.api.services.compute.model.Image imagePb) {
    return new Image(compute, new ImageInfo.BuilderImpl(imagePb));
  }
}
