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

package ru.histone.v2.evaluator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import ru.histone.HistoneException;
import ru.histone.v2.Constants;
import ru.histone.v2.evaluator.data.HistoneMacro;
import ru.histone.v2.evaluator.data.HistoneRegex;
import ru.histone.v2.evaluator.global.NumberComparator;
import ru.histone.v2.evaluator.node.*;
import ru.histone.v2.parser.node.*;
import ru.histone.v2.utils.ParserUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.histone.v2.Constants.*;
import static ru.histone.v2.evaluator.EvalUtils.*;
import static ru.histone.v2.utils.AsyncUtils.sequence;

/**
 * Created by alexey.nevinsky on 12.01.2016.
 */
public class Evaluator {
    private static final Comparator<Number> NUMBER_COMPARATOR = new NumberComparator();
    private static final String TO_STRING_FUNC_NAME = "toString";

    public String process(String baseUri, ExpAstNode node, Context context) {
        context.setBaseUri(baseUri);
        return processInternal(node, context);
    }

    private String processInternal(ExpAstNode node, Context context) {
        EvalNode res = evaluateNode(node, context).join();
        return ((StringEvalNode) context.call(res, TO_STRING_FUNC_NAME, Collections.singletonList(res)).join()).getValue();
    }

    public CompletableFuture<EvalNode> evaluateNode(AstNode node, Context context) {
        if (node == null) {
            return CompletableFuture.completedFuture(EmptyEvalNode.INSTANCE);
        }

        if (node.hasValue()) {
            return getValueNode(node);
        }

        ExpAstNode expNode = (ExpAstNode) node;
        switch (node.getType()) {
            case AST_ARRAY:
                return processArrayNode(expNode, context);
            case AST_REGEXP:
                return processRegExp(expNode);
            case AST_THIS:
                return processThisNode(expNode, context);
            case AST_GLOBAL:
                return processGlobalNode(expNode, context);
            case AST_NOT:
                return processNotNode(expNode, context);
            case AST_AND:
                return processAndNode(expNode, context);
            case AST_OR:
                return processOrNode(expNode, context);
            case AST_TERNARY:
                return processTernary(expNode, context);
            case AST_ADD:
                return processAddNode(expNode, context);
            case AST_SUB:
            case AST_MUL:
            case AST_DIV:
            case AST_MOD:
                return processArithmetical(expNode, context);
            case AST_USUB:
                return processUnaryMinus(expNode, context);
            case AST_LT:
            case AST_GT:
            case AST_LE:
            case AST_GE:
                return processRelation(expNode, context);
            case AST_EQ:
                return processEqNode(expNode, context, true);
            case AST_NEQ:
                return processEqNode(expNode, context, false);
            case AST_REF:
                return processReferenceNode(expNode, context);
            case AST_METHOD:
                return processMethod(expNode, context);
            case AST_PROP:
                return processPropertyNode(expNode, context);
            case AST_CALL:
                return processCall(expNode, context);
            case AST_VAR:
                return processVarNode(expNode, context);
            case AST_IF:
                return processIfNode(expNode, context);
            case AST_FOR:
                return processForNode(expNode, context);
            case AST_MACRO:
                return processMacroNode(expNode, context);
            case AST_RETURN:
                return processReturnNode(expNode, context);
            case AST_NODES:
                return processNodeList(expNode, context, true);
            case AST_NODELIST:
                return processNodeList(expNode, context, false);
            case AST_BOR:
                return processBorNode(expNode, context);
            case AST_BXOR:
                return processBxorNode(expNode, context);
            case AST_BAND:
                return processBandNode(expNode, context);
            case AST_SUPRESS:
                break;
            case AST_LISTEN:
                break;
            case AST_TRIGGER:
                break;
        }
        throw new HistoneException("WTF!?!?!? " + node.getType());

    }

