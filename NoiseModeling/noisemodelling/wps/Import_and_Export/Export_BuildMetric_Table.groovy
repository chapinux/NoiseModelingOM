/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 * @Author Nicolas Fortin, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Import_and_Export

import geoserver.GeoServer
import geoserver.catalog.Store
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.api.EmptyProgressVisitor
import org.h2gis.functions.io.csv.CSVDriverFunction
import org.h2gis.functions.io.dbf.DBFDriverFunction
import org.h2gis.functions.io.geojson.GeoJsonDriverFunction
import org.h2gis.functions.io.json.JsonDriverFunction
import org.h2gis.functions.io.kml.KMLDriverFunction
import org.h2gis.functions.io.shp.SHPDriverFunction
import org.h2gis.functions.io.tsv.TSVDriverFunction
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

import groovy.sql.Sql

title = 'Export BuildMetric table'
description = 'Export BuildMetric table from the database to a local file. </br> Valid file extensions : (csv, dbf, geojson, gpx, bz2, gz, osm, shp, tsv).'

inputs = [
        exportPath   : [
                name: 'Export path', title: 'Path of the file you want to export',
                description: 'Path of the file, including its extension. ' +
                        '</br> For example : c:/home/receivers.geojson',
                type: String.class
        ],
        listSource   : [
                name: 'List path', title: 'List of the file you want to export',
                description: 'List of the file, including its extension. ' +
                        '</br> For',
                type: String.class
        ],
        tableToExport: [
                name: 'Name of the table to export',
                title: 'Name of the table',
                description: 'Name of the table you want to export.',
                type: String.class
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def exec(Connection connection, input) {

    // Create a connection statement to interact with the database in SQL
     Sql sql = new Sql(connection)

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Export File')
    logger.info("inputs {}", input) // log inputs of the run


   // sql.execute('DROP TABLE COMPLETE IF EXISTS; ')
 //sql.execute('CREATE TABLE COMPLETE AS SELECT PK, NS_PERCU, AUDI_SIR, HZ500 FROM LDAY_GEOM A INNER JOIN RCVS20 B ON A.IDRECEIVER = B.PK; ')
String listSource = input["listSource"] as String

sql.execute('DROP TABLE BUILD_RCV_LVL IF EXISTS; ')
sql.execute('CREATE TABLE BUILD_RCV_LVL AS SELECT B.BUILD_PK, A.HZ500, A.IDSOURCE  FROM LDAY_GEOM A INNER JOIN RECEIVERS B ON A.IDRECEIVER = B.PK;')

sql.execute('DROP TABLE BUILD_GRID_LVL IF EXISTS; ')
sql.execute('CREATE TABLE BUILD_GRID_LVL AS SELECT B.PK, B.NB_LOGTS, A.HZ500, A.IDSOURCE FROM BUILD_RCV_LVL A LEFT JOIN BUILD_GRID B ON A.BUILD_PK = B.PK;')

sql.execute('DROP TABLE COUNT_BUILD IF EXISTS; ')
sql.execute('CREATE TABLE COUNT_BUILD AS SELECT SUM(NB_LOGTS) FROM BUILD_GRID_LVL WHERE HZ500>70 AND IDSOURCE IN '+listSource+';')

 //sql.execute('DROP TABLE CORREL IF EXISTS;')
 //sql.execute('CREATE TABLE CORREL AS SELECT CORR(AUDI_SIR, HZ500) FROM COMPLETE;')
 
    // get Export Path
    String exportPath = input["exportPath"] as String

    // get the name of the table to export
    String tableToExport = input["tableToExport"] as String
    // do it case-insensitive
    tableToExport = tableToExport.toUpperCase()

    List<String> fields = JDBCUtilities.getColumnNames(connection, tableToExport)
    if (fields.size()<1)
    {
        throw new Exception("The table is empty and can not be exported.")
    }

    CSVDriverFunction csvDriver = new CSVDriverFunction()
    csvDriver.exportTable(connection, tableToExport, new File(exportPath), true, new EmptyProgressVisitor())

    // print to command window
    logger.info(resultString)
    logger.info('End : Export File')
    // print to WPS Builder
    return resultString

}

def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}