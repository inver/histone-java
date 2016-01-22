package ru.histone.v2.evaluator;

import org.apache.commons.collections.CollectionUtils;
import ru.histone.HistoneException;
import ru.histone.v2.evaluator.node.*;
import ru.histone.v2.parser.node.*;
import ru.histone.v2.utils.ParserUtils;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.util.*;

/**
 * Created by alexey.nevinsky on 12.01.2016.
 */
public class Evaluator {

    public String process(String baseUri, ExpAstNode node, Context context) throws HistoneException {
        context.setBaseUri(baseUri);
        return processInternal(node, context);
    }

    private String processInternal(ExpAstNode node, Context context) throws HistoneException {
        StringBuilder sb = new StringBuilder();
        for (AstNode currentNode : node.getNodes()) {
            EvalNode evalNode = evaluateNode(currentNode, context);
            if (evalNode instanceof NullEvalNode) {
                sb.append("null");
            } else if (evalNode.getValue() != null) {
                sb.append(evalNode.asString());
            }
        }
        return sb.toString();
    }

    private EvalNode evaluateNode(AstNode node, Context context) throws HistoneException {
        if (node == null) {
            return EmptyEvalNode.INSTANCE;
        }

        if (node.hasValue()) {
            return getValueNode(node);
        }

        ExpAstNode expNode = (ExpAstNode) node;
        switch (node.getType()) {
            case AST_ARRAY:
                return processArrayNode(expNode, context);
            case AST_REGEXP:
                break;
            case AST_THIS:
                break;
            case AST_GLOBAL:
                break;
            case AST_NOT:
                break;
            case AST_AND:
                return processAndNode(expNode, context);
            case AST_OR:
                return processOrNode(expNode, context);
            case AST_TERNARY:
                break;
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
                break;
            case AST_GT:
                return processGreaterThan(expNode, context);
            case AST_LE:
                return processLessOrEquals(expNode, context);
            case AST_GE:
                break;
            case AST_EQ:
                return processEqNode(expNode, context);
            case AST_NEQ:
                return processEqNode(expNode, context).neg();
            case AST_REF:
                return processReferenceNode(expNode, context);
            case AST_METHOD:
                break;
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
                break;
            case AST_RETURN:
                break;
            case AST_NODES:
                return processNodeList(expNode, context.createNew());
            case AST_NODELIST:
                return processNodeList(expNode, context);
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

    private EvalNode processPropertyNode(ExpAstNode expNode, Context context) throws HistoneException {
        EvalNode value = evaluateNode(expNode.getNode(0), context);
        EvalNode propertyName = evaluateNode(expNode.getNode(1), context);

        Object obj = ((MapEvalNode) value).getProperty((String) propertyName.getValue());
        return EvalUtils.createEvalNode(obj);
    }

    private EvalNode processCall(ExpAstNode expNode, Context context) throws HistoneException {
        String functionName = ((StringAstNode) ((ExpAstNode) expNode.getNode(0)).getNode(0)).getValue();
        List<AstNode> values = expNode.getNodes().subList(1, expNode.getNodes().size());

        GlobalFunction globalFunction = context.getGlobalFunctions().get(functionName);
        if (globalFunction == null) {
            throw new HistoneException("wrong global function " + functionName);
        }
        EvalNode res = globalFunction.execute(values);

        return res;
    }

    private EvalNode processForNode(ExpAstNode expNode, Context context) throws HistoneException {
        EvalNode objToIterate = evaluateNode(expNode.getNode(2), context);
        if (!(objToIterate instanceof MapEvalNode)) {
            if (expNode.size() == 4) {
                return EmptyEvalNode.INSTANCE;
            }
            int i = 0;
            ExpAstNode expressionNode = expNode.getNode(i + 4);
            ExpAstNode bodyNode = expNode.getNode(i + 5);
            while (bodyNode != null) {
                EvalNode conditionNode = evaluateNode(expressionNode, context);
                if (EvalUtils.nodeAsBoolean(conditionNode)) {
                    String res = processInternal(bodyNode, context);
                    return new StringEvalNode(res);
                }
                i++;
                expressionNode = expNode.getNode(i + 4);
                bodyNode = expNode.getNode(i + 5);
            }
            if (expressionNode != null) {
                String res = processInternal(expressionNode, context);
                return new StringEvalNode(res);
            }

            return EmptyEvalNode.INSTANCE;
        }

        EvalNode keyVarName = evaluateNode(expNode.getNode(0), context);
        EvalNode valueVarName = evaluateNode(expNode.getNode(1), context);

        Context iterableContext;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Object> entry : ((MapEvalNode) objToIterate).getValue().entrySet()) {
            iterableContext = context.createNew();
            iterableContext.put((String) valueVarName.getValue(), entry.getValue());
            if (keyVarName != NullEvalNode.INSTANCE) {
                iterableContext.getVars().putIfAbsent((String) keyVarName.getValue(), entry.getKey());
            }
            iterableContext.put("self", constructSelfValue(i, ((MapEvalNode) objToIterate).getValue().entrySet().size() - 1));
            String res = processInternal(expNode.getNode(3), iterableContext);
            sb.append(res);
            i++;
        }

        return new StringEvalNode(sb.toString());
    }

    private Map<String, Integer> constructSelfValue(int currentIndex, int lastIndex) {
        Map<String, Integer> res = new HashMap<>();
        res.put("index", currentIndex);
        res.put("last", lastIndex);
        return res;
    }

    private EvalNode processAddNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode left = evaluateNode(node.getNode(0), context);
        EvalNode right = evaluateNode(node.getNode(1), context);

        if (!(left instanceof StringEvalNode || right instanceof StringEvalNode)) {
            if (EvalUtils.isNumberNode(left) && EvalUtils.isNumberNode(right)) {
                Float res = getValue(left) + getValue(right);
                if (res % 1 == 0 && res <= Integer.MAX_VALUE) {
                    return new LongEvalNode(res.longValue());
                } else {
                    return new FloatEvalNode(res);
                }
            }

            if (left instanceof MapEvalNode && right instanceof MapEvalNode) {
                throw new org.apache.commons.lang.NotImplementedException();
            }
        }

        return new StringEvalNode(left.asString() + right.asString());
    }

