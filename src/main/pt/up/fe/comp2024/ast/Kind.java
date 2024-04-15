package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.Set;

public enum Kind {
    PROGRAM,
    IMPORT_DECL,
    CLASS_DECL,
    VAR_DECL,
    TYPE,
    TYPE_INT,
    TYPE_BOOL,
    TYPE_VOID,
    TYPE_VARIABLE,
    METHOD_DECL,
    PARAM,
    ASSIGN_STMT,
    RETURN_STMT,
    BINARY_EXPR,
    INTEGER_LITERAL,
    LENGTH_ATTR_EXPR,
    VAR_REF_EXPR,
    BOOL_LITERAL,
    PARENTH_EXPR,
    ARRAY_EXPR,
    METHOD,
    METHOD_EXPR,
    EXPR_STMT,
    THIS,
    NEG_EXPR,
    NEW_ARRAY_EXPR,
    INIT_ARRAY_EXPR,
    NEW_OBJ_EXPR,
    IF_STMT,
    WHILE_STMT,
    LIST_ASSIGN_STMT;

    private static final Set<Kind> STATEMENTS = Set.of(ASSIGN_STMT, RETURN_STMT);
    private static final Set<Kind> EXPRESSIONS = Set.of(BINARY_EXPR, INTEGER_LITERAL, VAR_REF_EXPR, LENGTH_ATTR_EXPR);
    private static final Set<Kind> TYPES = Set.of(TYPE_INT, TYPE_BOOL, TYPE_VOID, TYPE_VARIABLE);
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

    public boolean isType() {
        return TYPES.contains(this);
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
