package com.moneydance.modules.features.invextension;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.infinitekind.moneydance.model.CurrencyType;
import com.moneydance.apps.md.controller.AccountBookWrapper;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.apps.md.controller.io.AccountBookUtil;

import javax.swing.*;
import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Singgleton Class holds all Moneydance Data
 */
public class MDData {
    File mdFolder;
    private FeatureModuleContext featureModuleContext;
    private AccountBook accountBook;
    private Account root;
    private BulkSecInfo currentInfo;
    private ObservableLastTransactionDate observableLastTransactionDate;
    private Calendar lastYahooqtUpdateDate;
    private Date lastPriceUpdateTime;
    private HashMap<String, Double> userRateMap = new HashMap<>();
    private Main extension;
    private Thread transactionMonitorThread;
    private TransactionMonitor transactionMonitor;

    private static MDData uniqueInstance;
    public static final DateFormat DATE_PATTERN_LONG =  DateFormat.getDateTimeInstance
            (DateFormat.LONG, DateFormat.LONG);
    public static final DateFormat DATE_PATTERN_SHORT =  DateFormat.getDateInstance
            (DateFormat.SHORT, Locale.getDefault());
    public static final DateFormat DATE_PATTERN_MEDIUM =  new SimpleDateFormat("HH:mm, dd-MMM-yyyy");


    /**
     * private constructor prevents Instantiation
     */
    private MDData(){}

    public static MDData getInstance(){
        if (uniqueInstance == null){
            uniqueInstance = new MDData();
        }
        return uniqueInstance;
    }




    public AccountBook getAccountBook() {
        return accountBook;
    }

    public ObservableLastTransactionDate getObservableLastTransactionDate(){
        return observableLastTransactionDate;
    }

    public Account getRoot() {
        return root;
    }

    public BulkSecInfo getCurrentInfo() {
        return currentInfo;
    }

    public Main getExtension() {
        return extension;
    }

    public void generateCurrentInfo(ReportConfig reportConfig) throws Exception {
        if(accountBook == null) throw new Exception("Call to generateCurrentInfo, but account book is null");
        this.currentInfo = new BulkSecInfo(accountBook, reportConfig);
    }

    public void startTransactionMonitorThread(ReportConfig reportConfig, long updateFrequencyMins){
        transactionMonitor = new TransactionMonitor(reportConfig, updateFrequencyMins);
        transactionMonitorThread = new Thread(transactionMonitor);
        transactionMonitorThread.start();
    }

    public void stopTransactionMonitorThread() throws InterruptedException {
        if(transactionMonitorThread!= null){
            transactionMonitor.doShutdown();
        }

    }



    public void setRoot(Account root) {
        this.root = root;
    }

    public void setExtension(Main extension){
        this.extension = extension;
    }

    @SuppressWarnings("unused")
    public void printAllPriceDates(){
        java.util.List<CurrencyType> currencies = accountBook
                .getCurrencies().getAllCurrencies();
        for (CurrencyType currency: currencies){
            String priceDate = currency.getParameter("price_date");
            if(priceDate != null){
                System.out.print(currency.getName() + " : "
                        + DATE_PATTERN_MEDIUM.format(new Date(Long.parseLong(priceDate))) + "\n");
            }
        }
    }

    public void generateCurrentPriceData() {
        java.util.List<CurrencyType> currencies = accountBook
                .getCurrencies().getAllCurrencies();
        long maxDateTime = 0L;
        for (CurrencyType currency : currencies) {
            String priceDateStr = currency.getParameter("price_date");
            String currencyID = currency.getParameter("id");
            double decimalPlaces = Math.pow(10.0,(double) currency.getDecimalPlaces());
            Double userRate = (double) Math.round(currency.getUserRate() * decimalPlaces) / decimalPlaces;
            if (priceDateStr != null) {
                Long priceDateLong = Long.parseLong(priceDateStr);
                maxDateTime = Math.max(maxDateTime, priceDateLong);
            }
            if(currencyID != null){
                userRateMap.put(currencyID, userRate);
            }
        }
        lastPriceUpdateTime =  new Date(maxDateTime);
    }

