/*
 * Copyright (c) 2016 MegaFon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.histone.v2.evaluator.function;

import ru.histone.v2.evaluator.Function;
import ru.histone.v2.evaluator.node.EvalNode;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Created by inv3r on 27/01/16.
 */
public abstract class AbstractFunction implements Function {
    protected final Executor executor;

    protected AbstractFunction() {
        executor = null;
    }

    protected AbstractFunction(Executor executor) {
        this.executor = executor;
    }

    protected <T> T getValue(List<EvalNode> args, int index) {
        return index > args.size() - 1 ? null : (T) args.get(index).getValue();
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isClear() {
        return false;
    }
}