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

package ru.histone.v2.evaluator.function.macro;

import ru.histone.v2.evaluator.Context;
import ru.histone.v2.evaluator.EvalUtils;
import ru.histone.v2.evaluator.Evaluator;
import ru.histone.v2.evaluator.Function;
import ru.histone.v2.evaluator.data.HistoneMacro;
import ru.histone.v2.evaluator.node.EmptyEvalNode;
import ru.histone.v2.evaluator.node.EvalNode;
import ru.histone.v2.evaluator.node.MacroEvalNode;
import ru.histone.v2.evaluator.node.MapEvalNode;
import ru.histone.v2.exceptions.FunctionExecutionException;
import ru.histone.v2.parser.node.AstNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 *
 * Created by gali.alykoff on 28/01/16.
 */
public class MacroCall implements Function, Serializable {
    public final static String NAME = "call";
    public static final int MACRO_NODE_INDEX = 0;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CompletableFuture<EvalNode> execute(String baseUri, List<EvalNode> args) throws FunctionExecutionException {
        final MacroEvalNode macroNode = (MacroEvalNode) args.get(MACRO_NODE_INDEX);
        final HistoneMacro histoneMacro = macroNode.getValue();
        final AstNode body = histoneMacro.getBody();
        final List<String> namesOfVars = histoneMacro.getArgs();
        final Evaluator evaluator = histoneMacro.getEvaluator();
        final Context context = histoneMacro.getContext();

        final List<EvalNode> params = getParams(args);
        final Context currentContext = context.createNew();
        for (int i = 0; i < namesOfVars.size(); i++) {
            final String argName = namesOfVars.get(i);
            final EvalNode param = (i < params.size())
                ? params.get(i)
                : EmptyEvalNode.INSTANCE;
            currentContext.put(argName, CompletableFuture.completedFuture(param));
        }
        return evaluator.evaluateNode(body, currentContext);
    }

    private List<EvalNode> getParams(List<EvalNode> args) {
        final List<EvalNode> params = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            if (i == MACRO_NODE_INDEX) {
                continue;
            }

            final EvalNode rawNode = args.get(i);
            if (rawNode instanceof MapEvalNode) {
                final MapEvalNode node = (MapEvalNode) rawNode;
                final List<EvalNode> innerArgs = node.getValue()
                        .values()
                        .stream()
                        .map(EvalUtils::createEvalNode)
                        .collect(Collectors.toList());
                params.addAll(innerArgs);
            } else {
                params.add(rawNode);
            }
        }
        return params;
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
