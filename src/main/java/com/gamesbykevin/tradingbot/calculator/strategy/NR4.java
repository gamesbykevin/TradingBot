package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.NR;

import java.util.List;

public class NR4 extends Strategy {

    //how we access our indicator(s)
    private static int INDEX_NR;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS = 4;

    //if the price goes below this we will sell
    private double sellBreak = 0;

    //track the time of the current candle
    private long candleTime;

    //is the stock oversold
    private static final float OVERSOLD = 30.0f;

    public NR4() {

        //call parent
        super(Key.NR4);

        //add indicator(s)
        INDEX_NR = addIndicator(new NR(PERIODS));
        INDEX_RSI = addIndicator(new RSI(PERIODS));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //obtain our indicator
        NR nr = (NR)getIndicator(INDEX_NR);
        RSI rsi = (RSI)getIndicator(INDEX_RSI);

        //we want the rsi level to be oversold
        if (getRecent(rsi.getValueRSI()) <= OVERSOLD) {

            //when the price breaks out above the high, we will buy
            if (currentPrice > nr.getNarrowRangeCandle().high) {
                candleTime = history.get(history.size() - 1).time;
                sellBreak = nr.getNarrowRangeCandle().low;
                return true;
            }
        }

        //no signal yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we hit our sell price we will sell
        if (currentPrice <= sellBreak)
            return true;

        //if the candle does not match the period has ended and we sell
        if (candleTime != history.get(history.size() - 1).time)
            return true;

        //no signal yet
        return false;
    }
}