package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String String_TYPE_NAME = "String";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
    }

    public static String getStringTypeName() {
        return String_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOLEAN_TYPE_NAME, false);
            case VAR_DECL, CLASS_INSTANCE, NEW_ARRAY -> new Type(expr.get("name"), false);
            case CALL_METHOD -> table.getReturnType(expr.get("methodCall"));
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "/", "-" -> new Type(INT_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        var varName = varRefExpr.get("name");
        var currentMethod = varRefExpr.getAncestor(Kind.METHOD_DECL).get().get("name");
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varName))) {
            return table.getLocalVariables(currentMethod).stream()
                    .filter(param -> param.getName().equals(varName))
                    .toList().get(0).getType();
        }
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varName))) {
            return table.getParameters(currentMethod).stream()
                    .filter(param -> param.getName().equals(varName))
                    .toList().get(0).getType();
        }
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varName))) {
            return table.getFields().stream()
                    .filter(param -> param.getName().equals(varName))
                    .toList().get(0).getType();
        }
        return null;
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}