/*-
 * #%L
 * de.hs-heilbronn.mi:crawler4j-frontier-hsqldb
 * %%
 * Copyright (C) 2010 - 2021 crawler4j-fork (pre-fork: Yasser Ganjisaffar)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.hshn.mi.crawler4j.frontier;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.hshn.mi.crawler4j.exception.HSQLDBFetchException;
import de.hshn.mi.crawler4j.url.HSQLDBWebURLFactory;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.frontier.DocIDServer;
import edu.uci.ics.crawler4j.frontier.Frontier;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import edu.uci.ics.crawler4j.url.WebURLFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class HSQLDBFrontierConfiguration implements FrontierConfiguration {

    private final HikariDataSource dataSource;
    private final DocIDServer docIDServer;
    private final Frontier frontier;

    public HSQLDBFrontierConfiguration(CrawlConfig crawlConfig, int poolSize) {
        this(crawlConfig, poolSize, false);
    }

    public HSQLDBFrontierConfiguration(CrawlConfig crawlConfig, int poolSize, boolean shutdown) {

        HikariConfig config = new HikariConfig();
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        config.setJdbcUrl(getJDBCUrl(crawlConfig.isResumableCrawling(), crawlConfig.getCrawlStorageFolder(), shutdown));
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(poolSize);

        dataSource = new HikariDataSource(config);

        prepareDatabaseSchema();

        frontier = new HSQLDBFrontierImpl(dataSource, getWebURLFactory(), crawlConfig);
        docIDServer = new HSQLDBDocIDServerImpl(dataSource);
    }

    private void prepareDatabaseSchema() {
        try (Connection c = dataSource.getConnection()) {

            String sequence = "CREATE SEQUENCE IF NOT EXISTS id_master_seq START WITH 1";

            String table = "CREATE TABLE IF NOT EXISTS weburl(" +
                    "  id bigint NOT NULL," +
                    "  url varchar(4096)," +
                    "  parenturl varchar(4096)," +
                    "  parentid bigint," +
                    "  cdepth int," +
                    "  priority int," +
                    "  anchor varchar(4096)," +
                    "  status varchar(16)," +
                    "  primary key (id)" +
                    ");";

            String indexUrl = "CREATE INDEX IF NOT EXISTS idx_url ON weburl (url)";
            String indexStatus = "CREATE INDEX IF NOT EXISTS idx_status ON weburl (status)";

            try (PreparedStatement s = c.prepareStatement(sequence)) {
                s.executeUpdate();
            }

            try (PreparedStatement s = c.prepareStatement(table)) {
                s.executeUpdate();
            }

            try (PreparedStatement s = c.prepareStatement(indexUrl)) {
                s.executeUpdate();
            }

            try (PreparedStatement s = c.prepareStatement(indexStatus)) {
                s.executeUpdate();
            }
        } catch (SQLException e) {
            throw new HSQLDBFetchException(e);
        }

    }

    private String getJDBCUrl(boolean resumableCrawling, String crawlStorageFolder, boolean shutdown) {
        if (resumableCrawling) {
            return "jdbc:hsqldb:file:" + crawlStorageFolder + "/frontier;sql.syntax_pgs=true" + (shutdown ? ";shutdown=true" : "");
        } else {
            return "jdbc:hsqldb:mem:crawler4j;sql.syntax_pgs=true";
        }
    }

    @Override
    public DocIDServer getDocIDServer() {
        return docIDServer;
    }

    @Override
    public Frontier getFrontier() {
        return frontier;
    }

    @Override
    public WebURLFactory getWebURLFactory() {
        return new HSQLDBWebURLFactory();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
