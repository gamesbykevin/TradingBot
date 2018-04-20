package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

public class ADX extends Strategy {

    //list of sma prices
    private List<Double> smaPrice;

    //the average directional index
    private List<Double> adx;

    //our +- indicators to calculate adx and signal trades
    private List<Double> dmPlusIndicator;
    private List<Double> dmMinusIndicator;

    //list of configurable values
    private static int PERIODS_SMA = 50;
    private static int PERIODS_ADX = 14;
    private static double TREND_ADX = 20.0d;

    private final int periodsSMA, periodsADX;

    private final double trendAdx;

    public ADX() {
        this(PERIODS_SMA, PERIODS_ADX, TREND_ADX);
    }

    public ADX(int periodsSMA, int periodsADX, double trendAdx) {

        //create our lists
        this.adx = new ArrayList<>();
        this.dmPlusIndicator = new ArrayList<>();
        this.dmMinusIndicator = new ArrayList<>();
        this.smaPrice = new ArrayList<>();

        this.periodsSMA = periodsSMA;
        this.periodsADX = periodsADX;
        this.trendAdx = trendAdx;
    }

    public List<Double> getAdx() {
        return this.adx;
    }

    public List<Double> getDmPlusIndicator() {
        return this.dmPlusIndicator;
    }

    public List<Double> getDmMinusIndicator() {
        return this.dmMinusIndicator;
    }

    public List<Double> getSmaPrice() {
        return this.smaPrice;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the most recent adx value is above the trend
        if (getRecent(getAdx()) > trendAdx) {

            //if the current stock price is above our sma average
            if (getRecent(history, Fields.Close) > getRecent(getSmaPrice())) {

                //if dm plus crosses above dm minus, that is our signal to buy
                if (hasCrossover(true, getDmPlusIndicator(), getDmMinusIndicator()))
                    agent.setBuy(true);
            }
        }

        //display data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the most recent adx value is above the trend
        if (getRecent(getAdx()) > trendAdx) {

            //if the current stock price is below our sma average
            if (getRecent(history, Fields.Close) < getRecent(getSmaPrice())) {

                //if the minus has crossed below the plus that is our signal to sell
                if (hasCrossover(false, getDmPlusIndicator(), getDmMinusIndicator()))
                    agent.setReasonSell(TransactionHelper.ReasonSell.Reason_Strategy);
            }
        }

        //display data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    protected void displayData(Agent agent, boolean write) {

        //display the recent values which we use as a signal
        display(agent, "+DI: ", getDmPlusIndicator(), write);
        display(agent, "-DI: ", getDmMinusIndicator(), write);
        displayMessage(agent, "ADX: " + getRecent(getAdx()), write);
        display(agent, "SMA: ", getSmaPrice(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear the list(s) before we calculate
        getAdx().clear();
        getDmPlusIndicator().clear();
        getDmMinusIndicator().clear();

        //our temp lists
        List<Double> tmpDmPlus = new ArrayList<>();
        List<Double> tmpDmMinus = new ArrayList<>();
        List<Double> tmpTrueRange = new ArrayList<>();

        //calculate for the entire history that we have
        for (int i = 0; i < history.size(); i++) {

            //we can't check the previous period here
            if (i <= 0)
                continue;

            //get the current and previous periods
            Period previous = history.get(i - 1);
            Period current = history.get(i);

            //calculate the high difference
            double highDiff = current.high - previous.high;

            //calculate the low difference
            double lowDiff = current.low - previous.low;

            //which values do we set
            if (highDiff > lowDiff) {

                //if less than 0, zero will be assigned
                if (highDiff < 0)
                    highDiff = 0d;

                tmpDmPlus.add(highDiff);
                tmpDmMinus.add(0d);

            } else {

                //if less than 0, zero will be assigned
                if (lowDiff < 0)
                    lowDiff = 0d;

                tmpDmPlus.add(0d);
                tmpDmMinus.add(lowDiff);
            }

            //current high minus current low
            double method1 = current.high - current.low;

            //if we have the previous period, current high minus previous period close (absolute value)
            double method2 = (previous == null) ? 0d : Math.abs(current.high - previous.close);

            //if we have the previous period, current low minus previous period close (absolute value)
            double method3 = (previous == null) ? 0d : Math.abs(current.low - previous.close);

            //the true range will be the greatest value of the 3 methods
            if (method1 >= method2 && method1 >= method3) {
                tmpTrueRange.add(method1);
            } else if (method2 >= method1 && method2 >= method3) {
                tmpTrueRange.add(method2);
            } else if (method3 >= method1 && method3 >= method2) {
                tmpTrueRange.add(method3);
            } else {
                tmpTrueRange.add(method1);
            }
        }

        List<Double> dmPlus = new ArrayList<>();
        List<Double> dmMinus = new ArrayList<>();
        List<Double> trueRange = new ArrayList<>();

        //smooth the values
        smooth(tmpDmMinus, dmMinus,     periodsADX);
        smooth(tmpDmPlus, dmPlus,       periodsADX);
        smooth(tmpTrueRange, trueRange, periodsADX);

        for (int i = 0; i < dmPlus.size(); i++) {

            //calculate the +- indicators
            double newPlus = (dmPlus.get(i) / trueRange.get(i)) * 100.0d;
            double newMinus = (dmMinus.get(i) / trueRange.get(i)) * 100.0d;

            //add it to the list
            getDmPlusIndicator().add(newPlus);
            getDmMinusIndicator().add(newMinus);
        }

        //directional movement index
        List<Double> dmIndex = new ArrayList<>();

        //calculate each dm index
        for (int i = 0; i < dmPlus.size(); i++) {
            double result1 = Math.abs(getDmPlusIndicator().get(i) - getDmMinusIndicator().get(i));
            double result2 = getDmPlusIndicator().get(i) + getDmMinusIndicator().get(i);
            dmIndex.add((result1 / result2) * 100.0d);
        }

        double sum = 0;

        //get the average for the first value
        for (int i = 0; i < periodsADX; i++) {
            sum += dmIndex.get(i);
        }

        //our first value is the average
        getAdx().add(sum / (double)periodsADX);

        //calculate the remaining average directional index values
        for (int i = periodsADX; i < dmIndex.size(); i++) {

            //get the most recent adx
            double previousAdx = getRecent(getAdx());

            //calculate the new adx value
            double newAdx = ((previousAdx * (double)(periodsADX - 1)) + dmIndex.get(i)) / (double)periodsADX;

            //add new value to our list
            getAdx().add(newAdx);
        }

        //calculate the sma
        calculateSMA(history, getSmaPrice(), periodsSMA, Fields.Close);
    }

    /**
     * Smooth out the values
     * @param tmp Our temp list of values
     * @param result Our final result of smoothed values
     */
    private static void smooth(List<Double> tmp, List<Double> result, int periods) {

        double sum = 0;

        //add the sum of the first x periods to get the first value
        for (int i = 0; i < periods; i++) {
            sum += tmp.get(i);
        }

        //add first result to our list as a sum
        result.add(sum);

        //now lets smooth the values for the remaining
        for (int i = periods; i < tmp.size(); i++) {

            //calculate our current
            double currentSum = 0;

            //calculate the sum of the current period
            for (int x = i - periods + 1; x <= i; x++) {
                currentSum += tmp.get(x);
            }

            //get the previous value
            double previousValue = result.get(result.size() - 1);

            //calculate the new smoothed value
            double newValue = previousValue - (previousValue / (double)periods) + currentSum;

            //add the smoothed value to our list
            result.add(newValue);
        }
    }
}