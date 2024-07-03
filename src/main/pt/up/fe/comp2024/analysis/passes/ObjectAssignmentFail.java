package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashMap;

public class ObjectAssignmentFail extends AnalysisVisitor {
    // This hashmap will store the local variables of the method
    private HashMap<String, String> localVariables = new HashMap<>();
    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGNMENT_EXPR, this::visitAssignStmt);
    }

    /**
     * This method will check if the assignment is valid
     * @param assignStmt The node that represents the assignment
     * @param table The symbol table
     * @return null
     */
    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {

        var imports = table.getImports();
        // get the ClassDecl node
        var classInfo = assignStmt.getAncestor(Kind.CLASS_DECL).get();
        table.getMethods().forEach(method -> {
            // get the local variables of the method, and store them in the hashmap
            table.getLocalVariables(method).forEach(localVariable -> {
                localVariables.put(localVariable.getName(), localVariable.getType().getName());
            });
        });
        // check if the assignment is valid
        if (assignStmt.get("op").equals("=")) {
            var children = assignStmt.getChildren();
            // check if the types are compatible
            if(children.get(0).getKind().equals(children.get(1).getKind())){
                // if the assignment is an array access, check if the array is a local variable
                if(children.get(0).getKind().equals(Kind.ARRAY_ACCESS_EXPR.toString())){
                    if(localVariables.containsKey(children.get(0).getChildren().get(0).get("name"))){
                        return null;
                    }
                }

                // checks if the right-hand side of the assignment is a local variable.
                if(localVariables.containsKey(children.get(1).get("name"))){
                    // checks if  the name of the class is the same as the type of the local variable
                   if(classInfo.get("name").equals(localVariables.get(children.get(1).get("name")))){
                       // if the class extends another class, check if the right-hand side of the assignment is an instance of the extended class
                       if(classInfo.hasAttribute("extendedClass") && classInfo.get("extendedClass").equals(localVariables.get(children.get(0).get("name")))){
                           return null;
                       }
                       else {
                            var message = "Incompatible assignment Types";
                            addReport(Report.newError(Stage.SEMANTIC,
                                      NodeUtils.getLine(assignStmt),
                                      NodeUtils.getColumn(assignStmt),
                                      message,
                                      null)
                            );
                       }
                   }
                }
            }
        }
            return null;
        }
    }

