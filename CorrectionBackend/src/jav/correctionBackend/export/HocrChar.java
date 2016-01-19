/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jav.correctionBackend.export;

/**
 *
 * @author finkf
 */
public class HocrChar extends AbstractHocrChar {

    private final int i;
    private final BoundingBox bb;
    private final HocrToken token;

    public HocrChar(HocrToken token, BoundingBox bb, int i) {
        this.token = token;
        this.i = i;
        this.bb = bb;
    }

    @Override
    public BoundingBox getBoundingBox() {
        return bb;
    }

    @Override
    public String getChar() {
        return token.charAt(i);
    }

    @Override
    public boolean isSuspicious() {
        // Check for x_wconf in title
        return false;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void substitute(String c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Char append(String c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Char prepend(String c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