    private EvalNode processArithmetical(ExpAstNode node, Context context) throws HistoneException {
        EvalNode left = evaluateNode(node.getNode(0), context);
        EvalNode right = evaluateNode(node.getNode(1), context);

        if ((EvalUtils.isNumberNode(left) || left instanceof StringEvalNode) &&
                (EvalUtils.isNumberNode(right) || right instanceof StringEvalNode)) {
            Float leftValue = getValue(left);
            Float rightValue = getValue(right);
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
            if (res % 1 == 0 && res <= Integer.MAX_VALUE) {
                return new LongEvalNode(res.longValue());
            } else {
                return new FloatEvalNode(res);
            }
        }
        return EmptyEvalNode.INSTANCE;
    }

    private Float getValue(EvalNode node) {
        if (node instanceof StringEvalNode) {
            return ParserUtils.isFloat(((StringEvalNode) node).getValue());
        } else {
            return Float.valueOf(node.getValue() + "");
        }
    }

    private EvalNode processGreaterThan(ExpAstNode node, Context context) throws HistoneException {
        EvalNode left = evaluateNode(node.getNode(0), context);
        EvalNode right = evaluateNode(node.getNode(1), context);

        return null;
    }

    private EvalNode processBorNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode conditionNode1 = evaluateNode(node.getNode(0), context);
        EvalNode conditionNode2 = evaluateNode(node.getNode(1), context);
        return new BooleanEvalNode(EvalUtils.nodeAsBoolean(conditionNode1) | EvalUtils.nodeAsBoolean(conditionNode2));
    }

    private EvalNode processBxorNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode conditionNode1 = evaluateNode(node.getNode(0), context);
        EvalNode conditionNode2 = evaluateNode(node.getNode(1), context);
        return new BooleanEvalNode(EvalUtils.nodeAsBoolean(conditionNode1) ^ EvalUtils.nodeAsBoolean(conditionNode2));
    }

    private EvalNode processBandNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode conditionNode1 = evaluateNode(node.getNode(0), context);
        EvalNode conditionNode2 = evaluateNode(node.getNode(1), context);
        return new BooleanEvalNode(EvalUtils.nodeAsBoolean(conditionNode1) & EvalUtils.nodeAsBoolean(conditionNode2));
    }

    private EvalNode processVarNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode valueNode = evaluateNode(node.getNode(1), context);
        EvalNode valueName = evaluateNode(node.getNode(0), context);
        if (valueNode.getValue() != null) {
            context.getVars().putIfAbsent(valueName.getValue() + "", valueNode.getValue());
        }
        return EmptyEvalNode.INSTANCE;
    }

    private EvalNode processArrayNode(ExpAstNode node, Context context) throws HistoneException {
        if (CollectionUtils.isEmpty(node.getNodes())) {
            return new ObjectEvalNode(Collections.emptyMap());
        }
        if (node.getNode(0).getType() == AstType.AST_VAR) {
            for (AstNode astNode : node.getNodes()) {
                evaluateNode(astNode, context);
            }
            return EmptyEvalNode.INSTANCE;
        } else {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < node.getNodes().size() / 2; i++) {
                EvalNode key = evaluateNode(node.getNodes().get(i * 2), context);
                EvalNode value = evaluateNode(node.getNodes().get(i * 2 + 1), context);
                map.put(key.getValue() + "", value.getValue());
            }
            return new ObjectEvalNode(map);
        }
    }

    private EvalNode processUnaryMinus(ExpAstNode node, Context context) throws HistoneException {
        EvalNode res = evaluateNode(node.getNode(0), context);
        if (res instanceof LongEvalNode) {
            Long value = ((LongEvalNode) res).getValue();
            return new LongEvalNode(-value);
        } else {
            throw new NotImplementedException();
        }
    }

    private EvalNode processLessOrEquals(ExpAstNode node, Context context) {
        throw new NotImplementedException();
    }

    private EvalNode<? extends Serializable> getValueNode(AstNode node) {
        ValueNode valueNode = (ValueNode) node;
        if (valueNode.getValue() == null) {
            return NullEvalNode.INSTANCE;
        }

        Object val = valueNode.getValue();
        if (val == null) {
            return NullEvalNode.INSTANCE;
        } else if (val instanceof Boolean) {
            return new BooleanEvalNode((Boolean) val);
        } else if (val instanceof Long) {
            return new LongEvalNode((Long) val);
        }
        return new StringEvalNode(val + "");
    }

    private EvalNode processReferenceNode(ExpAstNode node, Context context) {
        StringAstNode valueNode = node.getNode(0);
        Object value = getValueFromParentContext(context, valueNode.getValue());
        if (value != null) {
            return EvalUtils.createEvalNode(value);
        } else {
            return EmptyEvalNode.INSTANCE;
        }
    }

    private Object getValueFromParentContext(Context context, String valueName) {
        while (context != null) {
            if (context.getVars().containsKey(valueName)) {
                return context.getVars().get(valueName);
            }
            context = context.getParent();
        }
        return null;
    }

    private EvalNode processOrNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode conditionNode1 = evaluateNode(node.getNode(0), context);
        EvalNode conditionNode2 = evaluateNode(node.getNode(1), context);
        return new BooleanEvalNode(EvalUtils.nodeAsBoolean(conditionNode1) || EvalUtils.nodeAsBoolean(conditionNode2));
    }

    private EvalNode processAndNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode conditionNode1 = evaluateNode(node.getNode(0), context);
        EvalNode conditionNode2 = evaluateNode(node.getNode(1), context);
        return new BooleanEvalNode(EvalUtils.nodeAsBoolean(conditionNode1) && EvalUtils.nodeAsBoolean(conditionNode2));
    }

    private EvalNode processNodeList(ExpAstNode node, Context context) throws HistoneException {
        //todo rework this method, add rtti and other cool features
        if (node.getNodes().size() == 1) {
            AstNode node1 = node.getNode(0);
            return evaluateNode(node1, context);
        } else {
            StringBuilder sb = new StringBuilder();
            for (AstNode currNode : node.getNodes()) {
                EvalNode processed = evaluateNode(currNode, context);
                sb.append(processed.getValue());
            }
            return new StringEvalNode(sb.toString());
        }
    }

    private BooleanEvalNode processEqNode(ExpAstNode node, Context context) throws HistoneException {
        EvalNode left = evaluateNode(node.getNode(0), context);
        EvalNode right = evaluateNode(node.getNode(1), context);
        return new BooleanEvalNode(EvalUtils.equalityNode(left, right));
    }

    private EvalNode processIfNode(ExpAstNode node, Context context) throws HistoneException {
        Context current = context.createNew();
        EvalNode conditionNode = evaluateNode(node.getNode(1), context);
        EvalNode result;
        if (EvalUtils.nodeAsBoolean(conditionNode)) {
            result = evaluateNode(node.getNode(0), current);
        } else {
            result = evaluateNode(node.getNode(2), current);
        }
        current.setParent(null);
        return result;
    }

}
