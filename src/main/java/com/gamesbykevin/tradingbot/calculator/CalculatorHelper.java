package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

public class CalculatorHelper {

    /**
     * Check the history <br>
     * We will do 3 things<br>
     * 1) Add the period if it doesn't exist in the history
     * 2) Sort the list so the most recent period is at the end of the array list
     * 3) Verify the list to make sure the gap between each period matches our duration
     * @param history Our current list of history periods
     * @param period The period we want to check
     * @return true if no issues were found, false if there is a gap between periods
     */
    protected static void checkHistory(List<Period> history, Period period) {

        //did we find an existing period
        boolean match = false;

        //check every period
        for (int i = 0; i < history.size(); i++) {

            //if the time matches it already exists
            if (history.get(i).time == period.time) {

                //flag match
                match = true;

                //exit loop
                break;
            }
        }

        //if there was a match we don't need to add
        if (match)
            return;

        //since it wasn't found, add it to the list
        history.add(period);
    }

    /**
     * Sort the list so the most recent period is at the end of the array list
     * @param history Our current list of history periods
     */
    protected static void sortHistory(List<Period> history) {

        //sort so the periods are in order from oldest to newest
        for (int x = 0; x < history.size(); x++) {
            for (int y = x; y < history.size() - 1; y++) {

                //get the current and next period
                Period tmp1 = history.get(x);
                Period tmp2 = history.get(y + 1);

                //if the next object does not have a greater time, we need to swap
                if (tmp1.time > tmp2.time) {

                    //swap the values
                    history.set(x,     tmp2);
                    history.set(y + 1, tmp1);
                }
            }
        }
    }

    /**
     * Get the current trend
     * @param upwardTrend Are we checking for an upward trend
     * @param history Arraylist of historical periods
     * @param currentPrice Current price
     * @param periods Number of periods to check
     * @return If price is constantly upward "Trend.Upward", if price is constantly downward "Trend.Downward", else "Trend.None"
     */
    protected static boolean hasTrend(boolean upwardTrend, List<Period> history, double currentPrice, int periods) {

        //if not large enough skip, this shouldn't happen
        if (history.size() < periods || history.isEmpty())
            return false;

        //we want to start here
        Period begin = history.get(history.size() - periods);

        //if the first price is greater than the current price, there is no uptrend
        if (upwardTrend && begin.close > currentPrice)
            return false;

        //if the first price is less than the current price, there is no downtrend
        if (!upwardTrend && begin.close < currentPrice)
            return false;

        //our coordinates to calculate slope
        final double x1 = 0, y1, x2 = periods, y2;

        //are we detecting and upward or downward trend?
        if (upwardTrend) {
            y1 = begin.low;
            y2 = currentPrice;
        } else {
            y1 = begin.high;
            y2 = currentPrice;
        }

        //the value of y when x = 0
        final double yIntercept = y1;

        //calculate slope
        final double slope = (y2 - y1) / (x2 - x1);

        //check and see if every period is above the slope indicating an upward trend
        for (int i = history.size() - periods; i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //the current x-coordinate
            final double x = i - (history.size() - periods);

            //calculate the y-coordinate
            final double y = (slope * x) + yIntercept;

            //are we checking for an upward trend
            if (upwardTrend) {

                //if the current low is below the calculated y-coordinate slope we have a break
                if (current.low < y)
                    return false;

            } else {

                //if the current high is above the calculated y-coordinate slope we have a break
                if (current.high > y)
                    return false;
            }
        }

        //we have a trend
        return true;
    }

    /**
     * Do we have divergence?
     *
     * What we do to detect bullish divergence:
     * a) The first period the price is highest and the last period is the lowest (new low)
     * b) The first period the data is lowest and the last period is the highest (new high)
     *
     * What we do to detect bearish divergence:
     * a) The first period the price is the lowest and the last period is the highest (new high)
     * b) The first period the data is the highest and the last period is the lowest (new low)
     *
     * @param history Stock price history
     * @param periods How many periods do we check
     * @param bullish Are we looking for a bullish sign of divergence, if false we are looking for bearish
     * @param data Array data of our indicator
     * @return true if divergence is found, false otherwise
     */
    protected static synchronized boolean hasDivergence(List<Period> history, int periods, boolean bullish, List<Double> data) {

        if (bullish) {

            //now check the previous x periods to see if this is a new low
            for (int i = history.size() - periods; i < history.size() - 1; i++) {

                //at this point we want prices that are lower than the starting price
                if (history.get(i).close > history.get(history.size() - periods).close)
                    return false;

                //if this current closing price is <= our latest price, we don't have a new low
                if (history.get(i).close <= history.get(history.size() - 1).close)
                    return false;
            }

            //lets make sure the indicator data is higher than the start period
            for (int i = data.size() - periods; i < data.size(); i++) {

                //at this point we want every data point to be larger than the first
                if (data.get(i) < data.get(data.size() - periods))
                    return false;

                //and we want every data point to be smaller than the last
                if (data.get(i) > data.get(data.size() - 1))
                    return false;
            }

            //the current price is a new low, and the indicator data is increasing, we have a bullish divergence
            return true;

        } else {

            //now check the previous x periods to see if this is a new high
            for (int i = history.size() - periods; i < history.size() - 1; i++) {

                //at this point we want prices that are higher than the starting price
                if (history.get(i).close < history.get(history.size() - periods).close)
                    return false;

                //if this current closing price is >= our latest price, we don't have a new high
                if (history.get(i).close >= history.get(history.size() - 1).close)
                    return false;
            }

            //lets make sure the indicator data is lower than the start period
            for (int i = data.size() - periods; i < data.size(); i++) {

                //at this point we want every data point to be smaller than the first
                if (data.get(i) > data.get(data.size() - periods))
                    return false;

                //and we want every data point to be larger than the last
                if (data.get(i) < data.get(data.size() - 1))
                    return false;
            }

            //the current price is a new high, and the indicator data is decreasing, we have a bearish divergence
            return true;
        }
    }

    /**
     * Do we have crossover?
     *
     * If bullish we look for this:
     * a) We check the previous data where fast < slow
     * b) Then we check the current data where fast > slow
     *
     * If bearish we look for this:
     * a) We check the previous data where slow < fast
     * b) Then we check the current data where slow > fast
     *
     * @param bullish Are we checking bullish? if false we are checking bearish
     * @param fast Array of fast moving data
     * @param slow Array of slow moving data
     * @return true if crossover, false otherwise
     */
    protected static boolean hasCrossover(boolean bullish, List<Double> fast, List<Double> slow) {

        //our previous slow and fast values
        double previousSlow = slow.get(slow.size() - 2);
        double previousFast = fast.get(fast.size() - 2);

        //our current slow and fast values
        double currentSlow = slow.get(slow.size() - 1);
        double currentFast = fast.get(fast.size() - 1);

        //if we are checking bullish the long is greater then the short is greater
        if (bullish) {

            //to start we want the previous slow to be greater than the previous fast
            if (previousSlow > previousFast) {

                //now we want the current fast to be greater than the current slow
                if (currentFast > currentSlow)
                    return true;
            }

        } else {

            //to start we want the previous fast to be greater than the previous slow
            if (previousFast > previousSlow) {

                //now we want the current slow to be greater than the current fast
                if (currentSlow > currentFast)
                    return true;
            }

        }

        //no crossover detected
        return false;
    }
}