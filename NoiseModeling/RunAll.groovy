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

     
    // Fast compute?
    Boolean fastCompute = true
    if (input['fastCompute']) {
        fastCompute = Boolean.parseBoolean(input['fastCompute'] as String)
    }
     println fastCompute

    if (fastCompute){
      confDiffHorizontal = false
      confDiffVertical= false
      confMaxReflDist= 50
      confMaxSrcDist = 1500
      confReflOrder = 0
      println "fastCompute : OK " 
    }

      println "fastCompute : " + fastCompute

      // Input parameters

      // Diffractions
      Boolean diffHor = true
      if (input['confDiffHorizontal']) {
            diffHor = input['confDiffHorizontal'] as Boolean
      }
      println diffHor

      Boolean diffVer = true
      if (input['diffVer']) {
            diffVer = input['diffVer'] as Boolean
      }
      println diffVer

      //Max reflexion distance
      Double confMaxReflDist = 250 
      if (input['ReflDist']) {
            confMaxReflDist = input['ReflDist'] as Double
      }
      println confMaxReflDist

      //Favorable conditions probability
      String prob = "Hom"
      if (input['OccProb']) {
            prob = input['OccProb'] as String
      }
      switch(prob) { 
      case "Hom": 
      cond = '0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5'
      case "NO": 
      cond = '1,1,1,1,0,0,0,0,0,0,0,0,0,0,0,0'
      case "SE": 
      cond = '0,0,0,0,1,1,1,1,0,0,0,0,0,0,0,0'
      case "SO": 
      cond = '0,0,0,0,0,0,0,0,1,1,1,1,0,0,0,0'
      case "NE": 
      cond = '0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1'
      } 
      println prob

      // Meteo
      Double airTemp = 15
      if (input['airTemp']) {
            airTemp = input['airTemp']  as Double
      }
      println airTemp

      Double humidity = 70
      if (input['humidity']) {
            humidity = input['humidity']  as Double
      }
      println humidity

      // Max distance source-receiver
      Double confMaxSrcDist = 4500
      if (input['confMaxSrcDist']) {
            confMaxSrcDist = input['confMaxSrcDist']  as Double
      }
      println confMaxSrcDist

      //Reflexion order
      Integer refOrd = 1
      if (input['refOrd']) {
            refOrd = input['refOrd'] as Integer
      }
      println refOrd

      //DEM
      Boolean dem = true
      if (input['dem']) {
            dem = input['dem'] as Boolean
      }
      println dem

      //Wall alpha
      Double wallAlpha = 0.5 
      if (input['wallAlpha']) {
            wallAlpha = input['wallAlpha'] as Double
      }
      println wallAlpha
      
      // Layers

  runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"BUILD_GRID.shp",
        "inputSRID": "2154"])

      if (dem) {
      runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
            ["pathFile":inputDir+"DEM.shp",
            "inputSRID": "2154"])           
      }

  runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"LANDCOVER.shp",
        "inputSRID": "2154"])

  runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"SourceSi_2.shp",
        "inputSRID": "2154"])

  runScript(connection, "noisemodelling/wps/Geometric_Tools/Set_Height.groovy",
        ["tableName":"SourceSi_2",
       "height": 10.0])

  runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"RCVS20.shp",
       "inputSRID": "2154"])

  runScript(connection, "noisemodelling/wps/Geometric_Tools/Set_Height.groovy",
        ["tableName":"RCVS20",
       "height": 2.0])

//COMPUTE DELAUNAY


  /*runScript(connection, "noisemodelling/wps/Import_and_Export/Import_File.groovy",
        ["pathFile":inputDir+"DEM.shp",
        "inputSRID": "2154"])
*/
 // runScript(connection, "noisemodelling/wps/Import_and_Export/Import_Asc_File.groovy",
 //       ["pathFile":"resources/org/noise_planet/noisemodelling/wps/outfile.asc",
 //       "inputSRID": "2154"])


  // Step 5: Run Calculation
  
  runScript(connection, "noisemodelling/wps/NoiseModelling/Noise_level_from_source.groovy",
        ["tableBuilding":"BUILD_GRID", 
         "tableSources":"SourceSi_2", 
         "tableReceivers":"RCVS20",
         "tableDEM":"DEM", 
         "tableGroundAbs":"LANDCOVER",
         "confSkipLevening" : true,
         "confSkipLnight" : true,
         "confSkipLden" : true,
         "confDiffHorizontal" : diffHor,
         "confDiffVertical" : diffVer,
         "confMaxReflDist" : confMaxReflDist,
         "confMaxSrcDist" : confMaxSrcDist,
         "confFavorableOccurrencesDay" : cond,
         "confTemperature" : airTemp,
         "confHumidity" : humidity,          
         "confReflOrder" : refOrd,
         "paramWallAlpha" : wallAlpha
         ])

//COMPUTE ISO CONTOURS


  // Step 6: Export (& see) the results
runScript(connection, "noisemodelling/wps/Import_and_Export/Export_Table.groovy",
        ["exportPath":ouputDir+"LDAY_GEOM.shp", "tableToExport":"LDAY_GEOM"])

runScript(connection, "noisemodelling/wps/Import_and_Export/Export_Corr_Table.groovy",
        ["exportPath":ouputDir+"Corr.csv", "tableToExport":"COMPLETE"])
        
}