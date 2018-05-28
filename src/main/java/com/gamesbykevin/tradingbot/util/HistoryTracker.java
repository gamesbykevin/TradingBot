package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.getProductsAllUsd;
import static com.gamesbykevin.tradingbot.calculator.Calculator.ENDPOINT_HISTORIC;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.sortHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.updateHistory;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameHistoryTracker;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

/**
 * This class is responsible to track all the candles for all the products on GDAX
 */
public class HistoryTracker implements Runnable {

    //individual thread to track the history
    private Thread thread;

    /**
     * How long do we wait between our json calls
     */
    private static final long DELAY = (120L * 1000L);

    //this script will check in the candle history to github
    private static final String SHELL_SCRIPT_FILE = "./auto_check.sh";

    //where we write our log file(s)
    private static PrintWriter WRITER;

    /**
     * Default constructor
     */
    public HistoryTracker() {

        //create new thread and start it
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {

        //get our list of candles
        Candle[] candles = Candle.values();

        //our history object
        List<Period> history = new ArrayList<>();

        while (true) {

            try {

                //were any files changed?
                boolean changed = false;

                //check every candle
                for (int i = 0; i < candles.length; i++) {

                    Candle candle = candles[i];

                    //check every product
                    for (int j = 0; j < getProductsAllUsd().size(); j++) {

                        try {

                            //get the product id
                            String productId = getProductsAllUsd().get(j).getId();

                            //clear our history list before we load
                            history.clear();

                            //load the contents from the text file into our array list
                            History.load(history, productId, candle, null);

                            //get the current size to check for changes
                            int size = history.size();

                            //display message
                            displayMessage("Checking history: " + productId + ", " + candle.description + ", Size: " + history.size());

                            //format our endpoint
                            String endpoint = String.format(ENDPOINT_HISTORIC, productId, candle.duration);

                            //display endpoint
                            displayMessage("Endpoint: " + endpoint);

                            //make historic candle call and get json response
                            String json = getJsonResponse(endpoint);

                            //convert json text to multi array
                            double[][] data = GSon.getGson().fromJson(json, double[][].class);

                            //make sure we have data before we update and sort
                            if (data != null && data.length > 0) {
                                updateHistory(history, data);
                                sortHistory(history);
                            }

                            //if the history changed, write it to local storage
                            if (history.size() != size) {
                                displayMessage("Writing history: " + productId + ", " + candle.description + ", Size: " + history.size(), getWriter());
                                boolean result = History.write(history, productId, candle);

                                //if writing the file was successful flag it was changed
                                if (result)
                                    changed = true;
                            }

                        } catch (Exception e) {

                            //display message and write to log
                            displayMessage(e, getWriter());

                        } finally {

                            //sleep the thread for a short time
                            Thread.sleep(DELAY);
                        }
                    }
                }

                if (changed) {

                    //display that we are calling the bash script
                    displayMessage("Running bash script: " + SHELL_SCRIPT_FILE, getWriter());

                    //run shell script to commit file changes into github
                    Runtime.getRuntime().exec(SHELL_SCRIPT_FILE);

                    //display that we called the bash script
                    displayMessage("Bash script called", getWriter());
                }

            } catch (Exception ex) {

                //display error message and write to log
                displayMessage(ex, getWriter());
            }
        }
    }

    protected static final PrintWriter getWriter() {

        //create the main log file and place in our root logs directory
        if (WRITER == null)
            WRITER = LogFile.getPrintWriter(getFilenameHistoryTracker(), LogFile.getLogDirectory());

        return WRITER;
    }
}