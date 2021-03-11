package lexical;

import lexical.token.Mnemonic;
import lexical.token.Position;
import lexical.token.Token;
import lexical.token.TokenType;
import utils.StringUtils;
import utils.SymbolTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class LexicalScanner implements ILexicalScanner {

    private int lineNumber = 1;
    private int columnNumber = 0;
    private int currentColumn = 0;

    private FileInputStream fis = null;
    private SymbolTable<String, Token> keywords;

    public LexicalScanner(String inputFile) {
        try {
            File file = new File(inputFile);
            file.setWritable(false);
            fis = new FileInputStream(file);
            keywords = new SymbolTable<>();
            initKeywordTable();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Initializes the keyword table to allow verification of valid mnemonics and directions (in the future).
     */
    public void initKeywordTable() {
        keywords.put("halt", new Mnemonic("halt", 0x00));
        keywords.put("pop", new Mnemonic("pop", 0x01));
        keywords.put("dup", new Mnemonic("dup", 0x02));
        keywords.put("exit", new Mnemonic("exit", 0x03));
        keywords.put("ret", new Mnemonic("ret", 0x04));
        keywords.put("not", new Mnemonic("not", 0x0C));
        keywords.put("and", new Mnemonic("and", 0x0D));
        keywords.put("or", new Mnemonic("or", 0x0E));
        keywords.put("xor", new Mnemonic("xor", 0x0F));
        keywords.put("neg", new Mnemonic("neg", 0x10));
        keywords.put("inc", new Mnemonic("inc", 0x11));
        keywords.put("dec", new Mnemonic("dec", 0x12));
        keywords.put("add", new Mnemonic("add", 0x13));
        keywords.put("sub", new Mnemonic("sub", 0x14));
        keywords.put("mul", new Mnemonic("mul", 0x15));
        keywords.put("div", new Mnemonic("div", 0x16));
        keywords.put("rem", new Mnemonic("rem", 0x17));
        keywords.put("shl", new Mnemonic("shl", 0x18));
        keywords.put("shr", new Mnemonic("shr", 0x19));
        keywords.put("teq", new Mnemonic("teq", 0x1A));
        keywords.put("tne", new Mnemonic("tne", 0x1B));
        keywords.put("tlt", new Mnemonic("tlt", 0x1C));
        keywords.put("tgt", new Mnemonic("tgt", 0x1D));
        keywords.put("tle", new Mnemonic("tle", 0x1E));
        keywords.put("tge", new Mnemonic("tge", 0x1F));
        keywords.put("enter.u5", new Mnemonic("enter.u5", 0x70));
        keywords.put("ldc.i3", new Mnemonic("ldc.i3", 0x90));
        keywords.put("addv.u3", new Mnemonic("addv.u3", 0x98));
        keywords.put("ldv.u3", new Mnemonic("ldv.u3", 0xA0));
        keywords.put("stv.u3", new Mnemonic("stv.u3", 0xA8));
        //Will add the rest when we get to there
    }

    /**
     * Returns the next token. Supports Mnemonic of inherent/immediate types,
     * instruction, comment, EOL and EOF.
     *
     * @return the next token
     */
    public Token getNextToken() {
        StringBuilder sb = new StringBuilder();
        String alphabet = "[a-zA-Z]*$";
        String numbers = "[0-9]*$";
        int c = readChar();

        //   System.out.println([DEBUG] c);

        //skip ignored characters until we reach a valid character
        while (StringUtils.isIgnoredCharacter(c)) {
            c = readChar();
        }
        //label

        //Mnemonic inherent/immediate
        if (String.valueOf((char) c).matches(alphabet)) {
            return readAddressing(c, sb);
        }

        //instruction
        if (StringUtils.isMinusSign(c) || String.valueOf((char) c).matches(numbers)) {
            return readOperand(c, sb);
        }

        //comment
        if (StringUtils.isSemicolon(c)) {
            return readComment(c, sb);
        }

        //Check if next valid character is an EOL
        if (StringUtils.isEOL(c)) {
            return new Token(new Position(0, 0), "EOL", TokenType.EOL);
        }

        //Check EOF
        if (c == 65535 || c == '\0') {
            return new Token(new Position(0, 0), "EOF", TokenType.EOF);
        }

        //If it ever gets here, it should be reported as an error
        return new Token(new Position(0, 0), "UNKNOWN", TokenType.UNKNOWN);
    }

    /**
     * Returns the next available character.
     *
     * @return next character
     */
    private char readChar() {
        try {
            return (char) fis.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return '\0';
    }

    /**
     * Reads the .asm file for the next available mnemonic. It can be either immediate/inherent addressing.
     *
     * @param c
     * @return mnemonic token
     */
    private Token readAddressing(int c, StringBuilder sb) {
        while (!StringUtils.isSpace(c)) {
            //continue reading each character
            sb.append((char) c);
            c = readChar();
        }
        return new Token(new Position(0, 0), sb.toString(), TokenType.MNEMONIC);
    }

    /**
     * Returns the token of the operand (?). Supports negative values.
     * I am under the impression the operand is the number after the mnemonic and before the comment.
     *
     * @param c
     * @param sb
     * @return instruction token
     */
    private Token readOperand(int c, StringBuilder sb) {
        while (!StringUtils.isSpace(c)) {
            //continue reading each character
            sb.append((char) c);
            c = readChar();
        }
        return new Token(new Position(0, 0), sb.toString(), TokenType.OPERAND);
    }

    /**
     * Returns the comment token.
     *
     * @param c
     * @param sb
     * @return comment token
     */
    private Token readComment(int c, StringBuilder sb) {
        while (!StringUtils.isEOL(c)) {
            //continue reading each character
            sb.append((char) c);
            c = readChar();
            //Don't include this return as part of the comment
            if (c == '\r') {
                break;
            }
        }
        return new Token(new Position(0, 0), sb.toString(), TokenType.COMMENT);
    }

    public SymbolTable<String, Token> getKeywords() {
        return keywords;
    }
}
