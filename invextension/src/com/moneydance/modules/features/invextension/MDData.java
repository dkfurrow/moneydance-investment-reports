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
import java.util.*;

/**
 * Singgleton Class holds all Moneydance Data
 */
public class MDData {
    private FeatureModuleContext featureModuleContext;
    private AccountBook accountBook;
    private Account root;
    private BulkSecInfo currentInfo;
    private ObservableLastTransactionDate lastTransactionDate;
    private Calendar lastYahooqtUpdate;
    private TreeMap<CurrencyData, Date> currencyUpdateMap = new TreeMap<>();
    private Main extension;
    private Thread transactionMonitorThread;
    private static MDData uniqueInstance;
    public static final DateFormat DATE_PATTERN_LONG =  DateFormat.getDateTimeInstance
            (DateFormat.LONG, DateFormat.LONG);
    public static final DateFormat DATE_PATTERN_SHORT =  DateFormat.getDateInstance
            (DateFormat.SHORT, Locale.getDefault());


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

    public void refreshPrices(ReportConfig reportConfig) throws Exception {
        FeatureModuleContext featureModuleContext = extension.getUnprotectedContext();
        featureModuleContext.showURL("moneydance:fmodule:yahooqt:update");
        currentInfo = new BulkSecInfo(accountBook, reportConfig);

    }


    public AccountBook getAccountBook() {
        return accountBook;
    }

    public ObservableLastTransactionDate getLastTransactionDate(){
        return lastTransactionDate;
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

    public void setCurrentInfo(ReportConfig reportConfig) throws Exception {
        if(accountBook == null) throw new Exception("Call to setCurrentInfo, but account book is null");
        this.currentInfo = new BulkSecInfo(accountBook, reportConfig);
    }

    public void startTransactionMonitorThread(){
        transactionMonitorThread = new Thread(new TransactionMonitor());
        transactionMonitorThread.start();
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
                        + DATE_PATTERN_LONG.format(new Date(Long.parseLong(priceDate))) + "\n");
            }
        }
    }

    public void getCurrencyUpdateMap() {
        java.util.List<CurrencyType> currencies = accountBook
                .getCurrencies().getAllCurrencies();
        for (CurrencyType currency : currencies) {
            String priceDateStr = currency.getParameter("price_date");
            if (priceDateStr != null) {
                Date priceDate = new Date(Long.parseLong(priceDateStr));
                if (updatedOnLastYahooQt(priceDate)) {
                    currencyUpdateMap.put(new CurrencyData(currency), priceDate);
                }
            }
        }

    }

    public Boolean updatedOnLastYahooQt(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return lastYahooqtUpdate.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                lastYahooqtUpdate.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR);
    }

    public void getLastYahooUpdateDate(){
        Integer lastUpdateDate = root.getIntParameter("yahooqt.quoteLastUpdate",0);
        if (lastUpdateDate == 0){
            lastYahooqtUpdate = null;
        } else {
            Calendar gc = Calendar.getInstance();
            gc.setTime(DateUtils.convertToDate(lastUpdateDate));
            lastYahooqtUpdate = gc;
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

    java.util.List<String> getTransactionStatus(){
        List<String> msgs = new ArrayList<>();
        msgs.add("Last Modified Txn: " + DATE_PATTERN_LONG.format
                (lastTransactionDate.getLastTransactionDate()));
        if(lastYahooqtUpdate != null){
            msgs.add("YahooQuote last Update: " + DATE_PATTERN_SHORT.format(lastYahooqtUpdate.getTime()));
            getCurrencyUpdateMap();
            Date maxDate = Collections.max(currencyUpdateMap.values());
            msgs.add("YahooQuote latest Security Price: " +
                    DATE_PATTERN_LONG.format(maxDate));
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

    public void SetRunInApplication() {
        featureModuleContext = extension.getUnprotectedContext();
        accountBook = featureModuleContext.getCurrentAccountBook();
        root = accountBook.getRootAccount();
        lastTransactionDate = new ObservableLastTransactionDate(getLastTransactionModified());
        getLastYahooUpdateDate();
        if(lastYahooqtUpdate != null) getCurrencyUpdateMap();
    }

    public void loadMDFile(File mdFolder, ReportControlPanel reportControlPanel) {
        new MDFileLoader(mdFolder, reportControlPanel).execute();
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
        private long waitTime = 60000;
        private TreeSet<Date> newTransactionDateQueue = new TreeSet<>();

        @Override
        public void run() {
            while (true) {
                Date latestTransactionDate = getLastTransactionModified();
                boolean newTransaction = lastTransactionDate.isNewTransactionDate(latestTransactionDate);
                try {
                    if (newTransaction) {
                        boolean notSeenBefore = newTransactionDateQueue.add(latestTransactionDate);
                        if (notSeenBefore) { // wait for another cycle
                            Thread.sleep(waitTime);
                        } else { // seen before
                            if (new Date().getTime() - newTransactionDateQueue.last().getTime() >= waitTime) {
                                lastTransactionDate.setChanged(latestTransactionDate);
                                newTransactionDateQueue.clear();
                            }
                        }
                    } else {
                        Thread.sleep(waitTime);
                    }
                } catch (InterruptedException e) {
                    LogController.logException(e, "Error in Transaction Monitor");
                }

            }
        }

    }

    private class CurrencyData implements Comparable{
        CurrencyType currencyType;
        TreeSet<Date> dates;


        CurrencyData(CurrencyType currencyType){
            this.currencyType = currencyType;
        }

        @Override
        public int compareTo(Object o) {
            CurrencyData otherCurrency = (CurrencyData) o;
            return currencyType.getIDString().compareTo(otherCurrency.currencyType.getIDString());
        }
    }

    private class MDFileLoader extends SwingWorker<Void, String> {
        File mdFolder;
        ReportControlPanel reportControlPanel;

        MDFileLoader(File mdFile, ReportControlPanel reportControlPanel) {
            this.mdFolder = mdFile;
            this.reportControlPanel = reportControlPanel;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                publish("Loading " + mdFolder.getName());
                AccountBookWrapper wrapper = AccountBookWrapper.wrapperForFolder(mdFolder);
                // must add this section or get null pointer error
                ArrayList<File> folderFiles = new ArrayList<>();
                folderFiles.add(mdFolder);
                AccountBookUtil.INTERNAL_FOLDER_CONTAINERS = folderFiles;

                publish("Doing Initial Load of AccountBook...");
                wrapper.doInitialLoad(null);
                accountBook = wrapper.getBook();
                root = accountBook.getRootAccount();

                lastTransactionDate = new ObservableLastTransactionDate(getLastTransactionModified());
                getLastYahooUpdateDate();
                if(lastYahooqtUpdate != null) getCurrencyUpdateMap();
                for (String msg:getTransactionStatus()){
                    publish(msg);
                }
                reportControlPanel.setAccountAndFolderSubPanels();
                publish(mdFolder.getName() + " Loaded! Choose Report to run.");
                lastTransactionDate.addObserver(reportControlPanel);
                startTransactionMonitorThread();

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
