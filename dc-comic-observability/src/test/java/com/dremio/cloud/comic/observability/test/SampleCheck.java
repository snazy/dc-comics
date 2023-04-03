/*
 * Copyright (c) 2023 - Dremio - https://www.dremio.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.dremio.cloud.comic.observability.test;

import com.dremio.cloud.comic.api.scope.ApplicationScoped;
import com.dremio.cloud.comic.observability.health.HealthCheck;

import java.util.concurrent.CompletionStage;

import static com.dremio.cloud.comic.observability.health.HealthCheck.Status.OK;
import static java.util.concurrent.CompletableFuture.completedFuture;

@ApplicationScoped
public class SampleCheck implements HealthCheck {

    private CompletionStage<Result> check = completedFuture(new Result(OK, "worked"));

    public void setCheck(final CompletionStage<Result> check) {
        this.check = check;
    }

    @Override
    public String name() {
        return "test-check";
    }

    @Override
    public CompletionStage<Result> check() {
        return check;
    }


}
