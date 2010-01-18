/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.samples.daytrader.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.geronimo.samples.daytrader.persistence.api.MarketSummaryDataBean;
import org.apache.geronimo.samples.daytrader.util.Log;
import org.apache.geronimo.samples.daytrader.util.TradeConfig;
import org.apache.geronimo.samples.daytrader.api.TradeServicesManager;
import org.apache.geronimo.samples.daytrader.api.TradeServices;


/**
 * TradeJDBCDirect uses direct JDBC access to a
 * <code>javax.sql.DataSource</code> to implement the business methods of the
 * Trade online broker application. These business methods represent the
 * features and operations that can be performed by customers of the brokerage
 * such as login, logout, get a stock quote, buy or sell a stock, etc. and are
 * specified in the {@link org.apache.geronimo.samples.daytrader.TradeServices}
 * interface
 * 
 * Note: In order for this class to be thread-safe, a new TradeJDBC must be
 * created for each call to a method from the TradeInterface interface.
 * Otherwise, pooled connections may not be released.
 * 
 * @see org.apache.geronimo.samples.daytrader.TradeServices
 * 
 */

public class TradeServicesManagerImpl implements TradeServicesManager {

    private static TradeServices[] tradeServicesList = new TradeServices[TradeConfig.runTimeModeNames.length] ;
    private List tradeList = null;
    private List tradeDBList = null;

    // This lock is used to serialize market summary operations.
    private static final Integer marketSummaryLock = new Integer(0);
    private static long nextMarketSummary = System.currentTimeMillis();
//    private static MarketSummaryDataBean cachedMSDB = MarketSummaryDataBean.getRandomInstance();
    private static MarketSummaryDataBean cachedMSDB = null; 
    
    /**
      * TradeServicesManagerImpl null constructor
      */
    public TradeServicesManagerImpl() {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl()");
    }

    /**
      * init
      */
    public void init() {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:init()");
    }


    /**
      * Get CurrentModes that are registered
      */
    public ArrayList<Integer> getCurrentModes() {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:getCurrentModes()");
        ArrayList<Integer> modes = new ArrayList<Integer>();
        for (int i=0; i<tradeServicesList.length; i++) {
            TradeServices tradeServicesRef = tradeServicesList[i];
            if (tradeServicesRef != null) {
                modes.add(tradeServicesRef.getMode());
            }
        }
        return modes;
    }

    /**
      * Set TradeServicesList reference
      */
    public void setTradeServicesList(List tradeList) {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:setTradeServicesList()" , tradeList);
        this.tradeList = tradeList;
        Iterator it = tradeList.iterator();
        while (it.hasNext()) {  
            TradeServices tradeServices =  (TradeServices) it.next(); 
            this.tradeServicesList[tradeServices.getMode()] = tradeServices;
        }  
    }

    /**
      * Get TradeServices reference
      */
    public TradeServices getTradeServices() {
        if (Log.doTrace()) 
            Log.trace("TradeServicesManagerImpl:getTradeServices()");
        return tradeServicesList[TradeConfig.runTimeMode];
    }

    /**
      * Bind a new TradeServices implementation
      */
    public void bindService(TradeServices tradeServices, Map props) {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:bindService()", tradeServices, props);
        if (tradeServices != null) {
            tradeServicesList[tradeServices.getMode()] = tradeServices;
        }
    }

    /**
      * Unbind a TradeServices implementation
      */
    public void unbindService(TradeServices tradeServices, Map props) {
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:unbindService()", tradeServices, props);
        if (tradeServices != null) {
            tradeServicesList[tradeServices.getMode()] = null;
        }
    }

    /**
     * Market Summary is inherently a heavy database operation.  For servers that have a caching
     * story this is a great place to cache data that is good for a period of time.  In order to
     * provide a flexible framework for this we allow the market summary operation to be
     * invoked on every transaction, time delayed or never.  This is configurable in the 
     * configuration panel.  
     *
     * @return An instance of the market summary
     */
    public MarketSummaryDataBean getMarketSummary() throws Exception {
    
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getMarketSummary()");
        }
    
        if (Log.doTrace())
            Log.trace("TradeServicesManagerImpl:getMarketSummary()");

        if (TradeConfig.getMarketSummaryInterval() == 0) return getMarketSummaryInternal();
        if (TradeConfig.getMarketSummaryInterval() < 0) return cachedMSDB;
    
        /**
         * This is a little funky.  If its time to fetch a new Market summary then we'll synchronize
         * access to make sure only one requester does it.  Others will merely return the old copy until
         * the new MarketSummary has been executed.
         */
         long currentTime = System.currentTimeMillis();
         
         if (currentTime > nextMarketSummary) {
             long oldNextMarketSummary = nextMarketSummary;
             boolean fetch = false;

             synchronized (marketSummaryLock) {
                 /**
                  * Is it still ahead or did we miss lose the race?  If we lost then let's get out
                  * of here as the work has already been done.
                  */
                 if (oldNextMarketSummary == nextMarketSummary) {
                     fetch = true;
                     nextMarketSummary += TradeConfig.getMarketSummaryInterval()*1000;
                     
                     /** 
                      * If the server has been idle for a while then its possible that nextMarketSummary
                      * could be way off.  Rather than try and play catch up we'll simply get in sync with the 
                      * current time + the interval.
                      */ 
                     if (nextMarketSummary < currentTime) {
                         nextMarketSummary = currentTime + TradeConfig.getMarketSummaryInterval()*1000;
                     }
                 }
             }

            /**
             * If we're the lucky one then let's update the MarketSummary
             */
            if (fetch) {
                cachedMSDB = getMarketSummaryInternal();
            }
        }
         
        return cachedMSDB;
    }

    /**
     * Compute and return a snapshot of the current market conditions This
     * includes the TSIA - an index of the price of the top 100 Trade stock
     * quotes The openTSIA ( the index at the open) The volume of shares traded,
     * Top Stocks gain and loss
     *
     * @return A snapshot of the current market summary
     */
    private MarketSummaryDataBean getMarketSummaryInternal() throws Exception {
        if (Log.doActionTrace()) {
            Log.trace("TradeAction:getMarketSummaryInternal()");
        }
        MarketSummaryDataBean marketSummaryData = null;
        marketSummaryData = tradeServicesList[TradeConfig.runTimeMode].getMarketSummary();
        return marketSummaryData;
    }


}