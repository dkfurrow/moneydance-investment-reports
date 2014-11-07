:: to run an ANT script where the Ant Script is in the
:: same directory as this batch file
@ECHO OFF
::get script drive, script path
set _SCRIPT_DRIVE=%~d0 
set _SCRIPT_PATH=%~dp0
:: Output Variables
ECHO DRIVE: %_SCRIPT_DRIVE%
ECHO PATH: %_SCRIPT_PATH%
cd %_SCRIPT_PATH%
ant -listener org.apache.tools.ant.XmlLogger
PAUSE

