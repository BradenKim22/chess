package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a single chess piece
 */
public class ChessPiece {

    private ChessGame.TeamColor pieceColor;
    private PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ArrayList<ChessMove> moves = new ArrayList<>();

        switch (type) {
            case ROOK -> addSlidingMoves(board, myPosition, moves, new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1}
            });
            case BISHOP -> addSlidingMoves(board, myPosition, moves, new int[][]{
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            });
            case QUEEN -> addSlidingMoves(board, myPosition, moves, new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            });
            case KING -> addSingleStepMoves(board, myPosition, moves, new int[][]{
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
            });
            case KNIGHT -> addSingleStepMoves(board, myPosition, moves, new int[][]{
                    {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                    {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
            });
            case PAWN -> addPawnMoves(board, myPosition, moves);
        }

        return moves;
    }

    private void addSlidingMoves(ChessBoard board, ChessPosition start,
                                 ArrayList<ChessMove> moves, int[][] directions) {
        for (int[] direction : directions) {
            int row = start.getRow() + direction[0];
            int col = start.getColumn() + direction[1];

            while (isOnBoard(row, col)) {
                ChessPosition end = new ChessPosition(row, col);
                ChessPiece pieceAtEnd = board.getPiece(end);

                if (pieceAtEnd == null) {
                    moves.add(new ChessMove(start, end, null));
                } else {
                    if (pieceAtEnd.getTeamColor() != this.pieceColor) {
                        moves.add(new ChessMove(start, end, null));
                    }
                    break;
                }

                row += direction[0];
                col += direction[1];
            }
        }
    }

    private void addSingleStepMoves(ChessBoard board, ChessPosition start,
                                    ArrayList<ChessMove> moves, int[][] offsets) {
        for (int[] offset : offsets) {
            int row = start.getRow() + offset[0];
            int col = start.getColumn() + offset[1];

            if (isOnBoard(row, col)) {
                ChessPosition end = new ChessPosition(row, col);
                ChessPiece pieceAtEnd = board.getPiece(end);

                if (pieceAtEnd == null || pieceAtEnd.getTeamColor() != this.pieceColor) {
                    moves.add(new ChessMove(start, end, null));
                }
            }
        }
    }

    private void addPawnMoves(ChessBoard board, ChessPosition start,
                              ArrayList<ChessMove> moves) {
        int direction;
        int startingRow;
        int promotionRow;

        if (pieceColor == ChessGame.TeamColor.WHITE) {
            direction = 1;
            startingRow = 2;
            promotionRow = 8;
        } else {
            direction = -1;
            startingRow = 7;
            promotionRow = 1;
        }

        int oneForwardRow = start.getRow() + direction;
        int col = start.getColumn();

        // Move forward one square
        if (isOnBoard(oneForwardRow, col)) {
            ChessPosition oneForward = new ChessPosition(oneForwardRow, col);

            if (board.getPiece(oneForward) == null) {
                addPawnMoveOrPromotion(start, oneForward, moves, promotionRow);

                // Move forward two squares from starting row
                int twoForwardRow = start.getRow() + (2 * direction);
                ChessPosition twoForward = new ChessPosition(twoForwardRow, col);

                if (start.getRow() == startingRow &&
                        isOnBoard(twoForwardRow, col) &&
                        board.getPiece(twoForward) == null) {
                    moves.add(new ChessMove(start, twoForward, null));
                }
            }
        }

        // Capture diagonally
        int[] captureCols = {start.getColumn() - 1, start.getColumn() + 1};

        for (int captureCol : captureCols) {
            int captureRow = start.getRow() + direction;

            if (isOnBoard(captureRow, captureCol)) {
                ChessPosition capturePosition = new ChessPosition(captureRow, captureCol);
                ChessPiece pieceAtCapture = board.getPiece(capturePosition);

                if (pieceAtCapture != null && pieceAtCapture.getTeamColor() != this.pieceColor) {
                    addPawnMoveOrPromotion(start, capturePosition, moves, promotionRow);
                }
            }
        }
    }

    private void addPawnMoveOrPromotion(ChessPosition start, ChessPosition end,
                                        ArrayList<ChessMove> moves, int promotionRow) {
        if (end.getRow() == promotionRow) {
            moves.add(new ChessMove(start, end, PieceType.QUEEN));
            moves.add(new ChessMove(start, end, PieceType.BISHOP));
            moves.add(new ChessMove(start, end, PieceType.KNIGHT));
            moves.add(new ChessMove(start, end, PieceType.ROOK));
        } else {
            moves.add(new ChessMove(start, end, null));
        }
    }

    private boolean isOnBoard(int row, int col) {
        return row >= 1 && row <= 8 && col >= 1 && col <= 8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessPiece)) return false;
        ChessPiece that = (ChessPiece) o;
        return pieceColor == that.pieceColor && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    @Override
    public String toString() {
        return pieceColor + " " + type;
    }
}