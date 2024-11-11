package org.khachouch;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        // Remplacez `YourParser` par le nom de votre parseur généré par ANTLR
        Java8Parser parser = new Java8Parser(null);
        // Extraire les terminaux et leurs règles parentes
        List<TerminalParentPair> terminalParentPairs = findAllTerminalsAndTheirParentRules(parser);
        // Exporter les données vers un fichier Excel
        exportToExcel(terminalParentPairs);
    }

    /**
     * Classe interne pour stocker un terminal et sa règle parente
     */
    static class TerminalParentPair {
        String terminal;
        String parentRule;
        String comment;

        TerminalParentPair(String terminal, String parentRule, String comment) {
            this.terminal = terminal;
            this.parentRule = parentRule;
            this.comment = comment;
        }
    }

    /**
     * Trouver tous les terminaux dans chaque règle du parseur et les associer aux règles parentes
     * @param parser le parseur ANTLR contenant les règles
     * @return une liste d'objets TerminalParentPair contenant le terminal, sa règle parente et un commentaire
     */
    public static List<TerminalParentPair> findAllTerminalsAndTheirParentRules(Parser parser) {
        List<TerminalParentPair> result = new ArrayList<>();
        ATN atn = parser.getATN();

        // Parcourir chaque règle dans le parseur
        for (int ruleIndex = 0; ruleIndex < atn.ruleToStartState.length; ruleIndex++) {
            String ruleName = parser.getRuleNames()[ruleIndex];
            ATNState startState = atn.ruleToStartState[ruleIndex];
            Set<Integer> terminals = findTerminalsInRule(startState);

            // Associer chaque terminal trouvé à sa règle parente
            for (Integer terminal : terminals) {
                String terminalName = getTerminalName(parser, terminal);
                if (terminalName != null) {
                    // Ajoute un commentaire précisant si le terminal est un mot-clé ou un identifiant
                    String comment = (terminalName.startsWith("'") && terminalName.endsWith("'"))
                            ? "Littéral (mot-clé)"
                            : "Symbole (identifiant ou autre)";
                    result.add(new TerminalParentPair(terminalName, ruleName, comment));
                }
            }
        }
        return result;
    }

    /**
     * Extraire tous les terminaux dans une règle donnée
     * @param startState l'état de départ de la règle dans l'ATN
     * @return un ensemble d'indices des terminaux trouvés dans la règle
     */
    private static Set<Integer> findTerminalsInRule(ATNState startState) {
        Set<Integer> terminals = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        List<ATNState> stack = new ArrayList<>();
        stack.add(startState);

        // Parcourir l'ATN de la règle pour trouver tous les états de transition terminale
        while (!stack.isEmpty()) {
            ATNState state = stack.remove(stack.size() - 1);
            if (!visited.add(state.stateNumber)) {
                continue; // Ignore les états déjà visités
            }

            // Ajouter les transitions vers les terminaux ou poursuivre le parcours
            for (Transition transition : state.getTransitions()) {
                if (transition instanceof AtomTransition) { // Transition terminale
                    terminals.add(((AtomTransition) transition).label);
                } else {
                    stack.add(transition.target);
                }
            }
        }
        return terminals;
    }

    /**
     * Obtenir le nom d'un terminal soit par un littéral, soit par un symbole
     * @param parser le parseur ANTLR
     * @param terminal l'indice du terminal
     * @return le nom du terminal sous forme de chaîne de caractères
     */
    private static String getTerminalName(Parser parser, int terminal) {
        if (terminal >= 0 && terminal < parser.getTokenTypeMap().size()) {
            String literalName = parser.getVocabulary().getLiteralName(terminal);
            if (literalName != null) return literalName;
            return parser.getVocabulary().getSymbolicName(terminal);
        }
        return null;
    }

    /**
     * Exporter la liste des terminaux et de leurs règles parentes dans un fichier Excel
     * @param terminalParentPairs liste des terminaux avec leurs règles parentes et commentaires
     * @throws IOException si une erreur d'E/S se produit lors de la création du fichier
     */
    private static void exportToExcel(List<TerminalParentPair> terminalParentPairs) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Terminals and Parents");

        // Créer l'en-tête
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Terminal");
        headerRow.createCell(1).setCellValue("Parent Rule");
        headerRow.createCell(2).setCellValue("Comment");

        // Remplir les données des terminaux, règles parentes et commentaires
        int rowNum = 1;
        for (TerminalParentPair pair : terminalParentPairs) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(pair.terminal);
            row.createCell(1).setCellValue(pair.parentRule);
            row.createCell(2).setCellValue(pair.comment);
        }

        // Ajuster la taille des colonnes pour un affichage optimal
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);

        // Écrire le fichier Excel
        try (FileOutputStream fileOut = new FileOutputStream("Terminals_Parents.xlsx")) {
            workbook.write(fileOut);
        }

        workbook.close();
        System.out.println("Fichier Excel généré avec succès : Terminals_Parents.xlsx");
    }
}
