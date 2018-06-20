# Moneydance Investment Reports

The Investment Reports Extension (invextension.mxt) is a set of four useful investment reports which are displayed in table form or downloaded in .csv form to a directory of the user's choosing. Reports are detailed below, and a basic overview of classes is provided. Reference website is: http://code.google.com/p/moneydance-investment-reports/. Email for more info is: dkfurrow@gmail.com.

## Getting Started

To download the executable (*.mxt) file from its official source, got to the "Extensions/Manage Extension" in your moneydance app, and add this extension.  To build the app on your own machine, use ANT, the build.xml file is in the src directory.

### Prerequisites

Any version of Java above 8 (current build) 1.8.0_161 will build the .mxt file 

## Running the tests

The tests verify transaction data and replicate report output from a test data file "testMD02.moneydance" which is found in the resources directory.  The data file contains a series of dummy transactions and securities (from the 2009-2010 timeframe) which span a variety of attributes (transaction type, term, etc).  Tests include:

1. BulkSecInfo tests: test whether transaction data is correctly translated from base datafile into java objects, also specifically test whether lot matching and average cost basis is working correctly.
2. ReportProd tests: test consistency of reports (one 'FromTo' report and one 'Snap' report) compared to saved data (csv files) contained within the resources directory.
3. ConsistencyTest tests: Iteratively runs 'FromTo' report to check whether return values produced are consistent with values in the 'Snap Reports'.

### Also Useful:
Within the test folder is a module *TestReportOutput* which will run the reports (by default pointing to the test datafile indicated above) *headless*, i.e. from the development environment, without the need to open moneydance.  This feature is essential for running/debugging any modifications.

And of course, there is a help file accessible from within the application, or [here](https://github.com/dkfurrow/moneydance-investment-reports/blob/master/invextension/src/com/moneydance/modules/features/invextension/InvestmentReportsHelp.html) 

## Built With

* [Ant](https://ant.apache.org/) - build, dependency management

## Contributing

Please ensure that all test pass before submitting pull requests.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags for this project](https://github.com/dkfurrow/moneydance-investment-reports/tags). 

## Authors

* **Dale Furrow** - [Dale's Github](https://github.com/dkfurrow/)
* **Jim Larus** - [Jim's Wikipedia Entry](https://en.wikipedia.org/wiki/James_Larus)

## License

This project is licensed under the BSD License - see the [Open Source Initiative](https://opensource.org/licenses/BSD-3-Clause) file for details

## Acknowledgments

* Thanks to Sean Reilly at Moneydance of numerous tips and encouragements

&nbsp;&nbsp;&nbsp;Thanks to the following people for testing and encouragement

* Chris Capurro
* Darren Schapansky
* Allan Dean
* Hal Corbould
