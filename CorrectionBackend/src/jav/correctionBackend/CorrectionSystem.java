package jav.correctionBackend;

import jav.logging.log4j.Log;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import org.h2.jdbcx.JdbcConnectionPool;
import org.netbeans.api.progress.ProgressHandle;
import org.xml.sax.SAXException;

/**
 * Copyright (c) 2012, IMPACT working group at the Centrum für Informations- und
 * Sprachverarbeitung, University of Munich. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * This file is part of the ocr-postcorrection tool developed by the IMPACT
 * working group at the Centrum für Informations- und Sprachverarbeitung,
 * University of Munich. For further information and contacts visit
 * http://ocr.cis.uni-muenchen.de/
 *
 * @author thorsten (thorsten.vobl@googlemail.com)
 */
public class CorrectionSystem {

    private JdbcConnectionPool jcp;
    private SpreadIndexDocument document;
    private OcrDocumentParser parser;

    public CorrectionSystem() {
    }

    public int openDocument(String dbPath) {
        int retval = 0;
        jcp = JdbcConnectionPool.create("jdbc:h2:" + dbPath + ";AUTO_RECONNECT=TRUE;MVCC=true", "SA", "");
        jcp.setMaxConnections(50);
        jcp.setLoginTimeout(0);

        this.document = new SpreadIndexDocument(jcp);
        document.loadNumberOfPagesFromDB();
        document.loadNumberOfTokensFromDB();
        return retval;
    }

