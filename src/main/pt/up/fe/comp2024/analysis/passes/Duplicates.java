package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;
import java.util.Set;

public class Duplicates extends AnalysisVisitor {
    // Data structures to store unique elements
    private final Set<String> uniqueImports = new HashSet<>();
    private final Set<String> uniqueMethods = new HashSet<>();
    private final Set<String> uniqueFields = new HashSet<>();
    private final Set<String> uniqueParams = new HashSet<>();
    private final Set<String> uniqueLocals = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.IMPORT_STMT, this::visitImportStmt);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
    }

    /**
     * Visit an import statement node.
     * @param importStmt The import statement node.
     * @param table The symbol table.
     * @return null
     */
    private Void visitImportStmt(JmmNode importStmt, SymbolTable table) {
        var importName = importStmt.get("name").substring(1, importStmt.get("name").length() - 1);

        // Check for duplicate imports
        if(!uniqueImports.add(importName)){
            var message = "Duplicate import statement.";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(importStmt),
                    NodeUtils.getColumn(importStmt),
                    message,
                    null)
            );
        }
        return null;
    }

    /**
     * Visit a method declaration node.
     * @param methodDecl The method declaration node.
     * @param table The symbol table.
     * @return null
     */
    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        var methodName = methodDecl.get("name");

        // Check for duplicate method declarations
        if(!uniqueMethods.add(methodName)){
            var message = "Duplicate method declaration.";
            addReport(Report.newError(Stage.SEMANTIC,
                    NodeUtils.getLine(methodDecl),
                    NodeUtils.getColumn(methodDecl),
                    message,
                    null)
            );
        }

        // Check for duplicate parameters
        for (var param : table.getParameters(methodName)) {
            if(!uniqueParams.add(param.getName())){
                var message = "Duplicate parameter declaration.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(methodDecl),
                        NodeUtils.getColumn(methodDecl),
                        message,
                        null)
                );
            }
        }

        // Check for duplicate local variables using a pair of variable and method name
        for (var local : table.getLocalVariables(methodName)) {
            if(!uniqueLocals.add(local.getName() + methodName)){
                var message = "Duplicate local variable declaration.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(methodDecl),
                        NodeUtils.getColumn(methodDecl),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    /**
     * Visit a class declaration
     * @param classDecl The class declaration
     * @param table The symbol table
     * @return null
     */
    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {
        // Check for duplicate field declarations
        for (var field : table.getFields()) {
            if(!uniqueFields.add(field.getName())){
                var message = "Duplicate field declaration.";
                addReport(Report.newError(Stage.SEMANTIC,
                        NodeUtils.getLine(classDecl),
                        NodeUtils.getColumn(classDecl),
                        message,
                        null)
                );
            }
        }
        return null;
    }
}