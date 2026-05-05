package chess;

import java.util.Arrays;
import java.util.Objects;

/**
 * A chessboard that can hold and rearrange chess pieces.
 */
public class ChessBoard {

    private ChessPiece[][] board;

    public ChessBoard() {
        board = new ChessPiece[8][8];
    }

    /**
     * Adds a chess piece to the chessboard
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        board[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the chessboard
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        return board[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     */
    public void resetBoard() {
        board = new ChessPiece[8][8];

        ChessPiece.PieceType[] backRow = {
                ChessPiece.PieceType.ROOK,
                ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.QUEEN,
                ChessPiece.PieceType.KING,
                ChessPiece.PieceType.BISHOP,
                ChessPiece.PieceType.KNIGHT,
                ChessPiece.PieceType.ROOK
        };

        for (int col = 1; col <= 8; col++) {
            addPiece(new ChessPosition(1, col),
                    new ChessPiece(ChessGame.TeamColor.WHITE, backRow[col - 1]));

            addPiece(new ChessPosition(2, col),
                    new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN));

            addPiece(new ChessPosition(7, col),
                    new ChessPiece(ChessGame.TeamColor.BLACK, ChessPiece.PieceType.PAWN));

            addPiece(new ChessPosition(8, col),
                    new ChessPiece(ChessGame.TeamColor.BLACK, backRow[col - 1]));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChessBoard that)) {
            return false;
        }
        return Arrays.deepEquals(board, that.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }

    @Override
    public String toString() {
        return Arrays.deepToString(board);
    }
}