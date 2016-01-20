package ru.histone.v2.utils;

import ru.histone.v2.parser.node.AstNode;
import ru.histone.v2.parser.node.ExpAstNode;
import ru.histone.v2.parser.node.StringAstNode;

/**
 * Created by alexey.nevinsky on 12.01.2016.
 */
public class ParserUtils {
    public static String astToString(ExpAstNode node) {
        StringBuffer sb = new StringBuffer();
        nodeToString(sb, node);
        return sb.toString();
    }

    private static void nodeToString(StringBuffer sb, ExpAstNode node) {
        Object nodeValue = node.getValue();
        if (node.getType() == ExpAstNode.LEAF_NODE_TYPE_ID) {
            if (node.getValue() instanceof Number) {
                sb.append(node.getValue());
            } else {
                sb.append("\"").append(node.getValue()).append("\"");
            }
        } else {
            sb.append("[").append(node.getType());
            if (nodeValue != null) {
                sb.append(",\"").append(nodeValue).append("\"");
            }
            if (node.getNodes().size() > 0) {
                for (ExpAstNode child : node.getNodes()) {
                    sb.append(",");
                    nodeToString(sb, child);
                }
            }
            sb.append("]");
        }
    }

    public static String getValueFromStringNode(AstNode node) {
        return ((StringAstNode)node).getValue();
    }

    public static boolean isLeafNodeType(AstNode node) {
        return node != null && node.hasValue();
    }

    public static boolean isString(Object obj) {
        return obj instanceof String;
    }

    public static boolean isNumber(Object value) {
        if (value instanceof Number) {
            return true;
        } else if (!isString(value)) {
            return false;
        }

        String v = (String) value;
        try {
            Integer.parseInt(v);
            return true;
        } catch (Exception ignore) {

        }

        try {
            Double.parseDouble(v);
            return true;
        } catch (Exception ignore) {

        }
        return false;
    }

    public static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    public static boolean isDouble(String val) {
        try {
            Double.parseDouble(val);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }
}
