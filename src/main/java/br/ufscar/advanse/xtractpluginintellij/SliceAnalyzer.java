package br.ufscar.advanse.xtractpluginintellij;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.CKNotifier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.util.*;


public class SliceAnalyzer {

    public static void fragmentMetricExtractor(
            String methodCode,
            String slicesFileName,
            String metricsFileName
    ) {
        System.out.println("slicesFileName = " + slicesFileName);
        System.out.println("metricsFileName = " + metricsFileName);

        String methodName = "main";

        try {
            // ===== 1. Method code slices =====
            ParserConfiguration config = StaticJavaParser.getParserConfiguration();
            config.setStoreTokens(true);

            String wrapped = "class Temp { " + methodCode + " }";
            CompilationUnit cu = StaticJavaParser.parse(wrapped);
            MethodDeclaration methodAST = cu.findFirst(MethodDeclaration.class).orElseThrow(() -> new Exception("Method not found"));

            Set<String> loopVars = extractLoopControlVariables(methodAST);
            Map<String, Integer> variableUsageFreq = countVariableUsageInMethod(methodAST);
            List<Slice> slices = extractSlices(methodCode, variableUsageFreq, loopVars);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // Saves the original JSON with all the slice information.
//            FileWriter writer = new FileWriter("method_slices_usage_outside.json");
//            gson.toJson(slices, writer);
//            writer.close();
//            System.out.println("Slices extracted and saved in method_slices_usage_outside.json");

            // ===== 2. Simplified JSON =====
            List<SimpleSlice> simpleList = new ArrayList<>();
            Set<String> seenFragments = new HashSet<>();

            System.out.println("Number of slices: " + slices.size());
            int counter = 0;

            for (Slice s : slices) {
                System.out.println("s.slice: " + s.slice.size());
                System.out.println("s.label: " + s.label);
                System.out.println();
                for (String frag : s.slice) {
                    if (!deveExcluirSlice(frag, methodCode) && seenFragments.add(frag.trim())) {
                        simpleList.add(new SimpleSlice(
                                methodCode,
                                frag,
                                s.totalUsageOutsideSlice,
                                s.beginLine,
                                s.beginColumn,
                                s.endLine,
                                s.endColumn
                        ));
                        counter++;
                    }
                }
            }

            System.out.println("simpleList size: " + simpleList.size());
            System.out.println("counter: " + counter);

            try (FileWriter writer = new FileWriter(slicesFileName)) {
                gson.toJson(simpleList, writer);
                System.out.println("Simplified JSON saved in " + slicesFileName);
            }


            // ===== 3. Creation of Temp.java for CK =====
            String tempDirPath = "temp_ck_analysis";
            File tempDir = new File(tempDirPath);
            tempDir.mkdirs();

            String classCode = "class Temp { " + methodCode + " }";
//            FileWriter writer = new FileWriter(tempDirPath + "/Temp.java");
//            writer.write(classCode);
//            writer.close();
            try (FileWriter writer = new FileWriter(tempDirPath + "/Temp.java")) {
                writer.write(classCode);
            }

            // ===== 4. CK execution =====
            List<Map<String, Object>> metricsJson = new ArrayList<>();

            new CK().calculate(tempDirPath, new CKNotifier() {
                @Override
                public void notify(CKClassResult classe) {
                    for (CKMethodResult method : classe.getMethods()) {
//                        if (method.getMethodName().contains(methodName)) {
                        Map<String, Object> metrics = new LinkedHashMap<>();
                        metrics.put("id_column_name",1);
//                        metrics.put("methodName", method.getMethodName());
                        metrics.put("methodAnonymousClassesQty", method.getAnonymousClassesQty());
                        metrics.put("methodAssignmentsQty", method.getAssignmentsQty());
                        metrics.put("methodCbo", method.getCbo());
                        metrics.put("methodComparisonsQty", method.getComparisonsQty());
                        metrics.put("methodLambdasQty", method.getLambdasQty());
                        metrics.put("methodLoc", method.getLoc());
                        metrics.put("methodLoopQty", method.getLoopQty());
                        metrics.put("methodMathOperationsQty", method.getMathOperationsQty());
                        metrics.put("methodMaxNestedBlocks", method.getMaxNestedBlocks());
                        metrics.put("methodNumbersQty", method.getNumbersQty());
                        metrics.put("methodParametersQty", method.getParametersQty());
                        metrics.put("methodParenthesizedExpsQty", method.getParenthesizedExpsQty());
                        metrics.put("methodReturnQty", method.getReturnQty());
                        metrics.put("methodRfc", method.getRfc());
                        metrics.put("methodStringLiteralsQty", method.getStringLiteralsQty());
                        metrics.put("methodSubClassesQty",method.getInnerClassesQty());
                        metrics.put("methodTryCatchQty", method.getTryCatchQty());
                        metrics.put("methodUniqueWordsQty", method.getUniqueWordsQty());
                        metrics.put("methodVariablesQty", method.getVariablesQty());
                        metrics.put("methodWmc", method.getWmc());
//                        metrics.put("bugFixCount",0);
//                        metrics.put("refactoringsInvolved",0);
                        metricsJson.add(metrics);
//                        }
                    }
                }
            });

            // ===== 5. JSON save with CK metrics =====
//            try (FileWriter out = new FileWriter("method_metrics.json")) {
//                gson.toJson(metricsJson, out);
//                System.out.println("Metrics saved in method_metrics.json");
//            }

            // ======== Saving in CSV:
            try (FileWriter csvOut = new FileWriter(metricsFileName)) {
                // Headers
                csvOut.write("id_,methodAnonymousClassesQty,methodAssignmentsQty,methodCbo,methodComparisonsQty,methodLambdasQty,methodLoc,methodLoopQty,methodMathOperationsQty,methodMaxNestedBlocks,methodNumbersQty,methodParametersQty,methodParenthesizedExpsQty,methodReturnQty,methodRfc,methodStringLiteralsQty,methodSubClassesQty,methodTryCatchQty,methodUniqueWordsQty,methodVariablesQty,methodWmc\n");

                for (Map<String, Object> method : metricsJson) {
                    List<String> values = new ArrayList<>();
                    for (String key : method.keySet()) {
                        values.add(method.get(key).toString());
                    }
                    csvOut.write(String.join(",", values) + "\n");
                }

                System.out.println("Metrics saved in " + metricsFileName);
            }

            // ===== 6. Cleaning of temporary files =====
            File tempFile = new File(tempDirPath + "/Temp.java");
            if (tempFile.exists()) tempFile.delete();
            if (tempDir.exists() && tempDir.isDirectory() && tempDir.list().length == 0) {
                tempDir.delete();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Slice {
        String label;
        String justification;
        List<String> slice;
        List<String> variables;
        Map<String, Integer> variableUsageOutsideSliceCount;
        int totalUsageOutsideSlice;
        double usageDensityOutsideSlice;

        int beginLine;
        int beginColumn;
        int endLine;
        int endColumn;

        public Slice(String label, String justification,
                     List<String> slice,
                     List<String> variables,
                     Map<String, Integer> variableUsageOutsideSliceCount,
                     int totalUsageOutsideSlice,
                     int beginLine,
                     int beginColumn,
                     int endLine,
                     int endColumn
        ){
            this.label = label;
            this.justification = justification;
            this.slice = slice;
            this.variables = variables;
            this.variableUsageOutsideSliceCount = variableUsageOutsideSliceCount;
            this.totalUsageOutsideSlice = totalUsageOutsideSlice;

            this.beginLine = beginLine;
            this.beginColumn = beginColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;

        }

    }

    // New class for simplified JSON
    public static class SimpleSlice {
        String methodCode;
        String sourceCodeFragment;
        int totalUsageOutsideSlice;
        int beginLine;
        int beginColumn;
        int endLine;
        int endColumn;

        public SimpleSlice(String methodCode,
                           String sourceCodeFragment,
                           int totalUsageOutsideSlice,
                           int beginLine,
                           int beginColumn,
                           int endLine,
                           int endColumn) {
            this.methodCode = methodCode;
            this.sourceCodeFragment = sourceCodeFragment;
            this.totalUsageOutsideSlice = totalUsageOutsideSlice;
            this.beginLine = beginLine;
            this.beginColumn = beginColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
        }
    }

    public static List<Slice> extractSlices(String  methodCode, Map<String, Integer> variableUsageFreq, Set<String> loopVars) throws Exception {
        List<Slice> slices = new ArrayList<>();

        String wrapped = "class Temp { " + methodCode + " }";
        CompilationUnit cu = StaticJavaParser.parse(wrapped);
        MethodDeclaration method = cu.findFirst(MethodDeclaration.class).orElseThrow(() -> new Exception("Method not found"));

        if (!method.getBody().isPresent()) return slices;
        BlockStmt body = method.getBody().get();
//        BlockStmt body = method.getBody().orElse(null);

        analyzeBlock(body, slices, variableUsageFreq, loopVars);

        System.out.println("Total number of slices collected (before filter): " + slices.size());

        return slices;
    }

    private static void analyzeBlock(BlockStmt block, List<Slice> slices, Map<String, Integer> variableUsageFreq, Set<String> loopVars) {
        List<Statement> stmts = block.getStatements();

        List<Statement> currentSliceStatements = new ArrayList<>();
        String currentLabel = null;
        String currentJustification = null;

        for (Statement stmt : stmts) {

            // DEBUG: print the statement type
//            System.out.println("Statement: " + stmt);
//            System.out.println("Type: " + stmt.getClass().getSimpleName());
//            System.out.println("Begin: " + stmt.getRange().get().begin);
//            System.out.println("Statement: " + stmt);
            int beginLine = 0, beginColumn = 0, endLine = 0, endColumn = 0;

            if (stmt.getRange().isPresent()) {
                beginLine = stmt.getRange().get().begin.line;
                beginColumn = stmt.getRange().get().begin.column;
                endLine = stmt.getRange().get().end.line;
                endColumn = stmt.getRange().get().end.column;
            }
//            stmt.getRange().ifPresent(s -> {
//                    int beginLine2 = s.begin.line;
//                    beginColumn = stmt.getRange().get().begin.column;
//                    endLine = stmt.getRange().get().end.line;
//                    endColumn = stmt.getRange().get().end.column;
//                }
//            );

            if (stmt.isIfStmt()) {

                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);

                IfStmt ifStmt = stmt.asIfStmt();
                String label = "IF conditional block";
                String justification = "Executes code conditionally if the condition is true";

                if (ifStmt.getRange().isPresent()) {
                    beginLine = ifStmt.getRange().get().begin.line;
                    beginColumn = ifStmt.getRange().get().begin.column;
                    endLine = ifStmt.getRange().get().end.line;
                    endColumn = ifStmt.getRange().get().end.column;
                }

                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(ifStmt.toString());
                List<String> vars = extractVariables(ifStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(ifStmt, vars, variableUsageFreq,loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();
                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                Statement thenStmt = ifStmt.getThenStmt();
                if (thenStmt.isBlockStmt()) {
                    analyzeBlock(thenStmt.asBlockStmt(), slices, variableUsageFreq,loopVars);
                } else {
                    List<String> singleStmtSlice = new ArrayList<>();
                    singleStmtSlice.add(thenStmt.toString());
                    List<String> varsThen = extractVariables(thenStmt);
                    Map<String, Integer> outsideCountThen = calculateUsageOutsideSlice(thenStmt, varsThen, variableUsageFreq,loopVars);
                    int totalOutsideThen = outsideCountThen.values().stream().mapToInt(Integer::intValue).sum();

                    int tBeginLine = 0, tBeginColumn = 0, tEndLine = 0, tEndColumn = 0;

                    if (thenStmt.getRange().isPresent()) {
                        tBeginLine = thenStmt.getRange().get().begin.line;
                        tBeginColumn = thenStmt.getRange().get().begin.column;
                        tEndLine = thenStmt.getRange().get().end.line;
                        tEndColumn = thenStmt.getRange().get().end.column;
                    }

                    slices.add(new Slice("THEN block (single statement)", "Single statement in THEN",
                            singleStmtSlice,
                            varsThen,
                            outsideCountThen,
                            totalOutsideThen,
                            beginLine,
                            beginColumn,
                            endLine,
                            endColumn));
                }

                ifStmt.getElseStmt().ifPresent(elseStmt -> {
                    if (elseStmt.isBlockStmt()) {
                        analyzeBlock(elseStmt.asBlockStmt(), slices, variableUsageFreq,loopVars);
                    } else {
                        List<String> elseSlice = new ArrayList<>();
                        elseSlice.add(elseStmt.toString());
                        List<String> varsElse = extractVariables(elseStmt);
                        Map<String, Integer> outsideCountElse = calculateUsageOutsideSlice(elseStmt, varsElse, variableUsageFreq,loopVars);
                        int totalOutsideElse = outsideCountElse.values().stream().mapToInt(Integer::intValue).sum();

                        int eBeginLine = 0, eBeginColumn = 0, eEndLine = 0, eEndColumn = 0;

                        if (elseStmt.getRange().isPresent()) {
                            eBeginLine = elseStmt.getRange().get().begin.line;
                            eBeginColumn = elseStmt.getRange().get().begin.column;
                            eEndLine = elseStmt.getRange().get().end.line;
                            eEndColumn = elseStmt.getRange().get().end.column;
                        }

                        slices.add(new Slice("ELSE block", "Code executed if the IF condition is false",
                                elseSlice,
                                varsElse,
                                outsideCountElse,
                                totalOutsideElse,
                                eBeginLine,
                                eBeginColumn,
                                eEndLine,
                                eEndColumn));
                    }
                });

            } else if (stmt.isForEachStmt()) {
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);

                ForEachStmt forEachStmt = stmt.asForEachStmt();
                String label = "FOR-EACH loop block";
                String justification = "Executes repeatedly for each element in a collection";

                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(forEachStmt.toString());
                List<String> vars = extractVariables(forEachStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(forEachStmt, vars, variableUsageFreq,loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();
                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                Statement bodyStmt = forEachStmt.getBody();
                if (bodyStmt.isBlockStmt()) {
                    analyzeBlock(bodyStmt.asBlockStmt(), slices, variableUsageFreq,loopVars);
                } else {
                    List<String> singleStmtSlice = new ArrayList<>();
                    singleStmtSlice.add(bodyStmt.toString());
                    List<String> varsBody = extractVariables(bodyStmt);
                    Map<String, Integer> outsideCountBody = calculateUsageOutsideSlice(bodyStmt, varsBody, variableUsageFreq,loopVars);
                    int totalOutsideBody = outsideCountBody.values().stream().mapToInt(Integer::intValue).sum();
                    slices.add(new Slice("FOR-EACH body (single statement)",
                            "Single statement in the FOR-EACH body",
                            singleStmtSlice,
                            varsBody,
                            outsideCountBody,
                            totalOutsideBody,
                            beginLine,
                            beginColumn,
                            endLine,
                            endColumn));
                }

            } else if (stmt.isForStmt()) {
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);

                ForStmt forStmt = stmt.asForStmt();
                String label = "FOR loop block";
                String justification = "Executes repeatedly with initialization, condition, and increment";

                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(forStmt.toString());
                List<String> vars = extractVariables(forStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(forStmt, vars, variableUsageFreq,loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();
                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                Statement bodyStmt = forStmt.getBody();
                if (bodyStmt.isBlockStmt()) {
                    analyzeBlock(bodyStmt.asBlockStmt(), slices, variableUsageFreq,loopVars);
                } else {
                    List<String> singleStmtSlice = new ArrayList<>();
                    singleStmtSlice.add(bodyStmt.toString());
                    List<String> varsBody = extractVariables(bodyStmt);
                    Map<String, Integer> outsideCountBody = calculateUsageOutsideSlice(bodyStmt, varsBody, variableUsageFreq,loopVars);
                    int totalOutsideBody = outsideCountBody.values().stream().mapToInt(Integer::intValue).sum();
                    slices.add(new Slice("FOR loop body (single statement)",
                            "Single statement in the FOR loop body",
                            singleStmtSlice,
                            varsBody,
                            outsideCountBody,
                            totalOutsideBody,
                            beginLine,
                            beginColumn,
                            endLine,
                            endColumn));
                }

            } else if (stmt.isWhileStmt()) {
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);

                WhileStmt whileStmt = stmt.asWhileStmt();
                String label = "WHILE loop block";
                String justification = "Executes as long as the condition is true";

                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(whileStmt.toString());
                List<String> vars = extractVariables(whileStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(whileStmt, vars, variableUsageFreq,loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();
                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                Statement bodyStmt = whileStmt.getBody();
                if (bodyStmt.isBlockStmt()) {
                    analyzeBlock(bodyStmt.asBlockStmt(), slices, variableUsageFreq,loopVars);
                }

            } else if (stmt.isDoStmt()) {
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);

                DoStmt doStmt = stmt.asDoStmt();
                String label = "DO-WHILE loop block";
                String justification = "Executes at least once and repeats as long as the condition is true.";

                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(doStmt.toString());
                List<String> vars = extractVariables(doStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(doStmt, vars, variableUsageFreq,loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();
                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                Statement bodyStmt = doStmt.getBody();
                if (bodyStmt.isBlockStmt()) {
                    analyzeBlock(bodyStmt.asBlockStmt(), slices, variableUsageFreq,loopVars);
                }

            }else if (stmt.isTryStmt()) {
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq, loopVars);

                TryStmt tryStmt = stmt.asTryStmt();
                String label = "TRY block";
                String justification = "Block that can throw exceptions, with optional error handling";

                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(tryStmt.toString());
                List<String> vars = extractVariables(tryStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(tryStmt, vars, variableUsageFreq,loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();
                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                // Recursively analyzes the contents of the try block.
                if (tryStmt.getTryBlock().isBlockStmt()) {
                    analyzeBlock(tryStmt.getTryBlock(), slices, variableUsageFreq,loopVars);
                }

                // Can also analyze catch and finally blocks, if desired

            } else if (stmt.isSwitchStmt()) {
                // Saves any slice in the previous build
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq, loopVars);

                SwitchStmt switchStmt = stmt.asSwitchStmt();
                String label = "SWITCH block";
                String justification = "Flow control based on multiple case values";

                // Slice of the entire switch
                List<String> sliceCode = new ArrayList<>();
                sliceCode.add(switchStmt.toString());
                List<String> vars = extractVariables(switchStmt);
                Map<String, Integer> outsideCount = calculateUsageOutsideSlice(switchStmt, vars, variableUsageFreq, loopVars);
                int totalOutside = outsideCount.values().stream().mapToInt(Integer::intValue).sum();

                slices.add(new Slice(label,
                        justification,
                        sliceCode,
                        vars,
                        outsideCount,
                        totalOutside,
                        beginLine,
                        beginColumn,
                        endLine,
                        endColumn));

                // Now, for each case, group the statements into a single slice.
//                for (SwitchEntry entry : switchStmt.getEntries()) {
//                    if (!entry.getStatements().isEmpty()) {
//                        List<String> caseSlice = new ArrayList<>();
//                        BlockStmt syntheticBlock = new BlockStmt();
//
//                        for (Statement innerStmt : entry.getStatements()) {
//                            caseSlice.add(innerStmt.toString());
//                            syntheticBlock.addStatement(innerStmt);
//                        }
//
//                        List<String> varsInner = extractVariables(syntheticBlock);
//                        Map<String, Integer> outsideCountInner = calculateUsageOutsideSlice(syntheticBlock, varsInner, variableUsageFreq, loopVars);
//                        int totalOutsideInner = outsideCountInner.values().stream().mapToInt(Integer::intValue).sum();
//
//                        slices.add(new Slice(
//                            "Case do SWITCH",
//                            "Conjunto de instruções pertencente a um dos casos do switch",
//                            caseSlice,
//                            varsInner,
//                            outsideCountInner,
//                            totalOutsideInner
//                        ));
//                    }
//                }
            }
            else if (stmt.isBlockStmt()) {
                // Isolated code block
                saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);
                analyzeBlock(stmt.asBlockStmt(), slices, variableUsageFreq,loopVars);

            } else {
                // Accumulates sequential statements
                if (currentLabel == null) {
                    currentLabel = "Sequential code block";
                    currentJustification = "Group of sequential instructions without control structures";
                }
                currentSliceStatements.add(stmt);
            }
        }

        saveCurrentSlice(slices, currentLabel, currentJustification, currentSliceStatements, variableUsageFreq,loopVars);
    }

    private static void saveCurrentSlice(List<Slice> slices,
                                         String label,
                                         String justification,
                                         List<Statement> statementsNodes,
                                         Map<String, Integer> variableUsageFreq,
                                         Set<String> loopVars)
    {

        if (statementsNodes != null && !statementsNodes.isEmpty()) {
            Statement first = statementsNodes.get(0);
            Statement last = statementsNodes.get(statementsNodes.size() - 1);

            int beginLine = 0;
            int beginColumn = 0;
            int endLine = 0;
            int endColumn = 0;

            if (first.getRange().isPresent()) {
                beginLine = first.getRange().get().begin.line;
                beginColumn = first.getRange().get().begin.column;
            }

            if (last.getRange().isPresent()) {
                endLine = last.getRange().get().end.line;
                endColumn = last.getRange().get().end.column;
            }

            BlockStmt syntheticBlock = new BlockStmt();
            statementsNodes.forEach(syntheticBlock::addStatement);

            List<String> vars = extractVariables(syntheticBlock);
            Map<String, Integer> outsideCount =
                    calculateUsageOutsideSlice(syntheticBlock, vars, variableUsageFreq, loopVars);
            int totalOutside = outsideCount.values()
                    .stream()
                    .mapToInt(Integer::intValue)
                    .sum();

            List<String> codeLines = new ArrayList<>();
            for (Statement s : statementsNodes) {
                codeLines.add(s.toString());
            }
            System.out.println("codeLines.size: " + codeLines.size());
            String code = String.join("\n", codeLines);

            slices.add(new Slice(
                    label,
                    justification,
//            		codeLines,
                    Collections.singletonList(code),
                    vars,
                    outsideCount,
                    totalOutside,
                    beginLine,
                    beginColumn,
                    endLine,
                    endColumn
            ));

            statementsNodes.clear();
        }
    }

    public static List<String> extractVariables(Node node) {
        Set<String> vars = new HashSet<>();

        // Variables used (NameExpr)
        node.walk(NameExpr.class, nameExpr -> {
            vars.add(nameExpr.getNameAsString());
        });

        // Declared variables (VariableDeclarator)
        node.walk(VariableDeclarator.class, varDecl -> {
            vars.add(varDecl.getNameAsString());
        });

        List<String> sortedVars = new ArrayList<>(vars);
        Collections.sort(sortedVars);
        return sortedVars;
    }

    public static Set<String> extractLoopControlVariables(MethodDeclaration method) {
        Set<String> loopVars = new HashSet<>();

        method.findAll(ForStmt.class).forEach(forStmt -> {
            for (Expression initExpr : forStmt.getInitialization()) {
                if (initExpr.isVariableDeclarationExpr()) {
                    for (VariableDeclarator var : initExpr.asVariableDeclarationExpr().getVariables()) {
                        loopVars.add(var.getNameAsString());
                    }
                }
            }
        });

        method.findAll(ForEachStmt.class).forEach(forEachStmt -> {
            for (VariableDeclarator var : forEachStmt.getVariable().getVariables()) {
                loopVars.add(var.getNameAsString());
            }
        });

        return loopVars;
    }

    public static Map<String, Integer> countVariableUsageInMethod(MethodDeclaration method) {
        Map<String, Integer> freqMap = new HashMap<>();
        method.walk(NameExpr.class, nameExpr -> {
            String varName = nameExpr.getNameAsString();
            freqMap.put(varName, freqMap.getOrDefault(varName, 0) + 1);
        });

        method.walk(VariableDeclarator.class, varDecl -> {
            String varName = varDecl.getNameAsString();
            freqMap.put(varName, freqMap.getOrDefault(varName, 0) + 1);
        });

        return freqMap;
    }

    public static Map<String, Integer> countVariableUsageInSlice(Statement stmt) {
        Map<String, Integer> freqMap = new HashMap<>();
        stmt.walk(NameExpr.class, nameExpr -> {
            String varName = nameExpr.getNameAsString();
            freqMap.put(varName, freqMap.getOrDefault(varName, 0) + 1);
        });

        stmt.walk(VariableDeclarator.class, varDecl -> {
            String varName = varDecl.getNameAsString();
            freqMap.put(varName, freqMap.getOrDefault(varName, 0) + 1);
        });

        return freqMap;
    }

    public static Map<String, Integer> calculateUsageOutsideSlice(Statement stmt, List<String> vars, Map<String, Integer> variableUsageFreq, Set<String> loopVars)
    {
        Map<String, Integer> usageInSlice = countVariableUsageInSlice(stmt);
        Map<String, Integer> usageOutsideSlice = new HashMap<>();

        for (String v : vars) {
            if (loopVars.contains(v)) continue;  // <-- SKIP CONTROL VARIABLE

            int totalInMethod = variableUsageFreq.getOrDefault(v, 0);
            int inSlice = usageInSlice.getOrDefault(v, 0);
            int outside = Math.max(0, totalInMethod - inSlice);
            usageOutsideSlice.put(v, outside);
        }

        return usageOutsideSlice;
    }

    public static boolean deveExcluirSlice(String sourceCodeFragment, String methodCode) {
        // Normalizes the slice
        String frag = sourceCodeFragment.replaceAll("[\\s\\r\\n]+", "");

        // Extracts and normalizes the body of the original method.
        String methodBody = "";
        try {
            CompilationUnit cu = StaticJavaParser.parse("class Temp { " + methodCode + " }");
            MethodDeclaration method = cu.findFirst(MethodDeclaration.class).get();
            String bodyStr = method.getBody().get().toString();
            if (bodyStr.startsWith("{") && bodyStr.endsWith("}")) {
                bodyStr = bodyStr.substring(1, bodyStr.length() - 1);  // remove {}
            }
            methodBody = bodyStr.replaceAll("[\\s\\r\\n]+", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Rule 1: Slice is the same as the whole method.
        if (frag.equals(methodBody)) return true;

        // Rule 2: Standalone break or continue
        boolean containsBreak = frag.contains("break;");
        boolean containsContinue = frag.contains("continue;");
        boolean isSimpleStatement = !frag.contains("{") && !frag.contains("}");

        if (frag.startsWith("throw") && isSimpleStatement) return true;

        if ((containsBreak || containsContinue) && isSimpleStatement) return true;

        return false;
    }
}
