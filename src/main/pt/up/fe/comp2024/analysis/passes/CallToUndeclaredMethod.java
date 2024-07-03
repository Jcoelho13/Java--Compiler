package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import java.util.concurrent.atomic.AtomicBoolean;

public class CallToUndeclaredMethod extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_CALL_EXPR, this::visitMethodCall);
    }

    /**
     * Visit Method Call
     * @param methodCall node
     * @param table symbol table
     * @return null
     */
    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        AtomicBoolean flag = new AtomicBoolean(false);

        // get the name of the class and the imports
        var methodName = methodCall.getAncestor(Kind.CLASS_DECL).get();
        var importNames = table.getImports();

        for (String s : table.getMethods()) {
            for (Symbol localVariable : table.getLocalVariables(s)) {
                importNames.forEach(importName -> {
                    // if type of local variable is the same as the import
                    if (localVariable.getType().getName().equals(importName) ||
                            // or if type of local variable is the same as the name of the class and class is extended from the import, set flag to true
                            (localVariable.getType().getName().equals(methodName.get("name")) && (methodName.hasAttribute("extendedClass") && methodName.get("extendedClass").equals(importName)))) {
                        flag.set(true);
                    }
                });
            }
        }


        // check if the method call is a literal, if it is, set flag to true
        methodCall.getChildren().forEach(method -> {
            if(method.getKind().equals("ThisLiteralExpr") || method.getKind().equals("IntegerLiteralExpr")
                    || method.getKind().equals("StringLiteralExpr") || method.getKind().equals("BoolLiteralExpr")
                    || method.getKind().equals("AdditionExpr") || method.getKind().equals("MultiplicativeExpr")) {
                flag.set(true);
            }
            else {
                // check if the name of the method call is the same as the name of the import, if it is, set flag to true
                table.getImports().forEach(importName -> {
                    if ( method.hasAttribute("name") && method.get("name").equals(importName)) {
                        flag.set(true);
                    }
                });
            }
        });

        table.getMethods().forEach(method -> {
            table.getLocalVariables(method).forEach(localVariable -> {
                table.getImports().forEach(importName -> {
                    // if type of local variable is the same as the import, set flag to true
                    if (localVariable.getType().getName().equals(importName)) {
                        flag.set(true);
                    }
                });
            });
        });

        methodName.getChildren().forEach(method -> {
            // if the name of the method call is the same as the name of the class, set flag to true
                if (method.get("name").equals(methodCall.get("name"))) {
                    flag.set(true);
            }
        });

        // after all the checks, if flag is still false, add a report
        if (!flag.get()) {
            var message = "Call to undeclared method '" + methodCall + "'.";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(methodCall),
                    NodeUtils.getColumn(methodCall),
                    message,
                    null)
            );
        }
        return null;
        }
    }

