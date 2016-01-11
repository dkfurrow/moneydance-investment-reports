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
public class MDData extends Observable {
    private FeatureModuleContext featureModuleContext;
    private AccountBook accountBook;
    private Account root;
    private BulkSecInfo currentInfo;
    private Date lastModifiedTxn;
    private Date lastYahooqtUpdate;
    private TreeMap<Date, Integer> currencyUpdateMap = new TreeMap<>();
    private Main extension;
    private static MDData uniqueInstance;
    private static final DateFormat DATE_PATTERN_LONG =  DateFormat.getDateTimeInstance
            (DateFormat.LONG, DateFormat.LONG);
    private static final DateFormat DATE_PATTERN_SHORT =  DateFormat.getDateInstance
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

    public FeatureModuleContext getFeatureModuleContext() {
        return featureModuleContext;
    }

    public AccountBook getAccountBook() {
        return accountBook;
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

    public void setFeatureModuleContext(FeatureModuleContext featureModuleContext) {
        this.featureModuleContext = featureModuleContext;
    }

    public void setCurrentInfo(ReportConfig reportConfig) throws Exception {
        if(accountBook == null) throw new Exception("Call to setCurrentInfo, but account book is null");
        this.currentInfo = new BulkSecInfo(accountBook, reportConfig);
    }

    public void setRoot(Account root) {
        this.root = root;
    }

    public void setAccountBook(AccountBook accountBook) {
        this.accountBook = accountBook;
    }

    public void setExtension(Main extension){
        this.extension = extension;
    }

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

    public void getTransactionUpdateMap(){
        java.util.List<CurrencyType> currencies = accountBook
                .getCurrencies().getAllCurrencies();
        for (CurrencyType currency: currencies){
            String priceDateStr = currency.getParameter("price_date");
            if (priceDateStr != null) {
                Date priceDate = new Date(Long.parseLong(priceDateStr));
                if (currencyUpdateMap.containsKey(priceDate)){
                    int currencyCount = currencyUpdateMap.get(priceDate);
                    currencyCount ++;
                    currencyUpdateMap.put(priceDate, currencyCount);
                } else {
                    currencyUpdateMap.put(priceDate, 1);
                }
            }
        }
    }

    public void getLastYahooUpdateDate(){
        Integer lastUpdateDate = root.getIntParameter("yahooqt.quoteLastUpdate",0);
        if (lastUpdateDate == 0){
            lastYahooqtUpdate = null;
        } else {
            lastYahooqtUpdate = DateUtils.convertToDate(lastUpdateDate);
        }
    }

    public void setLastTransactionModified(){
        if(accountBook != null){
            File syncOutFolder = new File(accountBook.getRootFolder(), "safe/tiksync/out");
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".txn");
                }
            };
            File[] files = syncOutFolder.listFiles(filter);
            long tempModifedTxn = 0L;
            for(File file: files){
                tempModifedTxn = Math.max(tempModifedTxn, file.lastModified());
            }
            lastModifiedTxn = new Date(tempModifedTxn);
        }
    }

    java.util.List<String> getTransactionStatus(){
        List<String> msgs = new ArrayList<>();
        msgs.add("Last Modified Txn: " + DATE_PATTERN_LONG.format(lastModifiedTxn));
        if(lastYahooqtUpdate != null){
            msgs.add("YahooQuote last Update: " + DATE_PATTERN_SHORT.format(lastYahooqtUpdate));
            getTransactionUpdateMap();
            msgs.add("YahooQuote latest Security Price: " +
                    DATE_PATTERN_LONG.format(currencyUpdateMap.lastEntry().getKey()));
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
        setLastTransactionModified();
        getLastYahooUpdateDate();
        if(lastYahooqtUpdate != null) getTransactionUpdateMap();
    }

    public void loadMDFile(File mdFolder, ReportControlPanel reportControlPanel) {
        new MDFileLoader(mdFolder, reportControlPanel).execute();
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


                setLastTransactionModified();
                getLastYahooUpdateDate();
                if(lastYahooqtUpdate != null) getTransactionUpdateMap();
                for (String msg:getTransactionStatus()){
                    publish(msg);
                }
                reportControlPanel.setAccountAndFolderSubPanels();
                publish(mdFolder.getName() + " Loaded! Choose Report to run.");
            } catch (Exception e) {
                publish("Error! " + mdFolder.getName() + " not loaded!");
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
