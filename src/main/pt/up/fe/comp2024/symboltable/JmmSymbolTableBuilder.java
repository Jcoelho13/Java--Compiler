package pt.up.fe.comp2024.symboltable;

import org.antlr.runtime.misc.IntArray;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    private static String convertImport(List<String> imports) {

        return String.join(".", imports);
    }

    public static JmmSymbolTable build(JmmNode root) {

        var imports = root.getChildren(Kind.IMPORT_STMT).stream()
                .map(importNode -> convertImport(importNode.getObjectAsList("name", String.class)))
                .toList();

        var classDecl = root.getObject("cls", JmmNode.class);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superclassName = classDecl.getOptionalObject("extendedClass")
                .map(Object::toString)
                .orElse(null);

        var methods = buildMethods(classDecl);
        var fields = classDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getChildren(TYPE).get(0)), varDecl.get("name")))
                .toList();
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, methods, fields, returnTypes, params, locals, imports, superclassName);
    }

    private static Type getType(JmmNode typeNode) {
        Kind kind = Kind.fromString(typeNode.getKind());

        if (kind == ARRAY_TYPE) {
            return new Type(getType(typeNode.getObject("nameType", JmmNode.class)).getName(), true);

        } else if (kind == VARARG) {
            var varargType = new Type(getType(typeNode.getObject("nameType", JmmNode.class)).getName(), true);
            varargType.putObject("vararg", true);
            return varargType;
        } else {
            return new Type(typeNode.get("name"), false);
        }
    }
    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {

        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> {
                    if (method.get("name").equals("main")) {
                        map.put(method.get("name"), new Type("void", false));
                    }
                    else if (PARAM.check(method.getJmmChild(1)) && VARARG.check(method.getJmmChild(1).getJmmChild(0))) {
                        map.put(method.get("name"), new Type(getType(method.getChildren(TYPE).get(0)) + " ,varargs", true));
                    }
                    else {
                        map.put(method.get("name"), getType(method.getChildren(TYPE).get(0)));
                    }
                });

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), method.getChildren(PARAM).stream()
                        .map(param -> new Symbol(getType(param.getChildren(TYPE).get(0)), param.get("name")))
                        .toList()));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)) map.put(method.get("name"), getLocalsList(method));

        return map;
    }

    private static List<String> buildMethods(JmmNode methodDecl) {

        return methodDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(getType(varDecl.getChildren(TYPE).get(0)), varDecl.get("name")))
                .toList();
    }

}
