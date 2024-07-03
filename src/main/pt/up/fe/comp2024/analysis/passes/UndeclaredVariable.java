package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }

    /**
     * Visits a method declaration node and checks if the return statement is the last statement in the method.
     *
     * @param method The method declaration node.
     * @param table The symbol table.
     * @return null
     */
    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        if(currentMethod.equals("length")){
            return null;
        }
        // check if Kind.ReturnStmt is the last child of the method
        var children = method.getChildren();
        var size = children.size();
        for(int i = 0; i < size; i++){
            if(children.get(i).getKind().equals(Kind.RETURN_STMT.toString())){
                if(i==size-1){
                    break;
                }
                else{
                    var message = "Return statement must be the last statement in the method.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(children.get(i)),
                            NodeUtils.getColumn(children.get(i)),
                            message,
                            null)
                    );
                }
            }
        }
        return null;
    }

    /**
     * Visits a variable reference expression node and checks if the variable exists in the symbol table.
     *
     * @param varRefExpr The variable reference expression node.
     * @param table The symbol table.
     * @return null
     */
    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");
        if(varRefName.equals("length")){
            return null;
        }

        // Var is a field, return
        if (table.getFields().stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getImports().stream().anyMatch(imported -> imported.equals(varRefName))){
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(varRefExpr),
                NodeUtils.getColumn(varRefExpr),
                message,
                null)
        );

        return null;
    }


}
