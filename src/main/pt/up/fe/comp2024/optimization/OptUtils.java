package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static pt.up.fe.comp2024.ast.Kind.*;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {
        String typeName = "";
        if (typeNode.getKind().equals(TTYPE.toString())) {
            typeName = typeNode.get("name");
        }
        else if (typeNode.getKind().equals(ARRAY_TYPE.toString())) {
            typeName = "array" + toOllirType(typeNode.getChildren().get(0));
        }
        else if (typeNode.getKind().equals(VARARG.toString())) {
            typeName = "array" + toOllirType(typeNode.getChildren().get(0));
        }
        if (typeName.isEmpty()) {
            throw new NotImplementedException(typeName);
        }

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        if (type.isArray()) {
            return ".array" + toOllirType(type.getName());
        }
        return toOllirType(type.getName());
    }

    private static String toOllirType(String typeName) {

        if(typeName.contains("varargs")) {
            return ".i32";
        }

        String type = "." + switch (typeName) {
            case "int" -> "i32";
            case "void" -> ".V";
            case "boolean" -> "bool";
            case "String" -> "string";
            case "array" -> "array";
            case "this" -> "this";
            default -> typeName;
        };

        return type;
    }


}