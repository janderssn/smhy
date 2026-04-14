package smhy.ratings;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DegreeToDirectionConverterTest {

    private final DegreeToDirectionConverter converter = new DegreeToDirectionConverter();

    double[] n = {350,355,0,5,10};
    double[] nne = {15,20,25,30};
    double[] ne = {35,40,45,50,55};
    double[] ene = {60,65,70,75};
    double[] e = {80,85,90,95,100};
    double[] ese = {105,110,115,120};
    double[] se = {125,130,135,140,145};
    double[] sse = {150,155,160,165};
    double[] s = {170,175,180,185,190};
    double[] ssw = {195,200,205,210};
    double[] sw = {215,220,225,230,235};
    double[] wsw = {240,245,250,255};
    double[] w = {260,265,270,275,280};
    double[] wnw = {285,290,295,300};
    double[] nw = {305,310,315,320,325};
    double[] nnw = {330,335,340,345};

    double[][] degrees = {n,nne,ne,ene,e,ese,se,sse,s,ssw,sw,wsw,w,wnw,nw,nnw};
    String[] expected = {"N","NNE","NE","ENE","E","ESE","SE","SSE","S","SSW","SW","WSW","W","WNW","NW","NNW"};

    @Test
    void convert() {
        for (int i=0; i<expected.length; i++) {
            final String s = expected[i];
            assertTrue(Arrays.stream(degrees[i]).mapToObj(converter::convert).allMatch(s::equals));
        }
    }
}