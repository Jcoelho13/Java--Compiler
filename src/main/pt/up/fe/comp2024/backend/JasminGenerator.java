package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    /**
     * Represents a line break in Jasmin code.
     */
    private static final String NL = "\n";

    /**
     * Represents a tab in Jasmin code.
     */
    private static final String TAB = "   ";

    /**
     * The OllirResult containing the OLLIR representation of the program.
     */
    private final OllirResult ollirResult;

    /**
     * List of reports generated during code generation.
     */
    List<Report> reports;

    /**
     * The generated Jasmin code.
     */
    String code;

    /**
     * The current method being processed.
     */
    Method currentMethod;

    /**
     * The current class being processed.
     */
    ClassUnit currentClass;

    /**
     * A map of imported classes, with class names as keys and import paths as values.
     */
    HashMap<String, String> imports = new HashMap<>();

    /**
     * Counter for less-than branches in conditional statements.
     */
    private int lessThenBranchCounter = 0;

    /**
     * Counter for less-than-or-equal-to branches in conditional statements.
     */
    private int lessThenEqualBranchCounter = 0;

    /**
     * Counter for greater-than-or-equal-to branches in conditional statements.
     */
    private int greaterThenEqualBranchCounter = 0;

    /**
     * Counter for greater-than branches in conditional statements.
     */
    private int greaterThenBranchCounter = 0;

    /**
     * Counter for equal-to branches in conditional statements.
     */
    private int equalBranchCounter = 0;

    /**
     * Counter for not-equal-to branches in conditional statements.
     */
    private int notEqualBranchCounter = 0;

    /**
     * The current stack size during code generation.
     */
    int stackSize = 0;

    /**
     * The maximum stack size encountered during code generation.
     */
    int maxStackSize = 0;

    /**
     * Map containing generator functions for different types of OLLIR elements.
     */
    private final FunctionClassMap<TreeNode, String> generators;

    /**
     * Initializes a JasminGenerator with the given OllirResult.
     *
     * @param ollirResult The OllirResult containing the OLLIR representation of the program.
     */
    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.reports = new ArrayList<>();
        this.code = null;
        this.currentMethod = null;
        this.generators = new FunctionClassMap<>();
        initializeGenerators();
    }

    /**
     * Initializes the function class map for generating Jasmin code based on different OLLIR elements.
     * Associates each OLLIR element type with its respective generator function.
     */
    private void initializeGenerators() {
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(Field.class, this::generateField);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(CondBranchInstruction.class, this::generateCondBranch);
    }


    public List<Report> getReports() {
        return reports;
    }

    /**
     * Builds the import map based on the imports specified in the given class unit.
     *
     * @param classUnit The class unit containing the imports.
     */
    public void buildImportMap(ClassUnit classUnit) {
        imports = new HashMap<>();
        for (String importUnit : classUnit.getImports()) {
            String importedPath = importUnit.replace(".", "/");
            String importedClassName = importUnit.substring(importUnit.lastIndexOf(".") + 1).replace(";", "");
            imports.put(importedClassName, importedPath);
        }
    }

    /**
     * Gets the imported path for the specified class name.
     * If the class name is not found in the import map, returns the class name itself.
     *
     * @param className The name of the class to get the imported path for.
     * @return The imported path for the class name, or the class name itself if not found in the import map.
     */
    public String getImportedClass(String className) {
        return imports.getOrDefault(className, className);
    }

    /**
     * Checks the current stack size against the maximum stack size encountered so far.
     * If the current stack size exceeds the maximum stack size, updates the maximum stack size.
     */
    public void checkStackSize(){
        if(stackSize > maxStackSize){
            maxStackSize = stackSize;
        }
    }

    /**
     * Builds the Jasmin assembly code for the OLLIR class.
     *
     * @return The assembled Jasmin code as a string.
     */
    public String build() {
        if (code == null) {
            // Build the import map
            this.buildImportMap(ollirResult.getOllirClass());
            // Generate Jasmin assembly code for the OLLIR class
            code = generators.apply(ollirResult.getOllirClass());
        }
        return code;
    }

    /**
     * Generates the assembly code for a class unit.
     *
     * @param classUnit The class unit to generate code for.
     * @return A string representing the generated assembly code.
     */
    private String generateClassUnit(ClassUnit classUnit) {
        currentClass = classUnit;
        StringBuilder code = new StringBuilder();

        // Generate class name
        String className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);

        // Handle inheritance
        String superClass = classUnit.getSuperClass();
        if (superClass != null) {
            String superClassName = getImportedClass(superClass);
            code.append(".super ").append(superClassName).append(NL).append(NL);
        } else {
            code.append(".super java/lang/Object").append(NL).append(NL);
        }

        // Generate fields
        for (Field field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field)).append(NL);
        }

        // Generate a default constructor if no superclass exists
        if (superClass == null) {
            String defaultConstructor = """
            ; Default constructor
            .method public <init>()V
                aload_0
                invokespecial java/lang/Object/<init>()V
                return
            .end method
            """;
            code.append(defaultConstructor);
        }

        // Generate code for all other methods
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            // Skip constructor as it has already been added
            if (method.isConstructMethod()) {
                continue;
            }
            code.append(generators.apply(method));
        }

        return code.toString();
    }

    /**
     * Generates the Jasmin assembly code for a given method.
     *
     * @param method The method for which to generate the code.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateMethod(Method method) {
        // Set the current method context
        currentMethod = method;

        var code = new StringBuilder();
        var ephemeralCode = new StringBuilder();

        // Calculate the method access modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        // Determine the method signature
        if (methodName.equals("main")) {
            // Special case for the main method
            code.append(".method public static main([Ljava/lang/String;)V").append(NL);
        } else {
            var staticModifier = method.isStaticMethod() ? "static " : "";
            var methodRType = getJasminType(method.getReturnType());

            // Join the parameters' types into a single string
            var methodParams = method.getParams().stream()
                    .map(param -> getJasminType(param.getType()))
                    .collect(Collectors.joining("", "(", ")"));

            // Append the method signature to the code
            code.append(".method ")
                    .append(modifier)
                    .append(staticModifier)
                    .append(methodName)
                    .append(methodParams)
                    .append(methodRType)
                    .append(NL);
        }

        // Initialize limit values
        var limitLocals = method.getVarTable().values().stream()
                .mapToInt(Descriptor::getVirtualReg)
                .max().orElse(0) + 1;

        // Process each instruction in the method
        method.getInstructions().forEach(instruction -> {
            var joiner = new StringJoiner(NL + TAB, TAB, NL);

            // Add labels for the instruction
            method.getLabels().forEach((key, value) -> {
                if (value.equals(instruction)) {
                    joiner.add(key + ":");
                }
            });

            // Generate code for the instruction and add it
            StringLines.getLines(generators.apply(instruction)).forEach(joiner::add);
            ephemeralCode.append(joiner);

            // Adjust the stack by popping as necessary
            for (int i = 0; i < stackSize; i++) {
                ephemeralCode.append(TAB).append("pop").append(NL);
            }
            stackSize = 0;
        });

        // Set the limit for the stack size
        var limitStack = maxStackSize;
        maxStackSize = 0;

        // Append the .limit directives
        code.append(TAB).append(".limit stack ").append(limitStack).append(NL);
        code.append(TAB).append(".limit locals ").append(limitLocals).append(NL);

        // Append the generated code for the method's instructions
        code.append(ephemeralCode);

        // End the method definition
        code.append(".end method").append(NL);

        // Unset the current method context
        currentMethod = null;

        return code.toString();
    }

    /**
     * Generates the Jasmin assembly code for an assignment instruction.
     *
     * @param assign The assignment instruction to generate the code for.
     * @return The generated Jasmin assembly code as a string.
     * @throws Error if the current method is not set.
     */
    private String generateAssign(AssignInstruction assign) {
        // Ensure the current method is set
        if (currentMethod == null) {
            throw new Error("Method not set");
        }

        var code = new StringBuilder();
        var lhs = assign.getDest();

        // Validate the left-hand side operand
        if (!(lhs instanceof Operand operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        // Check if the RHS is a binary operation that can be converted to iinc
        if (assign.getRhs() instanceof BinaryOpInstruction binOp) {
            var leftOperand = binOp.getLeftOperand();
            var rightOperand = binOp.getRightOperand();
            var opType = binOp.getOperation().getOpType();

            // Check if it's an increment or decrement operation
            if ((opType == OperationType.ADD || opType == OperationType.SUB)
                    && leftOperand instanceof Operand
                    && rightOperand instanceof LiteralElement
                    && ((Operand) leftOperand).getName().equals(operand.getName())
            ) {

                int constant = Integer.parseInt(((LiteralElement) rightOperand).getLiteral());

                // Negate the constant for decrement operations
                if (opType == OperationType.SUB) {
                    constant = -constant;
                }

                // Get the register of the operand
                int reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

                // Generate the iinc instruction
                code.append("iinc ").append(reg).append(" ").append(constant).append(NL);
                return code.toString();
            }
        }

        // Get the register of the operand
        int reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // Handle array operands
        if (operand instanceof ArrayOperand arrayElement) {
            // Load array
            code.append(reg > 3 ? "aload " : "aload_").append(reg).append(NL);

            // Load index
            code.append(generators.apply(arrayElement.getIndexOperands().get(0)));

            stackSize++;
            checkStackSize();
        }

        // Generate code for the RHS
        code.append(generators.apply(assign.getRhs()));

        // Handle storing in array
        if (operand instanceof ArrayOperand) {
            code.append("iastore").append(NL);

            stackSize -= 3;
            checkStackSize();

            return code.toString();
        }

        // Store value in the stack in destination
        var elementType = assign.getTypeOfAssign().getTypeOfElement();
        switch (elementType) {
            case INT32, BOOLEAN -> code.append(reg > 3 ? "istore " : "istore_").append(reg).append(NL);
            case STRING, OBJECTREF, ARRAYREF -> code.append(reg > 3 ? "astore " : "astore_").append(reg).append(NL);
            case VOID -> {
                // Do nothing for VOID type
            }
            default -> throw new NotImplementedException(elementType);
        }

        stackSize--;
        checkStackSize();

        return code.toString();
    }

    /**
     * Generates the Jasmin assembly code for a single operand instruction.
     *
     * @param singleOp The single operand instruction to generate the code for.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    /**
     * Generates the Jasmin assembly code for a literal element.
     *
     * @param literal The literal element to generate the code for.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateLiteral(LiteralElement literal) {
        var code = new StringBuilder();

        // Determine the type of the literal element and generate the appropriate code
        var elementType = literal.getType().getTypeOfElement();
        switch (elementType) {
            case INT32, BOOLEAN -> {
                int value = Integer.parseInt(literal.getLiteral());
                if (value == -1) {
                    code.append("iconst_m1").append(NL);
                } else if (value >= 0 && value <= 5) {
                    code.append("iconst_").append(value).append(NL);
                } else if (value >= -128 && value <= 127) {
                    code.append("bipush ").append(value).append(NL);
                } else if (value >= -32768 && value <= 32767) {
                    code.append("sipush ").append(value).append(NL);
                } else {
                    code.append("ldc ").append(value).append(NL);
                }
            }
            case STRING -> code.append("ldc ").append(literal.getLiteral()).append(NL);
            case VOID -> {
                // Do nothing for VOID type
            }
            default -> throw new NotImplementedException(elementType);
        }

        stackSize++;
        checkStackSize();
        return code.toString();
    }

    /**
     * Generates the Jasmin assembly code for an operand.
     *
     * @param operand The operand to generate the code for.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateOperand(Operand operand) {
        var code = new StringBuilder();
        // Get the register associated with the operand
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        stackSize++;
        checkStackSize();

        if (operand instanceof ArrayOperand arrayElement) {
            appendArrayLoad(code, reg);
            code.append(generators.apply(arrayElement.getIndexOperands().get(0)))
                    .append("iaload")
                    .append(NL);
            stackSize++;
            checkStackSize();
            stackSize -=2;
        } else {
            appendOperandLoad(code, operand, reg);
        }

        return code.toString();
    }

    /**
     * Appends the Jasmin code to load an array element.
     *
     * @param code The StringBuilder to append the code to.
     * @param reg The register number.
     */
    private void appendArrayLoad(StringBuilder code, int reg) {
        if (reg > 3) {
            code.append("aload ").append(reg).append(NL);
        } else {
            code.append("aload_").append(reg).append(NL);
        }
    }

    /**
     * Appends the Jasmin code to load a non-array operand.
     *
     * @param code The StringBuilder to append the code to.
     * @param operand The operand to generate the code for.
     * @param reg The register number.
     */
    private void appendOperandLoad(StringBuilder code, Operand operand, int reg) {
        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                if (reg > 3) {
                    code.append("iload ").append(reg).append(NL);
                } else {
                    code.append("iload_").append(reg).append(NL);
                }
            }
            case VOID -> {
                // Do nothing for VOID type
            }
            case OBJECTREF, STRING, ARRAYREF -> {
                if (reg > 3) {
                    code.append("aload ").append(reg).append(NL);
                } else {
                    code.append("aload_").append(reg).append(NL);
                }
            }
            case THIS -> code.append("aload_0").append(NL);
            default -> throw new NotImplementedException(operand.getType());
        }
    }

    /**
     * Generates the Jasmin assembly code for a binary operation instruction.
     *
     * @param binaryOp The binary operation instruction to generate the code for.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // Load values of the left and right operands
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // Generate code for the binary operation
        switch (binaryOp.getOperation().getOpType()) {
            case ADD -> code.append("iadd").append(NL);
            case SUB -> code.append("isub").append(NL);
            case MUL -> code.append("imul").append(NL);
            case DIV -> code.append("idiv").append(NL);
            case ANDB, AND -> code.append("iand").append(NL);
            case LTH -> generateComparisonOp(code, "iflt", "lessThenBranch_", lessThenBranchCounter++);
            case LTE -> generateComparisonOp(code, "ifle", "lessThenEqualBranch_", lessThenEqualBranchCounter++);
            case GTH -> generateComparisonOp(code, "ifgt", "greaterThenBranch_", greaterThenBranchCounter++);
            case GTE -> generateComparisonOp(code, "ifge", "greaterThenEqualBranch_", greaterThenEqualBranchCounter++);
            case EQ -> generateComparisonOp(code, "ifeq", "equalBranch_", equalBranchCounter++);
            case NEQ -> generateComparisonOp(code, "ifne", "notEqualBranch_", notEqualBranchCounter++);
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        }

        stackSize--;
        checkStackSize();

        return code.toString();
    }

    /**
     * Generates the Jasmin assembly code for a comparison operation.
     *
     * @param code The StringBuilder to append the code to.
     * @param comparison The comparison instruction (e.g., "iflt").
     * @param branchPrefix The prefix for the branch labels.
     * @param branchCounter The counter for the branch labels.
     */
    private void generateComparisonOp(StringBuilder code, String comparison, String branchPrefix, int branchCounter) {
        code.append("isub").append(NL)
                .append(comparison).append(" ").append(branchPrefix).append(branchCounter).append(NL)
                .append("iconst_0").append(NL)
                .append("goto end").append(branchPrefix).append(branchCounter).append(NL)
                .append(branchPrefix).append(branchCounter).append(":").append(NL)
                .append("iconst_1").append(NL)
                .append("end").append(branchPrefix).append(branchCounter).append(":").append(NL);
    }

    /**
     * Generates the Jasmin assembly code for a unary operation instruction.
     *
     * @param unaryOp The unary operation instruction to generate the code for.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();

        // Load the operand value
        code.append(generators.apply(unaryOp.getOperand()));

        // Apply the unary operation based on the operation type
        switch (unaryOp.getOperation().getOpType()) {
            case NOT, NOTB -> {
                // NOT operation (logical complement). XOR with 1.
                code.append("iconst_1").append(NL);
                stackSize++; // Increment stack size for the constant
                checkStackSize(); // Check stack size immediately after pushing constant
                code.append("ixor").append(NL);
                stackSize--; // Decrement stack size after the XOR operation
                checkStackSize(); // Ensure stack size is correct after operation
            }
            default -> throw new NotImplementedException(unaryOp.getOperation().getOpType());
        }

        return code.toString();
    }

    /**
     * Generates the Jasmin assembly code for a return instruction.
     *
     * @param returnInst The return instruction to process.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();
        Element operand = returnInst.getOperand();

        // Handle return for 'main' method or when no operand is provided
        if (operand == null || "main".equals(currentMethod.getMethodName())) {
            // Optionally push default value if there is an operand (non-void methods except main)
            if (operand != null) {
                code.append("iconst_0").append(NL); // Push integer 0 for non-void returns in non-main methods
            }
            code.append("return").append(NL); // Return void from method
            return code.toString();
        }

        // Generate return code based on the type of the operand
        switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                code.append(generators.apply(operand)); // Generate code to load integer or boolean
                code.append("ireturn").append(NL); // Return integer from method
                stackSize--; // Adjust the stack size for returning a value
            }
            case STRING, THIS, ARRAYREF, OBJECTREF -> {
                code.append(generators.apply(operand)); // Generate code to load reference
                code.append("areturn").append(NL); // Return reference from method
                stackSize--; // Adjust the stack size for returning a reference
            }
            case VOID -> code.append("return").append(NL); // Return void (no stack size change)
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        }

        checkStackSize();

        return code.toString();
    }


    /**
     * Generates Jasmin assembly code for method calls, including handling object or array creation,
     * and array length retrieval based on the type of invocation specified in the call instruction.
     *
     * @param call The call instruction detailing the type of call, the method or constructor to invoke,
     *             and the arguments to pass.
     * @return The generated Jasmin assembly code as a string.
     */
    private String generateCall(CallInstruction call) {
        StringBuilder code = new StringBuilder();

        return switch (call.getInvocationType()) {
            case NEW -> handleNewInstance(code, call);
            case arraylength -> handleArrayLength(code, call);
            default -> handleMethodInvocation(code, call);
        };
    }

    /**
     * Handles creation of new objects or arrays. Generates appropriate Jasmin code
     * for 'new' keyword usage or array creation.
     *
     * @param code A StringBuilder to which the generated code is appended.
     * @param call The call instruction containing details about the object or array to create.
     * @return A string representing the Jasmin assembly code for creating an object or array.
     */
    private String handleNewInstance(StringBuilder code, CallInstruction call) {
        if (call.getReturnType() instanceof ClassType classType) {
            code.append("new ").append(getImportedClass(classType.getName())).append(NL);
        } else if (call.getReturnType() instanceof ArrayType) {
            code.append(generators.apply(call.getArguments().get(0)));
            code.append("newarray int").append(NL);
            stackSize--;
            checkStackSize();
        }
        return code.toString();
    }

    /**
     * Generates code for retrieving the length of an array.
     *
     * @param code A StringBuilder to which the generated code is appended.
     * @param call The call instruction that specifies the array whose length is to be retrieved.
     * @return A string representing the Jasmin assembly code for retrieving array length.
     */
    private String handleArrayLength(StringBuilder code, CallInstruction call) {
        code.append(generators.apply(call.getCaller()));
        code.append("arraylength").append(NL);
        stackSize--;
        checkStackSize();
        return code.toString();
    }

    /**
     * Handles method invocation for both static and instance methods.
     * Generates Jasmin code for method calls, including method name, argument passing, and handling returns.
     *
     * @param code A StringBuilder to which the generated code is appended.
     * @param call The call instruction detailing the method to invoke and the arguments to pass.
     * @return A string representing the Jasmin assembly code for the method invocation.
     */
    private String handleMethodInvocation(StringBuilder code, CallInstruction call) {
        if (call.getInvocationType() != CallType.invokestatic) {
            code.append(generators.apply(call.getCaller()));
        }

        String args = buildArgumentList(code, call.getArguments());
        String methodName = validateAndExtractMethodName(call);
        String returnType = getJasminType(call.getReturnType());
        String className = resolveClassName(call);

        code.append(call.getInvocationType()).append(" ").append(className)
                .append("/").append(methodName).append(args).append(returnType).append(NL);

        adjustStackSizeForMethodCall(returnType, call);

        return code.toString();
    }

    /**
     * Constructs a string representing the argument list for a method call, appending generated code
     * for each argument to a provided StringBuilder.
     *
     * @param code A StringBuilder to which the argument generation code is appended.
     * @param arguments A list of Element objects representing the arguments to be passed to the method.
     * @return A string formatted as a method signature part for the arguments.
     */
    private String buildArgumentList(StringBuilder code, List<Element> arguments) {
        StringJoiner joiner = new StringJoiner("", "(", ")");
        for (Element element : arguments) {
            String argType = getJasminType(element.getType());
            code.append(generators.apply(element));
            joiner.add(argType);
        }
        return joiner.toString();
    }

    /**
     * Validates the method name from a call instruction and extracts the literal string representing it.
     * Throws errors for non-literal or incorrectly formatted method names.
     *
     * @param call The call instruction containing the method name to validate.
     * @return The extracted and validated method name.
     */
    private String validateAndExtractMethodName(CallInstruction call) {
        if (call.getMethodNameTry().isEmpty())
            throw new Error("Method does not exist");

        if (!call.getMethodName().isLiteral())
            throw new Error("Method name is not a literal");

        LiteralElement methodNameElement = (LiteralElement) call.getMethodName();
        if (methodNameElement.getType().getTypeOfElement() != ElementType.STRING)
            throw new Error("Method name is not a String");

        String methodName = methodNameElement.getLiteral();
        return methodName.substring(1, methodName.length() - 1);
    }

    /**
     * Determines the class name for a method invocation, handling both static and instance method calls.
     *
     * @param call The call instruction that may contain either static or instance method invocation details.
     * @return The resolved class name as required for the Jasmin code.
     */
    private String resolveClassName(CallInstruction call) {
        if (call.getInvocationType() == CallType.invokestatic) {
            String fullClassName = call.getCaller().toString().split(" ")[1].split("\\.")[0];
            return getImportedClass(fullClassName);
        } else {
            ClassType classType = (ClassType) call.getCaller().getType();
            return getImportedClass(classType.getName());
        }
    }

    /**
     * Adjusts the JVM stack size according to the type of method call (static or instance) and its return type.
     * Updates the stack size accounting for the object reference, method arguments, and the method return type.
     *
     * @param returnType The return type of the method being invoked, which affects stack size adjustments.
     * @param call The call instruction detailing the method call specifics.
     */
    private void adjustStackSizeForMethodCall(String returnType, CallInstruction call) {
        // Adjust stack size based on return type
        stackSize += returnType.equals("V") ? -1 : 1;

        // Non-static methods decrement the stack for the object reference
        if (call.getInvocationType() != CallType.invokestatic) {
            stackSize--;
        }

        // Decrement stack for each argument passed
        stackSize -= call.getArguments().size();

        checkStackSize();
    }


    /**
     * Generates the assembly code for accessing a field of an object.
     * The field is specified by the getField instruction which includes both the object
     * reference and the field to be accessed.
     *
     * @param getField The instruction that contains information about the object and field being accessed.
     * @return A string of Jasmin assembly code to perform the field access operation.
     */
    private String generateGetField(GetFieldInstruction getField) {
        StringBuilder code = new StringBuilder();

        // Apply generator to get code for the object whose field is to be accessed
        code.append(generators.apply(getField.getObject()));

        // Prepare the getfield instruction using the current class name, field name, and its type
        String fieldName = getField.getField().getName();
        String fieldType = getJasminType(getField.getField().getType());
        String className = currentClass.getClassName();

        code.append(TAB)
                .append("getfield ")
                .append(className)
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldType)
                .append(NL);

        return code.toString();
    }

    /**
     * Generates assembly code for defining a field in a class.
     * The field may be static, final, and optionally initialized.
     *
     * @param field The field object containing information about the field.
     * @return A string of Jasmin assembly code defining the field.
     */
    private String generateField(Field field) {
        StringBuilder code = new StringBuilder();

        // Determine field modifiers
        String fieldModifiers = "";
        if (field.isStaticField()) {
            fieldModifiers += "static ";
        }
        if (field.isFinalField()) {
            fieldModifiers += "final ";
        }

        // Get field name, type, and initial value (if initialized)
        String fieldName = field.getFieldName();
        String fieldType = getJasminType(field.getFieldType());
        String fieldValue = field.isInitialized() ? " = " + field.getInitialValue() : "";

        // Generate code for the field definition
        code.append(".field ")
                .append(fieldName)
                .append(" ")
                .append(fieldModifiers)
                .append(fieldType)
                .append(fieldValue)
                .append(NL);

        return code.toString();
    }

    /**
     * Generates the assembly code for setting the value of a field in an object.
     *
     * @param putField The instruction containing information about the object, value, and field to be set.
     * @return A string of Jasmin assembly code to set the field value.
     */
    private String generatePutField(PutFieldInstruction putField) {
        StringBuilder code = new StringBuilder();

        // Apply generators to get code for the object reference and value to be assigned to the field
        code.append(generators.apply(putField.getObject()));
        code.append(generators.apply(putField.getValue()));

        // Get field name and type
        String fieldName = putField.getField().getName();
        String fieldType = getJasminType(putField.getField().getType());

        // Generate code for putting the field value
        code.append(TAB)
                .append("putfield ")
                .append(getImportedClass(currentClass.getClassName()))
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(fieldType)
                .append(NL);

        // Update the stack size and check for stack overflow
        stackSize -= 2;
        checkStackSize();

        return code.toString();
    }

    /**
     * Returns the corresponding Jasmin type descriptor for the given OLLIR type.
     *
     * @param type The OLLIR type for which the Jasmin type descriptor is needed.
     * @return The Jasmin type descriptor.
     */
    private String getJasminType(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32 -> {
                return "I";
            }
            case BOOLEAN -> {
                return "Z";
            }
            case STRING -> {
                return "Ljava/lang/String;";
            }
            case OBJECTREF -> {
                ClassType classType = (ClassType) type;
                return "L" + getImportedClass(classType.getName()) + ";";
            }
            case ARRAYREF -> {
                ArrayType arrayType = (ArrayType) type;
                return "[" + getJasminType(arrayType.getElementType());
            }
            case VOID -> {
                return "V";
            }
            case THIS -> {
                return "L" + ollirResult.getOllirClass().getClassName() + ";";
            }
            case CLASS -> {
                return "Ljava/lang/Class;";
            }
            default -> throw new NotImplementedException(type);
        }
    }

    /**
     * Generates the assembly code for a goto instruction.
     *
     * @param gotoInst The goto instruction containing the target label.
     * @return A string representing the generated assembly code.
     */
    private String generateGoto(GotoInstruction gotoInst) {
        StringBuilder code = new StringBuilder();

        // Append the goto instruction with the target label
        code.append("goto ").append(gotoInst.getLabel()).append(NL);

        return code.toString();
    }

    /**
     * Generates the assembly code for a conditional branch instruction.
     *
     * @param condBranch The conditional branch instruction.
     * @return A string representing the generated assembly code.
     */
    private String generateCondBranch(CondBranchInstruction condBranch) {
        StringBuilder code = new StringBuilder();

        // Append the condition code followed by the conditional branch instruction with the target label
        code.append(generators.apply(condBranch.getCondition())).append("ifne ").append(condBranch.getLabel()).append(NL);

        // Decrease the stack size after generating the instruction
        stackSize--;
        checkStackSize();

        return code.toString();
    }




}

