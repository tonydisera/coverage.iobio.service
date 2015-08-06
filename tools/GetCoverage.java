import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/*
*  Title:  GetCoverage
*  Author: Tony Di Sera, Gabor Marth Lab, iobio project
*  Written in July 2015
* 
*  DESCRIPTION
*  -----------
*  A simple java app that takes the output from samtools mpileup and
*  produces the following output records:
*   specific_points
*   1.  Filter the records, keeping those that are the specific positions passed
*       in as an argument
*   reduced_points.
*   2.  Reduce the records down to the maximum points, passed in an argument.
*       To reduce the pont data, the app calculate the median depth over
*       n positions.
*
*   ARGUMENTS
*   ---------
*   This app take 3 arguments, position dependent.  (TODO: make these args into
*   tag/value form and provide usage.)
*   1. maxpoints
*   2. the region (chromosome:start:end)
*   3. the specific positions (chromosome:start:end,chromosome:start:end, etc.)
*
*   Example:
*   >java -classpath ./ GetCoverage 1000 13:130000:150000 13:130044:1130045,13:140042:140043
*    
*    maxpoints = 1000
*    region = chromosome 13 for region spanning 130000 through 150000 
*    positions = 2 positions on chromosome 13; one at position 130044, the other at position 140042
*
*    IMPORTANT - The start position follows the samtools convention of zero-based half closed.
*                Note that the output follows the vcf convention, 1-based.   
*
*   OUTPUT
*   ------
*   The output from this app a set of records.  The records are delimited by
*   newline (\n) and the fields are delimited by tabs. The first field is the 
*   start position and next field is the depth. The data is returned in two
*   sections.  Each section is a header record follwed by detail records.
*
*   Example:
*
*   #specific_points
*   130044 40
*   140042 54
*   #reduced_points
*   130000 33
*   130100 44
*   130200 42
*
*/
public class GetCoverage {
    public static void main(String[] args) {

        int maxPoints = 0;
        String refName = "";
        Integer regionStart = null;
        Integer regionEnd = null;
        ArrayList keepPointList = new ArrayList();

        if (args != null && args.length > 0) {
            maxPoints = Integer.parseInt(args[0]);
        }
        if (args != null && args.length > 1) {
            String[] tokens = args[1].split(":");
            if (tokens.length == 3) {
                regionStart   = Integer.parseInt(tokens[1]);
                regionEnd     = Integer.parseInt(tokens[2]);
            }
        }
        if (args != null && args.length > 2) {
            String[] regions = args[2].split(",");
            for (int i = 0; i < regions.length; i++) {
              // Region arg looks like this 13:10000:20000
              // Grab the start position and increment by 1.
              // This will be the start position to match
              // the the pileup records.
              String region = regions[i];
              String[] regionTokens = region.split(":");
              if (regionTokens.length == 3) {
                refName = regionTokens[0];
                int start      = Integer.parseInt(regionTokens[1]);
                start++;
                keepPointList.add(new Integer(start));
              }
            }
        }
        try {
            BufferedReader br = 
                      new BufferedReader(new InputStreamReader(System.in));
 
            String input;
            ArrayList points = new ArrayList();

     
            while((input=br.readLine())!=null){
                if (input.startsWith("[mpileup]") || input.startsWith("<mpileup>")) {
                    continue;
                }
                String[] tokens = input.split("\t");
                Integer[] row = new Integer[2];
                row[0] = new Integer(Integer.parseInt(tokens[1]));
                row[1] = new Integer(Integer.parseInt(tokens[3]));
                points.add(row);
            }
            // Split out of the coverage point into two arrays -- one of all
            // of the points specified in the regions argument and another
            // for all remaining points.
            ArrayList pointsReserved = new ArrayList();
            ArrayList pointsRemaining = new ArrayList();
            splitPoints(points, keepPointList, pointsReserved, pointsRemaining);

            // We need to reduce the points down.  Take the remaining points (not specified in
            // the region parameter) and reduce it down to n points.
            ArrayList reducedPoints = reducePoints(regionStart.intValue(), regionEnd.intValue(), pointsRemaining, maxPoints);

            // Now output the points specified in the regions followed by the reduced point list.
            System.out.println("#specific_points");
            for (int i = 0; i < pointsReserved.size(); i++) {
              Integer[] row = (Integer[])pointsReserved.get(i);
              System.out.println(row[0] + "\t" + row[1]);
            }
            System.out.println("#reduced_points");
            for (int i = 0; i < reducedPoints.size(); i++) {
                Integer[] row = (Integer[])reducedPoints.get(i);
                System.out.println(row[0] + "\t" + row[1]);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
            System.out.println(e);
        }
    }

    public static void splitPoints(ArrayList points, ArrayList keepPointList, ArrayList pointsReserved, ArrayList pointsRemaining) {
      int pointIter = 0;
      int keepIter = 0;
      
      for( pointIter = 0, keepIter = 0; pointIter < points.size(); ) {
        Integer keepPoint = null;
        if (keepIter < keepPointList.size()) {          
           keepPoint = (Integer)keepPointList.get(keepIter);
        } 

        Integer[] pointRow = null;
        Integer thePoint = null;
        pointRow = (Integer[])points.get(pointIter);
        thePoint = pointRow[0];

        if (keepPoint != null && thePoint != null && (thePoint.intValue() == keepPoint.intValue())) {   
          pointsReserved.add(pointRow);  
          pointIter++;
          keepIter++;
        } else {
            pointsRemaining.add(pointRow);
            pointIter++;
        }
      }

    }

    public static ArrayList reducePoints(int regionStart, int regionEnd, ArrayList points, int maxPoints) {
      // Zero fill the points that were not provided in samtools mpileup
      int regionSize = regionEnd - regionStart;
      regionSize++;

      ArrayList zeroFilledPoints = new ArrayList();
      for (int i = 0; i < regionSize+1; i++) {
        Integer[] row = new Integer[2];
        row[0] = new Integer(regionStart+i);
        row[1] = new Integer(0);
        zeroFilledPoints.add(row);
      }
      for (int i = 0; i < points.size(); i++) {
        Integer[] sourceRow = (Integer[])points.get(i);

        Integer startPos = sourceRow[0];
  
        // bypass the mpileup entry if it is outside of the region
        if (startPos < regionStart || startPos > regionEnd ) {          
        } else {
          int idx = startPos - regionStart;
          Integer[] targetRow = (Integer[])zeroFilledPoints.get(idx);
          targetRow[1] = sourceRow[1];
        }

      }

      if (maxPoints <= 1 ) {
        return points;
      }
      int factor = zeroFilledPoints.size() / maxPoints;
      int modulo = zeroFilledPoints.size() % maxPoints;


      // Don't bother reducing if we aren't reducing at least by a factor of 2
      if (factor <= 1) {
        return points;
      }

      ArrayList results = new ArrayList();
      int sum = 0;
      ArrayList avgWindow = null;
      int windowSize = factor;
      int remainingModulo = modulo;

      // Create a sliding window of averages
      boolean finished = false;
      for(int i = 0; !finished; i += windowSize) {
        if (i >= zeroFilledPoints.size()) {
          finished = true;
          continue;
        }
        Integer[] anchorRow = (Integer[])zeroFilledPoints.get(i);

        // We need to spread the remainder (modulo) over the
        // chunks we are averaging.  Add 1 to each chunk
        // until we have consumed all of the remainder.  Then
        // revert back to the average window size (the factor).
        if (remainingModulo > 0) {
          windowSize = factor + 1;     
          remainingModulo--;     
        } else {
          windowSize = factor;
        }

        // Grab n rows.  This is the window that
        // we will average the points accross.
        avgWindow = new ArrayList();
        for (int x = 0; x < windowSize; x++) {
            if (i+x >= zeroFilledPoints.size()) {
                finished = true;
                continue;
            } 
            avgWindow.add(zeroFilledPoints.get(i+x));
        }  
        windowSize = avgWindow.size();  
        
        int min = 999999;
        int max = 0;
        // Sum the depths in the window
        for (int j = 0; j < windowSize; j++) {
            int y = ((Integer[])avgWindow.get(j))[1].intValue();
            sum += y;

            if (y > max) {
              max = y;
            }
            if (y < min) {
              min = y;
            }
        }
        // Now caclulate the median depth and
        // associate it with a data point
        // that represents the span of the window.
        int average = sum / windowSize;
        Integer[] row = new Integer[2];
        row[0] = anchorRow[0];
        row[1] = new Integer(average);
        results.add(row);
        sum = 0;
      }
      return results;        
    }
}