    public void generateLastYahooUpdateDate(){
        Integer lastUpdateDate = root.getIntParameter("yahooqt.quoteLastUpdate",0);
        if (lastUpdateDate == 0){
            lastYahooqtUpdateDate = null;
        } else {
            Calendar gc = Calendar.getInstance();
            gc.setTime(DateUtils.convertToDate(lastUpdateDate));
            lastYahooqtUpdateDate = gc;
        }
    }

    public Date getLastTransactionModified(){
        if(accountBook != null){
            File syncOutFolder = new File(accountBook.getRootFolder(), "safe/tiksync/out");
            FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith(".txn");
            File[] files = syncOutFolder.listFiles(filter);
            long tempModifedTxn = 0L;
            for(File file: files){
                tempModifedTxn = Math.max(tempModifedTxn, file.lastModified());
            }
            return new Date(tempModifedTxn);
        } else {
            return null;
        }
    }

    public Date getLastPriceUpdateTime() {
        return lastPriceUpdateTime;
    }

    public HashMap<String, Double> getUserRateMap() {
        return userRateMap;
    }

    java.util.List<String> getTransactionStatus(){
        List<String> msgs = new ArrayList<>();
        msgs.add("MD last modified: " + DATE_PATTERN_MEDIUM.format
                (observableLastTransactionDate.getLastTransactionDate()));
        if(lastYahooqtUpdateDate != null){
            msgs.add("YahooQuote updated on: " + DATE_PATTERN_SHORT.format(lastYahooqtUpdateDate.getTime()));
            generateCurrentPriceData();
            msgs.add("latest security price: " +
                    DATE_PATTERN_MEDIUM.format(lastPriceUpdateTime));
        } else {
            msgs.add("YahooQuote last Update: " + "NOT USED");
        }
        return msgs;
    }



    @SuppressWarnings("unused")
    private void setLastYahooQtDateUpdated(int selectedDateInt) {
        accountBook.getRootAccount().setParameter("yahooqt.quoteLastUpdate",selectedDateInt);
        boolean synced = accountBook.getRootAccount().syncItem();
        System.out.print("Synced: " + synced);
    }

    public void initializeMDDataInApplication(boolean newObservable) {
        featureModuleContext = extension.getUnprotectedContext();
        accountBook = featureModuleContext.getCurrentAccountBook();
        root = accountBook.getRootAccount();
        if (newObservable) observableLastTransactionDate =
                new ObservableLastTransactionDate(getLastTransactionModified());
        generateLastYahooUpdateDate();
        if(lastYahooqtUpdateDate != null) generateCurrentPriceData();

    }

    protected void initializeMDDataHeadless(boolean newObservable) throws Exception{
        AccountBookWrapper wrapper = AccountBookWrapper.wrapperForFolder(mdFolder);
        // must add this section or get null pointer error
        ArrayList<File> folderFiles = new ArrayList<>();
        folderFiles.add(mdFolder);
        AccountBookUtil.INTERNAL_FOLDER_CONTAINERS = folderFiles;
        wrapper.doInitialLoad(null);
        featureModuleContext = null;
        accountBook = wrapper.getBook();
        root = accountBook.getRootAccount();
        if(newObservable) observableLastTransactionDate =
                new ObservableLastTransactionDate(getLastTransactionModified());
        generateLastYahooUpdateDate();
        if(lastYahooqtUpdateDate != null) generateCurrentPriceData();
    }

    public void loadMDFile(File mdFolder, ReportControlPanel reportControlPanel) {
        new MDFileLoader(mdFolder, reportControlPanel).execute();
    }

    public void reloadMDData(ReportConfig reportConfig) throws Exception {
        if(featureModuleContext != null){
            initializeMDDataInApplication(false);
            currentInfo = new BulkSecInfo(accountBook, reportConfig);
        } else {
            initializeMDDataHeadless(false);
            currentInfo = new BulkSecInfo(accountBook, reportConfig);
        }
    }

    public boolean hasNewUserRate(HashMap<String, Double> lastUserRateMap) {
        for (String id: userRateMap.keySet()){
            Double lastUserRate = lastUserRateMap.get(id);
            Double currentUserRate = userRateMap.get(id);
            if(lastUserRate != null && currentUserRate != null){
                if(!Objects.equals(currentUserRate, lastUserRate)) return true;
            }
        }
        return false;
    }

