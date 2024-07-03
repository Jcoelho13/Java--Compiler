package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.HashMap;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";
    private final String L_PAREN = "(";
    private final String R_PAREN = ")";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(IMPORT_STMT, this::visitImportStmt);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(ARRAY_ACCESS_EXPR, this::visitExprStmt);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(SCOPE_STMT, this::visitScopeStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        //get the condition and the statement
        var condition = exprVisitor.visit(node.getJmmChild(0));
        var stmt = visit(node.getJmmChild(1));

        //loop structure
        code.append("whileCond1:\n");
        code.append(condition.getComputation());
        code.append("if(").append(condition.getCode()).append(") goto whileEnd1");
        code.append(END_STMT);
        code.append("whileLoop1:\n");
        code.append(stmt);
        code.append("goto whileCond1");
        code.append(END_STMT);
        code.append("whileEnd1:\n");

        return code.toString();
    }

    private String visitScopeStmt(JmmNode node, Void unused) {

        //visit the code inside the scope
        StringBuilder code = new StringBuilder();
        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }

    private String visitIfElseStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        //get the condition and the statements
        var condition = exprVisitor.visit(node.getJmmChild(0));
        var thenStmt = visit(node.getJmmChild(1));
        var elseStmt = visit(node.getJmmChild(2));

        //condition structure
        code.append(condition.getComputation());
        code.append("if");
        code.append(L_PAREN);
        code.append(condition.getCode());
        code.append(R_PAREN);
        code.append(" goto if1");
        code.append(END_STMT);
        code.append(thenStmt);
        code.append("goto endif1");
        code.append(END_STMT);
        code.append("if1:\n");
        code.append(elseStmt);
        code.append("endif1:\n");

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {

        //check for field variables
        if (CLASS_DECL.check(node.getParent())) {
            var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
            var id = node.get("name");

            String code = ".field " + "public " + id + typeCode + END_STMT;

            return code;
        }
        return "";
    }

    private String visitExprStmt(JmmNode node, Void unused) {

        //visit the expression
        var expr = exprVisitor.visit(node.getJmmChild(0));

        return expr.getCode();
    }

    private String visitImportStmt(JmmNode node, Void unused) {
        var name = node.get("name");

        // Check if the name starts and ends with square brackets before removing them
        if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }

        name = name.replace(", ", ".");

        // Ensure that the appendImportNode method is correctly handling the import node
        exprVisitor.appendImportNode(node);

        // Ensure that the generated OLLIR code for the import statement is in the correct format
        return "import " + name + END_STMT;
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        //get both parts of the assignment expr
        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        //get the method name and its return type
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        //visit the return expr
        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        //build the return statement structure
        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        //get the type and the name of the param
        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode + ", ";

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        // check if the method is public
        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        // get the parameters of the method
        int afterParam = 0;
        StringBuilder paramCode = new StringBuilder();
        for (JmmNode child : node.getChildren()) {
            //if current child is not from type return stmt, assign stmt or expr stmt, then it is a param
            if (!IF_ELSE_STMT.check(child) && !VAR_DECL.check(child) && !RETURN_STMT.check(child) && !ASSIGN_STMT.check(child) && !EXPR_STMT.check(child)) {
                var param = visit(child);

                // check if the param is varargs
                if (PARAM.check(child) && VARARG.check(child.getJmmChild(0))) {
                    code.append("varargs ");
                }

                paramCode.append(param);
                afterParam++;
            } else {
                break;
            }
        }

        // main method exception
        var name = node.get("name");
        if(name.equals("main")) {
            code.append("static main(args.array.String).V");
        }
        else{
            code.append(name);
        }

        // remove trailing comma and space
        if (!paramCode.isEmpty()) {
            paramCode.setLength(paramCode.length() - 2);
        }

        // type
        // if the method is main it has no return type
        if (!name.equals("main")) {
            code.append("(").append(paramCode).append(")");
            //if node.getJmmchild(0) is of Ttype code.append(OptUtils.toOllirType(node.getJmmChild(0))); else code.append(OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table)))
            if (TTYPE.check(node.getJmmChild(0))) {
                code.append(OptUtils.toOllirType(node.getJmmChild(0)));
            } else {
                code.append(OptUtils.toOllirType(TypeUtils.getExprType(node.getJmmChild(0), table)));
            }
        }
        code.append(L_BRACKET);

        // visit the statements inside the method
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        if (name.equals("main")) {
            code.append("ret.V;\n");
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        if (node.getAttributes().size() > 5) {
            code.append(" extends ");
            code.append(node.get("extendedClass"));
        }

        code.append(L_BRACKET);
        var needNl = true;

        // visit the children of the class
        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        // build the constructor structure
        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        return "";
    }
}