package tictactoe;

import java.awt.Point;

public class PowerUp {
    public enum PowerUpType {
        EXTRA_TURN,
        REMOVE_OPPONENT_MARK
    }

    public PowerUpType type;
    public Point position;

    public PowerUp(PowerUpType type, Point position) {
        this.type = type;
        this.position = position;
    }
}
