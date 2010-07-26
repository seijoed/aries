package org.apache.aries.transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleWideTxDataUtil {
    
    
    /**
     * based on method name and its associated compId, to check which transaction attribute should apply to
     * this method from the bundle wide transaction configuration.
     * 
     * if no transaction attribute should apply to this method, null should be returned
     * @param compId
     * @param method
     * @param bundleData
     * @return
     */
    public static String getAttribute(String compId, String method, List<BundleWideTxData> bundleData) {
        BundleWideTxData txData = null;
        if (bundleData.size() == 1)  {
            txData = bundleData.get(0);
            
            List<Pattern> beans = txData.getBean();
            List<Pattern> mtds = txData.getMethod();
            String value = txData.getValue();
            
            if (!beans.isEmpty() && !mtds.isEmpty()) {
                for (Pattern bean : beans) {
                    if (bean.matcher(compId).matches()) {
                        for (Pattern mtd : mtds) {
                            if (mtd.matcher(method).matches()) {
                                return value;
                            }
                        }
                    }
                }

            } else if (!beans.isEmpty()) {
                for (Pattern bean : beans) {
                    if (bean.matcher(compId).matches()) {
                        return value;
                    }
                }
                
            } else if (!mtds.isEmpty()) {
                for (Pattern mtd : mtds) {
                    if (mtd.matcher(method).matches()) {
                        return value;
                    }
                }
            } else {
                return value;
            }
        } else {
            // multiple bundledata available, let's try find the one that matches the compId
            return getTransactionAttribute(bundleData, compId, method);           
        }


        return null;
    }
    
    private static String getTransactionAttribute(List<BundleWideTxData> bundleData, String compId, String method) {
        List<BundleWideTxData> bundleDataBothMethodAndBean = new ArrayList<BundleWideTxData>();
        List<BundleWideTxData> bundleDataOnlyBean = new ArrayList<BundleWideTxData>();
        List<BundleWideTxData> bundleDataOnlyMethod = new ArrayList<BundleWideTxData>();
        List<BundleWideTxData> bundleDataNoRestriction = new ArrayList<BundleWideTxData>();
        
        for (BundleWideTxData txData : bundleData) {
            List<Pattern> beans = txData.getBean();
            List<Pattern> mtds = txData.getMethod();
            
            if (!beans.isEmpty() && !mtds.isEmpty()) {
                bundleDataBothMethodAndBean.add(txData);
            } else if (!beans.isEmpty() ) {
                bundleDataOnlyBean.add(txData);
            } else if (!mtds.isEmpty()) {
                bundleDataOnlyMethod.add(txData);
            } else {
                bundleDataNoRestriction.add(txData);
            }
        }
        
        // let's first check bundle data which has both 
        List<MatchedTxData> matchedTxData = new ArrayList<MatchedTxData>();
        for (BundleWideTxData txData : bundleDataBothMethodAndBean) {
            List<Pattern> beans = txData.getBean();
            List<Pattern> mtds = txData.getMethod();
            String value = txData.getValue();
            
            for (Pattern bean : beans) {
                if (bean.matcher(compId).matches()) {
                    for (Pattern mtd : mtds) {
                        if (mtd.matcher(method).matches()) {
                            //return value;
                            matchedTxData.add(new MatchedTxData(value, mtd, bean));
                        }
                    }
                }
            }
        }
        
        if (!matchedTxData.isEmpty()) {
            return findBestMatch(matchedTxData);
        }
        
        // let's then check bundle data that has bean only next
        for (BundleWideTxData txData : bundleDataOnlyBean) {
            List<Pattern> beans = txData.getBean();
            String value = txData.getValue();
            
            for (Pattern bean : beans) {
                if (bean.matcher(compId).matches()) {
                    matchedTxData.add(new MatchedTxData(value, null, bean));
                }
            }
        }
        
        if (!matchedTxData.isEmpty()) {
            return findBestMatchBeanOnly(matchedTxData);
        }
        
        // let's then check bundle data that has method only next
        for (BundleWideTxData txData : bundleDataOnlyMethod) {
            List<Pattern> mtds = txData.getMethod();
            String value = txData.getValue();
            
            for (Pattern mtd : mtds) {
                if (mtd.matcher(method).matches()) {
                    matchedTxData.add(new MatchedTxData(value, mtd, null));
                }
            }
        }
        
        if (!matchedTxData.isEmpty()) {
            return findBestMatchMethodOnly(matchedTxData);
        }
        
        
        // let's then check bundle data that doesn't have method or bean
        for (BundleWideTxData txData : bundleDataNoRestriction) {
            return txData.getValue();    
        }
        
        if (bundleDataNoRestriction.size() == 0) {
            return null;
        } else if (bundleDataNoRestriction.size() == 1) {
            return bundleDataNoRestriction.get(0).getValue();
        } else {
            // cannot have more than 1 transaction element that has no method or bean attribute
            throw new IllegalStateException("More than 1 bundle wide transaction elements that have no method or bean attribute specified " + bundleDataNoRestriction);
        }
        
    }
    
    // this method assume matchedTxData isn't empty.
    private static String findBestMatch(List<MatchedTxData> matchedTxData) {
        
        if (matchedTxData.size() == 1) {
            return matchedTxData.get(0).getValue();
        } else {
            // let's compare bean first
            List<MatchedTxData> matchesBean1 = selectPatternsWithFewestWildcards(matchedTxData, true);
            if (matchesBean1.size() == 1) {
                return matchesBean1.get(0).getValue();
            } else {
                List<MatchedTxData> matchesBean2 = selectLongestPatterns(matchesBean1, true);
                if (matchesBean2.size() == 1) {
                    return matchesBean2.get(0).getValue();
                } else {
                    // let's compare method
                    List<MatchedTxData> matchesMethod1 = selectPatternsWithFewestWildcards(matchesBean2, false);
                    if (matchesMethod1.size() == 1) {
                        return matchesMethod1.get(0).getValue();
                    } else {
                        List<MatchedTxData> matchesMethod2 = selectLongestPatterns(matchesMethod1, false);
                        if (matchesMethod2.size() == 1) {
                            return matchesMethod2.get(0).getValue();
                        } else {
                            // unable to find the best match!!
                            throw new IllegalStateException("Unable to apply patterns: " + matchedTxData);
                        }
                    }
                }
            }
        }

    }
    
    // this method assume matchedTxData isn't empty.
    private static String findBestMatchBeanOnly(List<MatchedTxData> matchedTxData) {
        
        if (matchedTxData.size() == 1) {
            return matchedTxData.get(0).getValue();
        } else {
            // let's compare bean first
            List<MatchedTxData> matchesBean1 = selectPatternsWithFewestWildcards(matchedTxData, true);
            if (matchesBean1.size() == 1) {
                return matchesBean1.get(0).getValue();
            } else {
                List<MatchedTxData> matchesBean2 = selectLongestPatterns(matchesBean1, true);
                if (matchesBean2.size() == 1) {
                    return matchesBean2.get(0).getValue();
                } else {
                    // unable to find the best match!!
                    throw new IllegalStateException("Unable to apply patterns: " + matchedTxData);                  
                }
            }
        }

    }
    
    // this method assume matchedTxData isn't empty.
    private static String findBestMatchMethodOnly(List<MatchedTxData> matchedTxData) {
        
        if (matchedTxData.size() == 1) {
            return matchedTxData.get(0).getValue();
        } else {
            // let's compare bean first
            List<MatchedTxData> matchesMethod1 = selectPatternsWithFewestWildcards(matchedTxData, false);
            if (matchesMethod1.size() == 1) {
                return matchesMethod1.get(0).getValue();
            } else {
                List<MatchedTxData> matchesMethod2 = selectLongestPatterns(matchesMethod1, false);
                if (matchesMethod2.size() == 1) {
                    return matchesMethod2.get(0).getValue();
                } else {
                    // unable to find the best match!!
                    throw new IllegalStateException("Unable to apply patterns: " + matchedTxData);                  
                }
            }
        }

    }
    
    private static List<MatchedTxData> selectPatternsWithFewestWildcards(List<MatchedTxData> matchedTxData, boolean isBean) {
        List<MatchedTxData> remainingMatches = new ArrayList<MatchedTxData>();
        int minWildcards = Integer.MAX_VALUE;
        
        for (MatchedTxData mData : matchedTxData) {
            Pattern p;
            if (isBean) {
                p = mData.getBean();
            } else {
                p = mData.getMethod();
            }
            String pattern = p.pattern();
            
            Matcher m = Constants.WILDCARD.matcher(pattern);
            int count = 0;
            
            while (m.find()) {
                count++;
            }
            
            if (count < minWildcards) {
                remainingMatches.clear();
                remainingMatches.add(mData);
                minWildcards = count;
            }
            else if (count == minWildcards) {
                remainingMatches.add(mData);
            }
        }
        
        
        return remainingMatches;
    }
    
    private static List<MatchedTxData> selectLongestPatterns(List<MatchedTxData> matchedTxData, boolean isBean) {
        List<MatchedTxData> remainingMatches = new ArrayList<MatchedTxData>();
        int longestLength = 0;
        
        for (MatchedTxData mData : matchedTxData) {
            Pattern p;
            if (isBean) {
                p = mData.getBean();
            } else {
                p = mData.getMethod();
            }
            String pattern = p.pattern();
            
            int length = pattern.length();
            
            if (length > longestLength) {
                remainingMatches.clear();
                remainingMatches.add(mData);
                longestLength = length;
            }
            else if (length == longestLength) {
                remainingMatches.add(mData);
            }
        }
        
        return remainingMatches;
    }
}
