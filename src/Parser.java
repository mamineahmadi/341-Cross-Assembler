import ir.IntermediateRep;
import ir.LineStatement;
import lexical.LexicalScanner;
import lexical.token.*;
import utils.SymbolTable;

public class Parser implements IParser {
    //Sequence of line statements
    private final IntermediateRep ir = new IntermediateRep();
    // private final LinkedList<ir.LineStatement> intermediateRep = new LinkedList<>();
    private final LexicalScanner lexicalScanner;
    private final SymbolTable<String, Mnemonic> keywords;
    private Token nextToken;
    private IErrorReporter errorReporter;

    public Parser(LexicalScanner lexicalScanner, SymbolTable<String, Mnemonic> keywords, IErrorReporter errorRep) {
        this.lexicalScanner = lexicalScanner;
        this.keywords = keywords;
        this.nextToken = lexicalScanner.getNextToken();
        this.errorReporter = errorRep;
    }

    /**
     * Generates line statements based upon tokens from the lexical analyzer.
     * Currently supports only Mnemomics.
     * <p>
     * <p>
     * Each LineStatement is composed of three optional parts (a label,
     * an instruction/directive and a comment) followed by an end-of-line (EOL).
     * <p>
     * LineStatement = [ Label ] [ Instruction | Directive ] [ Comment ] EOL .
     */
    public void parseTokens() {
        LineStatement ls = new LineStatement();
        // System.out.println("[Debug] Parsing tokens...");
        while (nextToken.getType() != TokenType.EOF) {
//            System.out.println("[Debug]" + this.nextToken);
            // System.out.println("[Debug] Parsing Token " + nextToken.toString());

            TokenType type = nextToken.getType();
            Position position = nextToken.getPosition();
            String value = nextToken.getValue();

            switch (type) {

                case EOL:               //If token is EOL, LS is finished, add it to IR and start a new one.
                    //System.out.println("[Debug] - " + ls);

                    if (ls.getInstruction() != null) {
                        if (ls.getInstruction().getOperand() == null) {
                            ls.getInstruction().getMnemonic().setMode("inherent");
                        }
                    }

                    //Error Reporting
                    errorReporting(ls);
                    //System.out.println("[Debug] - Line statement - " + ls.getInstruction());
                    ir.add(ls);
                    ls = new LineStatement();
                    break;

                case COMMENT:           // If token is a comment
                    ls.setComment(new Comment(position, value));
                    break;

                case MNEMONIC:          //If token is a mnemonic
                    ls.setInstruction(new Instruction(position, value)); // Set instruction
                    //Set newly created instruction's mnemonic
                    ls.getInstruction().setMnemonic(new Mnemonic(position, value));
                    break;

                case OPERAND:

                    //System.out.println("[Debug] - " + nextToken);
                    ls.getInstruction().getMnemonic().setOpCode(Integer.parseInt(value)); //Set mnemonic's opcode
                    Operand operand = new Operand(position, value);
                    operand.setOperand(Integer.parseInt(value));
                    ls.getInstruction().setOperand(operand); //set instruction's opcode
                    ls.getInstruction().getMnemonic().setMode("immediate");
                    break;

                case UNKNOWN:   // Unknown token
                    ErrorMsg unknown_token = new ErrorMsg("Unknown token", nextToken.getPosition());
                    errorReporter.record(unknown_token);

            }

            //Get the next token to process
            //System.out.println(nextToken);
            getNextToken();

        }
//        System.out.println("[Debug]" + this.nextToken);
        //System.out.println("[Debug] Done parsing tokens...");
        //System.out.println(Arrays.toString(intermediateRep.toArray()));
    }

    public IntermediateRep getIR() {
        return this.ir;
    }

    /**
     * Analyzes line statements and report error if one is found.
     *
     * @param ls
     */
    public void errorReporting(LineStatement ls) {
        ErrorMsg errorMsg = new ErrorMsg();
        Token token = nextToken;
        if (ls.getInstruction() != null) {//if no instruction then we assume its a line with only a comment and ignore it
            if (keywords.get(ls.getInstruction().getMnemonic().getValue()) == null) { //If Mnemonic not found in symbol table, it is considered invalid
                errorMsg.setMessage("Invalid mnemonic or directive.");
            } else if (keywords.get(ls.getInstruction().getMnemonic().getValue()) != null) {
                if (!keywords.get(ls.getInstruction().getMnemonic().getValue()).getMode().equals("inherent") && ls.getInstruction().getOperand() == null) { //If instruction in not inherent (immediate or relative) but does not have an operand
                    errorMsg.setMessage("Instruction requires an operand.");
                } else if (keywords.get(ls.getInstruction().getMnemonic().getValue()).getMode().equals("inherent") && ls.getInstruction().getOperand() != null) { //If instruction is inherent but contains an operand
                    errorMsg.setMessage("Inherent instruction must not have an operand.");
                }
            }
            if (!errorMsg.msg.isEmpty()) {
                errorMsg.setPosition(token.getPosition());
                this.errorReporter.record(errorMsg);
            }
        }
    }

    private void getNextToken() {
        this.nextToken = lexicalScanner.getNextToken();
    }
}
