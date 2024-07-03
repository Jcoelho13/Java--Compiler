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


public class Varargs extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.VARARG, this::visitVarargs);
    }

    /**
     * Visits a Vararg node and checks if it is used correctly.
     * @param varargs The Vararg node to visit.
     * @param table The symbol table.
     * @return null
     */
    private Void visitVarargs(JmmNode varargs, SymbolTable table) {
        for (var method : table.getMethods()) {

            // Ensure no method has a vararg as a return type
            if (table.getReturnType(method).hasAttribute("vararg")) {
                var message = "Method return type cannot be a Vararg.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varargs),
                        NodeUtils.getColumn(varargs),
                        message,
                        null)
                );
                return null;
            }

            // Get the parameters of the method
            List<Symbol> params = table.getParameters(method);
            boolean hasVarArg = false;
            String varargName = "";

            // Check if any parameter in the method is a vararg
            for (Symbol param : params) {
                System.out.println(param.getType().toString());
                if (param.getType().isArray() && varargs.getParent().get("name").equals(param.getName())) {
                    hasVarArg = true;
                    varargName = param.getName();
                    break;
                }
            }

            // If the method has no vararg parameter, skip the rest of the checks
            if (!hasVarArg) {
                continue;
            }

            if (!params.isEmpty()) {
                // Ensure the last parameter is an array type and matches the vararg node name
                Symbol lastParam = params.get(params.size() - 1);
                varargName = varargs.getParent().get("name");
                if (!lastParam.getType().isArray() || !lastParam.getName().equals(varargName)) {
                    var message = "Vararg parameter must be the last parameter.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(varargs),
                            NodeUtils.getColumn(varargs),
                            message,
                            null)
                    );
                    return null;
                }
            }
        }
        return null;
    }
}
