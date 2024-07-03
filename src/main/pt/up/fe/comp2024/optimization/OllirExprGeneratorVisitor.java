package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private List<JmmNode> importedNodes = new ArrayList<>();


    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    public void appendImportNode(JmmNode node) {
        boolean alreadyImported = false;
        for (JmmNode importedNode : importedNodes) {
            if (importedNode.get("ID").equals(node.get("ID"))) {
                alreadyImported = true;
                break;
            }
        }
        if (!alreadyImported) {
            importedNodes.add(node);
        }
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL_EXPR, this::visitInteger);
        addVisit(BOOLEAN_LITERAL_EXPR, this::visitBoolean);
        addVisit(UNARY_EXPR, this::visitUnaryExpr);
        addVisit(ASSIGNMENT_EXPR, this::visitAssignStmt);
        addVisit(ADDITIVE_EXPR, this::visitBinExpr);
        addVisit(MULTIPLICATIVE_EXPR, this::visitBinExpr);
        addVisit(METHOD_CALL_EXPR, this::visitMethodCall);
        addVisit(ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(NEW_CLASS_EXPR, this::visitNewClassExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(LENGTH_EXPR, this::visitLengthExpr);
        addVisit(RELATIONAL_EXPR, this::visitRelationalExpr);
        addVisit(SHORT_C_AND_EXPR, this::visitShortCAndExpr);
        addVisit(PAREN_EXPR, this::visitParenExpr);
        addVisit(THIS_LITERAL_EXPR, this::visitThisExpr);
        addVisit(ARRAY_INIT_EXPR, this::visitArrayInitExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitArrayInitExpr(JmmNode node, Void unused) {

        // Array initialization
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String tempVar = OptUtils.getTemp() + ".array.i32";
        computation.append(tempVar).append(" :=.array.i32 new(array, ").append(node.getNumChildren()).append(".i32).array.i32;\n");

        for (int i = 0; i < node.getNumChildren(); i++) {
            String tempVar1 = OptUtils.getTemp() + ".i32";
            computation.append(tempVar1).append(" :=.i32 ").append(visit(node.getJmmChild(i)).getCode()).append(";\n");
        }

        return new OllirExprResult(tempVar, computation.append(code));
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        // This is a special case, as it is a keyword
        return new OllirExprResult("this");
    }

    private OllirExprResult visitParenExpr(JmmNode node, Void unused) {
        // handle parenthesis expressions
        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitShortCAndExpr(JmmNode node, Void unused) {

        // Short-circuit AND expression
        String tempVar = OptUtils.getTemp() + ".bool";
        String code = "";
        if (METHOD_CALL_EXPR.check(node.getJmmChild(0)) || METHOD_CALL_EXPR.check(node.getJmmChild(1))) {
            String tempVar1 = OptUtils.getTemp() + ".bool";
            String tempVar2 = OptUtils.getTemp() + ".bool";
            code += tempVar1 + " :=.bool " + visit(node.getJmmChild(0)).getCode() + END_STMT;
            code += tempVar2 + " :=.bool " + visit(node.getJmmChild(1)).getCode() + END_STMT;
            code += tempVar + " :=.bool " + tempVar1 + " &&.bool " + tempVar2 + END_STMT;
        } else {
            code = tempVar + " :=.bool " + visit(node.getJmmChild(0)).getCode() + " &&.bool " + visit(node.getJmmChild(1)).getCode() + END_STMT;
        }

        return new OllirExprResult(tempVar, new StringBuilder(code));
    }

    private OllirExprResult visitRelationalExpr(JmmNode node, Void unused) {

        // boolean expression
        String tempVar = OptUtils.getTemp() + ".bool";
        String code = tempVar + " :=.bool " + visit(node.getJmmChild(0)).getCode() + " " + node.get("op") + ".bool " + visit(node.getJmmChild(1)).getCode() + END_STMT;

        return new OllirExprResult(tempVar, new StringBuilder(code));
    }

    private OllirExprResult visitLengthExpr(JmmNode node, Void unused) {

        // length expression
        String tempVar = OptUtils.getTemp() + ".i32";
        String computation = tempVar + " :=.i32 arraylength(" + visit(node.getJmmChild(0)).getCode() + ")" + ".i32" + END_STMT;
        String code = tempVar;

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused) {

        // new array expression
        var size = visit(node.getJmmChild(1));
        var type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        StringBuilder computation = new StringBuilder();

        computation.append(size.getComputation());

        String code = "new(array, " + size.getCode() + ")" + ollirType;

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNewClassExpr(JmmNode node, Void unused) {

        // new class expression
        var type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        // build class structure
        String code = "new(" + node.get("name") + ")" + ollirType + END_STMT;

        var lhs = visit(node.getParent().getJmmChild(0));

        code += "invokespecial(" + lhs.getCode() + ", \"<init>\").V";

        return new OllirExprResult(code);
    }

    private OllirExprResult visitArrayAccessExpr(JmmNode node, Void unused) {

        // array access expression
        var arrayName = visit(node.getJmmChild(0));
        var index = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        computation.append(arrayName.getComputation());
        computation.append(index.getComputation());

        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = arrayName.getCode() + "[" + index.getCode() + "]" + ollirType;

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {

        // integer literal
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {

        // boolean literal
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = node.get("value") + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitAssignStmt(JmmNode node, Void unused) {

        // get both sides of the assignment
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        // check if lhs is a field
        if (table.getFields().stream().anyMatch(param -> param.getName().equals(node.getJmmChild(0).get("name")))) {
            Type type = TypeUtils.getExprType(node.getJmmChild(0), table);
            Type type2 = TypeUtils.getExprType(node.getJmmChild(1), table);
            String ollirType = OptUtils.toOllirType(type);
            String ollirType2 = OptUtils.toOllirType(type2);
            String className = table.getClassName();

            // build field structure
            if(ollirType2.equals(".i32")) {
                String code = "putfield(this." + className + ", " + node.getJmmChild(0).get("name") + ollirType + ", " + node.getJmmChild(1).get("value") + ollirType2  + ").V" + END_STMT;
                return new OllirExprResult(code);
            }
            String code = "";

            return new OllirExprResult(code);
        }

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        // build assignment structure
        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        if(!rhs.getCode().contains(".")) {
            code.append(typeString);
        }

        code.append(END_STMT);

        return new OllirExprResult(code.toString());
    }

    private OllirExprResult visitUnaryExpr(JmmNode node, Void unused) {

        var child = visit(node.getJmmChild(0));

        StringBuilder computation = new StringBuilder();

        // code to compute the child
        computation.append(child.getComputation());

        // code to compute self
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);
        String code = OptUtils.getTemp() + ollirType;

        // build unary expression structure
        computation.append(code).append(SPACE)
                .append(ASSIGN).append(ollirType).append(SPACE)
                .append(node.get("op")).append(ollirType).append(SPACE)
                .append(child.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        // get both sides of the binary expression
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // handle special cases
        if (rhs.getCode().startsWith("invokevirtual")) {
            String tempVar = OptUtils.getTemp() + ".i32";
            computation.append(tempVar).append(" :=.i32 ").append(rhs.getCode()).append(";\n");
            rhs = new OllirExprResult(tempVar);
        }

        else if (rhs.getCode().startsWith("invokestatic")) {
            String tempVar = OptUtils.getTemp() + ".i32";
            computation.append(tempVar).append(" :=.i32 ").append(rhs.getCode()).append(";\n");
            rhs = new OllirExprResult(tempVar);
        }

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        // build binary expression structure
        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        // check if the variable is a field
        if (table.getFields().stream().anyMatch(param -> param.getName().equals(node.get("name")))) {
            Type type = TypeUtils.getExprType(node, table);
            String ollirType = OptUtils.toOllirType(type);
            String className = table.getClassName();
            String tempVar = OptUtils.getTemp() + ollirType;

            // build field structure
            String code = tempVar + " :=" + ollirType + " getfield(this." + className + ", " + node.get("name") + ollirType + ").V" + END_STMT;
            return new OllirExprResult(tempVar, new StringBuilder(code));
        }

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {

        // method call expression
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String id = node.get("name");

        // Check if the method is in the methods list -> virtual method
        if (this.table.getMethods().contains(id)) {
            StringBuilder ivCode = new StringBuilder();

            code.append("invokevirtual(");

            // Handle object type and class name
            var objOllirType = "";
            if (node.getNumChildren() == 1) {
                objOllirType = OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table));
                code.append(objOllirType.split("\\.")[1]).append(".").append(this.table.getClassName());
                code.append(", \"").append(id).append("\"");
            }

            else {
                // checks if the method is a varargs method
                if(this.table.getReturnType(id).getName().contains("varargs")){
                    String tempVar1 = OptUtils.getTemp() + ".array.i32";
                    computation.append(tempVar1).append(" :=.array.i32 new(array, ").append(node.getNumChildren()-1).append(".i32).array.i32;\n");

                    for (int i = 0; i < node.getNumChildren(); i++) {
                        if(i == 0) {
                            code.append(visit(node.getJmmChild(i)).getCode()).append(", ");
                            code.append("\"").append(id).append("\"").append(", ");
                        }
                        else {
                            String tempVar = OptUtils.getTemp() + ".i32";
                            computation.append(tempVar).append(" :=.i32 ").append(visit(node.getJmmChild(i)).getCode()).append(";\n");
                        }
                    }
                    code.append(tempVar1);
                }
                else {
                    // base case
                    for (int i = 0; i < node.getNumChildren(); i++) {
                        if (i == 1) {
                            code.append("\"").append(id).append("\"");
                            code.append(", ");
                        }
                        computation.append(visit(node.getJmmChild(i)).getComputation());
                        code.append(visit(node.getJmmChild(i)).getCode());
                        code.append(", ");
                    }
                    // Remove trailing comma and space
                    code.delete(code.length() - 2, code.length());
                }
            }

            computation.append(ivCode);

            code.append(")").append(OptUtils.toOllirType(this.table.getReturnType(id)));

            //if the parent is not a binary expression, we need to add a semicolon
            if (!SHORT_C_AND_EXPR.check(node.getParent()) && !ASSIGNMENT_EXPR.check(node.getParent()) && !ADDITIVE_EXPR.check(node.getParent()) && !MULTIPLICATIVE_EXPR.check(node.getParent()) && !METHOD_CALL_EXPR.check(node.getParent())) {
                code.append(END_STMT);
            }

        }

        // static methods
        else {
            StringBuilder attributes = new StringBuilder();
            for (int i = 1; i < node.getNumChildren(); i++) {
                if(LENGTH_EXPR.check(node.getJmmChild(i))) {
                    code.append(visit(node.getJmmChild(i)).getComputation());
                    attributes.append(", ").append(visit(node.getJmmChild(i)).getCode());
                }
                else if(METHOD_CALL_EXPR.check(node.getJmmChild(i))) {
                    String tempVar = OptUtils.getTemp() + ".i32";
                    code.append(tempVar).append(" :=.i32 ").append(visit(node.getJmmChild(i)).getCode()).append(";\n");

                    attributes.append(", ").append(tempVar);
                }
                else {
                    var child = visit(node.getJmmChild(i));
                    code.append(child.getComputation());
                    attributes.append(", ");
                    attributes.append(child.getCode());
                }
            }

            // static method structure
            code.append("invokestatic(");
            code.append(visit(node.getJmmChild(0)).getCode().split("\\.")[0]);
            code.append(", ").append("\"").append(id).append("\"");

            code.append(attributes);

            // Handle return type
            if (ASSIGNMENT_EXPR.check(node.getParent())) {
                Type type = TypeUtils.getExprType(node.getParent().getJmmChild(0), table);
                code.append(")").append(OptUtils.toOllirType(type));
            } else {
                code.append(").V");
            }

            //if the parent is not a binary expression, we need to add a semicolon
            if (!ARRAY_ACCESS_EXPR.check(node.getParent()) && !METHOD_CALL_EXPR.check(node.getParent()) && !ASSIGNMENT_EXPR.check(node.getParent()) && !ADDITIVE_EXPR.check(node.getParent()) && !MULTIPLICATIVE_EXPR.check(node.getParent())) {
                code.append(END_STMT);
            }
        }

        return new OllirExprResult(code.toString(), computation);
    }




    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return new OllirExprResult("default visit");
    }

}
