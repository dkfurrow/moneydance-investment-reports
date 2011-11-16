package com.moneydance.modules.features.invextension;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//NOTE: If tests won't run, workaround as follows:
// Remove JUnit.jar from build path, and add back
@RunWith(Suite.class)
@SuiteClasses({ BulkSecInfoTest.class, ReportProdTest.class })
public class AllTests {

}
