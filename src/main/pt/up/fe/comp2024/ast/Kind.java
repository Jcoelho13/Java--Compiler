package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.Set;

public enum Kind {
    PROGRAM,
    CLASS_DECL,
    VAR_DECL,
    TYPE,
    TTYPE,
    METHOD_DECL,
    MAIN_METHOD,
    PARAM,
    ASSIGN_STMT,
    RETURN_STMT,
    BINARY_EXPR,
    INTEGER_LITERAL,
    BOOLEAN_LITERAL,
    IMPORT_STMT,
    ARRAY_TYPE,
    VAR_REF_EXPR,
    MULTIPLICATIVE_EXPR,
    ADDITIVE_EXPR,
    ARRAY_INIT_EXPR,
    ARRAY_ACCESS_EXPR,
    NEW_ARRAY_EXPR,
    IF_ELSE_STMT,
    ASSIGNMENT_EXPR,
    WHILE_STMT,
    METHOD_CALL_EXPR,
    VARARG,
    LENGTH_EXPR,
    EXPR_STMT,
    INTEGER_LITERAL_EXPR,
    BOOLEAN_LITERAL_EXPR,
    NEW_CLASS_EXPR,
    RELATIONAL_EXPR,
    THIS_LITERAL_EXPR,
    UNARY_EXPR,
    SCOPE_STMT,
    SHORT_C_AND_EXPR,
    PAREN_EXPR;


    private static final Set<Kind> STATEMENTS = Set.of(ASSIGN_STMT, RETURN_STMT, WHILE_STMT, IF_ELSE_STMT);
    private static final Set<Kind> EXPRESSIONS = Set.of(BINARY_EXPR, INTEGER_LITERAL, VAR_REF_EXPR, MULTIPLICATIVE_EXPR, ADDITIVE_EXPR, ARRAY_INIT_EXPR, ARRAY_ACCESS_EXPR, METHOD_CALL_EXPR, LENGTH_EXPR, INTEGER_LITERAL_EXPR);

    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {

        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * @return true if this kind represents a statement, false otherwise
     */
    public boolean isStmt() {
        return STATEMENTS.contains(this);
    }

    /**
     * @return true if this kind represents an expression, false otherwise
     */
    public boolean isExpr() {
        return EXPRESSIONS.contains(this);
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.getKind().equals(getNodeName());
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {

        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {

            // if any matches, return successfully
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}