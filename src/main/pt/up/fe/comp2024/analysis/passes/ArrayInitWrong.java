package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.Kind;

import java.util.Objects;

public class ArrayInitWrong extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.ARRAY_INIT_EXPR, this::visitArrayInit);
    }

    /**
     * Check if the array initialization is valid
     * @param arrayInit The array initialization node
     * @param table The symbol table
     * @return null
     */
    private Void visitArrayInit(JmmNode arrayInit, SymbolTable table) {
        var arrayMembers = arrayInit.getChildren();
        boolean flag = false;
        for (JmmNode arrayMember : arrayMembers) {
            // if the array contains elements of different types, set the flag to true
            if (!Objects.equals(arrayMember.getKind(), arrayMembers.get(0).getKind())) {
                flag = true;
                break;
            }
        }

        // Create error report for mixed-type array elements
        if (flag) {
            var message = "Array contains elements of different type than declared.";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(arrayInit),
                    NodeUtils.getColumn(arrayInit),
                    message,
                    null)
            );
        }

        // Check if the parent of this arrayInit is an assignment expression
        if ("AssignmentExpr".equals(arrayInit.getParent().getKind())) {
            var parent = arrayInit.getParent();
            var sibling = parent.getChildren().get(0);

            // Ensure that VarRef is declared as an array
            if (!sibling.getKind().equals("VarRefExpr") || !isDeclaredAsArray(sibling, table)) {
                var message = "Cannot initialize an array without declaring it as an array.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(arrayInit),
                        NodeUtils.getColumn(arrayInit),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    // Helper method to check if a variable is declared as an array
    private boolean isDeclaredAsArray(JmmNode varRef, SymbolTable table) {
        String varName = varRef.get("name");
        for (String method : table.getMethods()) {
            for (Symbol variable : table.getLocalVariables(method)) {
                if (variable.getName().equals(varName) && variable.getType().isArray()) {
                    return true;
                }
            }
        }
        return false;
    }

}