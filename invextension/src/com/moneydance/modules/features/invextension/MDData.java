package com.moneydance.modules.features.invextension;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.infinitekind.moneydance.model.CurrencySnapshot;
import com.infinitekind.moneydance.model.CurrencyType;
import com.moneydance.apps.md.controller.AccountBookWrapper;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import com.moneydance.apps.md.controller.io.AccountBookUtil;

import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Singleton Class holds all Moneydance Data
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
class MDData {
    File mdFolder;
    private FeatureModuleContext featureModuleContext;
    private AccountBook accountBook;
    private Account root;
    private BulkSecInfo currentInfo;
    private ObservableLastTransactionDate observableLastTransactionDate;
    private Date lastPriceUpdateTime;
    private final HashMap<String, Double> userRateMap = new HashMap<>();
    private Main extension;
    private Thread transactionMonitorThread;
    private TransactionMonitor transactionMonitor;


    private static MDData uniqueInstance;
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

    public void startTransactionMonitorThread(TotalReportOutputFrame totalReportOutputFrame, long updateFrequencyMins){
        transactionMonitor = new TransactionMonitor(totalReportOutputFrame, updateFrequencyMins);
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
        GregorianCalendar mingc = new GregorianCalendar();
        mingc.setTime(new Date(Long.MIN_VALUE));
        Date maxDate = mingc.getTime();
        for (CurrencyType currency : currencies) {
            if (currency.getCurrencyType() == CurrencyType.Type.SECURITY && currency.getSnapshots().size() > 0) {
                CurrencySnapshot currencySnapshot = currency.getSnapshots().get(currency.getSnapshots().size() - 1);
                Double userRate = currencySnapshot.getRate();
                int snapshotDateInt = currencySnapshot.getDateInt();
                Date snapshotDate = DateUtils.convertToDate(snapshotDateInt);
                String currencyID = currency.getParameter("id");
                userRateMap.put(currencyID, userRate);
                if (snapshotDate.after(maxDate)) maxDate = snapshotDate;
            }
        }
        lastPriceUpdateTime = maxDate;
    }


    public Date getLastTransactionModified(){
        if(accountBook != null){
            File syncOutFolder = new File(accountBook.getRootFolder(), "safe/tiksync/out");
            FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith(".txn");
            File[] files = syncOutFolder.listFiles(filter);
            long tempModifedTxn = 0L;
            assert files != null;
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

    java.util.List<String> getTransactionStatus(){
        List<String> msgs = new ArrayList<>();
        msgs.add("MD last modified: " + DATE_PATTERN_MEDIUM.format
                (observableLastTransactionDate.getLastTransactionDate()));
        generateCurrentPriceData();
        msgs.add("latest security price: " +
                DATE_PATTERN_MEDIUM.format(lastPriceUpdateTime));
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

        generateCurrentPriceData();

    }

    protected void initializeMDDataHeadless(boolean newObservable) throws Exception{
        AccountBookWrapper wrapper = AccountBookWrapper.wrapperForFolder(mdFolder);
        // must add this section or get null pointer error
        ArrayList<File> folderFiles = new ArrayList<>();
        folderFiles.add(mdFolder);
        AccountBookUtil.INTERNAL_FOLDER_CONTAINERS = folderFiles;
        wrapper.loadDataModel(null);
        featureModuleContext = null;
        accountBook = wrapper.getBook();
        root = accountBook.getRootAccount();
        if(newObservable) observableLastTransactionDate =
                new ObservableLastTransactionDate(getLastTransactionModified());
        generateCurrentPriceData();
    }

    public void loadMDFile(File mdFolder, ReportControlPanel reportControlPanel) {
        new MDFileLoader(mdFolder, reportControlPanel).execute();
    }

    public void reloadMDData(ReportConfig reportConfig) throws Exception {
        if(featureModuleContext != null){
            initializeMDDataInApplication(false);
        } else {
            initializeMDDataHeadless(false);
        }
        currentInfo = new BulkSecInfo(accountBook, reportConfig);
    }


    public static class ObservableLastTransactionDate {
        private final PropertyChangeSupport support;
        Date lastTransactionDate;
        TreeSet<Date> previousLastTransactionDates = new TreeSet<>();
        
        ObservableLastTransactionDate(Date lastTransactionDate){

            this.lastTransactionDate = lastTransactionDate;
            this.support = new PropertyChangeSupport(this);
        }

        public void addPropertyChangeListener(PropertyChangeListener pcl) {
            support.addPropertyChangeListener(pcl);
        }

        public void removePropertyChangeListener(PropertyChangeListener pcl) {
            support.removePropertyChangeListener(pcl);
        }

        public void setChanged(Date newLastTransactionDate){
            support.firePropertyChange("lastTransactionDate",
                    this.lastTransactionDate, newLastTransactionDate);
            previousLastTransactionDates.add(lastTransactionDate);
            lastTransactionDate = newLastTransactionDate;
        }

        public boolean isNewTransactionDate(Date transactionDate){
            return transactionDate.after(lastTransactionDate);
        }

        public Date getLastTransactionDate() {
            return lastTransactionDate;
        }

    }
    

    private class TransactionMonitor implements Runnable {
        private  Date lastRefreshTime;
        private final long updateFrequencyMins;
        private final TotalReportOutputFrame totalReportOutputFrame;
        private volatile boolean shutdown = false;

        TransactionMonitor(TotalReportOutputFrame totalReportOutputFrame, long updateFrequencyMins){
            this.totalReportOutputFrame = totalReportOutputFrame;
            this.updateFrequencyMins = updateFrequencyMins;
        }



        @Override
        public void run() {
            totalReportOutputFrame.updateStatus(String.format("Starting transaction monitor with frequency %d minutes",
                    updateFrequencyMins));
            if(!totalReportOutputFrame.isLiveReport()) totalReportOutputFrame
                    .updateStatus("Warning: Report end date is not today!");
            while (!shutdown) {

                try {
                    // refresh Data, if update extension is used, it will go in the refreshData method
                    if(lastRefreshTime == null){ //refresh immediately
                        lastRefreshTime = new Date();
                        refreshData();
                    } else {
                        if(isTimeToRefresh()){// refresh after interval
                            lastRefreshTime = new Date();
                            refreshData();
                        }
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

        public void refreshData() {
            Date latestTransactionDate = getLastTransactionModified();
            boolean newTransaction = observableLastTransactionDate.isNewTransactionDate(latestTransactionDate);
            //check for new transactions
            if (newTransaction) {
                observableLastTransactionDate.setChanged(latestTransactionDate);
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
        protected Void doInBackground() {
            try {
                publish("Loading " + mdFolder.getName());
                initializeMDDataHeadless(true);
                getTransactionStatus().forEach(this::publish);
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