    private CompletableFuture<EvalNode> processThisNode(ExpAstNode expNode, Context context) {
        return context.getValue(Constants.THIS_CONTEXT_VALUE);
    }

    private CompletableFuture<EvalNode> processReturnNode(ExpAstNode expNode, Context context) {
        return evaluateNode(expNode.getNode(0), context).thenApply(r -> {
            r.setIsReturn();
            return r;
        });
    }

    private CompletableFuture<EvalNode> processGlobalNode(ExpAstNode expNode, Context context) {
        return CompletableFuture.completedFuture(new GlobalEvalNode());
    }

    private CompletableFuture<EvalNode> processNotNode(ExpAstNode expNode, Context context) {
        CompletableFuture<EvalNode> nodeFuture = evaluateNode(expNode.getNode(0), context);
        return nodeFuture.thenApply(n -> new BooleanEvalNode(!nodeAsBoolean(n)));
    }

    /**
     * [AST_ID, MACRO_BODY, NUM_OF_VARS, VARS...]
     */
    private CompletableFuture<EvalNode> processMacroNode(ExpAstNode node, Context context) {
        final int bodyIndex = 0;
        final int startVarIndex = 2;
        final CompletableFuture<List<AstNode>> astArgsFuture = CompletableFuture.completedFuture(
                node.size() < startVarIndex
                        ? Collections.<AstNode>emptyList()
                        : node.getNodes().subList(startVarIndex, node.size())
        );
        final CompletableFuture<List<String>> argsFuture = astArgsFuture.thenApply(astNodes ->
                astNodes.stream()
                        .map(x -> {
                            final StringAstNode nameNode = ((ExpAstNode) x).getNode(0);
                            return nameNode.getValue();
                        })
                        .collect(Collectors.toList())
        );
        return argsFuture.thenApply(args -> {
            final AstNode body = node.getNode(bodyIndex);
            return new MacroEvalNode(new HistoneMacro(args, body, context, Evaluator.this));
        });
    }

    private CompletableFuture<EvalNode> processMethod(ExpAstNode expNode, Context context) {
        final int valueIndex = 0;
        final int methodIndex = 1;
        final int startArgsIndex = 2;
        final CompletableFuture<List<EvalNode>> nodesFuture = evalAllNodesOfCurrent(expNode, context);

        return nodesFuture.thenCompose(nodes -> {
            final EvalNode valueNode = nodes.get(valueIndex);
            final StringEvalNode methodNode = (StringEvalNode) nodes.get(methodIndex);
            final List<EvalNode> argsNode = new ArrayList<>();
            argsNode.add(valueNode);
            argsNode.addAll(nodes.subList(startArgsIndex, nodes.size()));

            return context.call(valueNode, methodNode.getValue(), argsNode);
        });
    }

    private CompletableFuture<EvalNode> processMethod(ExpAstNode expNode, Context context, List<EvalNode> args) {
        final int valueIndex = 0;
        final int methodIndex = 1;
        final CompletableFuture<List<EvalNode>> processNodes = sequence(Arrays.asList(
                evaluateNode(expNode.getNode(valueIndex), context),
                evaluateNode(expNode.getNode(methodIndex), context)
        ));
        return processNodes.thenCompose(methodNodes -> {
            final EvalNode valueNode = methodNodes.get(valueIndex);
            final StringEvalNode methodNode = (StringEvalNode) methodNodes.get(methodIndex);
            final List<EvalNode> argsNodes = new ArrayList<>();
            argsNodes.add(valueNode);
            argsNodes.addAll(args);

            return context.call(valueNode, methodNode.getValue(), argsNodes);
        });
    }

    private CompletableFuture<EvalNode> processTernary(ExpAstNode expNode, Context context) {
        CompletableFuture<EvalNode> condition = evaluateNode(expNode.getNode(0), context);
        return condition.thenCompose(conditionNode -> {
            if (nodeAsBoolean(conditionNode)) {
                return evaluateNode(expNode.getNode(1), context);
            } else if (expNode.getNode(2) != null) {
                return evaluateNode(expNode.getNode(2), context);
            }
            return CompletableFuture.completedFuture(EmptyEvalNode.INSTANCE);
        });
    }

