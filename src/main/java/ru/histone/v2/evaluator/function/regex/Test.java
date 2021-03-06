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

package ru.histone.v2.evaluator.function.regex;

import ru.histone.v2.evaluator.EvalUtils;
import ru.histone.v2.evaluator.Function;
import ru.histone.v2.evaluator.data.HistoneRegex;
import ru.histone.v2.evaluator.node.EvalNode;
import ru.histone.v2.exceptions.FunctionExecutionException;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gali.alykoff on 26/01/16.
 */
public class Test implements Function, Serializable {
    public static final String NAME = "test";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompletableFuture<EvalNode> execute(String baseUri, List<EvalNode> args) throws FunctionExecutionException {
        try {
            final HistoneRegex regexHistone = (HistoneRegex) args.get(0).getValue();
            final EvalNode evalNode = args.get(1);
            final String exp = String.valueOf(evalNode.getValue());
            final Pattern pattern = regexHistone.getPattern();
//            final boolean isGlobal = regexHistone.isGlobal();

            final Matcher matcher = pattern.matcher(exp);
            final boolean result = matcher.find();

            return EvalUtils.getValue(result);
        } catch (Exception e) {
            throw new FunctionExecutionException(e);
        }
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isClear() {
        return true;
    }
}
