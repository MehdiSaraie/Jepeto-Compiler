package main.visitor.codeGenerator;

import main.ast.nodes.*;
import main.ast.nodes.declaration.*;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.*;
import main.ast.nodes.expression.values.primitive.*;
import main.ast.nodes.statement.*;
import main.ast.types.*;
import main.ast.types.functionPointer.*;
import main.ast.types.list.*;
import main.ast.types.single.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.FunctionSymbolTableItem;
import main.visitor.Visitor;
import main.visitor.type.ExpressionTypeChecker;

import java.io.*;
import java.util.*;

public class CodeGenerator extends Visitor<String> {
    private final String outputPath;
    private FileWriter mainFile;
    private final ExpressionTypeChecker expressionTypeChecker;
    private FunctionDeclaration curFuncDec;
    private Set<String> visited;
    int label_cnt;

    public CodeGenerator(ExpressionTypeChecker expressionTypeChecker , Set<String> visited) {
        this.expressionTypeChecker = expressionTypeChecker;
        this.visited = visited;
        outputPath = "./output/";
        label_cnt = 0;
        prepareOutputFolder();
    }

    private void prepareOutputFolder() {
        String jasminPath = "utilities/jarFiles/jasmin.jar";
        String listClassPath = "utilities/codeGenerationUtilityClasses/List.j";
        String fptrClassPath = "utilities/codeGenerationUtilityClasses/Fptr.j";
        try{
            File directory = new File(this.outputPath);
            File[] files = directory.listFiles();
            if(files != null)
                for (File file : files)
                    file.delete();
             directory.mkdir();
        }
        catch(SecurityException e) {//unreachable
        }
        copyFile(jasminPath, this.outputPath + "jasmin.jar");
        copyFile(listClassPath, this.outputPath + "List.j");
        copyFile(fptrClassPath, this.outputPath + "Fptr.j");

        try {
            String path = outputPath + "Main.j";
            File file = new File(path);
            file.createNewFile();
            mainFile = new FileWriter(path);
        } catch (IOException e) {//unreachable
        }
    }

    private void copyFile(String toBeCopied, String toBePasted) {
        try {
            File readingFile = new File(toBeCopied);
            File writingFile = new File(toBePasted);
            InputStream readingFileStream = new FileInputStream(readingFile);
            OutputStream writingFileStream = new FileOutputStream(writingFile);
            byte[] buffer = new byte[1024];
            int readLength;
            while ((readLength = readingFileStream.read(buffer)) > 0)
                writingFileStream.write(buffer, 0, readLength);
            readingFileStream.close();
            writingFileStream.close();
        } catch (IOException e) {//never reached
        }
    }

    private void addCommand(String command) {
        try {
            command = String.join("\n\t\t", command.split("\n"));
            if(command.startsWith("Label_"))
                mainFile.write("\t" + command + "\n");
            else if(command.startsWith("."))
                mainFile.write(command + "\n");
            else
                mainFile.write("\t\t" + command + "\n");
            mainFile.flush();
        } catch (IOException e) {//unreachable

        }
    }

    private void addStaticMainMethod() {
        //todo
        String command = ".method public static main([Ljava/lang/String;)V\n";
        command += ".limit stack 128\n";
        command += ".limit locals 128\n";
        command += "new Main\n";
        command += "invokespecial Main/<init>()V\n";
        command += "return\n";
        command += ".end method\n";
        addCommand(command);
    }

    private int slotOf(String identifier) {
        //todo
        if (identifier == "") {
            curFuncDec.increaseUsedTemps();
            return curFuncDec.getArgs().size() + curFuncDec.getUsedTemps();
        }
        for(int i = 0; i < curFuncDec.getArgs().size(); i++){
            if (curFuncDec.getArgs().get(i).getName().equals(identifier))
                return i+1;
        }
        return -1;
    }

