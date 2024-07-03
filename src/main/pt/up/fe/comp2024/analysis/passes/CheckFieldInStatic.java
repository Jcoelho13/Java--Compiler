package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;

public class CheckFieldInStatic extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitFieldDecl);
    }

    /**
     * Visit Field Declaration
     * @param classDecl node
     * @param table symbol table
     * @return null
     */
    private Void visitFieldDecl(JmmNode classDecl, SymbolTable table) {
        var fields = table.getFields();
        var methods = table.getMethods();

        for (var method : methods) {
            if (method.equals("main")) {
                // Get the main method node
                JmmNode mainMethodNode = getMethodNode(classDecl, method);
                if (mainMethodNode != null && mainMethodNode.getNumChildren() > 0) {
                    // Traverse the body of the main method to check for field usage
                    JmmNode mainMethodBody = mainMethodNode.getChildren().get(0);
                    checkFieldUsageInMethod(mainMethodBody, fields, classDecl);
                }
                break; // Only process the main method
            }
        }

        return null;
    }

    /**
     * Get the node of a method with a given name
     * @param classDecl node
     * @param methodName name of the method
     * @return node of the method
     */
    private JmmNode getMethodNode(JmmNode classDecl, String methodName) {
        for (JmmNode method : classDecl.getChildren()) {
            if (method.getKind().equals("MethodDecl") && method.get("name").equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Check field usage in a method
     * @param node node
     * @param fields list of fields
     * @param classDecl node
     */
    private void checkFieldUsageInMethod(JmmNode node, List<Symbol> fields, JmmNode classDecl) {
        // Check if the node is a field reference, and if it is, report an error
        if (isFieldReference(node, fields)) {
            var message = "Cannot access field in static method";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(node),
                    NodeUtils.getColumn(node),
                    message,
                    null
            ));
        }

        // Recursively check all children nodes
        for (JmmNode child : node.getChildren()) {
            checkFieldUsageInMethod(child, fields, classDecl);
        }
    }

    /**
     * Check if a node is a field reference
     * @param node node
     * @param fields list of fields
     * @return true if the node is a field reference, false otherwise
     */
    private boolean isFieldReference(JmmNode node, List<Symbol> fields) {
        if (node.getKind().equals("VarRefExpr")) {
            String varName = node.get("name");
            System.out.println("VarRef found: " + varName);
            for (var field : fields) {
                if (field.getName().equals(varName)) {
                    System.out.println("Field reference matches: " + varName);
                    return true;
                }
            }
        } else if (node.getKind().equals("AssignmentExpr") || node.getKind().equals("ExprStmt")) {
            for (JmmNode child : node.getChildren()) {
                if (isFieldReference(child, fields)) {
                    return true;
                }
            }
        }
        return false;
    }


}
