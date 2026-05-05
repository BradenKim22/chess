package chess;

import java.util.Objects;

/**
 * Represents a single square position on a chess board
 */
public class ChessPosition {

    private int row;
    private int column;

    public ChessPosition(int row, int col) {
        this.row = row;
        this.column = col;
    }

    /**
     * @return which row this position is in
     * 1 codes for the bottom row
     */
    public int getRow() {
        return row;
    }

    /**
     * @return which column this position is in
     * 1 codes for the left column
     */
    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessPosition)) return false;
        ChessPosition that = (ChessPosition) o;
        return row == that.row && column == that.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, column);
    }

    @Override
    public String toString() {
        return "(" + row + "," + column + ")";
    }
}