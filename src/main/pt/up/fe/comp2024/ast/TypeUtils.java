package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";
    private static final String VOID_TYPE_NAME = "void";
    private static final String STRING_TYPE_NAME = "String";

    public static String getTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBooleanTypeName() {
        return BOOLEAN_TYPE_NAME;
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
            case BINARY_EXPR, ADDITIVE_EXPR, MULTIPLICATIVE_EXPR, RELATIONAL_EXPR -> getBinExprType(expr);
            case UNARY_EXPR -> new Type(BOOLEAN_TYPE_NAME, false);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL,  INTEGER_LITERAL_EXPR-> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL, BOOLEAN_LITERAL_EXPR -> new Type(BOOLEAN_TYPE_NAME, false);
            case LENGTH_EXPR -> new Type(INT_TYPE_NAME, false);
            case ARRAY_ACCESS_EXPR -> getArrayAccessExprType(expr, table);
            case ARRAY_TYPE -> getArrayExpType(expr, table);
            case NEW_ARRAY_EXPR, ARRAY_INIT_EXPR -> getNewArrayExprType(expr, table);
            case METHOD_CALL_EXPR -> getMethodCallExprType(expr, table);
            case NEW_CLASS_EXPR -> getNewClassExprType(expr, table);
            case THIS_LITERAL_EXPR -> new Type("this", false);

            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getNewClassExprType(JmmNode newClassExpr, SymbolTable table) {
        var className =  newClassExpr.get("name");
        if(table.getClassName().equals(className)){
            var type = new Type(className, false);
            type.putObject("super", table.getSuper());
            return type;
        }
        var type = new Type(className, false);
        type.putObject("", table.getSuper());
        return type;
    }

    private static Type getMethodCallExprType(JmmNode methodCallExpr, SymbolTable table) {
        return new Type(INT_TYPE_NAME, false);
    }

    private static Type getArrayExpType(JmmNode arrayType, SymbolTable table) {
        /*
        var name = "";
        for (var child : arrayType.getChildren()) {
            if (child.getKind().equals("TTYPE")) {
                name = child.get("name");
            }
        }
        */
        return new Type(INT_TYPE_NAME, true);
    }

    private static Type getNewArrayExprType(JmmNode newArrayExpr, SymbolTable table) {
        return new Type(INT_TYPE_NAME, true);
    }

    private static Type getArrayAccessExprType(JmmNode arrayAccessExpr, SymbolTable table) {
        return new Type(INT_TYPE_NAME, false);
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "<"  -> {
                JmmNode left = binaryExpr.getChildren().get(0);
                JmmNode right = binaryExpr.getChildren().get(1);
                Type leftType = getExprType(left, null);
                Type rightType = getExprType(right, null);
                if(leftType.getName().equals(INT_TYPE_NAME) && rightType.getName().equals(INT_TYPE_NAME)){
                    yield new Type(BOOLEAN_TYPE_NAME, false);
                }
                else{
                    throw new RuntimeException("Invalid types for operator '<' of expression '" + binaryExpr + "'");
                }
            }
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        var varName = varRefExpr.get("name");
        var methodName = varRefExpr.getAncestor(Kind.METHOD_DECL).get().get("name");

        for(var symbol : table.getLocalVariables(methodName)){
            if(symbol.getName().equals(varName)){
                return symbol.getType();
            }
        }
        return new Type(INT_TYPE_NAME, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType, SymbolTable table) {
        // If the types are the same, they are assignable
        if (sourceType.getName().equals(destinationType.getName())) {
            return true;
        }

        // Check if sourceType is a subclass of destinationType
        String superClass = table.getSuper();
        while (superClass != null) {
            if (superClass.equals(destinationType.getName())) {
                return true;
            }
            superClass = table.getSuper(); // Assuming getSuper(String) returns the superclass of the given class
        }

        // Check if the types are imported
        List<String> imports = table.getImports();
        if (imports.contains(sourceType.getName()) && imports.contains(destinationType.getName())) {
            return true;
        }

        // If none of the above checks pass, the types are not assignable
        return false;
    }

    public static String getTypeDescriptor(Type type) {
        switch (type.getName()) {
            case "int":
                return "I";
            case "boolean":
                return "Z";
            // Add more cases as needed for other types
            default:
                throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }
}