    private CompletableFuture<EvalNode> processPropertyNode(ExpAstNode expNode, Context context) {
        return evalAllNodesOfCurrent(expNode, context)
                .thenApply(futures -> {
                    Object obj = ((MapEvalNode) futures.get(0)).getProperty((String) futures.get(1).getValue());
                    if (obj != null) {
                        return createEvalNode(obj);
                    }
                    return EmptyEvalNode.INSTANCE;
                });
    }

    private CompletableFuture<EvalNode> processCall(ExpAstNode expNode, Context context) {
        final ExpAstNode node = expNode.getNode(0);
        final boolean valueNodeExists = node.size() > 1;
        final List<AstNode> paramsAstNodes = expNode.getNodes().subList(1, expNode.getNodes().size());

        final CompletableFuture<EvalNode> functionNameFuture = evaluateNode(node.getNode(valueNodeExists ? 1 : 0), context);
        CompletableFuture<List<EvalNode>> argsFuture = sequence(paramsAstNodes.stream()
                .map(x -> evaluateNode(x, context))
                .collect(Collectors.toList()));
        return argsFuture.thenCompose(args -> functionNameFuture.thenCompose(functionNameNode -> {
            if (functionNameNode instanceof StringEvalNode && !valueNodeExists) {
                return context.call((String) functionNameNode.getValue(), args);
            } else {
                return processMethod(node, context, args);
            }
        }));
    }

    private CompletableFuture<EvalNode> processForNode(ExpAstNode expNode, Context context) {
        CompletableFuture<EvalNode> objToIterateFuture = evaluateNode(expNode.getNode(2), context);
        return objToIterateFuture.thenCompose(objToIterate -> {
            if (!(objToIterate instanceof MapEvalNode)) {
                return processNonMapValue(expNode, context);
            } else {
                return processMapValue(expNode, context, (MapEvalNode) objToIterate);
            }
        });
    }

    private CompletionStage<EvalNode> processNonMapValue(ExpAstNode expNode, Context context) {
        if (expNode.size() == 4) {
            return CompletableFuture.completedFuture(EmptyEvalNode.INSTANCE);
        }
        int i = 0;
        ExpAstNode expressionNode = expNode.getNode(i + 4);
        ExpAstNode bodyNode = expNode.getNode(i + 5);
        while (bodyNode != null) {
            CompletableFuture<EvalNode> conditionFuture = evaluateNode(expressionNode, context);
            EvalNode conditionNode = conditionFuture.join();
            if (nodeAsBoolean(conditionNode)) {
                return evaluateNode(bodyNode, context);
            }
            i++;
            expressionNode = expNode.getNode(i + 4);
            bodyNode = expNode.getNode(i + 5);
        }
        if (expressionNode != null) {
            return evaluateNode(expressionNode, context);
        }

        return CompletableFuture.completedFuture(EmptyEvalNode.INSTANCE);
    }

    private CompletableFuture<EvalNode> processMapValue(ExpAstNode expNode, Context context, MapEvalNode objToIterate) {
        CompletableFuture<EvalNode> keyVarName = evaluateNode(expNode.getNode(0), context);
        CompletableFuture<EvalNode> valueVarName = evaluateNode(expNode.getNode(1), context);

        CompletableFuture<List<EvalNode>> leftRightDone = sequence(keyVarName, valueVarName);
        return leftRightDone.thenCompose(keyValueNames -> {
            CompletableFuture<List<EvalNode>> res = iterate(expNode, context, objToIterate, keyValueNames.get(0), keyValueNames.get(1));
            return getEvaluatedString(context, res);
        });
    }

