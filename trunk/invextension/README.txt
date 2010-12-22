
DESCRIPTION

This is a toolkit that will assist in the creation of extensions
for the Moneydance personal finance application.  Extensions can
be compiled, packaged, and signed using the ANT build tool.  ANT
is open source, and is increasingly used to replace Makefiles 
when building Java projects.  ANT can be downloaded for many
platforms from http://ant.apache.org/

The Moneydance Developers Kit includes an ANT build.xml, the necessary
jar files to compile and build an extension, and working sample source
code for a new extension.  API documentation for Moneydance can be
found in the "Developer" section of http://moneydance.com/

Note: Extensions that you sign can only be used with Moneydance 
builds 248 and greater.


USAGE

Before building your extension, you will first need to generate a
key pair.  This can be done by running "ant genkeys" from the "src"
directory.  You will be prompted for a passphrase that is used to
encrypt the private key file.  Your new keys will be stored in the
priv_key and pub_key files.

Once your keys have been generated, you are ready to compile an 
extension.  The build.xml file has been set up to compile and
build the sample extension with ID "myextension".  The source 
code for this sample extension can be found under:
  src/com/moneydance/modules/features/myextension/

To compile and package the sample extension, run "ant myextension"
from the src directory.  After the extension is compiled and built,
you will be asked for the passphrase to your private key which will
be used to sign the extension and place the new extension file in
the dist directory with the name myextension.mxt.  Please feel free 
to modify the source to the "myextension" extension to build your own 
extensions.

If you would like to share your extension with others and would prefer
they not see the unrecognized-signature warning when loading the extension
then you can send your source code to support@moneydance.com where we
will inspect the source code for security problems, compile the
extension, and sign it with the official moneydance key.  If you like,
we can then also put the extension into the list of available extensions
for all Moneydance users to see.


ADVANCED USAGE

To create your own extension that is separate from the sample 
extension you must first come up with a unique ID for your extension.  
An extension ID is all lower case, alphanumeric, and preferably 8 
characters or less.  For this example, let's say your new extension
ID is "newextension".  You would take the following steps to set up
the development environment for the new extension:

1) Copy the files from 
     src/com/moneydance/modules/features/myextension/
   to
     src/com/moneydance/modules/features/newextension/
2) Edit the new java source files to change the package names from
     com.moneydance.modules.features.myextension
   to
     com.moneydance.modules.features.newextension
   When loading an extension, looks for the class named:
   com.moneydance.modules.features.{extensionid}.Main
3) Add a "newextension" target to the build.xml file.  This can be
   done easily by duplicating the "myextension" target in the
   build.xml file, and changing every instance of "myextension" to
   "newextension" in the new target.
4) At this point you can run "ant newextension" in the src directory
   and your new extension will be built and placed in the dist
   directory.


FURTHER ASSISTANCE

If you would like further assisstance, please contact support@moneydance.com
We will be more than happy to answer any questions.