    @Override
    public String visit(Program program) {
        //todo
        String command = "";

        command += ".class public Main\n";
        command += ".super java/lang/Object\n";

        addCommand(command);

        addStaticMainMethod();

        //ToDo visiting functions
        for(String func_name : visited){
            FunctionSymbolTableItem func_symbol_table;
            try {
                func_symbol_table = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + func_name));
            }catch(ItemNotFoundException e){
                func_symbol_table = null;
            }
            FunctionDeclaration cur_func_dec_vis = func_symbol_table.getFuncDeclaration();

            curFuncDec = cur_func_dec_vis;
            expressionTypeChecker.setCurFunction(func_symbol_table);

            cur_func_dec_vis.accept(this);
        }

        program.getMain().accept(this);

        return null;
    }

    @Override
        public String visit(FunctionDeclaration funcDeclaration) {
        //todo
        String command = "";
        FunctionSymbolTableItem func_symbol_table;
        try {
            func_symbol_table = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + funcDeclaration.getFunctionName().getName()));
        }catch(ItemNotFoundException e){
            func_symbol_table = null;
        }

        command += ".method public " + funcDeclaration.getFunctionName().getName() + "(";
        //Todo add arg types
        ArrayList<Identifier> func_args = funcDeclaration.getArgs();
        Map < String , Type > func_args_type = func_symbol_table.getArgTypes();

        for(Identifier cur_arg : func_args){
            Type cur_arg_type = func_args_type.get(cur_arg.getName());
            if(cur_arg_type instanceof IntType)
                command += "Ljava/lang/Integer;";
            else if(cur_arg_type instanceof BoolType)
                command += "Ljava/lang/Boolean;";
            else if(cur_arg_type instanceof StringType)
                command += "Ljava/lang/String;";
            else if(cur_arg_type instanceof FptrType)
                command += "LFptr;";
            else if(cur_arg_type instanceof ListType)
                command += "LList;";
        }
        command += ")";

        //Todo add return type \n
        Type return_type = func_symbol_table.getReturnType();
        if(return_type instanceof IntType)
            command += "Ljava/lang/Integer;";
        else if(return_type instanceof BoolType)
            command += "Ljava/lang/Boolean;";
        else if(return_type instanceof StringType)
            command += "Ljava/lang/String;";
        else if(return_type instanceof FptrType)
            command += "LFptr;";
        else if(return_type instanceof ListType)
            command += "LList;";
        else if(return_type instanceof VoidType)
            command += "V";
        command += "\n";
        //Body
        command += ".limit stack 128\n";
        command += ".limit locals 128\n";

        addCommand(command);

        //TODO Set curFuncDec
        funcDeclaration.getBody().accept(this);

        command = ".end method\n";
        addCommand(command);
        return null;
    }

    @Override
    public String visit(MainDeclaration mainDeclaration) {
        //todo
        String command = ".method public <init>()V\n";
        command += ".limit stack 128\n";
        command += ".limit locals 128\n";
        command += "aload_0\n";
        command += "invokespecial java/lang/Object/<init>()V\n";
        addCommand(command);

        mainDeclaration.getBody().accept(this);

        command = "return\n";
        command += ".end method\n";
        addCommand(command);

        return null;
    }


    @Override
    public String visit(BlockStmt blockStmt) {
        //todo
        for (Statement stmt : blockStmt.getStatements())
            stmt.accept(this);
        return null;
    }

    @Override
    public String visit(ConditionalStmt conditionalStmt) {
        //todo
        String command = "";

        if(conditionalStmt.getElseBody() != null) {
            command += conditionalStmt.getCondition().accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifeq Label_" + "else" + String.valueOf(label_cnt) + "\n";
            String label_else = "Label_else" + String.valueOf(label_cnt);
            label_cnt += 1;

            addCommand(command);

            conditionalStmt.getThenBody().accept(this);
            command = "goto Label_" + "endif" + String.valueOf(label_cnt) + "\n";
            String label_endif = "Label_endif" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "else" + ":\n";
            command += label_else + ":\n";
            addCommand(command);

            conditionalStmt.getElseBody().accept(this);
            //command = "Label_" + "endif" + ":\n";
            command = label_endif + ":\n";
            addCommand(command);
        }
        else{
            command += conditionalStmt.getCondition().accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifeq Label_" + "endif" + String.valueOf(label_cnt) + "\n";
            String label_endif = "Label_endif" + String.valueOf(label_cnt);
            label_cnt += 1;

            addCommand(command);

            conditionalStmt.getThenBody().accept(this);

            command = label_endif + ":\n";
            addCommand(command);
        }
        return null;
    }

    @Override
    public String visit(FunctionCallStmt funcCallStmt) {
        //todo
        expressionTypeChecker.setFunctioncallStmt(true);
        String command = "";
        command += funcCallStmt.getFunctionCall().accept(this);
        command += "pop\n";
        addCommand(command);
        expressionTypeChecker.setFunctioncallStmt(false);
        return null;
    }

    @Override
    public String visit(PrintStmt print) {
        //todo
        //Todo add printstream
        Type arg_type = print.getArg().accept(expressionTypeChecker);
        String command = "";

        if(!(arg_type instanceof ListType))
            command += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";

        //Todo add value
        command += print.getArg().accept(this);

        if(arg_type instanceof IntType)
            command += "invokevirtual java/lang/Integer/intValue()I\n";

        //Todo invoke matching print function

        if(arg_type instanceof IntType)
            command += "invokevirtual java/io/PrintStream/println(I)V\n";
        else if(arg_type instanceof BoolType)
            command += "invokevirtual java/io/PrintStream/println(Ljava/lang/Object;)V\n";
        else if(arg_type instanceof StringType)
            command += "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n";
        else if(arg_type instanceof ListType){
            command += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
            command += "ldc \"[\"\n";
            command += "invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V\n";

            String label_whileend = "Label_whileend" + String.valueOf(label_cnt);
            label_cnt += 1;

            String label_whilestart = "Label_whilestart" + String.valueOf(label_cnt);
            label_cnt += 1;

            String label_aftercomma = "Label_aftercomma" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += "iconst_0\n";
            int index_slot = slotOf("");
            if (index_slot > 3)
                command += "istore " + index_slot + "\n";
            else
                command += "istore_" + index_slot + "\n";

            int temp_element = slotOf("");

            command += label_whilestart + ":\n";
            command += "dup\n";
            command += "invokevirtual List/getSize()I\n";
            command += "iload " + index_slot + "\n";
            command += "if_icmple " + label_whileend + "\n";

            command += "iload " + index_slot + "\n";
            command += "iconst_0\n";
            command += "if_icmpeq " + label_aftercomma + "\n";

            command += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
            command += "ldc \",\"\n";
            command += "invokevirtual java/io/PrintStream/print(Ljava/lang/String;)V\n";

            command += label_aftercomma + ":\n";
            command += "dup\n";
            command += "iload " + index_slot + "\n";
            command += "invokevirtual List/getElement(I)Ljava/lang/Object;\n";
            command += "checkcast java/lang/Integer\n";
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "istore " + temp_element + "\n";
            command += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
            command += "iload " + temp_element + "\n";
            command += "invokevirtual java/io/PrintStream/print(I)V\n";
            command += "iinc " + index_slot + " 1\n";
            command += "goto " + label_whilestart + "\n";

            command += label_whileend + ":\n";
            command += "getstatic java/lang/System/out Ljava/io/PrintStream;\n";
            command += "ldc \"]\"\n";
            command += "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V\n";
            command += "pop\n";
        }


        addCommand(command);
        return null;
    }

    @Override
    public String visit(ReturnStmt returnStmt) {
        //todo
        String command = returnStmt.getReturnedExpr().accept(this);
        Type expr_type = returnStmt.getReturnedExpr().accept(expressionTypeChecker);

        if(expr_type instanceof VoidType)
            command += "return\n";
        else
            command += "areturn\n";

        addCommand(command);
        return null;
    }

    @Override
    public String visit(BinaryExpression binaryExpression) {
        //todo
        String command = "";
        Expression left = binaryExpression.getFirstOperand();
        Expression right = binaryExpression.getSecondOperand();
        BinaryOperator operator =  binaryExpression.getBinaryOperator();
        if (operator.equals(BinaryOperator.add)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "iadd\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.sub)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "isub\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.mult)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "imul\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.div)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "idiv\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        else if (operator.equals(BinaryOperator.eq)) { //fptr is void should be checked
            command += left.accept(this);
            command += right.accept(this);
            command += "if_acmpne Label_" + "false" + String.valueOf(label_cnt) + "\n";
            String label_false = "Label_false" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += "ldc 1\n";
            command += "goto Label_" + "after" + String.valueOf(label_cnt) + "\n";
            String label_after = "Label_after" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "false" + ":\n";
            command += label_false + ":\n";
            command += "ldc 0\n";

            //command += "Label_" + "after" + ":\n";
            command += label_after + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(BinaryOperator.neq)) { //fptr not void should be checked
            command += left.accept(this);
            command += right.accept(this);
            command += "if_acmpeq Label_" + "false" + String.valueOf(label_cnt) + "\n";
            String label_false = "Label_false" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += "ldc 1\n";
            command += "goto Label_" + "after" + String.valueOf(label_cnt) + "\n";
            String label_after = "Label_after" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "false" + ":\n";
            command += label_false + ":\n";
            command += "ldc 0\n";

            //command += "Label_" + "after" + ":\n";
            command += label_after + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(BinaryOperator.and)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifeq Label_" + "false" + String.valueOf(label_cnt) + "\n";
            String label_false = "Label_false" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += right.accept(this);
            command += "goto Label_" + "endif" + String.valueOf(label_cnt) + "\n";
            String label_endif = "Label_endif" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "false" + ":\n";
            command += label_false + ":\n";
            command += "ldc 0\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
            //command += "Label_" + "endif" + ":\n";
            command += label_endif + ":\n";
        }
        else if (operator.equals(BinaryOperator.or)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifne Label_" + "true" + String.valueOf(label_cnt) + "\n";
            String label_true = "Label_true" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += right.accept(this);
            command += "goto Label_" + "endif" + String.valueOf(label_cnt) + "\n";
            String label_endif = "Label_endif" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "true" + ":\n";
            command += label_true + ":\n";
            command += "ldc 1\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
            //command += "Label_" + "endif" + ":\n";
            command += label_endif + ":\n";
        }
        else if(operator.equals(BinaryOperator.lt)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "if_icmpge Label_" + "false" + String.valueOf(label_cnt) + "\n";
            String label_false = "Label_false" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += "ldc 1\n";
            command += "goto Label_" + "endif" + String.valueOf(label_cnt) + "\n";
            String label_endif = "Label_endif" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "false" + ":\n";
            command += label_false + ":\n";

            command += "ldc 0\n";
            //command += "Label_" + "endif" + ":\n";
            command += label_endif + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if(operator.equals(BinaryOperator.gt)){
            command += left.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += right.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "if_icmple Label_" + "false" + String.valueOf(label_cnt) + "\n";
            String label_false = "Label_false" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += "ldc 1\n";
            command += "goto Label_" + "endif" + String.valueOf(label_cnt) + "\n";
            String label_endif = "Label_endif" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "false" + ":\n";
            command += label_false + ":\n";
            command += "ldc 0\n";
            //command += "Label_" + "endif" + ":\n";
            command += label_endif + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(BinaryOperator.append)){
            command = left.accept(this);
            command += "dup\n";
            command += right.accept(this);
            command += "invokevirtual List/addElement(Ljava/lang/Object;)V\n";
        }
        return command;
    }

    @Override
    public String visit(UnaryExpression unaryExpression) {
        //todo
        Expression operand = unaryExpression.getOperand();
        UnaryOperator operator = unaryExpression.getOperator();
        String command = "";
        if (operator.equals(UnaryOperator.not)){
            command += operand.accept(this);
            command += "invokevirtual java/lang/Boolean/booleanValue()Z\n";
            command += "ifeq Label_" + "false" + String.valueOf(label_cnt) + "\n";
            String label_false = "Label_false" + String.valueOf(label_cnt);
            label_cnt += 1;

            command += "ldc 0\n";
            command += "goto Label_" + "after" + String.valueOf(label_cnt) + "\n";
            String label_after = "Label_after" + String.valueOf(label_cnt);
            label_cnt += 1;

            //command += "Label_" + "false" + ":\n";
            command += label_false + ":\n";
            command += "ldc 1\n";
            //command += "Label_" + "after" + ":\n";
            command += label_after + ":\n";
            command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        }
        else if (operator.equals(UnaryOperator.minus)){
            command += operand.accept(this);
            command += "invokevirtual java/lang/Integer/intValue()I\n";
            command += "ineg\n";
            command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        }
        return command;
    }

    @Override
    public String visit(AnonymousFunction anonymousFunction) {
        //todo
        String command = "";
        try{
            FunctionSymbolTableItem func = (FunctionSymbolTableItem) SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + anonymousFunction.getName());
            command += "new Fptr\n";
            command += "dup\n";
            command += "aload_0\n";
            command += "ldc " + "\"" + anonymousFunction.getName() + "\"\n";
            command += "invokespecial Fptr/<init>(Ljava/lang/Object;Ljava/lang/String;)V\n";
        }catch (ItemNotFoundException e){
            int slot_number = slotOf(anonymousFunction.getName());
            if (slot_number > 3)
                command += "aload " + String.valueOf(slot_number) + "\n";
            else
                command += "aload_" + String.valueOf(slot_number) + "\n";
        }
        return command;
    }

    @Override
    public String visit(Identifier identifier) {
        //todo
        String command = "";
        try{
            FunctionSymbolTableItem func = (FunctionSymbolTableItem) SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + identifier.getName());
            command += "new Fptr\n";
            command += "dup\n";
            command += "aload_0\n";
            command += "ldc " + "\"" + identifier.getName() + "\"\n";
            command += "invokespecial Fptr/<init>(Ljava/lang/Object;Ljava/lang/String;)V\n";
        }catch (ItemNotFoundException e){
            int slot_number = slotOf(identifier.getName());
            if (slot_number > 3)
                command += "aload " + String.valueOf(slot_number) + "\n";
            else
                command += "aload_" + String.valueOf(slot_number) + "\n";
        }
        return command;
    }

    @Override
    public String visit(ListAccessByIndex listAccessByIndex) {
        //todo
        String command = "";
        command += listAccessByIndex.getInstance().accept(this);
        command += listAccessByIndex.getIndex().accept(this);
        command += "invokevirtual java/lang/Integer/intValue()I\n";
        command += "invokevirtual List/getElement(I)Ljava/lang/Object;\n";
        command += "checkcast java/lang/Integer\n";
        return command;
    }

    @Override
    public String visit(ListSize listSize) {
        //todo
        String command = "";
        command += listSize.getInstance().accept(this);
        command += "invokevirtual List/getSize()I\n";
        command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        return command;
    }

    @Override
    public String visit(FunctionCall funcCall) {
        //todo
        String command = funcCall.getInstance().accept(this);

        FptrType fptr = (FptrType) funcCall.getInstance().accept(expressionTypeChecker);
        FunctionSymbolTableItem func_symbol_table;
        try {
            func_symbol_table = (FunctionSymbolTableItem) (SymbolTable.root.getItem(FunctionSymbolTableItem.START_KEY + fptr.getFunctionName()));
        }catch(ItemNotFoundException e){
            func_symbol_table = null;
        }

        ArrayList<Expression> args = funcCall.getArgs();
        Map<Identifier , Expression> args_with_keys = funcCall.getArgsWithKey();

        command += "new java/util/ArrayList\n";
        command += "dup\n";
        command += "invokespecial java/util/ArrayList/<init>()V\n";

        int arrayList_slot = slotOf("");
        if (arrayList_slot > 3)
            command += "astore " + arrayList_slot + "\n";
        else
            command += "astore_" + arrayList_slot + "\n";

        if(!args.isEmpty()){
            for(Expression cur_arg : args){
                if (arrayList_slot > 3)
                    command += "aload " + arrayList_slot + "\n";
                else
                    command += "aload_" + arrayList_slot + "\n";
                command += cur_arg.accept(this);
                command += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
                command += "pop\n";
            }
        }else if(!args_with_keys.isEmpty()){
            Map<String, Type> arg_types = func_symbol_table.getArgTypes();
            java.util.Set<String> arg_names = arg_types.keySet();
            for(String arg_name : arg_names){
                if (arrayList_slot > 3)
                    command += "aload " + arrayList_slot + "\n";
                else
                    command += "aload_" + arrayList_slot + "\n";

                Expression cur_arg = null;
                for(Identifier cur_id : args_with_keys.keySet()){
                    if(cur_id.getName().equals(arg_name)){
                        cur_arg = args_with_keys.get(cur_id);
                        break;
                    }
                }
                command += cur_arg.accept(this);
                command += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
                command += "pop\n";
            }
        }

        if (arrayList_slot > 3)
            command += "aload " + arrayList_slot + "\n";
        else
            command += "aload_" + arrayList_slot + "\n";

        command += "invokevirtual Fptr/invoke(Ljava/util/ArrayList;)Ljava/lang/Object;\n";

        Type func_return_type = func_symbol_table.getReturnType();
        if(func_return_type instanceof IntType)
            command += "checkcast java/lang/Integer\n";
        else if(func_return_type instanceof BoolType)
            command += "checkcast java/lang/Boolean\n";
        else if(func_return_type instanceof StringType)
            command += "checkcast java/lang/String\n";
        else if(func_return_type instanceof ListType)
            command += "checkcast List\n";
        else if(func_return_type instanceof FptrType)
            command += "checkcast Fptr\n";

        return command;
    }

    @Override
    public String visit(ListValue listValue) {
        //todo
        String command = "";
        command += "new java/util/ArrayList\n";
        command += "dup\n";
        command += "invokespecial java/util/ArrayList/<init>()V\n";
        int arrayList_slot = slotOf("");
        if (arrayList_slot > 3)
            command += "astore " + arrayList_slot + "\n";
        else
            command += "astore_" + arrayList_slot + "\n";

        for (Expression element: listValue.getElements()){
            if (arrayList_slot > 3)
                command += "aload " + arrayList_slot + "\n";
            else
                command += "aload_" + arrayList_slot + "\n";
            command += element.accept(this);
            command += "invokevirtual java/util/ArrayList/add(Ljava/lang/Object;)Z\n";
            command += "pop\n";
        }
        command += "new List\n";
        command += "dup\n";
        if (arrayList_slot > 3)
            command += "aload " + arrayList_slot + "\n";
        else
            command += "aload_" + arrayList_slot + "\n";
        command += "invokespecial List/<init>(Ljava/util/ArrayList;)V\n";
        return command;
    }

    @Override
    public String visit(IntValue intValue) {
        String command = "ldc " + intValue.getConstant() + "\n";
        command += "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer;\n";
        return command;
    }

    @Override
    public String visit(BoolValue boolValue) {
        String command = "ldc " + (boolValue.getConstant() ? 1 : 0) + "\n";
        command += "invokestatic java/lang/Boolean/valueOf(Z)Ljava/lang/Boolean;\n";
        return command;
    }

    @Override
    public String visit(StringValue stringValue) {
        //todo
        //TODO string ha bayad ba " vared shavand
        String command = "ldc \"" + stringValue.getConstant() + "\"\n";
        return command;
    }

    @Override
    public String visit(VoidValue voidValue) {
        //todo
        String command = "";
        return command;
    }
}
