package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class Returns extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }
    /**
     * Visit method declaration node and check return statements
     * @param methodDecl method declaration node
     * @param table symbol table
     * @return
     */
    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        var methodName = methodDecl.get("name");

        if(!methodName.equals("main")){
            var hasReturn = false;
            // check if method has a return statement
            for (var stmt : methodDecl.getChildren()) {
                if(Kind.RETURN_STMT.check(stmt)){
                    hasReturn = true;
                    break;
                }
            }
            // if method does not have a return statement, add error report
            if(!hasReturn){
                var message = "Method does not have a return statement.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(methodDecl),
                        NodeUtils.getColumn(methodDecl),
                        message,
                        null)
                );
            }
        }

        //check for more than one return statement
        var returnCount = 0;
        for (var stmt : methodDecl.getChildren()) {
            if(Kind.RETURN_STMT.check(stmt)){
                returnCount++;
            }
        }
        // if method has more than one return statement, add error report
        if(returnCount > 1){
            var message = "Method has more than one return statement.";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl),
                    message,
                    null)
            );
        }

        return null;
    }
}
