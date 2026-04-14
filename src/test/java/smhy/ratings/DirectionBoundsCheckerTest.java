package smhy.ratings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectionBoundsCheckerTest {

    private final DirectionBoundsChecker directionBoundsChecker = new DirectionBoundsChecker();

    @Test
    void isDirectionWithinBounds() {
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(0, 0, 0));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(0, 0, -1));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(0, -1, 0));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(0, -1, 1));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(-1, -2, 0));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(-1, -3, -1));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(0, -1, -1));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(1, 0, 2));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(90, 45, 180));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(180, 90, 270));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(0, -90, 90));
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(300, -90, 90));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(0, -2, -1));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(-3, -2, -1));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(40, 45, 180));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(200, 45, 180));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(80, 90, 270));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(280, 90, 270));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(-95, -90, 90));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(95, -90, 90));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(265, -90, 90));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(-265, -90, 90));
    }

    @Test
    void isDirectionWithinBounds_wrapAroundRange_directionInExcludedZone() {
        // min=90, max=-90: wrap-around range covers [90..180] and [-180..-90].
        // Direction 0 is in the excluded gap [-89..89] — not >= 90 and not <= -90.
        // This covers the final "return false" in the else branch.
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(0, 90, -90));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(45, 90, -90));
        Assertions.assertFalse(directionBoundsChecker.isDirectionWithinBounds(-45, 90, -90));
    }

    @Test
    void isDirectionWithinBounds_wrapAroundRange_directionMatchesMin() {
        // Direction 135 >= min(90): returns true via the first if in the else branch.
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(135, 90, -90));
    }

    @Test
    void isDirectionWithinBounds_wrapAroundRange_directionMatchesMax() {
        // Direction -135 <= max(-90): returns true via the second if in the else branch.
        Assertions.assertTrue(directionBoundsChecker.isDirectionWithinBounds(-135, 90, -90));
    }
}
