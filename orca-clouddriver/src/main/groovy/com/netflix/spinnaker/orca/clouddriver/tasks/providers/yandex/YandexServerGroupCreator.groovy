/*
 * Copyright 2020 YANDEX LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.clouddriver.tasks.providers.yandex

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.orca.api.pipeline.models.StageExecution
import com.netflix.spinnaker.orca.clouddriver.tasks.servergroup.ServerGroupCreator
import com.netflix.spinnaker.orca.kato.tasks.DeploymentDetailsAware
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Slf4j
@Component
class YandexServerGroupCreator implements ServerGroupCreator, DeploymentDetailsAware {

  boolean katoResultExpected = false
  String cloudProvider = "yandex"

  @Autowired
  ObjectMapper objectMapper

  @Override
  List<Map> getOperations(StageExecution stage) {
    def operation = [:]

    // If this stage was synthesized by a parallel deploy stage, the operation properties will be under 'cluster'.
    if (stage.context.containsKey("cluster")) {
      operation.putAll(stage.context.cluster as Map)
    } else {
      operation.putAll(stage.context)
    }

    if (operation.account && !operation.credentials) {
      operation.credentials = operation.account
    }

    operation.instanceTemplate.bootDiskSpec.diskSpec.imageId = operation.instanceTemplate.bootDiskSpec.diskSpec.imageId ?: getImageId(stage)
    if (!operation.instanceTemplate.bootDiskSpec.diskSpec.imageId) {
      throw new IllegalStateException("No image could be found in ${stage.context.region}.")
    }
    return [[(OPERATION): operation]]
  }

  private String getImageId(StageExecution stage) {
    String imageId

    withImageFromPrecedingStage(stage, null, cloudProvider) {
      imageId = imageId ?: it.imageId
    }

    withImageFromDeploymentDetails(stage, null, cloudProvider) {
      imageId = imageId ?: it.imageId
    }

    return imageId
  }

  @Override
  Optional<String> getHealthProviderName() {
    return Optional.of("Yandex")
  }
}