    private int newDocDatabase(String dbPath) {
        int retval = -1;
        try {
//            File f = new File(dbPath + ".h2.db");
            Statement s;
            jcp = JdbcConnectionPool.create("jdbc:h2:" + dbPath + ";AUTO_RECONNECT=TRUE;MVCC=true", "SA", "");
            jcp.setMaxConnections(50);
            jcp.setLoginTimeout(0);

            Connection conn = jcp.getConnection();
            s = conn.createStatement();
            s.execute("DROP TABLE token IF EXISTS");
//                s.execute("DROP TABLE page IF EXISTS");
            s.execute("DROP TABLE candidate IF EXISTS");
            s.execute("DROP TABLE pattern IF EXISTS");
            s.execute("DROP TABLE patternoccurrence IF EXISTS");
            s.execute("DROP TABLE undoredo IF EXISTS");
            s.execute("DROP TABLE correction_log IF EXISTS");

//                s.execute("CREATE TABLE token( tokenID IDENTITY(0), indexInDocument INTEGER, orig_id INTEGER, wOCR VARCHAR(60), wCorr VARCHAR(60), isNormal BOOLEAN, isCorrected BOOLEAN, numCands SMALLINT, cleft SMALLINT, cright SMALLINT, ctop SMALLINT, cbottom SMALLINT, special_seq VARCHAR(20), imageFile VARCHAR(200), isSuspicious BOOLEAN, pageIndex SMALLINT, topSuggestion VARCHAR(50), topCandDLev SMALLINT)");
            s.execute("CREATE TABLE token( tokenID INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0, INCREMENT BY 1) PRIMARY KEY, indexInDocument INTEGER, orig_id INTEGER, wOCR VARCHAR(60), wCorr VARCHAR(60), isNormal BOOLEAN, isCorrected BOOLEAN, numCands SMALLINT, cleft SMALLINT, cright SMALLINT, ctop SMALLINT, cbottom SMALLINT, special_seq VARCHAR(20), imageFile VARCHAR(200), isSuspicious BOOLEAN, pageIndex SMALLINT, topSuggestion VARCHAR(50), topCandDLev SMALLINT)");
//                s.execute("CREATE TABLE page( index SMALLINT GENERATED BY DEFAULT AS IDENTITY (START WITH 0, INCREMENT BY 1) PRIMARY KEY, token_index_from INTEGER, token_index_to INTEGER, imageFile VARCHAR(200))");
            s.execute("CREATE TABLE candidate( tokenID INTEGER, rank SMALLINT, suggestion VARCHAR(50), interpretation VARCHAR(200), voteweight REAL, dlev TINYINT, PRIMARY KEY (tokenID, rank))");
            s.execute("CREATE TABLE pattern (patternID INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0, INCREMENT BY 1) PRIMARY KEY, leftpart VARCHAR(5), rightpart VARCHAR(5), freq INTEGER, corrected INTEGER)");
            s.execute("CREATE TABLE patternoccurrence (patternID INTEGER, part INTEGER, PRIMARY KEY (patternID, part), wocr_lc VARCHAR(50), wsuggestion VARCHAR(50), freq INTEGER, corrected INTEGER)");
            s.execute("CREATE TABLE undoredo( operation_id SMALLINT, part SMALLINT, type VARCHAR(10), PRIMARY KEY(operation_id, part, type), edit_type VARCHAR(20), sql_command VARCHAR(100))");
            s.execute("CREATE TABLE correction_log( operation_id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 0, INCREMENT BY 1) PRIMARY KEY, user_name VARCHAR(20), operation_description VARCHAR(255))");

            s.execute("CREATE INDEX IDX_indexInDoc ON TOKEN(indexInDocument, pageIndex, isNormal, isSuspicious)");
            s.execute("CREATE INDEX IDX_indexInDoc_desc ON TOKEN(indexInDocument DESC, pageIndex, isNormal, isSuspicious)");

            s.close();
            conn.close();
            retval = 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return retval;
    }

    public int newDocumentFromOCRCXML(String dbPath, String ocrcxmlfile, String imagedir, ProgressHandle ph) {
        int retval = -1;
        if (this.newDocDatabase(dbPath) == 0) {
            ph.progress("Loading");
            jcp = JdbcConnectionPool.create("jdbc:h2:" + dbPath + ";AUTO_RECONNECT=TRUE;MVCC=true", "SA", "");
            jcp.setMaxConnections(50);
            jcp.setLoginTimeout(0);

            this.document = new SpreadIndexDocument(jcp);
            OcrXmlImporter.importDocument(document, ocrcxmlfile, imagedir);
            document.loadNumberOfPagesFromDB();
            document.loadNumberOfTokensFromDB();
            retval = 0;
        }
        return retval;
    }

    public int newDocumentFromXML(String dbPath, String xmldir, String imagedir, FileType t, String encoding, ProgressHandle ph) {
        int retval = -1;
        if (this.newDocDatabase(dbPath) == 0) {

            jcp = JdbcConnectionPool.create("jdbc:h2:" + dbPath + ";AUTO_RECONNECT=TRUE;MVCC=true", "SA", "");
            jcp.setMaxConnections(50);
            jcp.setLoginTimeout(0);

            FilenameFilter fil = t.getFilenameFilter();
            this.document = new SpreadIndexDocument(jcp);
            this.parser = getParser(t);
            File xmld = new File(xmldir);
            File imgd = new File(imagedir);
            String[] xmlfiles = xmld.list(fil);
            java.util.Arrays.sort(xmlfiles);
            HashMap<String, String> mappings = getImageFileMappings(imgd);

            long time_start = System.currentTimeMillis();
            for (String xmlfile : xmlfiles) {
                String basename = getBaseName(xmlfile);
                String imagefile = "";
                if (mappings.containsKey(basename)) {
                    imagefile = mappings.get(basename);
                }
//                Log.debug(
//                        this,
//                        "found image file: %s for file: %s",
//                        imagefile,
//                        xmlfile
//                );
                ph.progress("Parsing file " + xmlfile + " (" + imagefile + ")");
                try {
                    File f = new File(xmld, xmlfile);
                    parser.parse(
                            f.getCanonicalPath(),
                            imagefile,
                            encoding
                    );
                } catch (IOException ex) {
                    Log.error(
                            this, "could not parse %s: invalid image file: %s",
                            xmlfile,
                            ex.getMessage()
                    );
                }
            }
            long duration = System.currentTimeMillis() - time_start;

            ph.progress("Done parsing. Time elapsed " + duration);

            document.loadNumberOfPagesFromDB();
            document.loadNumberOfTokensFromDB();
            retval = 0;
        }
        return retval;
    }

    private OcrDocumentParser getParser(FileType type) {
        switch (type) {
            case ABBYY_XML_DIR:
                return new AbbyyXmlParser(this.document);
            case HOCR:
                return new HocrParser(this.document);
            default:
                return new AbbyyXmlParser(this.document);
        }
    }

    private HashMap<String, String> getImageFileMappings(File dir) {
        HashMap<String, String> mappings = new HashMap<>();
        String[] images = getAllImageFiles(dir);
        for (String image : images) {
            mappings.put(getBaseName(image), image);
        }
        return mappings;
    }

    private String[] getAllImageFiles(File dir) {
        if (dir == null) {
            return new String[0];
        }
        return dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.endsWith(".tif")
                        || name.endsWith(".jpg")
                        || name.endsWith(".jpeg");
            }
        });
    }

    private static String getBaseName(String filename) {
        return filename.substring(0, filename.indexOf('.') + 1);
    }

    public void importProfile(Document doc, String filename) throws IOException, SAXException {
        new ProfileImporter(doc).parse(filename);
    }

    public void closeDocument() {
        try {
            Connection conn = jcp.getConnection();
            Statement stat = conn.createStatement();
            stat.execute("SHUTDOWN COMPACT");
            stat.close();
            conn.close();
            jcp.dispose();
        } catch (SQLException ex) {
            Log.error(this, "could not shutdown database: %s", ex.getMessage());
        }
    }

    public Document getDocument() {
        return this.document;
    }
}
