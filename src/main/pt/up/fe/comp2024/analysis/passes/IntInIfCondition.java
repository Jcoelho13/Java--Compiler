package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;

import static pt.up.fe.comp2024.ast.Kind.*;

public class IntInIfCondition extends AnalysisVisitor {
    
    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_ELSE_STMT, this::visitIfStmt);
        addVisit(Kind.WHILE_STMT, this::visitIfStmt);
    }

    /**
     * Visits an if statement and checks if there is an integer literal in the condition
     * @param IfStmt The if statement node
     * @param table The symbol table
     * @return null
     */
    private Void visitIfStmt(JmmNode IfStmt, SymbolTable table) {
        for (JmmNode child : IfStmt.getChildren()) {

            // If the child is a binary expression, multiplicative expression or additive expression, then it will result in an integer literal in the condition, so we report an error
            if (child.getKind().equals(BINARY_EXPR.toString()) || child.getKind().equals(MULTIPLICATIVE_EXPR.toString()) || child.getKind().equals(ADDITIVE_EXPR.toString())) {

                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(IfStmt),
                        NodeUtils.getColumn(IfStmt),
                        "Integer literal in if condition must be part of a comparison.",
                        null));

                break;

            }
        }
        return null;
    }
}
