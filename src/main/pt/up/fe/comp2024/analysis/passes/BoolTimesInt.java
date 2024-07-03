package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

public class BoolTimesInt extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.MULTIPLICATIVE_EXPR, this::visitBinaryExpr);
        addVisit(Kind.ADDITIVE_EXPR, this::visitBinaryExpr);
    }

    /**
     * Check if the binary operation can be performed on the given operands
     * @param left The left operand
     * @param right The right operand
     * @param table The symbol table
     * @return True if the operation can be performed, false otherwise
     */
    private boolean canBinaryOp(JmmNode left, JmmNode right, SymbolTable table) {

        if (left.getKind().equals("CanMultiply") || right.getKind().equals("CanMultiply")) {
            return true;
        }

        if(isParenExpr(left)) {
            left = left.getChildren().get(0);
        }

        if(isParenExpr(right)) {
            right = right.getChildren().get(0);
        }

        // 10 * 10 -> if both are int literals, return true
        if (isIntLiteral(left) && isIntLiteral(right)) {
            System.out.println("int * int");
            return true;
        }

        // 10 * a -> if a is an int variable, return true
        if (isVarRef(left) && isIntLiteral(right)) {
            System.out.println("var * int");
            return isIntVariable(left, table);
        }

        // a * 10 -> if a is an int variable, return true
        if (isIntLiteral(left) && isVarRef(right)) {
            System.out.println("int * var");
            return isIntVariable(right, table);
        }

        // a * b -> if both are int variables, return true
        if (isVarRef(left) && isVarRef(right)) {
            System.out.println("var * var");
            return isIntVariable(left, table) && isIntVariable(right, table);
        }


        // 10 * a.length -> if a is an array, return true
        if(isIntLiteral(left) && isLengthExpr(right)) {
            System.out.println("int * length");
            return isIntArrayVariable(right, table);
        }

        // a.length * 10 -> if a is an array, return true
        if(isLengthExpr(left) && isIntLiteral(right)) {
            System.out.println("length * int");
            return isIntArrayVariable(left, table);
        }

        // a.length * b -> if a is an array and b is an int variable, return true
        if(isLengthExpr(left) && isVarRef(right)) {
            System.out.println("length * var");
            return isIntArrayVariable(left, table) && isIntVariable(right, table);
        }

        // a * b.length -> if a is an int variable and b is an array, return true
        if(isVarRef(left) && isLengthExpr(right)) {
            System.out.println("var * length");
            return isIntVariable(left, table) && isIntArrayVariable(right, table);
        }

        // a.length * b.length -> if both are arrays, return true
        if(isLengthExpr(left) && isLengthExpr(right)) {
            System.out.println("length * length");
            return isIntArrayVariable(left, table) && isIntArrayVariable(right, table);
        }

        // 10 * b[0] -> if a is an int variable and b is an array, return true
        if(isIntLiteral(left) && isArrayAccess(right)) {
            System.out.println("int * array");
            return isIntArrayVariable(right, table);
        }

        // a[0] * 10 -> if a is an array and b is an int variable, return true
        if(isArrayAccess(left) && isIntLiteral(right)) {
            System.out.println("array * int");
            return isIntArrayVariable(left, table);
        }

        // a[0] * b -> if a is an array and b is an int variable, return true
        if(isArrayAccess(left) && isVarRef(right)) {
            System.out.println("array * var");
            return isIntArrayVariable(left, table) && isIntVariable(right, table);
        }

        // a * b[0] -> if a is an int variable and b is an array, return true
        if(isVarRef(left) && isArrayAccess(right)) {
            System.out.println("var * array");
            return isIntVariable(left, table) && isIntArrayVariable(right, table);
        }

        // a[0] * b[0] -> if both are arrays, return true
        if(isArrayAccess(left) && isArrayAccess(right)) {
            System.out.println("array * array");
            return isIntArrayVariable(left, table) && isIntArrayVariable(right, table);
        }

        // a[0] * b.length -> if a is an array and b is an array, return true
        if(isArrayAccess(left) && isLengthExpr(right)) {
            System.out.println("array * length");
            return isIntArrayVariable(left, table) && isIntArrayVariable(right, table);
        }

        // a.length * b[0] -> if a is an array and b is an array, return true
        if(isLengthExpr(left) && isArrayAccess(right)) {
            System.out.println("length * array");
            return isIntArrayVariable(left, table) && isIntArrayVariable(right, table);
        }

        // 10 * method() -> if method returns int, return true
        if (isIntLiteral(left) && isMethodCall(right)) {
            System.out.println("int * method");
            return isMethodReturningInt(right, table);
        }

        // method() * 10 -> if method returns int, return true
        if (isMethodCall(left) && isIntLiteral(right)) {
            System.out.println("method * int");
            return isMethodReturningInt(left, table);
        }

        // a * method() -> if a is an int variable and method returns int, return true
        if (isVarRef(left) && isMethodCall(right)) {
            System.out.println("var * method");
            return isIntVariable(left, table) && isMethodReturningInt(right, table);
        }

        // method() * a -> if a is an int variable and method returns int, return true
        if (isMethodCall(left) && isVarRef(right)) {
            System.out.println("method * var");
            return isMethodReturningInt(left, table) && isIntVariable(right, table);
        }

        // method() * method() -> if both methods return int, return true
        if (isMethodCall(left) && isMethodCall(right)) {
            System.out.println("method * method");
            return areMethodsReturningInt(left, right, table);
        }

        // method() * b[0] -> if method returns int and b is an array, return true
        if (isMethodCall(left) && isArrayAccess(right)) {
            System.out.println("method * array");
            return isMethodReturningInt(left, table) && isIntArrayVariable(right, table);
        }

        // b[0] * method() -> if method returns int and b is an array, return true
        if (isArrayAccess(left) && isMethodCall(right)) {
            System.out.println("array * method");
            return isIntArrayVariable(left, table) && isMethodReturningInt(right, table);
        }

        // a.length * method() -> if a is an array and method returns int, return true
        if (isLengthExpr(left) && isMethodCall(right)) {
            System.out.println("length * method");
            return isIntArrayVariable(left, table) && isMethodReturningInt(right, table);
        }

        // method() * a.length -> if a is an array and method returns int, return true
        if (isMethodCall(left) && isLengthExpr(right)) {
            System.out.println("method * length");
            return isMethodReturningInt(left, table) && isIntArrayVariable(right, table);
        }


        return false;
    }

    /**
     * Check if the given node is an integer literal
     * @param node The node to check
     * @return True if the node is an integer literal, false otherwise
     */
    private boolean isIntLiteral(JmmNode node) {
        return node.getKind().equals(Kind.INTEGER_LITERAL_EXPR.toString());
    }

    /**
     * Check if the given node is a variable reference
     * @param node The node to check
     * @return True if the node is a variable reference, false otherwise
     */
    private boolean isVarRef(JmmNode node) {
        return node.getKind().equals(Kind.VAR_REF_EXPR.toString());
    }

    /**
     * Check if the given node is a length expression
     * @param node The node to check
     * @return True if the node is a length expression, false otherwise
     */
    private boolean isLengthExpr(JmmNode node) {
        return node.getKind().equals(Kind.LENGTH_EXPR.toString());
    }

    /**
     * Check if the given node is an array access expression
     * @param node The node to check
     * @return True if the node is an array access expression, false otherwise
     */
    private boolean isArrayAccess(JmmNode node) {
        return node.getKind().equals(Kind.ARRAY_ACCESS_EXPR.toString());
    }

    /**
     * Check if the given node is a method call expression
     * @param node The node to check
     * @return True if the node is a method call expression, false otherwise
     */
    private boolean isMethodCall(JmmNode node) {
        return node.getKind().equals(Kind.METHOD_CALL_EXPR.toString());
    }

    /**
     * Check if the given node is a parenthesized expression
     * @param node The node to check
     * @return True if the node is a parenthesized expression, false otherwise
     */
    private boolean isParenExpr(JmmNode node) {
        return node.getKind().equals(Kind.PAREN_EXPR.toString());
    }

    /**
     * Check if the given variable is an integer variable
     * @param node The node to check
     * @param table The symbol table
     * @return True if the variable is an integer variable, false otherwise
     */
    private boolean isIntVariable(JmmNode node, SymbolTable table) {
        String varName = node.get("name");
        for (String method : table.getMethods()) {
            for (Symbol variable : table.getLocalVariables(method)) {
                if(variable.getName().equals(varName) && !variable.getType().isArray()){
                    return variable.getType().getName().equals("int");
                }
            }
        }
        return false;
    }

    /**
     * Check if the given variable is an integer array variable
     * @param node The node to check
     * @param table The symbol table
     * @return True if the variable is an integer array variable, false otherwise
     */
    private boolean isIntArrayVariable(JmmNode node, SymbolTable table) {
        String varName = node.getChildren().get(0).get("name");
        for (String method : table.getMethods()) {
            for (Symbol variable : table.getLocalVariables(method)) {
                if (variable.getName().equals(varName) && variable.getType().isArray()) {
                    return variable.getType().getName().equals("int");
                }
            }
        }
        return true;
    }

    /**
     * Check if the given method is returning an integer
     * @param node The node to check
     * @param table The symbol table
     * @return True if the method is returning an integer, false otherwise
     */
    private boolean isMethodReturningInt(JmmNode node, SymbolTable table) {
        String methodName = node.get("name");
        for (String method : table.getMethods()) {
            if (method.equals(methodName)) {
                return table.getReturnType(methodName).getName().equals("int");
            }
        }
        return false;
    }

    /**
     * Check if the given methods are returning an integer
     * @param left The left method to check
     * @param right The right method to check
     * @param table The symbol table
     * @return True if both methods are returning an integer, false otherwise
     */
    private boolean areMethodsReturningInt(JmmNode left, JmmNode right, SymbolTable table) {
        String methodLeftName = left.get("name");
        String methodRightName = right.get("name");
        for (String method : table.getMethods()) {
            if (method.equals(methodLeftName) && method.equals(methodRightName)) {
                return table.getReturnType(methodLeftName).getName().equals("int") &&
                        table.getReturnType(methodRightName).getName().equals("int");
            }
        }
        return false;
    }

    /**
     * Visit a binary expression node and check if the operation can be performed
     * @param binaryOp The binary expression node
     * @param table The symbol table
     * @return null
     */
    private Void visitBinaryExpr(JmmNode binaryOp, SymbolTable table) {
        JmmNode left = binaryOp.getChildren().get(0);
        JmmNode right = binaryOp.getChildren().get(1);

        // Resolve and check operations for left and right operands
        left = resolveAndCheckOperation(left, table);
        right = resolveAndCheckOperation(right, table);

        // If both operands are not binary operations, check if the operation can be performed
        if (!left.hasAttribute("op") && !right.hasAttribute("op")) {
            boolean canMultiply = canBinaryOp(left, right, table);
            if (!canMultiply) {
                var message = "Cannot perform operation on these operands.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(binaryOp),
                        NodeUtils.getColumn(binaryOp),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    /**
     * Resolve and check the operation of the given node
     * @param node The node to resolve and check
     * @param table The symbol table
     * @return The resolved node
     */
    private JmmNode resolveAndCheckOperation(JmmNode node, SymbolTable table) {
        // Resolve parenthesized expressions first
        while(node.getKind().equals("ParenExpr")) {
            node = node.getChildren().get(0);
        }

        // If the node is a binary operation, resolve its children recursively
        if (isBinaryOperation(node)) {
            JmmNode left = resolveAndCheckOperation(node.getChildren().get(0), table);
            JmmNode right = resolveAndCheckOperation(node.getChildren().get(1), table);

            // Check if the operation can be performed, if not, add an error report
            if (!canBinaryOp(left, right, table)) {
                var message = "Cannot perform operation on non-integer operands.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(node),
                        NodeUtils.getColumn(node),
                        message,
                        null)
                );
            }

            // Compute and return the result of the binary operation
            return computeBinaryOperation(left, right, node.get("op"), table);
        }

        return node;
    }


    /**
     * Check if the given node is a binary operation
     * @param node The node to check
     * @return True if the node is a binary operation, false otherwise
     */
    private boolean isBinaryOperation(JmmNode node) {
        return node.getKind().equals("BinaryOp") || node.getKind().equals("AdditiveExpr") || node.getKind().equals("MultiplicativeExpr") || node.getKind().equals("MinusExpr") || node.getKind().equals("DivideExpr");
    }

    /**
     * Compute the result of a binary operation
     * @param left The left operand
     * @param right The right operand
     * @param op The operation to perform
     * @param table The symbol table
     * @return The result of the binary operation
     */
    private JmmNode computeBinaryOperation(JmmNode left, JmmNode right, String op, SymbolTable table) {
        // Check if both operands the value attribute
        boolean hasLeftValue = left.hasAttribute("value");
        boolean hasRightValue = right.hasAttribute("value");

        // if so, compute the result
        if(hasLeftValue && hasRightValue) {
            int leftValue = Integer.parseInt(left.get("value"));
            int rightValue = Integer.parseInt(right.get("value"));
            int result;

            switch (op) {
                case "*":
                    result = leftValue * rightValue;
                    break;
                case "+":
                    result = leftValue + rightValue;
                    break;
                case "-":
                    result = leftValue - rightValue;
                    break;
                case "/":
                    if (rightValue == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    result = leftValue / rightValue;
                    break;
                default:
                    throw new UnsupportedOperationException("Operation " + op + " not supported.");
            }

            // Create a new node with the computed result
            JmmNode resultNode = new JmmNodeImpl("IntegerLiteralExpr");
            resultNode.put("value", Integer.toString(result));

            return resultNode;
        }

        // if a binary operation is possible, return a new node (useful for varRef)
        if(canBinaryOp(left, right, table)) {
            return new JmmNodeImpl("CanMultiply");
        }

        return null;

    }

}