    private CompletableFuture<EvalNode> getEvaluatedString(Context context, CompletableFuture<List<EvalNode>> res) {
        return res.thenApply(nodes -> {
            Optional<EvalNode> rNode = nodes.stream().filter(EvalNode::isReturn).findFirst();
            List<EvalNode> nodesToProcess = nodes;
            if (rNode.isPresent()) {
                nodesToProcess = Collections.singletonList(rNode.get());
            }
            EvalNode result = new StringEvalNode(
                    nodesToProcess.stream()
                            .map(n -> context.call(n, TO_STRING_FUNC_NAME, Collections.singletonList(n)))
                            .map(CompletableFuture::join)
                            .map(n -> n.getValue() + "")
                            .collect(Collectors.joining())
            );
            if (rNode.isPresent()) {
                result.setIsReturn();
            }
            return result;

        });
    }

    private CompletableFuture<List<EvalNode>> iterate(ExpAstNode expNode, Context context, MapEvalNode
            objToIterate, EvalNode keyVarName, EvalNode valueVarName) {
        List<CompletableFuture<EvalNode>> futures = new ArrayList<>(objToIterate.getValue().size());
        int i = 0;
        for (Map.Entry<String, Object> entry : objToIterate.getValue().entrySet()) {
            Context iterableContext = getIterableContext(context, objToIterate, keyVarName, valueVarName, i, entry);
            futures.add(evaluateNode(expNode.getNode(3), iterableContext));
            i++;
        }
        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[futures.size()]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }

    private Context getIterableContext(Context context, MapEvalNode objToIterate, EvalNode keyVarName, EvalNode valueVarName, int i, Map.Entry<String, Object> entry) {
        Context iterableContext = context.createNew();
        if (valueVarName != NullEvalNode.INSTANCE) {
            iterableContext.put(valueVarName.getValue() + "", EvalUtils.getValue(entry.getValue()));
        }
        if (keyVarName != NullEvalNode.INSTANCE) {
            iterableContext.put(keyVarName.getValue() + "", EvalUtils.getValue(entry.getKey()));
        }
        iterableContext.put(SELF_CONTEXT_NAME, EvalUtils.getValue(constructSelfValue(
                entry.getKey(), entry.getValue(), i, objToIterate.getValue().entrySet().size() - 1
        )));
        return iterableContext;
    }

