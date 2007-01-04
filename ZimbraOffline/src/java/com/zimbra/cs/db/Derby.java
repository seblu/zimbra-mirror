/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * The Original Code is: Zimbra Network
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.db;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;

public class Derby extends Db {

    @Override
    void setErrorConstants() {
//        Error.DUPLICATE_ROW = 23505;
        Error.DUPLICATE_ROW = 20000;
        // FIXME: the following have not been altered from the MySQL values and are almost guaranteed to be incorrect
        Error.DEADLOCK_DETECTED = 1213;
        Error.FOREIGN_KEY_NO_PARENT = 1216;
        Error.FOREIGN_KEY_CHILD_EXISTS = 1217;
        Error.NO_SUCH_TABLE = 1146;
    }

    @Override
    void setCapabilities() {
        Capability.LIMIT_CLAUSE = false;
        Capability.BOOLEAN_DATATYPE = false;
        Capability.ON_DUPLICATE_KEY = false;
        Capability.ON_UPDATE_CASCADE = false;
        Capability.MULTITABLE_UPDATE = false;
        Capability.BITWISE_OPERATIONS = false;
        Capability.DISABLE_CONSTRAINT_CHECK = false;
    }

    @Override
    DbPool.PoolConfig getPoolConfig() {
        return new DerbyConfig();
    }

    static final class DerbyConfig extends DbPool.PoolConfig {
        DerbyConfig() {
            mDriverClassName = "org.apache.derby.jdbc.EmbeddedDriver";
            mPoolSize = 12;
            mRootUrl = null;
            mConnectionUrl = "jdbc:derby:" + System.getProperty("derby.system.home", "/opt/zimbra/derby");
            mLoggerUrl = null;
            mSupportsStatsCallback = false;
            mDatabaseProperties = getDerbyProperties();

            ZimbraLog.misc.debug("Setting connection pool size to " + mPoolSize);
        }

        private static Properties getDerbyProperties() {
            Properties props = new Properties();

            props.put("cacheResultSetMetadata", "true");
            props.put("cachePrepStmts", "true");
            props.put("prepStmtCacheSize", "25");        
            props.put("autoReconnect", "true");
            props.put("useUnicode", "true");
            props.put("characterEncoding", "UTF-8");
            props.put("dumpQueriesOnException", "true");
            props.put("user", LC.zimbra_mysql_user.value());
            props.put("password", LC.zimbra_mysql_password.value());

            return props;
        }
    }

    public static void main(String args[]) {
        // command line argument parsing
        Options options = new Options();
        CommandLine cl = Versions.parseCmdlineArgs(args, options);

        String outputDir = cl.getOptionValue("o");
        File outFile = new File(outputDir, "versions-init.sql");
        
        outFile.delete();
        
        Writer output = null;
        
        try {
            output = new BufferedWriter( new FileWriter(outFile) );

            String redoVer = com.zimbra.cs.redolog.Version.latest().toString();
            String outStr = "-- AUTO-GENERATED .SQL FILE - Generated by the Derby versions tool\n" +
                "INSERT INTO zimbra.config(name, value, description) VALUES\n" +
                "\t('db.version', '" + Versions.DB_VERSION + "', 'db schema version'),\n" + 
                "\t('index.version', '" + Versions.INDEX_VERSION + "', 'index version'),\n" +
                "\t('redolog.version', '" + redoVer + "', 'redolog version');\n";

            output.write(outStr);
            
            if (output != null)
                output.close();
        } catch (IOException e){
            System.out.println("ERROR - caught exception at\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
