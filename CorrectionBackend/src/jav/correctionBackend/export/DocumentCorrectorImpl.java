/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jav.correctionBackend.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author finkf
 */
public class DocumentCorrectorImpl extends DocumentCorrector {

    private final ArrayList<ArrayList<Char>> lines;
    private final PageParser pageParser;
    private final File input;

    public DocumentCorrectorImpl(File input, PageParser pageParser)
            throws IOException, Exception {
        this.pageParser = pageParser;
        lines = new ArrayList<>();
        this.input = input;
        parseLines();
    }

    @Override
    public int getNumberOfLines() {
        return lines.size();
    }

    @Override
    public String getLineAt(int i) {
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < lines.get(i).size(); ++j) {
            builder.append(lines.get(i).get(j).getChar());
        }
        return builder.toString();
    }

    @Override
    public void substitute(int i, int j, char c) {
        char tmp[] = {c};
        lines.get(i).get(j).substitute(new String(tmp));
    }

    @Override
    public void delete(int i, int j) {
        lines.get(i).get(j).delete();
        lines.get(i).remove(j);
    }

    @Override
    public void insert(int i, int j, char c) {
        char tmp[] = {c};
        if (j < lines.get(i).size()) {
            Char nc = lines.get(i).get(j).prepend(new String(tmp));
            lines.get(i).add(j, nc);
        } else {
            Char nc = lines.get(i).get(j).append(new String(tmp));
            lines.get(i).add(nc);
        }
    }

    @Override
    public void write(File output) throws IOException {
        try {
            pageParser.write(output);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void parseLines() throws IOException, Exception {
        Page page = pageParser.parse(input);
        for (Paragraph p : page) {
            for (Line l : p) {
                lines.add(new ArrayList<Char>());
                ArrayList<Char> back = lines.get(lines.size() - 1);
                for (Char c : l) {
                    back.add(c);
                }
            }
            lines.add(new ArrayList<Char>()); // append an empty line after paragraphs
        }
    }
}
