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

     String ouputDir = input['output_dir']
     println ouputDir

      String listSource = input['listSource']
     println listSource
   
   runScript(connection, "noisemodelling/wps/Import_and_Export/Export_BuildMetric_Table.groovy",
        ["exportPath":ouputDir+"Count.csv",
        "tableToExport":"COUNT_BUILD",
        "listSource":listSource])

}