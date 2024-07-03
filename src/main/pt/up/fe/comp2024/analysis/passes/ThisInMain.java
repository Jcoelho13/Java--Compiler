package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class ThisInMain extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.THIS_LITERAL_EXPR, this::visitThisLiteralExpr);
    }

    /**
     * Visits a THIS_LITERAL_EXPR node and checks if it is in the main method
     * @param thisLiteralExpr The THIS_LITERAL_EXPR node
     * @param table           The symbol table
     * @return null
     */
    private Void visitThisLiteralExpr(JmmNode thisLiteralExpr, SymbolTable table) {
        AtomicBoolean flag = new AtomicBoolean(false);

        // get the method name where the THIS_LITERAL_EXPR is
        var methodName = thisLiteralExpr.getAncestor(Kind.METHOD_DECL).get();

        // set flag to true if the method is the main method
        if (methodName.get("name").equals("main")) {
            flag.set(true);
        }

        // if the flag is true, add an error report
        if (flag.get()) {
            var message = "Cannot use 'this' in main method";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(thisLiteralExpr),
                    NodeUtils.getColumn(thisLiteralExpr),
                    message,
                    null)
            );
        }

        return null;
    }
}
