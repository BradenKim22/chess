package ui;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

public class BoardPrinter {

    public void printBoard(ChessGame.TeamColor perspective) {
        ChessBoard board = new ChessBoard();
        board.resetBoard();

        if (perspective == ChessGame.TeamColor.BLACK) {
            printBlackPerspective(board);
        } else {
            printWhitePerspective(board);
        }

        resetColors();
    }

    private void printWhitePerspective(ChessBoard board) {
        printHeaderWhite();

        for (int row = 8; row >= 1; row--) {
            printRow(board, row, 1, 8, 1);
        }

        printHeaderWhite();
    }

    private void printBlackPerspective(ChessBoard board) {
        printHeaderBlack();

        for (int row = 1; row <= 8; row++) {
            printRow(board, row, 8, 1, -1);
        }

        printHeaderBlack();
    }

    private void printHeaderWhite() {
        System.out.println("    a  b  c  d  e  f  g  h");
    }

    private void printHeaderBlack() {
        System.out.println("    h  g  f  e  d  c  b  a");
    }

    private void printRow(ChessBoard board, int row, int startCol, int endCol, int step) {
        System.out.print(" " + row + " ");

        for (int col = startCol; col != endCol + step; col += step) {
            setSquareColor(row, col);
            ChessPiece piece = board.getPiece(new ChessPosition(row, col));
            System.out.print(pieceSymbol(piece));
        }

        resetColors();
        System.out.println(" " + row);
    }

    private void setSquareColor(int row, int col) {
        boolean lightSquare = (row + col) % 2 == 0;

        if (lightSquare) {
            System.out.print(EscapeSequences.SET_BG_COLOR_WHITE);
        } else {
            System.out.print(EscapeSequences.SET_BG_COLOR_DARK_GREY);
        }

        System.out.print(EscapeSequences.SET_TEXT_COLOR_BLACK);
    }

    private String pieceSymbol(ChessPiece piece) {
        if (piece == null) {
            return EscapeSequences.EMPTY;
        }

        boolean whitePiece = piece.getTeamColor() == ChessGame.TeamColor.WHITE;

        return switch (piece.getPieceType()) {
            case KING -> whitePiece ? EscapeSequences.WHITE_KING : EscapeSequences.BLACK_KING;
            case QUEEN -> whitePiece ? EscapeSequences.WHITE_QUEEN : EscapeSequences.BLACK_QUEEN;
            case BISHOP -> whitePiece ? EscapeSequences.WHITE_BISHOP : EscapeSequences.BLACK_BISHOP;
            case KNIGHT -> whitePiece ? EscapeSequences.WHITE_KNIGHT : EscapeSequences.BLACK_KNIGHT;
            case ROOK -> whitePiece ? EscapeSequences.WHITE_ROOK : EscapeSequences.BLACK_ROOK;
            case PAWN -> whitePiece ? EscapeSequences.WHITE_PAWN : EscapeSequences.BLACK_PAWN;
        };
    }

    private void resetColors() {
        System.out.print(EscapeSequences.RESET_TEXT_COLOR);
        System.out.print(EscapeSequences.RESET_BG_COLOR);
    }
}