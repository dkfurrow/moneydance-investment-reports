package com.moneydance.modules.features.invextension;

import java.util.TreeMap;

public class TestReturn {

    /**
     * @param args
     */
    
    
    
    
    
    public static void main(String[] args) {
	TreeMap<Integer, Double> baseMap = new TreeMap<Integer, Double>();
	baseMap.put(20100202, -3797.0);
	baseMap.put(20100315, -1720.0);
	baseMap.put(20100329, 97.21);
	baseMap.put(20100408, -3.76);
	baseMap.put(20100415, 927.0);
	baseMap.put(20100428, 329.28);
	baseMap.put(20100514, 2457.06);
	
	double mdRet = -.22775;
	
	double annRet = RepFromTo.getAnnualReturn(baseMap, mdRet);
	
	System.out.println("Annual Return: " + annRet);
		
	
	
    }

}
