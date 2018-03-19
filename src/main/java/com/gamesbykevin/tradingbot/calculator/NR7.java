package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;

public class NR7 extends Indicator {

    /**
     * Narrow Range 7 will always be 7 periods
     */
    public static final int PERIODS = 7;

    //the period with the smallest range
    private Period smallest;

    public NR7() {

        //call parent
        super(PERIODS);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current $ breaks above high, we will buy
        if (currentPrice > smallest.high)
            agent.setReasonBuy(ReasonBuy.Reason_8);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //make sure the current price is above the purchase price
        if (currentPrice > agent.getWallet().getPurchasePrice()) {

            //if the next period closes above our purchase price, sell!!!!
            if (history.get(history.size() - 1).close > agent.getWallet().getPurchasePrice())
                agent.setReasonSell(ReasonSell.Reason_11);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        displayMessage(agent, "NR7: High $" + smallest.high + ", Low $" + smallest.low, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //which period has the smallest range
        smallest = null;

        //check the previous # of periods to look for the smallest range
        for (int i = history.size() - getPeriods(); i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //we are always looking for a period with the smallest range
            if (smallest == null || current.high - current.low < smallest.high - smallest.low)
                smallest = current;
        }
    }
}