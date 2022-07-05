/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps 
 * on very large urban areas. It can be used as a Java library or be controlled through 
 * a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the 
 * Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this 
 * License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

import org.h2gis.api.ProgressVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import groovy.json.JsonSlurper
import java.sql.ResultSet

def runScript(connection, scriptFile, arguments) {
    Logger logger = LoggerFactory.getLogger("script")
    GroovyShell shell = new GroovyShell()
    Script scriptInstance = shell.parse(new File(scriptFile))
    
    Object result = scriptInstance.invokeMethod("exec", [connection, arguments])
    if(result != null) {
        logger.info(result.toString())
    }
}

def exec(Connection connection, input) {

     String inputDir = input['input_dir']
     println inputDir

     String ouputDir = input['output_dir']
     println ouputDir

    Boolean fastCompute = true
    if (input['fastCompute']) {
        fastCompute = input['fastCompute']
    }
      println fastCompute

      Boolean confDiffHorizontal = true
      Boolean   confDiffVertical = true
      Double confMaxReflDist = 250
      if (input['ReflDist']) {
            confMaxReflDist = input['ReflDist'] as Double
      }
      println confMaxReflDist



      Double confMaxSrcDist = 4500
      if (input['confMaxSrcDist']) {
            confMaxSrcDist = input['confMaxSrcDist']  as Double
      }
      println confMaxSrcDist


      Integer confReflOrder = 1

    if (fastCompute){
      confDiffHorizontal = false
         confDiffVertical= false
         confMaxReflDist= 50
         confMaxSrcDist = 1500
          confReflOrder =0
    }
      println fastCompute


  runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"BUILD_GRID.shp",
        "inputSRID": "2154"])

  runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"SourceSi_30.shp",
        "inputSRID": "2154"])


  runScript(connection, "noisemodelling/wps/Receivers/Building_Grid.groovy",
        ["tableBuilding":"BUILD_GRID",
        "delta": 10,
        "height":4])

  // Step 5: Run Calculation
  
  runScript(connection, "noisemodelling/wps/NoiseModelling/Noise_level_from_source.groovy",
        ["tableBuilding":"BUILD_GRID", 
        "tableSources":"SourceSi_30", 
        "tableReceivers":"RECEIVERS",
         "tableDEM":"", 
         "tableGroundAbs":"",
         "confSkipLevening" : true,
         "confSkipLnight" : true,
         "confSkipLden" : true,
         "confDiffHorizontal" : confDiffHorizontal,
         "confDiffVertical" : confDiffVertical,
         "confMaxReflDist" : confMaxReflDist,
         "confMaxSrcDist" : confMaxSrcDist,
          "confReflOrder" : confReflOrder,
          "confExportSourceId" : true
         ])

//COMPUTE ISO CONTOURS

  // Step 6: Export (& see) the results
  runScript(connection, "noisemodelling/wps/Import_and_Export/Export_Table.groovy",
        ["exportPath":ouputDir+"AttMatrix.shp", "tableToExport":"LDAY_GEOM"])

 runScript(connection, "noisemodelling/wps/Import_and_Export/Export_Table.groovy",
        ["exportPath":ouputDir+"RECEIVERS.shp", "tableToExport":"RECEIVERS"])

}