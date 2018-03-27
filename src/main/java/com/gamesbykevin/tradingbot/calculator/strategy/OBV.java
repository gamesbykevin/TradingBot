package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

public class OBV extends Strategy {

    /**
     * How many periods to calculate the on balance volume
     */
    private static final int PERIODS_OBV = 10;

    //keep a historical list of the volume so we can check for divergence
    private List<Double> volume;

    //our desired periods
    private final int periodsObv;

    public OBV(int periods) {

        //call parent
        super(periods);

        //assign value
        this.periodsObv = periods;

        //create list
        this.volume = new ArrayList<>();
    }

    public OBV() {
        this(PERIODS_OBV);
    }

    public List<Double> getVolume() {
        return this.volume;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if there is a bullish divergence let's buy
        if (hasDivergence(history, getPeriods(), true, getVolume()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if there is a bearish divergence let's sell
        if (hasDivergence(history, getPeriods(), false, getVolume()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "OBV: ", getVolume(), (periodsObv / 2), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our historical list
        getVolume().clear();

        //calculate the obv for each period
        for (int i = 0; i < history.size(); i++) {

            //skip if not enough info
            if (i < getPeriods())
                continue;

            //get the obv for this period
            final double tmpVolume = calculateOBV(history, i);

            //add the obv calculation to the list
            getVolume().add(tmpVolume);
        }
    }

    private double calculateOBV(List<Period> history, int currentPeriod) {

        //the total sum
        double sum = 0;

        //check every period
        for (int i = currentPeriod - getPeriods(); i < currentPeriod - 1; i++) {

            Period prev = history.get(i);
            Period next = history.get(i + 1);

            if (next.close > prev.close) {

                //add to the total volume
                sum = sum + history.get(i).volume;

            } else if (next.close < prev.close) {

                //subtract from the total volume
                sum = sum - history.get(i).volume;
            }
        }

        //return the on balance volume
        return sum;
    }
}