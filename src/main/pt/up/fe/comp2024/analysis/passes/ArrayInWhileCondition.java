package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayInWhileCondition extends AnalysisVisitor {
    private HashMap<String, Boolean> variables = new HashMap<>();
   @Override
    public void buildVisitor() {
       addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
    }
    private Void visitWhileStmt(JmmNode WhileStmt, SymbolTable table) {
        // Prepare a map to store variable names and their array status
        Map<String, Boolean> variables = new HashMap<>();

        // Populate the map with local variables and their array status
        for (String method : table.getMethods()) {
            for (Symbol variable : table.getLocalVariables(method)) {
                variables.put(variable.getName(), variable.getType().isArray());
            }
        }

        // Traverse the condition of the while statement to check for arrays
        JmmNode condition = WhileStmt.getChildren().get(0);
        List<String> varNames = getVariableNames(condition, variables);
        for (String varName : varNames) {
            if (variables.containsKey(varName) && variables.get(varName)) {
                var message = "Cannot have an array in a while loop.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(WhileStmt),
                        NodeUtils.getColumn(WhileStmt),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    // Helper method to find variable names in expressions
    private List<String> getVariableNames(JmmNode node, Map<String, Boolean> variables) {
        List<String> varNames = new ArrayList<>();
        if (node.getKind().equals("VarRefExpr") && node.hasAttribute("name")) {
            varNames.add(node.get("name"));
        } else if (node.getKind().equals("LengthExpr")) {
            // Skip LengthExpr as it represents an integer literal
            return varNames;
        } else {
            for (JmmNode child : node.getChildren()) {
                varNames.addAll(getVariableNames(child, variables));
            }
        }
        return varNames;
    }


}
