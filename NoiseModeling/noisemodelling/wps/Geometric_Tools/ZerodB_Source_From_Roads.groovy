/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by Universit√© Gustave Eiffel and CNRS
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */
/**
 * @Author Valentin Le Bescond, Universit√© Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.Geometric_Tools

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Create 0db Source From Roads'
description = 'Creates a SOURCE table from a ROAD table.' +
        '<br/>The SOURCE table can then be used in the <b>Noise_level_from_source</b> WPS block with the "confExportSourceId" set to true. The Noise_level_from_source output will contain a list of source-receiver attenuation matrix independent of the source absolute noise power levels.'

inputs = [
        roadsTableName: [
                name: 'Input table name',
                title: 'Intput table name',
                description: 'The name of the Roads table.'+
                    '<br/>Must contain at least a <b>PK<b> field with a primary key index and a <b>THE_GEOM</b> geometry field',
                type: String.class
        ],
        sourcesTableName: [
                name: 'Output table name',
                title: 'Output table name',
                description: 'Name of the table you want to create: SOURCES_0DB',
                type: String.class
        ]
]

outputs = [
        result: [
                name: 'Result output string',
                title: 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type: String.class
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


def run(input) {

    // Get name of the database
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}


def exec(connection, input) {

    connection = new ConnectionWrapper(connection)

    Sql sql = new Sql(connection)

    String resultString = null

    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")
    logger.info('Start : Create_0db_Source_From_Roads')
    logger.info("inputs {}", input)

    String roadsTableName = input['roadsTableName']

    String sourcesTableName = input['sourcesTableName']

    sql.execute(String.format("DROP TABLE IF EXISTS %s", sourcesTableName))

    String query = "CREATE TABLE " + sourcesTableName + '''( 
        PK integer PRIMARY KEY,
        THE_GEOM geometry,
        LWD63 double precision, LWD125 double precision, LWD250 double precision, LWD500 double precision, LWD1000 double precision, LWD2000 double precision, LWD4000 double precision, LWD8000 double precision,
        LWE63 double precision, LWE125 double precision, LWE250 double precision, LWE500 double precision, LWE1000 double precision, LWE2000 double precision, LWE4000 double precision, LWE8000 double precision,
        LWN63 double precision, LWN125 double precision, LWN250 double precision, LWN500 double precision, LWN1000 double precision, LWN2000 double precision, LWN4000 double precision, LWN8000 double precision
        ) AS
        SELECT PK, THE_GEOM,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
        0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        FROM ''' + roadsTableName + ''' 
    ;'''

    sql.execute(query)

    logger.info('End : Create_0db_Source_From_Roads')
    resultString = "Process done. Table " + sourcesTableName + " created !"
    logger.info('Result : ' + resultString)
    return resultString
}

