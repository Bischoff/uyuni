/*
 * Copyright (c) 2021 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package com.redhat.rhn.taskomatic.task;

import com.redhat.rhn.common.db.datasource.*;
import com.redhat.rhn.common.hibernate.ConnectionManager;
import com.redhat.rhn.common.hibernate.ConnectionManagerFactory;
import com.redhat.rhn.common.hibernate.ReportDbHibernateFactory;

import org.hibernate.Session;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ReportDbUpdateTask extends RhnJavaJob {

    private static final int BATCH_SIZE = 100;

    private <T> Stream<DataResult<T>> batchStream(SelectMode m, int batchSize, int initialOffset) {
        return Stream.iterate(initialOffset, i -> i + batchSize)
                .map(offset -> {
                    return (DataResult<T>)m.execute(Map.of(
                            "offset", offset,
                            "limit", batchSize
                    ));
                })
                .takeWhile(batch -> !batch.isEmpty());
    }

    private WriteMode generateDelete(String table, Session session) {
        return new WriteMode(session, new ParsedMode() {
            @Override
            public String getName() {
                return "generated.delete."+table;
            }

            @Override
            public ModeType getType() {
                return ModeType.WRITE;
            }

            @Override
            public ParsedQuery getParsedQuery() {
                return new ParsedQuery() {
                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public String getAlias() {
                        return "";
                    }

                    @Override
                    public String getSqlStatement() {
                        return "DELETE FROM " + table + " WHERE mgm_id = :mgm_id";
                    }

                    @Override
                    public String getElaboratorJoinColumn() {
                        return "";
                    }

                    @Override
                    public List<String> getParameterList() {
                        return List.of("mgm_id");
                    }

                    @Override
                    public boolean isMultiple() {
                        return false;
                    }
                };
            }

            @Override
            public String getClassname() {
                return null;
            }

            @Override
            public List<ParsedQuery> getElaborators() {
                return null;
            }
        });
    }

    private WriteMode generateInsert(long mgmId, String table, Set<String> params, Session session) {
        return new WriteMode(session, new ParsedMode() {
            @Override
            public String getName() {
                return "generated.insert."+table;
            }

            @Override
            public ModeType getType() {
                return ModeType.WRITE;
            }

            @Override
            public ParsedQuery getParsedQuery() {
                return new ParsedQuery() {
                    @Override
                    public String getName() {
                        return "";
                    }

                    @Override
                    public String getAlias() {
                        return "";
                    }

                    @Override
                    public String getSqlStatement() {
                        return "INSERT INTO " + table + " ( mgm_id, synced_date," + params.stream().collect(Collectors.joining(",")) + ")\n" +
                                "      VALUES (" + mgmId + ", current_timestamp, " + params.stream().map(p -> ":" + p).collect(Collectors.joining(",")) + ")";
                    }

                    @Override
                    public String getElaboratorJoinColumn() {
                        return "";
                    }

                    @Override
                    public List<String> getParameterList() {
                        return new ArrayList<>(params);
                    }

                    @Override
                    public boolean isMultiple() {
                        return false;
                    }
                };
            }

            @Override
            public String getClassname() {
                return null;
            }

            @Override
            public List<ParsedQuery> getElaborators() {
                return null;
            }
        });
    }

    private void fillReportDbTable(Session session, String xmlQueryName, String queryMode, String tableName,
                                   Set<String> columns, long mgmId) {
        SelectMode m = ModeFactory.getMode(xmlQueryName, queryMode, Map.class);
        WriteMode rd = generateDelete(tableName, session);
        rd.executeUpdate(Map.of("mgm_id", 1));
        WriteMode insert = generateInsert(mgmId, tableName, columns, session);
        this.<Map<String, Object>>batchStream(m, BATCH_SIZE, 0)
                .flatMap(batch -> batch.stream())
                .forEach(set -> insert.executeUpdate(set));
    }

    @Override
    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        ConnectionManager rcm = ConnectionManagerFactory.localReportingConnectionManager();
        ReportDbHibernateFactory rh = new ReportDbHibernateFactory(rcm);
        //fillSystems(rh.getSession());
        fillReportDbTable(rh.getSession(), "SystemReport_queries", "system", "system",
                Set.of("system_id", "profile_name", "hostname", "minion_id",
                        "minion_os_family", "minion_kernel_live_version", "machine_id",
                        "registered_by", "registration_time", "last_checkin_time", "kernel_version",
                        "architecture", "organization", "hardware" ), 1L);
        fillReportDbTable(rh.getSession(), "SystemReport_queries", "SystemHistory", "SystemHistory",
                Set.of("history_id", "system_id", "event", "event_data", "event_time", "hostname"), 1L);
        rh.commitTransaction();
        rh.closeSession();
        rh.closeSessionFactory();
    }

    @Override
    public String getConfigNamespace() {
        return "report_db_update";
    }
}