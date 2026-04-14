package smhy.ratings;

import org.springframework.stereotype.Component;

/**
 * Utility class used to check if a direction (in degrees) is within specified bounds.
 */
@Component
public class DirectionBoundsChecker {

    /**
     * Returns true if the given direction is within the specified minDirection and maxDirection.
     * @param direction the direction to check
     * @param minDirection the minimum direction
     * @param maxDirection the maximum direction
     * @return true if the direction is within bounds, otherwise returns false.
     */
    public boolean isDirectionWithinBounds(double direction, double minDirection, double maxDirection) {
        direction = normalize(direction);
        maxDirection = normalize(maxDirection);
        minDirection = normalize(minDirection);
        if (minDirection < maxDirection) {
            return direction >= minDirection && direction <= maxDirection;
        } else {
            if (direction >= minDirection) {
                return true;
            }
            if (direction <= maxDirection) {
                return true;
            }
        }
        return false;
    }

    private static double normalize(double angle) {
        if (angle < -180) {
            return angle + 360;
        }
        if (angle > 180) {
            return angle - 360;
        }
        return angle;
    }

}