    public static class ObservableLastTransactionDate extends Observable{
        Date lastTransactionDate;
        TreeSet<Date> previousLastTransactionDates = new TreeSet<>();
        
        ObservableLastTransactionDate(Date lastTransactionDate){
            this.lastTransactionDate = lastTransactionDate;
        }

        public void setChanged(Date newLastTransactionDate){
            previousLastTransactionDates.add(lastTransactionDate);
            lastTransactionDate = newLastTransactionDate;
            setChanged();
            notifyObservers();
        }

        public boolean isNewTransactionDate(Date transactionDate){
            return transactionDate.after(lastTransactionDate);
        }

        public Date getLastTransactionDate() {
            return lastTransactionDate;
        }

        public Date getPreviousLastTransactionDate() {
            if(!previousLastTransactionDates.isEmpty()){
                return previousLastTransactionDates.last();
            } else {
                return null;
            }

        }
    }
    

    private class TransactionMonitor implements Runnable {
        private long transactionWaitTimeMills = 60000;
        private TreeSet<Date> newTransactionDateQueue = new TreeSet<>();
        private  Date lastRefreshTime;
        private long updateFrequencyMins;
        private ReportConfig reportConfig;
        private volatile boolean shutdown = false;

        TransactionMonitor(ReportConfig reportConfig, long updateFrequencyMins){
            this.reportConfig = reportConfig;
            this.updateFrequencyMins = updateFrequencyMins;
        }

        @Override
        public void run() {
            while (!shutdown) {
                Date latestTransactionDate = getLastTransactionModified();
                boolean newTransaction = observableLastTransactionDate.isNewTransactionDate(latestTransactionDate);
                try {
                    // refresh Yahoo quote extension
                    if(lastRefreshTime == null){ //refresh immediately
                        lastRefreshTime = new Date();
                        refreshPrices();
                    } else {
                        if(isTimeToRefresh()){// refresh after interval
                            lastRefreshTime = new Date();
                            refreshPrices();
                        }
                    }
                    //check for new transactions
                    if (newTransaction) {
                        boolean notSeenBefore = newTransactionDateQueue.add(latestTransactionDate);
                        if (notSeenBefore) { // wait for another cycle
                            Thread.sleep(transactionWaitTimeMills);
                        } else { // seen before
                            if (new Date().getTime() - newTransactionDateQueue.last().getTime() >= transactionWaitTimeMills) {
                                observableLastTransactionDate.setChanged(latestTransactionDate);
                                newTransactionDateQueue.clear();
                            }
                        }
                    } else {
                        Thread.sleep(transactionWaitTimeMills);
                    }
                } catch (Exception e) {
                    LogController.logException(e, "Error in Transaction Monitor");
                }
            }
        }

        public void doShutdown(){
            shutdown = true;
        }

        public boolean isTimeToRefresh(){
            long diff = new Date().getTime() - lastRefreshTime.getTime();
            return diff > updateFrequencyMins * 60000;
        }

        public void refreshPrices() throws Exception {
            if (extension != null) {
                FeatureModuleContext featureModuleContext = extension.getUnprotectedContext();
                featureModuleContext.showURL("moneydance:fmodule:yahooqt:update");
                currentInfo = new BulkSecInfo(accountBook, reportConfig); //TODO: Test if needed
            }
        }

    }

    private class MDFileLoader extends SwingWorker<Void, String> {

        ReportControlPanel reportControlPanel;

        MDFileLoader(File mdFolder, ReportControlPanel reportControlPanel) {
            MDData.this.mdFolder = mdFolder;
            this.reportControlPanel = reportControlPanel;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                publish("Loading " + mdFolder.getName());
                initializeMDDataHeadless(true);
                for (String msg:getTransactionStatus()){
                    publish(msg);
                }
                reportControlPanel.setAccountAndFolderSubPanels();
                publish(mdFolder.getName() + " Loaded! Choose Report to run.");

            } catch (Exception e) {
                publish("Error! " + mdFolder.getName() + " not loaded!");
                e.printStackTrace();
                LogController.logException(e, "Error on Loading MD File: ");
            }
            return null;
        }

        @Override
        protected void process(java.util.List<String> msgs) {
            reportControlPanel.updateStatus(msgs);
        }

    }


}
