# JIVE

## Setting Up Your Development Environment

* Install Eclipse Mars.

* Clone the repository.

* Open Eclipse and select the **jive** folder containing the cloned repo as your workspace folder.

* Bulk import plug-ins and fragments.

  * Select 'Import...' | 'Plug-ins and Fragments'. 
  * Select the workspace folder and the option 'Projects with source folders' in the 'Import As' section.
  * Click 'Next'.
  * Add all plug-ins and features.
  * Click 'Finish'.

* Import features (repeat this for each feature project).

  * Select 'Import...' | 'Existing Projects into Workspace'. 
  * Select the root folder for the feature.
  * Click 'Finish'.
  * The feature projects are: 
    * edu.buffalo.cse.jive.feature
    * edu.buffalo.cse.jive.feature.importer.fiji
    * edu.buffalo.cse.jive.feature.importer.jivere
    * edu.buffalo.cse.jive.feature.launch.pde
    * edu.buffalo.cse.jive.feature.launch.tomcat

* Your projects should build without errors. If you get JRE warnings about the strict compatibility of the JRE, follow the instructions on here to address this issue:

  https://kthoms.wordpress.com/2013/05/17/remove-build-path-specifies-execution-environment-warnings-from-problems-view/