    private Map<String, Object> constructSelfValue(String key, Object value, long currentIndex, long lastIndex) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put(SELF_CONTEXT_KEY, key);
        res.put(SELF_CONTEXT_VALUE, value);
        res.put(SELF_CONTEXT_CURRENT_INDEX, currentIndex);
        res.put(SELF_CONTEXT_LAST_INDEX, lastIndex);
        return res;
    }

    private CompletableFuture<EvalNode> processAddNode(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRight = evalAllNodesOfCurrent(node, context);
        return leftRight.thenCompose(lr -> {
            EvalNode left = lr.get(0);
            EvalNode right = lr.get(1);
            if (!(left instanceof StringEvalNode || right instanceof StringEvalNode)) {
                if (isNumberNode(left) && isNumberNode(right)) {
                    Float res = getValue(left).orElse(null) + getValue(right).orElse(null);
                    if (res % 1 == 0 && res <= Long.MAX_VALUE) {
                        return EvalUtils.getValue(res.longValue());
                    } else {
                        return EvalUtils.getValue(res);
                    }
                }

                if (left instanceof MapEvalNode && right instanceof MapEvalNode) {
                    throw new NotImplementedException();
                }
            }

            CompletableFuture<List<EvalNode>> lrFutures = sequence(
                    context.call(TO_STRING_FUNC_NAME, Collections.singletonList(left)),
                    context.call(TO_STRING_FUNC_NAME, Collections.singletonList(right))
            );
            return lrFutures.thenCompose(futures -> {
                StringEvalNode l = (StringEvalNode) futures.get(0);
                StringEvalNode r = (StringEvalNode) futures.get(1);
                return EvalUtils.getValue(l.getValue() + r.getValue());
            });
        });
    }

    private CompletableFuture<EvalNode> processArithmetical(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(futures -> {
            EvalNode left = futures.get(0);
            EvalNode right = futures.get(1);

            if ((isNumberNode(left) || left instanceof StringEvalNode) &&
                    (isNumberNode(right) || right instanceof StringEvalNode)) {
                Float leftValue = getValue(left).orElse(null);
                Float rightValue = getValue(right).orElse(null);
                if (leftValue == null || rightValue == null) {
                    return EmptyEvalNode.INSTANCE;
                }

                Float res;
                AstType type = node.getType();
                if (type == AstType.AST_SUB) {
                    res = leftValue - rightValue;
                } else if (type == AstType.AST_MUL) {
                    res = leftValue * rightValue;
                } else if (type == AstType.AST_DIV) {
                    res = leftValue / rightValue;
                } else {
                    res = leftValue % rightValue;
                }
                if (res % 1 == 0 && res <= Long.MAX_VALUE) {
                    return new LongEvalNode(res.longValue());
                } else {
                    return new FloatEvalNode(res);
                }
            }
            return EmptyEvalNode.INSTANCE;
        });
    }

    private Optional<Float> getValue(EvalNode node) {
        if (node instanceof StringEvalNode) {
            return ParserUtils.tryFloat(((StringEvalNode) node).getValue());
        } else {
            return Optional.of(Float.valueOf(node.getValue() + ""));
        }
    }

    private CompletableFuture<EvalNode> processRelation(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(f -> {
            EvalNode left = f.get(0);
            EvalNode right = f.get(1);

            final Integer compareResult;
            if (left instanceof StringEvalNode && isNumberNode(right)) {
                final Number rightValue = getNumberValue(right);
                final StringEvalNode stringLeft = (StringEvalNode) left;
                if (isNumeric(stringLeft)) {
                    final Number leftValue = getNumberValue(stringLeft);
                    compareResult = NUMBER_COMPARATOR.compare(leftValue, rightValue);
                } else {
                    throw new NotImplementedException(); // TODO call RTTI toString right
                }
            } else if (isNumberNode(left) && right instanceof StringEvalNode) {
                final StringEvalNode stringRight = (StringEvalNode) right;
                if (isNumeric(stringRight)) {
                    final Number rightValue = getNumberValue(right);
                    final Number leftValue = getNumberValue(left);
                    compareResult = NUMBER_COMPARATOR.compare(leftValue, rightValue);
                } else {
                    throw new NotImplementedException(); // TODO call RTTI toString left
                }
            } else if (!isNumberNode(left) || !isNumberNode(right)) {
                if (left instanceof StringEvalNode && right instanceof StringEvalNode) {
                    final StringEvalNode stringRight = (StringEvalNode) right;
                    final StringEvalNode stringLeft = (StringEvalNode) left;
                    final long leftLength = stringLeft.getValue().length();
                    final long rightLength = stringRight.getValue().length();
                    compareResult = Long.valueOf(leftLength).compareTo(rightLength);
                } else {
                    throw new NotImplementedException(); // TODO call RTTI toBoolean for both nodes
                }
            } else {
                final Number rightValue = getNumberValue(right);
                final Number leftValue = getNumberValue(left);
                compareResult = NUMBER_COMPARATOR.compare(leftValue, rightValue);
            }

            return processRelationHelper(node.getType(), compareResult);
        });
    }

    private EvalNode processRelationHelper(AstType astType, int compareResult) {
        switch (astType) {
            case AST_LT:
                return new BooleanEvalNode(compareResult < 0);
            case AST_GT:
                return new BooleanEvalNode(compareResult > 0);
            case AST_LE:
                return new BooleanEvalNode(compareResult <= 0);
            case AST_GE:
                return new BooleanEvalNode(compareResult >= 0);
        }
        throw new RuntimeException("Unknown type for this case");
    }

    private CompletableFuture<EvalNode> processBorNode(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(f -> new BooleanEvalNode(nodeAsBoolean(f.get(0)) | nodeAsBoolean(f.get(1))));
    }

    private CompletableFuture<EvalNode> processBxorNode(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(f -> new BooleanEvalNode(nodeAsBoolean(f.get(0)) ^ nodeAsBoolean(f.get(1))));
    }

    private CompletableFuture<EvalNode> processBandNode(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(f -> new BooleanEvalNode(nodeAsBoolean(f.get(0)) & nodeAsBoolean(f.get(1))));
    }

    private CompletableFuture<EvalNode> processVarNode(ExpAstNode node, Context context) {
        CompletableFuture<EvalNode> valueNameFuture = evaluateNode(node.getNode(1), context);
        CompletableFuture<EvalNode> valueNodeFuture = evaluateNode(node.getNode(0), context);

        CompletableFuture<List<EvalNode>> leftRightDone = sequence(valueNameFuture);
        return leftRightDone.thenApply(f -> {
            context.put(f.get(0).getValue() + "", valueNodeFuture);
            return EmptyEvalNode.INSTANCE;
        });
    }

    private CompletableFuture<EvalNode> processArrayNode(ExpAstNode node, Context context) {
        if (CollectionUtils.isEmpty(node.getNodes())) {
            return CompletableFuture.completedFuture(new MapEvalNode(Collections.emptyMap()));
        }
        if (node.getNode(0).getType() == AstType.AST_VAR) {
            return evalAllNodesOfCurrent(node, context).thenApply(evalNodes -> EmptyEvalNode.INSTANCE);
        } else {
            if (node.size() > 0) {
                CompletableFuture<List<EvalNode>> futures = evalAllNodesOfCurrent(node, context);
                return futures.thenApply(nodes -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (int i = 0; i < nodes.size() / 2; i++) {
                        EvalNode key = nodes.get(i * 2);
                        EvalNode value = nodes.get(i * 2 + 1);
                        map.put(key.getValue() + "", value.getValue());
                    }
                    return new MapEvalNode(map);
                });
            } else {
                return CompletableFuture.completedFuture(new MapEvalNode(new LinkedHashMap<>()));
            }
        }
    }

    private CompletableFuture<EvalNode> processUnaryMinus(ExpAstNode node, Context context) {
        CompletableFuture<EvalNode> res = evaluateNode(node.getNode(0), context);
        return res.thenApply(n -> {
            if (n instanceof LongEvalNode) {
                Long value = ((LongEvalNode) n).getValue();
                return new LongEvalNode(-value);
            } else if (n instanceof FloatEvalNode) {
                Float value = ((FloatEvalNode) n).getValue();
                return new FloatEvalNode(-value);
            }
            throw new NotImplementedException();
        });
    }

    private CompletableFuture<EvalNode> getValueNode(AstNode node) {
        ValueNode valueNode = (ValueNode) node;
        if (valueNode.getValue() == null) {
            return CompletableFuture.completedFuture(NullEvalNode.INSTANCE);
        }

        Object val = valueNode.getValue();
        if (val instanceof Boolean) {
            return CompletableFuture.completedFuture(new BooleanEvalNode((Boolean) val));
        } else if (val instanceof Long) {
            return CompletableFuture.completedFuture(new LongEvalNode((Long) val));
        } else if (val instanceof Float) {
            return CompletableFuture.completedFuture(new FloatEvalNode((Float) val));
        }
        return CompletableFuture.completedFuture(new StringEvalNode(val + ""));
    }

    private CompletableFuture<EvalNode> processReferenceNode(ExpAstNode node, Context context) {
        StringAstNode valueNode = node.getNode(0);
        CompletableFuture<EvalNode> value = getValueFromParentContext(context, valueNode.getValue());
        return value.thenCompose(v -> {
            if (v != null) {
                return CompletableFuture.completedFuture(v);
            } else {
                return CompletableFuture.completedFuture(EmptyEvalNode.INSTANCE);
            }
        });
    }

    private CompletableFuture<EvalNode> getValueFromParentContext(Context context, String valueName) {
        while (context != null) {
            if (context.contains(valueName)) {
                return context.getValue(valueName);
            }
            context = context.getParent();
        }
        return CompletableFuture.completedFuture(EmptyEvalNode.INSTANCE);
    }

    private CompletableFuture<EvalNode> processOrNode(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(f -> new BooleanEvalNode(nodeAsBoolean(f.get(0)) || nodeAsBoolean(f.get(1))));
    }

    private CompletableFuture<EvalNode> processAndNode(ExpAstNode node, Context context) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);
        return leftRightDone.thenApply(f -> new BooleanEvalNode(nodeAsBoolean(f.get(0)) && nodeAsBoolean(f.get(1))));
    }

    private CompletableFuture<EvalNode> processNodeList(ExpAstNode node, Context context, boolean createContext) {
        final Context ctx = createContext ? context.createNew() : context;
        if (node.getNodes().size() == 1) {
            AstNode node1 = node.getNode(0);
            CompletableFuture<EvalNode> res = evaluateNode(node1, ctx);
            ctx.release();
            return res;
        } else {
            CompletableFuture<List<EvalNode>> f = evalAllNodesOfCurrent(node, ctx);
            return getEvaluatedString(context, f);
        }
    }

    private CompletableFuture<List<EvalNode>> evalAllNodesOfCurrent(ExpAstNode node, Context context) {
        final List<CompletableFuture<EvalNode>> futures = node.getNodes()
                .stream()
                .map(currNode -> evaluateNode(currNode, context))
                .collect(Collectors.toList());
        return sequence(futures);
    }

    private CompletableFuture<EvalNode> processEqNode(ExpAstNode node, Context context, boolean isEquals) {
        CompletableFuture<List<EvalNode>> leftRightDone = evalAllNodesOfCurrent(node, context);

        return leftRightDone.thenApply(f -> {
            EvalNode left = f.get(0);
            EvalNode right = f.get(1);

            return new BooleanEvalNode(isEquals == equalityNode(left, right));
        });
    }

    private CompletableFuture<EvalNode> processIfNode(ExpAstNode node, Context context) {
        CompletableFuture<EvalNode> conditionFuture = evaluateNode(node.getNode(1), context);

        return conditionFuture
                .thenCompose(condNode -> {
                    Context current = context.createNew();
                    CompletableFuture<EvalNode> result;
                    if (nodeAsBoolean(condNode)) {
                        result = evaluateNode(node.getNode(0), current);
                    } else {
                        result = evaluateNode(node.getNode(2), current);
                    }
                    current.release();
                    return result;
                });
    }

    private CompletableFuture<EvalNode> processRegExp(ExpAstNode node) {
        return CompletableFuture.supplyAsync(() -> {
            final LongAstNode flagsNumNode = node.getNode(1);
            final long flagsNum = flagsNumNode.getValue();

            int flags = 0;
            if ((flagsNum & AstRegexType.RE_IGNORECASE.getId()) != 0) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            if ((flagsNum & AstRegexType.RE_MULTILINE.getId()) != 0) {
                flags |= Pattern.MULTILINE;
            }

            final boolean isGlobal = (flagsNum & AstRegexType.RE_GLOBAL.getId()) != 0;
            final StringAstNode expNode = node.getNode(0);
            final String exp = expNode.getValue();
            final Pattern pattern = Pattern.compile(exp, flags);
            return new RegexEvalNode(new HistoneRegex(isGlobal, pattern));
        });
    }

}
