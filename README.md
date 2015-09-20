# JIVE

## Quick Start Guide: Development Environment 

* Install Eclipse Mars.

* Clone the **JIVE** repository.

* Open Eclipse and select the **jive** folder containing the cloned repo as your workspace folder.

* Bulk import plug-ins and fragments.

  * Select **Import... | Plug-ins and Fragments**. 
  * In the **Import From** section, select the workspace folder as the **Directory**.
  * In the **Import As** section, select the option **Projects with source folders**.
  * Click **Next**.
  * Add all plug-ins and features.
  * Click **Finish**.

* Import features (repeat this for each feature project).

  * Select **Import... | Existing Projects into Workspace**. 
  * In the **Select root directory** option, select the respective feature project folder.
  * Click **Finish**.
  * The feature projects are: 
    * edu.buffalo.cse.jive.feature
    * edu.buffalo.cse.jive.feature.importer.fiji
    * edu.buffalo.cse.jive.feature.importer.jivere
    * edu.buffalo.cse.jive.feature.launch.pde
    * edu.buffalo.cse.jive.feature.launch.tomcat

* Your projects should build without errors. If you get JRE warnings about the strict compatibility of the JRE, follow the instructions on here to address this issue:

  https://kthoms.wordpress.com/2013/05/17/remove-build-path-specifies-execution-environment-warnings-from-problems-view/

## Quick Start Guide: Run JIVE from Eclipse

* This guides assumes your **JIVE** environment builds all projects successfully.

* Make sure you have the **Plug-in Development** perspective open.

* Select **Run | Run Configurations...**

* Double click the **Eclipse Application** option.

  * A default configuration should have appeared, named *New_configuration*.
  * Rename it as *Jive* or some other name of your preference.
  * (Optional) On the **Main** tab, select where the workspace data will be stored. This is the workspace for the Eclipse instance with the development version of **JIVE** you launch from Eclipse itself. The default should work in most cases.
  * (Optional) On the **Arguments** tab, tweak the arguments passed to the Eclipse instance. For most cases, you do not need to change this.
  * Once you have completed the steps above, save and close the configuration. You can now run and debug **JIVE** from Eclipse by running/debugging your project using this configuration.
