package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ArrayIndexNotInt extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccess);
    }

    /**
     * Check if the array access is valid
     * @param ArrayAccess The array access node
     * @param table The symbol table
     * @return null
     */

    private Void visitArrayAccess(JmmNode ArrayAccess, SymbolTable table) {
        // fetch the array and index nodes
        var array = ArrayAccess.getChildren().get(0);
        var index = ArrayAccess.getChildren().get(1);

        // get the method name
        var method = ArrayAccess.getAncestor(Kind.METHOD_DECL).get().get("name");
        var getValue = "name";

        for (Symbol variable : table.getLocalVariables(method)) {

            // check if an array access is being done on a variable that is not an array, if so, report an error
            if ((ArrayAccess.getChildren().get(0).get("name").equals(variable.getName())
                    && !variable.getType().isArray())) {
                var message = "Can't access an integer as array.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(ArrayAccess),
                        NodeUtils.getColumn(ArrayAccess),
                        message,
                        null)
                );
                return null;
            }
            // if the index is an integer literal, get the value of the integer
            if (index.getKind().equals(Kind.INTEGER_LITERAL_EXPR.toString())) {
                getValue = "value";
            }
            // if the index has a value/name and the variable name is the same as the index name, check if the index is an integer, else add report
            if (index.hasAttribute(getValue) && variable.getName().equals(index.get(getValue))) {
                if (!variable.getType().getName().equals("int")) {
                    var message = "Array index must be an integer.";
                    addReport(Report.newError(Stage.SEMANTIC,
                            NodeUtils.getLine(ArrayAccess),
                            NodeUtils.getColumn(ArrayAccess),
                            message,
                            null)
                    );
                }
                // if the index is the array itself, report an error
                else if (variable.getName().equals(array.get("name"))) {
                    var message = "Array cannot be indexed by itself.";
                    addReport(Report.newError(Stage.SEMANTIC,
                            NodeUtils.getLine(ArrayAccess),
                            NodeUtils.getColumn(ArrayAccess),
                            message,
                            null)
                    );
                }
            }
        }

        // ensure that .length can not be accessed by fields
        var fields = table.getFields();
        for (Symbol field : fields) {
            if (field.getName().equals(array.get("name")) && index.getKind().equals(Kind.METHOD_CALL_EXPR.toString())) {
                var message = "Cannot access .length from a field.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(ArrayAccess),
                        NodeUtils.getColumn(ArrayAccess),
                        message,
                        null)
                );
            }
        }

        return null;
    }
}
