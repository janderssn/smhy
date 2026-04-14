package smhy.ratings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smhy.conditions.ForecastCondition;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForecastRatingServiceLocationTest extends AbstractForecastRatingTest {

    @BeforeEach
    void setUp() {
        super.setUp();
    }

    @Test
    void rateForecastSalanda() {

        Collection<ForecastCondition> salanda = conditionsFor("salanda");

        assertEquals( 80.0, score(salanda, weakWindFrom(N)));
        assertEquals( 80.0, score(salanda, weakWindFrom(NNW)));
        assertEquals( 80.0, score(salanda, weakWindFrom(S)));
        assertEquals( 80.0, score(salanda, weakWindFrom(NW)));
        assertEquals( 80.0, score(salanda, weakWindFrom(SSE)));
        assertEquals( 50.0, score(salanda, weakWindFrom(SSW)));
        assertEquals( 50.0, score(salanda, weakWindFrom(NNE)));
        assertEquals( 50.0, score(salanda, weakWindFrom(NE)));
        assertEquals(  0.0, score(salanda, weakWindFrom(E)));
        assertEquals(  0.0, score(salanda, weakWindFrom(W)));
    }

    @Test
    void rateForecastAlleberg() {

        Collection<ForecastCondition> alleberg = conditionsFor("alleberg");

        assertEquals(100.0, score(alleberg, windFrom(6, NW)));
        assertEquals(100.0, score(alleberg, windFrom(6, WNW)));
        assertEquals( 50.0, score(alleberg, windFrom(7, NW)));
        assertEquals( 50.0, score(alleberg, windFrom(6, W)));
        assertEquals( 50.0, score(alleberg, windFrom(6, WSW)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(N)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(NNE)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(NNW)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(S)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(SSE)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(SSW)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(NE)));
        assertEquals(  0.0, score(alleberg, weakWindFrom(NW)));
    }

}