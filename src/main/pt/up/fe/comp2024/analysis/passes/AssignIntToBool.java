package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashMap;

public class AssignIntToBool extends AnalysisVisitor {
    private HashMap<String, String> localVariables = new HashMap<>();
    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGNMENT_EXPR, this::visitAssignStmt);
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {

        // get all methods
        var methods = table.getMethods();

        // place all local variables in the hashmap
        for (var method : methods){
            for (var localVariable : table.getLocalVariables(method)){
                localVariables.put(localVariable.getName(), localVariable.getType().getName());
            }
        }

        // get the left and right side of the assignment
        var firstChild = assignStmt.getChildren().get(0);
        var secondChild = assignStmt.getChildren().get(1);
        var name = "";


        // check if the left side is an array access, if so get the name of the array, otherwise get the name of the variable
        if (firstChild.getKind().equals("ArrayAccessExpr")) {
            name = firstChild.getChildren().get(0).get("name");
        }
        else {
            name = firstChild.get("name");
        }

        // get the kind of the right side of the assignment
        var value = secondChild.getKind();

        // check if the variable is in the hashmap and if the types are incompatible
        if((localVariables.containsKey(name) && localVariables.get(name).equals("boolean") && value.equals("IntegerLiteralExpr")) ||
                localVariables.containsKey(name) && localVariables.get(name).equals("int") && value.equals("BooleanLiteralExpr")) {
            var message = "Incompatible assignment Types";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
